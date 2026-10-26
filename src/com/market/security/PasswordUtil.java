package com.market.security;

import org.mindrot.jbcrypt.BCrypt;

/**
 * 사용자 비밀번호의 해시 생성과 검증을 담당합니다.
 */
public final class PasswordUtil {

    private static final int LOG_ROUNDS = 12;

    private PasswordUtil() {
        // 인스턴스 생성을 방지하는 유틸리티 클래스
    }

    /**
     * 평문 비밀번호를 BCrypt 해시 문자열로 변환합니다.
     *
     * @param plainPassword 사용자가 입력한 평문 비밀번호
     * @return BCrypt로 생성한 비밀번호 해시
     */
    public static String hashPassword(String plainPassword) {
        validatePassword(plainPassword);

        return BCrypt.hashpw(
                plainPassword,
                BCrypt.gensalt(LOG_ROUNDS)
        );
    }

    /**
     * 사용자가 입력한 비밀번호와 저장된 해시를 비교합니다.
     *
     * @param plainPassword 사용자가 입력한 평문 비밀번호
     * @param hashedPassword 데이터베이스에 저장된 BCrypt 해시
     * @return 비밀번호가 일치하면 true
     */
    public static boolean verifyPassword(
            String plainPassword,
            String hashedPassword
    ) {
        if (plainPassword == null || plainPassword.isBlank()) {
            return false;
        }

        if (hashedPassword == null || hashedPassword.isBlank()) {
            return false;
        }

        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException(
                    "비밀번호를 입력해야 합니다."
            );
        }

        if (password.length() < 8) {
            throw new IllegalArgumentException(
                    "비밀번호는 8자 이상이어야 합니다."
            );
        }
    }
}