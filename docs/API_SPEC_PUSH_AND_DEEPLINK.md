# 푸시·딥링크 API 명세

> 이번에 개발한 **푸시 알림** 및 **딥링크(채팅방 생성/라우팅)** API만 정리한 명세입니다.  
> Gateway 기준 경로: `http://localhost:8080` (실서비스는 해당 호스트로 교체).

---

## 1. 공통

### 1.1 인증

- **클라이언트 요청**: `Authorization: Bearer {accessToken}` (JWT)
- **Gateway**: JWT 검증 후 `X-User-Id` 헤더를 백엔드로 전달
- 딥링크·채팅방 생성 API는 `X-User-Id`(UUID) 필수

### 1.2 Content-Type

- 요청: `application/json` (Body 있는 경우)
- 응답: `application/json`

---

## 2. 푸시 알림 API (User 서비스)

Base path: `/api/notifications`

### 2.1 FCM 토큰 등록

**요청**

```
POST /api/notifications/register
```

| 구분 | 내용 |
|------|------|
| 헤더 | `X-User-Id`: 사용자 UUID (Gateway가 JWT에서 주입) |
| Body | `{ "token": "fcm_token_string", "platform": "android" }` 또는 `"ios"` |

**응답**

- `200 OK`: 성공 (Body 없음)
- `400 Bad Request`: 잘못된 userId 또는 등록 실패

---

### 2.2 푸시 발송 (내부용)

**요청**

```
POST /api/notifications/send
```

| 구분 | 내용 |
|------|------|
| Body | `userId`(UUID), `title`, `body`, `chatroomId`(UUID, 선택), `messageId`(UUID, 선택) |

**동작**

- 해당 사용자 FCM 토큰으로 푸시 전송
- data 페이로드: `chatroomId`, `messageId`(있을 때), `sentAt`(KST, ISO-8601)
- 알림 설정·당일 중복 발송 정책에 따라 스킵될 수 있음

**응답**

- `200 OK`: 성공 (Body 없음)
- `400 Bad Request`: 실패

---

### 2.3 안읽음 푸시 조회 (2026-03 추가)

**요청**

```
GET /api/notifications/unread?page=0&size=20
```

| 구분 | 내용 |
|------|------|
| 헤더 | `Authorization: Bearer {accessToken}`, `X-User-Id` (Gateway 주입) |
| 쿼리 | `page`(기본 0), `size`(기본 20) |

**응답** (200 OK)

```json
{
  "content": [{ "id": 1, "pushType": "CHATROOM_CREATE", "chatbotId": "uuid", "concept": "coworker", "topic": "일상", "startMessage": "...", "title": "직장 동료", "body": "...", "sentAt": "2026-03-03T09:00:00+09:00" }],
  "totalElements": 3,
  "totalPages": 1,
  "number": 0,
  "size": 20,
  "unreadCount": 3
}
```

### 2.4 안읽음 푸시 읽음 처리 (2026-03 추가)

```
POST /api/notifications/unread/mark-read
POST /api/notifications/unread/{id}/mark-read
```

- Body(일괄): `{ "ids": [1, 2, 3] }` — 지정 id만 읽음 처리. `ids`가 null/빈 배열이면 전체 읽음 처리.
- 단건: `POST /api/notifications/unread/1/mark-read`

### 2.5 푸시 발송 로그 조회

**요청**

```
GET /api/notifications/logs?userId={uuid}&page=0&size=20
```

| 쿼리 | 타입 | 필수 | 기본값 | 설명 |
|------|------|------|--------|------|
| userId | UUID | O | - | 사용자 ID |
| page | int | X | 0 | 페이지 번호 |
| size | int | X | 20 | 페이지 크기 |

**응답** (200 OK)

페이지 형식. `content` 배열 + 페이지 메타.

```json
{
  "content": [
    {
      "id": 1,
      "userId": "uuid",
      "chatroomId": "uuid",
      "sentDate": "2026-01-28",
      "createdAt": "2026-01-28T10:00:00"
    }
  ],
  "totalElements": 100,
  "totalPages": 5,
  "size": 20,
  "number": 0,
  "first": true,
  "last": false
}
```

---

### 2.6 주제 기반 푸시 발송 (관리자/스케줄러용)

**요청**

```
POST /api/notifications/send-by-topic
```

| 구분 | 내용 |
|------|------|
| Body | `userId`(UUID, 필수), `chatbotId`(UUID, 필수), `topic`(선택), `concept`(선택, 기본 "FRIEND"), `intimacyLevel`(선택, 기본 2, 1~3) |

