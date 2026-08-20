package com.youin.now.item;

import java.util.List;

/**
 * {@code item/} 이 남에게 열어 주는 창구. <b>시그니처는 {@code docs/04-ports.md} 그대로입니다.</b>
 *
 * <p><b>2026-08-20 확장 — 필드 4개를 더했습니다.</b> {@code name} · {@code categoryId} ·
 * {@code core} · {@code base} 입니다. 기존 필드는 이름도 타입도 그대로입니다.
 *
 * <p><b>왜 넓혔는가.</b> 점수식이 {@code core} 와 {@code base} 를 씁니다.
 * <pre>
 *   score = core x 1.7 - load x 0.75 - strict x (load x 0.35)
 *   load  = base + FQL[frequency]
 * </pre>
 * 이 창구가 둘을 안 날라서 {@code SubtractService} 가 <b>0 을 넣고 있었습니다.</b>
 * 지금은 스텁이 빈 목록이라 안 드러나지만, <b>실제 구현이 들어오는 순간 모든 항목의
 * 점수가 같아져 판정이 전부 동일하게 나옵니다.</b> 그때 발견하면 판정 API 를 다시 손대야 하고,
 * 그 시점은 통합일입니다. 그래서 지금 넓혔습니다.
 *
 * <p>{@code name} 은 판정 응답의 {@code results[].name} 자리입니다 —
 * 명세서가 필수로 두고 있는데 지금은 {@code itemId} 를 대신 넣고 있었습니다.
 *
 * <p><b>{@code docs/04-ports.md:56} 은 아직 옛 시그니처입니다.</b> {@code docs/} 를 고치지 않는
 * 규칙이라 {@code .agent/REQUESTS.md} #30 에 올렸습니다.
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
     * <b>넷은 전부 {@code care_items} 마스터에서 읽습니다.</b> {@code user_items} 에는 없습니다 —
     * 조인 한 번이면 되고, 마스터 시드는 2026-08-20 에 들어갔습니다.
     *
     * @param userItemId    <b>{@code user_items.id}</b> — {@code ui_01H8X…}. 마스터 ID 가 아닙니다.
     *                      {@code evaluation_results.user_item_id} 의 외래키가 이 값을 요구합니다.
     *                      <b>2026-08-20 추가</b> — 이것이 없어서 저장이 외래키에서 터질 상태였습니다
     * @param itemId        {@code care_items.id} — {@code cr1} {@code sl2} 처럼.
     *                      <b>응답의 {@code results[].itemId} 로 나가는 값</b>입니다.
     *                      직접 입력 항목은 마스터 ID 가 없으니 {@code userItemId} 를 그대로 넣어 주십시오
     * @param name          {@code care_items.name} — 「아침 보습 루틴」. <b>판정 응답에 그대로 나갑니다</b>
     * @param categoryId    {@code care_items.category_id} — {@code care} {@code sleep} …
     * @param core          {@code care_items.core} 중요도. <b>소수입니다</b> — 2.5 를 2 로 반올림하면 판정이 뒤집힙니다
     * @param base          {@code care_items.base} 기본 부담. <b>소수입니다</b>
     * @param frequency     {@code weekly_1} · {@code weekly_2} · {@code weekly_3} · {@code weekly_4plus} · {@code daily}
     *                      <b>{@code monthly} 는 없습니다.</b> 5종입니다
     * @param floor         하한선. {@code 2}=essential · {@code 1}=recommended · {@code 0}=optional · {@code -1}=excluded
     *                      <b>{@code user_items} 가 아니라 {@code care_items} 마스터에서 읽습니다</b>
     * @param evidenceLevel 근거 등급. 낮을수록 확실합니다. <b>{@code none} 이면 판정하지 않고 {@code keep} 고정입니다</b>
     */
    record SelectedItem(String userItemId, String itemId, String name, String categoryId,
                        double core, double base, String frequency,
                        int floor, int evidenceLevel) {}
}
