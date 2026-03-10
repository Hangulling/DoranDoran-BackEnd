# 채팅방 생성 500 오류 로그 재분석

**상태**: 이전에 적용했던 프론트 방어 로직(userId/concept 검사, concept 정규화) 및 백엔드 RuntimeException→404 매핑은 **일단 revert** 했음.  
**2026-02-06 서버 로그 직접 확인**으로 원인 확정함.

---

## 0. 서버 로그로 확인한 500 원인 (결론)

최근 3시간 Chat 서비스 로그(`docker logs dorandoran-chat --since 3h`)에서 다음이 확인됨.

### (1) 15:03:55 — 사용자가 겪은 "채팅방 생성 실패" 500

| 항목 | 내용 |
|------|------|
| **예외** | `DataIntegrityViolationException` → `ConstraintViolationException` → `PSQLException` |
| **메시지** | `duplicate key value violates unique constraint "idx_chatrooms_user_chatbot"` |
| **Detail** | `Key (user_id, chatbot_id)=(3fa11b2f-0e4d-4332-aa76-308407ec68ca, 22222222-2222-2222-2222-222222222224) already exists.` |
| **흐름** | `ChatService.createRoom` → INSERT → 유니크 위반 → catch에서 **방어 로직 미동작** → `GlobalExceptionHandler`까지 전파 → **500** |

**원인**: 동일 (user_id, chatbot_id)로 이미 활성 방이 있는데 새 방 INSERT를 시도함.  
**방어 로직이 500을 막지 못한 이유**: 현재 **배포된 Chat 서비스**에는 `isUserChatbotDuplicateConstraint`에서 **예외 메시지에 `idx_chatrooms_user_chatbot` 포함 여부로 판별**하는 수정이 **반영되어 있지 않음**. (Hibernate가 `getConstraintName()`을 null로 주는 환경에서 기존 코드는 false → 예외 재throw → 500.)

**조치**: **코드에는 이미 반영됨** (`ChatService.isUserChatbotDuplicateConstraint` 예외 메시지에 `idx_chatrooms_user_chatbot` 포함 시 true + 활성 방 재조회 반환). **해당 빌드를 Chat 서비스에 배포**하면 동일 상황에서 500 대신 200으로 기존 방을 반환함.

### (2) 15:01:33 — Postman/테스트에서 발생한 500

| 항목 | 내용 |
|------|------|
| **예외** | `HttpMessageNotReadableException` (Jackson) |
| **메시지** | `Cannot deserialize value of type java.util.UUID from String "{{user_id}}"` |
| **원인** | 요청 body의 `userId`에 **문자열 `"{{user_id}}"`** 가 그대로 전달됨. (Environment 변수 미설정 또는 치환 실패) |

**조치**: Postman 등에서 채팅방 생성 시 **Environment에 `user_id`** 를 실제 UUID로 설정하고, body에는 `"userId": "{{user_id}}"` 처럼 사용.

### (3) 15:05:43, 15:15:21 — 동일 duplicate key 500

- 15:05:43: `(3fa11b2f-..., 222...221)` (friend 컨셉)  
- 15:15:21: `(9a569fcd-bdfa-42ca-aedb-63302264ca23, 222...221)` (다른 사용자)  

둘 다 **idx_chatrooms_user_chatbot** 위반 → 위 (1)과 같은 원인·같은 배포 반영으로 해소 가능.

---

## 1. 사용자 제공 로그 (프론트)

```
[GA] Production GA Initialized
✅ GOOGLE_CLIENT_ID from env: ...
No token found, redirecting to login.
비활성 타이머 시작
비활성 타이머 시작
OnboardingPage-CCtJIS6P.js:1 온보딩 완료 처리 성공
api.doran-chat.com/api/chat/chatrooms:1  Failed to load resource: the server responded with a status of 500 ()
ClosenessPage-BXXl9O4G.js:1 채팅방 생성 실패: AxiosError ... status: 500
```

### 로그 순서 해석

1. **No token found, redirecting to login**  
   앱 진입 시 토큰 없음 → 로그인 페이지로 리다이렉트.
2. **비활성 타이머 시작** (2회)  
   로그인/메인 등에서 비활성 타이머 구독.
