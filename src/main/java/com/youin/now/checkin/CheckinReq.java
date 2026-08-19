package com.youin.now.checkin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * {@code POST /checkins} 요청. 명세서 {@code NOW-STATE-001} 그대로입니다.
 *
 * @param state         필수. {@code energetic} · {@code normal} · {@code low} · {@code drained} · {@code unknown}
 *                      <b>{@code unknown} 도 그대로 진행합니다.</b> 409 를 내지 않습니다
 * @param signalIds     선택. 마스터 징후 번호
 * @param customSignals 선택. <b>최대 5개.</b> 저장 전에 위기 신호 검사를 통과해야 합니다
 */
public record CheckinReq(
        @NotBlank(message = "상태 값이 올바르지 않습니다")
        String state,

        List<String> signalIds,

        @Size(max = 5, message = "직접 적은 징후는 최대 5개입니다")
        List<String> customSignals) {

    public List<String> signalIdsOrEmpty() {
        return signalIds == null ? List.of() : signalIds;
    }

    public List<String> customSignalsOrEmpty() {
        return customSignals == null ? List.of() : customSignals;
    }
}
