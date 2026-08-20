package com.youin.now.auth;

/** {@code POST /auth/signup} 응답. */
public record AuthSignupRes(String token, String userType, Migrated migrated) {
    public record Migrated(long items, long checkins, long logs) {}
}
