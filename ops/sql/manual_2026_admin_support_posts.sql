-- =============================================================
-- 어드민 문의내역/게시글관리 스키마 확장 (2026)
-- 적용 방법:
--   scp -i "$env:USERPROFILE\Downloads\dorandoran-key.pem" ops/sql/manual_2026_admin_support_posts.sql ec2-user@3.21.177.186:/home/ec2-user/
--   cat /home/ec2-user/manual_2026_admin_support_posts.sql | docker exec -i dorandoran-shared-db psql -U doran -d dorandoran -v ON_ERROR_STOP=1
-- =============================================================

BEGIN;

-- ============================================================
-- 1) support_requests 확장 (답변/상태 관리)
-- ============================================================
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

-- ============================================================
-- 2) 관리자 운영 원본 게시글 테이블 (소셜 가져오기 전용)
-- ============================================================
CREATE TABLE IF NOT EXISTS user_schema.admin_home_posts (
    id             BIGSERIAL PRIMARY KEY,
    source_domain  VARCHAR(20) NOT NULL,              -- INSTAGRAM, FACEBOOK
    external_id    VARCHAR(100),                      -- 원본 소셜 media id
    title          TEXT,
    description    TEXT,
    permalink      TEXT,
    media_type     VARCHAR(20),                       -- IMAGE, VIDEO, CAROUSEL_ALBUM
    cover_image_url TEXT,
    assets         JSONB,
    is_main_home   BOOLEAN NOT NULL DEFAULT FALSE,
    display_order  INTEGER,
    is_active      BOOLEAN NOT NULL DEFAULT TRUE,
    published_at   TIMESTAMP WITHOUT TIME ZONE,
    created_by     VARCHAR(100),
    updated_by     VARCHAR(100),
    created_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at     TIMESTAMP WITHOUT TIME ZONE
);

ALTER TABLE user_schema.admin_home_posts
    DROP CONSTRAINT IF EXISTS chk_admin_home_posts_source_domain;
ALTER TABLE user_schema.admin_home_posts
    ADD CONSTRAINT chk_admin_home_posts_source_domain
    CHECK (source_domain IN ('INSTAGRAM','FACEBOOK'));

ALTER TABLE user_schema.admin_home_posts
    DROP CONSTRAINT IF EXISTS chk_admin_home_posts_media_type;
ALTER TABLE user_schema.admin_home_posts
    ADD CONSTRAINT chk_admin_home_posts_media_type
    CHECK (media_type IS NULL OR media_type IN ('IMAGE','VIDEO','CAROUSEL_ALBUM'));

CREATE UNIQUE INDEX IF NOT EXISTS uq_admin_home_posts_order
    ON user_schema.admin_home_posts (display_order)
    WHERE is_main_home = TRUE AND deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_admin_home_posts_main_order
    ON user_schema.admin_home_posts (is_main_home, display_order)
    WHERE deleted_at IS NULL AND is_active = TRUE;

CREATE INDEX IF NOT EXISTS idx_admin_home_posts_external_id
    ON user_schema.admin_home_posts (external_id)
    WHERE deleted_at IS NULL;

-- ============================================================
-- 3) 소셜 자동동기화 백업 테이블 (Instagram + Facebook 통합)
-- ============================================================
CREATE TABLE IF NOT EXISTS user_schema.social_posts_backup (
    id            BIGSERIAL PRIMARY KEY,
    source_domain VARCHAR(20) NOT NULL,               -- INSTAGRAM, FACEBOOK
    external_id   VARCHAR(100) NOT NULL,
    title         TEXT,
    image_url     TEXT,
    description   TEXT,
    permalink     TEXT,
    published_at  TIMESTAMP WITHOUT TIME ZONE,
    fetched_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    media_type    VARCHAR(20),
    cover_image_url TEXT,
    assets        JSONB,
    CONSTRAINT uq_social_posts_backup_domain_ext UNIQUE (source_domain, external_id)
);

CREATE INDEX IF NOT EXISTS idx_social_backup_fetched
    ON user_schema.social_posts_backup (fetched_at DESC);

CREATE INDEX IF NOT EXISTS idx_social_backup_domain
    ON user_schema.social_posts_backup (source_domain, fetched_at DESC);

-- ============================================================
-- 4) posts_cache -> social_posts_backup 1회 이관 (Instagram 기존 데이터)
-- ============================================================
INSERT INTO user_schema.social_posts_backup (
    source_domain, external_id, title, image_url, description,
    permalink, published_at, fetched_at, media_type, cover_image_url, assets
)
SELECT
    'INSTAGRAM',
    pc.external_id, pc.title, pc.image_url, pc.description,
    pc.permalink, pc.published_at, pc.fetched_at, pc.media_type,
    pc.cover_image_url, pc.assets
FROM user_schema.posts_cache pc
ON CONFLICT (source_domain, external_id) DO UPDATE
SET title           = EXCLUDED.title,
    image_url       = EXCLUDED.image_url,
    description     = EXCLUDED.description,
    permalink       = EXCLUDED.permalink,
    published_at    = EXCLUDED.published_at,
    fetched_at      = EXCLUDED.fetched_at,
    media_type      = EXCLUDED.media_type,
    cover_image_url = EXCLUDED.cover_image_url,
    assets          = EXCLUDED.assets;

COMMIT;
