package com.market.server.servlet;

import com.market.server.dao.AuthDao;
import com.market.server.service.EmailService;
import com.market.server.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONObject;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@WebServlet("/api/auth/email/send")
public class EmailSendServlet extends HttpServlet {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final AuthDao authDao = new AuthDao();
    private final EmailService emailService = new EmailService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            JSONObject input = JsonUtil.readObject(request);
            String email = input.optString("email", "").trim().toLowerCase();
            if (!email.contains("@") || email.length() > 255) {
                JsonUtil.write(response, 400, JsonUtil.error("INVALID_EMAIL", "올바른 이메일을 입력해 주세요."));
                return;
            }
            if (authDao.emailExists(email)) {
                JsonUtil.write(response, 409, JsonUtil.error("EMAIL_ALREADY_EXISTS", "이미 가입된 이메일입니다."));
                return;
            }

            String code = String.format("%06d", RANDOM.nextInt(1_000_000));
            Instant expiresAt = Instant.now().plus(10, ChronoUnit.MINUTES);
            authDao.saveVerificationCode(email, code, expiresAt);
            emailService.sendVerificationCode(email, code);

            JsonUtil.write(response, 200, new JSONObject()
                    .put("ok", true)
                    .put("expiresInSeconds", 600));
        } catch (Exception e) {
            JsonUtil.write(response, 500, JsonUtil.error("EMAIL_SEND_FAILED", "인증번호 발송에 실패했습니다."));
        }
    }
}
