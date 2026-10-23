package com.portfolio.backend.analytics;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface AnalyticsGuardRepository extends JpaRepository<AnalyticsGuard, AnalyticsGuardId> {
    long countByIdScope(String scope);

    // JPA cannot lock a row that does not exist. The unique PK and atomic upsert
    // serialize creation, including requests from different application instances.
    // Unlike INSERT IGNORE this does not suppress unrelated insert errors.
    @Modifying
    @Query(value = """
        INSERT INTO analytics_guard (browser_hash, scope) VALUES (:browserHash, :scope)
        ON DUPLICATE KEY UPDATE browser_hash = analytics_guard.browser_hash
        """, nativeQuery = true)
    void ensureExists(@Param("browserHash") String browserHash, @Param("scope") String scope);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AnalyticsGuard> findLockedById(AnalyticsGuardId id);
}
