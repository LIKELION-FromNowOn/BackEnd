package com.youin.now.subtract;

import jakarta.validation.constraints.NotBlank;

/**
 * {@code NOW-SUB-003} 되돌리기 요청 본문.
 *
 * <p><b>2026-08-20 신설.</b> 그전에는 본문을 아예 안 받았는데 명세서는 필수로 두고 있습니다.
 * 어느 판정을 되돌리는지 지정하지 않으면 <b>화면이 보고 있는 판정과 서버가 고르는 판정이
 * 어긋날 수 있습니다</b> — 하루에 두 번 판정하면 그렇게 됩니다.
 */
public record SubtractRevertReq(
        @NotBlank(message = "되돌릴 판정 ID가 필요합니다")
        String evaluationId) {
}
