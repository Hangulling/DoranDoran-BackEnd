-- 문의/신고, 관심 주제, 알림 설정, 통계, FCM, 게시글 캐시 추가

-- 문의/신고 통합 테이블
CREATE TABLE IF NOT EXISTS user_schema.support_requests (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL,
    requester_name VARCHAR(100),
    requester_email VARCHAR(320) NOT NULL,
    type VARCHAR(20) NOT NULL,
    category VARCHAR(100),
    content TEXT NOT NULL,
    reply_requested BOOLEAN NOT NULL DEFAULT FALSE,
    reply_email VARCHAR(320),
    chatroom_id UUID,
    message_id UUID,
    message_content TEXT,
    ai_response_snapshot JSONB,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_support_type CHECK (type IN ('INQUIRY', 'REPORT'))
);

ALTER TABLE IF EXISTS user_schema.support_requests
    ADD COLUMN IF NOT EXISTS requester_name VARCHAR(100);

CREATE INDEX IF NOT EXISTS idx_support_user_created
    ON user_schema.support_requests (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_support_type_created
    ON user_schema.support_requests (type, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_support_message_id
    ON user_schema.support_requests (message_id);

-- 관심 주제 마스터
CREATE TABLE IF NOT EXISTS user_schema.interest_topics (
    topic_key VARCHAR(50) PRIMARY KEY,
    label VARCHAR(100) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

-- 사용자 관심 주제 매핑
CREATE TABLE IF NOT EXISTS user_schema.user_interest_topics (
    user_id UUID NOT NULL,
    topic_key VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, topic_key)
);

CREATE INDEX IF NOT EXISTS idx_user_interest_topic_user
    ON user_schema.user_interest_topics (user_id);

-- 사용자 알림 설정
CREATE TABLE IF NOT EXISTS user_schema.user_notification_settings (
    user_id UUID PRIMARY KEY,
    push_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

-- 사용자 통계 (연속 접속/퍼펙트)
CREATE TABLE IF NOT EXISTS user_schema.user_stats (
    user_id UUID PRIMARY KEY,
    streak_count INTEGER NOT NULL DEFAULT 0,
    last_active_date DATE,
    perfect_count INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

-- FCM 토큰
CREATE TABLE IF NOT EXISTS user_schema.fcm_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    token TEXT NOT NULL,
    platform VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_fcm_user_token UNIQUE (user_id, token)
);

CREATE INDEX IF NOT EXISTS idx_fcm_user
    ON user_schema.fcm_tokens (user_id);

-- 메인홈 게시글 캐시 (인스타그램 피드)
CREATE TABLE IF NOT EXISTS user_schema.posts_cache (
    external_id VARCHAR(100) PRIMARY KEY,
    title TEXT,
    image_url TEXT,
    description TEXT,
    permalink TEXT,
    published_at TIMESTAMP WITHOUT TIME ZONE,
    fetched_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_posts_cache_fetched
    ON user_schema.posts_cache (fetched_at DESC);
