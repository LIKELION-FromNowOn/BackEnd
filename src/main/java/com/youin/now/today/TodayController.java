package com.youin.now.today;

import com.youin.now.common.response.ApiResponse;
import com.youin.now.common.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code /today} — 오늘의 행동 {@code NOW-TODAY-001~005}.
 *
 * <p>실제 경로에는 {@code context-path: /api/v1} 이 앞에 붙습니다.
 *
 * <p><b>후보 선정과 순위는 코드가 하고 AI 는 문장만 만듭니다.</b>
 */
@RestController
@RequestMapping("/today")
public class TodayController {

    private final TodayService todayService;

    public TodayController(TodayService todayService) {
        this.todayService = todayService;
    }

    /**
     * {@code NOW-TODAY-001} 오늘의 행동. <b>없으면 이 시점에 만듭니다.</b>
     *
     * <p>판정이 없으면 409 입니다. 후보가 없으면 {@code data} 가 {@code null} 이고
     * 화면은 첫 발자국 카드로 갑니다.
     */
    @GetMapping
    public ApiResponse<TodayRes.Action> getToday(@CurrentUser String userId) {
        return ApiResponse.ok(todayService.getOrCreate(userId));
    }

    /** {@code NOW-TODAY-002} 다시 받기. 직전 추천과 최근 문장은 후보에서 빠집니다 */
    @PostMapping("/reroll")
    public ApiResponse<TodayRes.Reroll> reroll(@CurrentUser String userId,
                                               @Valid @RequestBody TodayReq.Reroll req) {
        return ApiResponse.ok(todayService.reroll(userId, req.actionId()));
    }

    /** {@code NOW-TODAY-003} 타이머 시작. {@code durationSec} 은 서버가 정합니다 */
    @PostMapping("/start")
    public ApiResponse<TodayRes.Timer> start(@CurrentUser String userId,
                                             @Valid @RequestBody TodayReq.Start req) {
        return ApiResponse.ok(todayService.start(userId, req.actionId()));
    }

    /** {@code NOW-TODAY-004} 완료. <b>만료 전에도 완료할 수 있습니다</b> */
    @PostMapping("/complete")
    public ApiResponse<TodayRes.Complete> complete(@CurrentUser String userId,
                                                   @Valid @RequestBody TodayReq.Complete req) {
        return ApiResponse.ok(todayService.complete(userId, req.actionId(), req.timerId()));
    }

    /** {@code NOW-TODAY-005} 거절. <b>실패로 저장하지 않습니다</b> */
    @PostMapping("/reject")
    public ApiResponse<TodayRes.Reject> reject(@CurrentUser String userId,
                                               @Valid @RequestBody TodayReq.Reject req) {
        return ApiResponse.ok(todayService.reject(userId, req.actionId(), req.reason()));
    }
}