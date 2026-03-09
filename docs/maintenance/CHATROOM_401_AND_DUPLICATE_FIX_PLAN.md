# 채팅 API 401 인증 오류 및 채팅방 중복 생성 오류 해결 계획 (앱 기준)

- **대상 이슈**
  - **1번**: `GET/HEAD https://api.doran-chat.com/api/chat/chatrooms` 호출 시 401 (Authorization 헤더 없음/잘못됨) → **Capacitor 포팅된 네이티브 앱 기준**으로만 조치.
  - **2-(2)번**: `POST /api/chat/chatrooms`(채팅방 생성) 시 `idx_chatrooms_user_chatbot` 중복 키 위반 → 서버 측 조치.
- **작성일**: 2026-02-05  
- **범위**: 1번은 **웹 조치 없음**, **앱( Capacitor 빌드) 전용** 플랜.

---

## 1번 문제: /api/chat/chatrooms 401 Unauthorized (앱 전용)

### 전제

- **웹(Vercel/브라우저)은 이번 계획에서 조치하지 않음.**
- **Capacitor로 포팅된 네이티브 앱**(iOS/Android)이 `https://api.doran-chat.com` 을 호출할 때만 대상.

### 원인 정리

- Gateway(`JwtAuthFilter`)에서 `/api/chat/chatrooms` 는 **인증 필요 경로**.
- 앱에서 요청 시 `Authorization: Bearer <JWT>` 가 없거나 형식이 잘못되면 **Gateway 단에서 401** 반환 (Chat 서비스까지 도달하지 않음).
- 로그: `"Authorization 헤더가 없거나 형식이 잘못됨: path=/api/chat/chatrooms"`

### 해결 방향 (앱만)

- **앱이 채팅 API 호출 시 항상 유효한 JWT를 헤더에 포함**하도록 보장.
- 앱에서 **401 수신 시** 토큰 갱신 또는 재로그인 플로우로 처리.

### 세부 계획 (앱 기준)

| 단계 | 작업 | 담당 | 비고 |
|------|------|------|------|
| 1.1 | **앱 API 클라이언트 일원화** | 앱( Capacitor 빌드) | 채팅 관련 모든 요청(목록 조회 `GET /api/chat/chatrooms`, 채팅방 생성 `POST /api/chat/chatrooms`, `last-interactions`, 메시지 목록/전송 등)이 **동일한 HTTP 클라이언트**를 사용하도록 정리. 해당 클라이언트에서 **요청 시 항상** `Authorization: Bearer <accessToken>` 를 붙이도록 구현. |
| 1.2 | **토큰 저장소 (앱)** | 앱 | 로그인 성공 시 받은 `accessToken` 을 앱에서 **영속 저장**. Capacitor 권장: `@capacitor/preferences` 또는 보안이 필요하면 플랫폼별 Secure Storage. **API 호출 직전**에 이 저장소에서 토큰을 읽어 헤더에 설정. (웹과 동일하게 `sessionStorage` 를 쓰는 경우, WebView/앱 생명주기에서 토큰이 비어 있지 않은지 확인.) |
| 1.3 | **토큰 없을 때 동작** | 앱 | 채팅 화면(또는 채팅 API가 필요한 화면) 진입 전에 **저장된 accessToken 존재 여부** 확인. 없으면 **로그인 화면으로 이동**하고, 채팅 API 호출을 하지 않음. |
| 1.4 | **401 수신 시 처리** | 앱 | 채팅 API 응답이 **401** 이면: (1) **토큰 갱신** (`/api/auth/refresh` 등) 시도 → 성공 시 새 accessToken 저장 후 실패한 요청 재시도. (2) 갱신 실패 또는 갱신 API 없으면 **로그인 만료**로 간주하고 **로그인 화면으로 이동** (및 저장된 토큰 삭제). 사용자에게 "다시 로그인해 주세요" 안내. |
| 1.5 | **앱 빌드 시 API Base URL** | 앱/빌드 설정 | Capacitor 앱이 **실서버**를 바라보도록 `https://api.doran-chat.com` 이 사용되는지 확인. (환경 변수 또는 앱 전용 설정 파일에서 API base URL 지정, 네이티브 빌드/Capacitor 설정에 반영.) |
| 1.6 | **문서화 (앱 개발용)** | `docs/` | “`/api/chat/*` 는 인증 필요. 앱에서 요청 시 `Authorization: Bearer <accessToken>` 필수. 401 시 갱신 또는 재로그인” 내용을 앱 개발/운영 문서에 명시. |

