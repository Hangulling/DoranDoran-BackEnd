# 채팅방 생성 500 오류: Hibernate flush 순서로 인한 유니크 제약 위반

---

## 0) 역할/출력 요구사항

시니어 백엔드/풀스택 개발자 + 기술문서 작성자 관점으로, 채팅방 생성 API 500 오류의 원인(같은 트랜잭션 내 flush 순서)과 해결(soft-delete 직후 `flush()` 호출)을 정리한 기술 문서다.

---

## 1) 한 줄 요약 (문제/해결/결과)

**요약**: 채팅방 들어가기(POST /api/chat/chatrooms) 시 500이 발생하던 문제를, **동일 트랜잭션 내에서 soft-delete(UPDATE)가 INSERT보다 나중에 flush되며 부분 유니크 제약을 위반**한 것으로 규명하고, **soft-delete 직후 `chatRoomRepository.flush()`를 호출**해 UPDATE를 DB에 먼저 반영하도록 수정하여 해결했다.

---

## 2) 환경 정보

| 항목 | 내용 |
|------|------|
| **OS** | 서버: Amazon Linux 2 (EC2), 로컬: Windows 10 |
| **언어/런타임** | Java 21 (toolchain) |
| **프레임워크** | Spring Boot 3.3.4 |
| **서버/컨테이너** | Tomcat (embed), Docker |
| **DB / ORM** | PostgreSQL, Spring Data JPA / Hibernate |
| **빌드/배포** | Gradle 8.5, Docker 이미지 빌드 → tar → EC2 SCP → `docker run` |
| **인코딩/로케일** | UTF-8 |
| **기타** | Flyway 10.8.1, PostgreSQL JDBC 42.7.4 |

---

## 3) 문제 상황

### 3-1) 사용자/업무 관점

| 항목 | 내용 |
|------|------|
| **어떤 화면/기능** | 메인에서 챗봇(친구/연인 등) 선택 → Closeness 페이지에서 거리 설정 후 Confirm → 채팅방 생성 API 호출 |
| **기대 동작(정상)** | 200 OK, 새 채팅방 또는 기존 활성 채팅방 정보 반환 |
| **실제 동작(비정상)** | 500 Internal Server Error, 프론트에 "채팅방 생성 실패" 표시 |

### 3-2) 에러 메시지/로그 (원문)

**프론트(브라우저):**
```
POST https://api.doran-chat.com/api/chat/chatrooms 500 (Internal Server Error)
채팅방 생성 실패: AxiosError ... status: 500
```

**서버(Chat 서비스 Docker 로그, JSON 한 줄):**
```json
{"@timestamp":"2026-02-07T18:04:39.502451246+09:00","level":"INFO","message":"createRoom: saved successfully roomId=5ff057f2-00ee-467f-8f36-0ef3c0239684, userId=52dee622-0dd1-4a3c-8b2e-a6c7f700f515, chatbotId=22222222-2222-2222-2222-222222222221", ...}
{"@timestamp":"2026-02-07T18:04:39.510521889+09:00","level":"ERROR","message":"ERROR: duplicate key value violates unique constraint \"idx_chatrooms_user_chatbot\"\n  Detail: Key (user_id, chatbot_id)=(52dee622-0dd1-4a3c-8b2e-a6c7f700f515, 22222222-2222-2222-2222-222222222221) already exists.", "logger_name":"org.hibernate.engine.jdbc.spi.SqlExceptionHelper", ...}
{"@timestamp":"2026-02-07T18:04:39.511872299+09:00","level":"ERROR","message":"예상치 못한 오류 발생","logger_name":"com.dorandoran.chat.exception.GlobalExceptionHandler", ...}
```

동일 스레드(`http-nio-0.0.0.0-8083-exec-3`)에서 **"saved successfully" 직후** duplicate key → GlobalExceptionHandler가 잡아 500 응답.

### 3-3) 영향 범위

| 항목 | 내용 |
|------|------|
| **발생 빈도** | 기존 활성 채팅방이 있는 (user_id, chatbot_id)로 "채팅방 들어가기"를 할 때마다 재현 가능 |
| **영향 사용자/기능** | 해당 API를 쓰는 모든 사용자, 채팅방 진입 플로우 |
| **긴급도** | 높음 — 진입 자체가 실패 |

