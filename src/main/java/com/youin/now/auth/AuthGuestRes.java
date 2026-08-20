package com.youin.now.auth;

/**
 * {@code POST /auth/guest} 응답. 명세서 {@code NOW-AUTH-001} 그대로입니다.
 *
 * <pre>
 * { "token": "eyJhbGciOiJIUzI1NiJ9...", "userType": "guest",
 *   "expiresAt": "2026-09-10T19:00:00+09:00" }
 * </pre>
 *
 * <p><b>필드 이름을 바꾸지 마십시오.</b> 프론트가 이 이름으로 이미 파싱하고 있습니다.
 */
public record AuthGuestRes(String token, String userType, String expiresAt) {}
