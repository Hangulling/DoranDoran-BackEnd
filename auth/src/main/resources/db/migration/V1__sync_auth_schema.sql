-- auth_schema 컬럼 정의 동기화
-- 서버 DB 기준으로 timestamp 타입 정밀도 및 DEFAULT 값 동기화

-- auth_events 테이블
ALTER TABLE auth_schema.auth_events 
ALTER COLUMN created_at TYPE timestamp(6) without time zone;

-- email_verifications 테이블
ALTER TABLE auth_schema.email_verifications 
ALTER COLUMN created_at TYPE timestamp(6) without time zone,
ALTER COLUMN expires_at TYPE timestamp(6) without time zone;

-- login_attempts 테이블
ALTER TABLE auth_schema.login_attempts 
ALTER COLUMN created_at TYPE timestamp(6) without time zone;

-- password_reset_tokens 테이블
ALTER TABLE auth_schema.password_reset_tokens 
ALTER COLUMN created_at TYPE timestamp(6) without time zone,
ALTER COLUMN expires_at TYPE timestamp(6) without time zone,
ALTER COLUMN used DROP DEFAULT;

-- refresh_tokens 테이블
ALTER TABLE auth_schema.refresh_tokens 
ALTER COLUMN issued_at TYPE timestamp(6) without time zone,
ALTER COLUMN expires_at TYPE timestamp(6) without time zone,
ALTER COLUMN revoked DROP DEFAULT;

-- token_blacklist 테이블
ALTER TABLE auth_schema.token_blacklist 
ALTER COLUMN created_at TYPE timestamp(6) without time zone,
ALTER COLUMN expires_at TYPE timestamp(6) without time zone;
