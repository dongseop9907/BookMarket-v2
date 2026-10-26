package com.market.server.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConfig {
    private static final String URL_ENV = "BOOKMARKET_DB_URL";
    private static final String USER_ENV = "BOOKMARKET_DB_USER";
    private static final String PASSWORD_ENV = "BOOKMARKET_DB_PASSWORD";

    private DatabaseConfig() {
    }

    public static Connection getConnection() throws SQLException {
        String url = required(URL_ENV);
        String user = required(USER_ENV);
        String password = required(PASSWORD_ENV);
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException ex) {
            throw new SQLException("PostgreSQL JDBC driver missing", "08001", ex);
        }
        java.util.Properties props = new java.util.Properties();
        props.setProperty("user", user);
        props.setProperty("password", password);
        props.setProperty("connectTimeout", "8");
        props.setProperty("socketTimeout", "8");
        return DriverManager.getConnection(toJdbcUrl(url), props);
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            value = System.getProperty(name);
        }
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required configuration: " + name);
        }
        return value.trim();
    }

    private static String toJdbcUrl(String value) {
        if (value.startsWith("jdbc:postgresql://")) {
            return value;
        }
        if (value.startsWith("postgresql://")) {
            return "jdbc:" + value;
        }
        if (value.startsWith("postgres://")) {
            return "jdbc:postgresql://" + value.substring("postgres://".length());
        }
        return value;
    }
}
