package com.youin.now.subtract;

import com.youin.now.common.response.ApiResponse;
import com.youin.now.common.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
        return ApiResponse.ok(subtractService.evaluate(userId, req.checkinId()));
    }

    /** {@code NOW-SUB-002} 최근 판정 조회. 없으면 {@code 404 EVALUATION_NOT_FOUND} */
    @GetMapping("/latest")
    public ApiResponse<SubtractRes> latest(@CurrentUser String userId) {
        return ApiResponse.ok(subtractService.latest(userId));
    }

    /**
     * {@code NOW-SUB-003} 판정 되돌리기.
     *
     * <p><b>{@code excluded} 항목은 {@code 409 CANNOT_REVERT_EXCLUDED} 입니다.</b>
     * 화면에서는 되돌리기 버튼 자체를 띄우지 마십시오 — 응답의 {@code revertable} 을 보시면 됩니다.
     */
    @PostMapping("/{itemId}/revert")
    public ApiResponse<SubtractRes> revert(@CurrentUser String userId,
                                           @PathVariable String itemId) {
        return ApiResponse.ok(subtractService.revert(userId, itemId));
    }
}
