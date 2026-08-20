package com.youin.now.care;

import com.youin.now.common.response.ApiResponse;
import com.youin.now.common.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code /me/plans} — 예정 {@code NOW-NOTE-004 · 005 · 006}.
 *
 * <p>실제 경로에는 {@code context-path: /api/v1} 이 앞에 붙습니다.
 *
 * <p>{@code /me/care} 두 건은 송원석 님 몫이라 여기 없습니다.
 */
@RestController
@RequestMapping("/me/plans")
public class PlanController {

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    /** {@code NOW-NOTE-004} 예정 목록 + 안내문 규칙 충돌 여부 */
    @GetMapping
    public ApiResponse<PlanRes.Plans> getPlans(@CurrentUser String userId) {
        return ApiResponse.ok(planService.getPlans(userId));
    }

    /** {@code NOW-NOTE-005} 예정 추가. 저장 전에 위기 신호 검사를 통과합니다 */
    @PostMapping
    public ApiResponse<PlanRes.Created> addPlan(@CurrentUser String userId,
                                                @Valid @RequestBody PlanReq.Add req) {
        return ApiResponse.ok(planService.addPlan(userId, req));
    }

    /** {@code NOW-NOTE-006} 예정 삭제. <b>없는 id 도 성공입니다(멱등)</b> */
    @DeleteMapping("/{planId}")
    public ApiResponse<Void> deletePlan(@CurrentUser String userId,
                                        @PathVariable String planId) {
        planService.deletePlan(userId, planId);
        return ApiResponse.ok(null);
    }
}