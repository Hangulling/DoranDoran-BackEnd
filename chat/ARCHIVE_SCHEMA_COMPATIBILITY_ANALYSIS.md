# Archive 스키마 호환성 분석 보고서

**분석일**: 2025-02-02  
**대상**: `DATASET_STRUCTURE_PROPOSAL.md`의 archive_schema 설계  
**현재 운영 스키마**: `chat_schema.chatrooms`, `chat_schema.messages`

## 📋 요약

**전체 평가: ✅ 양호하나 일부 개선 필요**

제안된 archive 스키마는 현재 코드 구조와 **대체로 잘 호환**되지만, 다음 사항들을 개선하면 더욱 견고해집니다:

1. ✅ **잘 설계된 부분**: 스냅샷 컬럼, source ID 추적, agent 결과 분리
2. ⚠️ **개선 필요**: JSONB 구조 매핑, 일부 필드 누락, 데이터 변환 로직

---

## 1. arch_chatrooms 호환성 분석

### 1.1 컬럼 매핑 비교

| 운영 스키마 (chat_schema.chatrooms) | Archive 스키마 (arch_chatrooms) | 호환성 | 비고 |
|-------------------------------------|--------------------------------|--------|------|
| `id` | `source_chatroom_id` | ✅ | UUID 유지, UNIQUE 제약 적절 |
| `user_id` | `user_id` + `user_email_snapshot` | ✅ | 스냅샷 추가로 삭제 대비 적절 |
| `chatbot_id` | `chatbot_id` + `chatbot_*_snapshot` | ✅ | 스냅샷으로 삭제 대비 적절 |
| `name` | `name` | ✅ | 동일 |
| `description` | `description` | ✅ | 동일 |
| `settings.concept` | `concept` (평면화) | ✅ | JSONB에서 추출하여 평면화 적절 |
| `settings.testModel` | `meta` 내부 | ⚠️ | `meta`에 포함 필요 (문서화 필요) |
| `context_data` | ❌ 누락 | ⚠️ | `meta`에 포함 고려 필요 |
| `last_message_at` | `last_message_at` | ✅ | 동일 |
| `last_message_id` | `source_last_message_id` | ✅ | 적절 |
| `is_archived` | `is_archived` | ✅ | 동일 |
| `is_deleted` | `is_deleted` | ✅ | 동일 |
| `created_at` | `source_created_at` | ✅ | 적절 |
| `updated_at` | `source_updated_at` | ✅ | 적절 |
| `settings` (전체) | `meta` (부분) | ⚠️ | 구조 매핑 명확화 필요 |

### 1.2 JSONB 구조 매핑

**현재 운영 구조** (`chatrooms.settings`):
```json
{
  "concept": "FRIEND|HONEY|COWORKER|SENIOR|BOSS",
  "testModel": "..."  // 선택적
}
```

**제안된 archive 구조** (`arch_chatrooms.meta`):
```json
{
  "roomKey": "Friend",              // concept과 동일 (대소문자만 다름)
  "intimacyLevel": 1,               // chatbot_intimacy_level_snapshot과 중복?
  "sequence": 13,                   // 출처 불명확
  "tags": ["report", "hotfix"],     // 신규 필드
  "source": { "env": "prod", "service": "chat" }
}
```

**문제점**:
1. ❌ `roomKey` vs `concept`: 현재 코드는 `concept`을 사용하므로 `roomKey` 대신 `concept` 사용 권장
2. ⚠️ `intimacyLevel`: `chatbot_intimacy_level_snapshot`과 중복 가능성
3. ❌ `sequence`: 출처 불명확 (운영 스키마에 없음)
4. ⚠️ `testModel`: `meta`에 포함되어야 하나 문서에 명시되지 않음

**개선 제안**:
```json
{
  "concept": "FRIEND",              // settings.concept에서 추출
  "testModel": "...",                // settings.testModel (있으면)
  "tags": ["report", "hotfix"],     // admin 태깅
  "source": { "env": "prod", "service": "chat" },
  "contextData": { ... }            // context_data 전체 복사 (선택적)
}
```

### 1.3 외래키 및 인덱스

✅ **외래키**: `ON DELETE SET NULL` 정책 적절 (archive 유지 우선)  
✅ **인덱스**: 조회 패턴에 맞게 잘 설계됨

---

