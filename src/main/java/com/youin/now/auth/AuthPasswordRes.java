package com.youin.now.auth;

/**
 * {@code PATCH /me/password} 응답.
 *
 * <p>토큰을 새로 주지 않습니다. 지금 쓰던 토큰이 그대로 살아 있습니다 —
 * 비밀번호를 바꿨다고 다시 로그인시키면 화면이 끊깁니다.
 */
public record AuthPasswordRes(boolean changed) {}
