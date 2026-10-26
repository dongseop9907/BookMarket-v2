package com.market.server.servlet;

import com.market.server.config.DatabaseConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.UUID;

/** Read-only public book catalog API; never exposes database credentials. */
@WebServlet(urlPatterns = {"/api/books", "/api/books/*"})
public final class BooksServlet extends HttpServlet {
    private static final int MAX_LIMIT = 50;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        try {
            String path = request.getPathInfo();
            if (path != null && !path.isBlank() && !"/".equals(path)) {
                detail(request, response, path);
            } else {
                listing(request, response);
            }
        } catch (IllegalArgumentException ex) {
            send(response, 400, new JSONObject().put("error", "INVALID_ARGUMENT"));
        } catch (SQLException ex) {
            getServletContext().log("Book catalog database error; SQLSTATE=" + ex.getSQLState());
            send(response, 503, new JSONObject().put("error", "DATABASE_UNAVAILABLE"));
        } catch (Exception ex) {
            getServletContext().log("Book catalog internal error", ex);
            send(response, 500, new JSONObject().put("error", "INTERNAL_ERROR"));
        }
    }

    private void listing(HttpServletRequest request, HttpServletResponse response) throws Exception {
        int page = number(request.getParameter("page"), 1, 1, 100000);
        int limit = number(request.getParameter("limit"), 12, 1, MAX_LIMIT);
        String q = trim(request.getParameter("q"), 100);
        String category = trim(request.getParameter("category"), 100);
        // BM_ADMIN_STOCK_V3: server-wide stock filtering.
        String stockFilter = trim(request.getParameter("stockFilter"), 10);
        if (!stockFilter.isEmpty() && !stockFilter.equals("out") && !stockFilter.equals("low")) throw new IllegalArgumentException("Invalid stock filter");
        // Use values as bound parameters; never concatenate user strings into SQL.
        String where = " WHERE (? = '' OR title ILIKE ? ESCAPE '!' OR author ILIKE ? ESCAPE '!' OR isbn ILIKE ? ESCAPE '!') " +
                " AND (? = '' OR category = ?) ";
        if (stockFilter.equals("out")) where += " AND stock = 0 ";
        else if (stockFilter.equals("low")) where += " AND stock BETWEEN 1 AND 5 ";
        String select = "SELECT id, isbn, title, author, publisher, description, category, price, stock, published_date, thumbnail_url FROM public.books";
        String countSql = "SELECT count(*) FROM public.books" + where;
        String listSql = select + where + " ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?";
        int offset = Math.toIntExact(((long) page - 1) * limit);
        String like = "%" + escapeLike(q) + "%";
        JSONArray items = new JSONArray();
        int total;
        try (Connection conn = DatabaseConfig.getConnection()) {
            try (PreparedStatement count = conn.prepareStatement(countSql)) {
                bindFilters(count, q, like, category);
                try (ResultSet rs = count.executeQuery()) {
                    rs.next();
                    total = rs.getInt(1);
                }
            }
            try (PreparedStatement statement = conn.prepareStatement(listSql)) {
                bindFilters(statement, q, like, category);
                statement.setInt(7, limit);
                statement.setInt(8, offset);
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) items.put(book(rs));
                }
            }
        }
        send(response, 200, new JSONObject().put("items", items)
                .put("page", page).put("limit", limit).put("total", total)
                .put("totalPages", (total + limit - 1) / limit));
    }

    private void detail(HttpServletRequest request, HttpServletResponse response, String path) throws Exception {
        if (!path.matches("/[0-9a-fA-F\\-]{36}")) throw new IllegalArgumentException("Invalid book ID");
        UUID id = UUID.fromString(path.substring(1));
        String sql = "SELECT id, isbn, title, author, publisher, description, category, price, stock, published_date, thumbnail_url " +
                "FROM public.books WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    send(response, 404, new JSONObject().put("error", "BOOK_NOT_FOUND"));
                    return;
                }
                send(response, 200, book(rs));
            }
        }
    }

    private static void bindFilters(PreparedStatement ps, String q, String like, String category) throws SQLException {
        ps.setString(1, q);
        ps.setString(2, like);
        ps.setString(3, like);
        ps.setString(4, like);
        ps.setString(5, category);
        ps.setString(6, category);
    }

    private static String escapeLike(String value) {
        return value.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }

    private static String trim(String s, int max) {
        if (s == null) return "";
        s = s.trim();
        if (s.length() > max) throw new IllegalArgumentException("Input too long");
        return s;
    }

    private static int number(String s, int def, int min, int max) {
        if (s == null || s.isBlank()) return def;
        int n = Integer.parseInt(s);
        if (n < min || n > max) throw new IllegalArgumentException("Out of range");
        return n;
    }

    private static JSONObject book(ResultSet rs) throws SQLException {
        JSONObject obj = new JSONObject();
        for (String col : new String[]{"id", "isbn", "title", "author", "publisher", "description", "category", "thumbnail_url"}) {
            Object v = rs.getObject(col);
            obj.put(col, v == null ? JSONObject.NULL : v.toString());
        }
        obj.put("price", rs.getInt("price"));
        obj.put("stock", rs.getInt("stock"));
        java.sql.Date d = rs.getDate("published_date");
        obj.put("published_date", d == null ? JSONObject.NULL : d.toString());
        return obj;
    }

    private static void send(HttpServletResponse response, int status, JSONObject body) throws IOException {
        response.setStatus(status);
        response.getWriter().write(body.toString());
    }
}
