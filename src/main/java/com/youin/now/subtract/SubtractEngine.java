package com.youin.now.subtract;

/**
 * 점수 계산 — <b>순수 함수입니다. DB 도 LLM 도 부르지 않습니다.</b>
 *
 * <pre>
 * load  = base + FQL[frequency]
 * score = core × 1.7 − load × 0.75 − strict × (load × 0.35)
 * load ≤ 1.5 → keep (이미 최소 단위)
 * </pre>
 *
 * <p><b>계수 1.7 · 0.75 · 0.35 와 임계값 0.8 · −2.5 는 임의 설계이며 검증 전입니다.</b>
 * 발표에서 「연구로 나온 값」이라고 말하면 안 됩니다. 말할 수 있는 것은 <b>설명 가능성</b>입니다 —
 * 같은 입력이면 항상 같은 결과가 나오고, 근거를 화면에서 그대로 보여 줍니다.
 *
 * <p>임계값은 프로토타입 {@code src/lib/engine.ts} 에서 그대로 옮겼습니다.
 * 그쪽은 스모크 테스트 543회를 통과한 코드라, 숫자를 바꾸면 그 검증이 무효가 됩니다.
 */
public final class SubtractEngine {

    private SubtractEngine() {}

    public static final double CORE_WEIGHT   = 1.7;
    public static final double LOAD_WEIGHT   = 0.75;
    public static final double STRICT_WEIGHT = 0.35;

    /** 이 부담 이하는 이미 최소 단위로 보고 손대지 않습니다. */
    public static final double MIN_LOAD      = 1.5;

    /** 이 점수 이상이면 그대로 둡니다. */
    public static final double KEEP_SCORE    = 0.8;
    /** 이 점수 이상이면 중간 강도, 아래면 최대 강도로 깎습니다. */
    public static final double MID_SCORE     = -2.5;

    public static double score(SubtractItem item, SubtractCondition condition) {
        double load = item.load();
        return item.core() * CORE_WEIGHT
             - load * LOAD_WEIGHT
             - condition.strict() * (load * STRICT_WEIGHT);
    }

    /**
     * 점수만으로 정한 판정. <b>하한선 보정 전 값입니다.</b>
     *
     * <p>①~⑤(선택·제외·클리닉·근거없음·되돌림)는 {@link SubtractPipeline} 이 먼저 걸러 냅니다.
     * 여기까지 온 항목은 「깎을 수 있는 항목」뿐입니다.
     */
    public static SubtractVerdict rawVerdict(SubtractItem item, SubtractCondition condition) {
        if (item.load() <= MIN_LOAD) return SubtractVerdict.KEEP;

        double sc = score(item, condition);
        if (sc >= KEEP_SCORE) return SubtractVerdict.KEEP;

        boolean mid = sc >= MID_SCORE;
        return switch (item.floor()) {
            // 생리적 필수 — 아무리 점수가 낮아도 「방식만」이 끝입니다
            case ESSENTIAL   -> SubtractVerdict.SIMPLIFY;
            case RECOMMENDED -> mid ? SubtractVerdict.SIMPLIFY : SubtractVerdict.REDUCE;
            case OPTIONAL    -> mid ? SubtractVerdict.REDUCE   : SubtractVerdict.SKIP;
            // floor = -1 은 여기까지 오지 않습니다 (②단계에서 제거)
            case EXCLUDED    -> SubtractVerdict.EXCLUDED;
        };
    }
}
