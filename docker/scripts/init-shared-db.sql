-- DoranDoran MSA 공유 데이터베이스 초기화 스크립트
-- 모든 서비스가 하나의 PostgreSQL 인스턴스를 공유하며 스키마로 분리

-- ========================================
-- 1. 스키마 생성
-- ========================================

-- UUID 생성 등을 위한 확장
CREATE EXTENSION IF NOT EXISTS pgcrypto;

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

-- User 스키마의 사용자 테이블 - 먼저 생성 (다른 테이블들이 참조)
DROP TABLE IF EXISTS user_schema.app_user CASCADE;

CREATE TABLE user_schema.app_user
(
    id    UUID NOT NULL DEFAULT gen_random_uuid(),
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
-- 값 제한: 사용자 상태/역할
ALTER TABLE user_schema.app_user
    ADD CONSTRAINT chk_app_user_status
    CHECK (status IN ('ACTIVE','INACTIVE','SUSPENDED'));
ALTER TABLE user_schema.app_user
    ADD CONSTRAINT chk_app_user_role
    CHECK (role IN ('ROLE_USER','ROLE_ADMIN'));

-- Chat 스키마의 챗봇 테이블 (단순화 버전)
DROP TABLE IF EXISTS chat_schema.chatbots CASCADE;

CREATE TABLE chat_schema.chatbots
(
    id    UUID NOT NULL DEFAULT gen_random_uuid(),
    name    character varying(100) NOT NULL,
    display_name    character varying(100) NOT NULL,
    description    text,
    bot_type    character varying(50) NOT NULL,
    model_name    character varying(100),
    personality    jsonb,
    system_prompt    text,
    capabilities    jsonb,
    settings    jsonb,
    intimacy_level    integer DEFAULT 0 CHECK (intimacy_level >= 0 AND intimacy_level <= 100),
    avatar_url    character varying(500),
    is_active    boolean DEFAULT true,
    created_at    timestamp without time zone DEFAULT NOW(),
    updated_at    timestamp without time zone DEFAULT NOW(),
    created_by    UUID REFERENCES user_schema.app_user(id) ON DELETE SET NULL
);

COMMENT ON COLUMN chat_schema.chatbots.id IS '챗봇 아이디';
COMMENT ON COLUMN chat_schema.chatbots.name IS '챗봇 이름';
COMMENT ON COLUMN chat_schema.chatbots.display_name IS '표시 이름';
COMMENT ON COLUMN chat_schema.chatbots.description IS '챗봇 설명';
COMMENT ON COLUMN chat_schema.chatbots.bot_type IS '챗봇 타입 (gpt, claude, custom)';
COMMENT ON COLUMN chat_schema.chatbots.model_name IS 'AI 모델명';
COMMENT ON COLUMN chat_schema.chatbots.personality IS '챗봇 성격 설정 (JSONB)';
COMMENT ON COLUMN chat_schema.chatbots.system_prompt IS '시스템 프롬프트';
COMMENT ON COLUMN chat_schema.chatbots.capabilities IS '챗봇 기능 설정 (JSONB)';
COMMENT ON COLUMN chat_schema.chatbots.settings IS '챗봇 설정 (JSONB)';
COMMENT ON COLUMN chat_schema.chatbots.intimacy_level IS '친밀도 레벨 (0-100)';
COMMENT ON COLUMN chat_schema.chatbots.avatar_url IS '아바타 URL';
COMMENT ON COLUMN chat_schema.chatbots.is_active IS '활성 상태';
COMMENT ON COLUMN chat_schema.chatbots.created_at IS '생성 시간';
COMMENT ON COLUMN chat_schema.chatbots.updated_at IS '수정 시간';
COMMENT ON COLUMN chat_schema.chatbots.created_by IS '생성자';
COMMENT ON TABLE chat_schema.chatbots IS 'AI 챗봇';

CREATE UNIQUE INDEX chatbots_PK ON chat_schema.chatbots (id);
ALTER TABLE chat_schema.chatbots ADD CONSTRAINT chatbots_PK PRIMARY KEY USING INDEX chatbots_PK;
-- 값 제한: 챗봇 타입
ALTER TABLE chat_schema.chatbots
    ADD CONSTRAINT chk_chatbots_type
    CHECK (bot_type IN ('gpt','claude','custom'));

-- 인덱스
CREATE INDEX idx_chatbots_type ON chat_schema.chatbots(bot_type);
CREATE INDEX idx_chatbots_active ON chat_schema.chatbots(is_active);
CREATE INDEX idx_chatbots_created_by ON chat_schema.chatbots(created_by);

-- Chat 스키마의 채팅방 테이블 (단순화 버전)
DROP TABLE IF EXISTS chat_schema.chatrooms CASCADE;

CREATE TABLE chat_schema.chatrooms
(
    id    UUID NOT NULL DEFAULT gen_random_uuid(),
    name    character varying(100) NOT NULL,
    description    text,
    chatbot_id    UUID NOT NULL REFERENCES chat_schema.chatbots(id) ON DELETE CASCADE,
    user_id    UUID NOT NULL REFERENCES user_schema.app_user(id) ON DELETE CASCADE,
    settings    jsonb DEFAULT '{}',
    context_data    jsonb,
    last_message_at    timestamp without time zone,
    last_message_id    UUID,
    is_archived    boolean DEFAULT false,
    is_deleted    boolean DEFAULT false,
    created_at    timestamp without time zone DEFAULT NOW(),
    updated_at    timestamp without time zone DEFAULT NOW()
);

COMMENT ON COLUMN chat_schema.chatrooms.id IS '채팅방 아이디';
COMMENT ON COLUMN chat_schema.chatrooms.name IS '채팅방 이름';
COMMENT ON COLUMN chat_schema.chatrooms.description IS '채팅방 설명';
COMMENT ON COLUMN chat_schema.chatrooms.chatbot_id IS '챗봇 아이디';
COMMENT ON COLUMN chat_schema.chatrooms.user_id IS '사용자 아이디';
COMMENT ON COLUMN chat_schema.chatrooms.settings IS '채팅방 설정 (JSONB)';
COMMENT ON COLUMN chat_schema.chatrooms.context_data IS '대화 컨텍스트 데이터 (JSONB)';
COMMENT ON COLUMN chat_schema.chatrooms.last_message_at IS '마지막 메시지 시간';
COMMENT ON COLUMN chat_schema.chatrooms.last_message_id IS '마지막 메시지 아이디';
COMMENT ON COLUMN chat_schema.chatrooms.is_archived IS '아카이브 여부';
COMMENT ON COLUMN chat_schema.chatrooms.is_deleted IS '삭제 여부';
COMMENT ON COLUMN chat_schema.chatrooms.created_at IS '생성 시간';
COMMENT ON COLUMN chat_schema.chatrooms.updated_at IS '수정 시간';
COMMENT ON TABLE chat_schema.chatrooms IS '채팅방';

CREATE UNIQUE INDEX chatrooms_PK ON chat_schema.chatrooms (id);
ALTER TABLE chat_schema.chatrooms ADD CONSTRAINT chatrooms_PK PRIMARY KEY USING INDEX chatrooms_PK;

-- 인덱스
CREATE UNIQUE INDEX idx_chatrooms_user_chatbot ON chat_schema.chatrooms(user_id, chatbot_id) WHERE NOT is_deleted;
CREATE INDEX idx_chatrooms_user ON chat_schema.chatrooms(user_id);
CREATE INDEX idx_chatrooms_chatbot ON chat_schema.chatrooms(chatbot_id);
CREATE INDEX idx_chatrooms_last_message ON chat_schema.chatrooms(last_message_at DESC);

-- Auth 스키마의 사용자 테이블 제거됨 (User 서비스 중심 구조로 변경)
-- 사용자 정보는 user_schema.app_user 테이블에서 관리

-- ================================
-- Auth 스키마 확장 테이블 (표준 구성)
-- ================================

-- (정리됨) 리프레시 토큰 정의는 하단 최신 버전으로 통일

-- (정리됨) 토큰 블랙리스트 정의는 하단 최신 버전으로 통일

-- (정리됨) 로그인 시도 기록 정의는 하단 최신 버전으로 통일

-- (정리됨) 인증 이벤트 정의는 하단 최신 버전으로 통일

-- 5) 이메일 인증 토큰 (아래 정의 참조)

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
ALTER TABLE auth_schema.login_attempts
    ADD CONSTRAINT fk_login_attempts_user
    FOREIGN KEY (user_id)
    REFERENCES user_schema.app_user(id)
    ON DELETE SET NULL;

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
        FOREIGN KEY (rotated_from_id) REFERENCES auth_schema.refresh_tokens(id) ON DELETE SET NULL
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
    metadata     JSONB,
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
ALTER TABLE user_schema.profiles
    ADD CONSTRAINT fk_profiles_user
    FOREIGN KEY (user_id)
    REFERENCES user_schema.app_user(id)
    ON DELETE CASCADE;

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
CREATE UNIQUE INDEX uq_settings_user_key ON user_schema.settings(user_id, setting_key);
ALTER TABLE user_schema.settings
    ADD CONSTRAINT fk_settings_user
    FOREIGN KEY (user_id)
    REFERENCES user_schema.app_user(id)
    ON DELETE CASCADE;


