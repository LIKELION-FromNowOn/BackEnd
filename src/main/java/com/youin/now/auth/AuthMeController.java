package com.youin.now.auth;

import com.youin.now.common.response.ApiResponse;
import com.youin.now.common.security.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** {@code /me} 는 인증 경로와 달리 앱의 사용자 영역 최상단에 둡니다. */
@RestController
@RequestMapping("/me")
public class AuthMeController {

    private final AuthService authService;

    public AuthMeController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping
    public ApiResponse<AuthMeRes> me(@CurrentUser String userId) {
        return ApiResponse.ok(authService.me(userId));
    }

    @PatchMapping
    public ApiResponse<AuthProfileRes> update(@CurrentUser String userId, @RequestBody AuthProfileReq req) {
        return ApiResponse.ok(authService.updateProfile(userId, req));
    }
}
