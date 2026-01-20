-- =========================================================
-- Archive 스키마 최종 SQL
-- =========================================================
-- 생성일: 2025-01-04
-- 버전: 1.0 (최종)
-- 호환성: ✅ 현재 프로젝트 구조와 호환 확인 완료
-- =========================================================

BEGIN;

-- UUID 생성
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 스키마 생성
CREATE SCHEMA IF NOT EXISTS archive_schema;

-- =========================================================
-- 0) JSONB 구조 데이터 계약 (주석)
-- =========================================================
-- [arch_chatrooms.meta] JSONB 구조
-- {
--   "concept": "FRIEND",              -- settings.concept에서 추출
--   "intimacyLevel": 1,               -- chatbot.intimacyLevel 또는 intimacy_progress.intimacy_level
--   "testModel": "...",                -- settings.testModel (있으면)
--   "tags": ["report", "hotfix"],     -- admin 태깅
--   "source": { "env": "prod", "service": "chat" },
--   "contextData": { ... }            -- context_data 전체 복사 (선택적)
-- }
-- 
-- [arch_messages.metadata_json] JSONB 구조
-- {
--   "analysis": {
--     "language": "ko",
--     "safety": { "flag": false, "reason": null }
--   },
--   "link": {
--     "storeId": "uuid",              -- store_schema.stores.id (message_id로 조회)
--     "usageRequestId": "req_...",    -- billing.ai_usage_events.request_id
--     "agentResults": {
--       "intimacy": "uuid",            -- arch_agent_results.id (agent_type='intimacy')
--       "voca": "uuid",                -- arch_agent_results.id (agent_type='voca')
--       "conver": "uuid"               -- arch_agent_results.id (agent_type='conver')
--     }
--   },
--   "originalMetadata": { ... }       -- 원본 metadata 전체 백업 (선택적)
-- }
--
-- [arch_agent_results.payload_json] JSONB 구조
-- - agent_type = "intimacy"
--   {
--     "detectedLevel": 1,
--     "correctedSentence": "나는 가아라야",
--     "feedback": { "ko": "...", "en": "..." },
--     "corrections": "...",
--     "alternativeExpressions": [...],
--     "confidence": 0.82  // 신규 필드 (있으면)
--   }
-- - agent_type = "voca"
--   {
--     "words": [                       // 여러 단어는 배열로 저장
--       {
--         "word": "매력",
--         "difficulty": 2,
--         "context": { "roma": "...", "ko": "...", "en": "..." }
--       }
--     ],
--     "definitions": ["...","..."],    // 신규 필드 (있으면)
--     "examples": ["..."]              // 신규 필드 (있으면)
--   }
-- - agent_type = "conver"
--   {
--     "response": "가아라 진짜 멋지지! ...",
--     "tone": "friendly",
--     "followUps": ["..."]            // 신규 필드 (있으면)
--   }

-- =========================================================
-- 1) arch_chatrooms
-- =========================================================
CREATE TABLE IF NOT EXISTS archive_schema.arch_chatrooms (
  id                      uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  source_chatroom_id      uuid NOT NULL UNIQUE,
  
  -- 스냅샷 컬럼 (삭제 대비)
  user_id                 uuid NULL,
  user_email_snapshot     varchar(320) NULL,
  chatbot_id              uuid NULL,
  chatbot_name_snapshot   varchar(100) NULL,
  chatbot_type_snapshot   varchar(20) NULL,
  chatbot_intimacy_level_snapshot integer NULL,
  
  -- 운영에서 오는 평면 컬럼
  name                    varchar(100) NOT NULL,
  description             text NULL,
  concept                 varchar(50) NULL,  -- settings.concept에서 추출
  
  last_message_at         timestamp without time zone NULL,
  source_last_message_id  uuid NULL,
  
  is_archived             boolean NOT NULL DEFAULT false,
  is_deleted              boolean NOT NULL DEFAULT false,
  
  source_created_at       timestamp without time zone NULL,
  source_updated_at       timestamp without time zone NULL,
  archived_at             timestamp without time zone NOT NULL DEFAULT now(),
  source_deleted_at       timestamp without time zone NULL,
  
  -- ✅ archive 확장 메타
  meta                    jsonb NOT NULL DEFAULT '{}'::jsonb
);


