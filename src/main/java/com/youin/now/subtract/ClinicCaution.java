package com.youin.now.subtract;

/**
 * 클리닉 안내문에서 나온, 오늘 살아 있는 제한 하나.
 *
 * <p><b>{@code daysLeft} 를 저장하지 않습니다.</b> 읽을 때마다 {@code max(0, daysPeriod - ago)} 로 셉니다.
 * 저장하면 날짜가 지나도 줄지 않습니다.
 *
 * @param itemId      제한이 걸리는 관리 항목 ID
 * @param sentenceNo  안내문 원문 문장 번호
 * @param daysPeriod  안내문이 정한 제한 기간(일)
 * @param daysLeft    오늘 기준 남은 일수
 */
public record ClinicCaution(String itemId, int sentenceNo, int daysPeriod, int daysLeft) {

    /**
     * @param ago 시술 후 경과일. <b>모르면 세지 않습니다</b> — 이 경우 제한은 살아 있는 것으로 봅니다
     */
    public static ClinicCaution of(String itemId, int sentenceNo, int daysPeriod, Integer ago) {
        int left = (ago == null) ? daysPeriod : Math.max(0, daysPeriod - ago);
        return new ClinicCaution(itemId, sentenceNo, daysPeriod, left);
    }

    /** 기간이 지났으면 더는 막지 않습니다. */
    public boolean alive() { return daysLeft > 0; }
}
