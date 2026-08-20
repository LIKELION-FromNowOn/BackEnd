package com.youin.now.checkin;

/** {@code POST /state/transition} 요청. */
public record CheckinTransitionReq(String checkinId, Boolean accept) {}
