# 안읽음 푸시 메시지 기능 개발 계획

> 키톡처럼 안읽음 메시지로 표시하고, 안읽음 푸시가 있을 경우 해당 메시지가 그리팅으로 오는 채팅방을 생성하는 기능

---

## 1. 배경 및 문제

- **요구사항**: 새 푸시 직전에 왔던 푸시를 확인하고 싶음
- **제약**: 프론트에서는 푸시 알림을 지우면 스토리지에 저장/읽기가 불가능함 (OS 제약)
- **해결 방향**: 백엔드에서 "안읽은 푸시"를 추적하고, 앱 진입 시 조회 API로 제공

---

## 2. 아키텍처 개요

```
[푸시 발송] → PushDeliveryLog / UnreadPushLog 저장
                    ↓
[앱 진입]   → GET /api/notifications/unread → 안읽음 푸시 목록
                    ↓
[알림 뱃지] → 안읽음 건수 표시 (키톡 스타일)
                    ↓
[푸시 클릭] → 해당 푸시 → 채팅방 생성 + 그리팅(startMessage)
[조회 완료] → POST /api/notifications/unread/mark-read
```

---

## 3. 상세 개발 계획

### 3.1 백엔드 (User 서비스)

#### 3.1.1 데이터 모델 확장

**옵션 A: 기존 `PushDeliveryLog` 확장**

- `read_at` 컬럼 추가: `NULL`이면 안읽음, 값이 있으면 읽음
- 장점: 기존 테이블 활용
- 단점: `PushDeliveryLog`는 현재 "일일 1회 dedup"용으로, chatroomId 대신 chatbotId를 기록

**옵션 B: 신규 테이블 `unread_push_logs` 생성 (권장)**

```sql
CREATE TABLE user_schema.unread_push_logs (
  id BIGSERIAL PRIMARY KEY,
  user_id UUID NOT NULL,
  push_type VARCHAR(20) NOT NULL,  -- 'CHATROOM_CREATE' | 'NEW_MESSAGE'
  chatbot_id UUID,
  chatroom_id UUID,
  message_id UUID,
  concept VARCHAR(20),
  topic VARCHAR(255),
  start_message TEXT,
  title VARCHAR(255),
  body TEXT,
  sent_at TIMESTAMPTZ NOT NULL,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  read_at TIMESTAMPTZ
);

CREATE INDEX idx_unread_push_user_read ON user_schema.unread_push_logs(user_id, read_at);
```

- `read_at`이 NULL이면 안읽음
- 푸시 발송 시점에 이 테이블에 insert
- 읽음 처리 시 `read_at` 업데이트

#### 3.1.2 API 설계

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/notifications/unread` | 안읽음 푸시 목록 조회 (페이지네이션) |
| POST | `/api/notifications/unread/mark-read` | 일괄 읽음 처리 (id 배열 또는 "all") |
| POST | `/api/notifications/unread/{id}/mark-read` | 단건 읽음 처리 |

**응답 예시 (GET /api/notifications/unread)**

```json
{
  "content": [
    {
      "id": 1,
      "pushType": "CHATROOM_CREATE",
      "chatbotId": "uuid",
      "concept": "coworker",
      "topic": "일상",
      "startMessage": "오늘 날씨 좋다~",
      "title": "직장 동료",
      "body": "오늘 날씨 좋다~",
      "sentAt": "2026-03-03T09:00:00+09:00"
    }
  ],
  "totalElements": 3,
  "unreadCount": 3
}
```

#### 3.1.3 푸시 발송 시점 연동

- `PushNotificationService.sendByTopic()` / `sendChatroomCreatePush()` / `sendToUser()` 호출 **성공 시** `unread_push_logs`에 insert
- 기존 `PushDeliveryLog`는 일일 dedup용으로 유지 (변경 없음)

### 3.2 백엔드 (Chat 서비스)

#### 3.2.1 기존 흐름 활용

- `GET /api/deeplink/chatroom/create?chatbotId=&topic=&concept=&intimacyLevel=`  
  - 이미 `createRoomWithTopic` 지원
- `GreetingService.sendGreeting(..., startMessage)`  
  - `startMessage`가 있으면 이를 기반으로 그리팅 생성 (이미 구현됨)

**추가 필요 사항**

- 앱에서 안읽음 푸시 항목 클릭 시:
  1. `GET /api/deeplink/chatroom/create?chatbotId=&topic=&concept=` 호출 (startMessage는 푸시 payload에서 전달받음)
  2. 채팅방 생성 후 `startMessage`로 greeting 요청
- `DeeplinkController` 또는 Greeting 트리거 API에 `startMessage` 쿼리 파라미터 추가 검토

### 3.3 프론트엔드 (앱)

#### 3.3.1 안읽음 푸시 목록 UI

- 앱 진입 시 `GET /api/notifications/unread` 호출
- 뱃지/알림 센터에 안읽음 건수 표시
- 목록에서 항목 클릭 → 해당 푸시의 `chatbotId`, `concept`, `topic`, `startMessage`로 채팅방 생성 API 호출 후 화면 이동
- 이동 성공 시 `POST /api/notifications/unread/mark-read` (해당 id)

#### 3.3.2 푸시 클릭 처리

- FCM data에서 `chatbotId`, `concept`, `topic`, `startMessage` 등 추출
- 채팅방 생성 API 호출 시 `startMessage` 전달
- 그리팅에 `startMessage`가 반영되도록 (이미 백엔드 지원)

### 3.4 데이터 흐름 (안읽음 푸시 → 그리팅)

1. 푸시 발송: `sendByTopic` → FCM 전송 → `unread_push_logs` insert
2. 사용자가 푸시를 "읽지 않고" 지움 → 스토리지 접근 불가
3. 앱 재진입: `GET /api/notifications/unread` → 서버에서 안읽음 목록 반환
4. 사용자가 안읽음 항목 클릭:
   - `GET /api/deeplink/chatroom/create?chatbotId=&topic=&concept=` + `startMessage` (Body 또는 별도 파라미터)
   - Chat 서비스가 채팅방 생성 후 greeting 트리거 (startMessage 사용)
5. 읽음 처리: `POST /api/notifications/unread/{id}/mark-read`

---

## 4. 구현 순서

| 순서 | 작업 | 담당 |
|------|------|------|
| 1 | `unread_push_logs` 테이블 마이그레이션 | User |
| 2 | `UnreadPushLog` 엔티티, Repository | User |
| 3 | `PushNotificationService`에서 푸시 발송 성공 시 `UnreadPushLog` insert | User |
| 4 | `GET /api/notifications/unread`, `POST .../mark-read` API | User | ✅ 완료 |
| 5 | Chat: `createRoomWithTopic` / Greeting에 `startMessage` 전달 경로 확인·보완 | Chat | (기존 greeting/start API 활용, 변경 불필요) |
| 6 | 앱: 안읽음 목록 조회 및 뱃지 UI | App |
| 7 | 앱: 안읽음 클릭 → 채팅방 생성 + 그리팅 연동 | App |

---

## 5. 주의사항

- **중복 채팅방**: 동일 topic + chatbot + user 조합에 대해 기존 `createRoomWithTopic` 동작 확인 필요 (신규 생성 vs 기존 반환)
- **읽음 처리 타이밍**: 채팅방 진입 완료 후 읽음 처리 권장 (진입 실패 시 재시도 가능)
- **만료 정책**: 오래된 안읽음 푸시 정리 배치 (예: 7일) 검토
