package com.youin.now.subtract;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 판정 파이프라인 ①~⑧ · 서버 측 검증 8가지 <b>동작 확인</b>.
 *
 * <p>JUnit 없이 {@code main} 으로 돕니다 — 스프링 프로젝트가 없어도 돌려 볼 수 있어야 해서입니다.
 * <pre>
 * javac -d /tmp/out $(find src -name '*.java' | grep -v Stub) &amp;&amp; java -cp /tmp/out com.youin.now.subtract.SubtractPipelineCheck
 * </pre>
 * 프로젝트가 올라온 뒤에는 JUnit 으로 옮기십시오. <b>단정문은 그대로 쓸 수 있습니다.</b>
 */
public final class SubtractPipelineCheck {

    static int pass = 0, fail = 0;

    static void check(String name, boolean ok, String detail) {
        if (ok) { pass++; System.out.println("  PASS  " + name); }
        else    { fail++; System.out.println("  FAIL  " + name + "  → " + detail); }
    }

    // ── 표본 항목 ──────────────────────────────────────────
    static SubtractItem essential(String id, String nm, double core, double base, SubtractFrequency f) {
        return new SubtractItem(id, nm, "care", core, base, SubtractFloor.ESSENTIAL, false, true, f, false);
    }
    static SubtractItem optional(String id, String nm, double core, double base, SubtractFrequency f) {
        return new SubtractItem(id, nm, "life", core, base, SubtractFloor.OPTIONAL, false, true, f, false);
    }
    static SubtractItem recommended(String id, String nm, double core, double base, SubtractFrequency f) {
        return new SubtractItem(id, nm, "move", core, base, SubtractFloor.RECOMMENDED, false, true, f, false);
    }
    static SubtractItem medical(String id, String nm) {
        return new SubtractItem(id, nm, "med", 5, 1, SubtractFloor.EXCLUDED, false, false, null, false);
    }

    static SubtractResult find(List<SubtractResult> rs, String id) {
        for (SubtractResult r : rs) if (r.itemId().equals(id)) return r;
        return null;
    }

