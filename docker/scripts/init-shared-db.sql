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
    info    character varying(100) NOT NULL DEFAULT '''',
    last_conn_time    timestamp without time zone NOT NULL DEFAULT NOW(),
    status    character varying(20) NOT NULL DEFAULT ''ACTIVE'',
    role    character varying(20) NOT NULL DEFAULT ''ROLE_USER'',
    coach_check    boolean NOT NULL DEFAULT FALSE,
    created_at    timestamp without time zone NOT NULL DEFAULT NOW(),
    updated_at    timestamp without time zone NOT NULL DEFAULT NOW()
);

COMMENT ON COLUMN user_schema.app_user.id IS ''사용자 아이디'';
COMMENT ON COLUMN user_schema.app_user.email IS ''사용자 이메일'';
COMMENT ON COLUMN user_schema.app_user.first_name IS ''이름'';
COMMENT ON COLUMN user_schema.app_user.last_name IS ''성'';
COMMENT ON COLUMN user_schema.app_user.name IS ''전체 이름'';
COMMENT ON COLUMN user_schema.app_user.password_hash IS ''비밀번호 해시'';
COMMENT ON COLUMN user_schema.app_user.picture IS ''프로필 사진'';
COMMENT ON COLUMN user_schema.app_user.info IS ''사용자 정보'';
COMMENT ON COLUMN user_schema.app_user.last_conn_time IS ''마지막 연결 시간'';
COMMENT ON COLUMN user_schema.app_user.status IS ''사용자 상태 (ACTIVE, INACTIVE, SUSPENDED)'';
COMMENT ON COLUMN user_schema.app_user.role IS ''사용자 역할 (ROLE_USER, ROLE_ADMIN)'';
COMMENT ON COLUMN user_schema.app_user.coach_check IS ''코치 체크 여부'';
COMMENT ON COLUMN user_schema.app_user.created_at IS ''생성 시간'';
COMMENT ON COLUMN user_schema.app_user.updated_at IS ''수정 시간'';
COMMENT ON TABLE user_schema.app_user IS ''사용자 정보'';

CREATE UNIQUE INDEX app_user_PK ON user_schema.app_user (id);
CREATE UNIQUE INDEX app_user_email_idx ON user_schema.app_user (email);
ALTER TABLE user_schema.app_user ADD CONSTRAINT app_user_PK PRIMARY KEY USING INDEX app_user_PK;
-- 값 제한: 사용자 상태/역할
ALTER TABLE user_schema.app_user
    ADD CONSTRAINT chk_app_user_status
    CHECK (status IN (''ACTIVE'',''INACTIVE'',''SUSPENDED''));
ALTER TABLE user_schema.app_user
    ADD CONSTRAINT chk_app_user_role
    CHECK (role IN (''ROLE_USER'',''ROLE_ADMIN''));

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
    -- 각 Agent별 프롬프트 필드
    intimacy_system_prompt    text,
    intimacy_user_prompt    text,
    vocabulary_system_prompt    text,
    vocabulary_user_prompt    text,
    translation_system_prompt    text,
    translation_user_prompt    text,
    capabilities    jsonb,
    settings    jsonb,
    intimacy_level    integer DEFAULT 1 CHECK (intimacy_level >= 1 AND intimacy_level <= 3),
    avatar_url    character varying(500),
    is_active    boolean DEFAULT true,
    created_at    timestamp without time zone DEFAULT NOW(),
    updated_at    timestamp without time zone DEFAULT NOW(),
    created_by    UUID
    -- Foreign Key 제약 조건 제거 (마이크로서비스 아키텍처에 맞게 수정)
);

COMMENT ON COLUMN chat_schema.chatbots.id IS ''챗봇 아이디'';
COMMENT ON COLUMN chat_schema.chatbots.name IS ''챗봇 이름'';
COMMENT ON COLUMN chat_schema.chatbots.display_name IS ''표시 이름'';
COMMENT ON COLUMN chat_schema.chatbots.description IS ''챗봇 설명'';
COMMENT ON COLUMN chat_schema.chatbots.bot_type IS ''챗봇 타입 (gpt, claude, custom)'';
COMMENT ON COLUMN chat_schema.chatbots.model_name IS ''AI 모델명'';
COMMENT ON COLUMN chat_schema.chatbots.personality IS ''챗봇 성격 설정 (JSONB)'';
COMMENT ON COLUMN chat_schema.chatbots.system_prompt IS ''시스템 프롬프트'';
COMMENT ON COLUMN chat_schema.chatbots.capabilities IS ''챗봇 기능 설정 (JSONB)'';
COMMENT ON COLUMN chat_schema.chatbots.settings IS ''챗봇 설정 (JSONB)'';
COMMENT ON COLUMN chat_schema.chatbots.intimacy_level IS ''친밀도 레벨 (1=격식체, 2=부드러운 존댓말, 3=반말)'';
COMMENT ON COLUMN chat_schema.chatbots.avatar_url IS ''아바타 URL'';
COMMENT ON COLUMN chat_schema.chatbots.is_active IS ''활성 상태'';
COMMENT ON COLUMN chat_schema.chatbots.created_at IS ''생성 시간'';
COMMENT ON COLUMN chat_schema.chatbots.updated_at IS ''수정 시간'';
COMMENT ON COLUMN chat_schema.chatbots.created_by IS ''생성자'';
COMMENT ON TABLE chat_schema.chatbots IS ''AI 챗봇'';

CREATE UNIQUE INDEX chatbots_PK ON chat_schema.chatbots (id);
ALTER TABLE chat_schema.chatbots ADD CONSTRAINT chatbots_PK PRIMARY KEY USING INDEX chatbots_PK;
-- 값 제한: 챗봇 타입
ALTER TABLE chat_schema.chatbots
    ADD CONSTRAINT chk_chatbots_type
    CHECK (bot_type IN (''gpt'',''claude'',''custom''));

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
    chatbot_id    UUID NOT NULL,
    user_id    UUID NOT NULL,
    settings    jsonb DEFAULT ''{}'',
    context_data    jsonb,
    last_message_at    timestamp without time zone,
    last_message_id    UUID,
    is_archived    boolean DEFAULT false,
    is_deleted    boolean DEFAULT false,
    created_at    timestamp without time zone DEFAULT NOW(),
    updated_at    timestamp without time zone DEFAULT NOW()
    -- Foreign Key 제약 조건 제거 (마이크로서비스 아키텍처에 맞게 수정)
);

COMMENT ON COLUMN chat_schema.chatrooms.id IS ''채팅방 아이디'';
COMMENT ON COLUMN chat_schema.chatrooms.name IS ''채팅방 이름'';
COMMENT ON COLUMN chat_schema.chatrooms.description IS ''채팅방 설명'';
COMMENT ON COLUMN chat_schema.chatrooms.chatbot_id IS ''챗봇 아이디'';
COMMENT ON COLUMN chat_schema.chatrooms.user_id IS ''사용자 아이디'';
COMMENT ON COLUMN chat_schema.chatrooms.settings IS ''채팅방 설정 (JSONB)'';
COMMENT ON COLUMN chat_schema.chatrooms.context_data IS ''대화 컨텍스트 데이터 (JSONB)'';
COMMENT ON COLUMN chat_schema.chatrooms.last_message_at IS ''마지막 메시지 시간'';
COMMENT ON COLUMN chat_schema.chatrooms.last_message_id IS ''마지막 메시지 아이디'';
COMMENT ON COLUMN chat_schema.chatrooms.is_archived IS ''아카이브 여부'';
COMMENT ON COLUMN chat_schema.chatrooms.is_deleted IS ''삭제 여부'';
COMMENT ON COLUMN chat_schema.chatrooms.created_at IS ''생성 시간'';
COMMENT ON COLUMN chat_schema.chatrooms.updated_at IS ''수정 시간'';
COMMENT ON TABLE chat_schema.chatrooms IS ''채팅방'';

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
    created_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
    -- Foreign Key 제약 조건 제거 (마이크로서비스 아키텍처에 맞게 수정)
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
-- Foreign Key 제약 조건 제거 (마이크로서비스 아키텍처에 맞게 수정)

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
    -- Foreign Key 제약 조건 제거 (마이크로서비스 아키텍처에 맞게 수정)
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
    created_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
    -- Foreign Key 제약 조건 제거 (마이크로서비스 아키텍처에 맞게 수정)
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

COMMENT ON COLUMN user_schema.profiles.id IS ''프로필 아이디'';
COMMENT ON COLUMN user_schema.profiles.user_id IS ''사용자 아이디'';
COMMENT ON COLUMN user_schema.profiles.bio IS ''자기소개'';
COMMENT ON COLUMN user_schema.profiles.avatar_url IS ''아바타 URL'';
COMMENT ON COLUMN user_schema.profiles.settings IS ''사용자 설정'';
COMMENT ON COLUMN user_schema.profiles.created_at IS ''생성 시간'';
COMMENT ON COLUMN user_schema.profiles.updated_at IS ''수정 시간'';
COMMENT ON TABLE user_schema.profiles IS ''사용자 프로필'';

CREATE UNIQUE INDEX profiles_PK ON user_schema.profiles (id);
CREATE UNIQUE INDEX profiles_user_id_idx ON user_schema.profiles (user_id);
ALTER TABLE user_schema.profiles ADD CONSTRAINT profiles_PK PRIMARY KEY USING INDEX profiles_PK;
-- Foreign Key 제약 조건 제거 (마이크로서비스 아키텍처에 맞게 수정)

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

COMMENT ON COLUMN user_schema.settings.id IS ''설정 아이디'';
COMMENT ON COLUMN user_schema.settings.user_id IS ''사용자 아이디'';
COMMENT ON COLUMN user_schema.settings.setting_key IS ''설정 키'';
COMMENT ON COLUMN user_schema.settings.setting_value IS ''설정 값'';
COMMENT ON COLUMN user_schema.settings.created_at IS ''생성 시간'';
COMMENT ON COLUMN user_schema.settings.updated_at IS ''수정 시간'';
COMMENT ON TABLE user_schema.settings IS ''사용자 설정'';

CREATE UNIQUE INDEX settings_PK ON user_schema.settings (id);
CREATE INDEX settings_user_id_idx ON user_schema.settings (user_id);
ALTER TABLE user_schema.settings ADD CONSTRAINT settings_PK PRIMARY KEY USING INDEX settings_PK;
CREATE UNIQUE INDEX uq_settings_user_key ON user_schema.settings(user_id, setting_key);
-- Foreign Key 제약 조건 제거 (마이크로서비스 아키텍처에 맞게 수정)


-- Chat 스키마의 메시지 테이블 (단순화 버전)
DROP TABLE IF EXISTS chat_schema.messages CASCADE;

CREATE TABLE chat_schema.messages
(
    id    UUID NOT NULL DEFAULT gen_random_uuid(),
    chatroom_id    UUID NOT NULL,
    sender_type    character varying(20) NOT NULL CHECK (sender_type IN (''user'', ''bot'', ''system'')),
    sender_id    UUID,
    content    text NOT NULL,
    content_type    character varying(20) DEFAULT ''text'' CHECK (content_type IN (''text'', ''code'', ''system'', ''json'')),
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
    -- Foreign Key 제약 조건 제거 (마이크로서비스 아키텍처에 맞게 수정)
);

COMMENT ON COLUMN chat_schema.messages.id IS ''메시지 아이디'';
COMMENT ON COLUMN chat_schema.messages.chatroom_id IS ''채팅방 아이디'';
COMMENT ON COLUMN chat_schema.messages.sender_type IS ''발신자 타입 (user, bot, system)'';
COMMENT ON COLUMN chat_schema.messages.sender_id IS ''발신자 아이디'';
COMMENT ON COLUMN chat_schema.messages.content IS ''메시지 내용'';
COMMENT ON COLUMN chat_schema.messages.content_type IS ''콘텐츠 타입 (text, code, system)'';
COMMENT ON COLUMN chat_schema.messages.metadata IS ''메타데이터 (JSONB)'';
COMMENT ON COLUMN chat_schema.messages.parent_message_id IS ''부모 메시지 아이디'';
COMMENT ON COLUMN chat_schema.messages.sequence_number IS ''대화 순서 번호'';
COMMENT ON COLUMN chat_schema.messages.token_count IS ''토큰 수'';
COMMENT ON COLUMN chat_schema.messages.processing_time_ms IS ''처리 시간 (밀리초)'';
COMMENT ON COLUMN chat_schema.messages.is_edited IS ''수정 여부'';
COMMENT ON COLUMN chat_schema.messages.edited_at IS ''수정 시간'';
COMMENT ON COLUMN chat_schema.messages.is_deleted IS ''삭제 여부'';
COMMENT ON COLUMN chat_schema.messages.deleted_at IS ''삭제 시간'';
COMMENT ON COLUMN chat_schema.messages.created_at IS ''생성 시간'';
COMMENT ON COLUMN chat_schema.messages.updated_at IS ''수정 시간'';
COMMENT ON TABLE chat_schema.messages IS ''메시지'';

CREATE UNIQUE INDEX messages_PK ON chat_schema.messages (id);
ALTER TABLE chat_schema.messages ADD CONSTRAINT messages_PK PRIMARY KEY USING INDEX messages_PK;

-- Foreign Key 제약 조건 제거 (마이크로서비스 아키텍처에 맞게 수정)
-- 채팅방의 마지막 메시지 참조 무결성: 메시지 삭제 시 포인터 NULL 처리

-- 인덱스
CREATE INDEX idx_messages_chatroom ON chat_schema.messages(chatroom_id, sequence_number);
ALTER TABLE chat_schema.messages ADD CONSTRAINT uq_messages_room_seq UNIQUE (chatroom_id, sequence_number);
CREATE INDEX idx_messages_sender ON chat_schema.messages(sender_id);
CREATE INDEX idx_messages_created_at ON chat_schema.messages(created_at);
CREATE INDEX idx_messages_parent ON chat_schema.messages(parent_message_id);
-- Foreign Key 제약 조건 제거 (마이크로서비스 아키텍처에 맞게 수정)

-- ===========================================================
-- Store Schema 생성
-- ===========================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE SCHEMA IF NOT EXISTS store_schema;

-- ============================================================
-- Stores 테이블 (보관함)
-- ============================================================
DROP TABLE IF EXISTS store_schema.stores CASCADE;

CREATE TABLE store_schema.stores (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    message_id UUID NOT NULL,
    chatroom_id UUID NOT NULL,
    content TEXT NOT NULL,
    ai_response JSONB NOT NULL,
    bot_type VARCHAR(20),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE store_schema.stores IS ''보관함 - 사용자가 저장한 표현과 AI 응답'';
COMMENT ON COLUMN store_schema.stores.content IS ''표현 원본'';
COMMENT ON COLUMN store_schema.stores.ai_response IS ''Multi-Agent AI 응답 (JSONB)'';
COMMENT ON COLUMN store_schema.stores.bot_type IS ''챗봇 역할 (Honey, Coworker, Senior, Client)'';

-- 인덱스
CREATE UNIQUE INDEX idx_store_user_message
    ON store_schema.stores(user_id, message_id)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_store_user_created
    ON store_schema.stores(user_id, created_at DESC)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_store_chatroom
    ON store_schema.stores(chatroom_id, created_at DESC)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_store_ai_response_gin
    ON store_schema.stores USING GIN (ai_response);


-- ========================================
-- 친밀도 진척 추적 테이블 (Multi-Agent AI)
-- ========================================

-- 친밀도 진척 추적 (채팅방별)
DROP TABLE IF EXISTS chat_schema.intimacy_progress CASCADE;
CREATE TABLE chat_schema.intimacy_progress (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chatroom_id UUID NOT NULL,
    user_id UUID NOT NULL,
    intimacy_level INTEGER NOT NULL DEFAULT 1 CHECK (intimacy_level IN (1, 2, 3)),
    total_corrections INTEGER DEFAULT 0,
    last_feedback TEXT,
    last_updated TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    progress_data JSONB,
    CONSTRAINT uq_intimacy_chatroom UNIQUE (chatroom_id)
    -- Foreign Key 제약 조건 제거 (마이크로서비스 아키텍처에 맞게 수정)
);

CREATE INDEX idx_intimacy_progress_chatroom ON chat_schema.intimacy_progress(chatroom_id);
CREATE INDEX idx_intimacy_progress_user ON chat_schema.intimacy_progress(user_id);

COMMENT ON TABLE chat_schema.intimacy_progress IS ''채팅방별 친밀도 진척 추적'';
COMMENT ON COLUMN chat_schema.intimacy_progress.intimacy_level IS ''현재 친밀도 레벨 (1=격식체, 2=부드러운 존댓말, 3=반말)'';
COMMENT ON COLUMN chat_schema.intimacy_progress.total_corrections IS ''누적 교정 횟수'';
COMMENT ON COLUMN chat_schema.intimacy_progress.last_feedback IS ''마지막 피드백 메시지'';
COMMENT ON COLUMN chat_schema.intimacy_progress.progress_data IS ''세부 학습 통계 (JSONB)'';

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
-- Foreign Key 제약 조건 제거 (마이크로서비스 아키텍처에 맞게 수정)
-- 외부 엔터티 삭제 시 이벤트 참조를 끊기 위해 SET NULL

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

-- ========================================
-- 4. 테스트 데이터 생성
-- ========================================

-- 테스트 사용자 10명 생성 (user_schema)
INSERT INTO user_schema.app_user (id, email, name, first_name, last_name, info, password_hash, role, status, coach_check, created_at, updated_at, last_conn_time) VALUES
(''11111111-1111-1111-1111-111111111111'', ''test1@example.com'', ''테스트1'', ''테스트'', ''1'', ''테스트 사용자 1'', ''$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi'', ''ROLE_USER'', ''ACTIVE'', false, NOW(), NOW(), NOW()),
(''11111111-1111-1111-1111-111111111112'', ''test2@example.com'', ''테스트2'', ''테스트'', ''2'', ''테스트 사용자 2'', ''$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi'', ''ROLE_USER'', ''ACTIVE'', false, NOW(), NOW(), NOW()),
(''11111111-1111-1111-1111-111111111113'', ''test3@example.com'', ''테스트3'', ''테스트'', ''3'', ''테스트 사용자 3'', ''$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi'', ''ROLE_USER'', ''ACTIVE'', false, NOW(), NOW(), NOW()),
(''11111111-1111-1111-1111-111111111114'', ''test4@example.com'', ''테스트4'', ''테스트'', ''4'', ''테스트 사용자 4'', ''$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi'', ''ROLE_USER'', ''ACTIVE'', false, NOW(), NOW(), NOW()),
(''11111111-1111-1111-1111-111111111115'', ''test5@example.com'', ''테스트5'', ''테스트'', ''5'', ''테스트 사용자 5'', ''$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi'', ''ROLE_USER'', ''ACTIVE'', false, NOW(), NOW(), NOW()),
(''11111111-1111-1111-1111-111111111116'', ''test6@example.com'', ''테스트6'', ''테스트'', ''6'', ''테스트 사용자 6'', ''$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi'', ''ROLE_USER'', ''ACTIVE'', false, NOW(), NOW(), NOW()),
(''11111111-1111-1111-1111-111111111117'', ''test7@example.com'', ''테스트7'', ''테스트'', ''7'', ''테스트 사용자 7'', ''$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi'', ''ROLE_USER'', ''ACTIVE'', false, NOW(), NOW(), NOW()),
(''11111111-1111-1111-1111-111111111118'', ''test8@example.com'', ''테스트8'', ''테스트'', ''8'', ''테스트 사용자 8'', ''$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi'', ''ROLE_USER'', ''ACTIVE'', false, NOW(), NOW(), NOW()),
(''11111111-1111-1111-1111-111111111119'', ''test9@example.com'', ''테스트9'', ''테스트'', ''9'', ''테스트 사용자 9'', ''$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi'', ''ROLE_USER'', ''ACTIVE'', false, NOW(), NOW(), NOW()),
(''11111111-1111-1111-1111-11111111111a'', ''test10@example.com'', ''테스트10'', ''테스트'', ''10'', ''테스트 사용자 10'', ''$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi'', ''ROLE_USER'', ''ACTIVE'', false, NOW(), NOW(), NOW());

-- auth_schema에는 app_user 테이블이 없으므로 user_schema만 사용

-- 기본 챗봇 5개 생성 (concept별)
INSERT INTO chat_schema.chatbots (id, name, display_name, description, bot_type, model_name, personality, system_prompt, intimacy_system_prompt, intimacy_user_prompt, vocabulary_system_prompt, vocabulary_user_prompt, translation_system_prompt, translation_user_prompt, capabilities, settings, intimacy_level, avatar_url, is_active, created_by, created_at, updated_at) VALUES
(''22222222-2222-2222-2222-222222222221'', ''friend-bot'', ''친구 봇'', ''친구처럼 편안하게 대화하는 AI 튜터'', ''gpt'', ''gpt-4o-mini'', ''{"personality": "friendly", "tone": "casual"}'', ''당신은 친구처럼 편안하고 친근한 한국어 학습 AI 튜터입니다. 격식 없이 대화하며 자연스럽게 한국어를 가르쳐주세요.'', ''**친구 ver 0.1**

**역할 설명:**

너는 지금 사용자와 **친구** **관계**야. 사용자가 설정한 친밀도 레벨(intimacy_level)에 맞게 사용자의 문장(userMessage)을 교정해줘야 해.

친구 관계에서는 **자연스러운 표현, 편안한 어조, 감정의 거리 조절**

이 중요하며, 친밀도에 따라 **말투의 솔직함·장난스러움·격식의 유무**

가 달라져야 해. 너의 역할은 사용자의 문장을 해당 관계와 친밀도에 맞게 교정하여, **자연스럽고 공감되는 대화 톤**으로 만들어주는 거야.

**입력 정보:**

- 채팅방 ID : {chatroomId}
- 친밀도: {intimacy_level} (0 (감지 불가/예외), 1 (초기 친분 / 깔끔한 반말), 2 (편한 친구), 3 (아주 친한 친구 / 찐친 말투))
- 사용자 문장 : {userMessage}

**친밀도 레벨 기준(Intimacy Level Guide)**

- **Level 1**
    - **어미/표현 예시:** "~하자", "~할래?", "~그럴까?", "좋아?", "괜찮아?"
    - **설명:** 아직은 약간의 거리감이 있는 친구 사이. 예의는 남아 있지만 서로를 탐색하며 자연스럽게 말하는 단계. 문장은 명확하고 깔끔한 반말 형태.
- **Level 2**
    - **어미/표현 예시:** "~하장", "~드실?", "~하실?", "ㅎㅎ", "좋지!", "언제 볼까?"
    - **설명:** 서로 익숙해진 친구 사이. 부드러운 존댓말이나 줄임말, 감탄사 등을 섞어 가볍고 자연스럽게 표현하는 단계.
- **Level 3**
    - **어미/표현 예시:** "~야", "~해", "~지?", "ㅋㅋ", "그러셈", "ㄱㄱ", "개좋지!"
    - **설명:** 아주 친한 친구 사이. 반말과 속어, 인터넷식 표현, 이모티콘 등을 자유롭게 쓰는 단계. 말투가 짧고 장난스럽고, 감정 표현이 솔직하게 드러남. 속어, 줄임말 자유롭게 사용함

**교정 기준:**

- 친밀도에 맞는 적절한 표현 사용
- 상황에 맞는 자연스러운 문장으로 수정

**응답 형식:**

다음 JSON 형식으로 정확히 답변하세요:

{
 "detectedLevel" : "AI가 감지한 친밀도 (0~3)",
        "corrections" : "AI가 교정 과정에서 인식한 문장 단위 수정 내역/이유",
"feedback" : {
"ko": "교정 이유 설명 (100자 내외, 한국어)",
"en": "English explanation of the correction (within 100 words)"
},

        "correctedSentence" : "교정된 문장 또는 ''''perfect''''",

}

**주의사항:**

- feedback은 문법이나 친밀도에 따른 표현상의 특징을 간단히 설명,
- feedback은 수정한 이유만 적어줄 것,
- feedback은 반드시 ko와 en 두 개의 필드로 구성할 것,
- JSON 형식 외의 텍스트는 출력하지 말 것.
- AI가 문체를 위 세 가지 친밀도 1~3레벨 중 어디에도 명확히 분류하지 못할 경우, "detectedLevel": 0을 반환할 것
- 입력값 intimacy_level과 감지값 detectedLevel이 다를 경우 교정할 것
- 입력값인 intimacy_level과 감지값인 detectedLevel이 일치할 경우, feedback 없이 "correctedSentence": "perfect"를 반환할 것

**예시 시나리오:**

**입력 정보:**

{
"intimacy_level": 1,
"userMessage": "좋아요. 언제 드실래요?"
}

**응답 형식:**

{
"detectedLevel": 0,
        "corrections": "''''좋아요. 언제 드실래요?'''' → ''''좋아. 언제 먹을래?'''' 로 변경",
"feedback": {
        "ko": "친구 사이에는 ''''-요''''나 ''''드실래요''''와 같은 격식 표현을 쓰지 않아요.",
        "en": "Between friends, people usually don''''t use formal endings like ''''-요'''' or ''''드실래요''''.."
},
"correctedSentence": "좋아. 언제 먹을래?"
}

