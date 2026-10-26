package com.market.server.servlet;

import com.market.server.dao.AuthDao;
import com.market.server.model.AuthUser;
import com.market.server.security.PasswordUtil;
import com.market.server.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Optional;

@WebServlet("/api/auth/login")
public class LoginServlet extends HttpServlet {
    private final AuthDao authDao = new AuthDao();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            JSONObject input = JsonUtil.readObject(request);
            String email = input.optString("email", "").trim().toLowerCase();
            String password = input.optString("password", "");

            Optional<AuthUser> found = authDao.findByEmail(email);
            if (found.isEmpty() || found.get().passwordHash() == null || !PasswordUtil.matches(password, found.get().passwordHash())) {
                JsonUtil.write(response, 401, JsonUtil.error("INVALID_CREDENTIALS", "이메일 또는 비밀번호가 올바르지 않습니다."));
                return;
            }

            AuthUser user = found.get();
            HttpSession session = request.getSession(true);
            session.setAttribute("userId", user.id().toString());
            session.setAttribute("role", user.role());
            session.setMaxInactiveInterval(30 * 60);

            JsonUtil.write(response, 200, new JSONObject()
                    .put("ok", true)
                    .put("user", new JSONObject()
                            .put("id", user.id().toString())
                            .put("email", user.email())
                            .put("name", user.name())
                            .put("phone", user.phone() == null ? JSONObject.NULL : user.phone())
                            .put("role", user.role())));
        } catch (Exception e) {
            JsonUtil.write(response, 500, JsonUtil.error("LOGIN_FAILED", "로그인 처리에 실패했습니다."));
        }
    }
}
