package com.market.server.servlet;

import com.market.server.dao.AuthDao;
import com.market.server.model.AuthUser;
import com.market.server.security.PasswordUtil;
import com.market.server.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/api/auth/register")
public class RegisterServlet extends HttpServlet {
    private final AuthDao authDao = new AuthDao();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            JSONObject input = JsonUtil.readObject(request);
            String email = input.optString("email", "").trim().toLowerCase();
            String password = input.optString("password", "");
            String name = input.optString("name", "").trim();
            String phone = input.optString("phone", "").trim();
            String address = input.optString("address", "").trim();
            String detailAddress = input.optString("detailAddress", "").trim();

            if (!email.contains("@") || name.isBlank() || !isValidPassword(password)) {
                JsonUtil.write(response, 400, JsonUtil.error(
                        "INVALID_REQUEST", "이메일, 이름, 8자 이상·공백 없음·특수문자 포함 비밀번호가 필요합니다."));
                return;
            }
            if (authDao.emailExists(email)) {
                JsonUtil.write(response, 409, JsonUtil.error("EMAIL_ALREADY_EXISTS", "이미 가입된 이메일입니다."));
                return;
            }

            AuthUser user = authDao.createUser(
                    email,
                    PasswordUtil.hash(password),
                    name,
                    phone,
                    address,
                    detailAddress
            );

            JsonUtil.write(response, 201, userJson(user).put("ok", true));
        } catch (IllegalStateException e) {
            if ("EMAIL_NOT_VERIFIED".equals(e.getMessage())) {
                JsonUtil.write(response, 400, JsonUtil.error("EMAIL_NOT_VERIFIED", "이메일 인증을 먼저 완료해 주세요."));
            } else {
                JsonUtil.write(response, 500, JsonUtil.error("REGISTER_FAILED", "회원가입에 실패했습니다."));
            }
        } catch (SQLException e) {
            JsonUtil.write(response, 500, JsonUtil.error("REGISTER_FAILED", "회원가입에 실패했습니다."));
        }
    }

    private JSONObject userJson(AuthUser user) {
        return new JSONObject()
                .put("user", new JSONObject()
                        .put("id", user.id().toString())
                        .put("email", user.email())
                        .put("name", user.name())
                        .put("phone", user.phone() == null ? JSONObject.NULL : user.phone())
                        .put("role", user.role()));
    }
    private boolean isValidPassword(String password) {
        return password != null
                && password.length() >= 8
                && !password.contains(" ")
                && password.matches(".*[!@#$%^&*()_+=\\-\\[\\]{}|;:'\",.<>/?`~].*");
    }

}