-- Chat 스키마의 메시지 테이블 (단순화 버전)
DROP TABLE IF EXISTS chat_schema.messages CASCADE;

CREATE TABLE chat_schema.messages
(
    id    UUID NOT NULL DEFAULT gen_random_uuid(),
    chatroom_id    UUID NOT NULL REFERENCES chat_schema.chatrooms(id) ON DELETE CASCADE,
    sender_type    character varying(20) NOT NULL CHECK (sender_type IN ('user', 'bot', 'system')),
    sender_id    UUID,
    content    text NOT NULL,
    content_type    character varying(20) DEFAULT 'text' CHECK (content_type IN ('text', 'code', 'system')),
    metadata    jsonb,
    parent_message_id    UUID,
    sequence_number    bigint NOT NULL,
    token_count    integer,
    processing_time_ms    integer,
    is_edited    boolean DEFAULT false,
    edited_at    timestamp without time zone,
    is_deleted    boolean DEFAULT false,
    deleted_at    timestamp without time zone,
    created_at    timestamp without time zone DEFAULT NOW(),
    updated_at    timestamp without time zone DEFAULT NOW()
);

COMMENT ON COLUMN chat_schema.messages.id IS '메시지 아이디';
COMMENT ON COLUMN chat_schema.messages.chatroom_id IS '채팅방 아이디';
COMMENT ON COLUMN chat_schema.messages.sender_type IS '발신자 타입 (user, bot, system)';
COMMENT ON COLUMN chat_schema.messages.sender_id IS '발신자 아이디';
COMMENT ON COLUMN chat_schema.messages.content IS '메시지 내용';
COMMENT ON COLUMN chat_schema.messages.content_type IS '콘텐츠 타입 (text, code, system)';
COMMENT ON COLUMN chat_schema.messages.metadata IS '메타데이터 (JSONB)';
COMMENT ON COLUMN chat_schema.messages.parent_message_id IS '부모 메시지 아이디';
COMMENT ON COLUMN chat_schema.messages.sequence_number IS '대화 순서 번호';
COMMENT ON COLUMN chat_schema.messages.token_count IS '토큰 수';
COMMENT ON COLUMN chat_schema.messages.processing_time_ms IS '처리 시간 (밀리초)';
COMMENT ON COLUMN chat_schema.messages.is_edited IS '수정 여부';
COMMENT ON COLUMN chat_schema.messages.edited_at IS '수정 시간';
COMMENT ON COLUMN chat_schema.messages.is_deleted IS '삭제 여부';
COMMENT ON COLUMN chat_schema.messages.deleted_at IS '삭제 시간';
COMMENT ON COLUMN chat_schema.messages.created_at IS '생성 시간';
COMMENT ON COLUMN chat_schema.messages.updated_at IS '수정 시간';
COMMENT ON TABLE chat_schema.messages IS '메시지';

