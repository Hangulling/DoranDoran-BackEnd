# 푸시·딥링크 API 정합성 검증 결과 (igu 트리)

> igu 트리로 옮겨진 푸시·딥링크 기능이 [API_SPEC_PUSH_AND_DEEPLINK.md](./API_SPEC_PUSH_AND_DEEPLINK.md) 및 Gateway 라우팅과 일치하는지 검증한 결과입니다.

---

## 1. 알림(푸시) API (User 서비스)

| 항목 | 명세 (API_SPEC_PUSH_AND_DEEPLINK) | igu 구현 | 일치 |
|------|-----------------------------------|----------|------|
| **2.1 FCM 토큰 등록** | `POST /api/notifications/register` | `NotificationController.registerFcmToken()` | ✅ |
| Body | token, platform | `FcmTokenRequest` (record) | ✅ |
| **2.2 푸시 발송** | `POST /api/notifications/send` | `NotificationController.sendPushNotification()` → `sendToUser()` | ✅ |
| Body | userId, title, body, chatroomId, messageId | `PushNotificationRequest` (record) | ✅ |
| FCM data | chatroomId, messageId, sentAt(KST) | sendToUser 시 deeplink, universalLink, chatroomId, messageId, startMessage, sentAt(KST) | ✅ |
| **2.3 푸시 로그 조회** | `GET /api/notifications/logs?userId=&page=0&size=20` | `PushDeliveryLogController` GET (매핑: `/api/notifications/logs`) | ✅ |
| 쿼리 | userId(필수), page, size | userId(required=false), page(0), size(20) | ⚠️ |
| 응답 | content + 페이지 메타 | `ApiResponse<PushDeliveryLogPageResponse>` (content, page.number/size/totalPages/totalElements) | ✅ |
| **2.4 send-by-topic** | `POST /api/notifications/send-by-topic` | `NotificationController.sendByTopic()` | ✅ |
| Body | userId, chatbotId, topic, concept, intimacyLevel | `SendByTopicRequest` (class) | ✅ |
| 동작 | Agent title/body + deeplink data | `PushNotificationService.sendByTopic()` + `PushNotificationAgent` | ✅ |
| FCM data | deeplink, chatbotId, topic, concept, intimacyLevel, sentAt(KST) | 동일 필드 포함 | ✅ |

**⚠️ 2.3 로그 조회**: 명세는 `userId` 필수. igu는 `userId`를 `required = false`로 두어 관리자 전체 조회를 허용할 수 있음. 클라이언트만 사용할 경우 쿼리에서 userId 항상 전달하면 명세와 동일하게 사용 가능.

---

## 2. 딥링크 API

### 2.1 GET /api/deeplink/chatroom/create (Chat 서비스)

| 항목 | 명세 | igu 구현 | 일치 |
|------|------|----------|------|
| 경로 | `GET /api/deeplink/chatroom/create` | `DeeplinkController(Chat).createChatroomFromDeeplink()` | ✅ |
| 쿼리 | chatbotId(필수), topic(선택), concept, intimacyLevel | chatbotId(필수), topic(선택), concept, intimacyLevel | ✅ |
| 사용자 | X-User-Id (Gateway 주입) | SecurityContext 또는 쿼리 userId | ✅ |
| 응답 | ChatRoomResponse | `ChatRoomResponse` (toChatRoomResponse) | ✅ |
| topic 없을 때 | 주제 없이 채팅방 생성/조회 | topic=null 로 `getOrCreateRoomWithTopic` 호출 | ✅ |

### 2.2 GET /api/deeplink/route (User 서비스)

| 항목 | 명세 | igu 구현 | 일치 |
|------|------|----------|------|
| 경로 | `GET /api/deeplink/route?path=...` | `DeeplinkController(User).route()` | ✅ |
| 쿼리 | path (필수) | @RequestParam String path | ✅ |
| 헤더 | X-User-Id (필수) | @RequestHeader("X-User-Id") UUID userId | ✅ |
| 응답 | { screen, params } | `DeeplinkRouteResponse` (screen, params) | ✅ |
| 파싱 로직 | chatroom, archive, chatroomCreate 등 | `DeeplinkService.parsePath()` | ✅ |

---

## 3. Gateway 라우팅

| 경로 | 명세 대상 | igu application.yml / application-docker.yml | 일치 |
|------|-----------|----------------------------------------------|------|
| `/api/notifications/**` | User | user-service-notifications → dorandoran-user:8082 | ✅ |
| `/api/deeplink/route` | User | deeplink-route → dorandoran-user:8082 | ✅ |
| `/api/deeplink/chatroom/**` | Chat | deeplink-chatroom → dorandoran-chat:8083 | ✅ |

**라우팅 순서**: `Path=/api/deeplink/route`가 `Path=/api/deeplink/chatroom/**`보다 먼저 정의되어 있어 `/api/deeplink/route`만 User로, 나머지 딥링크는 Chat으로 정확히 분기됨.

---

## 4. DTO·응답 형식

| API | 요청 | 응답 (igu) | 비고 |
|-----|------|------------|------|
| POST /api/notifications/register | FcmTokenRequest (record) | ApiResponse&lt;Void&gt; | ✅ |
| POST /api/notifications/send | PushNotificationRequest (record) | ApiResponse&lt;Void&gt; | ✅ |
| GET /api/notifications/logs | userId, page, size | ApiResponse&lt;PushDeliveryLogPageResponse&gt; (content, page) | ✅ |
| POST /api/notifications/send-by-topic | SendByTopicRequest (class) | ApiResponse&lt;Void&gt; + message | ✅ |
| GET /api/deeplink/chatroom/create | 쿼리 파라미터 | ChatRoomResponse (raw) | ✅ |
| GET /api/deeplink/route | path, X-User-Id | DeeplinkRouteResponse (raw) | ✅ |

---

## 5. 수정 반영 사항 (검증 중 적용)

- **Chat DeeplinkController**: 명세 3.1에 맞춰 `topic`을 **선택(optional)** 으로 변경. `@RequestParam(required = false) String topic`, topic 없을 때 `getOrCreateRoomWithTopic(..., topicVal=null)` 호출하도록 수정함.

---

## 6. 요약

- **푸시**: register, send, logs, send-by-topic 경로·Body·FCM data(sentAt KST) 모두 명세와 일치. 로그 조회만 userId를 igu에서 선택 파라미터로 두었음.
- **딥링크**: chatroom/create(chatbotId, topic 선택, concept, intimacyLevel), route(path, X-User-Id) 및 응답 형식 일치. Chat의 topic을 선택으로 맞춤.
- **Gateway**: `/api/deeplink/route` → User, `/api/deeplink/chatroom/**` → Chat 분기 정상.

이 문서는 igu 트리 검증 시점 기준이며, 명세 또는 구현 변경 시 재검증을 권장합니다.
