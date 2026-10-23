-- Preserve the historical 30-minute session metric separately from total visits.
-- Existing visit events remain untouched; pre-upgrade repeat visits cannot be recovered.
INSERT INTO analytics_event (scope, occurred_at, visit_date)
SELECT 'visit-session', occurred_at, visit_date FROM analytics_event WHERE scope = 'visit';
