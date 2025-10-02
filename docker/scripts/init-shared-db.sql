-- DoranDoran MSA 공유 데이터베이스 초기화 스크립트
-- 모든 서비스가 하나의 PostgreSQL 인스턴스를 공유하며 스키마로 분리

-- ========================================
-- 1. 스키마 생성
-- ========================================

-- Auth 스키마 (인증 관련)
CREATE SCHEMA IF NOT EXISTS auth;

-- User 스키마 (사용자 프로필 관련)
CREATE SCHEMA IF NOT EXISTS user_schema;

-- Chat 스키마 (채팅 관련)
CREATE SCHEMA IF NOT EXISTS chat;

-- Store 스키마 (저장소 관련)
CREATE SCHEMA IF NOT EXISTS store;

-- ========================================
-- 2. 테이블 생성
-- ========================================

-- Chat 스키마의 챗봇 테이블
DROP TABLE IF EXISTS chat.chatbot CASCADE;

CREATE TABLE chat.chatbot
(
    bot_id    UUID NOT NULL,
    bot_type    character varying(50) NOT NULL,
    intimacy    integer NOT NULL,
    create_time    timestamp without time zone NOT NULL,
    update_time    timestamp without time zone,
    bot_img_url    character varying NOT NULL
);

COMMENT ON COLUMN chat.chatbot.bot_id IS '챗봇 아이디';
COMMENT ON COLUMN chat.chatbot.bot_type IS '타입';
COMMENT ON COLUMN chat.chatbot.intimacy IS '친밀도';
COMMENT ON COLUMN chat.chatbot.create_time IS '생성시간';
COMMENT ON COLUMN chat.chatbot.update_time IS '업데이트 시간';
COMMENT ON COLUMN chat.chatbot.bot_img_url IS '챗봇 이미지';
COMMENT ON TABLE chat.chatbot IS '챗봇';

CREATE UNIQUE INDEX chatbot_PK ON chat.chatbot (bot_id);
ALTER TABLE chat.chatbot ADD CONSTRAINT chatbot_PK PRIMARY KEY USING INDEX chatbot_PK;

-- Chat 스키마의 채팅방 테이블
DROP TABLE IF EXISTS chat.chatroom CASCADE;

CREATE TABLE chat.chatroom
(
    room_id    UUID NOT NULL,
    create_at    timestamp without time zone NOT NULL,
    update_at    timestamp without time zone,
    room_name    character varying(50) NOT NULL,
    is_deleted    boolean DEFAULT 'true' NOT NULL,
    settings    json NOT NULL,
    user_id    UUID NOT NULL,
    bot_id    UUID NOT NULL
);

COMMENT ON COLUMN chat.chatroom.room_id IS '채팅방 아이디';
COMMENT ON COLUMN chat.chatroom.create_at IS '생성일시';
COMMENT ON COLUMN chat.chatroom.update_at IS '마지막 대화 일시';
COMMENT ON COLUMN chat.chatroom.room_name IS '채팅방 이름';
COMMENT ON COLUMN chat.chatroom.is_deleted IS '소프트 삭제 여';
COMMENT ON COLUMN chat.chatroom.settings IS '채팅방 설정';
COMMENT ON COLUMN chat.chatroom.user_id IS '사용자 아이디';
COMMENT ON COLUMN chat.chatroom.bot_id IS '챗봇 아이디';
COMMENT ON TABLE chat.chatroom IS '채팅방';

CREATE UNIQUE INDEX chatroom_PK ON chat.chatroom (bot_id, user_id, room_id);
ALTER TABLE chat.chatroom ADD CONSTRAINT chatroom_PK PRIMARY KEY USING INDEX chatroom_PK;

-- Auth 스키마의 사용자 테이블 (인증 정보) - 엔티티 기준으로 수정
DROP TABLE IF EXISTS auth.users CASCADE;

CREATE TABLE auth.users
(
    id    UUID NOT NULL,
    email    character varying(320) NOT NULL,
    password_hash    character varying(100) NOT NULL,
    status    character varying(20) NOT NULL DEFAULT 'ACTIVE',
    role    character varying(20) NOT NULL DEFAULT 'ROLE_USER',
    last_login    timestamp without time zone,
    created_at    timestamp without time zone NOT NULL,
    updated_at    timestamp without time zone NOT NULL
);

