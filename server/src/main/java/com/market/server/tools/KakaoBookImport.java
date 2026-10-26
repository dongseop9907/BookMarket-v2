package com.market.server.tools;

import com.market.server.config.DatabaseConfig;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Manual, local-only catalog import tool. Never exposed as an HTTP endpoint. */
public final class KakaoBookImport {
    private KakaoBookImport() {}

    public static void main(String[] args) throws Exception {
        String query = args.length >= 1 ? args[0].trim() : "자바";
        int size = args.length >= 2 ? Integer.parseInt(args[1]) : 12;
        boolean apply = args.length >= 3 && "--apply".equals(args[2]);
        if (query.isEmpty() || query.length() > 60 || size < 1 || size > 50) {
            throw new IllegalArgumentException("Query must be 1-60 chars; size must be 1-50");
        }
        if (args.length > 3 || (args.length == 3 && !apply)) {
            throw new IllegalArgumentException("Usage: <query> <1..50> [--apply]");
        }
        String key = System.getenv("BOOKMARKET_KAKAO_CLIENT_ID");
        if (key == null || key.isBlank()) key = System.getenv("BOOKMARKET_KAKAO_BOOK_REST_API_KEY");
        if (key == null || key.isBlank()) throw new IllegalStateException("Kakao REST API key missing in environment");

        String url = "https://dapi.kakao.com/v3/search/book?query=" +
                URLEncoder.encode(query, StandardCharsets.UTF_8) + "&size=" + size + "&page=1";
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Authorization", "KakaoAK " + key)
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(20)).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        System.out.println("KAKAO_HTTP=" + response.statusCode());
        if (response.statusCode() != 200) {
            System.out.println("IMPORT_STATUS=KAKAO_API_FAILURE");
            System.exit(2);
            return;
        }
        JSONArray docs = new JSONObject(response.body()).getJSONArray("documents");
        List<Book> books = new ArrayList<>();
        int skipped = 0;
        for (int i = 0; i < docs.length(); i++) {
            JSONObject doc = docs.getJSONObject(i);
            String isbn = chooseIsbn(doc.optString("isbn", ""));
            String title = doc.optString("title", "").trim();
            if (isbn == null || title.isEmpty()) { skipped++; continue; }
            String authors = join(doc.optJSONArray("authors"));
            String published = doc.optString("datetime", "");
            Date date = null;
            if (published.length() >= 10) {
                try { date = Date.valueOf(LocalDate.parse(published.substring(0, 10))); }
                catch (RuntimeException ignored) { }
            }
            books.add(new Book(isbn, cut(title,300), cut(authors,200),
                    cut(doc.optString("publisher", ""),200), doc.optString("contents", ""),
                    Math.max(0, doc.optInt("sale_price", -1) > 0 ? doc.optInt("sale_price") : doc.optInt("price",0)),
                    doc.optString("thumbnail", ""), date));
        }
        System.out.println("KAKAO_DOCUMENTS=" + docs.length());
        System.out.println("VALID_BOOKS=" + books.size());
        System.out.println("SKIPPED_NO_ISBN_OR_TITLE=" + skipped);
        System.out.println("MODE=" + (apply ? "APPLY" : "PREVIEW"));
        if (!apply) {
            System.out.println("DB_WRITES=0");
            for (int i = 0; i < Math.min(books.size(),5); i++) {
                System.out.println("BOOK_" + (i+1) + "=" + books.get(i).title());
            }
            System.out.println("IMPORT_STATUS=PREVIEW_OK");
            return;
        }
        String sql = "INSERT INTO public.books " +
                "(isbn,title,author,publisher,description,category,price,stock,published_date,thumbnail_url) " +
                "VALUES (?,?,?,?,?,'미분류',?,0,?,?) " +
                "ON CONFLICT (isbn) DO UPDATE SET title=EXCLUDED.title, author=EXCLUDED.author, " +
                "publisher=EXCLUDED.publisher, description=EXCLUDED.description, " +
                "price=EXCLUDED.price, published_date=EXCLUDED.published_date, thumbnail_url=EXCLUDED.thumbnail_url";
        int saved=0;
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (Book book : books) {
                    ps.setString(1,book.isbn()); ps.setString(2,book.title());
                    ps.setString(3,book.author()); ps.setString(4,book.publisher());
                    ps.setString(5,book.description()); ps.setInt(6,book.price());
                    ps.setDate(7,book.date()); ps.setString(8,book.thumbnail());
                    ps.addBatch();
                }
                int[] updates = ps.executeBatch();
                for (int v : updates) if (v >= 0 || v == PreparedStatement.SUCCESS_NO_INFO) saved++;
                conn.commit();
            } catch (Exception e) { conn.rollback(); throw e; }
        } catch (SQLException ex) {
            System.out.println("DB_ERROR_SQLSTATE=" + ex.getSQLState());
            throw new IllegalStateException("Import failed; no partial batch committed");
        }
        System.out.println("UPSERTED=" + saved);
        System.out.println("STOCK_POLICY=ZERO_UNTIL_ADMIN_SETS_REAL_INVENTORY");
        System.out.println("IMPORT_STATUS=APPLY_OK");
    }

    private static String chooseIsbn(String raw) {
        for (String token : raw.trim().split("\\s+")) {
            if (token.matches("[0-9]{13}")) return token;
        }
        for (String token : raw.trim().split("\\s+")) {
            if (token.matches("[0-9]{9}[0-9Xx]")) return token;
        }
        return null;
    }
    private static String join(JSONArray items) {
        if (items == null) return "";
        StringBuilder b = new StringBuilder();
        for (int i=0;i<items.length();i++) {
            if (i>0) b.append(", ");
            b.append(items.optString(i,""));
        }
        return b.toString();
    }
    private static String cut(String value, int limit) { return value.length() <= limit ? value : value.substring(0, limit); }
    private record Book(String isbn, String title, String author, String publisher,
                        String description, int price, String thumbnail, Date date) { }
}
