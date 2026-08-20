package com.youin.now.checkin;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * {@code checkins} 접근. <b>이 저장소는 {@code checkin/} 밖에서 부르지 않습니다.</b>
 * 다른 패키지는 {@link CheckinPort} 를 씁니다.
 */
public interface CheckinRepository extends JpaRepository<Checkin, String> {

    /** 하루 한 건이라 사용자 + 날짜로 유일합니다. */
    Optional<Checkin> findByUserIdAndCheckDate(String userId, LocalDate checkDate);

    /** 가장 최근 것. 판정 전에 「상태 체크가 있는가」를 볼 때 씁니다. */
    Optional<Checkin> findTopByUserIdOrderByCheckDateDesc(String userId);

    Optional<Checkin> findByIdAndUserId(String id, String userId);

    /** 오늘의 행동 추천 중단 여부. {@code users.recommendation_paused}에서 읽습니다. */
    @Query(value = "select recommendation_paused from users where id = :userId and deleted_at is null",
            nativeQuery = true)
    boolean findRecommendationPausedByUserId(@Param("userId") String userId);
}
