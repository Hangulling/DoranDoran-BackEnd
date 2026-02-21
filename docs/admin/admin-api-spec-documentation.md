# Admin API 명세서(구현 반영)

**작성일**: 2026-01-17  
**범위**: 이번 구현에 포함된 Admin API

## 1. 공통 규칙

- 모든 날짜/시간은 ISO-8601 문자열
- 페이지네이션 기본: `page`(0-based), `size`
- **Auth API** 응답: `{ data: ... }`
- **Admin API** 응답: DTO 단독 JSON

---

## 1.1 인증/권한 전제

- 모든 Admin API는 `Authorization: Bearer <accessToken>` 필요
- 사용자 로그인 이후 `ROLE_ADMIN` 권한 확인 필요
- 게이트웨이가 `X-User-Id` 헤더(UUID)를 주입하며, Admin API는 해당 값 사용
- 내부 호출(User Service -> Chat Service)은 HMAC 헤더 필요
  - `X-User-Id`, `X-Auth-Ts`, `X-Auth-Sign`

---

## 1.2 공통 에러 응답 (현재 구현 기준)

- 게이트웨이 `JwtAuthFilter`는 401 시 **응답 바디 없이** 상태 코드만 반환
- Chat Service `HmacAuthInterceptor`도 401/500 시 **응답 바디 없이** 상태 코드만 반환
- User Service Admin Controller는 예외 시 `ResponseEntity.internalServerError().build()`로 **응답 바디 없음**
- User Service Admin Controller는 유효성 오류 시 `ResponseEntity.badRequest().build()`로 **응답 바디 없음**

### 400 Bad Request

```
HTTP/1.1 400 Bad Request
(empty body)
```

### 401 Unauthorized

```
HTTP/1.1 401 Unauthorized
(empty body)
```

### 403 Forbidden

```
HTTP/1.1 403 Forbidden
(empty body)
```

### 500 Internal Server Error

```
HTTP/1.1 500 Internal Server Error
(empty body)
```

### Chat Service ErrorResponse (예외 처리기 기준)

```json
{
  "timestamp": "2026-01-17T09:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "잘못된 요청입니다.",
  "path": "/api/admin/conversations",
  "validationErrors": {
    "fieldName": "오류 메시지"
  }
}
```

---

## 1.3 스키마(전체/추가 구현)

### 1.3.1 전체 스키마 요약 (관리자 기능 관점)

- `user_schema`: `app_user`, `profiles`, `settings`, `prompt_versions`, `prompt_actives`, `admin_users`, `admin_roles`, `admin_user_roles`, `review_tickets`, `review_ticket_items`, `admin_audit_logs`
- `chat_schema`: `chatrooms`, `messages`
- `archive_schema`: `arch_chatrooms`, `arch_messages`, `management_queue` (관리자 조회/관리 큐)

### 1.3.2 user_schema (Admin 추가 테이블)

#### admin_users

| 컬럼 | 타입 | 비고 |
| --- | --- | --- |
| id | BIGSERIAL | PK |
| username | VARCHAR(255) | UNIQUE |
| password_hash | VARCHAR(255) | |
| name | VARCHAR(255) | |
| is_active | BOOLEAN | 기본 true |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |

#### admin_roles

| 컬럼 | 타입 | 비고 |
| --- | --- | --- |
| id | BIGSERIAL | PK |
| role_name | VARCHAR(50) | UNIQUE |
| description | VARCHAR(255) | |
| created_at | TIMESTAMP | |

#### admin_user_roles

| 컬럼 | 타입 | 비고 |
| --- | --- | --- |
| admin_user_id | BIGINT | FK → admin_users.id |
| admin_role_id | BIGINT | FK → admin_roles.id |
| created_at | TIMESTAMP | |

#### review_tickets

| 컬럼 | 타입 | 비고 |
| --- | --- | --- |
| id | BIGSERIAL | PK |
| conversation_id | UUID | |
| status | VARCHAR(20) | 기본 OPEN |
| agent_type | VARCHAR(50) | |
| note | TEXT | |
| created_by | UUID | |
| assignee | UUID | |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |
| done_at | TIMESTAMP | |

