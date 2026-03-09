# 채팅방 생성 500 오류 상세 분석 (오류 ID 중심)

## 1. 오류 요약

| 항목 | 값 |
|------|-----|
| **발생 시각** | 2026-02-06 15:03:55 |
| **API** | `POST /api/chat/chatrooms` |
| **에러** | `DataIntegrityViolationException` → 500 |
| **DB 제약** | `idx_chatrooms_user_chatbot` (user_id, chatbot_id 부분 유니크, WHERE NOT is_deleted) |
| **Detail** | `Key (user_id, chatbot_id)=(3fa11b2f-0e4d-4332-aa76-308407ec68ca, 22222222-2222-2222-2222-222222222224) already exists.` |

### 오류 난 ID

- **user_id**: `3fa11b2f-0e4d-4332-aa76-308407ec68ca`
- **chatbot_id**: `22222222-2222-2222-2222-222222222224`  
  - `ChatService.getChatbotIdByConcept` 기준 **SENIOR** 컨셉 챗봇

---

## 2. 설계 로직: "기존 방 있어도 새 방 생성"

- **의도**: 채팅방 들어갈 때마다 **새 대화(새 방)** 을 만든다.
- **구현** (`ChatService.createRoom`):
  1. 같은 (user_id, chatbot_id)의 **활성 방**이 있으면 → **소프트 삭제** (`softDeleteRoom`)
  2. 새 `ChatRoom` 엔티티 생성 후 `chatRoomRepository.save(room)` (INSERT)
  3. INSERT 시 **동시성 등으로** `idx_chatrooms_user_chatbot` 중복이 나면 →  
     `DataIntegrityViolationException` catch → `isUserChatbotDuplicateConstraint(ex)` 가 **true**이면  
     **같은 (userId, chatbotId)의 활성 방을 다시 조회해 그 방을 반환** (500 대신 200)

즉, "기존 방 있으면 소프트 삭제 후 새 방 생성"이 맞고, **중복 키는 동시 요청/레이스 시 발생할 수 있는 상황**으로 보고, 그때는 **이미 만들어진 새 방을 돌려주는 방어 로직**이 코드에 이미 있음.

---

## 3. 왜 이번에 500이 났는지 (원인)

### 3.1 DB/제약

- **제약**: `CREATE UNIQUE INDEX idx_chatrooms_user_chatbot ON chat_schema.chatrooms (user_id, chatbot_id) WHERE (NOT is_deleted)`  
  (`docs/schema_dump_server.sql`, `CHATROOM_401_AND_DUPLICATE_FIX_PLAN.md` 참고)
- **의미**: (user_id, chatbot_id) 조합당 **is_deleted = false 인 행은 1개만** 허용.
- **오류 메시지**: 해당 (user_id, chatbot_id)로 **이미 is_deleted = false 인 행이 있다**는 뜻 → INSERT 한 번 더 시도했다는 뜻.

### 3.2 가능한 시나리오 (오류 ID 기준)

1. **동시 요청(레이스)**  
   - 동일 (3fa11b2f-..., 222...224)에 대해 createRoom이 **거의 동시에 2번** 호출.
   - 둘 다 `findByUser_IdAndChatbot_IdAndIsDeletedFalse` 로 기존 방을 찾고, 둘 다 `softDeleteRoom` 호출 (또는 한쪽만 성공).
   - 그 다음 **둘 다** 새 방 INSERT 시도 → 한 건만 성공, 나머지 한 건이 **idx_chatrooms_user_chatbot** 위반.
   - 이때 **catch 블록**에서 위반을 "user_chatbot 중복"으로 인식하면 → 활성 방 재조회 후 반환 → 500이 나면 안 됨.

2. **catch가 "user_chatbot 중복"을 인식하지 못함**  
   - 실제 원인: **Hibernate + PostgreSQL** 조합에서 `ConstraintViolationException.getConstraintName()` 이 **null**을 반환하는 경우가 있음.  
   - `isUserChatbotDuplicateConstraint(ex)` 는 **constraint 이름이 정확히 `"idx_chatrooms_user_chatbot"`** 일 때만 true를 반환.
   - `getConstraintName()` 이 null 이면 → 조건 불일치 → **false** → 예외를 그대로 다시 던짐 → **500**.

정리하면:

- **직접 원인**: INSERT 시 `idx_chatrooms_user_chatbot` 위반이 발생함 (동시 요청 또는 softDelete 반영 타이밍 이슈).
- **500이 그대로 나간 이유**: 위반 예외를 잡았지만, **제약 이름을 인식하지 못해** (`getConstraintName() == null`) 방어 로직(활성 방 재조회 후 반환)이 실행되지 않고 예외가 재전파됨.

---

## 4. 코드 위치 정리

| 구분 | 파일·메서드 | 내용 |
|------|-------------|------|
| createRoom | `chat/.../ChatService.java` createRoom (약 127–175행) | 기존 활성 방 softDelete 후 새 방 save, DataIntegrityViolationException catch 후 isUserChatbotDuplicateConstraint 로 분기 |
| softDeleteRoom | `ChatService.java` (약 507–516행) | `room.setIsDeleted(true)` 후 save |
| 제약 판별 | `ChatService.java` isUserChatbotDuplicateConstraint (약 619–633행) | cause chain 에서 `ConstraintViolationException` 의 **getConstraintName()** 이 `"idx_chatrooms_user_chatbot"` 인지만 확인 → **null 이면 false** |
| DB 제약 | `docs/schema_dump_server.sql` 등 | `idx_chatrooms_user_chatbot` (user_id, chatbot_id, WHERE NOT is_deleted) |

---

## 5. 결론 및 조치

- **로직**: "기존 방 있으면 소프트 삭제 후 새 방 생성"은 의도대로 구현되어 있음.
- **오류 ID**: (user_id=3fa11b2f-0e4d-4332-aa76-308407ec68ca, chatbot_id=222...224) 에서 **동일 조합으로 중복 INSERT가 시도**되었고, 그때 **방어 로직이 동작하지 않아** 500이 발생한 것으로 판단됨.
- **방어 로직이 동작하지 않은 이유**: PostgreSQL 에서 Hibernate 가 제약 이름을 넘기지 않아 `getConstraintName()` 이 null 이고, 이 때문에 `isUserChatbotDuplicateConstraint` 가 false 를 반환함.

**권장 조치**  
- `isUserChatbotDuplicateConstraint` 를 보강하여, **constraint 이름이 null 이어도**  
  - 예외 메시지에 `"idx_chatrooms_user_chatbot"` 이 포함되어 있거나  
  - (선택) PostgreSQL unique 위반 SQL state `23505` 인 경우  
  **user_chatbot 중복**으로 간주하고 true 를 반환하도록 수정.  
- 이렇게 하면 동일 (user_id, chatbot_id) 에서 중복 INSERT 가 나와도 catch 블록에서 활성 방을 조회해 반환하므로, 해당 ID 조합에서 500이 다시 나지 않도록 할 수 있음.

---

## 6. 참고

- **프론트·백 연계 원인 점검**: `docs/maintenance/CHATROOM_DUPLICATE_KEY_FRONTEND_BACKEND.md` (duplicate key가 계속 나올 수 있는 프론트 호출 경로 및 이중 클릭 방지 조치)
- `docs/maintenance/CHATROOM_401_AND_DUPLICATE_FIX_PLAN.md`
- `docs/maintenance/CHAT_API_LOG_ANALYSIS_20260206.md`
- `docs/schema_dump_server.sql` (idx_chatrooms_user_chatbot)
