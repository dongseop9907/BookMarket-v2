package com.market.server.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** Live search of Kakao books; no persistence or database writes. */
@WebServlet("/api/discover/kakao")
public final class KakaoDiscoverServlet extends HttpServlet {
    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5)).build();

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store");
        String q = req.getParameter("q");
        q = q == null ? "" : q.trim();
        if (q.isBlank() || q.length() > 100) {
            error(resp, 400, "QUERY_REQUIRED"); return;
        }
        int page = parse(req.getParameter("page"), 1, 1, 50);
        int size = parse(req.getParameter("size"), 20, 1, 50);
        if (page < 0 || size < 0) { error(resp, 400, "INVALID_PAGE_OR_SIZE"); return; }
        String key = System.getenv("BOOKMARKET_KAKAO_CLIENT_ID");
        if (key == null || key.isBlank()) { error(resp, 503, "KAKAO_KEY_NOT_CONFIGURED"); return; }
        String uri = "https://dapi.kakao.com/v3/search/book?query=" +
                URLEncoder.encode(q, StandardCharsets.UTF_8) + "&page=" + page + "&size=" + size;
        HttpRequest upstream = HttpRequest.newBuilder(URI.create(uri))
            .header("Authorization", "KakaoAK " + key)
            .header("Accept", "application/json")
            .timeout(Duration.ofSeconds(12)).GET().build();
        try {
            HttpResponse<String> result = HTTP.send(upstream, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (result.statusCode() != 200) {
                getServletContext().log("Kakao book API failed: HTTP " + result.statusCode());
                error(resp, result.statusCode() == 429 ? 429 : 502, "KAKAO_UPSTREAM_FAILED");
                return;
            }
            JSONObject raw = new JSONObject(result.body());
            JSONArray found = raw.optJSONArray("documents");
            JSONArray items = new JSONArray();
            if (found != null) for (int i=0; i<found.length(); i++) {
                JSONObject b = found.optJSONObject(i);
                if (b == null) continue;
                JSONObject item = new JSONObject();
                for (String field : new String[]{"title", "isbn", "publisher", "thumbnail", "contents", "datetime", "status"})
                    item.put(field, b.optString(field, ""));
                item.put("authors", b.optJSONArray("authors") == null ? new JSONArray() : b.optJSONArray("authors"));
                item.put("price", b.optInt("price", 0));
                item.put("sale_price", b.optInt("sale_price", 0));
                item.put("orderable", false); // Discovery is not a stocked BookMarket item.
                items.put(item);
            }
            JSONObject meta = raw.optJSONObject("meta");
            JSONObject output = new JSONObject()
                .put("source", "KAKAO_LIVE")
                .put("stored", false)
                .put("page", page)
                .put("size", size)
                .put("items", items)
                .put("is_end", meta != null && meta.optBoolean("is_end", true))
                .put("total_count", meta == null ? 0 : meta.optInt("total_count", 0));
            resp.setStatus(200);
            resp.getWriter().write(output.toString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); error(resp, 503, "REQUEST_INTERRUPTED");
        } catch (Exception e) {
            getServletContext().log("Kakao book search error", e);
            error(resp, 502, "KAKAO_SEARCH_FAILED");
        }
    }

    private static int parse(String value, int fallback, int min, int max) {
        if (value == null || value.isBlank()) return fallback;
        try { int n=Integer.parseInt(value); return n>=min && n<=max ? n : -1; }
        catch (NumberFormatException e) { return -1; }
    }
    private static void error(HttpServletResponse response, int status, String code) throws IOException {
        response.setStatus(status);
        response.getWriter().write(new JSONObject().put("error", code).toString());
    }
}
