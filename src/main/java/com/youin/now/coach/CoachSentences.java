package com.youin.now.coach;

import java.util.List;

/**
 * 서버가 사전에 갖고 있는 문장들.
 *
 * <p><b>이 문장들은 폴백이 아니라 정답입니다</b>({@code docs/prompts/04-coach-answer.md}).
 * 「모른다」고 답하는 것은 실패가 아니고, <b>지어내는 것이 실패</b>입니다.
 *
 * <p>그래서 이 네 경우에는 <b>LLM 을 아예 부르지 않습니다.</b> 부르면 그럴듯한 답을
 * 만들어 내려 하고, 그 한 번이 사용자에게 갑니다.
 */
public final class CoachSentences {

    private CoachSentences() { }

    /** 안내문에 없는 것을 물었을 때 */
    public static final String NOT_IN_NOTE =
            "받으신 안내문에는 그 내용이 없습니다. 원문을 확인해 주시거나 클리닉에 문의해 주십시오.";

    /** 제한 기간이 이미 끝났을 때 */
    public static final String PERIOD_OVER =
            "안내받으신 제한 기간은 지났습니다. 다만 확실하지 않으시면 클리닉에 확인해 주십시오.";

    /** 통증 · 진물 · 부기 등 의학적 판단을 요구할 때 */
    public static final String MEDICAL =
            "그건 앱이 판단할 수 있는 부분이 아닙니다. 클리닉에 문의해 주십시오.";

    /** 제품 · 성분 추천을 요구할 때 */
    public static final String PRODUCT =
            "특정 제품을 권해 드리지는 않습니다. 오늘은 보습과 자외선 차단만 남기셔도 충분합니다.";

    /** {@code clinicNote} 가 근거일 때 폴백 문장 앞에 붙입니다 */
    public static final String CLINIC_FIRST = "클리닉에서 받으신 안내가 우선입니다. ";

    // ── LLM 이 실패했을 때 쓰는 폴백. level 별로 하나씩 ──
    public static String fallback(String level) {
        return switch (level == null ? "" : level) {
            case "no"   -> "오늘은 하지 않으시는 편이 낫습니다. 보습과 자외선 차단만 남기셔도 충분합니다.";
            case "soft" -> "하셔도 괜찮지만 오늘은 짧게만 하시는 편이 낫습니다.";
            case "ok"   -> "오늘은 하셔도 괜찮습니다.";
            default     -> NOT_IN_NOTE;
        };
    }

    /** 폴백일 때는 하나만 씁니다 */
    public static final List<String> FALLBACK_CHIPS = List.of("오늘은 뭘 하면 되나요");

    // ── 의도 분류용 ──
    // 규칙으로 봅니다. AI 에 먼저 넘기면 그 한 번이 이미 답을 만들어 버립니다

    /** 의학적 판단을 요구하는 말 */
    public static final List<String> MEDICAL_WORDS = List.of(
            "통증", "아파", "아픕", "진물", "고름", "부기", "부었", "붓고",
            "출혈", "피가", "염증", "감염", "물집", "흉터", "괴사");

    /** 제품 · 성분 추천을 요구하는 말 */
    public static final List<String> PRODUCT_WORDS = List.of(
            "추천", "어떤 제품", "무슨 제품", "브랜드", "제품 좀", "뭐 사", "뭘 사", "살까");

    /**
     * 응답에 있으면 안 되는 말.
     *
     * <p><b>브랜드명 목록은 없습니다.</b> 명세가 「브랜드명·제품명이 감지되면」이라고만 하고
     * 목록을 주지 않았습니다. 지어내지 않고 <b>의학적 단정만</b> 걸러 냅니다
     * ({@code .agent/REQUESTS.md} #39).
     */
    public static final List<String> BANNED = List.of(
            "치료됩니다", "치료돼", "낫습니다", "나아집니다", "효과가 있습니다",
            "효과적입니다", "에 좋습니다", "권장합니다", "처방");
}
