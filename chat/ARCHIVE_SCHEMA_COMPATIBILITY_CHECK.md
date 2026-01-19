# Archive 스키마 호환성 점검 보고서

> **점검일**: 2025-02-02  
> **점검 대상**: 최신화된 archive 스키마 vs 현재 프로젝트 구조  
> **결과**: ✅ 호환 가능 (일부 구현 주의사항 있음)

---

## 📊 점검 결과 요약

| 항목 | 평가 | 상태 |
|------|------|------|
| **엔티티 매핑** | ✅ 양호 | ChatRoom, Message 엔티티와 완벽 매핑 |
| **JSONB 구조** | ✅ 양호 | 운영 구조와 일치하도록 수정 완료 |
| **외래키 관계** | ✅ 독립성 | FK 제약 없음, UUID만 저장 (독립성 강화) |
| **인덱스 전략** | ✅ 양호 | 조회 패턴에 맞게 설계됨 |
| **데이터 타입** | ✅ 양호 | 모든 타입 일치 |
| **제약조건** | ⚠️ 수정 필요 | UNIQUE 제약 조건 1개 수정 필요 |
| **데이터 변환** | ⚠️ 구현 필요 | 변환 로직 구현 필요 |

**전체 평가**: ✅ **호환 가능** (제약조건 수정 후 완벽 호환)

---

## 1. 엔티티 매핑 점검

### 1.1 ChatRoom → arch_chatrooms

| 운영 엔티티 (ChatRoom) | Archive 스키마 | 호환성 | 비고 |
|------------------------|----------------|--------|------|
| `id` | `source_chatroom_id` | ✅ | UUID 유지 |
| `user.id` | `user_id` + `user_email_snapshot` | ✅ | 스냅샷 추가 |
| `chatbot.id` | `chatbot_id` + `chatbot_*_snapshot` | ✅ | 스냅샷 추가 |
| `name` | `name` | ✅ | 동일 |
| `description` | `description` | ✅ | 동일 |
| `settings` (JsonNode) | `concept` (평면화) + `meta` | ✅ | 추출 로직 필요 |
| `contextData` (JsonNode) | `meta.contextData` | ✅ | JSONB 복사 |
| `lastMessageAt` | `last_message_at` | ✅ | 동일 |
| `lastMessage.id` | `source_last_message_id` | ✅ | UUID 유지 |
| `isArchived` | `is_archived` | ✅ | 동일 |
| `isDeleted` | `is_deleted` | ✅ | 동일 |
| `createdAt` | `source_created_at` | ✅ | 동일 |
| `updatedAt` | `source_updated_at` | ✅ | 동일 |

**호환성**: ✅ 완벽 매핑

### 1.2 Message → arch_messages

| 운영 엔티티 (Message) | Archive 스키마 | 호환성 | 비고 |
|------------------------|----------------|--------|------|
| `id` | `source_message_id` | ✅ | UUID 유지, UNIQUE |
| `chatRoom.id` | `arch_chatroom_id` | ✅ | FK 매핑 |
| `parentMessage.id` | `source_parent_message_id` | ✅ | UUID 유지 |
| `senderType` | `sender_type` | ✅ | 동일, CHECK 제약 |
| `senderId` | `sender_id` | ✅ | 동일 |
| `content` | `content` | ✅ | 동일 |
| `contentType` | `content_type` | ✅ | 동일, CHECK 제약 |
| `sequenceNumber` | `sequence_number` | ✅ | **운영과 동일, 그대로 복사** (메시지 순서 번호) |
| `turnNumber` | `turn_number` | ✅ | **운영과 동일, 그대로 복사** (메시지 순서 번호를 묶어 1턴으로 하는 단위) |
| `tokenCount` | `token_count` | ✅ | 동일 |
| `processingTimeMs` | `processing_time_ms` | ✅ | 동일 |
| `isEdited` | `is_edited` | ✅ | 동일 |
| `editedAt` | `edited_at` | ✅ | 동일 |
| `isDeleted` | `is_deleted` | ✅ | 동일 |
| `deletedAt` | `deleted_at` | ✅ | 동일 |
| `createdAt` | `source_created_at` | ✅ | 동일 |
| `updatedAt` | `source_updated_at` | ✅ | 동일 |
| `metadata` (String) | `metadata_json` (JSONB) + `arch_agent_results` | ⚠️ | **구조 변환 필요** |

