package com.portfolio.backend.analytics;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnalyticsRepository extends JpaRepository<AnalyticsEvent, Long> {
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
