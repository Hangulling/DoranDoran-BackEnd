-- user_schema 컬럼 정의 동기화
-- 서버 DB 기준으로 timestamp 타입 정밀도, DEFAULT 값, NOT NULL 제약 동기화

-- app_user 테이블
ALTER TABLE user_schema.app_user 
ALTER COLUMN password_hash DROP NOT NULL,
ALTER COLUMN created_at TYPE timestamp without time zone,
ALTER COLUMN updated_at TYPE timestamp without time zone,
ALTER COLUMN last_conn_time TYPE timestamp without time zone,
ALTER COLUMN id SET DEFAULT gen_random_uuid(),
ALTER COLUMN coach_check SET DEFAULT false,
ALTER COLUMN info SET DEFAULT ''::character varying,
ALTER COLUMN role SET DEFAULT 'ROLE_USER'::character varying,
ALTER COLUMN status SET DEFAULT 'ACTIVE'::character varying;

-- 제약조건 이름 통일 (서버 기준)
DO $$
BEGIN
    -- 기존 제약조건이 있으면 삭제
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'app_user_status_check') THEN
        ALTER TABLE user_schema.app_user DROP CONSTRAINT app_user_status_check;
    END IF;
END $$;

-- 서버 기준 제약조건 추가
ALTER TABLE user_schema.app_user 
ADD CONSTRAINT chk_app_user_status CHECK (((status)::text = ANY (ARRAY[('ACTIVE'::character varying)::text, ('INACTIVE'::character varying)::text, ('SUSPENDED'::character varying)::text])));

-- 서버에만 있는 인덱스 추가
CREATE UNIQUE INDEX IF NOT EXISTS app_user_email_idx ON user_schema.app_user USING btree (email);

-- profiles 테이블
ALTER TABLE user_schema.profiles 
ALTER COLUMN created_at TYPE timestamp(6) without time zone,
ALTER COLUMN updated_at TYPE timestamp(6) without time zone,
ALTER COLUMN created_at DROP DEFAULT,
ALTER COLUMN updated_at DROP DEFAULT;

-- settings 테이블
ALTER TABLE user_schema.settings 
ALTER COLUMN created_at TYPE timestamp(6) without time zone,
ALTER COLUMN updated_at TYPE timestamp(6) without time zone,
ALTER COLUMN created_at DROP DEFAULT,
ALTER COLUMN updated_at DROP DEFAULT;
