package com.market.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import com.market.config.AppConfig;

public class DBUtil {
    public static Connection getConnection() {
        try {
            String url = AppConfig.getRequired("db.url");
            String user = AppConfig.getRequired("db.user");
            String password = AppConfig.getRequired("db.password");

            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(url, user, password);
        } catch (ClassNotFoundException | SQLException | IllegalStateException e) {
            System.err.println("DB 연결 오류: " + e.getMessage());
            return null;
        }
    }

    public static void main(String[] args) {
        Connection conn = DBUtil.getConnection();
        System.out.println(conn != null ? "DB 연결 성공" : "DB 연결 실패");
    }
}
