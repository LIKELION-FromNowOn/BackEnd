package com.youin.now.auth;

import com.youin.now.common.response.ApiResponse;
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
}
