package com.youin.now.subtract;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 처리 순서 8단계 — <b>순서를 바꾸지 마십시오.</b>
 *
 * <pre>
 * ① 선택 항목만 남김            코드
 * ② floor = -1 제거             코드
 * ③ 클리닉 우선 필터            코드   ← 안내문에 걸린 항목 excluded
 * ④ 근거 등급 0 은 keep         코드
 * ⑤ 되돌린 항목은 keep          코드
 * ⑥ 판정 + 근거 생성            AI    ← 여기 하나뿐
 * ⑦ 하한선 위반 폐기·되돌림      코드
 * ⑧ 저장                        코드
 * </pre>
 *
 * <p><b>⑥ 앞을 건너뛰고 LLM 에 먼저 넘기면 「처방약을 오늘은 건너뛰세요」가 한 번은 생성됩니다.</b>
 *
 * <p>이 클래스는 DB 도 LLM 도 직접 부르지 않습니다. 근거 문장 생성은 {@link ReasonGenerator} 로 주입받아,
 * 서버가 없어도 파이프라인 전체를 돌려 볼 수 있습니다.
 */
public final class SubtractPipeline {

    /** ⑥단계. 실패하면 {@code null} 을 돌려주십시오 — 폴백은 파이프라인이 넣습니다. */
    @FunctionalInterface
    public interface ReasonGenerator {
        /** @return itemId → 근거 문장. 실패 시 {@code null} 또는 빈 맵 */
        Map<String, String> generate(List<SubtractResult> drafts, SubtractCondition condition);
    }

    /** ⑥이 실패했을 때 쓰는 사전 문장. {@code docs/prompts/01-subtract-reason.md} 와 같은 값입니다. */
    public static String fallbackReason(SubtractVerdict v) {
        return switch (v) {
            case KEEP     -> "오늘은 이대로 두어도 괜찮습니다.";
            case SIMPLIFY -> "그만두는 것이 아니라, 오늘은 방식만 간단히 합니다.";
            case REDUCE   -> "오늘은 평소보다 조금만 줄여 봅니다.";
            case SKIP     -> "오늘 하루 쉬어도 흐름은 끊기지 않습니다.";
            case EXCLUDED -> "이 항목은 앱이 판단하지 않습니다.";
        };
    }

    private static final String CLINIC_REASON = "클리닉에서 받으신 안내가 우선입니다. 앱이 바꾸지 않습니다.";

    public record Outcome(List<SubtractResult> results,
                          VerdictPort.Summary summary,
                          List<String> validationFixes,
                          boolean llmUsed) {}

    /**
     * @param selected   ① 사용자가 선택한 항목만 넘겨 주십시오
     * @param cautions   ③ 오늘 살아 있는 클리닉 제한
     * @param generator  ⑥ 근거 문장 생성기. {@code null} 이면 전부 폴백 문장을 씁니다
     */
    public static Outcome run(List<SubtractItem> selected,
                              SubtractCondition condition,
                              List<ClinicCaution> cautions,
                              ReasonGenerator generator) {

        Set<String> picked = new HashSet<>();
        Set<String> evidenceNone = new HashSet<>();
        for (SubtractItem it : selected) {
            picked.add(it.itemId());
            if (it.evidenceNone()) evidenceNone.add(it.itemId());
        }

        // ③ 준비 — 살아 있는 제한만
        Map<String, ClinicCaution> blocked = new HashMap<>();
        if (cautions != null) {
            for (ClinicCaution c : cautions) if (c.alive()) blocked.put(c.itemId(), c);
        }

        List<SubtractResult> drafts = new ArrayList<>();
        List<SubtractResult> needReason = new ArrayList<>();

        for (SubtractItem it : selected) {

            // ② floor = -1 — 판정 자체를 하지 않습니다
            if (it.floor() == SubtractFloor.EXCLUDED) {
                drafts.add(new SubtractResult(it.itemId(), it.name(), SubtractVerdict.EXCLUDED,
                        null, it.floor(), "floor", null, null, false, false, 0));
                continue;
            }

            // ③ 클리닉 우선 — 안내문이 앱의 어떤 제안보다 앞섭니다
            ClinicCaution c = blocked.get(it.itemId());
            if (c != null) {
                drafts.add(new SubtractResult(it.itemId(), it.name(), SubtractVerdict.EXCLUDED,
                        CLINIC_REASON, it.floor(), "clinicNote",
                        c.sentenceNo(), c.daysLeft(), false, false, 0));
                continue;
            }

            // ④ 근거 없음 — 판정하지 않고 keep
            if (it.evidenceNone()) {
                drafts.add(new SubtractResult(it.itemId(), it.name(), SubtractVerdict.KEEP,
                        null, it.floor(), null, null, null, false, false, 0));
                continue;
            }

            // ⑤ 되돌린 항목 — keep 고정, 다음 판정에서도 기억
            if (it.reverted()) {
                drafts.add(new SubtractResult(it.itemId(), it.name(), SubtractVerdict.KEEP,
                        null, it.floor(), null, null, null, false, true, 0));
                continue;
            }

            // 점수 계산 — 판정 값 자체는 코드가 정합니다
            double sc = SubtractEngine.score(it, condition);
            SubtractVerdict v = SubtractEngine.rawVerdict(it, condition);
            SubtractResult r = new SubtractResult(it.itemId(), it.name(), v,
                    null, it.floor(), null, null, null, false, false, sc);
            drafts.add(r);
            needReason.add(r);
        }

        // ⑥ 근거 문장 생성 — AI 는 여기 하나뿐. 판정은 이미 정해져 있습니다
        boolean llmUsed = false;
        Map<String, String> reasons = null;
        if (generator != null && !needReason.isEmpty()) {
            try {
                reasons = generator.generate(List.copyOf(needReason), condition);
                llmUsed = reasons != null && !reasons.isEmpty();
            } catch (RuntimeException e) {
                reasons = null;   // 폴백으로 내려갑니다. 빈 결과를 반환하지 않습니다
            }
        }
        List<SubtractResult> withReason = new ArrayList<>();
        for (SubtractResult r : drafts) {
            if (r.reason() != null) { withReason.add(r); continue; }
            String s = (reasons == null) ? null : reasons.get(r.itemId());
            withReason.add(r.withReason(
                    (s == null || s.isBlank()) ? fallbackReason(r.verdict()) : s));
        }

        // ⑦ 서버 측 검증 8가지
        SubtractValidator.Report report =
                SubtractValidator.validate(withReason, picked, blocked.keySet(), evidenceNone,
                        SubtractPipeline::fallbackReason);

        // ⑧ 저장은 호출자가 합니다 (이 클래스는 DB 를 모릅니다)
        return new Outcome(report.results(), summarize(report.results()),
                report.fixes(), llmUsed);
    }

    public static VerdictPort.Summary summarize(List<SubtractResult> results) {
        int keep = 0, simplify = 0, reduce = 0, skip = 0, excluded = 0;
        for (SubtractResult r : results) {
            switch (r.verdict()) {
                case KEEP     -> keep++;
                case SIMPLIFY -> simplify++;
                case REDUCE   -> reduce++;
                case SKIP     -> skip++;
                case EXCLUDED -> excluded++;
            }
        }
        return new VerdictPort.Summary(keep, simplify, reduce, skip, excluded);
    }

    private SubtractPipeline() {}
}
