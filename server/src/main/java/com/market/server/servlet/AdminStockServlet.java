package com.market.server.servlet;

import com.market.server.config.DatabaseConfig;
import com.market.server.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/** Admin-only inventory adjustment. Use an actual logged-in BookMarket session. */
@WebServlet("/api/admin/books/stock")
public final class AdminStockServlet extends HttpServlet {
    // BM_ADMIN_DASH_V4: read-only aggregated stock statistics; auth is checked on every request.
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setHeader("Cache-Control", "no-store");
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            JsonUtil.write(res, 401, JsonUtil.error("UNAUTHORIZED", "Login required."));
            return;
        }
        try {
            UUID userId = UUID.fromString(session.getAttribute("userId").toString());
            try (Connection connection = DatabaseConfig.getConnection()) {
                try (PreparedStatement check = connection.prepareStatement("select role from public.users where id=?")) {
                    check.setObject(1, userId);
                    try (ResultSet found = check.executeQuery()) {
                        if (!found.next() || !"ADMIN".equals(found.getString(1))) {
                            JsonUtil.write(res, 403, JsonUtil.error("ADMIN_REQUIRED", "Admin required."));
                            return;
                        }
                    }
                }
                try (PreparedStatement ps = connection.prepareStatement(
                        "select count(*) total, count(*) filter (where stock=0) out_count, " +
                        "count(*) filter (where stock between 1 and 5) low_count, " +
                        "coalesce(sum(stock),0) stock_sum from public.books");
                     ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    JsonUtil.write(res, 200, new JSONObject()
                            .put("total", rs.getLong("total"))
                            .put("outOfStock", rs.getLong("out_count"))
                            .put("lowStock", rs.getLong("low_count"))
                            .put("totalStock", rs.getLong("stock_sum")));
                }
            }
        } catch (SQLException ex) {
            getServletContext().log("Admin dashboard SQL error", ex);
            JsonUtil.write(res, 503, JsonUtil.error("DATABASE_UNAVAILABLE", "Database unavailable."));
        } catch (Exception ex) {
            getServletContext().log("Admin dashboard error", ex);
            JsonUtil.write(res, 500, JsonUtil.error("INTERNAL_ERROR", "Stats unavailable."));
        }
    }
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setHeader("Cache-Control", "no-store");
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            JsonUtil.write(res, 401, JsonUtil.error("UNAUTHORIZED", "로그인이 필요합니다."));
            return;
        }
        // A custom request header prevents ordinary cross-origin form submissions.
        if (!"1".equals(req.getHeader("X-BookMarket-Request"))) {
            JsonUtil.write(res, 403, JsonUtil.error("REQUEST_HEADER_REQUIRED", "요청 헤더를 확인하세요."));
            return;
        }
        String origin = req.getHeader("Origin");
        if (origin != null && !origin.isBlank() && !origin.equals("http://localhost:8080")
                && !origin.equals("http://127.0.0.1:8080")) {
            JsonUtil.write(res, 403, JsonUtil.error("INVALID_ORIGIN", "요청 출처가 유효하지 않습니다."));
            return;
        }
        try {
            UUID userId = UUID.fromString(session.getAttribute("userId").toString());
            JSONObject input = JsonUtil.readObject(req);
            UUID bookId = UUID.fromString(input.getString("bookId"));
            Object rawStock = input.get("stock");
            if (!(rawStock instanceof Integer) && !(rawStock instanceof Long)) {
                throw new IllegalArgumentException("stock must be an integer");
            }
            long candidate = ((Number)rawStock).longValue();
            if (candidate < 0 || candidate > 1_000_000) throw new IllegalArgumentException("invalid stock");
            int stock = (int) candidate;

            try (Connection connection = DatabaseConfig.getConnection()) {
                // Read current role from the database, never trust a client-supplied role.
                try (PreparedStatement check = connection.prepareStatement(
                        "select role from public.users where id=?")) {
                    check.setObject(1, userId);
                    try (ResultSet found = check.executeQuery()) {
                        if (!found.next() || !"ADMIN".equals(found.getString(1))) {
                            JsonUtil.write(res, 403, JsonUtil.error("ADMIN_REQUIRED", "관리자 권한이 필요합니다."));
                            return;
                        }
                    }
                }
                try (PreparedStatement update = connection.prepareStatement(
                        "update public.books set stock=? where id=? returning id, isbn, title, stock")) {
                    update.setInt(1, stock);
                    update.setObject(2, bookId);
                    try (ResultSet rs = update.executeQuery()) {
                        if (!rs.next()) {
                            JsonUtil.write(res, 404, JsonUtil.error("BOOK_NOT_FOUND", "도서가 존재하지 않습니다."));
                            return;
                        }
                        JsonUtil.write(res, 200, new JSONObject().put("ok", true)
                                .put("book", new JSONObject().put("id", rs.getString("id"))
                                        .put("isbn", rs.getString("isbn"))
                                        .put("title", rs.getString("title"))
                                        .put("stock", rs.getInt("stock"))));
                    }
                }
            }
        } catch (IllegalArgumentException | org.json.JSONException ex) {
            JsonUtil.write(res, 400, JsonUtil.error("INVALID_REQUEST", "도서 ID와 재고 정수를 확인하세요."));
        } catch (SQLException ex) {
            getServletContext().log("Admin inventory update failed; SQLSTATE=" + ex.getSQLState());
            JsonUtil.write(res, 503, JsonUtil.error("DATABASE_UNAVAILABLE", "DB 연결을 확인하세요."));
        } catch (Exception ex) {
            getServletContext().log("Admin inventory internal error", ex);
            JsonUtil.write(res, 500, JsonUtil.error("INTERNAL_ERROR", "재고 수정에 실패했습니다."));
        }
    }
}
