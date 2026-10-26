package com.market.server.servlet;

import com.market.server.dao.AuthDao;
import com.market.server.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONObject;

import java.io.IOException;

@WebServlet("/api/auth/email/verify")
public class EmailVerifyServlet extends HttpServlet {
    private final AuthDao authDao = new AuthDao();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            JSONObject input = JsonUtil.readObject(request);
            String email = input.optString("email", "").trim().toLowerCase();
            String code = input.optString("code", "").trim();

            if (email.isBlank() || code.length() != 6) {
                JsonUtil.write(response, 400, JsonUtil.error("INVALID_REQUEST", "이메일과 6자리 인증번호가 필요합니다."));
                return;
            }

            boolean verified = authDao.verifyEmailCode(email, code);
            if (!verified) {
                JsonUtil.write(response, 400, JsonUtil.error("INVALID_OR_EXPIRED_CODE", "인증번호가 틀렸거나 만료되었습니다."));
                return;
            }

            JsonUtil.write(response, 200, new JSONObject().put("ok", true).put("verified", true));
        } catch (Exception e) {
            JsonUtil.write(response, 500, JsonUtil.error("VERIFY_FAILED", "이메일 인증 처리에 실패했습니다."));
        }
    }
}
