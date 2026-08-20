package com.youin.now.auth;

/** {@code GET /me} 응답. 게스트도 같은 형식으로 조회합니다. */
public record AuthMeRes(String userId, String userType, String name, String email,
                        String currentState, boolean recommendationPaused,
                        long itemCount, boolean hasCheckin) {}