CREATE UNIQUE INDEX messages_PK ON chat_schema.messages (id);
ALTER TABLE chat_schema.messages ADD CONSTRAINT messages_PK PRIMARY KEY USING INDEX messages_PK;

-- 채팅방의 마지막 메시지 참조 무결성: 메시지 삭제 시 포인터 NULL 처리
ALTER TABLE chat_schema.chatrooms
    ADD CONSTRAINT fk_chatrooms_last_message
    FOREIGN KEY (last_message_id)
    REFERENCES chat_schema.messages(id)
    ON DELETE SET NULL;

-- 인덱스
CREATE INDEX idx_messages_chatroom ON chat_schema.messages(chatroom_id, sequence_number);
ALTER TABLE chat_schema.messages ADD CONSTRAINT uq_messages_room_seq UNIQUE (chatroom_id, sequence_number);
CREATE INDEX idx_messages_sender ON chat_schema.messages(sender_id);
CREATE INDEX idx_messages_created_at ON chat_schema.messages(created_at);
CREATE INDEX idx_messages_parent ON chat_schema.messages(parent_message_id);
ALTER TABLE chat_schema.messages
    ADD CONSTRAINT fk_messages_parent
    FOREIGN KEY (parent_message_id)
    REFERENCES chat_schema.messages(id)
    ON DELETE SET NULL;

