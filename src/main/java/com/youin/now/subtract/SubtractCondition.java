package com.youin.now.subtract;

/**
 * 오늘의 상태 5단계.
 *
 * <p><b>{@link #UNKNOWN} 을 빼지 마십시오.</b> 정도의 눈금이 아니라 「답을 안 하겠다」는 선택지이고,
 * 여력값 60 이 따로 있습니다. 「모르겠다」도 판정을 막지 않습니다.
 *
 * <p><b>{@link #DRAINED} 는 추천을 전면 중단합니다.</b> 고른 항목은 사라지지 않고 내일 이어집니다.
 */
public enum SubtractCondition {

    ENERGETIC("energetic", "아주 좋아요", 88, 0.0,  "low"),
    NORMAL   ("normal",    "괜찮아요",   70, 1.5,  "medium"),
    LOW      ("low",       "좀 처져요",   48, 2.5,  "high"),
    DRAINED  ("drained",   "많이 지쳤어요", 26, 4.0, "max"),
    UNKNOWN  ("unknown",   "잘 모르겠어요", 60, 1.5, "medium");

    private final String code;
    private final String label;
    private final int capacity;
    private final double strict;
    private final String judgeStrength;

    SubtractCondition(String code, String label, int capacity, double strict, String judgeStrength) {
        this.code = code;
        this.label = label;
        this.capacity = capacity;
        this.strict = strict;
        this.judgeStrength = judgeStrength;
    }

    public String code()          { return code; }
    public String label()         { return label; }
    public int capacity()         { return capacity; }
    public double strict()        { return strict; }
    public String judgeStrength() { return judgeStrength; }

    /** {@code drained} 면 추천을 만들지 않습니다. */
    public boolean recommendationPaused() { return this == DRAINED; }

    public static SubtractCondition of(String code) {
        for (SubtractCondition c : values()) if (c.code.equals(code)) return c;
        throw new IllegalArgumentException("알 수 없는 상태: " + code);
    }
}
