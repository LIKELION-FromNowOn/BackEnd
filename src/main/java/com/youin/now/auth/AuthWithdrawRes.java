package com.youin.now.auth;

/**
 * {@code DELETE /me} 응답.
 *
 * <p>화면은 이 응답을 받으면 저장해 둔 세션을 지우고 처음 화면으로 보내야 합니다.
 * 서버는 토큰을 무효화하지 않습니다 — 세션 표를 안 쓰고 JWT 만 씁니다.
 * 다만 조회가 전부 {@code deleted_at IS NULL} 로 걸러져서, 남은 토큰으로 부르면
 * {@code 401} 이 납니다.
 */
public record AuthWithdrawRes(boolean withdrawn) {}
