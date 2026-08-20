package com.youin.now.auth;

/** {@code POST /auth/login} 요청. */
public record AuthLoginReq(String email, String password) {}
