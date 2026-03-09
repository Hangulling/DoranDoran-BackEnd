# 채팅방 진입 로직 — createRoom 호출 위치 점검 보고

- **목적**: 백엔드에서 createRoom이 한 흐름 안에서 두 번 연속 호출될 위험이 있는지 확인.
- **일시**: 2026-02-05

---

## 1. createRoom / createRoomWithTopic 호출 위치 (백엔드)

| 위치 | API | 호출 내용 | 호출 횟수/요청 |
|------|-----|-----------|----------------|
| **ChatController** | `POST /api/chat/chatrooms` | `chatService.createRoom(userId, chatbotId, name, concept, intimacyLevel, testModel)` | **1회** |
| **DeeplinkController** (chat) | `GET /api/deeplink/chatroom/create` | `chatService.createRoomWithTopic(uid, chatbotId, "새 대화", conceptStr, level, topicVal)` → 내부에서 `createRoom` **1회** | **1회** |

- **createRoomWithTopic** 은 내부에서 `createRoom` 한 번만 호출 (ChatService 183~185행).
- 위 두 경로 모두 **요청 1건당 createRoom(또는 그에 준하는 생성) 1회**만 수행.

---

## 2. 채팅방 “생성/진입” 관련 다른 API (createRoom 미호출)

| API | 설명 | 비고 |
|-----|------|------|
| **POST /api/chat/rooms** | `createOrGetRoom` → `chatService.getOrCreateRoom(...)` | **있으면 반환, 없으면 생성.** createRoom이 아닌 getOrCreateRoom 사용. |
| **GET /api/chat/chatrooms** | 목록 조회 | 생성 없음. |
| **GET /api/chat/chatrooms/{id}** | 단건 조회 | 생성 없음. |

- user 서비스의 **DeeplinkService / PushNotificationService** 는 `dorandoran://chatroom/create?...` 같은 **딥링크 URL만 생성**하며, **chat 서비스의 createRoom API를 서버에서 호출하지 않음.** (푸시 클릭 시 **앱이** GET `/api/deeplink/chatroom/create` 호출.)

---

## 3. 결론: 백엔드에서의 “두 번 연속 createRoom” 위험

- **한 요청 처리 흐름 안에서 createRoom이 두 번 호출되는 코드 경로는 없음.**
- createRoom을 쓰는 진입점은 다음 두 가지뿐이며, 각각 요청당 1회만 호출:
  - `POST /api/chat/chatrooms` → `createRoom` 1회
  - `GET /api/deeplink/chatroom/create` → `createRoomWithTopic` 1회 (내부 `createRoom` 1회)

따라서 **동일 (user_id, chatbot_id)에 대해 createRoom이 두 번 실행되는 경우는, 서로 다른 두 번의 HTTP 요청**이 원인일 수밖에 없음. (예: 클라이언트 더블 탭, 재시도, 또는 일반 진입과 딥링크 진입이 짧은 간격으로 둘 다 호출되는 경우.)

이미 **createRoom 내부에서 23505(idx_chatrooms_user_chatbot) 발생 시 활성 방 재조회 후 반환**하는 방어 로직을 넣었으므로, 위와 같은 “두 번의 요청”으로 인한 중복 생성/500은 서버에서 흡수 가능.

---

## 4. 참고: 채팅방 진입 시나리오별 진입 API

| 시나리오 | 예상 호출 API | createRoom 사용 여부 |
|----------|----------------|----------------------|
| 앱에서 챗봇 선택 후 “채팅 시작” | `POST /api/chat/chatrooms` | ✅ createRoom 1회 |
| 푸시/딥링크로 “주제 기반 새 대화” | `GET /api/deeplink/chatroom/create?...` | ✅ createRoomWithTopic → createRoom 1회 |
| (레거시) “방 있으면 들어가고 없으면 생성” | `POST /api/chat/rooms` | ❌ getOrCreateRoom (createRoom 아님) |

동일 사용자가 **같은 챗봇**에 대해 위 두 시나리오(일반 진입 + 딥링크 진입)를 거의 동시에 타면, **서로 다른 2회의 HTTP 요청**으로 createRoom이 두 번 호출될 수는 있으나, 백엔드 단일 요청 처리 경로 상의 “연속 2회 호출”은 없음.
