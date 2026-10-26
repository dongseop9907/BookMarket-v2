package com.market.server.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;

public final class JsonUtil {
    private JsonUtil() {
    }

    public static JSONObject readObject(HttpServletRequest request) throws IOException {
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        }
        if (body.isEmpty()) {
            return new JSONObject();
        }
        return new JSONObject(body.toString());
    }

    public static void write(HttpServletResponse response, int status, JSONObject body) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.getWriter().write(body.toString());
    }

    public static JSONObject error(String code, String message) {
        return new JSONObject()
                .put("ok", false)
                .put("error", code)
                .put("message", message);
    }
}