---

## 4) 관련 코드 (원문 그대로)

### 4-1) 문제 발생 지점: Controller → Service

**ChatController.createRoom**  
- **역할**: POST /api/chat/chatrooms 진입점. 인증/헤더에서 userId 추출, body 바인딩 후 `ChatService.createRoom` 호출, 성공 시 200 + `ChatRoomResponse` 반환.

```java
@PostMapping("/chatrooms")
public ResponseEntity<ChatRoomResponse> createRoom(
        @Valid @RequestBody ChatRoomCreateRequest request,
        @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
    UUID userId = extractUserIdFromSecurityContext();
    if (userId == null) userId = request.getUserId();
    if (userId == null && userIdHeader != null && !userIdHeader.isBlank()) {
        try { userId = UUID.fromString(userIdHeader); } catch (IllegalArgumentException ignored) {}
    }
    if (userId == null) return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();

    ChatRoom room = chatService.createRoom(
        userId,
        request.getChatbotId(),
        request.getName(),
        request.getConcept(),
        request.getIntimacyLevel(),
        request.getTestModel()
    );
    return ResponseEntity.ok(toChatRoomResponse(room));
}
```

**ChatService.createRoom (수정 전 — flush 없음)**  
- **역할**: 동일 (user_id, chatbot_id)의 활성 방이 있으면 소프트 삭제한 뒤 새 방 INSERT. 유니크 위반 시 기존 방 반환하는 방어 로직이 있으나, **예외가 이 메서드의 catch가 아닌 트랜잭션 커밋 단계에서 발생**해 500으로 이어짐.

```java
@Transactional
public ChatRoom createRoom(UUID userId, UUID chatbotId, String name, String concept, Integer intimacyLevel, String testModel) {
    log.info("createRoom start: userId={}, chatbotId={}, name={}, concept={}, intimacyLevel={}",
        userId, chatbotId, name, concept, intimacyLevel);

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException("User not found: " + userId));
    Chatbot chatbot = chatbotRepository.findById(chatbotId)
        .orElseThrow(() -> new RuntimeException("Chatbot not found: " + chatbotId));

    // 같은 사용자·챗봇 조합의 활성 채팅방이 있으면 소프트 삭제 후 새 방 생성
    chatRoomRepository.findByUser_IdAndChatbot_IdAndIsDeletedFalse(userId, chatbotId)
        .ifPresent(existing -> {
            log.info("createRoom: existing active room found, soft-deleting: roomId={}, userId={}, chatbotId={}",
                existing.getId(), userId, chatbotId);
            softDeleteRoom(existing.getId(), userId);
        });
    // ★ 수정 전에는 여기 아래에 flush() 없음

    UUID roomId;
    do {
        roomId = UUID.randomUUID();
    } while (chatRoomRepository.findById(roomId).isPresent());
    log.debug("createRoom: new roomId={}", roomId);

    ObjectNode settings = objectMapper.createObjectNode();
    settings.put("concept", concept != null ? concept : "FRIEND");
    if (testModel != null && !testModel.isBlank()) {
        settings.put("testModel", testModel);
    }
    int level = (intimacyLevel != null && intimacyLevel >= 1 && intimacyLevel <= 3) ? intimacyLevel : 2;

    ChatRoom room = ChatRoom.builder()
        .id(roomId)
        .user(user)
        .chatbot(chatbot)
        .name(name != null && !name.isBlank() ? name : "대화")
        .settings(settings)
        .isArchived(false)
        .isDeleted(false)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();

    try {
        ChatRoom savedRoom = chatRoomRepository.save(room);
        log.info("createRoom: saved successfully roomId={}, userId={}, chatbotId={}", savedRoom.getId(), userId, chatbotId);
        initializeIntimacyProgress(savedRoom.getId(), userId, level);
        return savedRoom;
    } catch (DataIntegrityViolationException ex) {
        String exMsg = ex.getMessage() != null ? ex.getMessage() : "";
        log.warn("createRoom: DataIntegrityViolationException caught: {}", exMsg.length() > 200 ? exMsg.substring(0, 200) + "..." : exMsg);
        boolean isDuplicate = isUserChatbotDuplicateConstraint(ex);
        log.info("createRoom: isUserChatbotDuplicateConstraint={}, action={}", isDuplicate, isDuplicate ? "return existing room" : "rethrow");
        if (isDuplicate) {
            log.warn("createRoom duplicate detected, returning existing active room: userId={}, chatbotId={}", userId, chatbotId);
            ChatRoom existingRoom = chatRoomRepository
                .findByUser_IdAndChatbot_IdAndIsDeletedFalse(userId, chatbotId)
                .orElseThrow(() -> ex);
            log.info("createRoom: returning existing roomId={}", existingRoom.getId());
            return existingRoom;
        }
        log.warn("createRoom: rethrowing (not idx_chatrooms_user_chatbot duplicate)");
        throw ex;
    }
}
```

