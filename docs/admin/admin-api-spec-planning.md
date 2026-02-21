# Admin API 명세(기획 반영 초안)

**작성일**: 2026-01-17  
**목적**: 기획 측 기능 명세를 반영하여 추가 스키마 작성 및 API 구현 계획을 정리

## 1. 문서 범위

- 본 문서는 `admin-schema-implementation-plan.md`와 별도의 **API 명세 초안**이다.
- 기획 측 기능 요구사항을 기준으로 **추가 스키마**와 **구현 예정 API**를 정리한다.
- UI/프론트 구현 상세는 포함하지 않는다.

---

## 1.1 서비스 구성/운영 방식 점검 요약

- 운영은 **MSA + 공유 DB(PostgreSQL)** 구조이며 스키마 분리(`auth_schema`, `user_schema`, `chat_schema` 등)
- 게이트웨이는 `/api/auth/**`, `/api/users/**`, `/api/chat/**`만 라우팅
- Admin API는 **User Service(`/api/admin/**`)**에 구현 중이며 프롬프트 테스트/동기화는 **Chat Service** 내부 API 사용
- 인증은 **Auth Service JWT**를 사용하며 Admin 프론트는 **ROLE_ADMIN** 체크
- Chat Service는 **HMAC 인터셉터**를 사용하고 있어 내부 호출 헤더 정합성 필요

---

## 1.2 확정된 구현 범위(운영 기준)

1. **Admin 인증은 기존 Auth API 사용**  
   - `/api/auth/login`, `/api/auth/refresh`, `/api/auth/logout`, `/api/auth/me`
   - Admin 권한은 `ROLE_ADMIN` 여부로 판별
2. **Admin API는 User Service에서 제공**  
   - `/api/admin/prompts/**`, `/api/admin/review-tickets/**`, `/api/admin/audit-logs`
3. **프롬프트 테스트/동기화는 Chat Service 내부 API 호출**  
   - User Service → Chat Service (`/api/admin/prompts/test`, `/api/admin/prompts/sync`)
4. **게이트웨이 라우팅 확정**  
   - `/api/admin/**` → **User Service**로 라우팅 추가
5. **HMAC 처리 방식 확정**  
   - User Service → Chat Service 내부 호출에 **HMAC 서명 추가**

---

## 2. 반영 대상 기능(요약)

1. 관리자 인증/인가 및 권한 분리
2. 사용자 채팅 내역 조회(리스트/상세)
3. 관리 필요 내역(티켓) 등록/조회/수정/삭제/완료
4. 프롬프트 테스트/편집/적용 및 버전 관리
5. 감사 로그 조회

---

## 3. 추가/보강 스키마 계획

### 3.1 Admin 기본 스키마

- `admin_users`, `admin_roles`, `admin_user_roles`
- 관리자 계정/역할 기반 접근제어(RBAC) 기반

### 3.2 관리 필요 내역(리뷰 티켓)

- `review_tickets`
  - 상태: OPEN/DONE
  - 에이전트 타입: intimacy/conver/voca
  - 메모/담당자/완료시각
- `review_ticket_items`
  - 티켓에 포함되는 항목 저장 (user/intimacy/conver/voca)
  - `snapshot_json`으로 당시 상태 보관

### 3.3 프롬프트 관리

- `prompt_versions`에 `created_by_admin_id` 추가
- `prompt_actives`에 `activated_by_admin_id` 추가
- 기존 데이터는 NULL 허용 후 점진적 이행

### 3.4 감사 로그

- `admin_audit_logs`
  - action_type, target_type, target_id, before_json/after_json, ip, user_agent

---

## 3.5 운영 기준 API/구현 상태 요약

### 인증(현행)
- **사용 API**: `/api/auth/login`, `/api/auth/refresh`, `/api/auth/logout`, `/api/auth/me`
- **권한 판별**: `ROLE_ADMIN` 필수

### 프롬프트 관리(현행, User Service)
- `/api/admin/prompts/active` (GET)
- `/api/admin/prompts/test` (POST) → User Service가 Chat Service 호출
- `/api/admin/prompts/versions` (GET/POST)
- `/api/admin/prompts/activate` (POST)
- `/api/admin/prompts/rollback` (POST)
- `/api/admin/prompts/save-and-activate` (POST)
- `/api/admin/prompts/options/*` (GET)
- `/api/admin/prompts/versions/{id}/active-status` (GET)
- `/api/admin/prompts/file-content` (GET)