-- 인덱스 (검색 성능 향상)
CREATE INDEX IF NOT EXISTS idx_arch_chatrooms_user_created
  ON archive_schema.arch_chatrooms(user_id, source_created_at DESC);

CREATE INDEX IF NOT EXISTS idx_arch_chatrooms_created
  ON archive_schema.arch_chatrooms(source_created_at DESC);

CREATE INDEX IF NOT EXISTS idx_arch_chatrooms_concept
  ON archive_schema.arch_chatrooms(concept);

CREATE INDEX IF NOT EXISTS idx_arch_chatrooms_archived
  ON archive_schema.arch_chatrooms(archived_at DESC);

-- meta 검색용 GIN 인덱스
CREATE INDEX IF NOT EXISTS idx_arch_chatrooms_meta_gin
  ON archive_schema.arch_chatrooms USING gin (meta);

-- =========================================================
-- 2) arch_messages
-- =========================================================
CREATE TABLE IF NOT EXISTS archive_schema.arch_messages (
  id                      uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  arch_chatroom_id         uuid NOT NULL,
  source_message_id        uuid NOT NULL UNIQUE,
  source_parent_message_id uuid NULL,
  
  sender_type              varchar(20) NOT NULL,
  sender_id                uuid NULL,
  content                  text NOT NULL,
  content_type             varchar(20) NOT NULL DEFAULT 'text',
  
  -- ✅ sequence_number: 운영과 동일 (메시지 순서 번호, 재계산 불필요)
  -- 운영의 sequence_number를 그대로 복사
  sequence_number          bigint NOT NULL,
  
  -- ✅ turn_number: 운영과 동일 (메시지 순서 번호를 묶어 1턴으로 하는 단위)
  -- Bot 응답부터 User 응답까지를 하나의 턴으로 묶음
  -- Bot 메시지: 새로운 턴 시작 (nextTurnNumber)
  -- User 메시지: turn_number = 0 (미완료 턴, User 메시지 수신 시 이전 턴 완료 처리)
  -- System 메시지: 가장 최근 Bot 메시지의 턴 번호 사용
  turn_number              bigint NOT NULL DEFAULT 0,
  
  token_count              integer NULL,
  processing_time_ms       integer NULL,
  
  is_edited                boolean NOT NULL DEFAULT false,
  edited_at                timestamp without time zone NULL,
  is_deleted               boolean NOT NULL DEFAULT false,
  deleted_at               timestamp without time zone NULL,
  
  source_created_at        timestamp without time zone NULL,
  source_updated_at        timestamp without time zone NULL,
  archived_at              timestamp without time zone NOT NULL DEFAULT now(),
  
  -- ✅ archive 확장 메타
  metadata_json            jsonb NOT NULL DEFAULT '{}'::jsonb,
  
  CONSTRAINT fk_arch_messages_room
    FOREIGN KEY (arch_chatroom_id) REFERENCES archive_schema.arch_chatrooms(id) ON DELETE CASCADE,
  CONSTRAINT arch_messages_sender_type_check
    CHECK (sender_type IN ('user','bot','system')),
  CONSTRAINT arch_messages_content_type_check
    CHECK (content_type IN ('text','code','system','json'))
);

-- ✅ sequence_number는 채팅방 내에서 고유 (운영과 동일)
-- 운영과 동일하게 방 내 sequence 유니크 유지
CREATE UNIQUE INDEX IF NOT EXISTS uq_arch_messages_room_seq
  ON archive_schema.arch_messages(arch_chatroom_id, sequence_number);

CREATE INDEX IF NOT EXISTS idx_arch_messages_room_sequence
  ON archive_schema.arch_messages(arch_chatroom_id, sequence_number);

