# Archive 스키마 개선 사항 및 제안

> **최종 업데이트**: 2025-02-02  
> **상태**: 모든 제안사항 적용 완료 ✅

## 📌 최종 적용 요약

모든 제안사항이 적용되어 현재 운영 스키마와 호환되는 archive 스키마 구조가 확정되었습니다.

### ✅ 적용 완료 사항

1. **arch_chatrooms.meta**: `concept`, `intimacyLevel`, `testModel`, `contextData` 포함
2. **arch_messages.sequence_number**: "유저-봇" 대화 단위 저장 (옵션 A)
3. **arch_messages.metadata_json**: `link.agentResults`, `originalMetadata` 추가
4. **arch_agent_results.payload_json**: 운영 구조와 일치하도록 수정
   - `intimacy`: `feedback` 객체 구조, `alternativeExpressions`, `corrections` 포함
   - `voca`: `context` 객체 구조 유지
   - `conver`: payload 구조 정의

---

## 📌 1. arch_chatrooms.meta 구조

### ❌ 문제점

1. **`roomKey` vs `concept` 불일치**
   - 제안: `roomKey: "Friend"`
   - 현재 코드: `concept: "FRIEND"` 사용
   - → 현재 코드와 일치하지 않음

2. **`testModel` 누락**
   - 운영: `settings.testModel` 존재
   - 제안: `meta`에 포함 명시되지 않음

3. **`context_data` 누락**
   - 운영: `context_data` (jsonb) 존재
   - 제안: `meta`에 포함 고려 필요

4. **`intimacyLevel` 중복 가능성**
   - `chatbot_intimacy_level_snapshot`과 중복 가능
   - ✅ **조율 결과**: `intimacyLevel`은 `meta`에 포함 (데이터 분석 용이성)

5. **`sequence` 저장 방법**
   - "유저 응답 - 챗봇 대답"을 1시퀀스로 묶은 단위
   - 데이터 분석 및 AI 개발 용이성을 위해 필요
   - ✅ **조율 완료**: `arch_messages.sequence_number` 필드 사용 (옵션 A 선택)

### ✅ 최종 결정 구조 (모든 제안 적용)

```json
{
  "concept": "FRIEND",              // ✅ roomKey → concept으로 변경
  "intimacyLevel": 1,               // ✅ 조율 완료: meta에 포함
  "testModel": "...",                // ✅ settings.testModel (있으면)
  "tags": ["report", "hotfix"],     // admin 태깅
  "source": { "env": "prod", "service": "chat" },
  "contextData": { ... }            // ✅ context_data 전체 복사 (선택적)
}
```

**적용된 변경 사항**:
- ✅ `roomKey` → `concept`으로 변경
- ✅ `intimacyLevel` 포함 (조율 완료)
- ✅ `testModel` 포함
- ✅ `contextData` 포함 (선택적)
- ✅ `sequence`: `arch_messages.sequence_number` 필드 사용 (조율 완료, 옵션 A)

---

## 📌 2. arch_messages.metadata_json 구조

### ❌ 문제점

1. **구조 완전 불일치**
   - 운영: `userMessageAnalysis`, `botResponseAnalysis`, `usage` 구조
   - 제안: `analysis`, `link` 구조
   - → 완전히 다른 구조

2. **Agent 결과 참조 누락**
   - `userMessageAnalysis.intimacy` → `arch_agent_results`로 분리되지만
   - `metadata_json`에 참조가 없음

3. **usage 정보 처리 불명확**
   - `usage` 정보가 `arch_agent_results`에만 있는지, `metadata_json`에도 필요한지 불명확

### ✅ 최종 결정 구조 (모든 제안 적용)

```json
{
  "analysis": {
    "language": "ko",
    "safety": { "flag": false, "reason": null }
  },
  "link": {
    "storeId": "uuid",                    // store_schema.stores.id
    "usageRequestId": "req_...",          // billing.ai_usage_events.request_id
    "agentResults": {                     // ✅ 추가: arch_agent_results 참조
      "intimacy": "uuid",                  // arch_agent_results.id (agent_type='intimacy')
      "voca": "uuid",                      // arch_agent_results.id (agent_type='voca')
      "conver": "uuid"                     // arch_agent_results.id (agent_type='conver')
    }
  },
  "originalMetadata": { ... }             // ✅ 원본 metadata 전체 백업 (선택적)
}
```

**적용된 변경 사항**:
- ✅ `link.agentResults` 추가하여 `arch_agent_results` 참조
- ✅ `originalMetadata` 백업 필드 추가 (선택적, 원본 구조 보존용)

---

## 📌 3. arch_agent_results.payload_json 구조

### ❌ 문제점

#### 3.1 intimacy agent

