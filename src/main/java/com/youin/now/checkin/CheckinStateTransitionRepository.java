package com.youin.now.checkin;

import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CheckinStateTransitionRepository extends JpaRepository<CheckinStateTransition, String> {
    Optional<CheckinStateTransition> findTopByUserIdAndAcceptedIsNullOrderByCreatedAtDesc(String userId);
    Optional<CheckinStateTransition> findTopByUserIdAndAcceptedFalseAndRespondedAtIsNotNullOrderByRespondedAtDesc(String userId);

    void deleteByUserIdAndAcceptedIsNull(String userId);

    @Modifying
    @Query(value = "update users set recommendation_paused = :paused where id = :userId and deleted_at is null", nativeQuery = true)
    void updateRecommendationPaused(@Param("userId") String userId, @Param("paused") boolean paused);
}
