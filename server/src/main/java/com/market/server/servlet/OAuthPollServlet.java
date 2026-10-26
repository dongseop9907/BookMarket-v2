package com.market.server.servlet;
import com.market.server.service.OAuthFlow;
import com.market.server.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.json.JSONObject;
import java.io.IOException;
@WebServlet("/api/oauth/poll")
public class OAuthPollServlet extends HttpServlet {
 @Override protected void doPost(HttpServletRequest req,HttpServletResponse res)throws IOException {
  res.setHeader("Cache-Control","no-store");
  String ticket=JsonUtil.readObject(req).optString("ticket","");OAuthFlow.Flow f=OAuthFlow.state(ticket);
  if(f==null){JsonUtil.write(res,410,JsonUtil.error("EXPIRED","로그인 시간이 만료되었습니다."));return;}
  if(f.error!=null){JsonUtil.write(res,400,JsonUtil.error(f.error,"소셜 로그인에 실패했습니다."));return;}
  if(f.userId==null){JsonUtil.write(res,200,new JSONObject().put("status","PENDING"));return;}
  f=OAuthFlow.take(ticket);if(f==null){JsonUtil.write(res,409,JsonUtil.error("ALREADY_CONSUMED","이미 완료된 인증입니다."));return;}
  HttpSession old=req.getSession(false);if(old!=null)old.invalidate();HttpSession session=req.getSession(true);
  session.setAttribute("userId",f.userId);session.setAttribute("role",f.role);session.setMaxInactiveInterval(1800);
  JsonUtil.write(res,200,new JSONObject().put("status","COMPLETE").put("user",new JSONObject().put("id",f.userId).put("email",f.email).put("name",f.name).put("phone",f.phone).put("role",f.role)));
 }
}
