package com.market.api;

import org.json.JSONObject;

public final class AuthApi {
    private AuthApi() {
    }

    public static HealthResult health() throws ApiException {
        JSONObject json = ApiClient.get("/api/health");
        return new HealthResult(
                json.optString("status", "UNKNOWN"),
                json.optString("database", "UNKNOWN")
        );
    }

    public static void sendVerificationCode(String email) throws ApiException {
        ApiClient.post("/api/auth/email/send", new JSONObject().put("email", email));
    }

    public static void verifyEmail(String email, String code) throws ApiException {
        ApiClient.post("/api/auth/email/verify", new JSONObject()
                .put("email", email)
                .put("code", code));
    }

    public static LoginResult register(String email,
                                       String password,
                                       String name,
                                       String phone,
                                       String address,
                                       String detailAddress) throws ApiException {
        JSONObject json = ApiClient.post("/api/auth/register", new JSONObject()
                .put("email", email)
                .put("password", password)
                .put("name", name)
                .put("phone", phone)
                .put("address", address)
                .put("detailAddress", detailAddress));
        return parseUser(json.getJSONObject("user"));
    }

    public static LoginResult login(String email, String password) throws ApiException {
        JSONObject json = ApiClient.post("/api/auth/login", new JSONObject()
                .put("email", email)
                .put("password", password));
        return parseUser(json.getJSONObject("user"));
    }

    public static void logout() throws ApiException {
        ApiClient.post("/api/auth/logout", new JSONObject());
    }

    private static LoginResult parseUser(JSONObject user) {
        return new LoginResult(
                user.optString("id", ""),
                user.optString("email", ""),
                user.optString("name", ""),
                user.isNull("phone") ? "" : user.optString("phone", ""),
                user.optString("role", "USER")
        );
    }

    public record LoginResult(String id, String email, String name, String phone, String role) {
    }

    public record HealthResult(String status, String database) {
        public boolean isReady() {
            return "UP".equalsIgnoreCase(status) && "CONNECTED".equalsIgnoreCase(database);
        }
    }
}