CREATE INDEX IF NOT EXISTS idx_arch_messages_room_turn
  ON archive_schema.arch_messages(arch_chatroom_id, turn_number);

CREATE INDEX IF NOT EXISTS idx_arch_messages_created
  ON archive_schema.arch_messages(source_created_at);

CREATE INDEX IF NOT EXISTS idx_arch_messages_archived
  ON archive_schema.arch_messages(archived_at);

-- metadata 검색용 GIN 인덱스
CREATE INDEX IF NOT EXISTS idx_arch_messages_metadata_gin
  ON archive_schema.arch_messages USING gin (metadata_json);

-- =========================================================
-- 3) arch_agent_results
-- =========================================================
CREATE TABLE IF NOT EXISTS archive_schema.arch_agent_results (
  id                   uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  arch_message_id      uuid NOT NULL,
  agent_type           varchar(20) NOT NULL,
  
  -- ✅ 핵심: 구조화 결과 (운영 구조와 일치)
  payload_json         jsonb NOT NULL,
  
  -- 추적/비용 연결(선택)
  request_id           text NULL,
  provider             text NULL,
  model                text NULL,
  latency_ms           integer NULL,
  input_tokens         integer NULL,
  output_tokens        integer NULL,
  
  source_created_at    timestamp without time zone NULL,
  archived_at          timestamp without time zone NOT NULL DEFAULT now(),
  
  CONSTRAINT fk_arch_agent_results_message
    FOREIGN KEY (arch_message_id) REFERENCES archive_schema.arch_messages(id) ON DELETE CASCADE,
  CONSTRAINT arch_agent_type_check
    CHECK (agent_type IN ('intimacy','conver','voca'))
);

-- voca 여러 단어는 단일 레코드에 words 배열로 저장하므로 UNIQUE 제약 유지 가능
CREATE UNIQUE INDEX IF NOT EXISTS uq_arch_agent_results_msg_agent
  ON archive_schema.arch_agent_results(arch_message_id, agent_type);

CREATE INDEX IF NOT EXISTS idx_arch_agent_results_request_id
  ON archive_schema.arch_agent_results(request_id);

CREATE INDEX IF NOT EXISTS idx_arch_agent_results_archived
  ON archive_schema.arch_agent_results(archived_at);

-- payload 검색용 GIN 인덱스
CREATE INDEX IF NOT EXISTS idx_arch_agent_results_payload_gin
  ON archive_schema.arch_agent_results USING gin (payload_json);

-- =========================================================
-- 4) 적재 상태(커서) 테이블
-- =========================================================
CREATE TABLE IF NOT EXISTS archive_schema.arch_ingestion_state (
  id                         bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
  job_name                   text NOT NULL UNIQUE,
  last_source_chatroom_id    uuid NULL,
  last_source_message_id     uuid NULL,
  last_source_message_created_at timestamp without time zone NULL,
  status                     text NOT NULL DEFAULT 'RUNNING',
  updated_at                 timestamp without time zone NOT NULL DEFAULT now(),
  note                       text NULL
);

-- =========================================================
-- 권한 부여
-- =========================================================
GRANT USAGE ON SCHEMA archive_schema TO doran;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA archive_schema TO doran;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA archive_schema TO doran;

-- 기본 권한 설정 (향후 생성되는 테이블에도 자동 적용)
ALTER DEFAULT PRIVILEGES IN SCHEMA archive_schema
  GRANT ALL ON TABLES TO doran;
ALTER DEFAULT PRIVILEGES IN SCHEMA archive_schema
  GRANT ALL ON SEQUENCES TO doran;

COMMIT;

-- =========================================================
-- 스키마 생성 완료
-- =========================================================
-- 호환성: ✅ 현재 프로젝트 구조와 호환 확인 완료
-- 다음 단계: 데이터 변환 로직 구현 및 Archive 서비스 개발
-- =========================================================

