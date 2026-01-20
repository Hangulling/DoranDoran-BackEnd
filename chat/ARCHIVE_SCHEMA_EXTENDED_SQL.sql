-- =========================================================
-- Archive 스키마 확장 SQL
-- =========================================================
-- 생성일: 2025-01-04
-- 버전: 2.0 (확장)
-- 목적: Store, Usage Events, Intimacy Progress 데이터 완전 이관
-- =========================================================

BEGIN;

-- =========================================================
-- 5) arch_stores
-- =========================================================
-- Store 데이터 완전 아카이빙
-- store_schema.stores의 모든 필드를 아카이빙
CREATE TABLE IF NOT EXISTS archive_schema.arch_stores (
  id                      uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  arch_chatroom_id        uuid NOT NULL,
  source_store_id         uuid NOT NULL UNIQUE,
  source_message_id       uuid NOT NULL,
  
  -- 관계 (UUID만 저장, FK 없음)
  user_id                 uuid NOT NULL,
  
  -- 표현 원본
  content                 text NOT NULL,
  corrected_content       text NULL,
  
  -- Multi-Agent AI 응답 (JSONB)
  ai_response             jsonb NOT NULL,
  
  -- 챗봇 역할
  bot_type                varchar(20) NOT NULL,
  
  -- 소프트 삭제
  is_deleted              boolean NOT NULL DEFAULT false,
  deleted_at              timestamp without time zone NULL,
  
  -- 타임스탬프
  source_created_at       timestamp without time zone NULL,
  source_updated_at       timestamp without time zone NULL,
  archived_at             timestamp without time zone NOT NULL DEFAULT now(),
  
  CONSTRAINT fk_arch_stores_chatroom
    FOREIGN KEY (arch_chatroom_id) REFERENCES archive_schema.arch_chatrooms(id) ON DELETE CASCADE
);

-- 인덱스
CREATE INDEX IF NOT EXISTS idx_arch_stores_chatroom
  ON archive_schema.arch_stores(arch_chatroom_id);

CREATE INDEX IF NOT EXISTS idx_arch_stores_message
  ON archive_schema.arch_stores(source_message_id);

CREATE INDEX IF NOT EXISTS idx_arch_stores_user
  ON archive_schema.arch_stores(user_id);

CREATE INDEX IF NOT EXISTS idx_arch_stores_archived
  ON archive_schema.arch_stores(archived_at);

-- ai_response 검색용 GIN 인덱스
CREATE INDEX IF NOT EXISTS idx_arch_stores_ai_response_gin
  ON archive_schema.arch_stores USING gin (ai_response);

-- =========================================================
-- 6) arch_usage_events
-- =========================================================
-- Usage 이벤트 완전 아카이빙
-- billing.ai_usage_events의 모든 필드를 아카이빙
CREATE TABLE IF NOT EXISTS archive_schema.arch_usage_events (
  id                      uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  arch_chatroom_id        uuid NOT NULL,
  source_usage_event_id   uuid NOT NULL UNIQUE,
  
  -- 관계 (UUID만 저장, FK 없음)
  user_id                 uuid NOT NULL,
  
  -- 이벤트 정보
  event_time              timestamp without time zone NOT NULL,
  provider                text NOT NULL,
  model                   text NOT NULL,
  request_id              text NULL,
  
  -- 토큰 및 비용
  input_tokens            integer NOT NULL DEFAULT 0,
  output_tokens           integer NOT NULL DEFAULT 0,
  cost_in                 numeric(18,6) NOT NULL DEFAULT 0,
  cost_out                numeric(18,6) NOT NULL DEFAULT 0,
  
  -- 메타데이터
  meta                    jsonb NULL,
  
  -- 타임스탬프
  archived_at             timestamp without time zone NOT NULL DEFAULT now(),
  
  CONSTRAINT fk_arch_usage_events_chatroom
    FOREIGN KEY (arch_chatroom_id) REFERENCES archive_schema.arch_chatrooms(id) ON DELETE CASCADE
);

-- 인덱스
CREATE INDEX IF NOT EXISTS idx_arch_usage_events_chatroom
  ON archive_schema.arch_usage_events(arch_chatroom_id);

CREATE INDEX IF NOT EXISTS idx_arch_usage_events_user
  ON archive_schema.arch_usage_events(user_id);

CREATE INDEX IF NOT EXISTS idx_arch_usage_events_event_time
  ON archive_schema.arch_usage_events(event_time);

CREATE INDEX IF NOT EXISTS idx_arch_usage_events_request_id
  ON archive_schema.arch_usage_events(request_id);

CREATE INDEX IF NOT EXISTS idx_arch_usage_events_archived
  ON archive_schema.arch_usage_events(archived_at);

-- meta 검색용 GIN 인덱스
CREATE INDEX IF NOT EXISTS idx_arch_usage_events_meta_gin
  ON archive_schema.arch_usage_events USING gin (meta);

-- =========================================================
-- 7) arch_intimacy_progress
-- =========================================================
-- Intimacy Progress 완전 아카이빙
-- chat_schema.intimacy_progress의 모든 필드를 아카이빙
CREATE TABLE IF NOT EXISTS archive_schema.arch_intimacy_progress (
  id                      uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  arch_chatroom_id        uuid NOT NULL,
  source_intimacy_progress_id uuid NOT NULL UNIQUE,
  
  -- 관계 (UUID만 저장, FK 없음)
  user_id                 uuid NOT NULL,
  
  -- 친밀도 정보
  intimacy_level          integer NOT NULL DEFAULT 1,
  total_corrections       integer DEFAULT 0,
  last_feedback           text NULL,
  last_updated            timestamp without time zone NULL,
  
  -- 세부 학습 통계 (JSONB)
  progress_data           jsonb NULL,
  
  -- 타임스탬프
  archived_at             timestamp without time zone NOT NULL DEFAULT now(),
  
  CONSTRAINT fk_arch_intimacy_progress_chatroom
    FOREIGN KEY (arch_chatroom_id) REFERENCES archive_schema.arch_chatrooms(id) ON DELETE CASCADE,
  CONSTRAINT arch_intimacy_level_check
    CHECK (intimacy_level IN (1, 2, 3))
);

-- 인덱스
CREATE INDEX IF NOT EXISTS idx_arch_intimacy_progress_chatroom
  ON archive_schema.arch_intimacy_progress(arch_chatroom_id);

CREATE INDEX IF NOT EXISTS idx_arch_intimacy_progress_user
  ON archive_schema.arch_intimacy_progress(user_id);

CREATE INDEX IF NOT EXISTS idx_arch_intimacy_progress_archived
  ON archive_schema.arch_intimacy_progress(archived_at);

-- progress_data 검색용 GIN 인덱스
CREATE INDEX IF NOT EXISTS idx_arch_intimacy_progress_data_gin
  ON archive_schema.arch_intimacy_progress USING gin (progress_data);

-- =========================================================
-- 권한 부여
-- =========================================================
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA archive_schema TO doran;

COMMIT;

-- =========================================================
-- 스키마 확장 완료
-- =========================================================
-- 추가된 테이블:
--   - arch_stores: Store 데이터 완전 아카이빙
--   - arch_usage_events: Usage 이벤트 완전 아카이빙
--   - arch_intimacy_progress: Intimacy Progress 완전 아카이빙
-- =========================================================


