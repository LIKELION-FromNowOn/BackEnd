package com.youin.now.common.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code app.llm.*} 설정.
 *
 * <p>값의 근거는 {@code docs/prompts/00-index.md} 에 있습니다. 2026-08-20 에 팀 키로
 * 9번 호출해 잰 것이고, <b>세 추론 단계가 전부 6초를 넘어</b> 타임아웃을 10초로 올렸습니다.
 *
 * @param apiKey          <b>비어 있어도 앱은 정상으로 뜹니다.</b> 그러면 호출하지 않고 폴백으로 갑니다
 * @param timeoutSeconds  10초. <b>재시도가 없어서 이것이 곧 최악 대기입니다</b>
 * @param maxRetries      <b>0.</b> 재시도를 넣으면 최악 대기가 두 배가 됩니다
 * @param reasoningEffort {@code low}. 대가가 0.39초뿐이고 편차가 가장 작았습니다
 */
@ConfigurationProperties(prefix = "app.llm")
public record LlmProperties(String apiKey, String model,
                            int timeoutSeconds, int maxRetries, String reasoningEffort) {
}
