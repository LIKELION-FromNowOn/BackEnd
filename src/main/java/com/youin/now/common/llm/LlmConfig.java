package com.youin.now.common.llm;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * {@link LlmProperties} 를 켭니다.
 *
 * <p><b>{@code NowApplication} 에 {@code @ConfigurationPropertiesScan} 을 붙이지 않았습니다.</b>
 * 그 파일은 앱 전체의 입구라 한 사람이 고치면 다른 사람 것까지 영향이 갑니다.
 * 그래서 설정을 이 폴더 안에 뒀습니다.
 *
 * <p>⚠️ <b>{@code common/} 은 이철희 님 폴더입니다</b>({@code docs/03-packages.md:7} ·
 * {@code docs/02-roles.md:17}). 「LLM 클라이언트」도 그 안에 명시돼 있습니다.
 * <b>2026-08-20 에 송원석이 파일 셋을 넣었고, {@code docs/02-roles.md:25} 가 요구하는
 * 슬랙 사전 공지를 빠뜨렸습니다.</b> {@code .agent/REQUESTS.md} #42 에 올렸습니다.
 * <b>되돌릴지 그대로 둘지는 이철희 님이 정하십니다</b> — 기존 파일은 안 건드렸고
 * 새 파일 셋뿐이라 지우면 원상복구됩니다.
 */
@Configuration
@EnableConfigurationProperties(LlmProperties.class)
public class LlmConfig {
}
