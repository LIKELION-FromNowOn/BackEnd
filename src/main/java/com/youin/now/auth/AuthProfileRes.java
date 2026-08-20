package com.youin.now.auth;

import java.util.List;

/** {@code PATCH /me} 응답. */
public record AuthProfileRes(String userId, String userType, String nickname, String email,
                             List<String> updatedFields) {}