#### review_ticket_items

| 컬럼 | 타입 | 비고 |
| --- | --- | --- |
| id | BIGSERIAL | PK |
| ticket_id | BIGINT | FK → review_tickets.id |
| message_id | UUID | |
| agent_type | VARCHAR(50) | |
| snapshot_json | JSONB | |
| created_at | TIMESTAMP | |

#### admin_audit_logs

| 컬럼 | 타입 | 비고 |
| --- | --- | --- |
| id | BIGSERIAL | PK |
| admin_user_id | UUID | |
| action_type | VARCHAR(50) | |
| target_type | VARCHAR(50) | |
| target_id | BIGINT | |
| summary | VARCHAR(500) | |
| before_json | JSONB | |
| after_json | JSONB | |
| ip | VARCHAR(50) | |
| user_agent | VARCHAR(500) | |
| created_at | TIMESTAMP | |

### 1.3.3 user_schema (프롬프트 테이블)

#### prompt_versions

| 컬럼 | 타입 | 비고 |
| --- | --- | --- |
| id | BIGINT | PK |
| agent_type | VARCHAR(50) | |
| concept | VARCHAR(20) | |
| intimacy_level | INTEGER | |
| version | VARCHAR(20) | |
| content | TEXT | |
| file_path | VARCHAR(500) | |
| memo | VARCHAR(500) | |
| parent_version_id | BIGINT | FK → prompt_versions.id |
| created_by | UUID | |
| created_at | TIMESTAMP | |

#### prompt_actives

| 컬럼 | 타입 | 비고 |
| --- | --- | --- |
| env | VARCHAR | PK (복합키) |
| agent_type | VARCHAR | PK (복합키) |
| concept | VARCHAR | PK (복합키) |
| intimacy_level | INTEGER | PK (복합키) |
| prompt_version_id | BIGINT | FK → prompt_versions.id |
| activated_by | UUID | |
| activated_at | TIMESTAMP | |

### 1.3.4 chat_schema (관리자 조회 참조 테이블)

#### chatrooms

| 컬럼 | 타입 | 비고 |
| --- | --- | --- |
| id | UUID | PK |
| user_id | UUID | |
| chatbot_id | UUID | |
| name | VARCHAR(100) | |
| description | VARCHAR(1000) | |
| settings | JSONB | |
| context_data | JSONB | |
| last_message_at | TIMESTAMP | |
| last_message_id | UUID | |
| is_archived | BOOLEAN | |
| is_deleted | BOOLEAN | |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |

#### messages

| 컬럼 | 타입 | 비고 |
| --- | --- | --- |
| id | UUID | PK |
| chatroom_id | UUID | |
| sender_type | VARCHAR(20) | user/bot/system |
| sender_id | UUID | |
| content | TEXT | |
| content_type | VARCHAR(20) | |
| metadata | JSONB | |
| parent_message_id | UUID | |
| sequence_number | BIGINT | |
| turn_number | BIGINT | |
| token_count | INTEGER | |
| processing_time_ms | INTEGER | |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |
| is_edited | BOOLEAN | |
| edited_at | TIMESTAMP | |
| is_deleted | BOOLEAN | |
| deleted_at | TIMESTAMP | |

### 1.3.5 archive_schema (관리자 조회 참조 테이블)

#### arch_chatrooms

| 컬럼 | 타입 | 비고 |
| --- | --- | --- |
| id | UUID | PK |
| source_chatroom_id | UUID | UNIQUE |
| user_id | UUID | |
| user_email_snapshot | VARCHAR(320) | |
| chatbot_id | UUID | |
| chatbot_name_snapshot | VARCHAR(100) | |
| chatbot_type_snapshot | VARCHAR(20) | |
| chatbot_intimacy_level_snapshot | INTEGER | |
| name | VARCHAR(100) | |
| description | TEXT | |
| concept | VARCHAR(50) | |
| last_message_at | TIMESTAMP | |
| source_last_message_id | UUID | |
| is_archived | BOOLEAN | |
| is_deleted | BOOLEAN | |
| source_created_at | TIMESTAMP | |
| source_updated_at | TIMESTAMP | |
| archived_at | TIMESTAMP | |
| source_deleted_at | TIMESTAMP | |
| meta | JSONB | |

