package com.youin.now.subtract;

/**
 * 하한선 4등급 — <b>여기보다 아래로는 절대 내려가지 않습니다.</b>
 *
 * <p>마스터 데이터의 {@code floor} 문자열({@code essential} 등)과 프로토타입의 숫자(2/1/0/-1)를
 * 한 곳에서 묶습니다. 두 표현이 코드 여기저기 섞이면 반드시 갈라집니다.
 */
public enum SubtractFloor {

    /** 생리적 필수 — 보습 · 자외선 차단 · 수면 · 취침 시각 · 세 끼 · 아침 · 물 (7개) */
    ESSENTIAL("essential", 2, SubtractVerdict.SIMPLIFY),

    /** 권장 기반 */
    RECOMMENDED("recommended", 1, SubtractVerdict.REDUCE),

    /** 선택적 관리 */
    OPTIONAL("optional", 0, SubtractVerdict.SKIP),

    /** 판정 제외 — 클리닉 안내 · 처방약 · 정기 검진 (3개) */
    EXCLUDED("excluded", -1, SubtractVerdict.EXCLUDED);

    private final String code;
    private final int level;
    private final SubtractVerdict minVerdict;

    SubtractFloor(String code, int level, SubtractVerdict minVerdict) {
        this.code = code;
        this.level = level;
        this.minVerdict = minVerdict;
    }

    public String code()                { return code; }
    public int level()                  { return level; }
    /** 이 등급이 내려갈 수 있는 <b>가장 센</b> 판정. */
    public SubtractVerdict minVerdict()  { return minVerdict; }

    public static SubtractFloor of(String code) {
        for (SubtractFloor f : values()) if (f.code.equals(code)) return f;
        throw new IllegalArgumentException("알 수 없는 floor: " + code);
    }
}
