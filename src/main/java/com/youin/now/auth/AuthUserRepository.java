package com.youin.now.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * {@code users} 접근. <b>이 저장소는 {@code auth/} 밖에서 부르지 않습니다.</b>
 * 다른 패키지가 사용자 정보를 필요로 하면 인터페이스를 하나 열어 주십시오.
 */
public interface AuthUserRepository extends JpaRepository<AuthUser, String> {

    /** 지운 사용자는 없는 것으로 봅니다. */
    Optional<AuthUser> findByIdAndDeletedAtIsNull(String id);

    Optional<AuthUser> findByEmailAndDeletedAtIsNull(String email);

    @Query(value = "select count(*) from user_items where user_id = :userId and deleted_at is null", nativeQuery = true)
    long countActiveItems(@Param("userId") String userId);

    @Query(value = """
            select (select count(*) from user_items where user_id = :userId and deleted_at is null) as items,
                   (select count(*) from checkins where user_id = :userId) as checkins,
                   (select count(*) from daily_logs where user_id = :userId) as logs
            """, nativeQuery = true)
    MigrationCounts countMigratedData(@Param("userId") String userId);

    interface MigrationCounts {
        long getItems();
        long getCheckins();
        long getLogs();
    }
}
