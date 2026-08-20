package com.youin.now.care;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** {@code plans} 접근. */
public interface PlanRepository extends JpaRepository<Plan, String> {

    /** {@code ix_plans_user_date} 인덱스를 탑니다 */
    List<Plan> findByUserIdOrderByPlanDateAscCreatedAtAsc(String userId);

    /**
     * <b>삭제는 반드시 이것으로 조회한 뒤에 하십시오.</b>
     * {@code findById(id)} 로 지우면 남의 예정이 지워집니다.
     */
    Optional<Plan> findByIdAndUserId(String id, String userId);
}