## 2. arch_messages 호환성 분석

### 2.1 컬럼 매핑 비교

| 운영 스키마 (chat_schema.messages) | Archive 스키마 (arch_messages) | 호환성 | 비고 |
|-------------------------------------|--------------------------------|--------|------|
| `id` | `source_message_id` | ✅ | UUID 유지, UNIQUE 제약 적절 |
| `chatroom_id` | `arch_chatroom_id` | ✅ | archive FK로 적절 |
| `parent_message_id` | `source_parent_message_id` | ✅ | UUID 유지 |
| `sender_type` | `sender_type` | ✅ | 동일, CHECK 제약 적절 |
| `sender_id` | `sender_id` | ✅ | 동일 |
| `content` | `content` | ✅ | 동일 |
| `content_type` | `content_type` | ✅ | 동일, CHECK 제약 적절 |
| `sequence_number` | `sequence_number` | ✅ | 동일, UNIQUE 제약 적절 |
| `turn_number` | `turn_number` | ✅ | 동일 |
| `token_count` | `token_count` | ✅ | 동일 |
| `processing_time_ms` | `processing_time_ms` | ✅ | 동일 |
| `is_edited` | `is_edited` | ✅ | 동일 |
| `edited_at` | `edited_at` | ✅ | 동일 |
| `is_deleted` | `is_deleted` | ✅ | 동일 |
| `deleted_at` | `deleted_at` | ✅ | 동일 |
| `created_at` | `source_created_at` | ✅ | 적절 |
| `updated_at` | `source_updated_at` | ✅ | 적절 |
| `metadata` | `metadata_json` (부분) | ⚠️ | 구조 분리 필요 (아래 참조) |

### 2.2 JSONB 구조 매핑 (중요)

**현재 운영 구조** (`messages.metadata`):
```json
{
  "userMessageAnalysis": {
    "userMessageId": "uuid",
    "intimacy": {
      "detectedLevel": 1,
      "correctedSentence": "...",
      "feedback": { "ko": "...", "en": "..." },
      "corrections": "...",
      "alternativeExpressions": [...]
    }
  },
  "botResponseAnalysis": {
    "vocabulary": {
      "words": [
        {
          "word": "단어",
          "difficulty": 2,
          "context": { "roma": "...", "ko": "...", "en": "..." }
        }
      ]
    }
  },
  "usage": {
    "inputTokens": 100,
    "outputTokens": 50,
    "total": 150
  }
}
```

**제안된 archive 구조** (`arch_messages.metadata_json`):
```json
{
  "analysis": {
    "language": "ko",
    "safety": { "flag": false, "reason": null }
  },
  "link": {
    "storeId": "uuid",
    "usageRequestId": "req_..."
  }
}
```

**문제점**:
1. ❌ **구조 불일치**: 운영의 `metadata`와 archive의 `metadata_json` 구조가 완전히 다름
2. ❌ **Agent 결과 누락**: `userMessageAnalysis.intimacy`, `botResponseAnalysis.vocabulary`가 `arch_agent_results`로 분리되지만, `metadata_json`에 참조가 없음
3. ⚠️ **usage 정보**: `usage` 정보가 `arch_agent_results`에만 있고 `metadata_json`에는 없음

