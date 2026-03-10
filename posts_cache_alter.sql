ALTER TABLE user_schema.posts_cache
    ADD COLUMN IF NOT EXISTS media_type VARCHAR(20),
    ADD COLUMN IF NOT EXISTS cover_image_url TEXT,
    ADD COLUMN IF NOT EXISTS assets JSONB;

COMMENT ON COLUMN user_schema.posts_cache.media_type IS 'IMAGE, VIDEO, CAROUSEL_ALBUM';
COMMENT ON COLUMN user_schema.posts_cache.cover_image_url IS '리스트 썸네일/대표 이미지 URL';
COMMENT ON COLUMN user_schema.posts_cache.assets IS '[{ "type": "IMAGE"|"VIDEO", "url": "...", "thumbnailUrl": "..." }]';
