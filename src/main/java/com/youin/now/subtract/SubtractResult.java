package com.youin.now.subtract;

/**
 * 항목 하나의 판정 결과.
 *
 * @param excludedBy {@code EXCLUDED} 일 때만. {@code medical}(의료 영역) 또는 {@code clinicNote}(안내문 제한)
 * @param noteSent   {@code clinicNote} 일 때만. 안내문 원문 문장 번호
 * @param daysLeft   {@code clinicNote} 일 때만. 남은 제한 일수
 * @param reason     ⑥단계에서 LLM 이 만든 근거 문장. 실패하면 폴백 문장
 * @param floorApplied ⑦단계에서 하한선 때문에 되돌려졌는가
 */
public record SubtractResult(
        String itemId,
        String name,
        SubtractVerdict verdict,
        String reason,
        SubtractFloor floor,
        String excludedBy,
        Integer noteSent,
        Integer daysLeft,
        boolean floorApplied,
        boolean reverted,
        double score
) {
    public SubtractResult withVerdict(SubtractVerdict v, boolean floorApplied) {
        return new SubtractResult(itemId, name, v, reason, floor,
                excludedBy, noteSent, daysLeft, floorApplied, reverted, score);
    }

    public SubtractResult withReason(String r) {
        return new SubtractResult(itemId, name, verdict, r, floor,
                excludedBy, noteSent, daysLeft, floorApplied, reverted, score);
    }
}