**개선 제안**:
```json
{
  "analysis": {
    "language": "ko",
    "safety": { "flag": false, "reason": null }
  },
  "link": {
    "storeId": "uuid",                    // store_schema.stores.id
    "usageRequestId": "req_...",          // billing.ai_usage_events.request_id
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

## 3. arch_agent_results 호환성 분석

### 3.1 설계 평가

✅ **잘 설계된 부분**:
- Agent 결과를 별도 테이블로 분리하여 정규화
- `agent_type` CHECK 제약으로 타입 보장
- `arch_message_id` + `agent_type` UNIQUE 제약으로 중복 방지
- `request_id`, `provider`, `model` 등 추적 정보 포함

### 3.2 JSONB 구조 매핑

**현재 운영 구조** (`messages.metadata`에서 추출):

**intimacy** (userMessageAnalysis.intimacy):
```json
{
  "detectedLevel": 1,
  "correctedSentence": "나는 가아라야",
  "feedback": { "ko": "...", "en": "..." },
  "corrections": "...",
  "alternativeExpressions": [...]
}
```

**제안된 archive 구조**:
```json
{
  "detectedLevel": 1,
  "correctedSentence": "나는 가아라야",
  "feedback": "조금 더 자연스럽게 말하면 ...",  // ⚠️ 구조 변경 (ko/en 분리 → 단일 문자열)
  "confidence": 0.82                            // 신규 필드
}
```

**문제점**:
1. ⚠️ `feedback`: 운영은 `{ "ko": "...", "en": "..." }` 구조인데 archive는 단일 문자열
2. ❌ `alternativeExpressions`: 누락됨
3. ⚠️ `corrections`: 누락됨 (또는 `feedback`에 포함?)

**voca** (botResponseAnalysis.vocabulary.words[0]):
```json
{
  "word": "매력",
  "difficulty": 2,
  "context": {
    "roma": "...",
    "ko": "...",
    "en": "..."
  }
}
```

**제안된 archive 구조**:
```json
{
  "word": "매력",
  "difficulty": 2,
  "context": "일상에서 ...",                    // ⚠️ 구조 변경 (객체 → 문자열)
  "definitions": ["...","..."],                  // 신규 필드
  "examples": ["..."]                            // 신규 필드
}
```

**문제점**:
1. ⚠️ `context`: 운영은 `{ "roma": "...", "ko": "...", "en": "..." }` 구조인데 archive는 단일 문자열
2. ⚠️ `words` 배열: 운영은 배열인데 archive는 단일 객체 (여러 단어 처리 방법 불명확)

**conver** (botResponseAnalysis는 없고, 실제로는 bot 메시지의 `content`가 conver 결과):
```json
{
  "response": "가아라 진짜 멋지지! ...",
  "tone": "friendly",
  "followUps": ["좋아하는 캐릭터 또 있어?"]
}
```

**문제점**:
1. ❌ 현재 운영 구조에 `conver` agent 결과가 명시적으로 저장되지 않음
2. ⚠️ bot 메시지의 `content`가 conver 결과인데, 이를 어떻게 archive에 저장할지 불명확

### 3.3 개선 제안

**intimacy payload**:
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

**voca payload**:
```json
{
  "word": "매력",
  "difficulty": 2,
  "context": {
    "roma": "...",
    "ko": "...",
    "en": "..."
  },
  "definitions": ["...","..."],  // 신규 필드 (있으면)
  "examples": ["..."]            // 신규 필드 (있으면)
}
```

**conver payload**:
```json
{
  "response": "가아라 진짜 멋지지! ...",
  "tone": "friendly",
  "followUps": ["좋아하는 캐릭터 또 있어?"]  // 신규 필드 (있으면)
}
```

---

## 4. 데이터 변환 로직 제안

### 4.1 ChatRoom → arch_chatrooms

```java
public ArchChatroom archiveChatroom(ChatRoom room, User user, Chatbot chatbot) {
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
    ObjectNode meta = objectMapper.createObjectNode();
    meta.put("concept", concept);
    if (room.getSettings() != null && room.getSettings().has("testModel")) {
        meta.put("testModel", room.getSettings().get("testModel").asText());
    }
    // context_data 복사 (선택적)
    if (room.getContextData() != null) {
        meta.set("contextData", room.getContextData());
    }
    meta.set("source", objectMapper.createObjectNode()
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
```

### 4.2 Message → arch_messages + arch_agent_results

```java
public void archiveMessage(Message message, ArchChatroom archChatroom) {
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
    archMsg.setSequenceNumber(message.getSequenceNumber());
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
    
    if (message.getMetadata() != null && !message.getMetadata().isBlank()) {
        JsonNode metadata = objectMapper.readTree(message.getMetadata());
        
        // intimacy agent 결과 추출
        if (metadata.has("userMessageAnalysis") && 
            metadata.get("userMessageAnalysis").has("intimacy")) {
            JsonNode intimacy = metadata.get("userMessageAnalysis").get("intimacy");
            UUID intimacyId = createAgentResult(archMsg.getId(), "intimacy", intimacy, message);
            agentResultIds.put("intimacy", intimacyId);
        }
        
        // vocabulary agent 결과 추출
        if (metadata.has("botResponseAnalysis") && 
            metadata.get("botResponseAnalysis").has("vocabulary")) {
            JsonNode vocab = metadata.get("botResponseAnalysis").get("vocabulary");
            // words 배열 처리
            if (vocab.has("words") && vocab.get("words").isArray()) {
                for (JsonNode word : vocab.get("words")) {
                    UUID vocabId = createAgentResult(archMsg.getId(), "voca", word, message);
                    agentResultIds.put("voca", vocabId); // 마지막 것만 저장 (또는 여러 개 생성)
                }
            }
        }
        
        // usage 정보 추출 (arch_agent_results에 저장하거나 metadata_json에 포함)
        if (metadata.has("usage")) {
            JsonNode usage = metadata.get("usage");
            // usage는 arch_agent_results에 저장하거나 metadata_json에 포함
        }
        
        // metadata_json 구성
        ObjectNode metadataJson = objectMapper.createObjectNode();
        metadataJson.set("analysis", objectMapper.createObjectNode()
            .put("language", "ko")
            .set("safety", objectMapper.createObjectNode()
                .put("flag", false)
                .put("reason", (String) null)));
        
        ObjectNode link = objectMapper.createObjectNode();
        // storeId, usageRequestId는 별도 조회 필요
        if (!agentResultIds.isEmpty()) {
            ObjectNode agentResults = objectMapper.createObjectNode();
            agentResultIds.forEach((type, id) -> agentResults.put(type, id.toString()));
            link.set("agentResults", agentResults);
        }
        metadataJson.set("link", link);
        
        // 원본 metadata 백업 (선택적)
        metadataJson.set("originalMetadata", metadata);
        
        archMsg.setMetadataJson(metadataJson);
    }
    
    archMessageRepository.save(archMsg);
}

private UUID createAgentResult(UUID archMessageId, String agentType, 
                               JsonNode payload, Message sourceMessage) {
    ArchAgentResult result = new ArchAgentResult();
    result.setArchMessageId(archMessageId);
    result.setAgentType(agentType);
    result.setPayloadJson(payload);
    
    // usage 정보에서 추출 (metadata에서)
    // request_id, provider, model, latency_ms, tokens 등
    
    result.setSourceCreatedAt(sourceMessage.getCreatedAt());
    result.setArchivedAt(LocalDateTime.now());
    
    archAgentResultRepository.save(result);
    return result.getId();
}
```

---

## 5. 개선 사항 요약

### 5.1 필수 개선 사항

1. **arch_chatrooms.meta 구조 명확화**
   - `roomKey` → `concept`으로 변경 (현재 코드와 일치)
   - `testModel` 포함 명시
   - `contextData` 포함 여부 결정

2. **arch_messages.metadata_json 구조 수정**
   - `link.agentResults` 추가하여 `arch_agent_results` 참조
   - `originalMetadata` 백업 필드 추가 (선택적)

3. **arch_agent_results.payload_json 구조 수정**
   - `intimacy`: `feedback`을 `{ "ko": "...", "en": "..." }` 구조로 유지
   - `intimacy`: `alternativeExpressions`, `corrections` 포함
   - `voca`: `context`를 `{ "roma": "...", "ko": "...", "en": "..." }` 구조로 유지
   - `voca`: 여러 단어 처리 방법 명시 (여러 레코드 생성 vs 단일 레코드)

### 5.2 선택적 개선 사항

1. **arch_chatrooms.meta에 `sequence` 필드 제거 또는 출처 명시**
2. **arch_agent_results에 `conver` agent 타입 처리 방법 명시**
3. **데이터 변환 로직 문서화**

---

## 6. 최종 평가

| 항목 | 평가 | 비고 |
|------|------|------|
| **전체 구조** | ✅ 양호 | 스냅샷 컬럼, source ID 추적 적절 |
| **컬럼 매핑** | ✅ 양호 | 대부분 잘 매핑됨 |
| **JSONB 구조** | ⚠️ 개선 필요 | 운영 구조와 일치하도록 수정 필요 |
| **인덱스** | ✅ 양호 | 조회 패턴에 맞게 설계됨 |
| **외래키** | ✅ 양호 | SET NULL 정책 적절 |
| **데이터 변환** | ⚠️ 문서화 필요 | 변환 로직 명시 필요 |

**결론**: 제안된 archive 스키마는 **기본 구조는 잘 설계**되었으나, **JSONB 필드 구조를 운영 스키마와 일치**시키고 **데이터 변환 로직을 명확히** 하면 완벽하게 호환됩니다.


