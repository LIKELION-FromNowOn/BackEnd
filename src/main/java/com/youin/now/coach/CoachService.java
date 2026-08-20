package com.youin.now.coach;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.youin.now.common.llm.LlmClient;
import com.youin.now.note.NoteRulePort;
import com.youin.now.note.NoteService;
import com.youin.now.safety.SafetyPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * {@code NOW-COACH-001} 케어 코치.
 *
 * <p><b>판정은 규칙이 하고 AI 는 문장만 만듭니다.</b> 명세의 처리 순서가 그렇고,
 * 순서를 바꿀 수 없습니다.
 *
 * <pre>
 * ① 위기 신호 검사      코드   ← 걸리면 여기서 끝. AI 를 안 부릅니다
 * ② 의도 분류          규칙
 * ③ 오늘 조건 판정      코드   ← level 과 basis 가 여기서 정해집니다
 * ④ 문장 생성          AI
 * ⑤ 근거 부착 · 금칙어   코드
 * </pre>
 *
 * <p><b>①②에서 끝나는 경우에는 LLM 을 아예 안 부릅니다.</b> 부르면 그럴듯한 답을
 * 만들어 내려 하고, <b>그 한 번이 사용자에게 갑니다.</b>
 */
@Service
public class CoachService {

    private static final Logger log = LoggerFactory.getLogger(CoachService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 버리는 기준.
     *
     * <p>프롬프트는 <b>120자</b>로 적어 두었는데, 조금 넘겼다고 버리면 AI 를 켜 놓고
     * 매번 폴백을 쓰게 됩니다. <b>진짜 위험은 125자가 아니라 답이 폭주하는 것</b>이라
     * 버리는 선은 넉넉히 두고, 120자를 넘으면 로그만 남깁니다.
     */
    private static final int MAX_ANSWER = 220;

    private final SafetyPort safety;
    private final NoteService notes;
    private final LlmClient llm;
    private final String systemPrompt;

    public CoachService(SafetyPort safety, NoteService notes, LlmClient llm,
                        CoachPrompt prompt) {
        this.safety = safety;
        this.notes = notes;
        this.llm = llm;
        this.systemPrompt = prompt.system();
    }

    public CoachRes ask(String userId, String question) {

        // ① 위기 신호. 걸리면 여기서 끝냅니다
        SafetyPort.SafetyResult safe = safety.check(question, SafetyPort.Source.COACH);
        if (safe.blocked()) {
            return new CoachRes(safe.message(),
                    new CoachRes.Basis("safety", "안전 안내", null, null, null),
                    null, "rule", List.of(), List.of(), true);
        }

        // ② 의도 분류 — 규칙으로 봅니다
        String q = question.replaceAll("\s", "");
        if (containsAny(q, CoachSentences.MEDICAL_WORDS)) return canned(CoachSentences.MEDICAL);
        if (containsAny(q, CoachSentences.PRODUCT_WORDS)) return canned(CoachSentences.PRODUCT);

        // ③ 오늘 조건 판정 — 안내문 규칙에서 찾습니다
        Optional<NoteRulePort.NoteRule> alive = notes.activeRules(userId).stream()
                .filter(r -> containsAny(q, r.keywords()))
                .findFirst();

        if (alive.isEmpty()) {
            // 기간이 지난 규칙에라도 걸리면 「기간이 지났다」로 답합니다.
            // 그냥 「없다」고 하면 사용자는 안내를 못 받은 줄 압니다
            boolean expired = notes.latest(userId)
                    .map(n -> n.rules().stream().anyMatch(r -> containsAny(q, r.kw())))
                    .orElse(false);
            return canned(expired ? CoachSentences.PERIOD_OVER : CoachSentences.NOT_IN_NOTE);
        }

        NoteRulePort.NoteRule rule = alive.get();
        String sourceText = notes.latest(userId)
                .map(n -> n.lines().size() >= rule.sentenceNo()
                        ? n.lines().get(rule.sentenceNo() - 1) : null)
                .orElse(null);

        CoachRes.Basis basis = new CoachRes.Basis("clinicNote", "클리닉 안내",
                rule.sentenceNo(), rule.daysLeft(), sourceText);

        // ④ 문장 생성. 실패하면 폴백이고 앱은 안 죽습니다
        CoachAnswer out = llm.ask(systemPrompt, payload(question, "no", basis), CoachAnswer.class);

        // ⑤ 근거 부착 · 금칙어 검사
        if (out == null || !acceptable(out.answer())) {
            return new CoachRes(
                    CoachSentences.CLINIC_FIRST + CoachSentences.fallback("no"),
                    basis, "no", "rule", CoachSentences.FALLBACK_CHIPS,
                    List.of(rule.sentenceNo()), false);
        }
        return new CoachRes(out.answer(), basis, "no", "rule+llm",
                trimChips(out.chips()), List.of(rule.sentenceNo()), false);
    }

    /** 사전 문장으로 끝나는 경우. <b>{@code level} 을 비웁니다</b> — 판단을 한 것이 아닙니다 */
    private static CoachRes canned(String sentence) {
        return new CoachRes(sentence, new CoachRes.Basis("none", null, null, null, null),
                null, "rule", List.of(), List.of(), false);
    }

    /** 프롬프트가 정한 입력 모양 그대로 */
    private String payload(String question, String level, CoachRes.Basis basis) {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("question", question);
        root.put("level", level);
        ObjectNode b = root.putObject("basis");
        b.put("type", basis.type());
        b.put("label", basis.label());
        if (basis.sent() != null)       b.put("sent", basis.sent());
        if (basis.daysLeft() != null)   b.put("daysLeft", basis.daysLeft());
        if (basis.sourceText() != null) b.put("sourceText", basis.sourceText());
        return root.toString();
    }

    /**
     * 서버가 응답을 다시 봅니다. <b>LLM 이 근거를 넘어가면 갈아 끼웁니다.</b>
     *
     * <p>브랜드명 목록이 명세에 없어서 <b>의학적 단정만</b> 걸러 냅니다. 지어내지 않았습니다.
     */
    private static boolean acceptable(String answer) {
        // 왜 버렸는지 남깁니다. 안 남기면 「AI 를 켰는데 왜 rule 이지」에서 멈춥니다.
        // 본문은 안 찍습니다 — 사용자 질문이 섞여 있을 수 있습니다
        if (answer == null || answer.isBlank()) {
            log.warn("코치 답 버림 — 비어 있음");
            return false;
        }
        if (answer.length() > MAX_ANSWER) {
            log.warn("코치 답 버림 — {}자 (한도 {})", answer.length(), MAX_ANSWER);
            return false;
        }
        if (answer.contains("!") || answer.contains("！")) {
            log.warn("코치 답 버림 — 느낌표");
            return false;
        }
        if (containsAny(answer.replaceAll("\s", ""), CoachSentences.BANNED)) {
            log.warn("코치 답 버림 — 금칙어");
            return false;
        }
        if (answer.length() > 120) {
            // 프롬프트는 120자인데 모델이 조금 넘길 때가 있습니다. 버리진 않고 남겨만 둡니다
            log.info("코치 답이 120자를 넘었습니다 ({}자). 프롬프트를 조일 여지", answer.length());
        }
        return true;
    }

    /** 최대 2개 · 각 20자 */
    private static List<String> trimChips(List<String> chips) {
        if (chips == null) return List.of();
        List<String> out = new ArrayList<>();
        for (String c : chips) {
            if (c == null || c.isBlank() || c.length() > 20) continue;
            out.add(c);
            if (out.size() == 2) break;
        }
        return out;
    }

    private static boolean containsAny(String haystack, List<String> needles) {
        for (String n : needles) {
            if (n == null || n.isBlank()) continue;
            if (haystack.contains(n.replaceAll("\s", ""))) return true;
        }
        return false;
    }
}
