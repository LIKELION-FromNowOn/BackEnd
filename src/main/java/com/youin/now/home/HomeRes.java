package com.youin.now.home;

import com.youin.now.footstep.FootstepService;
import com.youin.now.note.NoteRulePort;
import com.youin.now.today.TodayService;

/**
 * {@code NOW-HOME-001} 홈 집계.
 *
 * <p><b>여덟 개입니다.</b> 프론트 목에 있는 {@code streak} 은 명세에 없습니다 —
 * 저장할 표도 없고 {@code NOW-LOG-001} 이 거부한 연속일과 성격이 같아 넣지 않습니다
 * ({@code .agent/REQUESTS.md} #59).
 *
 * <p><b>홈은 읽기 전용입니다.</b> 오늘의 행동을 여기서 새로 만들지 않습니다.
 *
 * <p>각 조각은 <b>해당 패키지가 만든 record 를 그대로 실어 보냅니다</b> —
 * {@code docs/04-ports.md} 의 「{@code HomeService} 는 모으기만」 규약입니다.
 */
public record HomeRes(
        String nextStep,
        String state,
        boolean recommendationPaused,
        NoteRulePort.CareContext care,
        Subtract subtract,
        TodayService.ForHome today,
        FootstepService.ForHome footstepCard,
        Unlock unlock
) {

    /**
     * 덜어내기 요약. <b>판정 전이면 {@code null}</b> 입니다.
     *
     * @param removedCount <b>{@code reduce + skip}</b> 입니다. {@code simplify} 는 뺍니다 —
     *                     방식만 바꾼 것이지 걷어낸 것이 아닙니다
     */
    public record Subtract(String evaluationId, Summary summary, int removedCount) { }

    public record Summary(int keep, int simplify, int reduce, int skip, int excluded) { }

    /**
     * 기록 탭 잠금. 기록한 날이 쌓여야 주간·월간 발견이 열립니다.
     *
     * @param recordedDays {@code /logs/summary} 의 {@code daysRecorded} 와 같은 값입니다
     */
    public record Unlock(int recordedDays, boolean weeklyOpen,
                         boolean monthlyOpen, int monthlyNeed) { }
}