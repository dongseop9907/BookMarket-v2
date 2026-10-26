package com.market.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {
    public static Connection getConnection() throws Exception {
        // SQLite DB 파일 이름 (jar와 같은 폴더에 있어야 함)
        String url = "jdbc:sqlite:market.db";

        // 드라이버 로딩 (최신 버전은 생략 가능하지만 명시적으로 적는 게 좋음)
        Class.forName("org.sqlite.JDBC");

        return DriverManager.getConnection(url);
    }
}
