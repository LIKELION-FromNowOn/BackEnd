package com.youin.now.safety;

import java.util.List;

/**
 * 자유 입력 문장의 위기 신호 검사.
 *
 * <p><b>제공 — 송원석 · 사용 — 이철희(item) · 김민정(care, today)</b>
 *
 * <p>자유 입력 다섯 경로는 <b>LLM 에 보내기 전에</b> 이것을 먼저 통과해야 합니다.
 * {@code blocked} 가 true 면 LLM 을 호출하지 않고, 저장도 하지 않고, {@code message} 를 그대로 사용자에게 보여 줍니다.
 *
 * <p>시그니처 원본은 {@code docs/04-ports.md} 입니다. <b>여기서 바꾸지 마십시오.</b>
 */
public interface SafetyPort {

    /**
     * @param text   사용자가 입력한 원문
     * @param source 어느 화면에서 들어온 입력인지
     * @return 차단 여부와 안내 문구. <b>절대 null 을 반환하지 않습니다.</b>
     */
    SafetyResult check(String text, Source source);

    /**
     * @param blocked true 면 저장·LLM 호출을 모두 중단합니다
     * @param message 차단됐을 때 사용자에게 보여 줄 상담 안내. 통과면 null
     * @param hits    걸린 키워드. 로그·검수용이며 화면에 띄우지 않습니다
     */
    record SafetyResult(boolean blocked, String message, List<String> hits) {}

    enum Source { ITEM_CUSTOM, SIGNAL_CUSTOM, COACH, PLAN, NOTE }
}
