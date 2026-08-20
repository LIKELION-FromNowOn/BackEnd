package com.youin.now.auth;

import com.youin.now.common.response.ApiResponse;
import com.youin.now.common.security.CurrentUser;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code /auth/*} — <b>인증이 필요 없는 유일한 자리</b>입니다.
 *
 * <p>실제 경로는 {@code application.yml} 의 {@code context-path: /api/v1} 이 앞에 붙어
 * {@code POST /api/v1/auth/guest} 가 됩니다.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * {@code NOW-AUTH-001} 게스트 세션 발급. 본문도 헤더도 필요 없습니다.
     *
     * <p><b>다른 API 를 테스트하려면 이것부터 부르셔야 합니다.</b>
     */
    @PostMapping("/guest")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<AuthGuestRes> guest() {
        return ApiResponse.ok(authService.issueGuest());
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuthSignupRes> signup(@RequestBody AuthSignupReq req) {
        return ApiResponse.ok(authService.signup(req));
    }

    @PostMapping("/login")
    public ApiResponse<AuthLoginRes> login(@RequestBody AuthLoginReq req) {
        return ApiResponse.ok(authService.login(req));
    }

    /** 토큰 무효화는 하지 않습니다. 인증 성공 뒤 클라이언트가 저장한 토큰을 지우면 됩니다. */
    @PostMapping("/logout")
    public ApiResponse<AuthLogoutRes> logout(@CurrentUser String userId) {
        return ApiResponse.ok(new AuthLogoutRes(true));
    }
}
