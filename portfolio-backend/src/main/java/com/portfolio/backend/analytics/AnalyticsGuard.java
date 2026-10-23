package com.portfolio.backend.analytics;

import jakarta.persistence.*;
import java.time.Duration;
import java.time.Instant;

@Entity
@Table(name = "analytics_guard")
public class AnalyticsGuard {
    @EmbeddedId
    private AnalyticsGuardId id;
    @Column(name = "last_recorded_at")
    private Instant lastRecordedAt;

    protected AnalyticsGuard() {}
    public AnalyticsGuardId getId() { return id; }
    public Instant getLastRecordedAt() { return lastRecordedAt; }
    public boolean canRecordAt(Instant now) {
        return lastRecordedAt == null || !now.isBefore(lastRecordedAt.plus(Duration.ofMinutes(30)));
    }
    public void markRecordedAt(Instant now) { lastRecordedAt = now; }
}