-- Store 스키마는 단순화 버전에서 제거됨
-- 파일 첨부 기능이 제거되어 보관함 기능도 불필요

-- ========================================
-- 3. 권한 설정
-- ========================================

-- ========================================
-- Billing 스키마 (AI 사용량/비용)
-- ========================================

CREATE SCHEMA IF NOT EXISTS billing;

-- 원본 이벤트 테이블 (월 파티셔닝 권장)
DROP TABLE IF EXISTS billing.ai_usage_events CASCADE;
CREATE TABLE billing.ai_usage_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_time TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    user_id UUID NOT NULL,
    chatroom_id UUID NOT NULL,
    provider TEXT NOT NULL,
    model TEXT NOT NULL,
    request_id TEXT UNIQUE,
    input_tokens INT NOT NULL DEFAULT 0,
    output_tokens INT NOT NULL DEFAULT 0,
    cost_in NUMERIC(18,6) NOT NULL DEFAULT 0,
    cost_out NUMERIC(18,6) NOT NULL DEFAULT 0,
    meta JSONB
);
CREATE INDEX idx_ai_usage_events_user_time ON billing.ai_usage_events(user_id, event_time);
CREATE INDEX idx_ai_usage_events_time ON billing.ai_usage_events(event_time);
-- 외부 엔터티 삭제 시 이벤트 참조를 끊기 위해 SET NULL
ALTER TABLE billing.ai_usage_events
    ADD CONSTRAINT fk_ai_usage_events_user
    FOREIGN KEY (user_id)
    REFERENCES user_schema.app_user(id)
    ON DELETE SET NULL;
ALTER TABLE billing.ai_usage_events
    ADD CONSTRAINT fk_ai_usage_events_chatroom
    FOREIGN KEY (chatroom_id)
    REFERENCES chat_schema.chatrooms(id)
    ON DELETE SET NULL;

-- 월 집계 테이블
DROP TABLE IF EXISTS billing.monthly_user_costs CASCADE;
CREATE TABLE billing.monthly_user_costs (
    billing_month DATE NOT NULL,
    user_id UUID NOT NULL,
    input_tokens BIGINT NOT NULL DEFAULT 0,
    output_tokens BIGINT NOT NULL DEFAULT 0,
    cost_in NUMERIC(18,6) NOT NULL DEFAULT 0,
    cost_out NUMERIC(18,6) NOT NULL DEFAULT 0,
    total_cost NUMERIC(18,6) NOT NULL DEFAULT 0,
    last_aggregated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT monthly_user_costs_pk PRIMARY KEY (billing_month, user_id)
);
CREATE INDEX idx_monthly_user_costs_month ON billing.monthly_user_costs(billing_month);

GRANT USAGE ON SCHEMA billing TO doran;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA billing TO doran;

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
