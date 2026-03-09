# 푸시 클릭 → 채팅방 진입 구현 가이드

> 푸시 알림 클릭 시 FCM data를 기반으로 채팅방에 진입하고, 필요 시 startMessage로 대화를 시작하는 클라이언트 동작 정리

## 1. 푸시 클릭 시 추출할 FCM data

FCM **data** 페이로드에서 다음 필드를 사용한다.

| 필드 | 설명 |
|------|------|
| `chatroomId` | 이동할 채팅방 ID (있으면 채팅방 진입 플로우) |
| `messageId` | (선택) 특정 메시지로 스크롤 시 사용 |
| `startMessage` | (선택) 비어 있지 않으면 진입 후 이 내용으로 사용자 메시지 1회 전송 |
| `deeplink` / `universalLink` | 앱/웹 딥링크 URL |
| `sentAt` | 발송 시각 (예: 2026-01-19T12:00:00+09:00) |

명세: [API_SPECIFICATION_V3.md](./API_SPECIFICATION_V3.md) §5.4.4

## 2. 채팅방 진입 시 사용하는 API

**채팅방 진입**은 `chatroomId` 기준으로 라우팅한 뒤, **Chat 서비스 API만** 사용한다.

- **채팅방 정보**: `GET /api/chat/chatrooms/{chatroomId}`
- **메시지 목록**: `GET /api/chat/chatrooms/{chatroomId}/messages?page=0&size=50`
- **(선택) 특정 메시지**: `GET /api/chat/messages/{messageId}`

Gateway 경유 시 인증(예: JWT → X-User-Id) 및 공통 에러 코드(C001, C002 등)는 API_SPECIFICATION_V3.md를 참고한다.

## 3. startMessage 처리

`startMessage`가 **비어 있지 않으면**:

1. 위 API로 채팅방 진입 및 메시지 목록 표시
2. 진입 후 `POST /api/chat/chatrooms/{chatroomId}/messages` 를 한 번 호출
3. 요청 body의 `content`에 `startMessage` 값을 넣어 전송
4. 이를 통해 "푸시로 챗봇에게 한 마디 보내서 대화 시작"을 구현

## 4. 요약 흐름

```
[푸시 클릭]
    → FCM data에서 chatroomId, messageId, startMessage, deeplink 추출
    → chatroomId 있음: GET /api/chat/chatrooms/{chatroomId}
    → GET /api/chat/chatrooms/{chatroomId}/messages?page=0&size=50
    → (선택) messageId 있으면 해당 메시지로 스크롤
    → startMessage 있으면 POST /api/chat/chatrooms/{chatroomId}/messages { content: startMessage }
    → 채팅 화면 표시
```

채팅방 생성(딥링크) 플로우는 [PUSH_AND_DEEPLINK_CLIENT_GUIDE.md](./PUSH_AND_DEEPLINK_CLIENT_GUIDE.md) 참고.
