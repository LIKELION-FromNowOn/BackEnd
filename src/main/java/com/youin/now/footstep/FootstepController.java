package com.youin.now.footstep;

import com.youin.now.common.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * {@code NOW-STEP-001} 첫 발자국 사례 조회.
 *
 * <p>실제 경로는 {@code application.yml} 의 {@code context-path: /api/v1} 이 앞에 붙어
 * {@code GET /api/v1/footsteps} 가 됩니다.
 *
 * <p>사례가 8건뿐이라 목록에 상세까지 담습니다. 별도 상세 API 는 두지 않습니다. (8/11 확정)
 */
@RestController
@RequestMapping("/footsteps")
public class FootstepController {

    private final FootstepService footstepService;

    public FootstepController(FootstepService footstepService) {
        this.footstepService = footstepService;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<FootstepRes>> getFootsteps() {
        return ApiResponse.ok(footstepService.getFootsteps());
    }
}