**동작**

- 주제·컨셉에 맞는 제목/본문 생성 후 해당 사용자에게 FCM 1회 발송
- FCM **data**에 채팅방 생성용 딥링크 정보 포함: `deeplink`, `chatbotId`, `topic`, `concept`, `intimacyLevel`, `sentAt`(KST)

**응답**

- `200 OK`: Body `"주제 기반 푸시가 전송되었습니다."`
- `400 Bad Request`: userId/chatbotId 누락, FCM 토큰 없음 등 (메시지로 사유 반환)

---

### 2.7 FCM 페이로드 (참고) — 2026-03 표준화

**notification 공통**
- `title`: 기본값 `"도란도란"` (앱 브랜드)
- `body`: 실제 메시지 내용

**기존 채팅방 진입용** (2.2 send 사용 시)
- `data`: `deeplink`, `universalLink`, `chatroomId`, `messageId`, `startMessage`, `sentAt`

**채팅방 생성용** (2.4 send-by-topic, test-chatroom-push 사용 시)
- `data` (순서): `concept`, `chatbotId`, `topic`, `startMessage`, `deeplink`, `sentAt`

---

## 3. 딥링크 API

### 3.1 딥링크 채팅방 생성/조회 (Chat 서비스)

푸시 또는 앱 딥링크에서 "주제 기반 채팅방" 생성/조회 시 호출.

**요청**

```
GET /api/deeplink/chatroom/create?chatbotId={uuid}&topic={주제}&concept=FRIEND&intimacyLevel=2
```

| 구분 | 내용 |
|------|------|
| 헤더 | `X-User-Id`: 사용자 UUID (필수, Gateway 주입) |
| 쿼리 | `chatbotId`(필수), `topic`(선택), `concept`(선택, 기본 "FRIEND"), `intimacyLevel`(선택, 기본 2, 1~3) |

**동작**

- 해당 사용자·챗봇으로 기존 채팅방 조회 또는 생성
- `topic`이 있으면 `contextData.sessionData.currentTopic`에 저장

**응답** (200 OK)

채팅방 정보 (ChatRoomResponse).

```json
{
  "id": "uuid",
  "userId": "uuid",
  "chatbotId": "uuid",
  "name": "대화",
  "description": null,
  "lastMessageAt": null,
  "lastMessageId": null,
  "isArchived": false,
  "isDeleted": false,
  "createdAt": "2026-01-28T10:00:00",
  "updatedAt": "2026-01-28T10:00:00",
  "concept": "FRIEND",
  "intimacyLevel": 2
}
```

- `400 Bad Request`: X-User-Id 또는 chatbotId 누락 등

---

### 3.2 딥링크 라우팅 (User 서비스)

path 한 줄로 이동할 화면(screen)과 파라미터(params) 조회.

**요청**

```
GET /api/deeplink/route?path={path}
```

| 구분 | 내용 |
|------|------|
| 헤더 | `X-User-Id`: 사용자 UUID (필수) |
| 쿼리 | `path`: 딥링크 경로 (예: `/chatroom/{chatroomId}`, `/archive/{storeId}`) |

**응답** (200 OK)

```json
{
  "screen": "chatroom",
  "params": {
    "userId": "uuid",
    "chatroomId": "uuid"
  }
}
```

| screen | 설명 | params 예시 |
|--------|------|-------------|
| chatroom | 기존 채팅방 | chatroomId, userId |
| chatroomCreate | 채팅방 생성 화면 | userId, action |
| archive | 표현 보관함 | storeId, userId |
| unknown | 미지원 path | path, userId |

- `400 Bad Request`: path 또는 X-User-Id 누락

---

## 4. Gateway 라우팅 요약

| 경로 | 대상 서비스 |
|------|-------------|
| `/api/notifications/**` | User |
| `/api/deeplink/route` | User |
| `/api/deeplink/chatroom/**` | Chat |

---

## 5. 관련 문서

- **클라이언트 구현**: [PUSH_AND_DEEPLINK_CLIENT_GUIDE.md](./PUSH_AND_DEEPLINK_CLIENT_GUIDE.md)
- **푸시 클릭 → 채팅방 진입**: [PUSH_TO_CHAT_FLOW.md](./PUSH_TO_CHAT_FLOW.md)
- **정합성 검증**: [API_PUSH_DEEPLINK_CONSISTENCY_CHECK.md](./API_PUSH_DEEPLINK_CONSISTENCY_CHECK.md)
