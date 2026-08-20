package com.youin.now.checkin;

import com.fasterxml.jackson.annotation.JsonInclude;

/** {@code POST /state/transition} 응답. 수락과 거절에서 필요한 필드만 보냅니다. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CheckinTransitionRes(String state, boolean accepted, boolean recommendationPaused,
                                   Boolean needsRejudge, String nextProposalBlockedUntil) {}
