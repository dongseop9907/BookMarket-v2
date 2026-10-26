package com.market.server.service;

import com.market.server.config.DatabaseConfig;
import org.json.JSONObject;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class OAuthFlow {
    private OAuthFlow() {}
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build();
    private static final Map<String,Flow> FLOWS = new ConcurrentHashMap<>();
    private static final Duration TTL=Duration.ofMinutes(5);
    public static final class Flow {
        public final String provider, verifier;
        public final Instant expiry=Instant.now().plus(TTL);
        public volatile String userId, email, name, phone, role, error;
        private Flow(String p,String v){provider=p;verifier=v;}
    }
    public record Start(String ticket,String url) {}
    public static String random(){byte[] b=new byte[32];RANDOM.nextBytes(b);return Base64.getUrlEncoder().withoutPadding().encodeToString(b);}
    public static String url(String v){return URLEncoder.encode(v,StandardCharsets.UTF_8);}
    public static String env(String key){String v=System.getenv(key);if(v==null||v.isBlank())v=System.getProperty(key);return v==null?"":v.trim();}
    private static String redirect(String provider){String val=env("BOOKMARKET_OAUTH_REDIRECT_BASE"); if(val.isBlank())val="http://localhost:8080/bookmarket-api";return val.replaceAll("/+$","")+"/api/oauth/callback/"+provider;}
    public static Start start(String provider) {
        if(!provider.equals("google")&&!provider.equals("kakao"))throw new IllegalArgumentException("UNSUPPORTED_PROVIDER");
        String client=env(provider.equals("google")?"BOOKMARKET_GOOGLE_CLIENT_ID":"BOOKMARKET_KAKAO_CLIENT_ID");
        if(client.isBlank())throw new IllegalStateException("OAUTH_CLIENT_NOT_CONFIGURED");
        cleanup(); String ticket=random();String verifier=random();FLOWS.put(ticket,new Flow(provider,verifier));
        String callback=redirect(provider);
        String endpoint=provider.equals("google")?"https://accounts.google.com/o/oauth2/v2/auth":"https://kauth.kakao.com/oauth/authorize";
        String scope=provider.equals("google")?"openid email profile":"profile_nickname";
        // state binds browser callback to a single, expiring desktop login attempt.
        String target=endpoint+"?response_type=code&client_id="+url(client)+"&redirect_uri="+url(callback)+"&scope="+url(scope)+"&state="+url(ticket);
        return new Start(ticket,target);
    }
    private static void cleanup(){FLOWS.entrySet().removeIf(e->Instant.now().isAfter(e.getValue().expiry));}
    public static Flow state(String ticket){Flow f=FLOWS.get(ticket);return f!=null&&Instant.now().isBefore(f.expiry)?f:null;}
    public static Flow take(String ticket){Flow f=state(ticket);if(f==null||f.userId==null)return null;return FLOWS.remove(ticket);}
    public static void fail(String ticket,String error){Flow f=state(ticket);if(f!=null)f.error=error;}
    private static JSONObject json(HttpRequest req)throws Exception {HttpResponse<String> r=HTTP.send(req,HttpResponse.BodyHandlers.ofString());if(r.statusCode()/100!=2)throw new IllegalStateException("PROVIDER_HTTP_"+r.statusCode());return new JSONObject(r.body());}
    private static HttpRequest.Builder request(String u){return HttpRequest.newBuilder(URI.create(u)).timeout(Duration.ofSeconds(12));}
    public static void complete(String provider,String ticket,String code)throws Exception {
        Flow f=state(ticket);if(f==null||!f.provider.equals(provider))throw new IllegalArgumentException("INVALID_STATE");
        if(f.userId!=null||f.error!=null)throw new IllegalStateException("ALREADY_COMPLETED");
        String id=env(provider.equals("google")?"BOOKMARKET_GOOGLE_CLIENT_ID":"BOOKMARKET_KAKAO_CLIENT_ID");
        String secret=env(provider.equals("google")?"BOOKMARKET_GOOGLE_CLIENT_SECRET":"BOOKMARKET_KAKAO_CLIENT_SECRET");
        String endpoint=provider.equals("google")?"https://oauth2.googleapis.com/token":"https://kauth.kakao.com/oauth/token";
        String form="grant_type=authorization_code&client_id="+url(id)+"&redirect_uri="+url(redirect(provider))+"&code="+url(code);
        if(!secret.isBlank())form+="&client_secret="+url(secret);
        JSONObject token=json(request(endpoint).header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(form)).build());
        String access=token.getString("access_token");
        String userinfo=provider.equals("google")?"https://openidconnect.googleapis.com/v1/userinfo":"https://kapi.kakao.com/v2/user/me";
        JSONObject profile=json(request(userinfo).header("Authorization","Bearer "+access).GET().build());
        String subject=provider.equals("google")?profile.getString("sub"):String.valueOf(profile.getLong("id"));
        String email=provider.equals("google")&&profile.optBoolean("email_verified",false)?profile.optString("email",""):"";
        String name=provider.equals("google")?profile.optString("name","Google 사용자"):profile.optJSONObject("properties")==null?"Kakao 사용자":profile.getJSONObject("properties").optString("nickname","Kakao 사용자");
        // Social identity is keyed by provider + subject, never by an unverified email.
        JSONObject user=upsert(provider,subject,email,name);
        f.userId=user.getString("id");f.email=user.getString("email");f.name=user.getString("name");f.phone=user.optString("phone","");f.role=user.getString("role");
    }
    private static JSONObject upsert(String provider,String subject,String email,String name)throws SQLException {
        try(Connection c=DatabaseConfig.getConnection()){
            c.setAutoCommit(false);
            try{
                UUID id=null;String dbEmail=null,dbName=null,phone=null,role=null;
                try(PreparedStatement ps=c.prepareStatement("select u.id,u.email,u.name,u.phone,u.role from public.social_accounts s join public.users u on u.id=s.user_id where s.provider=? and s.provider_user_id=?")){
                    ps.setString(1,provider);ps.setString(2,subject);
                    try(ResultSet rs=ps.executeQuery()){if(rs.next()){id=rs.getObject(1,UUID.class);dbEmail=rs.getString(2);dbName=rs.getString(3);phone=rs.getString(4);role=rs.getString(5);}}
                }
                if(id==null){
                    // Never attach to existing local accounts merely because emails match.
                    // Generate a stable placeholder unique to this external identity.
                    String generated=provider+"_"+subject+"@social.bookmarket.invalid";
                    try(PreparedStatement ps=c.prepareStatement("insert into public.users(email,password_hash,name,role) values (?,null,?,'USER') returning id,email,name,phone,role")){
                        ps.setString(1,generated);ps.setString(2,name.isBlank()?"소셜 사용자":name);
                        try(ResultSet rs=ps.executeQuery()){rs.next();id=rs.getObject(1,UUID.class);dbEmail=rs.getString(2);dbName=rs.getString(3);phone=rs.getString(4);role=rs.getString(5);}
                    }
                    try(PreparedStatement ps=c.prepareStatement("insert into public.social_accounts(provider,provider_user_id,user_id,provider_email) values (?,?,?,?)")){
                        ps.setString(1,provider);ps.setString(2,subject);ps.setObject(3,id);ps.setString(4,email.isBlank()?null:email);ps.executeUpdate();
                    }
                }
                c.commit();return new JSONObject().put("id",id.toString()).put("email",dbEmail).put("name",dbName).put("phone",phone==null?"":phone).put("role",role);
            }catch(Exception e){c.rollback();if(e instanceof SQLException s)throw s;throw new SQLException("SOCIAL_ACCOUNT_CREATE_FAILED",e);}
            finally{c.setAutoCommit(true);}
        }
    }
}
