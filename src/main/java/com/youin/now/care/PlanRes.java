package com.youin.now.care;

import java.util.List;

/**
 * {@code NOW-NOTE-004 · 005} 응답.
 *
 * <p>필드명은 프론트 목({@code src/api/care.js}) 기준입니다.
 */
public final class PlanRes {

    private PlanRes() { }

    public record Plans(List<Item> plans) { }

    /**
     * @param conflict 안내문 규칙에 걸리는지
     * @param freeFrom 언제부터 괜찮은지. <b>여러 규칙에 걸리면 가장 늦게 풀리는 것</b>입니다 —
     *                 먼저 풀리는 것을 보여 주면 틀린 안내가 됩니다
     * @param sent     걸린 규칙의 문장 번호. 안 걸리면 {@code null}
     */
    public record Item(
            String planId,
            String date,
            String title,
            boolean conflict,
            String freeFrom,
            Integer sent
    ) { }

    /** {@code POST /me/plans}. <b>{@code sent} 가 없습니다</b> */
    public record Created(
            String planId,
            String date,
            String title,
            boolean conflict,
            String freeFrom
    ) { }
}