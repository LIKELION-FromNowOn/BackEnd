package com.youin.now.common.llm;

/**
 * LLM 호출 창구. <b>AI 를 부르는 자리는 네 곳인데 클라이언트는 하나입니다.</b>
 *
 * <table>
 *   <tr><th>#</th><th>어디</th><th>API</th><th>API 소유자</th></tr>
 *   <tr><td>1</td><td>판정 근거 문장</td><td>{@code NOW-SUB-001}</td><td>송원석</td></tr>
 *   <tr><td>2</td><td>오늘의 행동</td><td>{@code NOW-TODAY-001}</td><td>김민정</td></tr>
 *   <tr><td>3</td><td>직접 입력 해석</td><td>{@code NOW-ITEM-003}</td><td>이철희</td></tr>
 *   <tr><td>4</td><td>케어 코치 답변</td><td>{@code NOW-COACH-001}</td><td>송원석</td></tr>
 * </table>
 *
 * <p>각자 만들면 재시도·타임아웃·폴백이 세 벌이 됩니다. <b>여기 하나만 씁니다.</b>
 * 프롬프트 문자열은 코드가 아니라 산출물입니다 — {@code docs/prompts/} 를 보십시오.
 *
 * <p><b>구현은 이철희 님 몫입니다.</b> 규약은 {@code docs/prompts/00-index.md} 「호출 규약」과 같습니다 —
 * 타임아웃 6초 · 재시도 1회 · 실패하면 <b>예외를 던지지 말고 {@code null}</b> 을 돌려주십시오.
 * 부르는 쪽이 폴백 문장을 갖고 있습니다.
 */
public interface LlmClient {

    /**
     * @param systemPrompt {@code docs/prompts/} 의 시스템 프롬프트 전문
     * @param userPayload  서버가 채운 입력 JSON
     * @param responseType 출력 스키마에 대응하는 record 클래스
     * @return 파싱된 결과. <b>실패하면 {@code null}</b> (예외를 던지지 않습니다)
     */
    <T> T ask(String systemPrompt, String userPayload, Class<T> responseType);
}
