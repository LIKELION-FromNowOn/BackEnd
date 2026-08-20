package com.youin.now.checkin;

import java.time.format.DateTimeFormatter;

/**
 * {@code NOW-STATE-002} 최근 상태 조회 응답.
 *
 * <p><b>2026-08-20 명세서와 대조해 네 자리를 고쳤습니다.</b>
 * {@code threshold} · {@code transitionProposed} · {@code recommendationPaused} 가 빠져 있었고,
 * 시각이 {@code checkDate}(날짜)로 나가고 있었는데 명세는 {@code createdAt}(ISO 8601 시각)입니다.
 *
 * <p><b>판정 전에 반드시 불러 {@code checkinId} 를 확보해야 합니다.</b>
 *
 * <p>프론트 목 함수는 여기에 {@code signalIds} 가 있다고 보고 있는데
 * <b>명세에는 없습니다.</b> 서버를 늘리지 않고 그대로 뒀습니다 —
 * 넣으려면 명세서를 먼저 고쳐야 합니다.
 */
public record CheckinLatestRes(String checkinId, String state, int signalScore,
                               int threshold, boolean transitionProposed,
                               boolean recommendationPaused,
                               String judgeStrength, String createdAt) {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    public static CheckinLatestRes from(Checkin c, int threshold) {
        return new CheckinLatestRes(
                c.id(), c.state(), c.signalScore(),
                threshold,
                // 저장된 값으로 다시 판단합니다. 제출 때와 같은 규칙이라 결과도 같습니다
                c.signalScore() >= threshold,
                // 명세서 처리 규칙 4번 — drained 면 추천을 중단합니다
                "drained".equals(c.state()),
                c.judgeStrength(),
                c.createdAt() == null ? null : ISO.format(c.createdAt()));
    }
}