### 체크리스트 (1번, 앱만)

- [ ] 앱 내 채팅 관련 모든 API 호출이 **한 곳에서 헤더에 Bearer 토큰을 붙이는 클라이언트**를 사용하는지 확인
- [ ] 로그인 후 accessToken을 **앱 저장소**(Preferences/SecureStorage 등)에 저장하고, API 호출 시 해당 값 사용하는지 확인
- [ ] 채팅 진입 전(또는 최초 API 호출 전) **토큰 없으면 로그인 화면으로 이동**하는지 확인
- [ ] 401 수신 시 **토큰 갱신 → 실패 시 로그인 화면** 플로우가 동작하는지 확인
- [ ] 실기기/에뮬레이터에서 API base URL이 `https://api.doran-chat.com` 인지 확인
- [ ] 앱 개발 가이드/API 스펙에 “채팅 API 인증 필수 및 401 대응” 반영

---

## 2-(2)번 문제: 채팅방 생성 시 duplicate key (idx_chatrooms_user_chatbot)

*(웹/앱 구분 없음, 서버 측 조치만 해당.)*

### 서버 DDL 확인 결과 (기준: 운영 DB)

**확인 일시**: 2026-02-05. `chat_schema.chatrooms` 인덱스 조회 결과:

| indexname | indexdef |
|-----------|----------|
| chatrooms_pk | CREATE UNIQUE INDEX chatrooms_pk ON chat_schema.chatrooms USING btree (id) |
| idx_chatrooms_chatbot | CREATE INDEX idx_chatrooms_chatbot ON chat_schema.chatrooms USING btree (chatbot_id) |
| idx_chatrooms_last_message | CREATE INDEX idx_chatrooms_last_message ON chat_schema.chatrooms USING btree (last_message_at DESC) |
| idx_chatrooms_user | CREATE INDEX idx_chatrooms_user ON chat_schema.chatrooms USING btree (user_id) |
| **idx_chatrooms_user_chatbot** | **CREATE UNIQUE INDEX idx_chatrooms_user_chatbot ON chat_schema.chatrooms USING btree (user_id, chatbot_id) WHERE (NOT is_deleted)** |

**결론**: `idx_chatrooms_user_chatbot` 는 이미 **부분 유니크**(`WHERE (NOT is_deleted)`)가 걸려 있음. 전체 유니크가 아니므로 **DDL 변경(마이그레이션)은 불필요**함. (다른 환경에서 전체 유니크가 걸려 있다면, 그 환경에서만 아래 마이그레이션 예시 적용.)

### 원인 정리

- `ChatService.createRoom()`: 같은 `(user_id, chatbot_id)` 의 **삭제되지 않은** 채팅방이 있으면 **소프트 삭제** 후 **새 행 INSERT**.
- 인덱스가 **부분 유니크**(`WHERE (NOT is_deleted)`) 이므로, 정상 흐름에서는\n  - 기존 활성 방을 소프트 삭제(`is_deleted = true`) 한 뒤\n  - 새 방(`is_deleted = false`)을 INSERT 하면 **중복 없이** 성공해야 함.
- 실제 로그 상, 같은 `(user_id, chatbot_id)` 에 대해 **두 번의 duplicate key 오류(21:39, 22:54)** 가 발생했으며,\n  둘 다 `ChatController.createRoom → ChatService.createRoom → INSERT` 단계에서 500으로 떨어짐.
- 이는 동일 `(user, chatbot)` 조합에 대해 **거의 동시에 두 번 이상의 createRoom 요청이 들어와**,\n  첫 번째 요청이 새 방 INSERT를 성공시키고, 두 번째(또는 이후) 요청이 같은 `(user_id, chatbot_id, is_deleted = false)` 상태에서 INSERT 를 재시도하다가\n  **부분 유니크 제약에 걸려 실패한 패턴**으로 보는 것이 가장 합리적이다.

### 해결 방향

- **DB**: 서버 DDL 기준으로 이미 부분 유니크이므로 **추가 마이그레이션 없음**. 다른 DB(스테이징 등)에서 전체 유니크가 확인되면 그 환경에만 부분 유니크로 변경하는 마이그레이션 적용.
- **코드**: 제품 의도가 **“나갔다가 다시 들어오면 항상 새 대화”** 이므로,\n  - 기존 활성 방이 있으면 **소프트 삭제 후 새 방 INSERT** 하는 현행 흐름은 그대로 유지하되,\n  - INSERT 시 **중복 키(23505, `idx_chatrooms_user_chatbot`)가 발생하면, 이를 “이미 새 방이 하나 만들어진 상태”로 간주하고**\n    같은 `(userId, chatbotId)` 의 **현재 활성 방을 다시 조회해 그 방을 응답으로 돌려주는 방어 로직**을 추가한다.\n  - 이렇게 하면 동시/중복 요청이 들어와도 **실제 DB에는 새 방이 1개만 생성**되고,\n    나중에 들어온 요청들은 500 대신 그 **이미 생성된 새 방으로 진입**하게 된다.

