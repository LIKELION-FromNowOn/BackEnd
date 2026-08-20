package com.youin.now.auth;

/** {@code POST /auth/login} 응답. */
public record AuthLoginRes(String token, String userType, String name) {}
