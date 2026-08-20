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

    /**
     * 자유 입력을 받는 자리 다섯. <b>DB 의 {@code safety_checks.source} 와 1:1 입니다.</b>
     *
     * <p>2026-08-20 전송값을 붙였습니다. 그전에는 열거형 이름밖에 없어
     * 화면이 {@code custom_item} 을 보내면 서버가 못 읽었습니다
     * ({@code schema_v63.sql:538} 이 「매핑을 정하고 맞춰야 합니다」로 남겨 둔 것).
     *
     * <p><b>{@code todo} 는 없습니다.</b> 「오늘의 행동」에 자유 입력이 없어
     * 대응하는 화면도 API 도 없습니다 ({@code .agent/REQUESTS.md} #11).
     */
    enum Source {
        ITEM_CUSTOM("custom_item"),
        SIGNAL_CUSTOM("custom_signal"),
        COACH("coach"),
        NOTE("care_note"),
        PLAN("plan");

        private final String code;

        Source(String code) { this.code = code; }

        /** DB 와 API 에 나가는 값. 열거형 이름이 아닙니다 */
        public String code() { return code; }

        /** @return 모르는 값이면 {@code null}. 부르는 쪽이 400 으로 만듭니다 */
        public static Source ofOrNull(String code) {
            if (code == null) return null;
            for (Source s : values()) if (s.code.equals(code)) return s;
            return null;
        }
    }
}