### 4-2) 관련 의존 코드

**ChatService.softDeleteRoom**  
- **역할**: 지정한 채팅방을 소프트 삭제. `getChatRoomById`로 엔티티 로드 후 `isDeleted=true`, `updatedAt` 갱신, `chatRoomRepository.save(room)` 호출. **같은 트랜잭션 내에서 dirty 체크로 UPDATE만 스케줄링**하며, 이 시점에는 DB로 전송되지 않음.

```java
@Transactional
public void softDeleteRoom(UUID chatroomId, UUID userId) {
    ChatRoom room = getChatRoomById(chatroomId);
    if (!chatRoomRepository.existsByUserIdAndIdAndIsDeletedFalse(userId, chatroomId)) {
        throw new RuntimeException("Access denied or room already deleted: " + chatroomId);
    }
    room.setIsDeleted(true);
    room.setUpdatedAt(java.time.LocalDateTime.now());
    chatRoomRepository.save(room);
}
```

**ChatService.isUserChatbotDuplicateConstraint**  
- **역할**: `DataIntegrityViolationException`이 (user_id, chatbot_id) 부분 유니크 제약 위반인지 판별. `ConstraintViolationException.getConstraintName()` 또는 예외 메시지에 `idx_chatrooms_user_chatbot`(또는 동일 제약 이름) 포함 여부로 판단.  
- 이번 500에서는 **예외가 createRoom의 catch까지 전달되지 않고** 트랜잭션 커밋 시점에 발생해, 이 메서드는 호출되지 않음.

**ChatRoomRepository**  
- **역할**: `JpaRepository<ChatRoom, UUID>` 상속. `findByUser_IdAndChatbot_IdAndIsDeletedFalse`, `findById`, `save`, **`flush()`** 등 제공. `flush()`는 현재 영속성 컨텍스트의 변경을 DB에 즉시 반영.

**GlobalExceptionHandler.handleGenericException**  
- **역할**: `@ExceptionHandler(Exception.class)`. 처리되지 않은 예외를 받아 로그 "예상치 못한 오류 발생" 출력 후 500 + "서버 내부 오류가 발생했습니다." 반환.

```java
@ExceptionHandler(Exception.class)
public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {
    log.error("예상치 못한 오류 발생", ex);
    ErrorResponse error = ErrorResponse.builder()
        .timestamp(LocalDateTime.now())
        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
        .error("Internal Server Error")
        .message("서버 내부 오류가 발생했습니다.")
        .path(request.getDescription(false).replace("uri=", ""))
        .build();
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
}
```

### 4-3) DB 제약 (Flyway 마이그레이션)

**V2__chatroom_constraints.sql**  
- **역할**: (user_id, chatbot_id)에 대한 **부분 유니크 인덱스** — `is_deleted = false`인 행만 유일해야 함. 서버 로그의 제약 이름은 `idx_chatrooms_user_chatbot`

```sql
CREATE UNIQUE INDEX IF NOT EXISTS uq_chatrooms_user_bot_active
  ON chat_schema.chatrooms (user_id, chatbot_id)
  WHERE is_deleted = false;
```

---

## 5) 코드 흐름 (Call Flow / Data Flow)

