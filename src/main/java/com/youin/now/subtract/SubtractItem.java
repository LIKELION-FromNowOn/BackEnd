package com.youin.now.subtract;

/**
 * 판정 대상 항목 하나. <b>마스터 데이터 + 사용자가 고른 빈도</b>를 합친 것입니다.
 *
 * @param itemId            항목 ID
 * @param name              항목 이름
 * @param category          카테고리 코드
 * @param core              중요도 (마스터)
 * @param base              기본 부담 (마스터)
 * @param floor             하한선 등급 (마스터)
 * @param evidenceNone      근거 등급이 {@code none} 인가. <b>true 면 판정하지 않고 keep</b>
 * @param frequencyEditable 빈도를 받는 항목인가 (마스터)
 * @param frequency         사용자가 고른 빈도. {@code frequencyEditable} 이 false 면 무시됩니다
 * @param reverted          사용자가 되돌린 항목인가. <b>true 면 keep 고정</b>
 */
public record SubtractItem(
        String itemId,
        String name,
        String category,
        double core,
        double base,
        SubtractFloor floor,
        boolean evidenceNone,
        boolean frequencyEditable,
        SubtractFrequency frequency,
        boolean reverted
) {
    /**
     * {@code load = base + FQL[frequency]}
     *
     * <p>빈도를 받지 않는 항목은 {@code base} 만 씁니다.
     *
     * @throws IllegalStateException 빈도를 받는 항목인데 빈도가 비어 있을 때.
     *         <b>추측해서 채우지 않습니다.</b> 화면에서 반드시 받아야 하는 값입니다
     */
    public double load() {
        if (!frequencyEditable) return base;
        if (frequency == null) {
            throw new IllegalStateException(
                    "빈도가 없어 load 를 구할 수 없습니다: " + itemId +
                    " — 화면에서 빈도를 받아야 합니다 (기본값을 서버가 지어내지 않습니다)");
        }
        return base + frequency.weight();
    }
}
