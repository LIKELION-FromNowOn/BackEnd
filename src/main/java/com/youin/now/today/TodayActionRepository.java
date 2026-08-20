package com.youin.now.today;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface TodayActionRepository extends JpaRepository<TodayAction, String> {

    /** 오늘 살아 있는 행동. 자정에 만료되므로 하루에 하나입니다 */
    Optional<TodayAction> findByUserIdAndExpiresAtAfter(String userId, OffsetDateTime now);

    /** 최근 제안 문장 — 중복 방지용. {@code ix_actions_user_created} 인덱스를 씁니다 */
    List<TodayAction> findTop10ByUserIdOrderByCreatedAtDesc(String userId);
}