### 리뷰 티켓(현행, User Service)
- `/api/admin/review-tickets` (GET)
- `/api/admin/review-tickets/counts` (GET)
- `/api/admin/review-tickets/{ticketId}` (PATCH/DELETE)
- `/api/admin/review-tickets/complete` (POST)
- `/api/admin/review-tickets` (POST)
- `/api/admin/review-tickets/{ticketId}` (GET)

### 채팅 로그(현행, User Service -> Chat Service)
- `/api/admin/chat-logs/chatrooms` (GET)
- `/api/admin/conversations` (GET)
- `/api/admin/conversations/{conversationId}` (GET)

### 감사 로그(현행, User Service)
- `/api/admin/audit-logs` (GET)

---

## 4. API 명세(현행 코드 기준)

> 표기 규칙
> - 모든 날짜/시간은 ISO-8601 문자열
> - 페이지네이션 기본: `page`(0-based), `size`
> - **Auth API** 응답: `{ data: ... }`
> - **Admin API** 응답: DTO 단독 JSON

### 4.1 공통/인증 (Auth Service)

**POST** `/api/auth/login`  
- **요약**: 관리자 로그인(JWT 발급)
- **정책**: 로그인 후 `ROLE_ADMIN` 확인
- **요청 바디**
```json
{
  "email": "admin@example.com",
  "password": "secret"
}
```
- **응답**
```json
{
  "data": {
    "accessToken": "jwt",
    "refreshToken": "jwt",
    "user": {
      "id": "uuid",
      "email": "admin@example.com",
      "role": "ROLE_ADMIN"
    }
  }
}
```

**POST** `/api/auth/logout`  
- **요약**: 로그아웃

**POST** `/api/auth/refresh`  
- **요약**: 토큰 갱신

**GET** `/api/auth/me`  
- **요약**: 현재 사용자 정보 조회

---

### 4.2 프롬프트 옵션 (User Service)

**GET** `/api/admin/prompts/options/agent-types`  
- **요약**: 에이전트 타입 옵션
- **응답**
```json
[
  { "value": "INTIMACY_ANALYSIS", "label": "친밀도 분석" },
  { "value": "CONVERSATION", "label": "대화" }
]
```

**GET** `/api/admin/prompts/options/concepts`  
- **요약**: 컨셉 옵션
- **응답**
```json
[
  { "value": "friend", "label": "친구" },
  { "value": "coworker", "label": "동료" }
]
```

**GET** `/api/admin/prompts/options/intimacy-levels`  
- **요약**: 친밀도 레벨 옵션
- **응답**
```json
[
  { "value": 1, "label": "레벨 1" },
  { "value": 3, "label": "레벨 3" }
]
```

---

### 4.3 프롬프트 운영 (User Service)

**GET** `/api/admin/prompts/active`  
- **요약**: 활성 프롬프트 조회
- **쿼리**
  - agentType, concept, intimacyLevel, env
- **응답**
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

**POST** `/api/admin/prompts/test`  
- **요약**: 프롬프트 테스트 실행
- **요청 바디**
```json
{
  "agentType": "INTIMACY_ANALYSIS",
  "concept": "friend",
  "intimacyLevel": 1,
  "inputText": "테스트 문장",
  "promptVersionId": 12
}
```
- **응답**
```json
{
  "outputText": "테스트 응답",
  "latencyMs": 820,
  "tokens": 180
}
```

**POST** `/api/admin/prompts/versions`  
- **요약**: 새 버전 생성
- **요청 바디**
```json
{
  "agentType": "INTIMACY_ANALYSIS",
  "concept": "friend",
  "intimacyLevel": 1,
  "content": "prompt...",
  "memo": "수정 사항"
}
```
- **응답**
```json
{
  "id": 13,
  "agentType": "INTIMACY_ANALYSIS",
  "concept": "friend",
  "intimacyLevel": 1,
  "version": "v0.2",
  "content": "prompt...",
  "filePath": "prompts/intimacy/analysis/friend_1.txt",
  "memo": "수정 사항",
  "createdBy": "uuid",
  "createdAt": "2026-01-17T09:00:00Z"
}
```

**GET** `/api/admin/prompts/versions`  
- **요약**: 버전 목록 조회
- **쿼리**
  - agentType, concept, intimacyLevel, page, size, env
