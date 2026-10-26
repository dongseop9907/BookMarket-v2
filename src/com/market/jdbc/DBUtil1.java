package com.market.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBUtil1 {
    private static final String DB_PATH = "market.db"; // .jar 파일과 같은 위치

    public static Connection getConnection() {
        try {
            // 드라이버 로딩
            Class.forName("org.sqlite.JDBC");

            // SQLite 연결
            String url = "jdbc:sqlite:" + DB_PATH;
            return DriverManager.getConnection(url);
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) {
        Connection conn = getConnection();
        System.out.println(conn != null ? "✅ SQLite 연결 성공" : "❌ SQLite 연결 실패");
    }
}
