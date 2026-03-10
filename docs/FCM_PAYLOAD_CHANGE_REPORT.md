# FCM 푸시 페이로드 변경 가능 여부 보고

> 2026-03-03: 페이로드 표준화 적용 완료 (`PushNotificationService`)

---

## 1. 제안하신 페이로드 형식

```json
{
  "message": {
    "token": "...",
    "notification": { 
      "title": "도란도란",
      "body": "새로운 메시지가 도착했습니다."
    },
    "data": {
      "concept": "coworker",
      "chatbotId": "..."
    }
  }
}
```

---

## 2. 결론: **변경 가능합니다**

제안하신 형식은 FCM HTTP v1 API 스펙과 정확히 일치합니다.  
- `message` 래퍼: FCM `POST projects/{project}/messages:send` 요청 Body 형식  
- `token`, `notification`, `data`: FCM Message 리소스 표준 필드

---

## 3. 기존 페이로드 vs 제안 페이로드

### 3.1 기존 페이로드 (현재 코드 기준)

#### (1) `sendToUser` — 기존 채팅방 메시지 푸시

| 필드 | 값 예시 |
|------|---------|
| **token** | FCM 등록 토큰 |
| **notification** | `{ "title": "...", "body": "..." }` |
| **data** | `deeplink`, `universalLink`, `chatroomId`, `messageId`, `startMessage`, `sentAt` |

```json
{
  "token": "fcm_token_string",
  "notification": {
    "title": "직장 동료",
    "body": "오늘 날씨 어때?"
  },
  "data": {
    "deeplink": "dorandoran://chat?roomId=xxx&messageId=yyy",
    "universalLink": "https://www.doran-chat.com/chat?roomId=xxx&messageId=yyy",
    "chatroomId": "uuid",
    "messageId": "uuid",
    "startMessage": "오늘 날씨 어때?",
    "sentAt": "2026-03-03T09:00:00+09:00"
  }
}
```

#### (2) `sendByTopic` / `sendChatroomCreatePush` — 채팅방 생성용 푸시

| 필드 | 값 예시 |
|------|---------|
| **token** | FCM 등록 토큰 |
| **notification** | `{ "title": "직장 동료", "body": "greeting 문구" }` |
| **data** | `deeplink`, `chatbotId`, `topic`, `concept`, `startMessage`, `sentAt` |

```json
{
  "token": "fcm_token_string",
  "notification": {
    "title": "직장 동료",
    "body": "오늘 날씨 어때?"
  },
  "data": {
    "deeplink": "dorandoran://chatroom/create?chatbotId=xxx&topic=일상&concept=coworker",
    "chatbotId": "uuid",
    "topic": "일상",
    "concept": "coworker",
    "startMessage": "오늘 날씨 어때?",
    "sentAt": "2026-03-03T09:00:00+09:00"
  }
}
```

### 3.2 제안하신 페이로드

```json
{
  "message": {
    "token": "...",
    "notification": { 
      "title": "도란도란",
      "body": "새로운 메시지가 도착했습니다."
    },
    "data": {
      "concept": "coworker",
      "chatbotId": "..."
    }
  }
}
```

- `notification`: FCM 규격에 맞음  
- `data`: `concept`, `chatbotId` 추가는 문제 없음 (자유 key-value)

---

## 4. 변경 시 고려사항

### 4.1 data 필드 축소

제안 페이로드에는 다음 필드가 없습니다.

| 필드 | 용도 | 포함 권장 |
|------|------|-----------|
| `topic` | 채팅방 주제 | 채팅방 생성용이면 있으면 좋음 |
| `startMessage` | 그리팅 문구 | 안읽음 → 그리팅 연동 시 필요 |
| `deeplink` | 앱 딥링크 | 클릭 시 이동 경로 |
| `chatroomId` | 기존 채팅방 진입 | 기존 채팅방 푸시 시 필요 |
| `messageId` | 메시지 스크롤 위치 | 기존 채팅방 푸시 시 필요 |
| `sentAt` | 발송 시각 | 로깅·정렬용 |

`concept`, `chatbotId`만 있어도 동작은 가능하나, 기능을 온전히 유지하려면 위 필드들을 `data`에 유지하는 것을 권장합니다.

### 4.2 권장 확장 페이로드 (채팅방 생성용)

```json
{
  "message": {
    "token": "...",
    "notification": { 
      "title": "도란도란",
      "body": "새로운 메시지가 도착했습니다."
    },
    "data": {
      "concept": "coworker",
      "chatbotId": "...",
      "topic": "일상",
      "startMessage": "새로운 메시지가 도착했습니다.",
      "deeplink": "dorandoran://chatroom/create?chatbotId=...&topic=...&concept=coworker",
      "sentAt": "2026-03-03T09:00:00+09:00"
    }
  }
}
```

---

## 5. 구현 측면

- `PushNotificationService`에서 `Message.builder()` 사용 방식 변경은 거의 없음  
- `putData("concept", ...)`, `putData("chatbotId", ...)` 추가로 쉽게 반영 가능  
- 제안하신 최소 구조(`concept`, `chatbotId`)는 그대로 사용 가능하고, 필요한 필드만 `data`에 추가하면 됨

---

## 6. 요약

| 항목 | 내용 |
|------|------|
| 변경 가능 여부 | 가능 (FCM 스펙 준수) |
| 기존 notification | `title`, `body` 유지 |
| 제안 data | `concept`, `chatbotId` 그대로 사용 가능 |
| 추가 권장 data | `topic`, `startMessage`, `deeplink`, `sentAt` 등 (기능 요구에 따라) |

---

## 7. 적용된 변경 사항 (2026-03-03)

| 메서드 | notification.title | notification.body | data 필드 순서 |
|--------|-------------------|-------------------|----------------|
| `sendToUser` | 요청값 또는 `"도란도란"` | 요청값 또는 `"새로운 메시지가 도착했습니다."` | deeplink, universalLink, chatroomId, messageId, startMessage, sentAt |
| `sendByTopic` | `"도란도란"` (고정) | greeting 문구 | **concept**, **chatbotId**, topic, startMessage, deeplink, sentAt |
| `sendChatroomCreatePush` | 요청값 또는 `"도란도란"` | 요청값 또는 기본 문구 | **concept**, **chatbotId**, topic, **startMessage**, deeplink, sentAt |
