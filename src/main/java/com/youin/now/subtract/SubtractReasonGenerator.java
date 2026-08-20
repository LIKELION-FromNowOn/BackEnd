package com.youin.now.subtract;

import com.youin.now.common.llm.LlmClient;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * 판정 ⑥단계 — <b>근거 문장만</b> 만듭니다.
 *
 * <p><b>판정은 이미 끝나 있습니다.</b> {@code verdict} · {@code load} · {@code score} 는
 * 전부 코드가 정한 값이고, LLM 은 <b>그것을 사람이 읽을 문장으로 바꾸기만</b> 합니다.
 * {@code docs/prompts/01-subtract-reason.md} 가 그렇게 정했습니다.
 *
 * <p><b>한 번에 부릅니다.</b> 항목이 8개를 넘어도 나눠 부르지 않습니다 —
 * 한 사람의 판정은 한 호출로 끝나야 합니다. 나누면 최악 대기가 배로 늘고,
 * 앞뒤 문장이 서로 어긋납니다.
 *
 * <h2>실패하면 전부 폴백입니다</h2>
 *
 * <p>{@code null} 을 돌려주면 파이프라인이 {@link SubtractPipeline#fallbackReason} 을 씁니다.
 * <b>{@code itemId} 가 어긋난 항목만</b> 골라 폴백으로 두고 나머지는 살립니다 —
 * 하나 틀렸다고 전체를 버리지 않습니다. 명세가 그렇게 적어 두었습니다.
 */
@Component
public class SubtractReasonGenerator {

    private static final Logger log = LoggerFactory.getLogger(SubtractReasonGenerator.class);

    /** 명세가 정한 한 문장 길이. 넘치면 그 항목만 폴백으로 둡니다 */
    private static final int MAX_REASON = 60;

    private final LlmClient llm;
    private final String systemPrompt;

    public SubtractReasonGenerator(LlmClient llm) {
        this.llm = llm;
        this.systemPrompt = read("prompts/subtract-reason.txt");
    }

    /**
     * @param selected 마스터 값이 붙은 원본. {@code category} · {@code load} 같은 것이 여기 있습니다
     * @param drafts   판정이 끝난 것. <b>이 순서와 개수를 그대로 보냅니다</b>
     * @return {@code itemId → 문장}. 실패하면 <b>빈 지도</b>이고 부르는 쪽이 폴백을 씁니다
     */
    public Map<String, String> generate(List<SubtractItem> selected,
                                        List<SubtractResult> drafts,
                                        SubtractCondition condition) {
        if (drafts == null || drafts.isEmpty()) return Map.of();

        Map<String, SubtractItem> byId = new HashMap<>();
        for (SubtractItem s : selected) byId.put(s.itemId(), s);

        SubtractReasons out =
                llm.ask(systemPrompt, SubtractReasonPayload.of(byId, drafts, condition),
                        SubtractReasons.class);

        if (out == null || out.reasons() == null) return Map.of();

        Map<String, String> ok = new HashMap<>();
        int dropped = 0;
        for (SubtractReasons.Reason r : out.reasons()) {
            if (r == null || r.itemId() == null || r.reason() == null) { dropped++; continue; }
            // 우리가 안 보낸 항목이 오면 버립니다. 모델이 항목을 만들어 낸 것입니다
            if (!byId.containsKey(r.itemId()))      { dropped++; continue; }
            if (r.reason().isBlank())               { dropped++; continue; }
            if (r.reason().length() > MAX_REASON)   { dropped++; continue; }
            ok.put(r.itemId(), r.reason());
        }
        if (dropped > 0) {
            // 본문은 안 찍습니다. 몇 개가 왜 빠졌는지만 남깁니다
            log.info("판정 근거 {}개 중 {}개를 버렸습니다 — 그 항목만 폴백으로 갑니다",
                    out.reasons().size(), dropped);
        }
        return ok;
    }

    private static String read(String path) {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            String s = new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
            if (s.isEmpty()) throw new IllegalStateException("프롬프트가 비어 있습니다: " + path);
            return s;
        } catch (IOException e) {
            throw new IllegalStateException(
                    "프롬프트를 못 읽었습니다: " + path
                    + " — db/tools/sync_prompts.py 를 돌려 주십시오", e);
        }
    }
}
