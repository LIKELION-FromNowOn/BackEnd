package com.youin.now.common.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * {@link LlmClient} 실제 구현 — OpenAI Chat Completions.
 *
 * <p><b>의존성을 하나도 안 늘렸습니다.</b> Java 21 의 {@link HttpClient} 와
 * 스프링에 이미 있는 Jackson 만 씁니다.
 *
 * <h2>실패하면 반드시 {@code null} 을 돌려줍니다</h2>
 *
 * <p>키 없음 · 네트워크 실패 · 타임아웃 · 200 이 아닌 응답 · JSON 파싱 실패 —
 * <b>전부 예외를 밖으로 안 던지고 {@code null} 입니다.</b> 부르는 쪽이 폴백 문장을 쓰면 됩니다.
 * <b>AI 가 안 되는 것과 앱이 죽는 것은 다릅니다.</b>
 *
 * <h2>재시도하지 않습니다</h2>
 *
 * <p>{@code docs/prompts/00-index.md} 의 규약입니다. 재시도를 넣으면 최악 대기가
 * 10초에서 20초가 됩니다. <b>사용자를 20초 기다리게 하느니 폴백 문장을 바로 주는 편이 낫습니다.</b>
 *
 * <h2>JSON 을 API 수준에서 강제합니다</h2>
 *
 * <p>{@code response_format: {"type":"json_object"}} 를 붙입니다. 그래서 모델이
 * 설명이나 코드펜스를 앞에 붙이지 못합니다. <b>2026-08-20 실측 9회 전부 파싱에 성공했습니다.</b>
 */
@Component
public class OpenAiLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiLlmClient.class);
    private static final String URL = "https://api.openai.com/v1/chat/completions";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final LlmProperties props;
    private final HttpClient http;

    public OpenAiLlmClient(LlmProperties props) {
        this.props = props;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.max(1, props.timeoutSeconds() / 2)))
                .build();

        // 기동 로그에 한 줄 남깁니다. 발표 직전에 「AI 가 켜져 있나」를 여기서 봅니다.
        // 키는 절대 찍지 않습니다 — 켜졌는지와 모델 이름만입니다
        if (enabled()) {
            log.info("LLM 켜짐 — 모델 {} · 타임아웃 {}초 · 추론 {} · 재시도 {}회",
                    props.model(), props.timeoutSeconds(), props.reasoningEffort(), props.maxRetries());
        } else {
            log.warn("LLM 꺼짐 — 키가 없거나 sk- 로 시작하지 않습니다. 전부 폴백 문장으로 갑니다");
        }
    }

    /** 키가 있고 모양이 맞는가. <b>{@code sk-} 로 시작하지 않으면 부르지 않습니다</b> */
    public boolean enabled() {
        String k = props.apiKey();
        return k != null && k.startsWith("sk-");
    }

    @Override
    public <T> T ask(String systemPrompt, String userPayload, Class<T> responseType) {
        if (!enabled()) {
            log.debug("LLM 키가 없어 호출하지 않습니다. 폴백으로 갑니다");
            return null;
        }
        try {
            HttpResponse<String> res = http.send(
                    HttpRequest.newBuilder(URI.create(URL))
                            .timeout(Duration.ofSeconds(props.timeoutSeconds()))
                            .header("Authorization", "Bearer " + props.apiKey())
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(
                                    body(systemPrompt, userPayload), StandardCharsets.UTF_8))
                            .build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (res.statusCode() != 200) {
                // 본문을 로그에 남기지 않습니다 — 사용자가 적은 문장이 들어 있을 수 있습니다
                log.warn("LLM 응답 코드 {} — 폴백으로 갑니다", res.statusCode());
                return null;
            }
            String content = MAPPER.readTree(res.body())
                    .path("choices").path(0).path("message").path("content").asText(null);
            if (content == null || content.isBlank()) {
                log.warn("LLM 응답에 본문이 없습니다 — 폴백으로 갑니다");
                return null;
            }
            return MAPPER.readValue(content, responseType);

        } catch (Exception e) {
            // 타임아웃 · 네트워크 · 파싱 전부 여기로 옵니다. 밖으로 던지지 않습니다
            log.warn("LLM 호출 실패 ({}) — 폴백으로 갑니다", e.getClass().getSimpleName());
            return null;
        }
    }

    private String body(String systemPrompt, String userPayload) {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("model", props.model());
        root.putObject("response_format").put("type", "json_object");
        if (props.reasoningEffort() != null && !props.reasoningEffort().isBlank()) {
            root.put("reasoning_effort", props.reasoningEffort());
        }
        var messages = root.putArray("messages");
        messages.addObject().put("role", "system").put("content", systemPrompt);
        messages.addObject().put("role", "user").put("content", userPayload);
        return root.toString();
    }

    /** 진단용. <b>키는 절대 돌려주지 않습니다</b> — 켜졌는지와 모델 이름만입니다 */
    public JsonNode status() {
        ObjectNode n = MAPPER.createObjectNode();
        n.put("enabled", enabled());
        n.put("model", props.model());
        n.put("timeoutSeconds", props.timeoutSeconds());
        n.put("reasoningEffort", props.reasoningEffort());
        return n;
    }
}
