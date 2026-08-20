package com.youin.now.safety;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 위기 신호 검사 — <b>규칙 기반입니다. AI 를 쓰지 않습니다.</b>
 *
 * <p>자유 입력 다섯 경로가 전부 이것을 거칩니다.
 * <b>LLM 에 보내기 전, 저장하기 전에</b> 부르십시오. 프롬프트로 막으려 하지 마십시오.
 *
 * <p>키워드는 {@link SafetyKeywords} 에 있습니다. <b>2026-08-20 김민정 님 확정본(24개)입니다.</b> 초안 11개에서 늘었고 뺀 것은 없습니다.
 */
@Service
public class SafetyService implements SafetyPort {

    private final List<String> keywords;

    public SafetyService() { this(SafetyKeywords.CONFIRMED_24); }

    public SafetyService(List<String> keywords) { this.keywords = List.copyOf(keywords); }

    @Override
    public SafetyResult check(String text, Source source) {
        if (text == null || text.isBlank()) {
            return new SafetyResult(false, null, List.of());
        }
        // 띄어쓰기를 지우고 소문자로 — 「죽고 싶」과 「죽고싶」을 같게 봅니다
        String normalized = text.replaceAll("\\s", "").toLowerCase();

        List<String> hits = new ArrayList<>();
        for (String k : keywords) {
            String nk = k.replaceAll("\\s", "").toLowerCase();
            if (!nk.isEmpty() && normalized.contains(nk) && !hits.contains(nk)) hits.add(k);
        }
        if (hits.isEmpty()) return new SafetyResult(false, null, List.of());

        // 걸리면 저장도, LLM 호출도 하지 않습니다. hits 는 로그·검수용이며 화면에 띄우지 않습니다
        return new SafetyResult(true, SafetyKeywords.GUIDANCE, List.copyOf(hits));
    }
}
