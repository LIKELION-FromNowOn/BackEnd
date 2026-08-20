package com.youin.now.subtract;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** {@code evaluations} 접근. <b>{@code subtract/} 밖에서 부르지 않습니다.</b> */
public interface EvaluationRepository extends JpaRepository<Evaluation, String> {

    /** 체크인 하나에 판정 하나. 다시 판정하면 이걸 찾아 고칩니다 */
    Optional<Evaluation> findByCheckinId(String checkinId);

    Optional<Evaluation> findTopByUserIdOrderByCreatedAtDesc(String userId);

    /**
     * 기록 탭 H03 — 판정 이력. <b>최근 것이 먼저</b>입니다.
     *
     * <p>기간을 안 주면 전부입니다. {@code created_at} 이 {@code DATETIME} 이라 날짜 경계로 자릅니다.
     */
    @Query("select e from Evaluation e where e.userId = :userId "
         + "and (:from is null or e.createdAt >= :from) "
         + "and (:to is null or e.createdAt < :to) "
         + "order by e.createdAt desc")
    List<Evaluation> findHistory(@Param("userId") String userId,
                                 @Param("from") java.time.OffsetDateTime from,
                                 @Param("to") java.time.OffsetDateTime to);

    /**
     * 덜어내기를 한 <b>날 수</b>. 판정 횟수가 아닙니다 — 같은 날 두 번 판정해도 하루입니다.
     */
    @Query(value = """
            select count(distinct date(created_at))
              from evaluations
             where user_id = :userId
               and (:from is null or created_at >= :from)
               and (:to   is null or created_at <  :to)
            """, nativeQuery = true)
    int countSubtractedDays(@Param("userId") String userId,
                            @Param("from") java.time.OffsetDateTime from,
                            @Param("to") java.time.OffsetDateTime to);

    /** 홈 전용 — 그날 판정. {@code created_at} 이 {@code DATETIME} 이라 날짜로 자릅니다 */
    @Query("select e from Evaluation e where e.userId = :userId "
         + "and e.createdAt >= :from and e.createdAt < :to "
         + "order by e.createdAt desc limit 1")
    Optional<Evaluation> findOfDate(@Param("userId") String userId,
                                    @Param("from") java.time.OffsetDateTime from,
                                    @Param("to") java.time.OffsetDateTime to);
}
