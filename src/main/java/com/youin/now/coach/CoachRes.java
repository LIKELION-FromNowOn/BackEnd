package com.youin.now.coach;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * {@code NOW-COACH-001} 응답. <b>필드 이름은 노션 명세서 그대로입니다.</b>
 *
 * @param level       {@code no} · {@code soft} · {@code ok}.
 *                    <b>안내문에서 근거를 못 찾은 경우에는 비웁니다</b> —
 *                    명세가 그 경우의 값을 정하지 않았고, 셋 중 아무거나 넣으면
 *                    하지 않은 판단을 한 것처럼 됩니다 ({@code .agent/REQUESTS.md} #38)
 * @param generatedBy {@code rule+llm} 또는 {@code rule}. <b>판정은 언제나 규칙이 합니다</b>
 * @param chips       이어서 물을 만한 질문. 최대 2개
 * @param citedSents  <b>명세에 없는 추가 필드.</b> 프론트 목이 이 이름을 읽습니다.
 *                    {@code basis.sent} 를 배열로 담은 것뿐입니다
 * @param crisis      <b>명세에 없는 추가 필드.</b> 위기 신호가 잡혔는지. 프론트 목이 읽습니다
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CoachRes(String answer, Basis basis, String level, String generatedBy,
                       List<String> chips, List<Integer> citedSents, boolean crisis) {

    /**
     * @param type       {@code clinicNote} · {@code safety} · {@code none}
     * @param sent       <b>안내문 원문 몇 번째 문장인지.</b> 이 앱 신뢰 구조의 핵심입니다
     * @param daysLeft   남은 제한 일수. <b>규칙 엔진 계산값이고 AI 가 만든 숫자가 아닙니다</b>
     * @param sourceText 원문 문장 그대로. {@code clinicNote} 일 때만
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Basis(String type, String label, Integer sent,
                        Integer daysLeft, String sourceText) {}
}
