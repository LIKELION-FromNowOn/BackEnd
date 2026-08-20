package com.youin.now.subtract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 판정 근거 프롬프트에 <b>보내는 값이 맞는지</b> 확인합니다.
 *
 * <p><b>왜 이 검사가 따로 필요한가</b> — 판정 API 는 항목이 0개라
 * {@code 400 MIN_ITEMS_REQUIRED} 로 끝납니다({@code ItemPort} 가 아직 스텁).
 * 그래서 <b>실제로 호출해서는 이 자리를 못 밟습니다.</b>
 * LLM 호출 자체는 케어 코치로 실동작을 확인했으니, 남는 위험은 <b>「보내는 값」</b>뿐입니다.
 *
 * <pre>
 * javac -encoding UTF-8 -cp "jackson-*.jar" -d build/check ...
 * java -cp "build/check;jackson-*.jar" com.youin.now.subtract.SubtractReasonPayloadCheck
 * </pre>
 */
public final class SubtractReasonPayloadCheck {

    static int pass = 0, fail = 0;

    static void check(String name, boolean ok, String detail) {
        if (ok) { pass++; System.out.println("  PASS  " + name); }
        else    { fail++; System.out.println("  FAIL  " + name + "  → " + detail); }
    }

    public static void main(String[] args) throws Exception {

        // 마스터 값이 붙은 항목 셋. 실제 시드에서 가져온 값입니다
        List<SubtractItem> selected = List.of(
                new SubtractItem("cr1", "아침 보습 루틴", "care", 4.0, 2.0,
                        SubtractFloor.ESSENTIAL, false, false, null, false),
                new SubtractItem("mv1", "헬스장 가기", "move", 3.0, 1.6,
                        SubtractFloor.RECOMMENDED, false, true, SubtractFrequency.WEEKLY_3, false),
                new SubtractItem("lf2", "책상 정리 매일", "life", 1.0, 2.0,
                        SubtractFloor.OPTIONAL, true, false, null, false));

        Map<String, SubtractItem> byId = new HashMap<>();
        for (SubtractItem s : selected) byId.put(s.itemId(), s);

        List<SubtractResult> drafts = new ArrayList<>();
        drafts.add(new SubtractResult("cr1", "아침 보습 루틴", SubtractVerdict.KEEP,
                null, SubtractFloor.ESSENTIAL, null, null, null, false, false, 5.1));
        drafts.add(new SubtractResult("mv1", "헬스장 가기", SubtractVerdict.SIMPLIFY,
                null, SubtractFloor.RECOMMENDED, null, null, null, false, false, 1.4));
        drafts.add(new SubtractResult("lf2", "책상 정리 매일", SubtractVerdict.SKIP,
                null, SubtractFloor.OPTIONAL, null, null, null, false, false, -0.9));

        String json = SubtractReasonPayload.of(byId, drafts, SubtractCondition.LOW);
        JsonNode n = new ObjectMapper().readTree(json);
        System.out.println("== 보내는 값 ==");
        System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter()
                .writeValueAsString(n));
        System.out.println();

        // ── 프롬프트가 요구하는 것 (docs/prompts/01-subtract-reason.md) ──
        check("state", "low".equals(n.path("state").asText()), n.path("state").asText());
        check("capacity", n.path("capacity").asInt() == 48, n.path("capacity").asText());
        check("judgeStrength", "high".equals(n.path("judgeStrength").asText()),
                n.path("judgeStrength").asText());

        JsonNode items = n.path("items");
        check("items 개수가 draft 와 같음", items.size() == drafts.size(),
                items.size() + " vs " + drafts.size());

        for (int i = 0; i < drafts.size(); i++) {
            JsonNode it = items.get(i);
            String id = drafts.get(i).itemId();
            check("[" + id + "] 순서 유지", id.equals(it.path("itemId").asText()),
                    it.path("itemId").asText());
            for (String f : List.of("name", "category", "floor", "score", "verdict", "load")) {
                check("[" + id + "] " + f, it.has(f), "없습니다");
            }
        }

        // 빈도를 받는 항목만 frequency 가 있어야 합니다
        check("mv1 에 frequency", items.get(1).has("frequency"), "없습니다");
        check("cr1 에는 frequency 없음", !items.get(0).has("frequency"), "있으면 안 됩니다");

        // 근거 등급 none 은 keep 고정 대상이라 그대로 실려야 합니다
        check("lf2 evidenceLevel none",
                "none".equals(items.get(2).path("evidenceLevel").asText()),
                items.get(2).path("evidenceLevel").asText());

        // ★ 이미 정해진 판정이 그대로 실리는지
        check("cr1 verdict keep", "keep".equals(items.get(0).path("verdict").asText()), "");
        check("mv1 verdict simplify", "simplify".equals(items.get(1).path("verdict").asText()), "");
        check("lf2 verdict skip", "skip".equals(items.get(2).path("verdict").asText()), "");

        // load = base + FQL[frequency]
        check("mv1 load = 1.6 + 1.9", Math.abs(items.get(1).path("load").asDouble() - 3.5) < 0.001,
                items.get(1).path("load").asText());
        check("cr1 load = base 그대로", Math.abs(items.get(0).path("load").asDouble() - 2.0) < 0.001,
                items.get(0).path("load").asText());

        // 보내면 안 되는 것 — 사용자가 적은 원문이 섞이면 안 됩니다
        check("reason 을 안 보냄", !items.get(0).has("reason"), "보내고 있습니다");

        System.out.println();
        System.out.printf("통과 %d · 실패 %d%n", pass, fail);
        if (fail > 0) System.exit(1);
    }
}
