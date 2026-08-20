package com.youin.now.note;

import java.util.List;

/**
 * {@code NOW-NOTE-001 GET /me/care} 응답. {@code PUT} 도 같은 모양으로 돌려줍니다.
 *
 * <p><b>등록된 것이 없으면 404 가 아니라 빈 값입니다</b> — 명세서가
 * 「등록된 관리 맥락이 없으면 빈 객체를 반환한다. 404를 내지 않는다」로 정했습니다.
 * 처음 쓰는 사람에게 오류를 보여 줄 이유가 없습니다.
 *
 * @param lastType 최근 관리 종류. 없으면 {@code null}
 * @param ago      경과일. 없으면 {@code null}
 * @param cautions <b>오늘 살아 있는 것만.</b> {@code daysLeft} 가 0이 된 것은 담기지 않습니다
 * @param hasNote  안내문이 등록되어 있는가. <b>{@code cautions} 가 비어도 {@code true} 일 수 있습니다</b> —
 *                 제한이 전부 풀린 경우입니다
 * @param saved    {@code PUT} 일 때만 채웁니다. 저장한 주의사항 수. 프론트 목이 이 필드를 봅니다
 */
public record CareRes(String lastType,
                      Integer ago,
                      List<Caution> cautions,
                      boolean hasNote,
                      Integer saved) {

    /** @param daysLeft <b>저장값이 아닙니다.</b> 읽을 때마다 {@code max(0, dp − 경과일)} 로 셉니다 */
    public record Caution(String itemId, String text, int sent, Integer dp, Integer daysLeft) { }

    static CareRes empty() {
        return new CareRes(null, null, List.of(), false, null);
    }
}
