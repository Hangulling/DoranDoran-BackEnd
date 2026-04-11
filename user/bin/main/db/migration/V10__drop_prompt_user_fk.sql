-- 탈퇴 시 app_user 삭제 시 prompt_actives/prompt_versions에서 FK 위반 방지
-- FK 제거 후 activated_by, created_by 컬럼 값은 고아 ID로 남김 (이력 보존용)

ALTER TABLE user_schema.prompt_actives
    DROP CONSTRAINT IF EXISTS prompt_actives_activated_by_fkey;

ALTER TABLE user_schema.prompt_versions
    DROP CONSTRAINT IF EXISTS prompt_versions_created_by_fkey;