**호환성**: ✅ 완벽 매핑 (metadata 변환 필요)

---

## 2. JSONB 구조 점검

### 2.1 arch_chatrooms.meta

**현재 코드에서 추출 가능한 데이터**:
- ✅ `concept`: `extractConceptFromSettings(room.getSettings())` 사용 가능
- ✅ `intimacyLevel`: `chatbot.getIntimacyLevel()` 또는 `intimacyProgress.getIntimacyLevel()`
- ✅ `testModel`: `room.getSettings().get("testModel")`
- ✅ `contextData`: `room.getContextData()` 직접 복사

**호환성**: ✅ 모든 필드 추출 가능

### 2.2 arch_messages.metadata_json

**운영 metadata 구조**:
```json
{
  "userMessageAnalysis": {
    "userMessageId": "uuid",
    "intimacy": { ... }
  },
  "botResponseAnalysis": {
    "vocabulary": { "words": [...] }
  },
  "usage": { "inputTokens": 100, "outputTokens": 50 }
}
```

**Archive metadata_json 구조**:
```json
{
  "analysis": { "language": "ko", "safety": {...} },
  "link": {
    "storeId": "uuid",
    "usageRequestId": "req_...",
    "agentResults": { "intimacy": "uuid", "voca": "uuid" }
  },
  "originalMetadata": { ... }
}
```

**변환 가능성**: ✅ 변환 가능
- `userMessageAnalysis.intimacy` → `arch_agent_results` (agent_type='intimacy')
- `botResponseAnalysis.vocabulary` → `arch_agent_results` (agent_type='voca')
- `usage` → `arch_agent_results` 또는 `link.usageRequestId`
- 원본은 `originalMetadata`에 백업

### 2.3 arch_agent_results.payload_json

**운영 구조와 일치 확인**:
- ✅ `intimacy.feedback`: 객체 구조 유지 (`{ "ko": "...", "en": "..." }`)
- ✅ `intimacy.alternativeExpressions`: 배열 구조 유지
- ✅ `intimacy.corrections`: 문자열 유지
- ✅ `voca.context`: 객체 구조 유지 (`{ "roma": "...", "ko": "...", "en": "..." }`)
- ✅ `voca.words`: 배열로 저장 (여러 단어 처리)

**호환성**: ✅ 운영 구조와 완벽 일치

---

## 3. 서비스 로직 호환성

### 3.1 concept 추출 로직

**현재 코드**:
```java
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

**Archive 적용**: ✅ 동일 로직 사용 가능

### 3.2 sequence_number와 turn_number 사용

**운영 로직**:
- `nextSequenceNumber()`: 메시지 순서 번호 (1, 2, 3, ...)
- `nextTurnNumber()`: "Bot 응답부터 User 응답까지" 턴 번호
  - Bot 메시지: 새로운 턴 시작 (nextTurnNumber)
  - User 메시지: turn_number = 0 (미완료 턴, User 메시지 수신 시 이전 턴 완료 처리)
  - System 메시지: 가장 최근 Bot 메시지의 턴 번호 사용

**Archive 로직**:
- 운영의 `sequence_number`를 그대로 복사 (재계산 불필요)
  - 메시지 순서 번호 (1, 2, 3, 4, ...)
- 운영의 `turn_number`를 그대로 복사 (재계산 불필요)
  - 메시지 순서 번호를 묶어 1턴으로 하는 단위
  - Bot 응답부터 User 응답까지를 하나의 턴으로 묶음

**호환성**: ✅ 운영과 동일 (그대로 복사)

### 3.3 metadata 변환

**운영**: `buildBotMetadata()` 메서드로 생성
**Archive**: 역변환하여 `arch_agent_results`로 분리

**호환성**: ✅ 변환 가능 (가이드 제공됨)

---

## 4. 외부 테이블 연결

### 4.1 store_schema.stores

**연결 방법**:
```sql
SELECT id FROM store_schema.stores 
WHERE message_id = ? AND is_deleted = false;
```

**호환성**: ✅ UUID로 연결 가능

### 4.2 billing.ai_usage_events

**연결 방법**:
```sql
SELECT request_id FROM billing.ai_usage_events 
WHERE chatroom_id = ? 
  AND event_time BETWEEN ? AND ?
