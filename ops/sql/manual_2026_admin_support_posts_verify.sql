-- =============================================================
-- 스키마 적용 검증 쿼리
-- 적용 방법:
--   cat /home/ec2-user/manual_2026_admin_support_posts_verify.sql | docker exec -i dorandoran-shared-db psql -U doran -d dorandoran
-- =============================================================

-- A. 신규 테이블 존재 확인
SELECT table_schema, table_name
FROM information_schema.tables
WHERE table_schema = 'user_schema'
  AND table_name IN ('admin_home_posts', 'social_posts_backup', 'support_requests')
ORDER BY table_name;

-- B. support_requests 신규 컬럼 확인
SELECT column_name, data_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_schema = 'user_schema'
  AND table_name = 'support_requests'
  AND column_name IN ('status','answer_content','answered_by','answered_at','deleted_at')
ORDER BY column_name;

-- C. admin_home_posts / social_posts_backup 제약 확인
SELECT t.relname AS table_name, conname, pg_get_constraintdef(c.oid) AS definition
FROM pg_constraint c
JOIN pg_class t ON c.conrelid = t.oid
JOIN pg_namespace n ON t.relnamespace = n.oid
WHERE n.nspname = 'user_schema'
  AND t.relname IN ('admin_home_posts','social_posts_backup','support_requests')
ORDER BY t.relname, conname;

-- D. 인덱스 확인
SELECT tablename, indexname, indexdef
FROM pg_indexes
WHERE schemaname = 'user_schema'
  AND tablename IN ('admin_home_posts','social_posts_backup','support_requests')
ORDER BY tablename, indexname;

-- E. 데이터 건수 확인
SELECT
  (SELECT count(*) FROM user_schema.posts_cache)                                                   AS posts_cache_count,
  (SELECT count(*) FROM user_schema.social_posts_backup)                                           AS backup_total,
  (SELECT count(*) FROM user_schema.social_posts_backup WHERE source_domain = 'INSTAGRAM')        AS backup_instagram,
  (SELECT count(*) FROM user_schema.social_posts_backup WHERE source_domain = 'FACEBOOK')         AS backup_facebook,
  (SELECT count(*) FROM user_schema.admin_home_posts WHERE deleted_at IS NULL)                     AS admin_posts_active,
  (SELECT count(*) FROM user_schema.support_requests WHERE deleted_at IS NULL)                     AS support_active;
