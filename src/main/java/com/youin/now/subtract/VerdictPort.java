package com.youin.now.subtract;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 오늘의 덜어내기 판정 결과 조회.
 *
 * <p><b>제공 — 송원석 · 사용 — 김민정(today, home)</b>
 *
 * <p><b>{@link #summary} 가 따로 있는 이유</b> — 홈은 판정 전체가 아니라 개수 다섯 개만 필요합니다.
 * 같은 메서드를 쓰면 홈이 열릴 때마다 판정 수십 건이 통째로 옵니다.
 * <b>응답 다이어트를 인터페이스 수준에서 강제하는 장치입니다.</b> 홈에서 {@link #of} 를 부르지 마십시오.
 *
 * <p>시그니처 원본은 {@code docs/04-ports.md} 입니다. <b>여기서 바꾸지 마십시오.</b>
 */
public interface VerdictPort {

    /*
     * 2026-08-20 — VerdictSet 과 ItemVerdict 에 필드를 더했습니다.
     * docs/04-ports.md:44 는 아직 옛 시그니처입니다 (.agent/REQUESTS.md #37).
     *
     * 왜 — actions 테이블이 evaluation_id 와 user_item_id 를 둘 다 NOT NULL 외래키로
     * 요구하는데 이 창구가 그 둘을 안 날랐습니다. today/ 가 행동을 저장할 수가 없습니다.
     * 소비자가 아직 없을 때 고치는 것이 가장 쌉니다.
     */

    /**
     * @return 그날 판정이 아직 없으면 {@link Optional#empty()}
     */
    Optional<VerdictSet> of(String userId, LocalDate date);

    /**
     * 홈 전용. 판정이 없으면 <b>전부 0 인 Summary</b> 를 돌려줍니다 (null 아님).
     */
    Summary summary(String userId, LocalDate date);

    /**
     * 기록 요약 전용. <b>{@code NOW-LOG-002} 의 {@code daysSubtracted} · {@code topSubtracted} 입니다.</b>
     *
     * <p>{@code evaluations} · {@code evaluation_results} 는 {@code subtract/} 소유라
     * {@code log/} 에서 직접 읽지 않습니다 — {@code docs/04-ports.md} 「남의 {@code Repository} 를
     * 직접 부르지 않습니다」.
     *
     * <p><b>이름은 안 담습니다.</b> {@code care_items.name} 은 {@code master/} 소유이고
     * 김민정 님 것이라, {@code itemId} 만 드리면 그쪽에서 붙이는 편이 경계가 깨끗합니다.
     *
     * <p>기간을 안 주면 전부입니다. 없으면 <b>0 과 빈 목록</b>입니다 (null 아님).
     *
     * @param from 이 날부터 (KST, 포함). {@code null} 이면 처음부터
     * @param to   이 날까지 (KST, 포함). {@code null} 이면 오늘까지
     */
    Stats stats(String userId, LocalDate from, LocalDate to);

    /**
     * 홈의 {@code subtract} 블록. <b>{@code NOW-HOME-001} 이 요구하는 세 칸을 그대로 냅니다.</b>
     *
     * <p>{@link #summary} 는 개수 다섯 개만 줍니다 — {@code evaluationId} 를 얻을 자리가 없어
     * 김민정 님이 홈을 만들 수 없었습니다. {@code Summary} 에 칸을 더하면
     * {@code SubtractPipeline} 까지 파급되므로 <b>메서드를 따로 엽니다.</b>
     *
     * <p><b>그날 판정이 없으면 {@code null} 입니다</b> — 명세가 「subtract: object (null 허용).
     * 판정 전이면 null」로 정했습니다.
     */
    HomeSubtract subtractForHome(String userId, LocalDate date);

    /**
     * @param removedCount <b>오늘 걷어낸 항목 수 = {@code reduce + skip}.</b>
     *                     {@code simplify} 는 <b>세지 않습니다</b> — 「양과 목표는 그대로 두고
     *                     측정·실행 방식만 낮춤」이라 걷어낸 것이 아닙니다.
     *                     명세 예시가 {@code keep 3 · simplify 2 · reduce 1 · skip 1 · excluded 2} 에
     *                     {@code removedCount 2} 입니다 — {@code reduce + skip} 과만 맞습니다.
     */
    record HomeSubtract(String evaluationId, Summary summary, int removedCount) {}

    /**
     * @param daysSubtracted <b>덜어내기를 한 날 수.</b> 판정 횟수가 아니라 날짜 수입니다 —
     *                       같은 날 두 번 판정해도 하루입니다
     * @param topSubtracted  자주 덜어낸 항목. <b>많은 것부터</b>. {@code keep} 과 {@code excluded} 는 셈에서 뺍니다
     */
    record Stats(int daysSubtracted, List<TopItem> topSubtracted) {}

    /** @param itemId 마스터 항목 id({@code cr4}). 직접 입력 항목이면 {@code user_items.id} 입니다 */
    record TopItem(String itemId, int count) {}

    /**
     * @param evaluationId <b>2026-08-20 추가.</b> {@code actions.evaluation_id} 가 {@code NOT NULL}
     *                     외래키라 {@code today/} 가 「오늘의 행동」을 저장할 때 반드시 필요합니다
     */
    record VerdictSet(String evaluationId, List<ItemVerdict> results) {}

    /**
     * @param userItemId <b>2026-08-20 추가.</b> {@code user_items.id}.
     *                   {@code actions.user_item_id} 외래키가 이 값을 요구합니다
     * @param itemId     {@code care_items.id} — {@code cr4} 처럼. <b>화면에 나가는 것은 이쪽</b>입니다
     *                   ({@code docs/07-response-rules.md:57}). 항목 이름도 이것으로 찾습니다.
     *                   <b>직접 입력 항목은 마스터가 없어 {@code userItemId} 와 같은 값이 옵니다</b>
     * @param verdict    keep | simplify | reduce | skip | excluded
     * @param excludedBy excluded 일 때만. medical | clinicNote. 아니면 null
     */
    record ItemVerdict(String userItemId, String itemId, String verdict,
                       String reason, String excludedBy) {}

    record Summary(int keep, int simplify, int reduce,
                   int skip, int excluded) {}
}
