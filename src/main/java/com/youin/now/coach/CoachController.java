package com.youin.now.coach;

import com.youin.now.common.error.ApiException;
import com.youin.now.common.error.ErrorCode;
import com.youin.now.common.response.ApiResponse;
import com.youin.now.common.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code NOW-COACH-001} 케어 코치 질의.
 *
 * <p><b>이 앱에서 가장 깊은 기능이고 가드레일이 가장 중요한 자리</b>입니다.
 * 「사우나 가도 되나요」에 <b>클리닉 안내문 원문을 근거로</b> 답합니다.
 * 못 찾으면 지어내지 않고 <b>원문을 확인해 달라</b>고 합니다.
 */
@RestController
@RequestMapping("/coach")
public class CoachController {

    private final CoachService coachService;

    public CoachController(CoachService coachService) {
        this.coachService = coachService;
    }

    @PostMapping("/ask")
    public ApiResponse<CoachRes> ask(@CurrentUser String userId,
                                     @Valid @RequestBody CoachAskReq req) {
        String question = req.ask();
        if (question == null) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "질문을 적어 주십시오");
        }
        return ApiResponse.ok(coachService.ask(userId, question));
    }
}
