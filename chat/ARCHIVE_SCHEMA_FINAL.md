# Archive 스키마 최종 설계서

> **생성일**: 2025-02-02  
> **상태**: 호환성 점검 완료 ✅  
> **버전**: 1.0 (최종)

## 📋 목차

1. [호환성 점검 결과](#호환성-점검-결과)
2. [최종 스키마 정의](#최종-스키마-정의)
3. [데이터 변환 가이드](#데이터-변환-가이드)
4. [주의사항 및 제약조건](#주의사항-및-제약조건)

---

## 호환성 점검 결과

### ✅ 호환성 평가: 양호

현재 프로젝트 구조와 archive 스키마는 **호환 가능**하며, 다음 사항들을 확인했습니다:

| 항목 | 평가 | 비고 |
|------|------|------|
| **엔티티 매핑** | ✅ 양호 | ChatRoom, Message 엔티티와 잘 매핑됨 |
| **JSONB 구조** | ✅ 양호 | 운영 구조와 일치하도록 수정 완료 |
| **외래키 관계** | ✅ 독립성 | FK 제약 없음, UUID만 저장 (독립성 강화) |
| **인덱스 전략** | ✅ 양호 | 조회 패턴에 맞게 설계됨 |
| **데이터 변환** | ⚠️ 구현 필요 | 변환 로직 구현 필요 (가이드 제공) |

### ⚠️ 주의사항

1. **sequence_number와 turn_number**
   - **sequence_number**: 메시지 순서 번호 (1, 2, 3, 4, ...) - 운영과 동일, 그대로 복사
   - **turn_number**: 메시지 순서 번호를 묶어 1턴으로 하는 단위
     - Bot 응답부터 User 응답까지를 하나의 턴으로 묶음
     - Bot 메시지: 새로운 턴 시작 (nextTurnNumber)
     - User 메시지: turn_number = 0 (미완료 턴, User 메시지 수신 시 이전 턴 완료 처리)
     - System 메시지: 가장 최근 Bot 메시지의 턴 번호 사용
   - → 운영과 동일, 그대로 복사 (재계산 불필요)

2. **voca 여러 단어 처리**
   - UNIQUE 제약: (arch_message_id, agent_type)
   - 여러 단어는 단일 레코드에 배열로 저장 권장

---

## 최종 스키마 정의

### 1. arch_chatrooms

```sql
CREATE TABLE archive_schema.arch_chatrooms (
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

-- ✅ FK 제약 없음: Archive 독립성 강화
-- user_id와 chatbot_id는 UUID만 저장 (참조용)
-- 스냅샷 컬럼(user_email_snapshot, chatbot_name_snapshot 등)으로 필요한 정보 보존
-- 운영 스키마 변경/삭제 시에도 archive에 영향 없음

-- 인덱스 (검색 성능 향상)
CREATE INDEX idx_arch_chatrooms_user_created
  ON archive_schema.arch_chatrooms(user_id, source_created_at DESC);
CREATE INDEX idx_arch_chatrooms_created
  ON archive_schema.arch_chatrooms(source_created_at DESC);
CREATE INDEX idx_arch_chatrooms_concept
  ON archive_schema.arch_chatrooms(concept);
CREATE INDEX idx_arch_chatrooms_archived
  ON archive_schema.arch_chatrooms(archived_at DESC);
CREATE INDEX idx_arch_chatrooms_meta_gin
  ON archive_schema.arch_chatrooms USING gin (meta);
```

**meta JSONB 구조**:
```json
{
  "concept": "FRIEND",              // settings.concept에서 추출
  "intimacyLevel": 1,               // chatbot.intimacyLevel 또는 intimacy_progress.intimacy_level
  "testModel": "...",                // settings.testModel (있으면)
  "tags": ["report", "hotfix"],     // admin 태깅
  "source": { "env": "prod", "service": "chat" },
  "contextData": { ... }            // context_data 전체 복사 (선택적)
}
```

---

### 2. arch_messages

```sql
CREATE TABLE archive_schema.arch_messages (
  id                      uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  arch_chatroom_id         uuid NOT NULL,
  source_message_id        uuid NOT NULL UNIQUE,
  source_parent_message_id uuid NULL,
  
  sender_type              varchar(20) NOT NULL,
  sender_id                uuid NULL,
  content                  text NOT NULL,
  content_type             varchar(20) NOT NULL DEFAULT 'text',
  
  -- ✅ sequence_number: 운영과 동일 (메시지 순서 번호, 재계산 불필요)
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
CREATE UNIQUE INDEX uq_arch_messages_room_seq
  ON archive_schema.arch_messages(arch_chatroom_id, sequence_number);

-- sequence별 조회를 위한 인덱스
CREATE INDEX idx_arch_messages_room_sequence
  ON archive_schema.arch_messages(arch_chatroom_id, sequence_number);
CREATE INDEX idx_arch_messages_room_turn
  ON archive_schema.arch_messages(arch_chatroom_id, turn_number);
CREATE INDEX idx_arch_messages_created
  ON archive_schema.arch_messages(source_created_at);
CREATE INDEX idx_arch_messages_archived
  ON archive_schema.arch_messages(archived_at);
CREATE INDEX idx_arch_messages_metadata_gin
  ON archive_schema.arch_messages USING gin (metadata_json);
```

**metadata_json JSONB 구조**:
```json
{
  "analysis": {
    "language": "ko",
    "safety": { "flag": false, "reason": null }
  },
  "link": {
    "storeId": "uuid",                    // store_schema.stores.id (message_id로 조회)
    "usageRequestId": "req_...",          // billing.ai_usage_events.request_id (metadata.usage에서 추출)
    "agentResults": {                     // arch_agent_results 참조
      "intimacy": "uuid",                  // arch_agent_results.id (agent_type='intimacy')
      "voca": "uuid",                      // arch_agent_results.id (agent_type='voca')
      "conver": "uuid"                     // arch_agent_results.id (agent_type='conver')
    }
  },
  "originalMetadata": { ... }             // 원본 metadata 전체 백업 (선택적)
}
```

---

### 3. arch_agent_results

```sql
CREATE TABLE archive_schema.arch_agent_results (
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

-- ⚠️ UNIQUE 제약: voca 여러 단어 처리 고려 필요
-- 옵션 1: 단일 레코드에 words 배열로 저장 (권장)
-- 옵션 2: UNIQUE 제약 수정 (arch_message_id, agent_type, word_index)
CREATE UNIQUE INDEX uq_arch_agent_results_msg_agent
  ON archive_schema.arch_agent_results(arch_message_id, agent_type);

CREATE INDEX idx_arch_agent_results_request_id
  ON archive_schema.arch_agent_results(request_id);
CREATE INDEX idx_arch_agent_results_archived
  ON archive_schema.arch_agent_results(archived_at);
CREATE INDEX idx_arch_agent_results_payload_gin
  ON archive_schema.arch_agent_results USING gin (payload_json);
```

**payload_json 구조** (운영과 일치):

**intimacy**:
```json
{
  "detectedLevel": 1,
  "correctedSentence": "나는 가아라야",
  "feedback": {
    "ko": "조금 더 자연스럽게 말하면 ...",
    "en": "You can say it more naturally..."
  },
  "corrections": "...",
  "alternativeExpressions": [
    {
      "expression": "...",
      "tone": "...",
      "example": "..."
    }
  ],
  "confidence": 0.82  // 신규 필드 (있으면)
}
```

**voca** (여러 단어는 배열로 저장):
```json
{
  "words": [  // ⚠️ 여러 단어는 배열로 저장 (UNIQUE 제약 고려)
    {
      "word": "매력",
      "difficulty": 2,
      "context": {
        "roma": "...",
        "ko": "...",
        "en": "..."
      }
    }
  ],
  "definitions": ["...","..."],  // 신규 필드 (있으면)
  "examples": ["..."]            // 신규 필드 (있으면)
}
```

**conver**:
```json
{
  "response": "가아라 진짜 멋지지! ...",
  "tone": "friendly",
  "followUps": ["좋아하는 캐릭터 또 있어?"]  // 신규 필드 (있으면)
}
```

---

### 4. arch_ingestion_state

```sql
CREATE TABLE archive_schema.arch_ingestion_state (
  id                         bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
  job_name                   text NOT NULL UNIQUE,
  last_source_chatroom_id    uuid NULL,
  last_source_message_id     uuid NULL,
  last_source_message_created_at timestamp without time zone NULL,
  status                     text NOT NULL DEFAULT 'RUNNING',
  updated_at                 timestamp without time zone NOT NULL DEFAULT now(),
  note                       text NULL
);
```

---

## 데이터 변환 가이드

### 1. ChatRoom → arch_chatrooms

```java
public ArchChatroom archiveChatroom(ChatRoom room, User user, Chatbot chatbot, IntimacyProgress progress) {
    ArchChatroom arch = new ArchChatroom();
    
    // Source ID
    arch.setSourceChatroomId(room.getId());
    
    // User 스냅샷
    arch.setUserId(user != null ? user.getId() : null);
    arch.setUserEmailSnapshot(user != null ? user.getEmail() : null);
    
    // Chatbot 스냅샷
    arch.setChatbotId(chatbot != null ? chatbot.getId() : null);
    arch.setChatbotNameSnapshot(chatbot != null ? chatbot.getName() : null);
    arch.setChatbotTypeSnapshot(chatbot != null ? chatbot.getBotType() : null);
    arch.setChatbotIntimacyLevelSnapshot(chatbot != null ? chatbot.getIntimacyLevel() : null);
    
    // 평면 컬럼
    arch.setName(room.getName());
    arch.setDescription(room.getDescription());
    
    // concept 추출 (settings JSONB에서)
    String concept = extractConceptFromSettings(room.getSettings());
    arch.setConcept(concept);
    
    // Meta JSONB 구성
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode meta = mapper.createObjectNode();
    meta.put("concept", concept);
    
    // intimacyLevel: progress 또는 chatbot에서 가져오기
    int intimacyLevel = progress != null ? progress.getIntimacyLevel() 
                        : (chatbot != null ? chatbot.getIntimacyLevel() : 1);
    meta.put("intimacyLevel", intimacyLevel);
    
    // testModel
    if (room.getSettings() != null && room.getSettings().has("testModel")) {
        meta.put("testModel", room.getSettings().get("testModel").asText());
    }
    
    // contextData 복사
    if (room.getContextData() != null) {
        meta.set("contextData", room.getContextData());
    }
    
    // source 정보
    meta.set("source", mapper.createObjectNode()
        .put("env", "prod")
        .put("service", "chat"));
    
    arch.setMeta(meta);
    
    // 기타 필드
    arch.setLastMessageAt(room.getLastMessageAt());
    arch.setSourceLastMessageId(room.getLastMessage() != null ? room.getLastMessage().getId() : null);
    arch.setIsArchived(room.getIsArchived());
    arch.setIsDeleted(room.getIsDeleted());
    arch.setSourceCreatedAt(room.getCreatedAt());
    arch.setSourceUpdatedAt(room.getUpdatedAt());
    arch.setArchivedAt(LocalDateTime.now());
    
    return arch;
}

private String extractConceptFromSettings(JsonNode settings) {
    if (settings != null && settings.has("concept")) {
        JsonNode conceptNode = settings.get("concept");
        if (conceptNode.isTextual()) {
            return conceptNode.asText().toUpperCase();
        }
    }
    return "FRIEND"; // 기본값
}
```

### 2. Message → arch_messages + arch_agent_results

```java
public void archiveMessage(Message message, ArchChatroom archChatroom, 
                          Map<UUID, UUID> storeIdMap, Map<String, String> usageRequestIdMap) {
    ObjectMapper mapper = new ObjectMapper();
    
    // 1. arch_messages 생성
    ArchMessage archMsg = new ArchMessage();
    archMsg.setArchChatroomId(archChatroom.getId());
    archMsg.setSourceMessageId(message.getId());
    archMsg.setSourceParentMessageId(message.getParentMessage() != null 
        ? message.getParentMessage().getId() : null);
    archMsg.setSenderType(message.getSenderType());
    archMsg.setSenderId(message.getSenderId());
    archMsg.setContent(message.getContent());
    archMsg.setContentType(message.getContentType());
    
    // ✅ sequence_number: 운영과 동일 (그대로 복사, 재계산 불필요)
    archMsg.setSequenceNumber(message.getSequenceNumber());
    
    // ✅ turn_number: 운영과 동일 (그대로 복사, 재계산 불필요)
    // Bot 응답부터 User 응답까지를 하나의 턴으로 묶은 단위
    // Bot 메시지: 새로운 턴 시작, User 메시지: turn_number = 0 (미완료 턴)
    archMsg.setTurnNumber(message.getTurnNumber());
    archMsg.setTokenCount(message.getTokenCount());
    archMsg.setProcessingTimeMs(message.getProcessingTimeMs());
    archMsg.setIsEdited(message.getIsEdited());
    archMsg.setEditedAt(message.getEditedAt());
    archMsg.setIsDeleted(message.getIsDeleted());
    archMsg.setDeletedAt(message.getDeletedAt());
    archMsg.setSourceCreatedAt(message.getCreatedAt());
    archMsg.setSourceUpdatedAt(message.getUpdatedAt());
    archMsg.setArchivedAt(LocalDateTime.now());
    
    // 2. metadata 파싱 및 agent_results 분리
    Map<String, UUID> agentResultIds = new HashMap<>();
    String originalMetadataJson = null;
    
    if (message.getMetadata() != null && !message.getMetadata().isBlank()) {
        try {
            JsonNode metadata = mapper.readTree(message.getMetadata());
            originalMetadataJson = message.getMetadata();
            
            // intimacy agent 결과 추출
            if (metadata.has("userMessageAnalysis") && 
                metadata.get("userMessageAnalysis").has("intimacy")) {
                JsonNode intimacy = metadata.get("userMessageAnalysis").get("intimacy");
                UUID intimacyId = createAgentResult(archMsg.getId(), "intimacy", intimacy, message, metadata);
                agentResultIds.put("intimacy", intimacyId);
            }
            
            // vocabulary agent 결과 추출
            if (metadata.has("botResponseAnalysis") && 
                metadata.get("botResponseAnalysis").has("vocabulary")) {
                JsonNode vocab = metadata.get("botResponseAnalysis").get("vocabulary");
                // 여러 단어는 단일 레코드에 배열로 저장
                if (vocab.has("words") && vocab.get("words").isArray()) {
                    UUID vocabId = createVocabularyAgentResult(archMsg.getId(), vocab, message, metadata);
                    agentResultIds.put("voca", vocabId);
                }
            }
            
            // usage 정보 추출 (arch_agent_results에 저장)
            if (metadata.has("usage")) {
                JsonNode usage = metadata.get("usage");
                String requestId = extractRequestId(usage, message);
                if (requestId != null) {
                    usageRequestIdMap.put(message.getId().toString(), requestId);
                }
            }
            
            // metadata_json 구성
            ObjectNode metadataJson = mapper.createObjectNode();
            metadataJson.set("analysis", mapper.createObjectNode()
                .put("language", "ko")
                .set("safety", mapper.createObjectNode()
                    .put("flag", false)
                    .put("reason", (String) null)));
            
            ObjectNode link = mapper.createObjectNode();
            // storeId 조회
            UUID storeId = storeIdMap.get(message.getId());
            if (storeId != null) {
                link.put("storeId", storeId.toString());
            }
            // usageRequestId
            String usageRequestId = usageRequestIdMap.get(message.getId().toString());
            if (usageRequestId != null) {
                link.put("usageRequestId", usageRequestId);
            }
            // agentResults
            if (!agentResultIds.isEmpty()) {
                ObjectNode agentResults = mapper.createObjectNode();
                agentResultIds.forEach((type, id) -> agentResults.put(type, id.toString()));
                link.set("agentResults", agentResults);
            }
            metadataJson.set("link", link);
            
            // originalMetadata 백업
            metadataJson.set("originalMetadata", metadata);
            
            archMsg.setMetadataJson(metadataJson);
            
        } catch (Exception e) {
            log.error("Metadata 파싱 실패: messageId={}", message.getId(), e);
            // 기본 metadata_json 설정
            archMsg.setMetadataJson(mapper.createObjectNode());
        }
    } else {
        archMsg.setMetadataJson(mapper.createObjectNode());
    }
    
    archMessageRepository.save(archMsg);
}

/**
 * Intimacy agent 결과 생성
 */
private UUID createAgentResult(UUID archMessageId, String agentType, 
                               JsonNode payload, Message sourceMessage, JsonNode originalMetadata) {
    ArchAgentResult result = new ArchAgentResult();
    result.setArchMessageId(archMessageId);
    result.setAgentType(agentType);
    result.setPayloadJson(payload);
    
    // usage 정보에서 추출
    if (originalMetadata.has("usage")) {
        JsonNode usage = originalMetadata.get("usage");
        // request_id는 별도 조회 필요 (billing.ai_usage_events에서)
        // provider, model, tokens 등은 metadata에서 추출 가능
    }
    
    result.setSourceCreatedAt(sourceMessage.getCreatedAt());
    result.setArchivedAt(LocalDateTime.now());
    
    archAgentResultRepository.save(result);
    return result.getId();
}

/**
 * Vocabulary agent 결과 생성 (여러 단어는 배열로 저장)
 */
private UUID createVocabularyAgentResult(UUID archMessageId, JsonNode vocabNode, 
                                         Message sourceMessage, JsonNode originalMetadata) {
    // words 배열을 그대로 payload로 저장
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode payload = mapper.createObjectNode();
    payload.set("words", vocabNode.get("words"));
    
    ArchAgentResult result = new ArchAgentResult();
    result.setArchMessageId(archMessageId);
    result.setAgentType("voca");
    result.setPayloadJson(payload);
    result.setSourceCreatedAt(sourceMessage.getCreatedAt());
    result.setArchivedAt(LocalDateTime.now());
    
    archAgentResultRepository.save(result);
    return result.getId();
}
```

---

## 주의사항 및 제약조건

### 1. sequence_number와 turn_number

**sequence_number**:
- 운영과 동일 (메시지 순서 번호, 1, 2, 3, 4, ...)
- 운영의 `sequence_number`를 그대로 복사 (재계산 불필요)
- 채팅방 내에서 고유하므로 UNIQUE 제약 유지

**turn_number**:
- 운영과 동일 (메시지 순서 번호를 묶어 1턴으로 하는 단위)
- Bot 응답부터 User 응답까지를 하나의 턴으로 묶음
- Bot 메시지: 새로운 턴 시작 (nextTurnNumber)
- User 메시지: turn_number = 0 (미완료 턴, User 메시지 수신 시 이전 턴 완료 처리)
- System 메시지: 가장 최근 Bot 메시지의 턴 번호 사용
- 운영의 `turn_number`를 그대로 복사 (재계산 불필요)

### 2. FK 제약 없음 (Archive 독립성)

**설계 결정**: `arch_chatrooms` 테이블에 FK 제약 없음
- `user_id`, `chatbot_id`는 UUID만 저장 (참조용)
- 스냅샷 컬럼(`user_email_snapshot`, `chatbot_name_snapshot` 등)으로 필요한 정보 보존
- 운영 스키마 변경/삭제 시에도 archive에 영향 없음
- 완전한 독립 저장소로 관리/감사 목적에 적합

**주의사항**:
- 애플리케이션 레벨에서 UUID 유효성 검증 필요
- 잘못된 UUID 삽입 방지를 위한 검증 로직 구현 권장

### 3. voca 여러 단어 처리

**문제**: UNIQUE 제약 (arch_message_id, agent_type)
- 여러 단어가 있을 경우 단일 레코드에 배열로 저장

**해결**: 이미 구현에 반영됨 (words 배열)

### 4. 외부 테이블 연결

**storeId 조회**:
```sql
SELECT id FROM store_schema.stores 
WHERE message_id = ? AND is_deleted = false;
```

**usageRequestId 조회**:
```sql
SELECT request_id FROM billing.ai_usage_events 
WHERE chatroom_id = ? AND event_time BETWEEN ? AND ?;
```

---

## 최종 체크리스트

### 스키마 정의
- [x] arch_chatrooms 테이블 정의
- [x] arch_messages 테이블 정의
- [x] arch_agent_results 테이블 정의
- [x] arch_ingestion_state 테이블 정의
- [x] 인덱스 정의
- [x] 외래키 제약조건 정의

### JSONB 구조
- [x] arch_chatrooms.meta 구조 정의
- [x] arch_messages.metadata_json 구조 정의
- [x] arch_agent_results.payload_json 구조 정의 (운영과 일치)

### 데이터 변환
- [x] ChatRoom → arch_chatrooms 변환 로직
- [x] Message → arch_messages 변환 로직
- [x] Metadata → arch_agent_results 분리 로직
- [x] sequence_number 복사 (운영과 동일)

### 제약조건 수정
- [ ] UNIQUE 제약 조건 수정 (uq_arch_messages_room_seq 제거)
- [ ] voca 여러 단어 처리 확인

---

## 참고 문서

- `DATASET_STRUCTURE_PROPOSAL.md`: 초기 스키마 제안
- `ARCHIVE_SCHEMA_IMPROVEMENTS.md`: 개선 사항 및 조율 결과
- `ARCHIVE_SCHEMA_COMPATIBILITY_ANALYSIS.md`: 호환성 분석 상세

