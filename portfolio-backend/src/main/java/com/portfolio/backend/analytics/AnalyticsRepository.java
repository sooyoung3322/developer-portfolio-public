package com.portfolio.backend.analytics;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnalyticsRepository extends JpaRepository<AnalyticsEvent, Long> {
    List<AnalyticsEvent> findTop500ByIpExpiresAtLessThanEqualOrderByIpExpiresAtAsc(java.time.Instant cutoff);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true)
    @Query("update AnalyticsEvent e set e.ipToken = null, e.ipExpiresAt = null where e in :expired")
    int clearIp(@Param("expired") List<AnalyticsEvent> expired);

    default int clearExpiredIp(java.time.Instant cutoff) {
        var expired = findTop500ByIpExpiresAtLessThanEqualOrderByIpExpiresAtAsc(cutoff);
        return expired.isEmpty() ? 0 : clearIp(expired);
    }
    long countByScope(String scope);
    long countByScopeAndVisitDate(String scope, LocalDate visitDate);

    interface DailyCount {
        LocalDate getDate();
        long getVisits();
    }
    interface ProjectCount {
        String getScope();
        long getViews();
    }
    @Query("""
        select e.visitDate as date, count(e) as visits from AnalyticsEvent e
        where e.scope = 'visit' and e.visitDate between :start and :end
        group by e.visitDate
        """)
    List<DailyCount> countDailyVisits(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("""
        select e.scope as scope, count(e) as views from AnalyticsEvent e
        where e.scope in ('project-one', 'project-two', 'project-three', 'project-four', 'project-five', 'project-six', 'developer-portfolio') group by e.scope
        """)
    List<ProjectCount> countProjectViews();
}
