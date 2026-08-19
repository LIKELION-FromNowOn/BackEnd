package com.youin.now.subtract;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** {@code evaluations} 접근. <b>{@code subtract/} 밖에서 부르지 않습니다.</b> */
public interface EvaluationRepository extends JpaRepository<Evaluation, String> {

    /** 체크인 하나에 판정 하나. 다시 판정하면 이걸 찾아 고칩니다 */
    Optional<Evaluation> findByCheckinId(String checkinId);

    Optional<Evaluation> findTopByUserIdOrderByCreatedAtDesc(String userId);

    /** 홈 전용 — 그날 판정. {@code created_at} 이 {@code DATETIME} 이라 날짜로 자릅니다 */
    @Query("select e from Evaluation e where e.userId = :userId "
         + "and e.createdAt >= :from and e.createdAt < :to "
         + "order by e.createdAt desc limit 1")
    Optional<Evaluation> findOfDate(@Param("userId") String userId,
                                    @Param("from") java.time.OffsetDateTime from,
                                    @Param("to") java.time.OffsetDateTime to);
}
