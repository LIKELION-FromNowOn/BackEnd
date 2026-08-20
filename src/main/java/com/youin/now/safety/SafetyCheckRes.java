package com.youin.now.safety;

/**
 * {@code NOW-SAFE-001} 응답. <b>필드 이름은 노션 명세서 그대로입니다.</b>
 *
 * @param flagged 위기 신호가 잡혔는지
 * @param action  {@code none} 또는 {@code pause_and_refer}
 * @param message 상담 창구 안내. 안 잡혔으면 {@code null}
 * @param stored  <b>항상 {@code false}</b> — 원문을 저장하지 않습니다.
 *                걸린 기록은 해시와 키워드만 남습니다
 * @param blocked <b>명세에 없는 추가 필드입니다.</b> 프론트 목이 이 이름을 읽고 있어서
 *                {@code flagged} 와 같은 값을 함께 줍니다. 프론트가 {@code flagged} 로
 *                옮기면 뺍니다. 더 주는 것은 화면을 깨뜨리지 않습니다
 */
public record SafetyCheckRes(boolean flagged, String action, String message,
                             boolean stored, boolean blocked) {

    static SafetyCheckRes of(boolean flagged, String message) {
        return new SafetyCheckRes(flagged,
                flagged ? "pause_and_refer" : "none",
                flagged ? message : null,
                false,
                flagged);
    }
}