- **응답**
```json
{
  "content": [
    {
      "id": 13,
      "agentType": "INTIMACY_ANALYSIS",
      "concept": "friend",
      "intimacyLevel": 1,
      "version": "v0.2",
      "memo": "수정 사항",
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

**GET** `/api/admin/prompts/versions/{versionId}`  
- **요약**: 버전 상세 조회
- **응답**
```json
{
  "id": 13,
  "agentType": "INTIMACY_ANALYSIS",
  "concept": "friend",
  "intimacyLevel": 1,
  "version": "v0.2",
  "content": "prompt...",
  "memo": "수정 사항",
  "isActive": true
}
```

**POST** `/api/admin/prompts/activate`  
- **요약**: 프롬프트 활성화(Active 전환)
- **요청 바디**
```json
{
  "env": "prod",
  "agentType": "INTIMACY_ANALYSIS",
  "concept": "friend",
  "intimacyLevel": 1,
  "versionId": 13
}
```
- **응답**
```json
{}
```

**POST** `/api/admin/prompts/rollback`  
- **요약**: 롤백(Active 전환)
- **요청 바디**
```json
{
  "env": "prod",
  "agentType": "INTIMACY_ANALYSIS",
  "concept": "friend",
  "intimacyLevel": 1,
  "previousVersionId": 12
}
```

**POST** `/api/admin/prompts/save-and-activate`  
- **요약**: 저장 및 적용(버전 생성 + 활성화)
- **요청 바디**
```json
{
  "env": "prod",
  "agentType": "INTIMACY_ANALYSIS",
  "concept": "friend",
  "intimacyLevel": 1,
  "content": "prompt...",
  "memo": "수정 사항"
}
```

**GET** `/api/admin/prompts/versions/{versionId}/active-status`  
- **요약**: 버전 활성화 상태 조회
- **응답**
```json
{
  "versionId": 13,
  "isActive": true,
  "env": "prod",
  "activatedAt": "2026-01-17T09:00:00Z",
  "activatedBy": "uuid"
}
```

**GET** `/api/admin/prompts/file-content`  
- **요약**: 현재 Chat Service의 파일 내용 조회
- **응답**
```json
{
  "content": "prompt..."
}
```

---

### 4.4 리뷰 티켓 (User Service)

**GET** `/api/admin/review-tickets`  
- **요약**: 티켓 목록 조회
- **쿼리**
  - status, agentType, page, size
- **응답**
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

**GET** `/api/admin/review-tickets/counts`  
- **요약**: 상태/에이전트 타입별 카운트
- **응답**
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

**PATCH** `/api/admin/review-tickets/{ticketId}`  
- **요약**: 티켓 메모 수정
- **요청 바디**
```json
{
  "note": "메모 수정"
}
```
- **응답**
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

**DELETE** `/api/admin/review-tickets/{ticketId}`  
- **요약**: 티켓 삭제

**POST** `/api/admin/review-tickets/complete`  
- **요약**: 다건 처리 완료
- **요청 바디**
```json
{
  "ticketIds": [1001, 1002]
}
```

**POST** `/api/admin/review-tickets`  
- **요약**: 티켓 생성(내보내기)
- **요청 바디**
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
- **응답**
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

**GET** `/api/admin/review-tickets/{ticketId}`  
- **요약**: 티켓 상세 조회
- **응답**
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

### 4.5 채팅 로그 (User Service -> Chat Service)

**GET** `/api/admin/chat-logs/chatrooms`  
- **요약**: 채팅룸 옵션 조회(보관 데이터 기준)
- **응답**
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

**GET** `/api/admin/conversations`  
- **요약**: 대화 목록 조회(필터/페이지)
  - `dataSource`: `chat` | `archive` (default: `chat`)

**GET** `/api/admin/conversations/{conversationId}`  
- **요약**: 대화 상세(타임라인) 조회
  - `dataSource`: `chat` | `archive` (default: `chat`)

---

### 4.6 감사 로그 (User Service)

**GET** `/api/admin/audit-logs`  
- **요약**: 감사 로그 조회

---

## 5. 감사 로그 기록 정책(요약)

- 프롬프트 생성/활성화/롤백 시 `admin_audit_logs` 기록
- 리뷰 티켓 생성/수정/삭제/완료 시 기록
- 로그 필드: action_type, target_type, target_id, summary, before_json, after_json, ip, user_agent

---

## 6. 결정 사항(최종)

1. **리뷰 티켓 수정 범위**: MVP는 **메모만 수정** 허용
2. **프롬프트 테스트 메타 저장**: 저장하지 않고 응답에만 포함
3. **관리자 세션/토큰 폐기**: 단기 토큰 + 로그아웃 시 블랙리스트 캐시
4. **프롬프트 활성화 캐시 갱신**: activate 호출 시 캐시 강제 갱신
5. **게이트웨이 라우팅**: `/api/admin/**` → User Service
6. **HMAC 처리**: User Service 내부 호출에 HMAC 서명 추가

---

## 7. 미구현 API 우선순위(확정)

- (현재 미구현 API 없음, 2026-01-17 기준)

---

## 8. 확정 전 필수 보완 체크(잔여)

1. (잔여 없음)
