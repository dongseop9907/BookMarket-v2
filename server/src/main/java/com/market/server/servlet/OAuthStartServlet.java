package com.market.server.servlet;
import com.market.server.service.OAuthFlow;
import com.market.server.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.json.JSONObject;
import java.io.IOException;
@WebServlet("/api/oauth/start/*")
public class OAuthStartServlet extends HttpServlet {
 @Override protected void doPost(HttpServletRequest req,HttpServletResponse res)throws IOException {
  try {String provider=req.getPathInfo()==null?"":req.getPathInfo().replace("/","");OAuthFlow.Start s=OAuthFlow.start(provider);
   JsonUtil.write(res,200,new JSONObject().put("ticket",s.ticket()).put("authorizationUrl",s.url()));
  }catch(IllegalArgumentException e){JsonUtil.write(res,400,JsonUtil.error("INVALID_PROVIDER","지원하지 않는 로그인 제공자입니다."));}
   catch(Exception e){JsonUtil.write(res,503,JsonUtil.error("OAUTH_NOT_CONFIGURED","소셜 로그인 설정을 확인하세요."));}
 }
}
