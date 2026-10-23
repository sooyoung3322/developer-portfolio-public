package com.portfolio.backend.analytics;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class AnalyticsGuardId implements Serializable {
    @Column(name = "browser_hash", nullable = false, length = 64, columnDefinition = "char(64)")
    private String browserHash;
    @Column(nullable = false, length = 64)
    private String scope;

    protected AnalyticsGuardId() {}
    public AnalyticsGuardId(String browserHash, String scope) {
        this.browserHash = browserHash;
        this.scope = scope;
    }
    public String getBrowserHash() { return browserHash; }
    public String getScope() { return scope; }
    @Override public boolean equals(Object other) {
        return this == other || other instanceof AnalyticsGuardId id
            && Objects.equals(browserHash, id.browserHash) && Objects.equals(scope, id.scope);
    }
    @Override public int hashCode() { return Objects.hash(browserHash, scope); }
}