    public static void main(String[] args) {
        System.out.println("판정 엔진 동작 확인 — " + java.time.LocalDate.now());
        System.out.println("=".repeat(64));

        // ── 1. 부담·점수 계산 ────────────────────────────────
        System.out.println("\n[1] load · score 계산");
        SubtractItem water = optional("et_003", "물 2L 마시기", 4, 2, SubtractFrequency.DAILY);
        check("load = base + FQL[daily] = 2 + 3.6", water.load() == 5.6, "" + water.load());

        double sc = SubtractEngine.score(water, SubtractCondition.NORMAL);
        double expect = 4 * 1.7 - 5.6 * 0.75 - 1.5 * (5.6 * 0.35);
        check("score 공식이 명세서와 같다", Math.abs(sc - expect) < 1e-9, sc + " vs " + expect);

        SubtractItem light = optional("lt_001", "가벼운 항목", 4, 0.5, SubtractFrequency.WEEKLY_1);
        check("load ≤ 1.5 이면 무조건 keep",
                SubtractEngine.rawVerdict(light, SubtractCondition.DRAINED) == SubtractVerdict.KEEP,
                "load=" + light.load());

        // ── 2. 빈도가 없으면 계산하지 않는다 ──────────────────
        System.out.println("\n[2] 빈도 누락 — 추측하지 않고 막는다");
        SubtractItem noFreq = optional("nf_001", "빈도 없음", 4, 2, null);
        boolean threw = false;
        try { noFreq.load(); } catch (IllegalStateException e) { threw = true; }
        check("빈도가 비면 예외를 던진다 (기본값을 지어내지 않음)", threw, "예외가 안 났습니다");

        SubtractItem fixed = new SubtractItem("fx_001", "처방 안내", "care", 5, 1,
                SubtractFloor.RECOMMENDED, false, false, null, false);
        check("빈도를 안 받는 항목은 load = base", fixed.load() == 1.0, "" + fixed.load());

        // ── 3. 하한선 ────────────────────────────────────────
        System.out.println("\n[3] 하한선 — 아래로 내려가지 않는다");
        SubtractItem sunscreen = essential("cr2", "외출 전 자외선 차단", 5, 2, SubtractFrequency.DAILY);
        SubtractVerdict v = SubtractEngine.rawVerdict(sunscreen, SubtractCondition.DRAINED);
        check("생리적 필수는 소진 상태에서도 simplify 가 끝",
                v == SubtractVerdict.SIMPLIFY, v.code());

        boolean anyBelow = false;
        for (SubtractCondition c : SubtractCondition.values())
            for (double core = 1; core <= 5; core += 1)
                for (SubtractFrequency f : SubtractFrequency.values()) {
                    SubtractVerdict rv = SubtractEngine.rawVerdict(essential("x", "x", core, 2, f), c);
                    if (rv.isBelow(SubtractFloor.ESSENTIAL)) anyBelow = true;
                }
        check("모든 상태 × 중요도 × 빈도 조합에서 essential 이 하한 아래로 안 감", !anyBelow, "위반 조합 있음");

        // ── 4. 8단계 순서 ────────────────────────────────────
        System.out.println("\n[4] 처리 순서 8단계");
        List<SubtractItem> selected = new ArrayList<>(List.of(
                sunscreen,
                water,
                medical("md_001", "처방약 복용"),
                new SubtractItem("cr4", "고기능성 · 각질 관리", "care", 3, 2,
                        SubtractFloor.OPTIONAL, false, true, SubtractFrequency.WEEKLY_2, false),
                new SubtractItem("ev_000", "근거 없는 항목", "life", 1, 3,
                        SubtractFloor.OPTIONAL, true, true, SubtractFrequency.DAILY, false),
                new SubtractItem("rv_000", "되돌린 항목", "life", 1, 3,
                        SubtractFloor.OPTIONAL, false, true, SubtractFrequency.DAILY, true)));

        List<ClinicCaution> cautions = List.of(ClinicCaution.of("cr4", 2, 3, 2));

        SubtractPipeline.Outcome out =
                SubtractPipeline.run(selected, SubtractCondition.NORMAL, cautions, null);

        check("② floor=-1 은 excluded / excludedBy=floor",
                find(out.results(), "md_001").verdict() == SubtractVerdict.EXCLUDED
                && "floor".equals(find(out.results(), "md_001").excludedBy()),
                String.valueOf(find(out.results(), "md_001")));

        SubtractResult clinic = find(out.results(), "cr4");
        check("③ 클리닉 제한은 excluded / excludedBy=clinicNote / sent·daysLeft 동봉",
                clinic.verdict() == SubtractVerdict.EXCLUDED
                && "clinicNote".equals(clinic.excludedBy())
                && clinic.noteSent() == 2 && clinic.daysLeft() == 1,
                String.valueOf(clinic));

        check("④ 근거 없는 항목은 keep",
                find(out.results(), "ev_000").verdict() == SubtractVerdict.KEEP,
                find(out.results(), "ev_000").verdict().code());

        check("⑤ 되돌린 항목은 keep",
                find(out.results(), "rv_000").verdict() == SubtractVerdict.KEEP,
                find(out.results(), "rv_000").verdict().code());

        check("판정이 없는 항목이 하나도 없다 (전부 문장이 채워짐)",
                out.results().stream().allMatch(r -> r.reason() != null && !r.reason().isBlank()),
                "빈 문장 있음");

        check("요약 합계 = 결과 개수",
                out.summary().keep() + out.summary().simplify() + out.summary().reduce()
                + out.summary().skip() + out.summary().excluded() == out.results().size(),
                String.valueOf(out.summary()));

        // ── 5. 기간이 지난 클리닉 제한 ───────────────────────
        System.out.println("\n[5] 안내문 기간이 지나면 더는 막지 않는다");
        SubtractPipeline.Outcome expired = SubtractPipeline.run(
                selected, SubtractCondition.NORMAL,
                List.of(ClinicCaution.of("cr4", 2, 3, 5)), null);
        check("daysLeft = 0 이면 excluded 가 아니다",
                find(expired.results(), "cr4").verdict() != SubtractVerdict.EXCLUDED,
                find(expired.results(), "cr4").verdict().code());

        // ── 6. LLM 실패 → 폴백 ──────────────────────────────
        System.out.println("\n[6] LLM 이 죽어도 화면이 비지 않는다");
        SubtractPipeline.Outcome boom = SubtractPipeline.run(selected, SubtractCondition.LOW, cautions,
                (drafts, cond) -> { throw new RuntimeException("LLM timeout"); });
        check("예외가 나도 파이프라인이 결과를 돌려준다", boom.results().size() == selected.size(),
                boom.results().size() + " / " + selected.size());
        check("전부 폴백 문장으로 채워진다",
                boom.results().stream().allMatch(r -> r.reason() != null && !r.reason().isBlank()),
                "빈 문장 있음");
        check("llmUsed = false 로 표시된다", !boom.llmUsed(), "true 로 나옴");

        // ── 7. 서버 측 검증 8가지 ────────────────────────────
        System.out.println("\n[7] 서버 측 검증 8가지 — LLM 이 규칙을 어겼을 때");
        SubtractPipeline.ReasonGenerator naughty = (drafts, cond) -> {
            Map<String, String> m = new HashMap<>();
            for (SubtractResult r : drafts) m.put(r.itemId(), "이걸 그만두면 피부가 낫습니다.");
            return m;
        };
        SubtractPipeline.Outcome caught =
                SubtractPipeline.run(selected, SubtractCondition.NORMAL, cautions, naughty);
        check("[7] 의학적 단정이 폴백 문장으로 교체된다",
                caught.results().stream().noneMatch(r -> SubtractValidator.hasMedicalClaim(r.reason())),
                "단정 문장이 남았습니다");
        check("교체 기록이 남는다",
                caught.validationFixes().stream().anyMatch(s -> s.startsWith("[7]")),
                String.valueOf(caught.validationFixes()));

        List<SubtractResult> tampered = List.of(
                new SubtractResult("cr2", "자외선 차단", SubtractVerdict.SKIP, "오늘은 건너뛰세요.",
                        SubtractFloor.ESSENTIAL, null, null, null, false, false, -9),
                new SubtractResult("md_001", "처방약", SubtractVerdict.SKIP, "오늘은 안 드셔도 됩니다.",
                        SubtractFloor.EXCLUDED, null, null, null, false, false, -9),
                new SubtractResult("ghost", "고르지 않은 항목", SubtractVerdict.REDUCE, "줄이세요.",
                        SubtractFloor.OPTIONAL, null, null, null, false, false, -1));

        SubtractValidator.Report rep = SubtractValidator.validate(
                tampered, Set.of("cr2", "md_001"), Set.of(), Set.of(),
                SubtractPipeline::fallbackReason);

        check("[3] 생리적 필수의 skip 이 simplify 로 되돌려진다",
                find(rep.results(), "cr2").verdict() == SubtractVerdict.SIMPLIFY
                && find(rep.results(), "cr2").floorApplied(),
                String.valueOf(find(rep.results(), "cr2")));
        check("[1] 판정 제외 항목에 붙은 판정이 제거된다",
                find(rep.results(), "md_001") == null, "남아 있습니다");
        check("[8] 고르지 않은 항목이 제거된다",
                find(rep.results(), "ghost") == null, "남아 있습니다");

        SubtractValidator.Report ev = SubtractValidator.validate(
                List.of(new SubtractResult("ev_000", "근거 없음", SubtractVerdict.SKIP, "쉬세요.",
                        SubtractFloor.OPTIONAL, null, null, null, false, false, -9)),
                Set.of("ev_000"), Set.of(), Set.of("ev_000"),
                SubtractPipeline::fallbackReason);
        check("[5] 근거 등급 0 인데 판정이 붙으면 keep 으로",
                find(ev.results(), "ev_000").verdict() == SubtractVerdict.KEEP,
                find(ev.results(), "ev_000").verdict().code());

        // ── 8. 상태 5단계 ────────────────────────────────────
        System.out.println("\n[8] 상태 5단계 — 「모르겠다」가 판정을 막지 않는다");
        SubtractPipeline.Outcome unknown =
                SubtractPipeline.run(selected, SubtractCondition.UNKNOWN, cautions, null);
        check("unknown 으로도 판정이 정상 생성된다",
                unknown.results().size() == selected.size(), "" + unknown.results().size());
        check("unknown 의 strict 는 normal 과 같은 1.5",
                SubtractCondition.UNKNOWN.strict() == SubtractCondition.NORMAL.strict(), "다릅니다");
        check("unknown 의 여력값은 60 (normal 70 과 다름)",
                SubtractCondition.UNKNOWN.capacity() == 60, "" + SubtractCondition.UNKNOWN.capacity());
        check("drained 만 추천을 중단한다",
                SubtractCondition.DRAINED.recommendationPaused()
                && !SubtractCondition.UNKNOWN.recommendationPaused(), "중단 조건이 틀립니다");

        // ── 9. 결정성 ────────────────────────────────────────
        System.out.println("\n[9] 같은 입력이면 항상 같은 결과 (무작위 요소 없음)");
        boolean same = true;
        String first = String.valueOf(SubtractPipeline.run(selected, SubtractCondition.LOW, cautions, null).results());
        for (int i = 0; i < 50; i++)
            if (!first.equals(String.valueOf(
                    SubtractPipeline.run(selected, SubtractCondition.LOW, cautions, null).results()))) same = false;
        check("50회 반복 결과가 모두 같다", same, "결과가 흔들립니다");

        // ── 10. 되돌리기 가능 여부 ──────────────────────────
        System.out.println("\n[10] 되돌리기");
        check("excluded 는 되돌릴 수 없다", !SubtractVerdict.EXCLUDED.revertible(), "가능으로 나옴");
        check("keep 도 되돌릴 것이 없다", !SubtractVerdict.KEEP.revertible(), "가능으로 나옴");
        check("simplify · reduce · skip 은 되돌릴 수 있다",
                SubtractVerdict.SIMPLIFY.revertible() && SubtractVerdict.REDUCE.revertible()
                && SubtractVerdict.SKIP.revertible(), "불가로 나옴");

        System.out.println("\n" + "=".repeat(64));
        System.out.printf("통과 %d · 실패 %d%n", pass, fail);
        if (fail > 0) System.exit(1);
    }
}