COMMENT ON COLUMN auth.users.id IS '사용자 아이디';
COMMENT ON COLUMN auth.users.email IS '사용자 이메일';
COMMENT ON COLUMN auth.users.password_hash IS '비밀번호 해시';
COMMENT ON COLUMN auth.users.status IS '사용자 상태 (ACTIVE, INACTIVE, SUSPENDED)';
COMMENT ON COLUMN auth.users.role IS '사용자 역할 (ROLE_USER, ROLE_ADMIN)';
COMMENT ON COLUMN auth.users.last_login IS '마지막 로그인 시간';
COMMENT ON COLUMN auth.users.created_at IS '생성 시간';
COMMENT ON COLUMN auth.users.updated_at IS '수정 시간';
COMMENT ON TABLE auth.users IS '인증 사용자';

CREATE UNIQUE INDEX users_PK ON auth.users (id);
CREATE UNIQUE INDEX users_email_idx ON auth.users (email);
ALTER TABLE auth.users ADD CONSTRAINT users_PK PRIMARY KEY USING INDEX users_PK;

-- User 스키마의 프로필 테이블 - 엔티티 기준으로 수정
DROP TABLE IF EXISTS user_schema.profiles CASCADE;

CREATE TABLE user_schema.profiles
(
    id    BIGSERIAL NOT NULL,
    user_id    UUID NOT NULL,
    bio    TEXT,
    avatar_url    character varying(500),
    settings    JSONB,
    created_at    timestamp without time zone NOT NULL,
    updated_at    timestamp without time zone NOT NULL
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
    created_at    timestamp without time zone NOT NULL,
    updated_at    timestamp without time zone NOT NULL
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

-- Chat 스키마의 메시지 테이블
DROP TABLE IF EXISTS chat.message CASCADE;

CREATE TABLE chat.message
(
    messege_id    UUID NOT NULL,
    content    character varying(100) NOT NULL,
    sender_type    character varying NOT NULL,
    chat_num    integer NOT NULL,
    message_send_time    timestamp without time zone NOT NULL,
    message_type    character varying(10) NOT NULL,
    message_meta    json,
    room_id    UUID NOT NULL,
    bot_id    UUID NOT NULL,
    user_id    UUID NOT NULL
);

COMMENT ON COLUMN chat.message.messege_id IS '메세지 아이디';
COMMENT ON COLUMN chat.message.content IS '내용';
COMMENT ON COLUMN chat.message.sender_type IS '발신자 타입';
COMMENT ON COLUMN chat.message.chat_num IS '대화순번';
COMMENT ON COLUMN chat.message.message_send_time IS '생성일시';
COMMENT ON COLUMN chat.message.message_type IS '메세지 타입';
COMMENT ON COLUMN chat.message.message_meta IS '메타데이터';
COMMENT ON COLUMN chat.message.room_id IS '채팅방 아이디';
COMMENT ON COLUMN chat.message.bot_id IS '챗봇 아이디';
COMMENT ON COLUMN chat.message.user_id IS '사용자 아이디';
COMMENT ON TABLE chat.message IS '메세지';

CREATE UNIQUE INDEX message_PK ON chat.message (room_id, bot_id, user_id, messege_id);
ALTER TABLE chat.message ADD CONSTRAINT message_PK PRIMARY KEY USING INDEX message_PK;

-- Store 스키마의 저장소 테이블
DROP TABLE IF EXISTS store.store CASCADE;

CREATE TABLE store.store
(
    store_id    UUID NOT NULL,
    store_meta    json NOT NULL,
    store_content    character varying NOT NULL,
    messege_id    UUID NOT NULL,
    user_id    UUID NOT NULL
);

COMMENT ON COLUMN store.store.store_id IS '보관함 아이디';
COMMENT ON COLUMN store.store.store_meta IS '메타데이터';
COMMENT ON COLUMN store.store.store_content IS '내용';
COMMENT ON COLUMN store.store.messege_id IS '메세지 아이디';
COMMENT ON COLUMN store.store.user_id IS '사용자 아이디';
COMMENT ON TABLE store.store IS '보관함';

CREATE UNIQUE INDEX store_PK ON store.store (messege_id, user_id, store_id);
ALTER TABLE store.store ADD CONSTRAINT store_PK PRIMARY KEY USING INDEX store_PK;

-- ========================================
-- 3. 권한 설정
-- ========================================

-- 각 스키마에 대한 권한 부여
GRANT USAGE ON SCHEMA auth TO doran;
GRANT USAGE ON SCHEMA user_schema TO doran;
GRANT USAGE ON SCHEMA chat TO doran;
GRANT USAGE ON SCHEMA store TO doran;

-- 테이블 권한 부여
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA auth TO doran;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA user_schema TO doran;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA chat TO doran;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA store TO doran;

-- 시퀀스 권한 부여 (향후 추가될 경우)
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA auth TO doran;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA user_schema TO doran;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA chat TO doran;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA store TO doran;
