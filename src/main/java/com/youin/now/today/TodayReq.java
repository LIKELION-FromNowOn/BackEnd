package com.youin.now.today;

import jakarta.validation.constraints.NotBlank;

/** {@code NOW-TODAY-002~005} 요청 본문. */
public final class TodayReq {

    private TodayReq() { }

    public record Reroll(@NotBlank String actionId) { }

    public record Start(@NotBlank String actionId) { }

    /** {@code timerId} 는 타이머 없이 완료하면 {@code null} 입니다 */
    public record Complete(@NotBlank String actionId, String timerId) { }

    /** {@code reason} — time · fit · none */
    public record Reject(@NotBlank String actionId, @NotBlank String reason) { }
}