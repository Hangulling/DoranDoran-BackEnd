-- DoranDoran MSA 공유 데이터베이스 초기화 스크립트
-- 모든 서비스가 하나의 PostgreSQL 인스턴스를 공유하며 스키마로 분리

-- ========================================
-- 1. 스키마 생성
-- ========================================

-- Auth 스키마 (인증 관련)
CREATE SCHEMA IF NOT EXISTS auth_schema;

-- User 스키마 (사용자 프로필 관련)
CREATE SCHEMA IF NOT EXISTS user_schema;

-- Chat 스키마 (채팅 관련)
CREATE SCHEMA IF NOT EXISTS chat_schema;

-- Batch 스키마 (배치 관련)
CREATE SCHEMA IF NOT EXISTS batch_schema;

-- Store 스키마 (저장소 관련)
CREATE SCHEMA IF NOT EXISTS store;

-- ========================================
-- 2. 테이블 생성
-- ========================================

-- Chat 스키마의 챗봇 테이블
DROP TABLE IF EXISTS chat_schema.chatbot CASCADE;

CREATE TABLE chat_schema.chatbot
(
    bot_id    UUID NOT NULL,
    bot_type    character varying(50) NOT NULL,
    intimacy    integer NOT NULL,
    create_time    timestamp without time zone NOT NULL DEFAULT NOW(),
    update_time    timestamp without time zone DEFAULT NOW(),
    bot_img_url    character varying NOT NULL
);

COMMENT ON COLUMN chat_schema.chatbot.bot_id IS '챗봇 아이디';
COMMENT ON COLUMN chat_schema.chatbot.bot_type IS '타입';
COMMENT ON COLUMN chat_schema.chatbot.intimacy IS '친밀도';
COMMENT ON COLUMN chat_schema.chatbot.create_time IS '생성시간';
COMMENT ON COLUMN chat_schema.chatbot.update_time IS '업데이트 시간';
COMMENT ON COLUMN chat_schema.chatbot.bot_img_url IS '챗봇 이미지';
COMMENT ON TABLE chat_schema.chatbot IS '챗봇';

CREATE UNIQUE INDEX chatbot_PK ON chat_schema.chatbot (bot_id);
ALTER TABLE chat_schema.chatbot ADD CONSTRAINT chatbot_PK PRIMARY KEY USING INDEX chatbot_PK;

-- Chat 스키마의 채팅방 테이블
DROP TABLE IF EXISTS chat_schema.chatroom CASCADE;

CREATE TABLE chat_schema.chatroom
(
    room_id    UUID NOT NULL,
    create_at    timestamp without time zone NOT NULL DEFAULT NOW(),
    update_at    timestamp without time zone DEFAULT NOW(),
    room_name    character varying(50) NOT NULL,
    is_deleted    boolean DEFAULT 'true' NOT NULL,
    settings    json NOT NULL,
    user_id    UUID NOT NULL,
    bot_id    UUID NOT NULL
);

COMMENT ON COLUMN chat_schema.chatroom.room_id IS '채팅방 아이디';
COMMENT ON COLUMN chat_schema.chatroom.create_at IS '생성일시';
COMMENT ON COLUMN chat_schema.chatroom.update_at IS '마지막 대화 일시';
COMMENT ON COLUMN chat_schema.chatroom.room_name IS '채팅방 이름';
COMMENT ON COLUMN chat_schema.chatroom.is_deleted IS '소프트 삭제 여';
COMMENT ON COLUMN chat_schema.chatroom.settings IS '채팅방 설정';
COMMENT ON COLUMN chat_schema.chatroom.user_id IS '사용자 아이디';
COMMENT ON COLUMN chat_schema.chatroom.bot_id IS '챗봇 아이디';
COMMENT ON TABLE chat_schema.chatroom IS '채팅방';

CREATE UNIQUE INDEX chatroom_PK ON chat_schema.chatroom (bot_id, user_id, room_id);
ALTER TABLE chat_schema.chatroom ADD CONSTRAINT chatroom_PK PRIMARY KEY USING INDEX chatroom_PK;

-- Auth 스키마의 사용자 테이블 제거됨 (User 서비스 중심 구조로 변경)
-- 사용자 정보는 user_schema.app_user 테이블에서 관리

-- ================================
-- Auth 스키마 확장 테이블 (표준 구성)
-- ================================