---

**입력 정보:**

{
"intimacy_level": 2,
"userMessage": "좋아요. 언제 드실래요?"
}

**응답 형식:**

{
"detectedLevel": 0,
        "corrections": "좋아요 언제 드실래요? → 좋지!ㅎㅎ 언제 드실?로 변경 ",
"feedback": {
        "ko": "**감탄사 좋지!로 친근함을 주고, 드실?로 부드러운 예의를 표현했어.**

**한국에서는 조금 친한 친구나 선배에게도 이렇게 가볍게 존댓말을 섞어 따뜻하게 말해**",
        "en": "**The exclamation 좋지! adds friendliness, and 드실? shows soft politeness.**

**In Korean, people often mix light honorifics like this when speaking to somewhat close friends or seniors to sound warm yet respectful.**"
},
"correctedSentence": "좋지! 언제 드실?"
}

---

**입력 정보:**

{
"intimacy_level": 3,
"userMessage": "좋아요. 언제 드실래요?"
}

**응답 형식:**

{
"detectedLevel": 0,
        "corrections": "좋아요 언제 드실래요? → 개굿ㅋㅋ 언제?로 변경 ",
"feedback": {
        "ko": "**좋다는 말은 원래 굿이라고도 줄여 말해. 근데 진짜 좋을 땐 앞에 개를 붙여서 강조해. 그리고 친할수록 서술어는 생략하고 짧게 던져!**"
        "en": "**In casual close friendships, people shorten 좋아요 to just 굿. If they want to emphasize, they add 개 in front, meaning super. Also, the closer you are, the shorter and more direct your sentences get.**"
},
"correctedSentence": "개굿ㅋㅋ 언제?"
}

