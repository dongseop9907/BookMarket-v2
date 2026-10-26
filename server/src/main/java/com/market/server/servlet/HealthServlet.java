package com.market.server.servlet;

import com.market.server.config.DatabaseConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet("/api/health")
public class HealthServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");

        JSONObject body = new JSONObject();
        body.put("service", "bookmarket-api");

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement("select 1");
             ResultSet resultSet = statement.executeQuery()) {

            boolean databaseOk = resultSet.next() && resultSet.getInt(1) == 1;
            body.put("status", databaseOk ? "UP" : "DEGRADED");
            body.put("database", databaseOk ? "CONNECTED" : "UNKNOWN");
            response.setStatus(databaseOk ? HttpServletResponse.SC_OK
                    : HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        } catch (Exception e) {
            if (e instanceof java.sql.SQLException sqlException) {
                System.err.println("BOOKMARKET_DB_SQLSTATE=" + sqlException.getSQLState());
                System.err.println("BOOKMARKET_DB_VENDOR_CODE=" + sqlException.getErrorCode());
                Throwable cause = sqlException;
                for (int depth = 0; cause != null && depth < 8; depth++, cause = cause.getCause()) {
                    System.err.println("BOOKMARKET_CAUSE_CLASS_" + depth + "=" + cause.getClass().getSimpleName());
                    String message = cause.getMessage() == null ? "" : cause.getMessage().toLowerCase(java.util.Locale.ROOT);
                    String category = "OTHER";
                    if (message.contains("unknown host") || message.contains("name or service not known") || message.contains("nodename nor servname")) category = "DNS";
                    else if (message.contains("connection refused")) category = "CONNECTION_REFUSED";
                    else if (message.contains("timeout") || message.contains("timed out")) category = "TIMEOUT";
                    else if (message.contains("ssl") || message.contains("certificate") || message.contains("handshake")) category = "TLS_SSL";
                    else if (message.contains("authentication") || message.contains("password")) category = "AUTHENTICATION";
                    else if (message.contains("connection attempt failed")) category = "CONNECTION_ATTEMPT_FAILED";
                    else if (message.contains("network") || message.contains("unreachable")) category = "NETWORK_UNREACHABLE";
                    System.err.println("BOOKMARKET_CAUSE_CATEGORY_" + depth + "=" + category);
                }
            } else {
                System.err.println("BOOKMARKET_DB_EXCEPTION=" + e.getClass().getSimpleName());
            }
            body.put("status", "DOWN");
            body.put("database", "DISCONNECTED");
            body.put("error", e.getClass().getSimpleName());
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        }

        response.getWriter().write(body.toString());
    }
}
