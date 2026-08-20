package com.youin.now.auth;

/** {@code POST /auth/signup} 요청. {@code guestToken} 이 있으면 해당 게스트 데이터를 유지한 채 회원으로 전환합니다. */
public record AuthSignupReq(String email, String password, String nickname, String guestToken) {}