- **요청 진입점**: `POST /api/chat/chatrooms` → `ChatController.createRoom` → `chatService.createRoom(...)`.
- **라우팅**: Spring MVC DispatcherServlet → `ChatController.createRoom` (body: `ChatRoomCreateRequest`).
- **주요 메서드 호출 순서**  
  1. `ChatService.createRoom`  
  2. `userRepository.findById`, `chatbotRepository.findById`  
  3. `chatRoomRepository.findByUser_IdAndChatbot_IdAndIsDeletedFalse` → 기존 방 있으면 `softDeleteRoom(existing.getId(), userId)`  
  4. `softDeleteRoom`: `getChatRoomById` → `room.setIsDeleted(true)`, `chatRoomRepository.save(room)` (UPDATE 스케줄링만, 아직 flush 없음)  
  5. `chatRoomRepository.save(room)` — 새 ChatRoom 엔티티 persist (INSERT 스케줄링)  
  6. `initializeIntimacyProgress(...)`  
  7. 메서드 정상 반환 → **트랜잭션 커밋** 시점에 Hibernate가 flush 실행.
- **데이터 흐름**: request body (userId, chatbotId, name, concept, intimacyLevel, testModel) → User/Chatbot 조회 → 기존 방 있으면 엔티티만 isDeleted=true로 변경 → 새 ChatRoom 빌드 후 save → 트랜잭션 커밋 시 INSERT/UPDATE가 DB로 전송.
- **예외 흐름**: **수정 전**에는 `DataIntegrityViolationException`이 **커밋 단계(flush)** 에서 발생해 컨트롤러/서비스 밖으로 나가므로, `createRoom`의 catch가 아닌 `GlobalExceptionHandler.handleGenericException`에서 처리 → 500.

---

## 6) 원인 가설 목록

**가설 1)**  
- **내용**: 동일 (user_id, chatbot_id)로 **다른 요청이 먼저 방을 만들어** 우리 INSERT가 유니크 위반.  
- **근거**: 에러 메시지가 "duplicate key ... already exists"이기 때문.  
- **확인 방법**: 서버 로그에서 같은 시각대에 동일 (user_id, chatbot_id)로 두 개의 createRoom 요청이 있는지, 스레드가 다른지 확인.  
- **결과**: 로그상 **같은 스레드**에서 "saved successfully" 직후 duplicate key가 나옴. 다른 요청과의 경합이 아니라 **같은 요청·같은 트랜잭션 내**에서 발생.

**가설 2)**  
- **내용**: `createRoom`의 catch에서 `DataIntegrityViolationException`을 잡지 못해 500이 난다.  
- **근거**: catch 블록에 "DataIntegrityViolationException caught" 로그가 **한 번도 없음**.  
- **확인 방법**: 서버 로그에서 "DataIntegrityViolationException caught" 또는 "isUserChatbotDuplicateConstraint=" 검색.  
- **결과**: 해당 로그 없음 → 예외가 **save(room) 직후가 아니라 그 이후(트랜잭션 커밋 시 flush)** 에 발생함을 확인.

**가설 3)**  
- **내용**: 같은 트랜잭션 안에서 **soft-delete UPDATE보다 새 방 INSERT가 먼저 flush**되어, INSERT 시점에 DB에는 아직 `is_deleted=false`인 기존 행이 남아 있어 유니크 위반.  
- **근거**: Hibernate는 flush 시 엔티티 타입/연관 관계 등에 따라 순서를 정하며, 새로 persist한 엔티티의 INSERT가 기존 엔티티의 UPDATE보다 먼저 나갈 수 있음.  
- **확인 방법**: 공식 문서 및 동일 시나리오에서 soft-delete 직후 `flush()`를 넣었을 때 500이 사라지는지 재현/비재현 테스트.  
- **결과**: **flush() 추가 후 재배포하여 500 미발생** → 이 가설이 **최종 원인**으로 확정.

