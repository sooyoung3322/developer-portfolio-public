CREATE TABLE portfolio_knowledge (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 category VARCHAR(64) NOT NULL, title VARCHAR(255) NOT NULL, content TEXT NOT NULL,
 project_slug VARCHAR(64), source_ref VARCHAR(255) NOT NULL, published BOOLEAN NOT NULL,
 created_at TIMESTAMP(6) NOT NULL, updated_at TIMESTAMP(6) NOT NULL
);
CREATE TABLE ai_chat_history (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, question TEXT NULL, answer TEXT NULL,
 model_name VARCHAR(128) NOT NULL, status VARCHAR(32) NOT NULL, category VARCHAR(64),
 prompt_tokens INT NULL, completion_tokens INT NULL, response_time_ms BIGINT NOT NULL,
 created_at TIMESTAMP(6) NOT NULL, expires_at TIMESTAMP(6) NOT NULL,
 INDEX idx_ai_history_expiry (expires_at)
);
CREATE TABLE ai_api_usage (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, usage_date DATE NOT NULL,
 model_name VARCHAR(128) NOT NULL,
 request_count BIGINT NOT NULL DEFAULT 0, admitted_count BIGINT NOT NULL DEFAULT 0,
 api_request_count BIGINT NOT NULL DEFAULT 0, success_count BIGINT NOT NULL DEFAULT 0,
 error_count BIGINT NOT NULL DEFAULT 0, rate_limited_count BIGINT NOT NULL DEFAULT 0,
 provider_rate_limited_count BIGINT NOT NULL DEFAULT 0, api_error_count BIGINT NOT NULL DEFAULT 0,
 no_context_count BIGINT NOT NULL DEFAULT 0, prompt_tokens BIGINT NULL, completion_tokens BIGINT NULL,
 token_reported_count BIGINT NOT NULL DEFAULT 0, response_time_ms BIGINT NOT NULL DEFAULT 0,
 completed_count BIGINT NOT NULL DEFAULT 0, updated_at TIMESTAMP(6) NOT NULL,
 UNIQUE KEY uk_ai_usage_day_model (usage_date,model_name)
);
CREATE TABLE ai_chat_guard (id BIGINT NOT NULL PRIMARY KEY, next_api_at TIMESTAMP(6) NULL);
INSERT INTO ai_chat_guard (id) VALUES (1);