**현재 운영 구조**:
```json
{
  "detectedLevel": 1,
  "correctedSentence": "나는 가아라야",
  "feedback": { "ko": "...", "en": "..." },  // ⚠️ 객체 구조
  "corrections": "...",
  "alternativeExpressions": [...]             // ❌ 누락
}
```

**제안된 구조**:
```json
{
  "detectedLevel": 1,
  "correctedSentence": "나는 가아라야",
  "feedback": "조금 더 자연스럽게 말하면 ...",  // ⚠️ 단일 문자열로 변경
  "confidence": 0.82                            // 신규 필드
}
```

**문제점**:
- `feedback`: 객체 → 문자열로 구조 변경
- `alternativeExpressions`: 누락
- `corrections`: 누락

#### 3.2 voca agent

**현재 운영 구조**:
```json
{
  "word": "매력",
  "difficulty": 2,
  "context": {                                // ⚠️ 객체 구조
    "roma": "...",
    "ko": "...",
    "en": "..."
  }
}
```

**제안된 구조**:
```json
{
  "word": "매력",
  "difficulty": 2,
  "context": "일상에서 ...",                  // ⚠️ 단일 문자열로 변경
  "definitions": ["...","..."],               // 신규 필드
  "examples": ["..."]                         // 신규 필드
}
```

**문제점**:
- `context`: 객체 → 문자열로 구조 변경
- `words` 배열: 운영은 배열인데 archive는 단일 객체 (여러 단어 처리 방법 불명확)

#### 3.3 conver agent

**문제점**:
- 현재 운영 구조에 `conver` agent 결과가 명시적으로 저장되지 않음
- bot 메시지의 `content`가 conver 결과인데, archive에 저장 방법 불명확

### ✅ 최종 결정 구조 (모든 제안 적용)

#### intimacy payload
```json
{
  "detectedLevel": 1,
  "correctedSentence": "나는 가아라야",
  "feedback": {                                // ✅ 객체 구조 유지 (운영과 일치)
    "ko": "조금 더 자연스럽게 말하면 ...",
    "en": "You can say it more naturally..."
  },
  "corrections": "...",                        // ✅ 추가 (운영과 일치)
  "alternativeExpressions": [                 // ✅ 추가 (운영과 일치)
    {
      "expression": "...",
      "tone": "...",
      "example": "..."
    }
  ],
  "confidence": 0.82                           // 신규 필드 (있으면)
}
```

#### voca payload
```json
{
  "word": "매력",
  "difficulty": 2,
  "context": {                                 // ✅ 객체 구조 유지 (운영과 일치)
    "roma": "...",
    "ko": "...",
    "en": "..."
  },
  "definitions": ["...","..."],                // 신규 필드 (있으면)
  "examples": ["..."]                          // 신규 필드 (있으면)
}
```

**여러 단어 처리 방법**: ✅ 옵션 1 선택 - 여러 레코드 생성
- 각 단어마다 `arch_agent_results` 레코드를 별도로 생성
- `arch_message_id` + `agent_type`이 동일하므로 UNIQUE 제약 위반 가능
- → **해결**: `agent_type`을 `voca_1`, `voca_2` 등으로 구분하거나, UNIQUE 제약 조건 수정 필요
- → **권장**: 단일 레코드에 배열로 저장 (`words: [...]`) 또는 UNIQUE 제약에서 `agent_type` 제외

#### conver payload
```json
{
  "response": "가아라 진짜 멋지지! ...",
  "tone": "friendly",
  "followUps": ["좋아하는 캐릭터 또 있어?"]  // 신규 필드 (있으면)
}
```

**적용된 변경 사항**:
- ✅ `intimacy.feedback`: 객체 구조 유지 (운영과 일치)
- ✅ `intimacy`: `alternativeExpressions`, `corrections` 포함
- ✅ `voca.context`: 객체 구조 유지 (운영과 일치)
- ⚠️ `voca`: 여러 단어 처리 방법 결정 필요 (UNIQUE 제약 고려)
- ✅ `conver`: payload 구조 정의

---

## 📌 4. sequence 저장 방법 (조율 완료)

### ✅ 결정 사항: 옵션 A - arch_messages.sequence_number 필드 사용

- **sequence 정의**: "유저 응답 - 챗봇 대답"을 1시퀀스로 묶은 단위
- **목적**: 데이터 분석 및 AI 개발 용이성
- **저장 위치**: `arch_messages.sequence_number` (INTEGER)
- **운영 스키마와의 차이**: `messages.turn_number`는 "Bot 응답 → User 응답"을 1턴으로 묶음 (반대 방향)

### 📋 스키마 구조