ORDER BY event_time DESC
LIMIT 1;
```

**호환성**: ✅ request_id로 연결 가능

---

## 5. 제약조건 점검

### 5.1 UNIQUE 제약 조건

**sequence_number UNIQUE 제약**:
```sql
-- ✅ 운영과 동일: 채팅방 내에서 sequence_number는 고유
CREATE UNIQUE INDEX uq_arch_messages_room_seq
  ON archive_schema.arch_messages(arch_chatroom_id, sequence_number);
```

**상태**: ✅ 운영과 동일하게 유지

### 5.2 CHECK 제약 조건

- ✅ `sender_type`: `('user','bot','system')` - 운영과 일치
- ✅ `content_type`: `('text','code','system','json')` - 운영과 일치
- ✅ `agent_type`: `('intimacy','conver','voca')` - 운영과 일치

**호환성**: ✅ 완벽 일치

---

## 6. 인덱스 전략 점검

### 6.1 조회 패턴 분석

**예상 조회 패턴**:
1. 채팅방별 메시지 조회: `arch_chatroom_id`
2. sequence별 조회: `arch_chatroom_id, sequence_number`
3. 시간순 조회: `source_created_at`
4. 메타데이터 검색: `metadata_json` (GIN 인덱스)

**인덱스 구성**: ✅ 모든 패턴 커버

### 6.2 성능 고려사항

- ✅ GIN 인덱스: JSONB 필드 검색 최적화
- ✅ 복합 인덱스: 조회 패턴에 맞게 구성
- ✅ 조건부 인덱스: 필요시 추가 가능

**호환성**: ✅ 최적화됨

---

## 7. 최종 호환성 평가

### ✅ 호환 가능 항목

1. **엔티티 매핑**: 모든 필드 매핑 가능
2. **JSONB 구조**: 운영 구조와 일치
3. **데이터 타입**: 모든 타입 일치
4. **외래키 관계**: FK 제약 없음 (Archive 독립성 강화)
5. **인덱스 전략**: 조회 패턴에 최적화

### ⚠️ 구현 주의사항

1. **sequence_number 사용**: 운영의 `sequence_number`를 그대로 복사 (재계산 불필요)
2. **metadata 변환**: 운영 → Archive 구조 변환 로직 구현
3. **외부 테이블 연결**: storeId, usageRequestId 조회 로직 필요

### 📋 구현 체크리스트

- [ ] `extractConceptFromSettings()` 메서드 활용
- [ ] `archiveChatroom()` 메서드 구현
- [ ] `archiveMessage()` 메서드 구현 (sequence_number 그대로 복사)
- [ ] `createAgentResult()` 메서드 구현
- [ ] 외부 테이블 연결 로직 구현

---

## 8. 결론

**최종 평가**: ✅ **호환 가능**

현재 프로젝트 구조와 archive 스키마는 **완벽하게 호환**됩니다. 

- 모든 엔티티 필드가 매핑 가능
- JSONB 구조가 운영과 일치
- 데이터 변환 로직이 명확히 정의됨
- 제약조건 수정사항이 반영됨

**다음 단계**:
1. 스키마 생성 (SQL 실행)
2. 데이터 변환 로직 구현
3. Archive 서비스 개발
4. 테스트 및 검증

