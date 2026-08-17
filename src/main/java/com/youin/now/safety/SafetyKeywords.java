package com.youin.now.safety;

import java.util.List;

/**
 * 위기 키워드 목록.
 *
 * <p><b>이 목록은 아직 확정본이 아닙니다.</b> 프로토타입의 11개를 그대로 옮긴 초안이며,
 * 김민정 님의 확정본으로 교체해야 합니다 (요구사항 「위기 키워드 목록 확정」).
 *
 * <p>명세서 요구 — <b>배포 없이 갱신할 수 있어야 합니다.</b>
 * 그래서 상수로 두지 않고 이 클래스 하나만 갈아 끼우거나,
 * 나중에 설정·DB 에서 읽도록 바꿀 수 있게 분리해 두었습니다.
 */
public final class SafetyKeywords {

    private SafetyKeywords() {}

    /** 프로토타입 {@code src/data/carenote.ts} 의 {@code CRISIS} 11개. <b>확정본 아님.</b> */
    public static final List<String> DRAFT_11 = List.of(
            "죽고싶", "죽고 싶", "자살", "자해",
            "사라지고싶", "사라지고 싶", "없어지고싶", "없어지고 싶",
            "살기싫", "살기 싫", "뛰어내리");

    /** 감지됐을 때 사용자에게 보여 줄 문구. <b>진단하지 않고, 판단하지 않습니다.</b> */
    public static final String GUIDANCE =
            "지금 많이 힘드신 것 같습니다. 이건 앱이 도와드릴 수 있는 부분이 아닙니다. "
            + "가까운 사람이나 전문 상담에 연결하시는 편이 낫습니다.";
}
