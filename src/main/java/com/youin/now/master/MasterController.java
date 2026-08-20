package com.youin.now.master;

import com.youin.now.common.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 마스터 데이터 조회 {@code NOW-MASTER-001~003}.
 *
 * <p>실제 경로는 {@code application.yml} 의 {@code context-path: /api/v1} 이 앞에 붙습니다.
 *
 * <p>세 API 의 경로가 서로 달라 클래스 레벨 {@code @RequestMapping} 을 두지 않았습니다.
 * {@code auth/} · {@code footstep/} 과 다른 점입니다.
 *
 * <p><b>권한은 게스트·회원 모두입니다.</b> 온보딩 화면이 로그인 전에 이 값을 씁니다.
 */
@RestController
public class MasterController {

    private final MasterService masterService;

    public MasterController(MasterService masterService) {
        this.masterService = masterService;
    }

    @GetMapping("/categories")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<MasterRes.Categories> getCategories() {
        return ApiResponse.ok(masterService.getCategories());
    }

    @GetMapping("/care-items")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<MasterRes.CareItems> getCareItems() {
        return ApiResponse.ok(masterService.getCareItems());
    }

    @GetMapping("/signals")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<MasterRes.Signals> getSignals() {
        return ApiResponse.ok(masterService.getSignals());
    }
}