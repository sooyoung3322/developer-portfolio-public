CREATE TABLE analytics_guard (
    browser_hash CHAR(64) NOT NULL,
    scope VARCHAR(64) NOT NULL,
    last_recorded_at TIMESTAMP(6) NULL,
    PRIMARY KEY (browser_hash, scope)
);
CREATE TABLE analytics_event (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    scope VARCHAR(64) NOT NULL,
    occurred_at TIMESTAMP(6) NOT NULL,
    visit_date DATE NOT NULL,
    INDEX idx_analytics_scope_date (scope, visit_date),
    INDEX idx_analytics_date (visit_date)
);
