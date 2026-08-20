package com.youin.now.subtract;

import java.util.List;

/**
 * 덜어내기 이력. <b>기록 탭 H03 이 씁니다.</b>
 *
 * <p><b>연속 달성일과 달성률은 담지 않습니다</b> — 2026-08-20 송원석 님 결정.
 * {@code NOW-LOG-001} 이 「연속 달성일을 만들지 않습니다」 · 「하지 못한 날을 세지 않습니다」로
 * 정해 둔 것을 그대로 지킵니다. 두 값은 추후 개선사항으로 발표합니다.
 *
 * <p>필드 이름은 {@code NOW-LOG-001} 의 목록 응답과 같은 모양으로 맞췄습니다 —
 * 화면이 두 API 를 같은 방식으로 다룰 수 있게.
 *
 * @param history 판정 이력. <b>최근 것이 먼저</b>입니다
 * @param total   기간 안의 전체 판정 수. {@code limit} 과 무관합니다
 * @param hasMore 더 있으면 {@code true}
 */
public record SubtractHistoryRes(List<Entry> history, int total, boolean hasMore) {

    /**
     * @param date        판정한 날 (KST)
     * @param state       그날 컨디션
     * @param summary     판정별 건수. <b>비율이 아니라 개수</b>입니다
     * @param generatedBy {@code llm} 또는 {@code fallback}
     */
    public record Entry(String evaluationId,
                        String date,
                        String state,
                        SubtractRes.Summary summary,
                        String generatedBy) { }
}
