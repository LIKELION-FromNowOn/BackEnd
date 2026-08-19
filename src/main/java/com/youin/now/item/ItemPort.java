package com.youin.now.item;

import java.util.List;

/**
 * {@code item/} 이 남에게 열어 주는 창구. <b>시그니처는 {@code docs/04-ports.md} 그대로입니다.</b>
 *
 * <p><b>바꾸지 마십시오.</b> 판정 코드가 이 모양에 맞춰 쓰여 있습니다.
 *
 * <p><b>누가 쓰나</b> — 판정 파이프라인의 ①단계 「사용자가 선택한 항목만 추림」입니다.
 * 이 창구가 없으면 판정 API 가 실제 데이터로 돌지 못합니다.
 *
 * <p><b>만드는 사람은 이철희 님</b>입니다({@code docs/04-ports.md:15}).
 * 판정 API 가 이것부터 기다려서 <b>2026-08-20 에 송원석이 스텁만 먼저 열었습니다.</b>
 * 실제 구현은 {@code user_items} 를 읽어 채우면 됩니다.
 */
public interface ItemPort {

    /**
     * 이 사람이 관리하기로 한 항목들.
     *
     * @return 아직 아무것도 안 골랐으면 <b>빈 목록</b>. {@code null} 을 돌려주지 마십시오
     */
    List<SelectedItem> selected(String userId);

    /**
     * @param itemId        {@code care_items.id} — {@code cr1} {@code sl2} 처럼
     * @param frequency     {@code weekly_1} · {@code weekly_2} · {@code weekly_3} · {@code weekly_4plus} · {@code daily}
     *                      <b>{@code monthly} 는 없습니다.</b> 5종입니다
     * @param floor         하한선. {@code 2}=essential · {@code 1}=recommended · {@code 0}=optional · {@code -1}=excluded
     *                      <b>{@code user_items} 가 아니라 {@code care_items} 마스터에서 읽습니다</b>
     * @param evidenceLevel 근거 등급. 낮을수록 확실합니다. <b>{@code none} 이면 판정하지 않고 {@code keep} 고정입니다</b>
     */
    record SelectedItem(String itemId, String frequency,
                        int floor, int evidenceLevel) {}
}