#### arch_messages

| 컬럼 | 타입 | 비고 |
| --- | --- | --- |
| id | UUID | PK |
| arch_chatroom_id | UUID | FK → arch_chatrooms.id |
| source_message_id | UUID | UNIQUE |
| source_parent_message_id | UUID | |
| sender_type | VARCHAR(20) | user/bot/system |
| sender_id | UUID | |
| content | TEXT | |
| content_type | VARCHAR(20) | |
| sequence_number | BIGINT | |
| turn_number | BIGINT | |
| token_count | INTEGER | |
| processing_time_ms | INTEGER | |
| is_edited | BOOLEAN | |
| edited_at | TIMESTAMP | |
| is_deleted | BOOLEAN | |
| deleted_at | TIMESTAMP | |
| source_created_at | TIMESTAMP | |
| source_updated_at | TIMESTAMP | |
| archived_at | TIMESTAMP | |
| metadata_json | JSONB | |

#### management_queue

| 컬럼 | 타입 | 비고 |
| --- | --- | --- |
| id | UUID | PK, 기본값 `gen_random_uuid()` |
| queue_type | VARCHAR(50) | CORRECTION, DELETION |
| status | VARCHAR(20) | PENDING, COMPLETED |
| request_data | JSONB | 요청 데이터 |
| result_data | JSONB | 처리 결과 |
| admin_name | VARCHAR(100) | 등록한 관리자 |
| admin_ip | VARCHAR(45) | 접속 IP |
| created_at | TIMESTAMP | 기본값 `CURRENT_TIMESTAMP` |
| updated_at | TIMESTAMP | |
| completed_at | TIMESTAMP | |
| error_message | TEXT | |

---

## 2. 리뷰 티켓 (User Service)

### 2.1 티켓 생성

**POST** `/api/admin/review-tickets`

**요청 바디**
```json
{
  "conversationId": "uuid",
  "agentType": "intimacy",
  "note": "검토 필요",
  "items": [
    {
      "messageId": "uuid",
      "agentType": "intimacy",
      "snapshotJson": {}
    }
  ]
}
```

**응답**
```json
{
  "id": 1002,
  "conversationId": "uuid",
  "status": "OPEN",
  "agentType": "intimacy",
  "note": "검토 필요",
  "createdBy": "uuid",
  "assignee": null,
  "createdAt": "2026-01-17T09:00:00Z",
  "updatedAt": "2026-01-17T09:00:00Z",
  "doneAt": null,
  "items": [
    {
      "messageId": "uuid",
      "agentType": "intimacy",
      "snapshotJson": {}
    }
  ]
}
```

---

### 2.2 티켓 상세 조회

**GET** `/api/admin/review-tickets/{ticketId}`

**응답**
```json
{
  "id": 1001,
  "conversationId": "uuid",
  "status": "OPEN",
  "agentType": "intimacy",
  "note": "검토 필요",
  "createdBy": "uuid",
  "assignee": null,
  "createdAt": "2026-01-17T09:00:00Z",
  "updatedAt": "2026-01-17T09:00:00Z",
  "doneAt": null,
  "items": [
    {
      "messageId": "uuid",
      "agentType": "intimacy",
      "snapshotJson": {}
    }
  ]
}
```

---

### 2.3 티켓 목록 조회

**GET** `/api/admin/review-tickets`

**쿼리 파라미터**
- `status` (optional, default: OPEN)
- `agentType` (optional)
- `page` (default: 0)
- `size` (default: 20)

