package com.youin.now.safety;

import com.youin.now.common.error.ApiException;
import com.youin.now.common.error.ErrorCode;
import com.youin.now.common.id.Ids;
import com.youin.now.common.response.ApiResponse;
import com.youin.now.common.security.CurrentUserArgumentResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code NOW-SAFE-001} 위기 신호 검사.
 *
 * <p><b>독립 엔드포인트이면서 동시에 사전 필터</b>입니다. 자유 입력을 받는 다섯 자리가
 * 전부 이것을 먼저 통과해야 합니다 — 직접 입력 항목 · 직접 입력 징후 · 코치 · 안내문 · 예정.
 *
 * <p><b>LLM 에 먼저 넘기지 않습니다.</b> 넘기면 부적절한 응답이 한 번은 생성됩니다.
 * 그 한 번이 사용자에게 갑니다.
 *
 * <p><b>비회원도 부를 수 있습니다.</b> 그래서 {@code @CurrentUser} 를 쓰지 않습니다 —
 * 그것을 쓰면 토큰이 없을 때 401 이 납니다. 토큰이 있으면 기록에 사용자를 같이 남기려고
 * 요청 속성에서 <b>있으면 꺼내고 없으면 넘어갑니다.</b>
 */
@RestController
@RequestMapping("/safety")
public class SafetyController {

    private final SafetyService safety;
    private final SafetyCheckRepository checks;

    public SafetyController(SafetyService safety, SafetyCheckRepository checks) {
        this.safety = safety;
        this.checks = checks;
    }

    @PostMapping("/check")
    public ApiResponse<SafetyCheckRes> check(HttpServletRequest request,
                                             @Valid @RequestBody SafetyCheckReq req) {

        SafetyPort.Source source = SafetyPort.Source.ofOrNull(req.source());
        if (source == null) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "입력 자리 값이 올바르지 않습니다");
        }

        SafetyPort.SafetyResult result = safety.check(req.text(), source);

        // 원문이 아니라 해시만 남깁니다. 같은 문장이 반복되는지만 보려는 것입니다
        checks.save(new SafetyCheck(
                Ids.of("sc"),
                (String) request.getAttribute(CurrentUserArgumentResolver.ATTR),   // 없으면 null
                source.code(),
                result.blocked(),
                result.hits().isEmpty() ? null : result.hits().get(0),
                sha256(req.text())));

        // hits 는 응답에 넣지 않습니다 — 어떤 말이 걸리는지 알려주면 피해 갈 수 있습니다
        return ApiResponse.ok(SafetyCheckRes.of(result.blocked(), result.message()));
    }

    private static String sha256(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 이 없습니다", e);   // 표준 JDK 에는 항상 있습니다
        }
    }
}
