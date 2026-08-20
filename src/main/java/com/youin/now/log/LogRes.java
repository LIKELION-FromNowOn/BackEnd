package com.youin.now.log;

import java.util.List;

/**
 * {@code NOW-LOG-001 · 002} 응답.
 *
 * <p>필드명은 명세와 프론트 목({@code src/api/records.js}) 기준입니다.
 *
 * <p><b>{@code date} 는 {@code 2026-08-18} 형식입니다.</b> 목의 「8월 18일」은 화면 표기라
 * 프론트가 만듭니다.
 *
 * <p><b>달성률 · 연속일 · 하지 못한 날은 없습니다.</b> 명세의 「하지 않는 것」입니다 —
 * 비율을 만들면 못 한 날이 드러나고 끊기는 순간이 부담이 됩니다.
 */
public final class LogRes {

    private LogRes() { }

    // ── NOW-LOG-001 ────────────────────────────────

    public record Days(List<Day> days) { }

    public record Day(String date, List<Item> logs) { }

    /**
     * @param logId     {@code actions.id}. 명세가 「기록 ID」라고만 하고 어느 표인지 안 정했습니다
     * @param usedTimer {@code started_at IS NOT NULL} 파생
     */
    public record Item(
            String logId,
            String title,
            String categoryId,
            String categoryName,
            boolean usedTimer
    ) { }

    // ── NOW-LOG-002 ────────────────────────────────

    /**
     * @param period         {@code week} 7일 · {@code month} 30일. <b>기본은 {@code month}</b>
     * @param totalCount     완료한 행동 수. <b>{@code total} 이 아닙니다</b>
     * @param sentence       요약 문장. <b>한 것만 가지고 만듭니다</b> —
     *                       부족한 카테고리를 지적하지 않습니다 (명세 단서)
     * @param daysRecorded   기록한 날. <b>{@code CheckinPort} 대기 중이라 지금은 0</b>
     * @param daysSubtracted 덜어낸 날. <b>{@code VerdictPort.stats()} 대기 중이라 지금은 0</b>
     * @param topState       최빈 컨디션. <b>{@code CheckinPort} 대기 중이라 지금은 null</b>
     * @param topSubtracted  자주 덜어낸 항목. <b>{@code VerdictPort.stats()} 대기 중이라 지금은 빈 목록</b>
     */
    public record Summary(
            String period,
            int totalCount,
            List<CategoryCount> byCategory,
            String sentence,
            int daysRecorded,
            int daysSubtracted,
            String topState,
            List<ItemCount> topSubtracted
    ) { }

    public record CategoryCount(String categoryName, int count) { }

    public record ItemCount(String itemId, String name, int count) { }
}