3. **온보딩 완료 처리 성공**  
   `OnboardingPage`에서 `updateOnboarding(userId, true)` 성공 후 `navigate('/', { replace: true })` → 메인(/)으로 이동.
4. **POST /api/chat/chatrooms → 500**  
   메인에서 챗방 하나 클릭 → `/closeness/:id` 진입 → Confirm 클릭 → `createRoom` 호출 → 서버가 500 반환.
5. **채팅방 생성 실패**  
   `ClosenessPage`의 `onError`에서 "채팅방 생성 실패" 로그.

즉, **온보딩 직후 메인 → 챗방 선택 → Closeness Confirm → 채팅방 생성 API 500** 흐름이다.

---

## 2. 500이 날 수 있는 백엔드 원인

`ChatController.createRoom` → `ChatService.createRoom` 경로에서 500으로 이어지는 경우:

| 원인 | 위치 | 결과 |
|------|------|------|
| **User not found** | `userRepository.findById(userId)` empty | `RuntimeException("User not found: " + userId)` → **GlobalExceptionHandler** `Exception` 처리 → **500** |
| **Chatbot not found** | `chatbotRepository.findById(chatbotId)` empty | `RuntimeException("Chatbot not found: " + chatbotId)` → 동일 → **500** |
| **idx_chatrooms_user_chatbot 중복** | `save(room)` 후 동시 요청 등으로 유니크 위반 | `DataIntegrityViolationException` → `isUserChatbotDuplicateConstraint`가 false면 재throw → **500** |
| **softDeleteRoom 실패** | 기존 방 소프트 삭제 시 권한/상태 오류 | `RuntimeException("Access denied or room already deleted")` → **500** |
| **요청 바인딩 실패** | `userId`/`chatbotId` null·빈문자열 등 | `@Valid` 또는 UUID 역직렬화 실패 → 대부분 400, 예외 처리에 따라 500 가능 |

프론트 로그만으로는 **어느 케이스인지 구분 불가**하므로, **서버 로그의 예외 메시지·스택** 확인이 필요하다.

---

## 3. “온보딩 직후”에서 유력한 원인

### 3.1 User not found (채팅 DB에 사용자 없음)

- 채팅 서비스가 **user_schema.app_user**를 참조하고, 사용자 생성은 **auth/user 서비스**에서만 되는 구조라면,  
  **방금 가입·온보딩 완료한 사용자**가 아직 채팅 DB(또는 동기화)에 없을 수 있음.
- 이 경우 `userRepository.findById(userId)`가 empty → `RuntimeException("User not found: ...")` → **500**.

**확인 방법**: 해당 요청 시각의 서버 로그에서 `"User not found"` 검색.

### 3.2 concept/chatbotId 비어 있음

- ClosenessPage는 `concept = location.state?.concept || ''`.
- **페이지 새로고침·직접 URL**이면 `location.state`가 비어 `concept === ''`.
- `getChatBotIdByConcept('')` → `''` → 요청 body에 `chatbotId: ""` 전달.
- 백엔드 `ChatRoomCreateRequest`는 `@NotNull UUID chatbotId` → 빈 문자열 역직렬화 실패 시 400 또는 예외로 500 가능.

**확인 방법**: 서버 로그에 UUID 파싱/validation 관련 예외가 있는지 확인. 프론트에서 concept 미전달 시 chatbotId가 빈 문자열로 갈 수 있음.

### 3.3 duplicate key (idx_chatrooms_user_chatbot)

- 동일 (user_id, chatbot_id)로 두 요청이 겹치면 한쪽이 유니크 위반.
- 이전에 `isUserChatbotDuplicateConstraint`가 `getConstraintName() == null` 때문에 false를 반환해 500이 났고, **예외 메시지 포함 여부**로 판별하도록 수정했음.
- **배포 버전에 해당 수정이 반영되지 않았으면** 동일 500이 재발할 수 있음.

**확인 방법**: 서버 로그에서 `"duplicate key value violates unique constraint \"idx_chatrooms_user_chatbot\""` 및 `"createRoom duplicate detected"` 여부 확인.

---

## 4. 서버 에러 로그 확인 방법

Chat 서비스는 **stdout**으로 로그를 남기며, Docker로 띄운 경우 **컨테이너 로그**에서 확인한다.

### 4.1 서버 접속 후 Chat 컨테이너 로그 보기

