package com.youin.now.care;

import jakarta.validation.constraints.NotBlank;

/** {@code NOW-NOTE-005} 요청 본문. */
public final class PlanReq {

    private PlanReq() { }

    public record Add(@NotBlank String date, @NotBlank String title) { }
}