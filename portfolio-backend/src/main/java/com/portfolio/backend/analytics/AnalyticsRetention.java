package com.portfolio.backend.analytics;

import java.time.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.*;
import org.springframework.transaction.annotation.Transactional;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "analytics.enabled", havingValue = "true")
public class AnalyticsRetention {
    private final AnalyticsRepository events;
    private final Clock clock;
    public AnalyticsRetention(AnalyticsRepository events, Clock clock) { this.events = events; this.clock = clock; }

    /** Bounded transaction; repeat each minute to drain backlog without deleting events. */
    @Scheduled(fixedDelayString = "${analytics.ip.cleanup-ms:60000}")
    @Transactional
    public int purge() { return events.clearExpiredIp(clock.instant()); }
}
