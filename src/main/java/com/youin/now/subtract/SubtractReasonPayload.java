package com.youin.now.subtract;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;

/**
 * 판정 근거 프롬프트에 <b>보내는 값</b>을 만듭니다.
 *
 * <p><b>일부러 순수하게 뒀습니다</b> — 스프링도 로거도 안 씁니다. 그래야
 * {@code SubtractReasonPayloadCheck} 가 Jackson 만으로 돌아갑니다.
 * <b>판정 API 는 항목이 0개라 실제로 못 밟는 자리</b>라, 이 검사가 유일한 확인 수단입니다.
 *
 * <p>모양은 {@code docs/prompts/01-subtract-reason.md} 의 「서버가 채워 넣는 입력」 그대로입니다.
 */
public final class SubtractReasonPayload {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SubtractReasonPayload() { }

    /**
     * @param byId   마스터 값이 붙은 원본. {@code category} · {@code load} 가 여기 있습니다
     * @param drafts 판정이 끝난 것. <b>이 순서와 개수를 그대로 보냅니다</b>
     */
    public static String of(Map<String, SubtractItem> byId,
                            List<SubtractResult> drafts,
                            SubtractCondition condition) {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("state", condition.code());
        root.put("capacity", condition.capacity());
        root.put("judgeStrength", condition.judgeStrength());

        ArrayNode items = root.putArray("items");
        for (SubtractResult d : drafts) {
            SubtractItem s = byId.get(d.itemId());
            ObjectNode n = items.addObject();
            n.put("itemId", d.itemId());
            n.put("name", d.name());
            if (s != null) {
                n.put("category", s.category());
                if (s.frequency() != null) n.put("frequency", s.frequency().code());
                n.put("evidenceLevel", s.evidenceNone() ? "none" : "high");
                n.put("load", s.load());
            }
            n.put("floor", d.floor().code());
            n.put("score", d.score());
            // ★ 이미 정해진 값입니다. 바꾸라고 하지 않습니다
            n.put("verdict", d.verdict().code());
        }
        // 사용자가 적은 원문(reason)은 안 보냅니다. 아직 비어 있기도 하고,
        // 있어도 LLM 에 되돌려 줄 이유가 없습니다
        return root.toString();
    }
}