```bash
# 컨테이너 이름 확인 (chat 관련)
docker ps | grep -i chat

# 최근 1시간 로그에서 채팅방 생성·예외만 필터 (컨테이너 이름은 실제에 맞게)
docker logs dorandoran-chat --since 1h 2>&1 | grep -E "chatrooms|User not found|Chatbot not found|duplicate key|idx_chatrooms_user_chatbot|DataIntegrityViolation|ConstraintViolation|RuntimeException|createRoom duplicate"
```

- **prod/docker** 프로파일에서는 JSON 로그가 stdout에 출력된다.  
- 로그에 **한 줄에 한 이벤트**가 들어 있으므로, 위 `grep`으로 예외가 포함된 줄을 찾은 뒤 그 **앞뒤 타임스탬프**로 같은 시각대의 다른 로그를 맞추면 된다.

### 4.2 500 원인별로 검색할 키워드

| 검색 키워드 | 나오면 의심 원인 |
|-------------|------------------|
| `User not found` | 채팅 DB(user_schema.app_user)에 해당 userId 없음 |
| `Chatbot not found` | chatbotId가 DB에 없거나 잘못 전달됨 |
| `duplicate key` / `idx_chatrooms_user_chatbot` | 동일 (user_id, chatbot_id)로 중복 INSERT 시도 |
| `createRoom duplicate detected` | 동일 위반이지만 **방어 로직이 동작**한 경우(WARN 로그) → 500이 아님 |
| `DataIntegrityViolationException` | DB 제약 위반 (위 duplicate 포함) |
| `Access denied or room already deleted` | softDeleteRoom 등에서 권한/상태 오류 |

### 4.3 원인 파악 절차

1. **프론트 로그에서 시각·흐름 확인**  
   - 사용자가 “채팅방 생성 실패”를 본 **대략 시각**(브라우저/앱 로그 또는 사용자 진술).
2. **해당 시각 전후로 Chat 서비스 로그 검색**  
   - `docker logs dorandoran-chat --since 2h 2>&1` 등을 저장한 뒤,  
   - 그 시각대(예: 15:03:50 ~ 15:04:00)에서 `POST /api/chat/chatrooms` 또는 `createRoom` 관련 로그와 **예외 스택**을 찾는다.
3. **예외 메시지로 원인 특정**  
   - 위 4.2 표에 따라 메시지가 포함된 줄이 있으면 해당 원인으로 간주.  
   - `createRoom duplicate detected`만 있고 500이 났다면, 방어 로직은 동작했지만 그 이후 단계에서 실패한 것이므로 스택 전체 확인.
4. **이전 분석과의 대조**  
   - `docs/maintenance/CHAT_API_LOG_ANALYSIS_20260206.md` 에서는 **duplicate key**로 500이 났고, **"createRoom duplicate detected"** 로그는 한 건도 없어 **방어 로직이 타지 않은 것**으로 정리됨.  
   - 현재 배포에 `isUserChatbotDuplicateConstraint`의 **예외 메시지 기반 판별**이 포함돼 있는지 확인하면, duplicate 시 500 재발 여부를 판단하는 데 도움이 됨.

---

## 5. 조치 (원인 확정 후)

- **User not found**: 채팅 DB와 사용자 생성/동기화 경로 점검. 필요 시 RuntimeException → 404 매핑 재검토.
- **Chatbot not found**: concept → chatbotId 매핑 및 요청 body 검증.
- **duplicate key**: 배포에 duplicate 방어(예외 메시지로 idx_chatrooms_user_chatbot 인식) 반영 여부 확인 및 프론트 이중 클릭 방지 유지.
- **concept/chatbotId 비어 있음**: 프론트에서 userId·concept·chatbotId 검사 및 concept 정규화 재적용 검토.

---

## 6. 참고

- `docs/maintenance/CHATROOM_CREATE_500_ANALYSIS.md` — duplicate key 상세
- `docs/maintenance/CHATROOM_DUPLICATE_KEY_FRONTEND_BACKEND.md` — 프론트 호출 경로·이중 클릭 방지
- `docs/maintenance/CHAT_API_LOG_ANALYSIS_20260206.md` — 서버 로그 분석 요약(duplicate key, createRoom 500 등)
