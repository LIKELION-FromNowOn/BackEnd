package com.youin.now.master;

import com.youin.now.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 마스터 데이터 조회 {@code NOW-MASTER-001~003}.
 *
 * <p>실제 경로에는 {@code context-path: /api/v1} 이 앞에 붙습니다.
 *
 * <p>세 API 의 경로가 서로 달라 클래스 레벨 {@code @RequestMapping} 을 두지 않았습니다.
 *
 * <p><b>권한은 게스트·회원 모두입니다.</b> 온보딩 화면이 로그인 전에 이 값을 씁니다.
 */
@RestController
public class MasterController {

    private final MasterService masterService;

    public MasterController(MasterService masterService) {
        this.masterService = masterService;
    }

    /** {@code NOW-MASTER-001} 카테고리 7건 */
    @GetMapping("/categories")
    public ApiResponse<List<MasterRes.Category>> getCategories() {
        return ApiResponse.ok(masterService.getCategories());
    }

    /**
     * {@code NOW-MASTER-002} 관리 항목 32건.
     *
     * @param category 분류 필터. 없는 값이면 {@code 400 VALIDATION_FAILED}
     */
    @GetMapping("/care-items")
    public ApiResponse<List<MasterRes.CareItem>> getCareItems(
            @RequestParam(required = false) String category) {
        return ApiResponse.ok(masterService.getCareItems(category));
    }

    /** {@code NOW-MASTER-003} 이상 징후 14건 */
    @GetMapping("/signals")
    public ApiResponse<MasterRes.Signals> getSignals() {
        return ApiResponse.ok(masterService.getSignals());
    }
}