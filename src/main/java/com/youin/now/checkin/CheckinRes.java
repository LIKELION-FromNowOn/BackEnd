package com.youin.now.checkin;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * {@code POST /checkins} 응답. 명세서 {@code NOW-STATE-001} 그대로입니다.
 *
 * <p><b>{@code proposedState} 와 {@code reasons} 는 제안이 없으면 필드 자체가 빠집니다.</b>
 * 명세서가 「제안이 없으면 필드 없음」으로 적어 두었습니다. {@code null} 로 내보내지 않습니다.
 *
 * @param checkinId            <b>판정 API 에 넘길 ID</b>
 * @param signalScore          고른 징후의 가중치 합. 직접 적은 것은 각 2점
 * @param threshold            전환 제안 임계값. 현재 5
 * @param maxScore             전체 가중치 합. 징후 14개 기준 25
 * @param transitionProposed   {@code signalScore >= threshold} 일 때만 true
 * @param recommendationPaused {@code state} 가 {@code drained} 면 true
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CheckinRes(
        String checkinId,
        String state,
        int signalScore,
        int threshold,
        int maxScore,
        boolean transitionProposed,
        String proposedState,
        List<String> reasons,
        String message,
        boolean recommendationPaused,
        String judgeStrength) {
}
