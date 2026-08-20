package com.youin.now.home;

import com.youin.now.common.response.ApiResponse;
import com.youin.now.common.security.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code /home} — 홈 집계 {@code NOW-HOME-001}.
 *
 * <p>실제 경로에는 {@code context-path: /api/v1} 이 앞에 붙습니다.
 *
 * <p><b>홈은 읽기 전용입니다.</b> 오늘의 행동을 여기서 새로 만들지 않습니다.
 */
@RestController
@RequestMapping("/home")
public class HomeController {

    private final HomeService homeService;

    public HomeController(HomeService homeService) {
        this.homeService = homeService;
    }

    /** {@code nextStep} 하나로 다음 화면을 정합니다 */
    @GetMapping
    public ApiResponse<HomeRes> get(@CurrentUser String userId) {
        return ApiResponse.ok(homeService.get(userId));
    }
}