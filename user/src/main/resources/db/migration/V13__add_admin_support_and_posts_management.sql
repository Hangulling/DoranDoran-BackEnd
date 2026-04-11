-- 관리자 문의내역/게시글관리 스키마 확장

-- 1) support_requests 확장 (답변/상태 관리)
ALTER TABLE user_schema.support_requests
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS answer_content TEXT,
    ADD COLUMN IF NOT EXISTS answered_by VARCHAR(100),
    ADD COLUMN IF NOT EXISTS answered_at TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITHOUT TIME ZONE;

ALTER TABLE user_schema.support_requests
    DROP CONSTRAINT IF EXISTS chk_support_status;
ALTER TABLE user_schema.support_requests
    ADD CONSTRAINT chk_support_status CHECK (status IN ('PENDING','COMPLETED'));

CREATE INDEX IF NOT EXISTS idx_support_status_created
    ON user_schema.support_requests (status, created_at DESC)
    WHERE deleted_at IS NULL;

-- 2) 관리자 운영 원본 게시글 테이블
CREATE TABLE IF NOT EXISTS user_schema.admin_home_posts (
    id              BIGSERIAL PRIMARY KEY,
    source_domain   VARCHAR(20) NOT NULL,
    external_id     VARCHAR(100),
    title           TEXT,
    description     TEXT,
    permalink       TEXT,
    media_type      VARCHAR(20),
    cover_image_url TEXT,
    assets          JSONB,
    is_main_home    BOOLEAN NOT NULL DEFAULT FALSE,
    display_order   INTEGER,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    published_at    TIMESTAMP WITHOUT TIME ZONE,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    created_at      TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT chk_admin_home_posts_source_domain CHECK (source_domain IN ('INSTAGRAM','FACEBOOK')),
    CONSTRAINT chk_admin_home_posts_media_type CHECK (media_type IS NULL OR media_type IN ('IMAGE','VIDEO','CAROUSEL_ALBUM'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_admin_home_posts_order
    ON user_schema.admin_home_posts (display_order)
    WHERE is_main_home = TRUE AND deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_admin_home_posts_main_order
    ON user_schema.admin_home_posts (is_main_home, display_order)
    WHERE deleted_at IS NULL AND is_active = TRUE;

CREATE INDEX IF NOT EXISTS idx_admin_home_posts_external_id
    ON user_schema.admin_home_posts (external_id)
    WHERE deleted_at IS NULL;
