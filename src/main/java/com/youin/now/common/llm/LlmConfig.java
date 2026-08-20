package com.youin.now.common.llm;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * {@link LlmProperties} 를 켭니다.
 *
 * <p><b>{@code NowApplication} 에 {@code @ConfigurationPropertiesScan} 을 붙이지 않았습니다.</b>
 * 그 파일은 앱 전체의 입구라 한 사람이 고치면 다른 사람 것까지 영향이 갑니다.
 * <b>{@code common/llm/} 은 송원석 소유</b>({@code .agent/REQUESTS.md} #18)라 여기 둡니다.
 */
@Configuration
@EnableConfigurationProperties(LlmProperties.class)
public class LlmConfig {
}
