package com.youin.now.auth;

/**
 * {@code DELETE /me} 요청.
 *
 * <p><b>비밀번호를 다시 받습니다.</b> 되돌릴 수 없는 동작이라 토큰만으로 실행하지 않습니다.
 *
 * @param password 지금 쓰는 비밀번호. 틀리면 {@code 401 INVALID_CREDENTIALS}
 */
public record AuthWithdrawReq(String password) {}
