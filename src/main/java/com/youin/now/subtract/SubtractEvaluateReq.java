package com.youin.now.subtract;

import jakarta.validation.constraints.NotBlank;

/**
 * {@code POST /subtract/evaluate} 요청.
 *
 * <p><b>항목을 지정하지 않습니다.</b> {@code checkinId} 만 받고, 무엇을 판정할지는
 * 서버가 {@code ItemPort} 로 읽습니다. 그래서 「제외 항목 하나만 지정해 판정 요청」이라는
 * 상황이 없습니다 ({@code DISCREPANCIES-0819} 회신 7).
 */
public record SubtractEvaluateReq(
        @NotBlank(message = "checkinId 가 필요합니다")
        String checkinId) {
}