**최종 원인 (Root Cause)**  
- **원인 한 줄**: 같은 트랜잭션 내에서 **soft-delete(UPDATE)가 DB에 반영되기 전에** 새 채팅방 **INSERT**가 flush되면서, 부분 유니크 제약 `(user_id, chatbot_id) WHERE is_deleted = false` 위반이 발생했다.  
- **기술적 메커니즘**:  
  - JPA 트랜잭션에서는 `save()`/엔티티 수정이 곧바로 SQL로 나가지 않고, **flush**(또는 커밋 시 자동 flush) 시점에 한꺼번에 실행된다.  
  - Hibernate의 flush 순서는 **엔티티 추가(insert) → 엔티티 수정(update) → 삭제** 등으로 정해질 수 있어, 이번 경우 INSERT가 먼저 나갔다.  
  - 그 결과, INSERT 시점에는 기존 행이 아직 `is_deleted = false`로 남아 있어, (user_id, chatbot_id)가 동일한 활성 행이 두 개가 되고, 부분 유니크 인덱스 위반 → PostgreSQL이 duplicate key 예외를 던짐.  
  - 해당 예외는 **트랜잭션 롤백/커밋 처리 과정**에서 발생하므로 `createRoom`의 try-catch 밖으로 전파되고, `GlobalExceptionHandler`에서 500으로 응답.

---

## 7) 해결 방법 (Fix)

### 7-1) 해결 전략

- **접근**: soft-delete를 수행한 직후, 새 방을 `save(room)` 하기 **전에** 영속성 컨텍스트의 변경(UPDATE)을 DB에 반영하도록 **명시적 `flush()`** 호출.  
- **선택 이유**:  
  - **다른 방법(예: soft-delete만 별도 `@Transactional(propagation = REQUIRES_NEW)`)** 는 트랜잭션 분리로 인한 복잡성·일관성 이슈가 생길 수 있음.  
  - **INSERT 전에 기존 행만 논리 삭제하는 쿼리(예: `@Modifying UPDATE ... WHERE ...`)**: 동작은 가능하나, 기존 `softDeleteRoom`과 이중으로 유지보수해야 함.  
  - **flush() 한 줄 추가**: 같은 트랜잭션·같은 서비스 로직을 유지하면서, 단지 **SQL 전송 순서만 보장**하므로 변경 범위가 작고 이해하기 쉬움.

### 7-2) 변경 내용 (Before / After)

**Before**  
- `ifPresent(... softDeleteRoom(...));` 다음에 아무 것도 없이 바로 `UUID roomId;` 로 진행.

**After**  
- `ifPresent` 블록 직후에 `chatRoomRepository.flush();` 추가.

**Diff:**

```diff
         chatRoomRepository.findByUser_IdAndChatbot_IdAndIsDeletedFalse(userId, chatbotId)
             .ifPresent(existing -> {
                 log.info("createRoom: existing active room found, soft-deleting: roomId={}, userId={}, chatbotId={}",
                     existing.getId(), userId, chatbotId);
                 softDeleteRoom(existing.getId(), userId);
             });
+        // flush: soft-delete UPDATE를 DB에 먼저 반영. 그렇지 않으면 save(room) 시 flush 순서로 INSERT가 먼저 나가 유니크(idx_chatrooms_user_chatbot) 위반 발생
+        chatRoomRepository.flush();

         UUID roomId;
```

### 7-3) 변경된 함수/역할 설명

- **chatRoomRepository.flush()**  
  - **입력/출력**: 없음 (void).  
  - **책임**: 현재 JPA 영속성 컨텍스트에서 보류 중인 INSERT/UPDATE/DELETE를 DB로 즉시 전송.  
  - **부작용/주의**: 같은 트랜잭션 내에서만 의미 있으며, 불필요한 flush는 성능에 약간 부담을 줄 수 있으나, 이번처럼 **순서 보장이 필수인 경우**에는 필요하다.

- **createRoom (수정 후)**  
  - **역할**: 기존과 동일하나, **기존 활성 방 soft-delete 직후 flush**를 넣어, 그 다음 `save(room)` 및 커밋 시 flush에서 나가는 INSERT가 실행될 때는 이미 기존 행이 `is_deleted = true`로 반영된 상태가 되도록 함.

---

## 8) 검증

- **재현 케이스**: 이미 (user_id, chatbot_id)로 활성 채팅방이 있는 상태에서 동일 조합으로 "채팅방 들어가기" → POST /api/chat/chatrooms.  
- **로그 변화**: 수정 전에는 "createRoom: saved successfully" 직후 같은 스레드에서 "duplicate key ... idx_chatrooms_user_chatbot" 및 "예상치 못한 오류 발생". 수정 후에는 "saved successfully"만 있고 500/duplicate key 로그 없음.  
- **화면/응답**: 수정 전 500, 수정 후 200 + 채팅방 정보.  
- **DB**: 수정 후에는 해당 (user_id, chatbot_id)에 대해 `is_deleted = false`인 행이 하나만 존재(기존 행은 is_deleted=true, 새 행이 하나 생성).  
- **성능**: flush 한 번 추가로 인한 오버헤드는 미미하며, 해당 API는 사용자 액션당 1회 호출 수준.

