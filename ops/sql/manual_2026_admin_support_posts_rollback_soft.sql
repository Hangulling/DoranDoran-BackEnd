-- =============================================================
-- 소프트 롤백 (데이터 보존, 기능 비활성화)
-- 주의: DROP TABLE은 포함하지 않음. 안정화 후 별도 진행.
-- 적용 방법:
--   cat /home/ec2-user/manual_2026_admin_support_posts_rollback_soft.sql | docker exec -i dorandoran-shared-db psql -U doran -d dorandoran -v ON_ERROR_STOP=1
-- =============================================================

BEGIN;

-- 1) 메인홈 노출 전부 off (데이터 보존)
UPDATE user_schema.admin_home_posts
SET is_main_home = FALSE,
    updated_at   = NOW()
WHERE deleted_at IS NULL;

-- 2) 제약 완화
DROP INDEX IF EXISTS user_schema.uq_admin_home_posts_order;

ALTER TABLE user_schema.admin_home_posts
    DROP CONSTRAINT IF EXISTS chk_admin_home_posts_media_type;
ALTER TABLE user_schema.admin_home_posts
    DROP CONSTRAINT IF EXISTS chk_admin_home_posts_source_domain;

-- 3) support 상태 기본값 원복
ALTER TABLE user_schema.support_requests
    ALTER COLUMN status SET DEFAULT 'PENDING';
ALTER TABLE user_schema.support_requests
    DROP CONSTRAINT IF EXISTS chk_support_status;

-- 4) social_posts_backup 유니크 제약 완화
ALTER TABLE user_schema.social_posts_backup
    DROP CONSTRAINT IF EXISTS uq_social_posts_backup_domain_ext;

COMMIT;
