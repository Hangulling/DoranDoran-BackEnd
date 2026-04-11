-- 소셜 자동동기화 백업 테이블 (Instagram/Facebook 통합)

CREATE TABLE IF NOT EXISTS user_schema.social_posts_backup (
    id              BIGSERIAL PRIMARY KEY,
    source_domain   VARCHAR(20) NOT NULL,
    external_id     VARCHAR(100) NOT NULL,
    title           TEXT,
    image_url       TEXT,
    description     TEXT,
    permalink       TEXT,
    published_at    TIMESTAMP WITHOUT TIME ZONE,
    fetched_at      TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    media_type      VARCHAR(20),
    cover_image_url TEXT,
    assets          JSONB,
    CONSTRAINT uq_social_posts_backup_domain_ext UNIQUE (source_domain, external_id)
);

CREATE INDEX IF NOT EXISTS idx_social_backup_fetched
    ON user_schema.social_posts_backup (fetched_at DESC);

CREATE INDEX IF NOT EXISTS idx_social_backup_domain
    ON user_schema.social_posts_backup (source_domain, fetched_at DESC);

-- posts_cache -> social_posts_backup 1회 이관
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
ON CONFLICT (source_domain, external_id) DO NOTHING;
