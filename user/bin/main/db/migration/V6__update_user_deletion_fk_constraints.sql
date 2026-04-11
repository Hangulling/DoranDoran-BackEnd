-- ============================================
-- 사용자 완전 삭제를 위한 FK 제약 조건 변경
-- 작성일: 2026-01-28
-- 목적: 사용자 완전 삭제 시 연관 데이터를 안전하게 처리하기 위한 FK 제약 조건 변경
-- ============================================

-- 1. auth_schema.refresh_tokens: ON DELETE CASCADE
-- 리프레시 토큰은 유저 삭제 시 함께 삭제되어야 함
ALTER TABLE auth_schema.refresh_tokens
    DROP CONSTRAINT IF EXISTS fk5a9ypl7oycxycfscqnsepj5t8;

ALTER TABLE auth_schema.refresh_tokens
    ADD CONSTRAINT fk_refresh_tokens_user 
    FOREIGN KEY (user_id) 
    REFERENCES user_schema.app_user(id) 
    ON DELETE CASCADE;

-- 2. auth_schema.email_verifications: ON DELETE CASCADE
-- 이메일 인증 데이터는 유저 삭제 시 불필요하므로 삭제
ALTER TABLE auth_schema.email_verifications
    DROP CONSTRAINT IF EXISTS fk5vri8t8tr81le36apgppy94ch;

ALTER TABLE auth_schema.email_verifications
    ADD CONSTRAINT fk_email_verifications_user 
    FOREIGN KEY (user_id) 
    REFERENCES user_schema.app_user(id) 
    ON DELETE CASCADE;

-- 3. auth_schema.login_attempts: ON DELETE CASCADE
-- 로그인 시도 기록은 유저 삭제 시 불필요하므로 삭제
ALTER TABLE auth_schema.login_attempts
    DROP CONSTRAINT IF EXISTS fkeix6yqtdy2p9t2vj8hs6ji8of;

ALTER TABLE auth_schema.login_attempts
    ADD CONSTRAINT fk_login_attempts_user 
    FOREIGN KEY (user_id) 
    REFERENCES user_schema.app_user(id) 
    ON DELETE CASCADE;

-- 4. auth_schema.password_reset_tokens: ON DELETE CASCADE
-- 비밀번호 재설정 토큰은 유저 삭제 시 불필요하므로 삭제
ALTER TABLE auth_schema.password_reset_tokens
    DROP CONSTRAINT IF EXISTS fkj9so57i2ys7gbwiljqyrivrnb;

ALTER TABLE auth_schema.password_reset_tokens
    ADD CONSTRAINT fk_password_reset_tokens_user 
    FOREIGN KEY (user_id) 
    REFERENCES user_schema.app_user(id) 
    ON DELETE CASCADE;

-- 5. auth_schema.auth_events: ON DELETE SET NULL
-- 감사 로그는 법적 요구사항으로 보존 필요하지만, user_id는 NULL로 변경
ALTER TABLE auth_schema.auth_events
    DROP CONSTRAINT IF EXISTS fkq2wxphtsj555bl7w9yvcr08q8;

ALTER TABLE auth_schema.auth_events
    ADD CONSTRAINT fk_auth_events_user 
    FOREIGN KEY (user_id) 
    REFERENCES user_schema.app_user(id) 
    ON DELETE SET NULL;

-- 6. user_schema.profiles: ON DELETE CASCADE
-- 프로필은 유저와 함께 삭제되어야 함
ALTER TABLE user_schema.profiles
    DROP CONSTRAINT IF EXISTS fko9irkw5uae1s5s10pmstcvipw;

ALTER TABLE user_schema.profiles
    ADD CONSTRAINT fk_profiles_user 
    FOREIGN KEY (user_id) 
    REFERENCES user_schema.app_user(id) 
    ON DELETE CASCADE;

-- 7. user_schema.settings: ON DELETE CASCADE
-- 설정은 유저와 함께 삭제되어야 함
ALTER TABLE user_schema.settings
    DROP CONSTRAINT IF EXISTS fk5w7p1w60kfsalo61akkmfirv3;

ALTER TABLE user_schema.settings
    ADD CONSTRAINT fk_settings_user 
    FOREIGN KEY (user_id) 
    REFERENCES user_schema.app_user(id) 
    ON DELETE CASCADE;

-- 변경 사항 확인 쿼리 (주석 처리)
-- 다음 쿼리로 변경된 FK 제약 조건을 확인할 수 있습니다:
-- 
-- SELECT 
--     tc.table_schema,
--     tc.table_name,
--     tc.constraint_name,
--     rc.delete_rule
-- FROM information_schema.table_constraints tc
-- JOIN information_schema.referential_constraints rc 
--     ON tc.constraint_name = rc.constraint_name
-- WHERE tc.constraint_type = 'FOREIGN KEY'
--     AND rc.unique_constraint_name IN (
--         SELECT constraint_name 
--         FROM information_schema.table_constraints 
--         WHERE table_schema = 'user_schema' 
--             AND table_name = 'app_user'
--     )
-- ORDER BY tc.table_schema, tc.table_name;
