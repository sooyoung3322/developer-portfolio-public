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
    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false, length = 16)
    private DeviceType deviceType = DeviceType.UNKNOWN;
    @Column(name = "ip_token", length = 64) private String ipToken;
    @Column(name = "ip_expires_at") private Instant ipExpiresAt;
    protected AnalyticsEvent() {}
    AnalyticsEvent(String scope, Instant now) {
        this(scope, now, AnalyticsMetadata.Context.UNKNOWN);
    }
    AnalyticsEvent(String scope, Instant now, AnalyticsMetadata.Context context) {
        this.deviceType = context.device();
        this.ipToken = context.ipToken();
        this.ipExpiresAt = ipToken == null ? null : now.plus(Duration.ofDays(30));
        this.scope = scope; this.occurredAt = now;
        this.visitDate = now.atZone(ZoneId.of("Asia/Seoul")).toLocalDate();
    }
}
