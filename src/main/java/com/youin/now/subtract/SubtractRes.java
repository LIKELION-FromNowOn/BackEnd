package com.youin.now.subtract;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * {@code POST /subtract/evaluate} · {@code GET /subtract/latest} 응답.
 *
 * <p><b>{@code generatedBy} 를 항상 채웁니다.</b> 발표에서 「AI 가 진짜 도는지」를 이 필드로 보여 줍니다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SubtractRes(
        String evaluationId,
        String checkinId,
        String state,
        String judgeStrength,
        String generatedBy,
        Summary summary,
        List<Item> items) {

    /** 홈이 쓰는 얇은 것. 판정 32건 전부가 아니라 개수 다섯 개만 */
    public record Summary(int keep, int simplify, int reduce, int skip, int excluded) {}

    /**
     * @param excludedBy   {@code excluded} 일 때만. {@code medical} 또는 {@code clinicNote}
     * @param noteSent     {@code clinicNote} 일 때만. 안내문 원문 문장 번호
     * @param daysLeft     {@code clinicNote} 일 때만. <b>조회 시점에 다시 계산한 값</b>
     * @param floorApplied 서버가 판정을 하한선으로 되돌렸으면 true
     * @param revertable   <b>{@code excluded} 는 false.</b> 화면에서 되돌리기 버튼을 띄우지 마십시오
     */
    public record Item(String itemId, String verdict, String reason,
                       String floor, boolean floorApplied, boolean reverted,
                       boolean revertable,
                       String excludedBy, Integer noteSent, Integer daysLeft) {}
}
