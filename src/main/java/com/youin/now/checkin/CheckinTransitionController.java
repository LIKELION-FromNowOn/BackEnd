package com.youin.now.checkin;

import com.youin.now.common.response.ApiResponse;
import com.youin.now.common.security.CurrentUser;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 상태 전환 응답은 체크인 목록이 아니라 상태 영역의 행동이므로 {@code /state} 경로를 씁니다. */
@RestController
@RequestMapping("/state")
public class CheckinTransitionController {
    private final CheckinService checkins;

    public CheckinTransitionController(CheckinService checkins) {
        this.checkins = checkins;
    }

    @PostMapping("/transition")
    public ApiResponse<CheckinTransitionRes> respond(@CurrentUser String userId,
                                                      @RequestBody CheckinTransitionReq req) {
        return ApiResponse.ok(checkins.respondTransition(userId, req));
    }
}
