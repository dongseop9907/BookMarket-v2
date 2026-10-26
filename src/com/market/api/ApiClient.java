package com.market.api;

import org.json.JSONObject;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class ApiClient {
    private static final String DEFAULT_BASE_URL = "http://localhost:8080/bookmarket-api";
    private static final CookieManager COOKIE_MANAGER = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .cookieHandler(COOKIE_MANAGER)
            .build();

    private ApiClient() {
    }

    public static JSONObject get(String path) throws ApiException {
        HttpRequest request = HttpRequest.newBuilder(uri(path))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .GET()
                .build();
        return send(request);
    }

    public static JSONObject post(String path, JSONObject body) throws ApiException {
        HttpRequest request = HttpRequest.newBuilder(uri(path))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("X-BookMarket-Request", "1")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
        return send(request);
    }

    public static String getBaseUrl() {
        String systemProperty = System.getProperty("bookmarket.api.baseUrl");
        if (systemProperty != null && !systemProperty.isBlank()) {
            return trimSlash(systemProperty.trim());
        }

        String environment = System.getenv("BOOKMARKET_API_BASE_URL");
        if (environment != null && !environment.isBlank()) {
            return trimSlash(environment.trim());
        }
        return DEFAULT_BASE_URL;
    }

    private static URI uri(String path) {
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return URI.create(getBaseUrl() + normalizedPath);
    }

    private static JSONObject send(HttpRequest request) throws ApiException {
        try {
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            JSONObject json;
            try {
                json = response.body() == null || response.body().isBlank()
                        ? new JSONObject()
                        : new JSONObject(response.body());
            } catch (Exception parseError) {
                throw new ApiException(response.statusCode(), "INVALID_SERVER_RESPONSE",
                        "서버 응답 형식이 올바르지 않습니다.");
            }

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ApiException(
                        response.statusCode(),
                        json.optString("error", "HTTP_" + response.statusCode()),
                        json.optString("message", "서버 요청에 실패했습니다.")
                );
            }
            return json;
        } catch (IOException e) {
            throw new ApiException(0, "SERVER_UNREACHABLE",
                    "Tomcat 서버에 연결할 수 없습니다. 서버 실행 상태를 확인해 주세요.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(0, "REQUEST_INTERRUPTED", "서버 요청이 중단되었습니다.");
        }
    }

    private static String trimSlash(String value) {
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }
}
