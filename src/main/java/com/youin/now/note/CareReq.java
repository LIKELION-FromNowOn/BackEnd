package com.youin.now.note;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * {@code NOW-NOTE-002 PUT /me/care} 요청.
 *
 * <p><b>날짜를 절대값이 아니라 경과일({@code ago})로 받습니다</b> — 명세서 규칙입니다.
 * 앱을 며칠 뒤에 열어도 「그날로부터 며칠」이 그대로 유지되어야 합니다.
 *
 * <p>{@code noteLines} 는 <b>명세서에 없고 프론트 목에만 있습니다.</b> 그래도 받습니다 —
 * 없으면 {@code cautions[].sent} 가 가리킬 문장이 없고, 외래키
 * {@code (care_note_id, sent_no)} 때문에 주의사항을 넣을 수도 없습니다.
 * {@code .agent/REQUESTS.md} 에 명세 반영을 올렸습니다.
 *
 * @param lastType   최근 관리의 종류. 「피부 관리」 같은 것
 * @param ago        경과일 <b>0~90</b>. 벗어나면 400
 * @param noteLines  안내문 원문 문장. <b>배열 순서가 곧 문장 번호이고 1부터</b>입니다
 * @param cautions   살아 있는 주의사항
 */
public record CareReq(
        @NotBlank @Size(max = 255) String lastType,
        @NotNull @Min(0) @Max(90) Integer ago,
        List<@NotBlank String> noteLines,
        List<@Valid Caution> cautions) {

    /**
     * @param itemId 걸리는 관리 항목. 없으면 {@code null}
     * @param text   <b>화면에 그대로 보여 줄 문장.</b> 서버가 조립하지 않습니다
     * @param sent   {@code noteLines} 의 몇 번째 문장인지 (1부터)
     * @param dp     제한 일수. <b>{@code null} 이면 기간을 모르는 것</b>이라 자동 만료시키지 않습니다
     */
    public record Caution(String itemId,
                          @NotBlank @Size(max = 255) String text,
                          @NotNull @Min(1) Integer sent,
                          Integer dp) { }
}