**응답**
```json
{
  "content": [
    {
      "id": 1001,
      "conversationId": "uuid",
      "status": "OPEN",
      "agentType": "intimacy",
      "note": "검토 필요",
      "createdBy": "uuid",
      "assignee": "uuid",
      "createdAt": "2026-01-17T09:00:00Z",
      "updatedAt": "2026-01-17T09:00:00Z",
      "doneAt": null
    }
  ],
  "page": {
    "number": 0,
    "size": 20,
    "totalPages": 1,
    "totalElements": 1
  }
}
```

---

### 2.4 상태/에이전트 타입별 카운트

**GET** `/api/admin/review-tickets/counts`

**쿼리 파라미터**
- `status` (optional, default: OPEN)

**응답**
```json
{
  "total": 10,
  "byAgentType": {
    "intimacy": 5,
    "conver": 3,
    "voca": 2
  }
}
```

---

### 2.5 티켓 메모 수정

**PATCH** `/api/admin/review-tickets/{ticketId}`

**요청 바디**
```json
{
  "note": "메모 수정"
}
```

**응답**
```json
{
  "id": 1001,
  "conversationId": "uuid",
  "status": "OPEN",
  "agentType": "intimacy",
  "note": "메모 수정",
  "createdBy": "uuid",
  "assignee": "uuid",
  "createdAt": "2026-01-17T09:00:00Z",
  "updatedAt": "2026-01-17T09:00:00Z",
  "doneAt": null
}
```

---

### 2.6 티켓 삭제

**DELETE** `/api/admin/review-tickets/{ticketId}`

**응답**
```
HTTP/1.1 200 OK
(empty body)
```

---

### 2.7 다건 처리 완료

**POST** `/api/admin/review-tickets/complete`

**요청 바디**
```json
{
  "ticketIds": [1001, 1002]
}
```

**응답**
```
HTTP/1.1 200 OK
(empty body)
```

---

## 3. 감사 로그 (User Service)

### 3.1 감사 로그 조회

**GET** `/api/admin/audit-logs`

**쿼리 파라미터**
- `adminUserId` (optional)
- `actionType` (optional)
- `from` (optional, ISO-8601)
- `to` (optional, ISO-8601)
- `page` (default: 0)
- `size` (default: 20)

**응답**
```json
{
  "content": [
    {
      "id": 1,
      "adminUserId": "uuid",
      "actionType": "REVIEW_EXPORT",
      "targetType": "REVIEW_TICKET",
      "targetId": 1002,
      "summary": "티켓 1002 생성",
      "beforeJson": null,
      "afterJson": { "conversationId": "uuid" },
      "ip": "1.2.3.4",
      "userAgent": "Mozilla/5.0",
      "createdAt": "2026-01-17T09:00:00Z"
    }
  ],
  "page": {
    "number": 0,
    "size": 20,
    "totalPages": 1,
    "totalElements": 1
  }
}
```

---

## 4. 채팅 로그 (User Service -> Chat Service)

### 4.0 채팅룸 옵션 조회 (archive)

**GET** `/api/admin/chat-logs/chatrooms`

**응답**
```json
[
  {
    "id": "uuid",
    "name": "대화방 이름",
    "concept": "friend",
    "userEmailSnapshot": "user@example.com"
  }
]
```

---

### 4.1 대화 목록 조회

**GET** `/api/admin/conversations`

**쿼리 파라미터**
- `userEmail` (optional)
- `from` (optional, ISO-8601)
- `to` (optional, ISO-8601)
- `roomKey` (optional)
- `intimacyLevel` (optional)
- `dataSource` (optional, `chat` | `archive`, default: `chat`)
- `page` (default: 0)
- `size` (default: 20)

**응답**
```json
{
  "content": [
    {
      "conversationId": "uuid",
      "userId": "uuid",
      "roomKey": "friend",
      "intimacyLevel": 1,
      "lastMessageAt": "2026-01-17T09:00:00Z",
      "lastSequenceNumber": 123
    }
  ],
  "page": {
    "number": 0,
    "size": 20,
    "totalPages": 1,
    "totalElements": 1
  }
}
```

