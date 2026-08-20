package com.youin.now.subtract;

import com.youin.now.common.error.ErrorCode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import com.youin.now.common.response.ApiResponse;
import com.youin.now.common.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code /subtract} — 덜어내기 판정. <b>이 앱의 심장입니다.</b>
 *
 * <p>실제 경로에는 {@code context-path: /api/v1} 이 앞에 붙습니다.
 *
 * <p><b>판정은 코드가 하고 AI 는 문장만 만듭니다.</b> 그래서 같은 입력에는 항상 같은
 * 판정이 나옵니다. 근거 문장만 달라질 수 있습니다.
 */
@RestController
@RequestMapping("/subtract")
public class SubtractController {

    private final SubtractService subtractService;

    public SubtractController(SubtractService subtractService) {
        this.subtractService = subtractService;
    }

    /**
     * {@code NOW-SUB-001} 판정 실행.
     *
     * <p><b>상태 체크가 먼저입니다.</b> 없으면 {@code 409 NO_CHECKIN} 입니다.
     */
    @PostMapping("/evaluate")
    public ApiResponse<SubtractRes> evaluate(@CurrentUser String userId,
                                             @Valid @RequestBody SubtractEvaluateReq req) {
        try {
            return ApiResponse.ok(subtractService.evaluate(userId, req.checkinId()));
        } catch (DataIntegrityViolationException e) {
            // 같은 checkinId 로 판정이 동시에 두 번 들어온 경우입니다.
            //
            // evaluations 에 UNIQUE (checkin_id) 가 있고(schema_v63.sql:303),
            // SubtractService 는 「있으면 재사용, 없으면 생성」인데 그 검사와 INSERT 사이가
            // 원자적이지 않습니다. 둘 다 「없음」을 보고 각자 만들면 뒤에 온 쪽이 여기로 옵니다.
            //
            // 뒤에 온 쪽에서 다시 판정하지 않고 **먼저 만들어진 판정을 그대로 돌려줍니다.**
            // 「체크 하나에 판정 하나」라서 두 요청의 결과가 같아야 맞습니다.
            // 다시 부르면 AI 문장이 새로 나와 같은 화면에서 두 사람이 다른 근거를 보게 됩니다.
            //
            // 유니크 키 때문에 뒤에 온 INSERT 는 앞 트랜잭션이 끝날 때까지 기다렸다가 실패하므로,
            // 여기 도착한 시점에는 앞의 판정이 이미 커밋돼 있습니다.
            //
            // 2026-08-21 황인서 님 3차 회신에서 신고. 화면을 빠르게 두 번 누르거나
            // 탭을 두 개 열면 납니다. 개발 모드의 StrictMode 에서도 재현됩니다.
            return ApiResponse.ok(subtractService.result(userId, null, null));
        }
    }

    /**
     * {@code NOW-SUB-002} 판정 결과 조회. 없으면 {@code 404 EVALUATION_NOT_FOUND}
     *
     * <p><b>2026-08-20 경로를 {@code /latest} 에서 {@code /result} 로 고쳤습니다.</b>
     * 명세서와 프론트 목이 둘 다 {@code /subtract/result} 인데 서버만 달랐습니다.
     * 이대로 두면 프론트가 스위치를 켜는 순간 404 가 났습니다.
     *
     * @param evaluationId 없으면 가장 최근 판정
     * @param verdict      {@code ?verdict=reduce,skip} — 콤마로 여러 개
     */
    @GetMapping("/result")
    public ApiResponse<SubtractRes> result(@CurrentUser String userId,
                                           @RequestParam(required = false) String evaluationId,
                                           @RequestParam(required = false) String verdict) {
        return ApiResponse.ok(subtractService.result(userId, evaluationId, verdict));
    }

    /**
     * {@code NOW-SUB-003} 판정 되돌리기.
     *
     * <p><b>{@code excluded} 항목은 {@code 409 CANNOT_REVERT_EXCLUDED} 입니다.</b>
     * 화면에서는 되돌리기 버튼 자체를 띄우지 마십시오 — 응답의 {@code revertable} 을 보시면 됩니다.
     */
    /**
     * 덜어내기 이력. <b>기록 탭 H03 이 씁니다.</b>
     *
     * <p>명세서 36건에 없던 API 입니다 — 2026-08-20 에 더했습니다.
     * `.agent/REQUESTS.md` 에 노션 카드 신설을 올렸습니다.
     */
    @GetMapping("/history")
    public ApiResponse<SubtractHistoryRes> history(
            @CurrentUser String userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Integer limit) {
        return ApiResponse.ok(subtractService.history(userId, from, to, limit));
    }

    @PostMapping("/{itemId}/revert")
    public ApiResponse<SubtractRevertRes> revert(@CurrentUser String userId,
                                                 @PathVariable String itemId,
                                                 @Valid @RequestBody SubtractRevertReq req) {
        return ApiResponse.ok(subtractService.revert(userId, itemId, req.evaluationId()));
    }

    /**
     * {@code ?from=nope} 처럼 날짜가 깨져 들어오면 <b>500 이 나가고 있었습니다.</b>
     *
     * <p>{@code GlobalExceptionHandler} 가 {@code MethodArgumentTypeMismatchException} 을
     * 안 잡아 {@code Exception.class} 로 떨어집니다. 그건 {@code common/error/} 라
     * 이철희 님 소유여서 여기서만 막습니다. 전역 수정은 {@code .agent/REQUESTS.md} 에 올렸습니다 —
     * <b>{@code GET /logs?from=} 도 같은 자리에서 500 이 납니다.</b>
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> badParam(MethodArgumentTypeMismatchException e) {
        // 무엇이 틀렸는지 알려 줍니다. 「날짜는 YYYY-MM-DD」를 limit 에도 붙이면
        // 고치는 사람이 엉뚱한 곳을 봅니다
        String hint = e.getRequiredType() == java.time.LocalDate.class
                ? "날짜는 YYYY-MM-DD 입니다"
                : "숫자여야 합니다";
        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.status())
                .body(ApiResponse.fail(ErrorCode.VALIDATION_FAILED.name(),
                        "'" + e.getName() + "' 값이 올바르지 않습니다. " + hint));
    }
}
