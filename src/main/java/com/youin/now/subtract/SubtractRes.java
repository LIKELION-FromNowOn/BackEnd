package com.youin.now.subtract;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * {@code NOW-SUB-001} · {@code NOW-SUB-002} 응답.
 *
 * <p><b>필드 이름과 순서는 노션 API 명세서 그대로입니다.</b> 2026-08-20 에 대조해서
 * 어긋난 것을 고쳤습니다 — 배열 이름이 {@code items} 였는데 명세는 {@code results} 이고,
 * {@code name} · {@code evidenceLevel} · {@code createdAt} · {@code filter} 가 빠져 있었습니다.
 * <b>프론트 목 함수가 명세대로 쓰여 있어서, 고치지 않았으면 화면이 빈 목록으로 떴습니다.</b>
 *
 * <p>{@code null} 인 필드는 응답에서 빠집니다. 명세가 「제안이 없으면 필드 없음」으로
 * 적어 둔 자리들이 있어서입니다.
 *
 * @param filter 적용된 {@code verdict} 필터. <b>없으면 빈 배열</b>입니다 ({@code null} 아님)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SubtractRes(
        String evaluationId,
        String checkinId,
        String createdAt,
        String state,
        String judgeStrength,
        List<String> filter,
        String generatedBy,
        Summary summary,
        List<Item> results) {

    /**
     * 판정별 건수.
     *
     * <p><b>필터를 걸어도 전체 기준입니다.</b> 명세가 그렇게 정했습니다 —
     * 필터가 걸린 화면에서도 사용자가 오늘 전체 그림을 볼 수 있어야 하기 때문입니다.
     */
    public record Summary(int keep, int simplify, int reduce, int skip, int excluded) {}

    /**
     * @param revertable <b>명세에 없는 추가 필드입니다.</b> 되돌리기 버튼을 띄울지 판단하는 데 씁니다.
     *                   더 주는 것은 프론트를 깨뜨리지 않아 넣어 두었습니다
     */
    public record Item(String itemId, String name, String frequency,
                       String verdict, String reason,
                       String evidenceLevel, String floor,
                       boolean floorApplied, boolean reverted,
                       boolean revertable,
                       String excludedBy, Integer noteSent, Integer daysLeft) {}
}
