package com.youin.now.today;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * {@code NOW-TODAY-001~005} 응답.
 *
 * <p>필드명은 프론트 목({@code src/api/today.js}) 기준입니다.
 *
 * <p><b>{@code durationSec} 은 서버가 정합니다.</b> 15분 고정이 아니고 LLM 도 정하지 않습니다.
 * 화면에서 상수로 쓰지 마십시오.
 */
public final class TodayRes {

    private TodayRes() { }

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    /** {@code GET /today} */
    public record Action(
            String actionId,
            String categoryId,
            String categoryName,
            String title,
            int durationSec,
            String sourceItemId,
            String status,
            int rerollLeft,
            String generatedBy,
            String expiresAt
    ) { }

    /**
     * {@code POST /today/reroll}.
     *
     * <p><b>{@link Action} 과 필드가 다릅니다</b> — {@code sourceItemId} · {@code status} 가 없고
     * {@code rerollCount} 가 있습니다.
     */
    public record Reroll(
            String actionId,
            String categoryId,
            String categoryName,
            String title,
            int durationSec,
            int rerollLeft,
            int rerollCount,
            String generatedBy,
            String expiresAt
    ) { }

    /** {@code POST /today/start} */
    public record Timer(
            String timerId,
            String actionId,
            int durationSec,
            String startedAt,
            String endsAt,
            boolean blockScreen
    ) { }

    /** {@code POST /today/complete} */
    public record Complete(
            String logId,
            String actionId,
            String completedAt,
            String categoryId,
            boolean usedTimer,
            String message
    ) { }

    /** {@code POST /today/reject} */
    public record Reject(boolean accepted, String reason) { }

    static String iso(OffsetDateTime t) {
        return t == null ? null : ISO.format(t);
    }
}