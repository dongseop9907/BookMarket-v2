package com.market.server.servlet;
import com.market.server.service.OAuthFlow;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
@WebServlet("/api/oauth/callback/*")
public class OAuthCallbackServlet extends HttpServlet {
 @Override protected void doGet(HttpServletRequest req,HttpServletResponse res)throws IOException {
  String p=req.getPathInfo()==null?"":req.getPathInfo().replace("/","");String ticket=req.getParameter("state");
  res.setContentType("text/html;charset=UTF-8");res.setHeader("Cache-Control","no-store");res.setHeader("X-Content-Type-Options","nosniff");
  if(ticket==null||OAuthFlow.state(ticket)==null){res.setStatus(400);res.getWriter().print("<h2>인증 요청이 만료되었습니다.</h2>");return;}
  try {if(req.getParameter("error")!=null)throw new IllegalStateException("PROVIDER_DENIED");String code=req.getParameter("code");if(code==null||code.isBlank())throw new IllegalArgumentException("MISSING_CODE");
   OAuthFlow.complete(p,ticket,code);/* OAUTH_BLANK_CLOSE_V3_MARKER */ res.getWriter().print("<!doctype html><html lang=\"ko\"><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><title>BookMarket</title></head><body style=\"margin:0;background:#fff\"><script>setTimeout(function(){window.close();},1000);</script></body></html>");
  }catch(Exception e){OAuthFlow.fail(ticket,"OAUTH_CALLBACK_FAILED");res.setStatus(400);res.getWriter().print("<h2>인증에 실패했습니다.</h2><p>BookMarket에서 다시 시도해 주세요.</p>");}
 }
}
