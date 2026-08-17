package com.youin.now.note;

import java.util.List;

/**
 * 클리닉 안내문에서 뽑아 낸, 오늘 살아 있는 제한 규칙.
 *
 * <p><b>제공 — 송원석 · 사용 — 김민정(today, home)</b>
 *
 * <p><b>{@code daysLeft} 를 저장하지 않습니다.</b> 읽을 때마다 {@code max(0, daysPeriod - 시술 후 경과일)} 로 계산합니다.
 * 저장하면 날짜가 지나도 줄어들지 않습니다.
 *
 * <p>한 항목이 여러 규칙에 걸리면 <b>{@code daysPeriod} 가 가장 큰 것</b>을 씁니다.
 * 먼저 풀리는 것을 보여 주면 틀린 안내가 됩니다.
 *
 * <p>시그니처 원본은 {@code docs/04-ports.md} 입니다. <b>여기서 바꾸지 마십시오.</b>
 */
public interface NoteRulePort {

    /**
     * @return 오늘 기준으로 아직 살아 있는 규칙만. 없으면 <b>빈 리스트</b> (null 아님)
     */
    List<NoteRule> activeRules(Long userId);

    /**
     * @param sentenceNo  안내문 원문 문장 번호. 화면에서 원문으로 이동할 때 씁니다
     * @param daysPeriod  안내문이 정한 제한 기간(일)
     * @param name        화면에 보여 줄 규칙 이름
     * @param keywords    이 규칙이 걸리는 키워드
     * @param itemId      직접 연결된 관리 항목 id. 없으면 null
     * @param daysLeft    읽는 시점에 계산된 남은 일수. <b>저장값이 아닙니다</b>
     */
    record NoteRule(int sentenceNo, int daysPeriod, String name,
                    List<String> keywords, String itemId, int daysLeft) {}
}
