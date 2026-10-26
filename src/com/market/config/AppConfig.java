package com.market.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Loads local application settings from config.properties.
 * The real config.properties file is excluded from Git.
 */
public final class AppConfig {
    private static final String CONFIG_FILE = "config.properties";
    private static final Properties PROPERTIES = loadProperties();

    private AppConfig() {
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream(CONFIG_FILE)) {
            properties.load(input);
            return properties;
        } catch (IOException e) {
            throw new IllegalStateException(
                    "config.properties 파일을 찾을 수 없습니다. "
                    + "config.example.properties를 복사하여 설정값을 입력하세요.", e);
        }
    }

    public static String getRequired(String key) {
        String value = PROPERTIES.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("필수 설정값이 없습니다: " + key);
        }
        return value.trim();
    }
}
