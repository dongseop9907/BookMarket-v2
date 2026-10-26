package com.market.bookitem;

import com.market.api.ApiClient;
import com.market.api.ApiException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/** Books are read only through Tomcat; no client-side JDBC credentials. */
public final class BookInIt {
    private static ArrayList<Book> mBookList = new ArrayList<>();
    private static int mTotalBook = 0;
    private BookInIt() {}

    public static synchronized void init() {
        ArrayList<Book> loaded = new ArrayList<>();
        try {
            int page = 1;
            final int limit = 50;
            int total;
            do {
                JSONObject response = ApiClient.get("/api/books?page=" + page + "&limit=" + limit);
                total = response.getInt("total");
                JSONArray items = response.getJSONArray("items");
                for (int i = 0; i < items.length(); i++) {
                    JSONObject json = items.getJSONObject(i);
                    String isbn = json.optString("isbn", "");
                    String title = json.optString("title", "");
                    if (isbn.isBlank() || title.isBlank()) continue;
                    String published = json.optString("published_date", "");
                    if ("null".equals(published)) published = "";
                    Book book = new Book(isbn, title,
                            json.optString("author", ""),
                            json.optInt("price", 0),
                            json.optInt("stock", 0),
                            json.optString("publisher", ""),
                            json.optString("category", ""), published);
                    book.setStock(json.optInt("stock", 0));
                    book.setThumbnailUrl(json.optString("thumbnail_url", ""));
                    loaded.add(book);
                }
                if (items.isEmpty()) break;
                page++;
                if (page > 100) throw new IllegalStateException("도서 목록이 5,000권을 초과합니다. 서버 페이지 조회 방식이 필요합니다.");
            } while (loaded.size() < total);
            mBookList = loaded;
            mTotalBook = loaded.size();
            System.out.println("BOOKMARKET_BOOK_SOURCE=TOMCAT_API BOOKS_LOADED=" + mTotalBook);
        } catch (ApiException e) {
            throw new IllegalStateException("Tomcat 도서 API 조회 실패: " + e.getMessage(), e);
        }
    }

    public static int totalFileToBookList() { return mTotalBook; }
    public static void setFileToBookList(ArrayList<Book> target) {
        init();
        target.clear();
        target.addAll(mBookList);
    }
    public static ArrayList<Book> getmBookList() { return mBookList; }
    public static void setmBookList(ArrayList<Book> books) {
        mBookList = books == null ? new ArrayList<>() : books;
        mTotalBook = mBookList.size();
    }
    public static int getmTotalBook() { return mTotalBook; }
    public static void setmTotalBook(int total) { mTotalBook = total; }
}