**DATASET_STRUCTURE_PROPOSAL.md에 이미 포함됨**:
```sql
CREATE TABLE archive_schema.arch_messages (
  ...
  sequence_number          bigint NOT NULL,  -- ✅ 이미 포함됨
  turn_number              bigint NOT NULL DEFAULT 0,
  ...
);

-- 인덱스 (이미 포함됨)
CREATE UNIQUE INDEX uq_arch_messages_room_seq
  ON archive_schema.arch_messages(arch_chatroom_id, sequence_number);

-- sequence별 조회를 위한 추가 인덱스 (권장)
CREATE INDEX idx_arch_messages_room_sequence
  ON archive_schema.arch_messages(arch_chatroom_id, sequence_number);
```

### 🔢 계산 로직

**규칙**:
- **User 메시지**: 새로운 sequence 시작 (이전 sequence의 최대값 + 1)
- **Bot 메시지**: 가장 최근 User 메시지의 sequence 사용
- **System 메시지**: 가장 최근 Bot 메시지의 sequence 사용

**예시**:
```
sequence 1:
  - User: "안녕" (sequence_number = 1)
  - Bot: "안녕하세요!" (sequence_number = 1)

sequence 2:
  - User: "오늘 날씨 어때?" (sequence_number = 2)
  - Bot: "오늘 날씨가 좋네요!" (sequence_number = 2)
```

### 💻 구현 예시

```java
/**
 * Archive 메시지의 sequence_number 계산
 */
public int calculateSequenceNumber(UUID archChatroomId, String senderType) {
    if ("user".equals(senderType)) {
        // User 메시지: 새로운 sequence 시작
        Integer maxSeq = archMessageRepository
            .findMaxSequenceByChatroomId(archChatroomId)
            .orElse(0);
        return maxSeq + 1;
    } else {
        // Bot/System 메시지: 가장 최근 User 메시지의 sequence 사용
        return archMessageRepository
            .findLatestUserMessageSequence(archChatroomId)
            .orElse(1); // User 메시지가 없으면 sequence 1 사용
    }
}
```

### 📊 데이터 분석 활용

**sequence별 조회 예시**:
```sql
-- 특정 채팅방의 sequence 1 조회
SELECT * FROM archive_schema.arch_messages
WHERE arch_chatroom_id = '...'
  AND sequence_number = 1
ORDER BY source_created_at;

-- 채팅방별 sequence 수 집계
SELECT 
  arch_chatroom_id,
  MAX(sequence_number) as total_sequences
FROM archive_schema.arch_messages
GROUP BY arch_chatroom_id;
```

---

## 📌 5. arch_messages 구조 확인

### ✅ sequence_number 필드

**현재 상태**: `DATASET_STRUCTURE_PROPOSAL.md`에 이미 포함됨
- 컬럼: `sequence_number bigint NOT NULL`
- 인덱스: `uq_arch_messages_room_seq` (UNIQUE, arch_chatroom_id, sequence_number)
- ✅ 추가 권장 인덱스: `idx_arch_messages_room_sequence` (성능 최적화)

**참고**: 운영 스키마의 `messages.sequence_number`와는 다른 의미
- 운영: 메시지 순서 번호 (1, 2, 3, ...)
- Archive: "유저-봇" 대화 단위 번호 (1, 1, 2, 2, ...)

---

## 📌 6. 선택적 개선 사항

1. **arch_agent_results의 `conver` agent 타입**
   - 처리 방법 명시 필요 (bot 메시지 content와의 관계)

2. **데이터 변환 로직 문서화**
   - 운영 → archive 변환 로직 명시

---

## 📋 최종 적용 체크리스트

### ✅ 적용 완료 사항

- [x] `arch_chatrooms.meta`: `intimacyLevel` 포함 (조율 완료)
- [x] `arch_messages`: `sequence_number` 필드 사용 (조율 완료, 옵션 A)
- [x] `arch_chatrooms.meta`: `roomKey` → `concept` 변경
- [x] `arch_chatrooms.meta`: `testModel` 포함
- [x] `arch_chatrooms.meta`: `contextData` 포함 (선택적)
- [x] `arch_messages.metadata_json`: `link.agentResults` 추가
- [x] `arch_messages.metadata_json`: `originalMetadata` 백업 필드 추가 (선택적)
- [x] `arch_agent_results.payload_json` (intimacy): `feedback` 객체 구조 유지
- [x] `arch_agent_results.payload_json` (intimacy): `alternativeExpressions`, `corrections` 포함
- [x] `arch_agent_results.payload_json` (voca): `context` 객체 구조 유지
- [x] `arch_agent_results.payload_json` (conver): payload 구조 정의

### ⚠️ 추가 검토 필요 사항

- [ ] `voca` 여러 단어 처리 방법: UNIQUE 제약 조건 고려 필요
  - 옵션 A: 단일 레코드에 `words: [...]` 배열로 저장
  - 옵션 B: UNIQUE 제약 조건 수정 (`arch_message_id` + `agent_type` + `word` 등)
- [ ] 데이터 변환 로직 구현 및 문서화

