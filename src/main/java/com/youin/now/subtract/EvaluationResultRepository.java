package com.youin.now.subtract;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** {@code evaluation_results} 접근. <b>{@code subtract/} 밖에서 부르지 않습니다.</b> */
public interface EvaluationResultRepository extends JpaRepository<EvaluationResult, String> {

    List<EvaluationResult> findByEvaluationId(String evaluationId);

    Optional<EvaluationResult> findByEvaluationIdAndUserItemId(String evaluationId, String userItemId);

    /**
     * 재판정할 때 이전 결과를 지웁니다.
     *
     * <p><b>파생 delete 메서드를 쓰면 안 됩니다.</b> 그것은 엔티티를 로드해 remove 표시만 하고,
     * 하이버네이트는 flush 에서 <b>INSERT 를 DELETE 보다 먼저</b> 내보냅니다.
     * 그러면 {@code ux_eval_results_item (evaluation_id, user_item_id)} 에 걸려
     * <b>같은 날 두 번째 판정이 500</b> 이 됩니다. 실제로 그랬습니다(2026-08-20).
     * 벌크 삭제로 즉시 실행하고 영속성 컨텍스트를 비웁니다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from EvaluationResult r where r.evaluationId = :evaluationId")
    void deleteByEvaluationId(@Param("evaluationId") String evaluationId);

    /**
     * 이 사용자가 <b>지금까지 한 번이라도 되돌린</b> 마스터 항목 ID.
     *
     * <p>{@code NOW-SUB-003} 의 {@code persisted: true} 약속을 지키는 자리입니다 —
     * 「다음 판정에서도 keep 으로 고정」. <b>{@code user_items.id} 가 아니라 마스터 ID 로 셉니다.</b>
     * {@code PUT /me/items} 가 기존 행을 지우고 새로 넣어서 {@code user_items.id} 는 매번 바뀝니다.
     */
    @Query(value = """
            select distinct coalesce(ui.care_item_id, ui.id)
              from evaluation_results er
              join evaluations e  on e.id  = er.evaluation_id
              join user_items  ui on ui.id = er.user_item_id
             where e.user_id = :userId and er.reverted = true
            """, nativeQuery = true)
    List<String> findRevertedItemIds(@Param("userId") String userId);

    /**
     * 자주 덜어낸 항목. <b>{@code keep} 과 {@code excluded} 는 세지 않습니다</b> —
     * 그대로 둔 것과 앱이 판단하지 않은 것은 「덜어냈다」가 아닙니다.
     *
     * <p><b>되돌린 것도 빠집니다.</b> {@code revert} 가 {@code verdict} 를 {@code keep} 으로 바꾸므로
     * 조건에서 자연히 제외됩니다 — 사용자가 되돌렸으면 덜어낸 것이 아닙니다.
     *
     * <p>{@code user_items} 는 {@code item/} 소유지만 {@code evaluation_results.user_item_id} 가
     * 외래키로 가리키는 한 단계라 함께 읽습니다. <b>{@code care_items} 까지는 안 갑니다</b> —
     * 이름은 {@code master/} 소유라 김민정 님이 붙이십니다.
     */
    @Query(value = """
            select coalesce(ui.care_item_id, ui.id) as itemId, count(*) as cnt
              from evaluation_results er
              join evaluations e  on e.id  = er.evaluation_id
              join user_items  ui on ui.id = er.user_item_id
             where e.user_id = :userId
               and er.verdict in ('simplify', 'reduce', 'skip')
               and (:from is null or e.created_at >= :from)
               and (:to   is null or e.created_at <  :to)
             group by itemId
             order by cnt desc
            """, nativeQuery = true)
    List<TopRow> findTopSubtracted(@Param("userId") String userId,
                                   @Param("from") java.time.OffsetDateTime from,
                                   @Param("to") java.time.OffsetDateTime to);

    interface TopRow { String getItemId(); int getCnt(); }
}
