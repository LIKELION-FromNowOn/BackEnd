package com.youin.now.checkin;

import com.youin.now.common.error.ApiException;
import com.youin.now.common.error.ErrorCode;
import com.youin.now.common.response.ApiResponse;
import com.youin.now.common.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code /checkins} — 오늘 상태 체크.
 *
 * <p>실제 경로에는 {@code context-path: /api/v1} 이 앞에 붙어
 * {@code POST /api/v1/checkins} 가 됩니다.
 *
 * <p><b>이 API 는 LLM 을 쓰지 않습니다.</b> 같은 입력에는 항상 같은 결과가 나와야
 * 사용자가 신뢰할 수 있기 때문입니다({@code NOW-STATE-001} 처리 규칙 5).
 */
@RestController
@RequestMapping("/checkins")
public class CheckinController {

    private final CheckinService checkinService;

    public CheckinController(CheckinService checkinService) {
        this.checkinService = checkinService;
    }

    /** {@code NOW-STATE-001} 상태 체크 제출. <b>하루 한 번이고, 다시 내면 덮어씁니다.</b> */
    @PostMapping
    public ApiResponse<CheckinRes> submit(@CurrentUser String userId,
                                          @Valid @RequestBody CheckinReq req) {
        return ApiResponse.ok(checkinService.submit(userId, req));
    }

    /**
     * {@code NOW-STATE-002} 최근 상태 조회. <b>판정 전에 반드시 있어야 합니다.</b>
     *
     * <p>없으면 {@code 409 NO_CHECKIN} 입니다. 판정 API 가 같은 코드를 씁니다.
     */
    @GetMapping("/latest")
    public ApiResponse<CheckinLatestRes> latest(@CurrentUser String userId) {
        return ApiResponse.ok(checkinService.latest(userId)
                .map(CheckinLatestRes::from)
                .orElseThrow(() -> new ApiException(ErrorCode.NO_CHECKIN)));
    }
}
