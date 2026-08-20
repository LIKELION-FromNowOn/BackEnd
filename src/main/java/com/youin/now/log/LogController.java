package com.youin.now.log;

import com.youin.now.common.response.ApiResponse;
import com.youin.now.common.security.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code /logs} — 기록 {@code NOW-LOG-001 · 002}.
 *
 * <p>실제 경로에는 {@code context-path: /api/v1} 이 앞에 붙습니다.
 *
 * <p><b>{@code from} · {@code to} 를 {@code String} 으로 받습니다.</b>
 * {@code LocalDate} 로 받으면 {@code ?from=nope} 가 500 이 됩니다 —
 * {@code MethodArgumentTypeMismatchException} 을 전역 처리가 아직 안 잡습니다
 * ({@code .agent/REQUESTS.md} #51). 서비스에서 파싱하며 400 을 냅니다.
 */
@RestController
@RequestMapping("/logs")
public class LogController {

    private final LogService logService;

    public LogController(LogService logService) {
        this.logService = logService;
    }

    /**
     * {@code NOW-LOG-001} 완료한 행동. <b>평평한 배열</b>로 나갑니다.
     *
     * @param from       없으면 30일 전
     * @param to         없으면 오늘
     * @param categoryId 없으면 전부
     * @param limit      1~100, 기본 30
     */
    @GetMapping
    public ApiResponse<LogRes.Logs> getLogs(@CurrentUser String userId,
                                            @RequestParam(required = false) String from,
                                            @RequestParam(required = false) String to,
                                            @RequestParam(required = false) String categoryId,
                                            @RequestParam(required = false) Integer limit) {
        return ApiResponse.ok(logService.getLogs(userId, from, to, categoryId, limit));
    }

    /**
     * {@code NOW-LOG-002} 요약. <b>건수와 분포만</b> 냅니다.
     *
     * @param period {@code week} 또는 {@code month}. 없으면 {@code month}
     */
    @GetMapping("/summary")
    public ApiResponse<LogRes.Summary> getSummary(@CurrentUser String userId,
                                                  @RequestParam(required = false) String period) {
        return ApiResponse.ok(logService.getSummary(userId, period));
    }
}