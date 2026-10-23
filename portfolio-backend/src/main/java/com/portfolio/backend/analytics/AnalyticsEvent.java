package com.portfolio.backend.analytics;

import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "analytics_event")
public class AnalyticsEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 64) private String scope;
    @Column(name = "occurred_at", nullable = false) private Instant occurredAt;
    @Column(name = "visit_date", nullable = false) private LocalDate visitDate;
    protected AnalyticsEvent() {}
    AnalyticsEvent(String scope, Instant now) {
        this.scope = scope; this.occurredAt = now;
        this.visitDate = now.atZone(ZoneId.of("Asia/Seoul")).toLocalDate();
    }
}