---

## 9) 회귀 방지

- **테스트**:  
  - **통합 테스트**에서 "이미 활성 채팅방이 있는 (user_id, chatbot_id)로 createRoom 호출 → 200, 기존 방 소프트 삭제 후 새 방 하나 생성" 시나리오 추가 권장.  
  - **단위 테스트**: `ChatService.createRoom`에서 soft-delete가 호출된 경우, `chatRoomRepository.flush()`가 호출되는지(또는 flush 후 save가 호출되는지) mock으로 검증 가능.  
- **모니터링**: POST /api/chat/chatrooms 5xx 비율 또는 "예상치 못한 오류 발생" + "idx_chatrooms_user_chatbot" 로그 모니터링.  
- **체크리스트**:  
  - "한 트랜잭션 안에서 기존 행 UPDATE와 새 행 INSERT를 같이 할 때, DB 제약(유니크 등)이 있다면 flush 순서가 보장되는지 확인"  
  - "JPA에서 UPDATE 후 INSERT 시, 제약 위반이 나면 flush/커밋 시점 예외를 고려해 try-catch 위치 검토"

---

## 10) 배운 점 / 정리

- **이번에 확정된 점**: 500의 직접 원인은 "다른 요청과의 경합"이 아니라 **같은 요청·같은 트랜잭션 내 flush 순서**였다. `save()`가 성공한 것처럼 로그가 찍혀도, 실제 제약 위반은 **커밋 시 flush**에서 발생할 수 있다.  
- **다음에 빨리 찾기 위한 포인트**: "saved successfully" 같은 로그가 있는데 그 직후 같은 스레드에서 제약 위반/예외가 나오면, **트랜잭션 경계와 flush 시점**을 의심할 것.  
- **일반화**: 한 트랜잭션에서 "기존 행 수정(UPDATE/soft-delete)"과 "새 행 추가(INSERT)"를 모두 할 때, DB 제약(유니크, 외래키 등)이 있으면 **수정이 먼저 DB에 반영되도록** 중간에 `flush()`를 넣는 패턴을 적용할 수 있다.

---

## 11) 참고 자료

- Hibernate: Flush ordering (공식 문서 Flush behavior).  
- Spring Data JPA: `JpaRepository#flush()`.  
- 프로젝트 내부: `docs/maintenance/CHATROOM_500_ERROR_LOG_ANALYSIS.md`, `CHATROOM_DUPLICATE_KEY_FRONTEND_BACKEND.md`.

---

## 재발 방지 체크리스트 (5개)

1. **같은 트랜잭션에서 UPDATE 후 INSERT**를 할 때, 유니크/제약이 걸린 컬럼이 있으면 **UPDATE가 먼저 DB에 반영되도록** 필요한 위치에 `repository.flush()` 또는 `entityManager.flush()`를 넣었는지 확인한다.  
2. **채팅방 생성/수정 로직 변경 시** "기존 활성 방 soft-delete → 새 방 생성" 순서와 flush 호출이 제거되거나 바뀌지 않았는지 코드 리뷰에서 확인한다.  
3. **createRoom 관련 통합 테스트**에 "이미 활성 방이 있는 (user_id, chatbot_id)로 생성 요청 → 200 및 DB에 활성 방 1개" 시나리오를 포함해 두고, CI에서 항상 실행한다.  
4. **배포 후** 짧은 기간이라도 POST /api/chat/chatrooms 5xx 또는 "duplicate key ... idx_chatrooms_user_chatbot" 로그가 없는지 확인한다.  
5. **JPA flush/commit 시점 예외**는 컨트롤러/서비스 메서드의 try-catch로 잡히지 않을 수 있으므로, "제약 위반 시 500"을 줄이려면 가능한 한 **flush 순서를 보장**해 제약 위반 자체가 나지 않도록 하는 쪽을 우선한다.