-- 1) 리프레시 토큰: 저장/로테이션/기기별 관리
DROP TABLE IF EXISTS auth_schema.refresh_tokens CASCADE;
CREATE TABLE auth_schema.refresh_tokens (
    id                BIGSERIAL PRIMARY KEY,
    user_id           UUID NOT NULL,
    token             TEXT NOT NULL,                  -- 토큰 원문 대신 해시 저장 권장
    issued_at         TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    expires_at        TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    revoked           BOOLEAN NOT NULL DEFAULT FALSE,
    rotated_from_id   BIGINT,                         -- 로테이션 체인 추적
    device_id         VARCHAR(200),                   -- 디바이스 구분자
    user_agent        VARCHAR(500),
    ip_address        INET,
    CONSTRAINT fk_refresh_user
        FOREIGN KEY (user_id) REFERENCES user_schema.app_user(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_refresh_user_id ON auth_schema.refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_expires ON auth_schema.refresh_tokens(expires_at);
CREATE INDEX IF NOT EXISTS idx_refresh_revoked ON auth_schema.refresh_tokens(revoked);

-- 2) 토큰 블랙리스트: 액세스/리프레시 강제 무효화 목록
DROP TABLE IF EXISTS auth_schema.token_blacklist CASCADE;
CREATE TABLE auth_schema.token_blacklist (
    id           BIGSERIAL PRIMARY KEY,
    token_hash   VARCHAR(128) NOT NULL,               -- 토큰 해시(원문 저장 금지)
    token_type   VARCHAR(20) NOT NULL,                -- access|refresh
    reason       VARCHAR(200),
    created_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    expires_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_blacklist_token_hash ON auth_schema.token_blacklist(token_hash);
CREATE INDEX IF NOT EXISTS idx_blacklist_expires ON auth_schema.token_blacklist(expires_at);

-- 3) 로그인 시도 기록: 락/지연/레이트리밋 근거 데이터
DROP TABLE IF EXISTS auth_schema.login_attempts CASCADE;
CREATE TABLE auth_schema.login_attempts (
    id           BIGSERIAL PRIMARY KEY,
    user_id      UUID,
    email        VARCHAR(320),
    succeeded    BOOLEAN NOT NULL,
    ip_address   INET,
    user_agent   VARCHAR(500),
    created_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_login_attempt_user ON auth_schema.login_attempts(user_id, created_at);

-- 4) 인증 이벤트(감사 로그): 로그인/로그아웃/로테이션/비번변경 등 추적
DROP TABLE IF EXISTS auth_schema.auth_events CASCADE;
CREATE TABLE auth_schema.auth_events (
    id           BIGSERIAL PRIMARY KEY,
    user_id      UUID,
    event_type   VARCHAR(50) NOT NULL,                -- LOGIN, LOGOUT, TOKEN_ROTATE, PASSWORD_CHANGE 등
    metadata     JSONB,
    created_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_auth_events_user ON auth_schema.auth_events(user_id, created_at);

-- 5) 비밀번호 재설정 토큰
DROP TABLE IF EXISTS auth_schema.password_reset_tokens CASCADE;
CREATE TABLE auth_schema.password_reset_tokens (
    id           BIGSERIAL PRIMARY KEY,
    user_id      UUID NOT NULL,
    token_hash   VARCHAR(128) NOT NULL,
    expires_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    used         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_pwdreset_user
        FOREIGN KEY (user_id) REFERENCES user_schema.app_user(id) ON DELETE CASCADE
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_pwdreset_token_hash ON auth_schema.password_reset_tokens(token_hash);
CREATE INDEX IF NOT EXISTS idx_pwdreset_user ON auth_schema.password_reset_tokens(user_id);

-- 6) 이메일 인증 토큰
DROP TABLE IF EXISTS auth_schema.email_verifications CASCADE;
CREATE TABLE auth_schema.email_verifications (
    id           BIGSERIAL PRIMARY KEY,
    user_id      UUID NOT NULL,
    token_hash   VARCHAR(128) NOT NULL,
    expires_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    verified     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_emailver_user
        FOREIGN KEY (user_id) REFERENCES user_schema.app_user(id) ON DELETE CASCADE
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_emailver_token_hash ON auth_schema.email_verifications(token_hash);
CREATE INDEX IF NOT EXISTS idx_emailver_user ON auth_schema.email_verifications(user_id);

-- 7) 로그인 시도 기록
DROP TABLE IF EXISTS auth_schema.login_attempts CASCADE;
CREATE TABLE auth_schema.login_attempts (
    id           BIGSERIAL PRIMARY KEY,
    user_id      UUID,
    email        VARCHAR(320),
    succeeded    BOOLEAN NOT NULL,
    ip_address   VARCHAR(45),
    user_agent   VARCHAR(500),
    created_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_login_attempts_user ON auth_schema.login_attempts(user_id);
CREATE INDEX IF NOT EXISTS idx_login_attempts_email ON auth_schema.login_attempts(email);
CREATE INDEX IF NOT EXISTS idx_login_attempts_created ON auth_schema.login_attempts(created_at);

-- 8) 리프레시 토큰
DROP TABLE IF EXISTS auth_schema.refresh_tokens CASCADE;
CREATE TABLE auth_schema.refresh_tokens (
    id               BIGSERIAL PRIMARY KEY,
    user_id          UUID NOT NULL,
    token            TEXT NOT NULL,
    issued_at        TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    expires_at       TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    revoked          BOOLEAN NOT NULL DEFAULT FALSE,
    rotated_from_id  BIGINT,
    device_id        VARCHAR(200),
    user_agent       VARCHAR(500),
    ip_address       VARCHAR(45),
    CONSTRAINT fk_refresh_user
        FOREIGN KEY (user_id) REFERENCES user_schema.app_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_refresh_rotated
        FOREIGN KEY (rotated_from_id) REFERENCES auth_schema.refresh_tokens(id)
);
CREATE INDEX IF NOT EXISTS idx_refresh_user ON auth_schema.refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_expires ON auth_schema.refresh_tokens(expires_at);
CREATE INDEX IF NOT EXISTS idx_refresh_revoked ON auth_schema.refresh_tokens(revoked);

-- 9) 토큰 블랙리스트
DROP TABLE IF EXISTS auth_schema.token_blacklist CASCADE;
CREATE TABLE auth_schema.token_blacklist (
    id           BIGSERIAL PRIMARY KEY,
    token_hash   VARCHAR(128) NOT NULL UNIQUE,
    token_type   VARCHAR(20) NOT NULL,
    reason       VARCHAR(200),
    created_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    expires_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_blacklist_token_hash ON auth_schema.token_blacklist(token_hash);
CREATE INDEX IF NOT EXISTS idx_blacklist_expires ON auth_schema.token_blacklist(expires_at);

-- 10) 비밀번호 재설정 토큰
DROP TABLE IF EXISTS auth_schema.password_reset_tokens CASCADE;
CREATE TABLE auth_schema.password_reset_tokens (
    id           BIGSERIAL PRIMARY KEY,
    user_id      UUID NOT NULL,
    token_hash   VARCHAR(128) NOT NULL,
    expires_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    used         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_pwreset_user
        FOREIGN KEY (user_id) REFERENCES user_schema.app_user(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_pwreset_user ON auth_schema.password_reset_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_pwreset_expires ON auth_schema.password_reset_tokens(expires_at);
CREATE INDEX IF NOT EXISTS idx_pwreset_used ON auth_schema.password_reset_tokens(used);

-- 11) 인증 이벤트 로그
DROP TABLE IF EXISTS auth_schema.auth_events CASCADE;
CREATE TABLE auth_schema.auth_events (
    id           BIGSERIAL PRIMARY KEY,
    user_id      UUID,
    event_type   VARCHAR(50) NOT NULL,
    metadata     TEXT,
    created_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_auth_events_user ON auth_schema.auth_events(user_id);
CREATE INDEX IF NOT EXISTS idx_auth_events_type ON auth_schema.auth_events(event_type);
CREATE INDEX IF NOT EXISTS idx_auth_events_created ON auth_schema.auth_events(created_at);

-- User 스키마의 프로필 테이블 - 엔티티 기준으로 수정
DROP TABLE IF EXISTS user_schema.profiles CASCADE;

CREATE TABLE user_schema.profiles
(
    id    BIGSERIAL NOT NULL,
    user_id    UUID NOT NULL,
    bio    TEXT,
    avatar_url    character varying(500),
    settings    JSONB,
    created_at    timestamp without time zone NOT NULL DEFAULT NOW(),
    updated_at    timestamp without time zone NOT NULL DEFAULT NOW()
);

COMMENT ON COLUMN user_schema.profiles.id IS '프로필 아이디';
COMMENT ON COLUMN user_schema.profiles.user_id IS '사용자 아이디';
COMMENT ON COLUMN user_schema.profiles.bio IS '자기소개';
COMMENT ON COLUMN user_schema.profiles.avatar_url IS '아바타 URL';
COMMENT ON COLUMN user_schema.profiles.settings IS '사용자 설정';
COMMENT ON COLUMN user_schema.profiles.created_at IS '생성 시간';
COMMENT ON COLUMN user_schema.profiles.updated_at IS '수정 시간';
COMMENT ON TABLE user_schema.profiles IS '사용자 프로필';

CREATE UNIQUE INDEX profiles_PK ON user_schema.profiles (id);
CREATE UNIQUE INDEX profiles_user_id_idx ON user_schema.profiles (user_id);
ALTER TABLE user_schema.profiles ADD CONSTRAINT profiles_PK PRIMARY KEY USING INDEX profiles_PK;

-- User 스키마의 설정 테이블 - 엔티티 기준으로 수정
DROP TABLE IF EXISTS user_schema.settings CASCADE;

CREATE TABLE user_schema.settings
(
    id    BIGSERIAL NOT NULL,
    user_id    UUID NOT NULL,
    setting_key    character varying(100) NOT NULL,
    setting_value    TEXT,
    created_at    timestamp without time zone NOT NULL DEFAULT NOW(),
    updated_at    timestamp without time zone NOT NULL DEFAULT NOW()
);

COMMENT ON COLUMN user_schema.settings.id IS '설정 아이디';
COMMENT ON COLUMN user_schema.settings.user_id IS '사용자 아이디';
COMMENT ON COLUMN user_schema.settings.setting_key IS '설정 키';
COMMENT ON COLUMN user_schema.settings.setting_value IS '설정 값';
COMMENT ON COLUMN user_schema.settings.created_at IS '생성 시간';
COMMENT ON COLUMN user_schema.settings.updated_at IS '수정 시간';
COMMENT ON TABLE user_schema.settings IS '사용자 설정';

CREATE UNIQUE INDEX settings_PK ON user_schema.settings (id);
CREATE INDEX settings_user_id_idx ON user_schema.settings (user_id);
ALTER TABLE user_schema.settings ADD CONSTRAINT settings_PK PRIMARY KEY USING INDEX settings_PK;

-- User 스키마의 사용자 테이블 - User 엔티티에 맞춰 추가
DROP TABLE IF EXISTS user_schema.app_user CASCADE;

CREATE TABLE user_schema.app_user
(
    id    UUID NOT NULL,
    email    character varying(320) NOT NULL,
    first_name    character varying(50) NOT NULL,
    last_name    character varying(50) NOT NULL,
    name    character varying(50) NOT NULL,
    password_hash    character varying(100) NOT NULL,
    picture    character varying(1000),
    info    character varying(100) NOT NULL DEFAULT '',
    last_conn_time    timestamp without time zone NOT NULL DEFAULT NOW(),
    status    character varying(20) NOT NULL DEFAULT 'ACTIVE',
    role    character varying(20) NOT NULL DEFAULT 'ROLE_USER',
    coach_check    boolean NOT NULL DEFAULT FALSE,
    created_at    timestamp without time zone NOT NULL DEFAULT NOW(),
    updated_at    timestamp without time zone NOT NULL DEFAULT NOW()
);

COMMENT ON COLUMN user_schema.app_user.id IS '사용자 아이디';
COMMENT ON COLUMN user_schema.app_user.email IS '사용자 이메일';
COMMENT ON COLUMN user_schema.app_user.first_name IS '이름';
COMMENT ON COLUMN user_schema.app_user.last_name IS '성';
COMMENT ON COLUMN user_schema.app_user.name IS '전체 이름';
COMMENT ON COLUMN user_schema.app_user.password_hash IS '비밀번호 해시';
COMMENT ON COLUMN user_schema.app_user.picture IS '프로필 사진';
COMMENT ON COLUMN user_schema.app_user.info IS '사용자 정보';
COMMENT ON COLUMN user_schema.app_user.last_conn_time IS '마지막 연결 시간';
COMMENT ON COLUMN user_schema.app_user.status IS '사용자 상태 (ACTIVE, INACTIVE, SUSPENDED)';
COMMENT ON COLUMN user_schema.app_user.role IS '사용자 역할 (ROLE_USER, ROLE_ADMIN)';
COMMENT ON COLUMN user_schema.app_user.coach_check IS '코치 체크 여부';
COMMENT ON COLUMN user_schema.app_user.created_at IS '생성 시간';
COMMENT ON COLUMN user_schema.app_user.updated_at IS '수정 시간';
COMMENT ON TABLE user_schema.app_user IS '사용자 정보';

CREATE UNIQUE INDEX app_user_PK ON user_schema.app_user (id);
CREATE UNIQUE INDEX app_user_email_idx ON user_schema.app_user (email);
ALTER TABLE user_schema.app_user ADD CONSTRAINT app_user_PK PRIMARY KEY USING INDEX app_user_PK;

-- Chat 스키마의 메시지 테이블
DROP TABLE IF EXISTS chat_schema.message CASCADE;

CREATE TABLE chat_schema.message
(
    message_id    UUID NOT NULL,
    content    character varying(100) NOT NULL,
    sender_type    character varying NOT NULL,
    chat_num    integer NOT NULL,
    message_send_time    timestamp without time zone NOT NULL,
    message_type    character varying(10) NOT NULL,
    message_meta    json,
    room_id    UUID NOT NULL,
    bot_id    UUID NOT NULL,
    user_id    UUID NOT NULL,
    created_at    timestamp without time zone NOT NULL DEFAULT NOW(),
    updated_at    timestamp without time zone NOT NULL DEFAULT NOW()
);

COMMENT ON COLUMN chat_schema.message.message_id IS '메세지 아이디';
COMMENT ON COLUMN chat_schema.message.content IS '내용';
COMMENT ON COLUMN chat_schema.message.sender_type IS '발신자 타입';
COMMENT ON COLUMN chat_schema.message.chat_num IS '대화순번';
COMMENT ON COLUMN chat_schema.message.message_send_time IS '생성일시';
COMMENT ON COLUMN chat_schema.message.message_type IS '메세지 타입';
COMMENT ON COLUMN chat_schema.message.message_meta IS '메타데이터';
COMMENT ON COLUMN chat_schema.message.room_id IS '채팅방 아이디';
COMMENT ON COLUMN chat_schema.message.bot_id IS '챗봇 아이디';
COMMENT ON COLUMN chat_schema.message.user_id IS '사용자 아이디';
COMMENT ON TABLE chat_schema.message IS '메세지';

CREATE UNIQUE INDEX message_PK ON chat_schema.message (room_id, bot_id, user_id, message_id);
ALTER TABLE chat_schema.message ADD CONSTRAINT message_PK PRIMARY KEY USING INDEX message_PK;

-- Store 스키마의 저장소 테이블
DROP TABLE IF EXISTS store.store CASCADE;

CREATE TABLE store.store
(
    store_id    UUID NOT NULL,
    store_meta    json NOT NULL,
    store_content    character varying NOT NULL,
    message_id    UUID NOT NULL,
    user_id    UUID NOT NULL
);

COMMENT ON COLUMN store.store.store_id IS '보관함 아이디';
COMMENT ON COLUMN store.store.store_meta IS '메타데이터';
COMMENT ON COLUMN store.store.store_content IS '내용';
COMMENT ON COLUMN store.store.message_id IS '메세지 아이디';
COMMENT ON COLUMN store.store.user_id IS '사용자 아이디';
COMMENT ON TABLE store.store IS '보관함';

CREATE UNIQUE INDEX store_PK ON store.store (message_id, user_id, store_id);
ALTER TABLE store.store ADD CONSTRAINT store_PK PRIMARY KEY USING INDEX store_PK;

-- ========================================
-- 3. 권한 설정
-- ========================================

-- 각 스키마에 대한 권한 부여
GRANT USAGE ON SCHEMA auth_schema TO doran;
GRANT USAGE ON SCHEMA user_schema TO doran;
GRANT USAGE ON SCHEMA chat_schema TO doran;
GRANT USAGE ON SCHEMA batch_schema TO doran;
GRANT USAGE ON SCHEMA store TO doran;

-- 테이블 권한 부여
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA auth_schema TO doran;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA user_schema TO doran;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA chat_schema TO doran;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA batch_schema TO doran;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA store TO doran;

-- 시퀀스 권한 부여 (향후 추가될 경우)
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA auth_schema TO doran;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA user_schema TO doran;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA chat_schema TO doran;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA batch_schema TO doran;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA store TO doran;
