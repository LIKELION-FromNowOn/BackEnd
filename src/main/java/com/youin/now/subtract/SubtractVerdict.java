package com.youin.now.subtract;

/**
 * 판정 값 5종. <b>2026-08-15 확정.</b>
 *
 * <p>프로토타입의 짧은 이름({@code simp} {@code red} {@code drop} {@code lock})은 내부용이며,
 * <b>서버와 API 는 처음부터 이 이름을 씁니다.</b>
 */
public enum SubtractVerdict {

    /** 그대로 — 오늘도 그대로 합니다. */
    KEEP("keep", 4),

    /** 방식만 — 양과 목표는 그대로 두고 실행·측정 방식만 낮춥니다. <b>「줄여라」가 아닙니다.</b> */
    SIMPLIFY("simplify", 3),

    /** 줄이기 — 양·횟수를 낮춥니다. */
    REDUCE("reduce", 2),

    /** 오늘은 쉬기 — 오늘은 하지 않습니다. */
    SKIP("skip", 1),

    /**
     * 판정 안 함 — 앱이 판단하지 않습니다.
     * <b>화면에서 숨기는 것이 아닙니다.</b> 「건드리지 않은 것」 영역에 사유와 함께 표시하고,
     * 되돌리기 버튼만 띄우지 않습니다.
     */
    EXCLUDED("excluded", 0);

    private final String code;
    /** 클수록 「덜 건드린」 판정. 하한선 비교에 씁니다. */
    private final int rank;

    SubtractVerdict(String code, int rank) {
        this.code = code;
        this.rank = rank;
    }

    public String code() { return code; }
    public int rank()    { return rank; }

    /** 이 판정이 {@code floor} 하한선보다 더 세게 깎았는가. */
    public boolean isBelow(SubtractFloor floor) {
        return this != EXCLUDED && this.rank < floor.minVerdict().rank;
    }

    public static SubtractVerdict of(String code) {
        for (SubtractVerdict v : values()) if (v.code.equals(code)) return v;
        throw new IllegalArgumentException("알 수 없는 판정 값: " + code);
    }

    /** 되돌리기 가능 여부. {@code EXCLUDED} 는 서버가 막습니다 ({@code CANNOT_REVERT_EXCLUDED}). */
    public boolean revertible() {
        return this == SIMPLIFY || this == REDUCE || this == SKIP;
    }
}
