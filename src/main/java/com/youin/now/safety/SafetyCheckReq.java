package com.youin.now.safety;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * {@code NOW-SAFE-001} 요청.
 *
 * @param text   검사할 자유 입력. <b>서버는 이 원문을 저장하지 않습니다</b>
 * @param source 어느 자리에서 왔는지. {@code custom_item} · {@code custom_signal} ·
 *               {@code coach} · {@code care_note} · {@code plan}
 */
public record SafetyCheckReq(
        @NotBlank(message = "검사할 문장이 필요합니다")
        @Size(max = 2000, message = "문장이 너무 깁니다")
        String text,

        @NotBlank(message = "입력 자리를 알려 주십시오")
        String source) {
}
