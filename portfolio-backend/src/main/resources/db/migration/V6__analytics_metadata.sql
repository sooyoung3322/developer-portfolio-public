-- Additive only: retain every historical event, receipt, and session count.
-- Historical network/device information cannot be reconstructed.
ALTER TABLE analytics_event ADD COLUMN device_type VARCHAR(16) NOT NULL DEFAULT 'UNKNOWN';
ALTER TABLE analytics_event ADD COLUMN ip_token VARCHAR(64) NULL;
ALTER TABLE analytics_event ADD COLUMN ip_expires_at TIMESTAMP(6) NULL;
CREATE INDEX idx_analytics_ip_expiry ON analytics_event (ip_expires_at);
