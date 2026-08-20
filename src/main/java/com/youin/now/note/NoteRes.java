package com.youin.now.note;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * {@code NOW-NOTE-003} 안내문 원문 조회 응답. <b>필드 이름은 노션 명세서 그대로입니다.</b>
 *
 * @param from   발신 클리닉. DB 는 {@code from_name} 이지만 <b>응답은 {@code from}</b> 입니다
 * @param sample <b>가상 샘플이면 {@code true}.</b> 화면에 반드시 표시해야 합니다 —
 *               실제 클리닉 문서가 아니라 형식만 재현한 것입니다
 * @param lines  <b>배열 순서가 곧 문장 번호이고 1부터 셉니다.</b>
 *               {@code rules[].sent} 가 이 순서를 가리킵니다
 */
public record NoteRes(String title,
                      @JsonProperty("from") String from,
                      boolean sample,
                      List<String> lines,
                      List<Rule> rules) {

    /**
     * @param sent   <b>원문 역추적의 유일한 근거.</b> {@code lines} 의 몇 번째 문장인지 (1부터)
     * @param dp     제한 일수 (D+n)
     * @param kw     매칭용 키워드
     * @param itemId 걸리는 관리 항목. <b>항목에 안 붙는 제한도 있어 {@code null} 일 수 있습니다</b>
     */
    public record Rule(int sent, int dp, String name, List<String> kw, String itemId) {}
}