---

### 4.2 대화 상세 조회

**GET** `/api/admin/conversations/{conversationId}`

**응답**
```json
{
  "conversationId": "uuid",
  "timeline": [
    {
      "messageId": "uuid",
      "senderType": "user",
      "content": "안녕!",
      "contentType": "text",
      "metadata": "{}",
      "sequenceNumber": 1,
      "turnNumber": 1,
      "createdAt": "2026-01-17T09:00:00Z"
    }
  ]
}
```

---

## 5. 프롬프트 관리 (User Service)

> **DB가 기준이며**, Chat Service는 **DB 우선, 파일 fallback**으로 프롬프트를 읽는다.  
> 활성화/저장-적용 시 DB에 저장하고, **Chat Service 파일 동기화를 시도**한다(실패해도 DB 활성화는 유지).

### 5.1 프롬프트 옵션

**GET** `/api/admin/prompts/options/agent-types`  
**GET** `/api/admin/prompts/options/concepts`  
**GET** `/api/admin/prompts/options/intimacy-levels`

**응답 (agent-types)**
```json
[
  { "value": "INTIMACY_ANALYSIS", "label": "친밀도 분석" },
  { "value": "INTIMACY_CORRECTION", "label": "친밀도 교정" },
  { "value": "VOCABULARY_EXTRACTION", "label": "어휘 추출" },
  { "value": "VOCABULARY_EXPLANATION", "label": "어휘 설명" },
  { "value": "CONVERSATION", "label": "대화" },
  { "value": "GREETING", "label": "인사" }
]
```

**응답 (concepts)**
```json
[
  { "value": "friend", "label": "친구" },
  { "value": "coworker", "label": "동료" },
  { "value": "boss", "label": "상사" },
  { "value": "senior", "label": "선배" },
  { "value": "honey", "label": "연인" }
]
```

**응답 (intimacy-levels)**
```json
[
  { "value": 1, "label": "레벨 1" },
  { "value": 3, "label": "레벨 3" }
]
```

---

### 5.2 Active 프롬프트 조회

**GET** `/api/admin/prompts/active`

**쿼리 파라미터**
- `agentType`
- `concept`
- `intimacyLevel`
- `env` (default: prod)

**응답**
```json
{
  "env": "prod",
  "agentType": "INTIMACY_ANALYSIS",
  "concept": "friend",
  "intimacyLevel": 1,
  "versionId": 12,
  "version": "v0.1",
  "content": "prompt...",
  "activatedAt": "2026-01-17T09:00:00Z"
}
```

---

### 5.3 프롬프트 테스트

**POST** `/api/admin/prompts/test`

**요청 바디**
```json
{
  "agentType": "INTIMACY_ANALYSIS",
  "concept": "friend",
  "intimacyLevel": 1,
  "inputText": "테스트 문장",
  "promptVersionId": 12
}
```

**응답**
```json
{
  "outputText": "테스트 응답",
  "latencyMs": 820,
  "tokens": 180
}
```

---

### 5.4 프롬프트 버전 관리

**POST** `/api/admin/prompts/versions`  
**GET** `/api/admin/prompts/versions`  
**GET** `/api/admin/prompts/versions/{versionId}`

#### 5.4.1 버전 생성

**POST** `/api/admin/prompts/versions`

**요청 바디**
```json
{
  "agentType": "INTIMACY_ANALYSIS",
  "concept": "friend",
  "intimacyLevel": 1,
  "content": "prompt...",
  "memo": "초안"
}
```

**응답**
```json
{
  "id": 101,
  "agentType": "INTIMACY_ANALYSIS",
  "concept": "friend",
  "intimacyLevel": 1,
  "version": "v0.2",
  "content": "prompt...",
  "filePath": "prompts/intimacy/analysis/friend_1.txt",
  "memo": "초안",
  "createdBy": "uuid",
  "createdAt": "2026-01-17T09:00:00Z"
}
```

