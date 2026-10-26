package com.market.jdbc;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;

import org.json.JSONArray;
import org.json.JSONObject;

import com.market.bookitem.Book;
import com.market.config.AppConfig;

public class KaKaoBookImporter {

    public static void main(String[] args) {
        try {
            String[] keywords = { "자바", "파이썬", "C언어", "알고리즘", "자료구조", "프로그래밍", "웹", "컴퓨터공학" };
            String apiKey = "KakaoAK " + AppConfig.getRequired("kakao.rest.api.key");

            for (String keyword : keywords) {
                String query = URLEncoder.encode(keyword, "UTF-8");

                for (int page = 1; page <= 20; page++) {
                	
                    String apiURL = "https://dapi.kakao.com/v3/search/book?query=" + query + "&size=50&page=" + page;
                    HttpURLConnection conn = (HttpURLConnection) new URL(apiURL).openConnection();
                    conn.setRequestProperty("Authorization", apiKey);

                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }

                    JSONObject json = new JSONObject(sb.toString());
                    JSONArray docs = json.getJSONArray("documents");

                    if (docs.length() == 0) break;

                    for (int i = 0; i < docs.length(); i++) {
                        JSONObject item = docs.getJSONObject(i);
                        	
                        String isbn = item.optString("isbn", "NOISBN").split(" ")[0];
                        String title = item.optString("title", "제목 없음");
                        String author = item.getJSONArray("authors").optString(0, "저자 없음");
                        String publisher = item.optString("publisher", "출판사 없음");
                        int price = item.optInt("price", 0);
                        String thumbnail = item.optString("thumbnail", "");

                        // ✅ 추가된 항목
                        String releaseDateRaw = item.optString("datetime", "");
                        String releaseDate = releaseDateRaw.length() >= 10 ? releaseDateRaw.substring(0, 10) : "";

                        String category = getCategoryByTitle(title);

                        Book book = new Book(isbn, title, price, author, thumbnail, category, releaseDate);
                        
                        System.out.println("DEBUG: " + book.getName() + " | " + book.getCategory() + " | " + book.getReleaseDate());
                        
                        insertBookToDB(book);
                    }

                    System.out.println("✅ [" + keyword + "] " + page + "페이지 완료");
                    Thread.sleep(300); // API 요청 제한 방지
                }
            }

            System.out.println("📚 전체 키워드 도서 수집 완료!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void insertBookToDB(Book book) {
        try (Connection conn = DBUtil.getConnection()) {
        	String sql = "REPLACE INTO books (isbn, title, author, price, stock, publisher, category, release_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, book.getBookId());
            ps.setString(2, book.getName());
            ps.setString(3, book.getAuthor());
            ps.setInt(4, book.getUnitPrice());
            ps.setInt(5, 10); // 기본 재고
            ps.setString(6, book.getDescription()); // 썸네일
            ps.setString(7, book.getCategory());
            ps.setString(8, book.getReleaseDate());

            ps.executeUpdate();
            ps.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getCategoryByTitle(String title) {
        title = title.toLowerCase();

        if (title.contains("java") || title.contains("자바")) return "자바";
        if (title.contains("python") || title.contains("파이썬")) return "파이썬";
        if (title.contains("c++") || title.contains("c+")) return "c++";
        if (title.contains("c언어") || title.contains("c코딩")) return "c";
        if (title.contains("c프로그래밍") || title.contains("C프로그래밍")) return "c";
        if (title.contains("html") || title.contains("css") || title.contains("javascript") || title.contains("웹")) return "웹 개발";
        if (title.contains("sql") || title.contains("mysql") || title.contains("oracle")) return "데이터베이스";
        if (title.contains("spring") || title.contains("jsp")) return "웹 프레임워크";
        if (title.contains("ai") || title.contains("딥러닝") || title.contains("머신러닝")) return "인공지능";
        if (title.contains("algorithm") || title.contains("알고리즘")|| title.contains("자료구조")) return "자료구조";
        

        return "기타";
    }
}
