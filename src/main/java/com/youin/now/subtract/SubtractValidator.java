package com.youin.now.subtract;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * ⑦단계 · 서버 측 검증 8가지.
 *
 * <p><b>프롬프트가 잘 돌아도 이것은 반드시 돕니다.</b> LLM 출력을 믿지 않는 것이 이 클래스의 존재 이유입니다.
 *
 * <p>순수 함수입니다. 위반을 고친 결과와, 무엇을 고쳤는지 기록을 함께 돌려줍니다.
 * 기록은 로그·검수용이며 화면에 띄우지 않습니다.
 */
public final class SubtractValidator {

    private SubtractValidator() {}

    /** 브랜드·제품명 감지에 쓰는 표시. 실제 목록은 {@code safety/} 가 관리합니다. */
    public static final Set<String> MEDICAL_CLAIMS = Set.of(
            "치료", "완치", "낫습니다", "낫는다", "효과가 있습니다",
            "좋습니다", "개선됩니다", "예방합니다", "회복됩니다");

    public record Report(List<SubtractResult> results, List<String> fixes) {
        public boolean clean() { return fixes.isEmpty(); }
    }

    /**
     * @param picked      사용자가 선택한 항목 ID 전체 (검사 8)
     * @param blockedByNote 클리닉 안내문에 걸린 항목 ID (검사 2)
     * @param evidenceNone  근거 등급이 {@code none} 인 항목 ID (검사 5)
     * @param fallback    문장이 버려졌을 때 대신 넣을 폴백 제공자
     */
    public static Report validate(List<SubtractResult> input,
                                  Set<String> picked,
                                  Set<String> blockedByNote,
                                  Set<String> evidenceNone,
                                  java.util.function.Function<SubtractVerdict, String> fallback) {

        List<SubtractResult> out = new ArrayList<>();
        List<String> fixes = new ArrayList<>();

        for (SubtractResult r : input) {

            // 8. 선택하지 않은 항목이 결과에 있는가 → 제거
            if (!picked.contains(r.itemId())) {
                fixes.add("[8] 선택하지 않은 항목 제거: " + r.itemId());
                continue;
            }

            SubtractResult cur = r;

            // 1. floor = -1 항목에 판정이 있는가 → 제거
            if (cur.floor() == SubtractFloor.EXCLUDED && cur.verdict() != SubtractVerdict.EXCLUDED) {
                fixes.add("[1] 판정 제외 항목에 판정이 붙어 제거: " + cur.itemId());
                continue;
            }

            // 2. 클리닉 주의사항에 걸린 항목에 판정이 있는가 → excluded 로
            if (blockedByNote.contains(cur.itemId()) && cur.verdict() != SubtractVerdict.EXCLUDED) {
                fixes.add("[2] 클리닉 제한 항목을 excluded 로: " + cur.itemId());
                cur = cur.withVerdict(SubtractVerdict.EXCLUDED, false);
            }

            // 3·4. 하한선 위반 → 하한선까지 되돌림
            if (cur.verdict().isBelow(cur.floor())) {
                SubtractVerdict min = cur.floor().minVerdict();
                fixes.add("[" + (cur.floor() == SubtractFloor.ESSENTIAL ? "3" : "4") + "] 하한선 위반 되돌림: "
                        + cur.itemId() + " " + cur.verdict().code() + " → " + min.code());
                cur = cur.withVerdict(min, true);
            }

            // 5. 근거 등급 0 인데 판정이 있는가 → keep 으로
            //    (④에서 이미 고정되지만, LLM 이나 상류 코드가 덮어썼을 수 있어 한 번 더 봅니다)
            if (evidenceNone.contains(cur.itemId())
                    && cur.verdict() != SubtractVerdict.KEEP
                    && cur.verdict() != SubtractVerdict.EXCLUDED) {
                fixes.add("[5] 근거 없는 항목을 keep 으로: " + cur.itemId());
                cur = cur.withVerdict(SubtractVerdict.KEEP, false);
            }

            // 5-1. 되돌린 항목도 같은 이유로 다시 확인합니다 (⑤ 재검)
            if (cur.reverted() && cur.verdict() != SubtractVerdict.KEEP
                    && cur.verdict() != SubtractVerdict.EXCLUDED) {
                fixes.add("[5-1] 되돌린 항목을 keep 으로: " + cur.itemId());
                cur = cur.withVerdict(SubtractVerdict.KEEP, false);
            }

            // 6·7. 근거 문장 검사 → 사전 문장으로 교체
            String reason = cur.reason();
            if (reason != null) {
                if (hasMedicalClaim(reason)) {
                    fixes.add("[7] 의학적 단정 감지, 폴백 문장으로 교체: " + cur.itemId());
                    cur = cur.withReason(fallback.apply(cur.verdict()));
                } else if (reason.isBlank()) {
                    fixes.add("[6] 빈 근거 문장, 폴백 문장으로 교체: " + cur.itemId());
                    cur = cur.withReason(fallback.apply(cur.verdict()));
                }
            } else {
                fixes.add("[6] 근거 문장 없음, 폴백 문장으로 교체: " + cur.itemId());
                cur = cur.withReason(fallback.apply(cur.verdict()));
            }

            out.add(cur);
        }
        return new Report(out, fixes);
    }

    public static boolean hasMedicalClaim(String text) {
        if (text == null) return false;
        for (String w : MEDICAL_CLAIMS) if (text.contains(w)) return true;
        return false;
    }
}
