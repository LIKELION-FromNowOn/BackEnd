package com.youin.now.auth;

/**
 * {@code PATCH /me/password} 요청.
 *
 * <p><b>지금 비밀번호를 반드시 다시 받습니다.</b> 토큰만으로 바꾸게 하면 토큰이 한 번 새는 순간
 * 계정을 통째로 뺏깁니다. 토큰은 30일짜리라 새고 나서 알아채기까지 시간이 깁니다.
 *
 * @param currentPassword 지금 쓰는 비밀번호. 틀리면 {@code 401 INVALID_CREDENTIALS}
 * @param newPassword     새 비밀번호. 8~64자
 */
public record AuthPasswordReq(String currentPassword, String newPassword) {}
