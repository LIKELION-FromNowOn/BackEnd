package com.youin.now.auth;

import com.youin.now.common.response.ApiResponse;
import com.youin.now.common.security.CurrentUser;
import org.springframework.web.bind.annotation.DeleteMapping;
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

    /**
     * 비밀번호 변경. <b>지금 비밀번호를 다시 받습니다.</b>
     *
     * <p>틀리면 {@code 401 INVALID_CREDENTIALS}, 게스트면 {@code 403 GUEST_FORBIDDEN} 입니다.
     */
    @PatchMapping("/password")
    public ApiResponse<AuthPasswordRes> changePassword(@CurrentUser String userId,
                                                       @RequestBody AuthPasswordReq req) {
        return ApiResponse.ok(authService.changePassword(userId, req));
    }

    /**
     * 회원 탈퇴. <b>행을 지우지 않고 {@code deleted_at} 만 찍습니다.</b>
     *
     * <p>되돌릴 수 없어 비밀번호를 다시 받습니다. 화면은 성공하면 세션을 지우십시오.
     */
    @DeleteMapping
    public ApiResponse<AuthWithdrawRes> withdraw(@CurrentUser String userId,
                                                 @RequestBody AuthWithdrawReq req) {
        return ApiResponse.ok(authService.withdraw(userId, req));
    }
}