### 세부 계획

| 단계 | 작업 | 담당 | 비고 |
|------|------|------|------|
| 2.1 | **운영 DB 제약 확인** | 완료 | 위 “서버 DDL 확인 결과” 참고. 부분 유니크 확인됨. |
| 2.2 | **마이그레이션** | `chat` 모듈 | **현재 운영 기준으로는 불필요.** 다른 환경에서 전체 유니크가 걸려 있을 때만 아래 “마이그레이션 예시” 적용. |
| 2.3 | **createRoom 중복 키 방어 로직** | `chat` 모듈 | `ChatService.createRoom`(또는 `ChatController.createRoom`) 에서 `DataIntegrityViolationException` / `ConstraintViolationException` 중 23505 + `idx_chatrooms_user_chatbot` 인 경우를 캐치. <br>1) 같은 `(userId, chatbotId)` 에 대한 **활성 채팅방**을 `findByUser_IdAndChatbot_IdAndIsDeletedFalse` 로 다시 조회하고, <br>2) 그 방이 있으면 **그 방을 그대로 반환**. <br>3) 없으면(이상 상태) 원래 예외를 그대로 다시 던지되, 로그를 자세히 남겨 추적 가능하게 함. |
| 2.4 | **테스트** | 단위/통합 | (1) 단일 요청 시: 기존 활성 방 소프트 삭제 후 새 방 INSERT 가 정상 동작하는지 확인. <br>(2) 동일 `(user, chatbot)` 에 대해 의도적으로 동시에 두 번 createRoom 을 호출했을 때, 실제 DB에는 새 방이 1개만 생기고, 두 요청 모두 200 으로 같은 방 정보를 받는지 검증. |

### 마이그레이션 예시 (다른 환경에서 전체 유니크일 때만)

```sql
-- chat_schema.chatrooms: (user_id, chatbot_id) 전체 유니크가 걸려 있는 환경에서만 실행.
-- 서버 DDL 확인 후 전체 유니크일 때 DROP 하고, 부분 유니크로 재생성.
DROP INDEX IF EXISTS chat_schema.idx_chatrooms_user_chatbot;

CREATE UNIQUE INDEX idx_chatrooms_user_chatbot
  ON chat_schema.chatrooms (user_id, chatbot_id)
  WHERE (NOT is_deleted);
```

(실제 서버에는 이미 동일한 부분 유니크가 있으므로 위 스크립트를 그대로 돌리면 DROP 후 같은 정의로 다시 생성하게 됨. **다른 DB에서만** 전체 유니크가 확인된 경우에 적용.)

### 체크리스트 (2-(2)번)

- [x] 운영 DB에서 chatrooms 테이블 인덱스·유니크 제약 목록 확인 → **부분 유니크 확인됨**
- [ ] (다른 환경만) 전체 유니크가 걸려 있으면 위 마이그레이션 예시 적용
- [ ] `createRoom` 에서 23505 (`idx_chatrooms_user_chatbot`) 발생 시 활성 방 재조회 후 반환하는 방어 로직 구현
- [ ] 단위/통합 테스트로 **동일 `(user, chatbot)` 에 대한 중복 createRoom** 시나리오를 포함해 검증

---

## 적용 순서 제안

1. **2-(2)번**: 서버 DDL 확인 완료. **이미 부분 유니크**이므로 추가 마이그레이션 없음. (다른 환경에서 전체 유니크가 있으면 해당 환경에만 부분 유니크 마이그레이션 적용.) `createRoom` 은 “항상 새 대화”에 맞게 **소프트 삭제 후 새 방 INSERT** 흐름을 유지하되, **중복 키(23505, `idx_chatrooms_user_chatbot`) 발생 시 활성 방 재조회 후 반환하는 방어 로직**을 추가해 500을 방지.
2. **1번**: **앱(Capacitor)** 에서 채팅 API 호출 시 Bearer 토큰 필수, 401 시 갱신/재로그인 처리 → 401 원인 제거. (웹은 이번 계획에서 변경하지 않음.)
