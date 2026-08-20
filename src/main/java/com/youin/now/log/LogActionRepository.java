package com.youin.now.log;

import com.youin.now.today.TodayAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * {@code actions} 읽기 전용. <b>기록은 {@code actions} 파생입니다</b> —
 * {@code daily_logs} 는 쓰지 않습니다 ({@code .agent/REQUESTS.md} #2 확정).
 *
 * <p>엔티티는 {@code today/TodayAction} 을 그대로 씁니다. 두 패키지 다 김민정 님 것이고,
 * 같은 표에 엔티티를 둘 만들면 하이버네이트가 {@code Duplicate entity} 로 죽습니다.
 *
 * <p><b>완료한 것만 셉니다.</b> 달성률·연속일을 만들지 않는 것이 명세의 「하지 않는 것」입니다.
 */
public interface LogActionRepository extends JpaRepository<TodayAction, String> {

    /** {@code ix_actions_user_created} 인덱스를 탑니다 */
    List<TodayAction> findByUserIdAndStatusAndCompletedAtBetweenOrderByCompletedAtDesc(
            String userId, String status, OffsetDateTime from, OffsetDateTime to);
}