package com.youin.now.auth;

/** {@code POST /auth/logout} 응답. 서버는 stateless이므로 클라이언트가 토큰을 삭제합니다. */
public record AuthLogoutRes(boolean loggedOut) {}
