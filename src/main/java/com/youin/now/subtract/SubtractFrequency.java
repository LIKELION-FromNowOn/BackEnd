package com.youin.now.subtract;

/**
 * 빈도 5종과 부담 가중치 (FQL).
 *
 * <p><b>빈도가 없으면 {@code load} 를 구할 수 없고, {@code load} 가 없으면 판정 점수가 안 나옵니다.</b>
 * 화면에서 반드시 받아야 하는 값입니다 (2026-08-15 시안 수정 요청 ①).
 *
 * <p>단, 마스터 데이터의 {@code frequencyEditable} 이 {@code false} 인 항목은 빈도를 받지 않으며
 * 그 경우 {@code load = base} 입니다. {@link SubtractItem} 을 보십시오.
 */
public enum SubtractFrequency {

    WEEKLY_1("weekly_1", "주 1회", 0.5),
    WEEKLY_2("weekly_2", "주 2회", 1.1),
    WEEKLY_3("weekly_3", "주 3회", 1.9),
    WEEKLY_4PLUS("weekly_4plus", "주 4회 이상", 2.9),
    DAILY("daily", "매일", 3.6);

    private final String code;
    private final String label;
    private final double weight;

    SubtractFrequency(String code, String label, double weight) {
        this.code = code;
        this.label = label;
        this.weight = weight;
    }

    public String code()   { return code; }
    public String label()  { return label; }
    public double weight() { return weight; }

    /** 알 수 없는 값이면 {@code null}. <b>가까운 값으로 옮기지 마십시오.</b> */
    public static SubtractFrequency ofOrNull(String code) {
        if (code == null) return null;
        for (SubtractFrequency f : values()) if (f.code.equals(code)) return f;
        return null;
    }
}
