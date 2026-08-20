package com.youin.now.checkin;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * {@code checkin/} 이 남에게 열어 주는 창구. <b>시그니처는 {@code docs/04-ports.md} 그대로입니다.</b>
 *
 * <p><b>바꾸지 마십시오.</b> 남이 이 모양에 맞춰 코드를 씁니다.
 *
 * <table>
 *   <tr><th>누가 쓰나</th><th>무엇에</th></tr>
 *   <tr><td>{@code home/}</td><td>홈 집계의 「내 상태」 조각</td></tr>
 *   <tr><td>{@code today/}</td><td>오늘의 행동 생성 — 상태에 따라 강도가 달라집니다</td></tr>
 *   <tr><td>{@code subtract/}</td><td>판정 — 상태 체크가 없으면 {@code NO_CHECKIN} 409</td></tr>
 * </table>
 *
 * <p><b>소유가 8/17 에 이철희 → 송원석으로 옮겨졌습니다.</b>
 * {@code docs/04-ports.md} 의 「이철희 제공」 주석은 낡은 표기입니다 ({@code REQUESTS.md} #19).
 *
 * <p><b>지금은 스텁만 있습니다.</b> {@link CheckinPortStub} 이 항상 빈 값을 돌려줍니다 —
 * 부르는 쪽은 「아직 오늘 상태를 안 골랐다」로 받으면 됩니다. 정상적인 경로입니다.
 */
public interface CheckinPort {

    /**
     * 이 사람의 가장 최근 상태 체크.
     *
     * @return 오늘 상태를 아직 안 골랐으면 {@link Optional#empty()}.
     *         <b>예외를 던지지 않습니다.</b> 없는 것은 오류가 아니라 정상 상태입니다
     */
    Optional<LatestCheckin> latest(String userId);

    /**
     * 상태 체크 기록 요약. 날짜가 {@code null}이면 전체 기간을 집계합니다.
     */
    CheckinStats stats(String userId, LocalDate from, LocalDate to);

    /**
     * @param state          {@code energetic} · {@code normal} · {@code low} · {@code drained} · {@code unknown}
     *                       <b>{@code unknown} 은 정도의 눈금이 아니라 「답을 안 하겠다」는 선택지입니다.</b>
     * @param signalIds      고른 이상 징후 번호들. 마스터 {@code signals} 14개 중에서
     * @param signalStrength 고른 징후의 가중치 합. <b>5를 넘으면 상태 전환을 제안합니다</b> (합계 상한 25)
     * @param at             체크한 시각
     * @param recommendationPaused 오늘의 행동 추천을 중단했는지 여부
     */
    record LatestCheckin(String state, List<String> signalIds,
                         double signalStrength, LocalDateTime at,
                         boolean recommendationPaused) {}

    /**
     * @param daysRecorded 기록한 서로 다른 날짜 수
     * @param topState     가장 자주 기록한 상태. 기록이 없으면 {@code null}
     */
    record CheckinStats(int daysRecorded, String topState) {}
}
