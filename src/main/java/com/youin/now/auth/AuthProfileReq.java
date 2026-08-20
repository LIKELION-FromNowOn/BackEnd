package com.youin.now.auth;

import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * {@code PATCH /me} 요청. null 과 미전송을 구분해야 하므로 record 대신 전송 여부를 함께 보관합니다.
 * {@code email: null} 은 이메일 삭제이고, 이메일 필드 미전송은 이메일을 그대로 둡니다.
 */
public final class AuthProfileReq {
    private String nickname;
    private String email;
    private boolean nicknameProvided;
    private boolean emailProvided;

    @JsonSetter("nickname")
    public void setNickname(String nickname) {
        this.nickname = nickname;
        this.nicknameProvided = true;
    }

    @JsonSetter("email")
    public void setEmail(String email) {
        this.email = email;
        this.emailProvided = true;
    }

    public String nickname() { return nickname; }
    public String email() { return email; }
    public boolean nicknameProvided() { return nicknameProvided; }
    public boolean emailProvided() { return emailProvided; }
}
