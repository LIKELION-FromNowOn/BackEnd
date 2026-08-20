package com.youin.now.today;

/**
 * LLM 출력. {@code docs/prompts/02-today-action.md} 의 출력 스키마입니다.
 *
 * <p>{@code actionId} · {@code durationSec} · {@code rank} 등은 <b>서버가 채웁니다.</b>
 * LLM 에 요구하지 않습니다.
 *
 * @param title 40자 이내. 행동 문장 하나
 * @param why   60자 이내. 「왜 지금 이것인가」
 */
public record TodayLlmOut(String title, String why) { }