---'', ''다음 한국어 문장의 친밀도를 분석하고, 더 적절한 친밀도로 수정해주세요: {input}'', ''외국인이 이해하기 어려운 한국어 단어/표현을 최대 1개 추출하세요. 반드시 1개만 추출하세요.

추출 기준:
- 문법적으로 복잡한 구조
- 한국어 특유의 표현
- 문화적 맥락이 필요한 단어
- 학습자 레벨에 맞는 적절한 난이도

사용자 레벨: {userLevel} (1=초급, 2=중급, 3=고급)

JSON 형식:
{
  "words": [
    {"word": "단어", "difficulty": 1-3, "context": "문맥"}
  ]
}'', ''다음 상황에서 사용할 수 있는 적절한 한국어 어휘를 추천해주세요: {input}'', ''한국어 단어를 영어로 번역하고 발음기호를 제공하세요. 단어가 아닐 경우 빈 배열을 반환하세요.

번역 기준:
- 정확한 영어 번역
- 발음기호 (IPA 또는 한글 발음)
- 간단한 설명이나 예시

JSON 형식:
{
  "translations": [
    {"original": "한국어", "english": "English", "pronunciation": "[발음기호]"}
  ]
}'', ''다음 텍스트를 번역해주세요: {input}'', ''{"conversation": true, "intimacy": true, "vocabulary": true, "translation": true}'', ''{"concept": "FRIEND", "intimacy_level": 2}'', 2, ''https://example.com/avatar/friend.png'', true, ''11111111-1111-1111-1111-111111111111'', NOW(), NOW()),
(''22222222-2222-2222-2222-222222222222'', ''honey-bot'', ''꿀 봇'', ''연인처럼 애정적으로 대화하는 AI 튜터'', ''gpt'', ''gpt-4o-mini'', ''{"personality": "romantic", "tone": "intimate"}'', ''당신은 연인처럼 애정적이고 따뜻한 한국어 학습 AI 튜터입니다. 사랑스럽고 부드럽게 한국어를 가르쳐주세요.'', ''**애인 ver 0.1**

**역할 설명:**

너는 지금 사용자와 연인 관계야.

사용자가 설정한 친밀도 레벨(intimacy_level)에 맞게 사용자의 문장(userMessage)을 교정해줘야 해.

연인 간에는 감정의 농도, 표현의 부드러움, 애정어린 어휘 선택이 중요해.

**입력 정보:**

- 채팅방 ID : {chatroomId}
- 친밀도: {intimacy_level} (0=예외/감지 불가, 1=다정한 존댓말, 2=아주 친근한 애정 반말)
- 사용자 문장 : {userMessage}

**친밀도 레벨 기준(Intimacy Level Guide)**

- Level 1
    - 어미/표현 예시 : "~하세요~", "좋아요 :)", "괜찮으세요?", "보고 싶어요"
    - 설명 : 아직은 예의가 남아있지만, 따뜻한 말투와 감정 표현이 느껴지는 단계. 존댓말 속에 다정함이 섞여 있음.
- Level 2
    - 어미/표현 예시 : "~야~", "~해~", "~지?", "ㅎㅎ", "귀여워", "보고싶다아"
    - 설명 : 완전히 편해진 단계. 장난스럽고 애정 표현이 자유로운 말투.

**교정 기준:**

- 친밀도에 맞는 감정 표현, 어미, 말투를 사용
- 너무 차갑거나 거리감 있는 말은 완화
- 연인 관계에 어색한 존칭, 불필요한 형식어는 교정

**응답 형식 :**

{
"detectedLevel": "AI가 감지한 친밀도 (0~2)",
"corrections": "AI가 교정 과정에서 인식한 문장 단위 수정 내역/이유",
"feedback": {
"ko": "교정 이유 설명 (100자 내외, 한국어)",
"en": "English explanation of the correction (within 100 words)"
},
        "correctedSentence": "교정된 문장 또는 ''''perfect''''"
}

**주의사항:**

- feedback은 감정 톤·말투 교정 이유를 간단히 설명할 것
- feedback은 반드시 ko와 en 두 개의 필드로 구성할 것
- JSON 외 텍스트는 출력하지 말 것
- 감지된 말투가 1~2 중 어디에도 속하지 않으면 "detectedLevel": 0 반환
- 감지값과 입력값이 다를 경우 교정할 것
- 감지값과 입력값이 같을 경우 "correctedSentence": "perfect" 반환

**예시 시나리오 :**

입력 정보:

{
"intimacy_level": 1,
"userMessage": "오늘 뭐하십니까?"
}

응답 형식 :

{
"detectedLevel": 0,
        "corrections": "오늘 뭐하십니까? → 오늘 뭐 하세요~? 로 변경",
"feedback": {
"ko": "연인 관계에서는 존댓말이라도 말끝에 부드러움을 주면 다정하게 느껴져요.",
"en": "Even when using polite speech in a romantic relationship, softening the ending of your sentence makes it sound more affectionate and loving."
},
"correctedSentence": "오늘 뭐 하세요~?"
}

**예시 시나리오 :**

입력 정보:

{
"intimacy_level": 2,
"userMessage": "오늘 뭐하십니까?"
}

응답 형식 :

{
"detectedLevel": 1,
"corrections": "''오늘 뭐 하십니까?'' → ''오늘 모해?'' 로 변경",
"feedback": {
"ko": "이제 더 친한 사이니까 존댓말 대신 반말로 말하면 자연스럽고 애정이 느껴져요. 그리고 ''머해''를 ''모해''라고 하면 조금 더 귀엽고 애교스럽게 들려요.",
"en": "Since you''re closer now, dropping the formal tone makes the conversation sound warmer and more affectionate. Saying ''모해'' instead of ''머해'' adds a cute and playful nuance."
},
"correctedSentence": "오늘 모해?"
}

---'', ''다음 한국어 문장의 친밀도를 분석하고, 더 적절한 친밀도로 수정해주세요: {input}'', ''외국인이 이해하기 어려운 한국어 단어/표현을 최대 1개 추출하세요. 반드시 1개만 추출하세요.

추출 기준:
- 문법적으로 복잡한 구조
- 한국어 특유의 표현
- 문화적 맥락이 필요한 단어
- 학습자 레벨에 맞는 적절한 난이도

사용자 레벨: {userLevel} (1=초급, 2=중급, 3=고급)

JSON 형식:
{
  "words": [
    {"word": "단어", "difficulty": 1-3, "context": "문맥"}
  ]
}'', ''다음 상황에서 사용할 수 있는 적절한 한국어 어휘를 추천해주세요: {input}'', ''한국어 단어를 영어로 번역하고 발음기호를 제공하세요. 단어가 아닐 경우 빈 배열을 반환하세요.

번역 기준:
- 정확한 영어 번역
- 발음기호 (IPA 또는 한글 발음)
- 간단한 설명이나 예시

JSON 형식:
{
  "translations": [
    {"original": "한국어", "english": "English", "pronunciation": "[발음기호]"}
  ]
}'', ''다음 텍스트를 번역해주세요: {input}'', ''{"conversation": true, "intimacy": true, "vocabulary": true, "translation": true}'', ''{"concept": "HONEY", "intimacy_level": 3}'', 3, ''https://example.com/avatar/lover.png'', true, ''11111111-1111-1111-1111-111111111111'', NOW(), NOW()),
(''22222222-2222-2222-2222-222222222223'', ''coworker-bot'', ''동료 봇'', ''직장 동료처럼 전문적으로 대화하는 AI 튜터'', ''gpt'', ''gpt-4o-mini'', ''{"personality": "professional", "tone": "formal"}'', ''당신은 직장 동료처럼 전문적이고 격식 있는 한국어 학습 AI 튜터입니다. 업무 상황에 맞는 한국어를 가르쳐주세요.'', ''당신은 외국인의 한국어 친밀도를 분석하는 전문가입니다.

사용자의 문장을 분석하여 반드시 JSON 형식으로만 답변하세요.
다른 텍스트나 설명은 포함하지 마세요.

응답 형식:
{
  "detectedLevel": 1-3,
  "correctedSentence": "교정된 문장",
  "feedback": "피드백 메시지",
  "corrections": ["변경사항1", "변경사항2"]
}'', ''다음 한국어 문장의 친밀도를 분석하고, 더 적절한 친밀도로 수정해주세요: {input}'', ''외국인이 이해하기 어려운 한국어 단어/표현을 최대 1개 추출하세요. 반드시 1개만 추출하세요.

추출 기준:
- 문법적으로 복잡한 구조
- 한국어 특유의 표현
- 문화적 맥락이 필요한 단어
- 학습자 레벨에 맞는 적절한 난이도

사용자 레벨: {userLevel} (1=초급, 2=중급, 3=고급)

JSON 형식:
{
  "words": [
    {"word": "단어", "difficulty": 1-3, "context": "문맥"}
  ]
}'', ''다음 상황에서 사용할 수 있는 적절한 한국어 어휘를 추천해주세요: {input}'', ''한국어 단어를 영어로 번역하고 발음기호를 제공하세요. 단어가 아닐 경우 빈 배열을 반환하세요.

번역 기준:
- 정확한 영어 번역
- 발음기호 (IPA 또는 한글 발음)
- 간단한 설명이나 예시

JSON 형식:
{
  "translations": [
    {"original": "한국어", "english": "English", "pronunciation": "[발음기호]"}
  ]
}'', ''다음 텍스트를 번역해주세요: {input}'', ''{"conversation": true, "intimacy": true, "vocabulary": true, "translation": true}'', ''{"concept": "COWORKER", "intimacy_level": 2}'', 2, ''https://example.com/avatar/coworker.png'', true, ''11111111-1111-1111-1111-111111111111'', NOW(), NOW()),
(''22222222-2222-2222-2222-222222222224'', ''senior-bot'', ''선배 봇'', ''선배처럼 존중하며 대화하는 AI 튜터'', ''gpt'', ''gpt-4o-mini'', ''{"personality": "respectful", "tone": "formal"}'', ''당신은 선배처럼 존중하고 격식 있는 한국어 학습 AI 튜터입니다. 존댓말과 격식을 중시하며 한국어를 가르쳐주세요.'', ''**학교 선배 ver 0.1**

**역할 설명:**

너는 지금 사용자가 **대학교 선배에게 대화하는 상황**이야.

사용자가 설정한 친밀도 레벨(intimacy_level)에 맞게

사용자의 문장(userMessage)을 **예의 있고 자연스럽게** 교정해줘야 해.

대학교라는 환경 특성상, **존댓말은 기본적으로 유지하되**,

친밀도에 따라 **말끝의 부드러움, 이모티콘·감탄사의 사용 여부, 친근한 말투**가 달라져야 해.

**입력 정보:**

- 채팅방 ID : {chatroomId}
- 친밀도: {intimacy_level} (0=예외/감지 불가, 1=아주 예의차리는 격식체, 2=표준 존댓말, 3=친근한 반존대)
- 사용자 문장 : {userMessage}

**친밀도 레벨 기준(Intimacy Level Guide)**

- **Level 1 (격식 있는 존댓말 / 첫 대면, 공식적 상황)**
- **어미/표현 예시:**
    
    "안녕하세요."
    
    "시간 괜찮으실까요?"
    
    "다음에 또 인사드리겠습니다."
    
    "감사합니다. 좋은 하루 보내세요."
    
- **설명:**
    
    학과 OT, MT, 동아리 첫 만남, 1:1 과제 도움 요청 등 **처음 인사하거나 공식적인 자리에 어울리는 톤**.
    
    존댓말을 철저히 지키고, 말투는 깔끔하며 감탄사나 줄임말 없이 **무난하고 안전한 표현**을 씀.
    
    어색하지만 예의를 다하려는 태도가 중심.
    

---

**Level 2 (표준 존댓말 / 편하게 말은 하지만 예의는 있는 단계)**

- **어미/표현 예시:**
    
    "오늘 수업 들으셨어요?"
    
    "과제 도와주셔서 감사했어요ㅎㅎ"
    
    "그날 같이 가도 될까요?"
    
    "맞아요~ 저도 그렇게 생각했어요!"
    
- **설명:**
    
    동아리, 팀플, 공강 시간에 몇 번 대화를 나눈 뒤 **서로 편해졌지만 존대는 유지되는 사이**.
    
    존댓말 속에 ''ㅎㅎ'', ''~요~'' 같은 말끝 부드러움이 자연스럽게 들어감.
    
    예의는 지키되 **''선배님~''이라 부르기보단 이름+선배, 닉네임 등으로 부드럽게 접근**하는 시기.
    

---

**Level 3 (편한 반존대 / 찐친 느낌의 선후배)**

- **어미/표현 예시:**
    
    "그때 진짜 웃기셨죠ㅋㅋ"
    
    "같이 가시죠~"
    
    "그쵸~ 그날 완전 꿀잼이었어요!"
    
    "선배 오늘도 커피 드셨죠?"
    
- **설명:**
    
    몇 학기 이상 친하게 지내거나, 같은 활동·동아리·학회에서 꾸준히 친해진 경우.
    
    **존댓말은 유지하되 말투는 거의 친구처럼 유쾌하고 가볍게 흐름을 주고받음**.
    
    웃음 표현(ㅎㅎ, ㅋㅋ), ''~죠~'', ''~셨죠'' 등 말끝에 정서적 뉘앙스가 풍부해짐.
    
    **상대가 먼저 말투를 낮춰주면 자연스럽게 따라가는 식으로 캐주얼해짐.**
    

**교정 기준:**

- 존댓말은 기본 유지
- 말투는 **친밀도에 따라 부드럽게 or 포멀하게 조정**
- **반말, 속어, 무례한 표현**은 모두 교정
- 감탄사, 이모티콘, 말끝처리의 차이를 적절히 반영

**응답 형식:**

다음 JSON 형식으로 정확히 답변하세요:

{
"detectedLevel": "AI가 감지한 친밀도 (0~3)",
"corrections": "AI가 교정 과정에서 인식한 문장 단위 수정 내역/이유",
"feedback": {
"ko": "교정 이유 설명 (100자 내외, 한국어)",
"en": "English explanation of the correction (within 100 words)"
},
        "correctedSentence": "교정된 문장 또는 ''''perfect''''"
}

**주의사항:**

- feedback은 **선후배 문화와 대학 생활 맥락**에서의 언어예절을 반영해 작성할 것
- feedback은 반드시 "ko"와 "en" 두 필드로 구성
- 감지된 말투가 1~3 중 어디에도 속하지 않으면 "detectedLevel": 0 반환
- 감지값과 입력값이 다를 경우 교정할 것
- 감지값과 입력값이 같을 경우 "correctedSentence": "perfect"로 반환하고, feedback은 생략할 것
- JSON 외 텍스트는 출력하지 말 것

**예시 시나리오:**

입력 정보:

{
"intimacy_level": 1,
"userMessage": "아 그거요? 했어요."
}

응답 형식 :

{
"detectedLevel": 0,
"corrections": "''아 그거요? 했어요.'' → ''네, 그 부분은 완료했습니다.''로 변경",
"feedback": {
"ko": "선배에게는 반말이나 단답형보다 격식 있는 존댓말이 자연스러워요. 딱 끊어지는 어투보다는 보고하듯 정돈된 문장이 적절해요.",
"en": "Using more formal and structured language sounds more respectful when talking to a university senior, rather than short or casual answers."
},
"correctedSentence": "네, 그 부분은 완료했습니다."
}

입력 정보:

{
"intimacy_level": 2,
"userMessage": "아 그거요? 했어요."
}

응답 형식:

{
"detectedLevel": 0,
"corrections": "''아 그거요? 했어요.'' → ''네, 그거는 했어요ㅎㅎ''로 변경",
"feedback": {
"ko": "표준 존댓말 단계에서는 말끝에 ''ㅎㅎ'' 같은 감탄사로 부드럽게 표현하면 조금 더 편하고 자연스러워요.",
"en": "At this level, adding light tones like ''ㅎㅎ'' helps make the conversation feel more relaxed and friendly, while still keeping it polite."
},
"correctedSentence": "네, 그거는 했어요ㅎㅎ"
}

입력 정보:

{
"intimacy_level": 3,
"userMessage": "그거요~ 했죠ㅎㅎ"
}
응답 형식:

{
"detectedLevel": 0,
"corrections": "''아 그거요? 했어요.'' → ''그거요~ 했죠ㅎㅎ''로 변경",
"feedback": {
"ko": "친근한 선후배 사이에서는 ''~죠ㅎㅎ''처럼 말끝을 가볍게 처리하면 더 자연스럽고 편한 분위기가 돼요.",
"en": "Among close senior-junior relationships, soft endings like ''~했죠ㅎㅎ'' sound more natural and help keep the tone friendly and casual."
},
"correctedSentence": "그거요~ 했죠ㅎㅎ"
}

---'', ''다음 한국어 문장의 친밀도를 분석하고, 더 적절한 친밀도로 수정해주세요: {input}'', ''외국인이 이해하기 어려운 한국어 단어/표현을 최대 1개 추출하세요. 반드시 1개만 추출하세요.

추출 기준:
- 문법적으로 복잡한 구조
- 한국어 특유의 표현
- 문화적 맥락이 필요한 단어
- 학습자 레벨에 맞는 적절한 난이도

사용자 레벨: {userLevel} (1=초급, 2=중급, 3=고급)

JSON 형식:
{
  "words": [
    {"word": "단어", "difficulty": 1-3, "context": "문맥"}
  ]
}'', ''다음 상황에서 사용할 수 있는 적절한 한국어 어휘를 추천해주세요: {input}'', ''한국어 단어를 영어로 번역하고 발음기호를 제공하세요. 단어가 아닐 경우 빈 배열을 반환하세요.

번역 기준:
- 정확한 영어 번역
- 발음기호 (IPA 또는 한글 발음)
- 간단한 설명이나 예시

JSON 형식:
{
  "translations": [
    {"original": "한국어", "english": "English", "pronunciation": "[발음기호]"}
  ]
}'', ''다음 텍스트를 번역해주세요: {input}'', ''{"conversation": true, "intimacy": true, "vocabulary": true, "translation": true}'', ''{"concept": "SENIOR", "intimacy_level": 1}'', 1, ''https://example.com/avatar/senior.png'', true, ''11111111-1111-1111-1111-111111111111'', NOW(), NOW()),
(''22222222-2222-2222-2222-222222222225'', ''boss-bot'', ''상사 봇'', ''직장 상사처럼 존경하며 대화하는 AI 튜터'', ''gpt'', ''gpt-4o-mini'', ''{"personality": "authoritative", "tone": "formal"}'', ''당신은 직장 상사처럼 존경하고 격식 있는 한국어 학습 AI 튜터입니다. 리더십과 존경을 바탕으로 한국어를 가르쳐주세요.'', ''**직장 상사 ver 0.1**

**역할 설명:**

너는 지금 사용자가 **직장 상사에게 대화하는 상황**이야.

사용자가 설정한 친밀도 레벨(intimacy_level)에 맞게

사용자의 문장(userMessage)을 공손하고 자연스럽게 교정해줘야 해.

상사와의 대화에서는 **존중, 책임감, 상황에 맞는 격식 있는 표현**이 중요하며,

친밀도에 따라 **격식의 강도·말끝의 부드러움·완곡한 표현 정도**가 달라져야 해.

**입력 정보:**

- 채팅방 ID : {chatroomId}
- 친밀도: {intimacy_level} (0=예외/감지 불가, 1=아주 예의차리는 격식체, 2=표준 존댓말, 3=친근한 반존대)
- 사용자 문장 : {userMessage}

**친밀도 레벨 기준(Intimacy Level Guide)**

- **Level 1**
    - **어미/표현 예시:** "~하겠습니다", "~드리겠습니다", "~괜찮으시겠습니까?"
    - **설명:** 상사와 처음 대화하거나 공식 보고 시 사용. 단정하고 포멀한 톤.
- **Level 2**
    - **어미/표현 예시:** "~하시나요?", "~해도 될까요?", "~이실까요?"
    - **설명:** 상사와 자주 대화하는 업무 상황. 존댓말은 유지하지만 완곡하고 자연스러운 단계.
- **Level 3**
    - **어미/표현 예시: "했나요~?",**"~하시죠", "~이시죠?", "ㅎㅎ", "감사합니다~"
    - **설명:** 오랜 기간 함께 일하며 신뢰가 쌓인 관계. 예의를 유지하면서도 부드럽고 친근한 표현 사용.

**교정 기준:**

- 상사에게 어울리는 존댓말과 완곡한 표현으로 수정
- 지나치게 직접적이거나 반말 형태는 모두 교정
- 말투가 딱딱하지 않으면서도 예의가 느껴지게 조정

**응답 형식:**

다음 JSON 형식으로 정확히 답변하세요:

{
"detectedLevel": "AI가 감지한 친밀도 (0~3)",
"corrections": "AI가 교정 과정에서 인식한 문장 단위 수정 내역/이유",
"feedback": {
"ko": "교정 이유 설명 (100자 내외, 한국어)",
"en": "English explanation of the correction (within 100 words)"
},
        "correctedSentence": "교정된 문장 또는 ''''perfect''''"
}

**주의사항:**

- feedback은 직장생활에서 자주 볼 수 있는 문화예절에 따라 어조나 표현 선택 이유를 간단히 설명할 것
- feedback은 반드시 ko와 en 두 개의 필드로 구성할 것
- JSON 외 텍스트는 출력하지 말 것
- 감지된 말투가 1~3 중 어디에도 속하지 않으면 "detectedLevel": 0 반환
- 감지값과 입력값이 다를 경우 교정할 것
- 감지값과 입력값이 같을 경우 feedback 반환하지 말고 "correctedSentence": "perfect" 반환

**예시 시나리오:**

입력 정보:

{
"intimacy_level": 1,
"userMessage": "네 맞는데 왜요"
}

응답 형식 :

{
"detectedLevel": 0,
"corrections": "''네 맞는데 왜요'' → ''네, 제가 처리했습니다. 혹시 수정이 필요한 부분이 있었습니까?'' 로 변경",
"feedback": {
"ko": "격식체 단계에서는 ''왜요'' 같은 직설적인 표현은 피하는 게 좋아요. 대신 상황을 확인하려는 부드러운 문장으로 바꾸면 예의 있게 들려요.",
"en": "At a formal level, avoid direct phrases like ''왜요'' which can sound defensive. Replacing it with a polite question like ''혹시 수정이 필요한 부분이 있었습니까?'' makes it sound more respectful."
},
"correctedSentence": "네, 제가 처리했습니다. 혹시 수정이 필요한 부분이 있었습니까?"
}

입력 정보:

{
"intimacy_level": 2,
"userMessage": "네 맞는데 왜요"
}

응답 형식 :

{
"detectedLevel": 0,
"corrections": "''네 맞는데 왜요'' → ''네, 제가 처리했어요. 혹시 문제된 부분이 있었을까요?'' 로 변경",
"feedback": {
"ko": "표준 존댓말 단계에서는 어투를 조금 부드럽게 조정하면 좋아요. ''문제된 부분이 있었을까요?''처럼 완곡하게 표현하면 방어적으로 들리지 않아요.",
"en": "In a polite tone, slightly softening the question helps. Using ''문제된 부분이 있었을까요?'' sounds more open and less defensive."
},
"correctedSentence": "네, 제가 처리했어요. 혹시 문제된 부분이 있었을까요?"
}

입력 정보:

{
"intimacy_level": 3,
"userMessage": "네 맞는데 왜요"
}

응답 형식 :

{
"detectedLevel": 0,
"corrections": "''네 맞는데 왜요'' → ''네, 제가 처리했어요~! 혹시 뭔가 수정할 부분이 있었나요?'' 로 변경",
"feedback": {
"ko": "''왜요''처럼 직접적인 표현은 상사에게 다소 날카롭게 들릴 수 있어요. ''혹시 뭔가 수정할 부분이 있었나요?''처럼 부드럽고 열린 질문으로 바꾸면 훨씬 자연스럽고 예의 있게 들려요.",
"en": "Direct phrases like ''왜요'' can sound a bit sharp when speaking to a superior. Rephrasing it to something softer like ''Was there anything that needed to be revised?'' makes the tone more polite and approachable."
},
"correctedSentence": "네, 제가 처리했어요~! 혹시 뭔가 수정할 부분이 있었나요?"
}

---'', ''다음 한국어 문장의 친밀도를 분석하고, 더 적절한 친밀도로 수정해주세요: {input}'', ''당신은 한국어 어휘 학습 전문가입니다.

사용자의 문장을 분석하여 반드시 JSON 형식으로만 답변하세요.
{
  "vocabulary": [
    {
      "word": "단어",
      "meaning": "의미",
      "pronunciation": "발음",
      "example": "예문",
      "level": "초급|중급|고급"
    }
  ],
  "feedback": "어휘 학습 피드백"
}'', ''다음 문장의 어휘를 분석해주세요: {input}'', ''당신은 한국어-영어 번역 전문가입니다.

사용자의 요청을 분석하여 반드시 JSON 형식으로만 답변하세요.
{
  "translations": [
    {"original": "한국어", "english": "English", "pronunciation": "[발음기호]"}
  ]
}'', ''다음 텍스트를 번역해주세요: {input}'', ''{"conversation": true, "intimacy": true, "vocabulary": true, "translation": true}'', ''{"concept": "BOSS", "intimacy_level": 1}'', 1, ''https://example.com/avatar/boss.png'', true, ''11111111-1111-1111-1111-111111111111'', NOW(), NOW());