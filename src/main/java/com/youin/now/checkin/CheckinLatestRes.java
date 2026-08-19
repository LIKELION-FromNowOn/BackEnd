package com.youin.now.checkin;

/**
 * {@code GET /checkins/latest} 응답 — {@code NOW-STATE-002}.
 *
 * @param checkDate KST 기준 날짜. <b>「오늘 것인지」는 클라이언트가 이 값으로 판단합니다</b>
 */
public record CheckinLatestRes(String checkinId, String state, int signalScore,
                               String judgeStrength, String checkDate) {

    public static CheckinLatestRes from(Checkin c) {
        return new CheckinLatestRes(c.id(), c.state(), c.signalScore(),
                c.judgeStrength(), c.checkDate().toString());
    }
}