---

#### 5.4.2 버전 목록 조회

**GET** `/api/admin/prompts/versions`

**쿼리 파라미터**
- `agentType`
- `concept`
- `intimacyLevel`
- `page` (default: 0)
- `size` (default: 10)
- `env` (optional, default: prod)

**응답**
```json
{
  "content": [
    {
      "id": 101,
      "agentType": "INTIMACY_ANALYSIS",
      "concept": "friend",
      "intimacyLevel": 1,
      "version": "v0.2",
      "content": "prompt...",
      "filePath": "prompts/intimacy/analysis/friend_1.txt",
      "memo": "초안",
      "createdBy": "uuid",
      "createdAt": "2026-01-17T09:00:00Z",
      "isActive": true
    }
  ],
  "page": {
    "number": 0,
    "size": 10,
    "totalPages": 1,
    "totalElements": 1
  }
}
```

---

#### 5.4.3 버전 상세 조회

**GET** `/api/admin/prompts/versions/{versionId}`

**쿼리 파라미터**
- `env` (optional, default: prod)

**응답**
```json
{
  "id": 101,
  "agentType": "INTIMACY_ANALYSIS",
  "concept": "friend",
  "intimacyLevel": 1,
  "version": "v0.2",
  "content": "prompt...",
  "filePath": "prompts/intimacy/analysis/friend_1.txt",
  "memo": "초안",
  "createdBy": "uuid",
  "createdAt": "2026-01-17T09:00:00Z",
  "isActive": true
}
```

---

### 5.5 활성화/롤백/저장 및 적용

**POST** `/api/admin/prompts/activate`  
**POST** `/api/admin/prompts/rollback`  
**POST** `/api/admin/prompts/save-and-activate`

#### 5.5.1 활성화

**POST** `/api/admin/prompts/activate`

**요청 바디**
```json
{
  "env": "prod",
  "agentType": "INTIMACY_ANALYSIS",
  "concept": "friend",
  "intimacyLevel": 1,
  "versionId": 101
}
```

**응답**
```
HTTP/1.1 200 OK
(empty body)
```

---

#### 5.5.2 롤백

**POST** `/api/admin/prompts/rollback`

**요청 바디**
```json
{
  "env": "prod",
  "agentType": "INTIMACY_ANALYSIS",
  "concept": "friend",
  "intimacyLevel": 1,
  "previousVersionId": 100
}
```

**응답**
```
HTTP/1.1 200 OK
(empty body)
```

---

#### 5.5.3 저장 및 적용

**POST** `/api/admin/prompts/save-and-activate`

**요청 바디**
```json
{
  "env": "prod",
  "agentType": "INTIMACY_ANALYSIS",
  "concept": "friend",
  "intimacyLevel": 1,
  "content": "prompt...",
  "memo": "즉시 적용"
}
```

**응답**
```json
{
  "id": 102,
  "agentType": "INTIMACY_ANALYSIS",
  "concept": "friend",
  "intimacyLevel": 1,
  "version": "v0.3",
  "content": "prompt...",
  "filePath": "prompts/intimacy/analysis/friend_1.txt",
  "memo": "즉시 적용",
  "createdBy": "uuid",
  "createdAt": "2026-01-17T09:00:00Z",
  "isActive": true
}
```

---

### 5.6 버전 활성화 상태 조회

**GET** `/api/admin/prompts/versions/{versionId}/active-status`

**쿼리 파라미터**
- `env` (optional, default: prod)

**응답**
```json
{
  "versionId": 101,
  "isActive": true,
  "env": "prod",
  "activatedAt": "2026-01-17T09:00:00Z",
  "activatedBy": "uuid"
}
```

---

### 5.7 파일 내용 조회

**GET** `/api/admin/prompts/file-content`

**쿼리 파라미터**
- `agentType`
- `concept`
- `intimacyLevel`

**응답**
```json
{
  "content": "현재 Chat Service의 파일 내용..."
}
```


