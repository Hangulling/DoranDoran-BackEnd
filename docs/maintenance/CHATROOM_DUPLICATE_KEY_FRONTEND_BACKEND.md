# duplicate key (idx_chatrooms_user_chatbot) 원인 점검: 프론트 + 백 연계

## 1. 요약

- **현상**: `POST /api/chat/chatrooms` 호출 시 `duplicate key value violates unique constraint "idx_chatrooms_user_chatbot"` 로 500 발생.
- **원인**: 동일 (user_id, chatbot_id)로 **createRoom API가 두 번 이상 호출**되면, 백엔드에서 두 번째 INSERT 시 위 제약 위반이 발생함.
- **프론트에서 중복 호출이 나올 수 있는 지점**과 **백엔드 방어**를 함께 점검한 결과를 정리함.

---

## 2. 프론트: 채팅방 생성 API 호출 위치

### 2.1 호출 경로 (전체)

| 구분 | 파일 | 트리거 |
|------|------|--------|
| **일반** | `src/api/chats.ts` → `createChatRoom()` | `api.post(CHAT_ENDPOINTS.CREATE, data)` |
| **테스트** | `src/api/chats.ts` → `createTestChatRoom()` | 동일 엔드포인트 `POST /api/chat/chatrooms` |

실제 **UI에서 호출하는 곳**은 아래 두 곳뿐임.

| 페이지 | 파일 | 호출 시점 |
|--------|------|-----------|
| **ClosenessPage** | `src/pages/ClosenessPage.tsx` | 사용자가 **Confirm** 버튼 클릭 시 `handleConfirm()` → `createRoom(...)` |
| **TestClosenessPage** | `src/test/TestClosenessPage.tsx` | 동일하게 **Confirm** 클릭 시 `handleConfirm()` → `createRoom(...)` |

- **진입 흐름**: MainPage(채팅방 목록) → 챗봇 클릭 → `/closeness/:id` (ClosenessPage) → Confirm → `createRoom` → `/chat/:id` (ChatPage).
- **다른 경로**: 딥링크/푸시 등에서 **같은 엔드포인트를 직접 호출하는 프론트 코드는 없음**. (앱/웹 외부에서 POST를 보내는 경우는 별도.)

### 2.2 왜 같은 (user_id, chatbot_id)로 두 번 호출될 수 있나

1. **Confirm 버튼 이중 클릭**
   - `handleConfirm` 안에서 `if (!id || isPending) return` 과 버튼 `disabled={isPending}` 만으로는, **첫 클릭 후 리렌더 전에 두 번째 클릭**이 들어오면 두 번 모두 `createRoom` 이 호출될 수 있음.
   - `isPending` 은 뮤테이션 시작 후 비동기로 바뀌므로, **연타 시 동일 파라미터로 2회 POST** → 백엔드에서 두 번째 요청이 duplicate key 유발.

2. **동일 사용자가 여러 탭에서 동시에 Confirm**
   - 같은 계정으로 두 탭에서 `/closeness/:id` 를 열고 거의 동시에 Confirm → 동일 (user_id, chatbot_id)로 2회 POST 가능.

3. **네트워크 재시도**
   - 클라이언트/프록시의 재시도로 인해 동일 요청이 두 번 전달될 수 있음 (현재 프론트 코드에는 별도 재시도 로직 없음).

---

## 3. 백엔드: duplicate 발생 시 처리

- **설계**: “기존 방 있으면 소프트 삭제 후 새 방 생성”이므로, **동시에 두 요청이 와서** 한쪽이 먼저 INSERT 하고 다른 쪽이 INSERT 할 때 **idx_chatrooms_user_chatbot** 위반이 나는 것은 가능한 시나리오.
- **의도된 방어**: `ChatService.createRoom` 에서 `DataIntegrityViolationException` catch 후 `isUserChatbotDuplicateConstraint(ex)` 가 true 이면, **같은 (userId, chatbotId)의 활성 방을 다시 조회해 그 방을 반환**해 500 대신 200으로 응답.
- **과거 500 원인**: PostgreSQL + Hibernate 에서 `getConstraintName()` 이 null 로 오는 경우가 있어, 위 제약 위반을 인식하지 못하고 예외를 그대로 재전파했음.  
  → **조치**: 예외 메시지에 `"idx_chatrooms_user_chatbot"` 포함 여부로도 판별하도록 `isUserChatbotDuplicateConstraint` 보강 완료.

자세한 오류 ID·시나리오는 `docs/maintenance/CHATROOM_CREATE_500_ANALYSIS.md` 참고.

---

## 4. 적용한 조치 요약

### 4.1 백엔드 (이미 반영)

- `ChatService.isUserChatbotDuplicateConstraint`: constraint 이름이 null 이어도 **예외 메시지에 `idx_chatrooms_user_chatbot` 포함**이면 user_chatbot 중복으로 간주 → 활성 방 재조회 후 반환하여 500 방지.

### 4.2 프론트 (이중 클릭 방지)

- **ClosenessPage**  
  - `handleConfirm` 진입 시 `useRef` 로 **동기적으로** 제출 중 플래그 설정.  
  - `if (!id || isPending || submittingRef.current) return` 으로 두 번째 클릭 무시.  
  - `onSettled`(및 catch)에서 플래그 초기화.
- **TestClosenessPage**  
  - 동일하게 `submittingRef` + `onSettled` 로 이중 클릭 방지.

이렇게 해서 **같은 사용자·같은 페이지에서 Confirm 연타로 인한 중복 POST**를 줄이고, 혹시 중복이 나와도 **백엔드에서 duplicate key → 기존 활성 방 반환**으로 500이 나지 않도록 함.

---

## 5. 점검 체크리스트

- [x] 프론트에서 `POST /api/chat/chatrooms` 호출 위치: **ClosenessPage, TestClosenessPage 의 Confirm 버튼뿐**.
- [x] 중복 호출 가능 원인: **Confirm 이중 클릭**(및 다중 탭 등).
- [x] 백엔드: **idx_chatrooms_user_chatbot** 위반 시 예외 메시지로도 인식해 활성 방 반환하도록 보강.
- [x] 프론트: **Confirm 1회만 처리**되도록 `submittingRef` + `onSettled` 로 이중 클릭 방지.

---

## 6. 참고 문서

- `docs/maintenance/CHATROOM_CREATE_500_ANALYSIS.md` — 500 오류 ID·백엔드 원인 상세
- `docs/maintenance/CHATROOM_401_AND_DUPLICATE_FIX_PLAN.md` — 채팅방 중복/401 대응 계획
