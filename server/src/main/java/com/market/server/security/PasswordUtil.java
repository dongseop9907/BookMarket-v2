package com.market.server.security;

import org.mindrot.jbcrypt.BCrypt;

public final class PasswordUtil {
    private static final int LOG_ROUNDS = 12;

    private PasswordUtil() {
    }

    public static String hash(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(LOG_ROUNDS));
    }

    public static boolean matches(String rawPassword, String passwordHash) {
        if (rawPassword == null || passwordHash == null || passwordHash.isBlank()) {
            return false;
        }
        try {
            return BCrypt.checkpw(rawPassword, passwordHash);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
