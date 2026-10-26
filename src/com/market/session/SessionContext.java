package com.market.session;

import com.market.api.AuthApi;

public final class SessionContext {
    private static AuthApi.LoginResult currentUser;

    private SessionContext() {
    }

    public static void login(AuthApi.LoginResult user) {
        currentUser = user;
    }

    public static AuthApi.LoginResult getCurrentUser() {
        return currentUser;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static boolean isAdmin() {
        return currentUser != null && "ADMIN".equalsIgnoreCase(currentUser.role());
    }

    public static void clear() {
        currentUser = null;
    }
}
