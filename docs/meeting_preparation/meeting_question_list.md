# DoranDoran 프로젝트 면접 답안 목록

> 면접 대비를 위한 프로젝트 질문별 답안 정리 문서

---

## 1. 프로젝트/역할 전반

### Q1. 도란도란 서비스가 어떤 문제를 해결하려는 서비스인지, 한 줄로 설명해보세요.

**답안**: 외국인 사용자가 AI 챗봇과 대화하며 한국어 친밀도(존댓말/반말)를 학습하고, 어휘를 습득할 수 있는 실시간 한국어 학습 플랫폼입니다.

---

### Q2. 이 프로젝트에서 본인이 맡은 역할을 한 문장으로 정의한다면요?

**답안**: BE 3명 중 Gateway/Auth/User/Chat/모니터링/인프라 전반을 담당하여, MSA 아키텍처 설계부터 인증/인가, 실시간 채팅, 멀티 에이전트 AI 시스템, 성능 최적화, 장애 대응까지 백엔드 전 영역을 책임졌습니다.

---

### Q3. BE 3명 중에 Gateway/Auth/User/Chat/모니터링/인프라까지 맡게 된 과정이 어떻게 됐나요?

**답안**: 
- 초기에는 역할 분담을 시도했으나, 한 달 남짓한 짧은 기간에 5개 서비스 + Gateway + 인프라를 구축해야 하는 상황에서
- 팀원들의 다른 우선순위(프론트엔드 연동, 다른 기능 개발)로 인해 백엔드 인프라와 핵심 서비스 구축이 필요했고
- 제가 MSA 아키텍처 설계 경험이 있어 Gateway와 서비스 간 통신 구조를 설계하고 구현하게 되었습니다
- 이후 인증/인가, 실시간 채팅, AI 통합 등 핵심 기능들이 모두 제가 담당한 영역과 연관되어 자연스럽게 전 영역을 맡게 되었습니다
- 모니터링과 인프라는 서비스 안정성 확보를 위해 필수적이었고, 장애 대응 경험을 통해 직접 구축하게 되었습니다

---

### Q4. MSA로 나눈 서비스(Auth/User/Chat/Store/Batch)의 경계를 어떻게 정의했나요? 왜 이렇게 나눴나요?

**답안**:

**서비스 경계 정의 기준**:
1. **도메인 독립성**: 각 서비스가 독립적인 비즈니스 도메인을 담당
   - Auth: 인증/인가 전담 (JWT, OAuth, 토큰 관리)
   - User: 사용자 정보 관리 (프로필, 설정)
   - Chat: 채팅 및 AI 에이전트 오케스트레이션
   - Store: 표현 보관함 (독립적인 기능)
   - Batch: 스케줄링된 작업 (독립 실행)

2. **데이터 소유권**: 서비스별 스키마 분리
   - `auth_schema`: RefreshToken, TokenBlacklist, LoginAttempts
   - `user_schema`: app_user, profiles, settings
   - `chat_schema`: chatrooms, messages, intimacy_progress
   - `store_schema`: stores
   - `batch_schema`: 배치 작업 데이터

3. **독립 배포 가능성**: 서비스별 독립적인 배포 및 스케일링
   - Chat Service는 AI 처리로 인한 높은 부하 → 독립 스케일링
   - Auth Service는 인증 요청 집중 → 독립 스케일링

**이렇게 나눈 이유**:
- **확장성**: Chat Service만 독립적으로 스케일 아웃 가능
- **장애 격리**: 한 서비스 장애가 다른 서비스에 영향 최소화 (Circuit Breaker)
- **기술 다양화**: Gateway는 Reactive, 다른 서비스는 MVC 패턴 사용 가능
- **팀 독립성**: 향후 팀 확장 시 서비스별 담당 가능

---

### Q5. 한 달 남짓한 기간에 이 정도 범위를 어떻게 우선순위 정해서 진행했는지 설명해보세요.

**답안**:

**우선순위 결정 기준**:
1. **사용자 가치**: 사용자가 실제로 사용할 수 있는 기능 우선
2. **의존성**: 다른 기능의 기반이 되는 인프라/인증 우선
3. **위험도**: 장애 시 전체 서비스에 영향이 큰 부분 우선

**단계별 진행**:

**Week 1: 인프라 & 인증 기반 구축**
- Docker Compose로 로컬 개발 환경 구축
- PostgreSQL 단일 인스턴스 + 멀티 스키마 구조 설계
- Gateway + Auth Service 기본 구조 (JWT 인증)
- User Service 기본 CRUD

**Week 2: 핵심 기능 구현**
- Chat Service 기본 구조 (채팅방, 메시지)
- SSE 실시간 통신 구현
- OpenAI API 통합 (기본 대화 생성)

**Week 3: 멀티 에이전트 시스템**
- IntimacyAgent, VocabularyAgent, ConversationAgent 구현
- MultiAgentOrchestrator 병렬/순차 처리 로직
- SSE 이벤트 스트리밍

**Week 4: 최적화 & 안정화**
- 커넥션 풀 최적화 (SSE + open-in-view 이슈 해결)
- Redis 캐싱 전략 적용
- 모니터링 스택 구축 (Prometheus + Grafana)
- 장애 대응 및 트러블슈팅

**결정 원칙**:
- **MVP 우선**: 완벽한 구현보다 동작하는 최소 기능 우선
- **점진적 개선**: 기본 기능 구현 후 성능/안정성 개선
- **리스크 관리**: 장애 발생 가능성 높은 부분(SSE, DB 연결) 조기 검증

---

## 2. Java/Spring & Web 기본

### Q6. Spring Security + JWT + OAuth2를 사용했다고 했는데, 인증/인가 흐름을 요청 한 번 기준으로 설명해보세요.

**답안**:

**일반 API 요청 흐름**:

```
1. Client → Gateway: GET /api/chat/rooms
   Authorization: Bearer <accessToken>

2. Gateway (JwtAuthFilter):
   - Authorization 헤더에서 토큰 추출
   - Auth Service로 토큰 검증 요청: GET /api/auth/validate
   - 검증 성공 시:
     * JWT 페이로드 파싱 (Base64 디코딩)
     * userId, email, name 추출
     * HMAC 서명 생성: HMAC-SHA256(userId|timestamp, secret)
     * X-User-Id, X-Auth-Ts, X-Auth-Sign 헤더 주입

3. Gateway → Chat Service: GET /api/chat/rooms
   X-User-Id: <userId>
   X-Auth-Ts: <timestamp>
   X-Auth-Sign: <hmacSignature>

4. Chat Service (HmacAuthInterceptor):
   - HMAC 헤더 검증
   - 타임스탬프 검증 (60초 이내)
   - 서명 검증: HMAC-SHA256(userId|ts, secret) == X-Auth-Sign
   - SecurityContext에 사용자 정보 설정

5. Chat Service → DB: 쿼리 실행 (사용자 정보 사용)

6. Chat Service → Gateway: 응답

7. Gateway → Client: 응답
```

**핵심 포인트**:
- Gateway에서 JWT 검증 후 HMAC 헤더 주입으로 서비스 간 인증
- 타임스탬프 기반 리플레이 공격 방지
- 서비스는 JWT 검증 없이 HMAC만 검증 (성능 최적화)

---

### Q7. Refresh Token 회전 구조를 어떻게 설계했나요? (테이블 구조 + 플로우)

**답안**:

**테이블 구조** (`auth_schema.refresh_tokens`):

```sql
CREATE TABLE auth_schema.refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL,
    token TEXT NOT NULL,  -- 원문 저장 (향후 해시 저장 권장)
    issued_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    rotated_from_id BIGINT,  -- 회전 추적
    device_id VARCHAR(200),
    user_agent VARCHAR(500),
    ip_address VARCHAR(45),
    FOREIGN KEY (rotated_from_id) REFERENCES refresh_tokens(id) ON DELETE SET NULL
);
```

**회전 플로우**:

```
1. Client → Gateway: POST /api/auth/refresh
   { refreshToken: "..." }

2. Gateway → Auth Service: POST /api/auth/refresh

3. Auth Service (RefreshTokenService.rotate()):
   a. Refresh Token 검증 (만료, 서명)
   b. DB에서 현재 토큰 조회
   c. revoked = true로 설정 (기존 토큰 무효화)
   d. 새 Refresh Token 생성
   e. 새 토큰 저장 (rotated_from_id = 기존 토큰 id)
   f. 새 Access Token 생성
   
4. Auth Service → Gateway: 
   { accessToken, refreshToken }

5. Gateway → Client: 새 토큰 반환
```

**회전 전략**:
- **매번 회전**: Refresh Token 사용 시마다 새 토큰 발급
- **기존 토큰 무효화**: `revoked = true`로 설정하여 재사용 방지
- **회전 추적**: `rotated_from_id`로 토큰 체인 추적 (보안 감사)

**보안 고려사항**:
- 향후 토큰 해시 저장으로 DB 유출 시 피해 최소화
- Device ID, IP 주소 기록으로 이상 접근 감지

---

### Q8. ChatService 에서 핵심 비즈니스 로직은 무엇이고, 트랜잭션 경계는 어디까지로 잡았나요?

**답안**:

**핵심 비즈니스 로직**:

1. **채팅방 조회/생성** (`getOrCreateRoom`)
   - 사용자-챗봇 조합으로 채팅방 조회 또는 생성
   - 컨셉(FRIEND, COWORKER 등) 및 친밀도 레벨 설정

2. **메시지 전송** (`sendMessage`)
   - 메시지 저장 (sequence_number 자동 채번)
   - 채팅방의 last_message_at, last_message_id 업데이트
   - 사용자-챗봇 상호작용 기록 업데이트

3. **메시지 조회** (`getMessages`)
   - 채팅방별 메시지 페이징 조회
   - sequence_number 기준 정렬

**트랜잭션 경계**:

```java
@Transactional
public ChatRoom getOrCreateRoom(UUID userId, UUID chatbotId, ...) {
    // 채팅방 조회 또는 생성
    // IntimacyProgress 초기화
    // 하나의 트랜잭션으로 원자성 보장
}

@Transactional
public Message sendMessage(UUID chatroomId, UUID senderId, ...) {
    // 1. 메시지 저장
    Message saved = messageRepository.save(message);
    
    // 2. 채팅방 업데이트
    chatRoom.setLastMessageAt(now);
    chatRoom.setLastMessage(saved);
    chatRoomRepository.save(chatRoom);
    
    // 3. 상호작용 기록 업데이트
    updateLastInteraction(...);
    
    // 하나의 트랜잭션으로 데이터 일관성 보장
    return saved;
}
```

**트랜잭션 전략**:
- **Service 레이어**: `@Transactional`로 메서드 단위 트랜잭션
- **Controller 레이어**: 트랜잭션 없음 (Service에 위임)
- **AI 처리**: `@Async`, `@Transactional(propagation = REQUIRES_NEW)`로 별도 트랜잭션
  - AI 처리 실패 시 메시지 저장은 롤백되지 않음

**주의사항**:
- `open-in-view=false`로 인해 Lazy 로딩은 트랜잭션 내에서만 가능
- DTO 변환은 Service 레이어에서 완료

---

### Q9. SSE 스트리밍과 일반 REST API를 같은 서비스 안에서 어떻게 구분해서 구현했나요?

**답안**:

**구현 방식**:

1. **SSE 엔드포인트** (`SSEController`):
```java
@GetMapping(value = "/stream/{chatroomId}", 
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public ResponseEntity<SseEmitter> stream(@PathVariable UUID chatroomId) {
    // SSE 연결 생성
    SseEmitter emitter = sseManager.create(chatroomId);
    return ResponseEntity.ok()
        .header("Cache-Control", "no-cache")
        .header("Connection", "keep-alive")
        .body(emitter);
}
```

2. **일반 REST API** (`ChatController`):
```java
@PostMapping("/rooms/{chatroomId}/messages")
public ResponseEntity<MessageResponse> sendMessage(...) {
    // 일반 HTTP 요청 처리
    Message saved = chatService.sendMessage(...);
    return ResponseEntity.ok(MessageResponse.from(saved));
}
```

**구분 기준**:
- **Content-Type**: SSE는 `text/event-stream`, REST는 `application/json`
- **응답 방식**: SSE는 `SseEmitter`, REST는 `ResponseEntity<T>`
- **연결 지속성**: SSE는 long-running, REST는 요청-응답 후 종료

**SSE 이벤트 전송**:
```java
// SSEManager를 통한 이벤트 브로드캐스트
sseManager.send(chatroomId, "intimacy_analysis", data);
sseManager.send(chatroomId, "conversation_chunk", text);
```

**Gateway 라우팅**:
- SSE 경로: `/api/chat/stream/**` → Chat Service
- 일반 REST: `/api/chat/**` → Chat Service
- Gateway에서 SSE 타임아웃 600초 설정

---

### Q10. open-in-view를 제거했다고 했는데, open-in-view가 무엇이고, 왜 문제가 됐나요?

**답안**:

**open-in-view란**:
- Spring의 `OpenEntityManagerInViewFilter` (또는 `OpenSessionInViewFilter`)
- HTTP 요청 시작 시 DB 연결 획득 → 응답 완료까지 연결 유지
- 기본값: `spring.jpa.open-in-view=true`

**문제 상황**:

**SSE + open-in-view 조합**:
```
1. Client → Server: GET /api/chat/stream/{chatroomId}
2. open-in-view=true: DB 연결 획득
3. SSE 스트림 시작 (long-running, 수 분~수십 분 유지)
4. ❌ DB 연결이 SSE 연결 동안 계속 점유됨
5. 연결 풀 고갈 (10개 연결 모두 점유)
6. 새 요청 처리 불가 (30초 타임아웃 후 실패)
```

**실제 발생한 증상** (2025-10-14):
- `HikariPool-1 - Connection is not available, request timed out after 30000ms`
- 모든 API 요청 503 Service Unavailable
- 컨테이너 `unhealthy` 상태

**해결 방법**:
```yaml
spring:
  jpa:
    open-in-view: false  # SSE 연결 누수 방지
```

**효과**:
- 트랜잭션 종료 시 즉시 DB 연결 반환
- SSE 같은 long-running 요청에서 연결 점유 방지
- 연결 풀 여유 확보

**주의사항**:
- `LazyInitializationException` 발생 가능
- DTO 변환을 Service 레이어에서 완료해야 함
- Fetch Join 또는 `@EntityGraph` 사용 필요

---

## 3. JPA / DB / ERD 관련

### Q11. PostgreSQL 단일 인스턴스 + 멀티 스키마 구조를 선택한 이유는 무엇인가요?

**답안**:

**선택 이유**:

1. **비용 최적화**
   - Aurora RDS → 단일 PostgreSQL로 마이그레이션하여 월 인프라 비용 50% 절감
   - 단일 인스턴스로 운영 비용 최소화

2. **MSA 원칙 준수**
   - 서비스별 스키마 분리로 데이터 격리
   - 향후 서비스별 독립 DB로 분리 가능 (스키마 → DB 마이그레이션 용이)

3. **운영 단순성**
   - 백업/복원이 단일 인스턴스로 간단
   - 모니터링 포인트 단일화
   - 트랜잭션 경계 명확 (같은 DB 내)

4. **개발 속도**
   - 초기 구축 시간 단축 (단일 DB 설정)
   - 스키마 분리만으로 서비스 독립성 확보

**스키마 구조**:
```sql
CREATE SCHEMA auth_schema;    -- Auth Service
CREATE SCHEMA user_schema;    -- User Service
CREATE SCHEMA chat_schema;     -- Chat Service
CREATE SCHEMA store_schema;   -- Store Service
CREATE SCHEMA batch_schema;   -- Batch Service
```

**장단점**:

**장점**:
- 비용 효율적
- 운영 단순
- 트랜잭션 경계 명확

**단점**:
- 단일 장애점 (하지만 백업/복원 전략으로 완화)
- 서비스별 독립 스케일링 불가 (하지만 현재 트래픽 수준에서는 불필요)

**향후 전략**:
- 트래픽 증가 시 서비스별 독립 DB로 분리
- 스키마 → DB 마이그레이션 스크립트 준비

---

### Q12. ERD에서 app_user, chatrooms, messages 관계를 기준으로, 한 유저가 채팅 한 번 보낼 때 어떤 쿼리들이 나가는지 설명해보세요.

**답안**:

**엔티티 관계**:
```
app_user (user_schema)
  ↓ 1:N
chatrooms (chat_schema)
  ↓ 1:N
messages (chat_schema)
```

**메시지 전송 시 쿼리 흐름**:

```
1. POST /api/chat/rooms/{chatroomId}/messages
   { content: "안녕하세요" }

2. ChatController.sendMessage()
   → ChatService.sendMessage()

3. 쿼리 1: 채팅방 조회
   SELECT * FROM chat_schema.chatrooms 
   WHERE id = ? AND is_deleted = false
   (채팅방 존재 및 권한 확인)

4. 쿼리 2: 다음 sequence_number 조회
   SELECT MAX(sequence_number) FROM chat_schema.messages 
   WHERE chatroom_id = ?
   (메시지 순서 보장)

5. 쿼리 3: 메시지 저장
   INSERT INTO chat_schema.messages 
   (id, chatroom_id, sender_id, sender_type, content, 
    sequence_number, created_at, updated_at, is_deleted)
   VALUES (?, ?, ?, 'user', ?, ?, NOW(), NOW(), false)

6. 쿼리 4: 채팅방 업데이트
   UPDATE chat_schema.chatrooms 
   SET last_message_at = NOW(), 
       last_message_id = ?, 
       updated_at = NOW()
   WHERE id = ?

7. 쿼리 5: 사용자-챗봇 상호작용 기록 업데이트
   INSERT INTO chat_schema.user_chatbot_last_interaction 
   (user_id, chatbot_id, last_interaction_at, chatroom_id)
   VALUES (?, ?, NOW(), ?)
   ON CONFLICT (user_id, chatbot_id) 
   DO UPDATE SET last_interaction_at = NOW(), chatroom_id = ?
```

**총 쿼리 수**: 5개 (트랜잭션 내에서 실행)

**최적화 포인트**:
- `sequence_number` 조회는 `@Query`로 최적화 가능
- `last_message` 업데이트는 JPA `@OneToOne` 관계로 자동화 가능 (현재는 명시적 업데이트)

**N+1 문제 방지**:
- `open-in-view=false`로 인해 Lazy 로딩은 트랜잭션 내에서만
- 필요한 경우 `@EntityGraph` 또는 Fetch Join 사용

---

### Q13. JSONB를 어디에 사용했고, RDB 테이블 컬럼으로 풀지 않고 JSONB로 둔 이유가 뭔가요?

**답안**:

**JSONB 사용 위치**:

1. **`chatrooms.settings`** (JSONB):
```json
{
  "concept": "FRIEND",
  "testModel": "a"
}
```

2. **`intimacy_progress.progress_data`** (JSONB):
```json
{
  "summaryHistory": [...],
  "keywordIndex": { "items": [...] },
  "correctionsHistory": [...],
  "lastContextSnapshot": {...}
}
```

3. **`messages.metadata`** (JSONB):
```json
{
  "userMessageAnalysis": {
    "intimacy": {
      "detectedLevel": 1,
      "correctedSentence": "...",
      "feedback": {"ko": "...", "en": "..."}
    }
  },
  "botResponseAnalysis": {
    "vocabulary": { "words": [...] }
  },
  "usage": {
    "inputTokens": 100,
    "outputTokens": 50
  }
}
```

**JSONB를 선택한 이유**:

1. **유연한 스키마 변경**
   - AI 에이전트 응답 구조가 자주 변경됨
   - 정규화된 테이블로 분리 시 마이그레이션 비용 높음
   - JSONB는 스키마 변경 없이 필드 추가/수정 가능

2. **부분 업데이트 지원**
   - PostgreSQL 14+ `jsonb_set()` 함수로 특정 필드만 업데이트
   - `progress_data`의 `summaryHistory`만 추가하는 경우 효율적

3. **인덱싱 지원**
   - GIN 인덱스로 JSONB 필드 검색 최적화
   ```sql
   CREATE INDEX idx_chatrooms_settings_concept 
   ON chatrooms USING GIN ((settings->>'concept'));
   ```

4. **데이터 구조 특성**
   - `metadata`는 메시지별로 구조가 다를 수 있음 (user 메시지 vs bot 메시지)
   - `progress_data`는 복잡한 중첩 구조 (배열, 객체 혼합)
   - 정규화 시 JOIN 비용이 높고 쿼리 복잡도 증가

5. **성능 고려**
   - 메시지 조회 시 metadata는 항상 함께 조회됨
   - 별도 테이블로 분리 시 JOIN 비용 발생
   - JSONB는 컬럼으로 저장되어 JOIN 불필요

**트레이드오프**:
- **장점**: 유연성, 개발 속도, 부분 업데이트
- **단점**: 쿼리 복잡도 (하지만 PostgreSQL JSONB 연산자로 완화)

---

### Q14. 소프트 삭제(soft delete)를 적용했다고 했는데, 어떤 엔티티에 적용했고, 구현 방식은 어떻게 했나요?

**답안**:

**적용 엔티티**:

1. **`ChatRoom`**:
```java
@Column(name = "is_deleted")
private Boolean isDeleted;

@PrePersist
private void prePersist() {
    if (isDeleted == null) {
        isDeleted = false;
    }
}
```

2. **`Message`**:
```java
@Column(name = "is_deleted")
private Boolean isDeleted;

@Column(name = "deleted_at")
private LocalDateTime deletedAt;
```

3. **`Store`**:
```java
@Column(name = "is_deleted", nullable = false)
@Builder.Default
private Boolean isDeleted = false;

@Column(name = "deleted_at")
private LocalDateTime deletedAt;
```

**구현 방식**:

1. **엔티티 레벨**:
   - `is_deleted` Boolean 필드 추가
   - `deleted_at` LocalDateTime 필드 추가 (선택적)
   - `@PrePersist`로 기본값 설정

2. **Repository 레벨**:
```java
// 삭제되지 않은 것만 조회
boolean existsByUser_IdAndIdAndIsDeletedFalse(UUID userId, UUID id);

// 삭제되지 않은 메시지 조회
List<Message> findByChatRoomIdAndIsDeletedFalse(UUID chatroomId);
```

3. **Service 레벨**:
```java
public void deleteChatRoom(UUID chatroomId) {
    ChatRoom room = chatRoomRepository.findById(chatroomId)
        .orElseThrow();
    room.setIsDeleted(true);
    room.setUpdatedAt(LocalDateTime.now());
    chatRoomRepository.save(room);
    // 하드 삭제는 Batch Service에서 처리
}
```

4. **인덱스 최적화**:
```sql
-- 부분 인덱스로 삭제되지 않은 것만 인덱싱
CREATE INDEX idx_chatrooms_user_active 
ON chatrooms(user_id) 
WHERE is_deleted = false;
```

**하드 삭제 전략**:
- Batch Service에서 주기적으로 삭제된 데이터 정리
- `@Scheduled(cron = "0 0 2 * * *")` 매일 새벽 2시 실행
- 90일 이상 삭제된 데이터만 하드 삭제

**장점**:
- 데이터 복구 가능
- 삭제 이력 추적
- 참조 무결성 유지 (메시지가 채팅방을 참조하는 경우)

---

### Q15. DB 마이그레이션으로 월 인프라 비용을 50% 줄였다고 했는데, Before → What I did → After 를 DB 관점에서 자세히 설명해보세요. (쿼리 최적화, 인덱스, 캐싱 등)

**답안**:

**Before (Aurora RDS)**:

**인프라 구성**:
- Aurora PostgreSQL (db.t3.medium, 2 vCPU, 4GB RAM)
- 월 비용: 약 $150-200

**문제점**:
- 오버프로비저닝: 실제 사용량 대비 과도한 리소스
- 비용 대비 성능 미달: 트래픽 수준에 비해 비싼 인프라

**What I did**:

1. **단일 PostgreSQL 인스턴스로 마이그레이션**
   - EC2 t3.medium에 PostgreSQL 17 컨테이너 실행
   - Docker Compose로 관리

2. **스키마 분리 전략**
   ```sql
   CREATE SCHEMA auth_schema;
   CREATE SCHEMA user_schema;
   CREATE SCHEMA chat_schema;
   CREATE SCHEMA store_schema;
   CREATE SCHEMA batch_schema;
   ```

3. **연결 풀 최적화**
   ```yaml
   # 서비스별 연결 풀 크기 조정
   Chat: maximum-pool-size: 20
   User: maximum-pool-size: 15
   Auth: maximum-pool-size: 10
   Store: maximum-pool-size: 10
   Batch: maximum-pool-size: 5
   총: 60개 (PostgreSQL max_connections=100, 여유 40개)
   ```

4. **인덱스 최적화**
   ```sql
   -- 복합 인덱스
   CREATE INDEX idx_chatrooms_user_id_id 
   ON chatrooms(user_id, id) 
   WHERE is_deleted = false;
   
   -- 메시지 조회 최적화
   CREATE INDEX idx_messages_chatroom_seq 
   ON messages(chatroom_id, sequence_number);
   
   -- JSONB 인덱스
   CREATE INDEX idx_chatrooms_settings_concept 
   ON chatrooms USING GIN ((settings->>'concept'));
   ```

5. **Redis 캐싱 전략**
   ```java
   @Cacheable(value = "messageHistory", key = "#chatroomId")
   private List<Map<String, String>> buildMessageHistory(UUID chatroomId) {
       // 메시지 히스토리 조회 및 캐싱
   }
   
   @Cacheable(value = "intimacy", key = "#chatroomId")
   private int getCurrentIntimacyLevel(UUID chatroomId) {
       // 친밀도 레벨 조회 및 캐싱
   }
   ```

6. **쿼리 최적화**
   - `open-in-view=false`로 불필요한 연결 점유 방지
   - Fetch Join으로 N+1 문제 해결
   - 부분 인덱스로 삭제된 데이터 제외

**After (단일 PostgreSQL)**:

**인프라 구성**:
- EC2 t3.medium (기존 인스턴스 활용)
- PostgreSQL 17 컨테이너
- 월 비용: 약 $75-100 (50% 절감)

**성능 개선**:
- 연결 풀 사용률: 60% (60/100)
- 쿼리 응답 시간: 평균 50ms → 30ms (인덱스 최적화)
- 캐시 히트율: 70% (Redis 캐싱)

**모니터링**:
- Prometheus + Grafana로 DB 메트릭 수집
- PostgreSQL Exporter로 연결 수, 쿼리 성능 추적

**검증 방법**:
1. **부하 테스트**: 동일 트래픽으로 성능 비교
2. **메트릭 비교**: 쿼리 응답 시간, 연결 수, CPU/메모리 사용률
3. **비용 분석**: AWS 비용 대시보드로 실제 절감 확인

---

## 4. 트러블슈팅 & 성능 이슈

### Q16. "DB 커넥션 풀 고갈 & 메모리 이슈"를 겪었다고 했는데, 당시에 어떤 증상으로 시작됐나요?

**답안**:

**초기 증상** (2025-10-14 15:20):

1. **API 응답 실패**
   - 모든 API 요청이 30초 타임아웃 후 실패
   - `503 Service Unavailable` 응답

2. **컨테이너 상태**
   - `dorandoran-chat` 컨테이너: `unhealthy`
   - `/actuator/health` 엔드포인트: 500 Internal Server Error

3. **에러 로그**:
```
HikariPool-1 - Connection is not available, request timed out after 30000ms 
(total=10, active=10, idle=0, waiting=0)

DataSource health check failed
org.springframework.jdbc.CannotGetJdbcConnectionException: 
Failed to obtain JDBC Connection
```

4. **연결 누수 감지**:
```
Connection leak detection triggered for org.postgresql.jdbc.PgConnection@64f613da
Previously reported leaked connection ... was returned to the pool (unleaked)
```

**증상 분석**:
- 연결 풀 10개가 모두 `active` 상태
- 새 요청은 `waiting=0` (대기 없이 즉시 실패)
- DB 측에서는 연결이 `idle` 상태 (정상 반환됨)
- 문제는 애플리케이션 레이어(HikariCP)에 있음

---

### Q17. Prometheus/Grafana 지표 중 특히 어떤 그래프를 보고 원인을 좁혀갔나요?

**답안**:

**핵심 지표**:

1. **HikariCP 연결 풀 메트릭**:
   - `hikaricp_connections_active`: 10 (최대치)
   - `hikaricp_connections_idle`: 0
   - `hikaricp_connections_pending`: 증가 추세
   - **결론**: 연결 풀 고갈 확인

2. **HTTP 요청 메트릭**:
   - `http_server_requests_seconds_count{uri="/api/chat/stream/{chatroomId}"}`: 증가
   - `http_server_requests_seconds_max{uri="/api/chat/stream/{chatroomId}"}`: 600초 (타임아웃)
   - **결론**: SSE 연결이 오래 유지됨

3. **JVM 메모리 메트릭**:
   - `jvm_memory_used_bytes{area="heap"}`: 증가 추세
   - `jvm_gc_pause_seconds_count`: 증가
   - **결론**: 메모리 압박 (부차적 문제)

4. **PostgreSQL 연결 메트릭** (Postgres Exporter):
   - `pg_stat_activity_count{state="idle"}`: 9개
   - `pg_stat_activity_count{state="active"}`: 1개
   - **결론**: DB는 정상, 애플리케이션 레이어 문제

**원인 추적 과정**:

1. **연결 풀 고갈 확인** → HikariCP 메트릭
2. **SSE 연결 확인** → HTTP 요청 메트릭
3. **open-in-view 설정 확인** → 설정 파일 검토
4. **연결 누수 확인** → 로그 분석

**Grafana 대시보드 구성**:
- HikariCP 연결 풀 대시보드
- HTTP 요청 지연시간 대시보드
- JVM 메모리 대시보드
- PostgreSQL 연결 수 대시보드

---

### Q18. SSE + open-in-view 조합에서 커넥션이 어떻게 길게 잡혀 있었나요? 흐름을 자세히 말해보세요.

**답안**:

**문제 흐름**:

```
1. Client → Gateway: GET /api/chat/stream/{chatroomId}
   Authorization: Bearer <token>

2. Gateway → Chat Service: GET /api/chat/stream/{chatroomId}
   (JWT 검증 완료, HMAC 헤더 주입)

3. Chat Service (SSEController.stream()):
   - HTTP 요청 시작
   - open-in-view=true → OpenEntityManagerInViewFilter 실행
   - DB 연결 획득 (HikariCP에서 연결 빌려옴)
   - EntityManager 열림 상태로 유지

4. 채팅방 권한 확인:
   SELECT * FROM chatrooms 
   WHERE user_id = ? AND id = ? AND is_deleted = false
   (트랜잭션 내에서 실행, 연결 점유)

5. SSE 연결 생성:
   SseEmitter emitter = sseManager.create(chatroomId);
   return ResponseEntity.ok().body(emitter);
   
   ❌ 문제: HTTP 응답이 완료되지 않음 (SSE는 long-running)
   ❌ open-in-view는 HTTP 요청 종료까지 연결 유지
   ❌ SSE 연결이 수 분~수십 분 유지되는 동안 DB 연결도 계속 점유

6. 시간 경과:
   - SSE 연결: 계속 유지 (클라이언트가 연결 유지)
   - DB 연결: 계속 점유 (open-in-view가 연결 반환 안 함)
   - 연결 풀: 10개 연결 모두 점유됨

7. 새 요청:
   - DB 연결 획득 시도
   - 연결 풀에 여유 없음
   - 30초 타임아웃 후 실패
```

**핵심 문제**:
- `open-in-view=true`: HTTP 요청 생명주기 동안 DB 연결 유지
- SSE: HTTP 요청이 완료되지 않음 (long-running)
- 결과: DB 연결이 SSE 연결 동안 계속 점유

**해결 후 흐름**:

```
1-4. 동일

5. SSE 연결 생성:
   SseEmitter emitter = sseManager.create(chatroomId);
   
   ✅ open-in-view=false → 트랜잭션 종료 시 즉시 연결 반환
   ✅ SSE 연결은 DB 연결과 독립적으로 유지

6. 시간 경과:
   - SSE 연결: 계속 유지
   - DB 연결: 이미 반환됨 (트랜잭션 종료 시)
   - 연결 풀: 여유 있음
```

---

### Q19. 커넥션 풀 설정은 어떤 값을 기준으로 조정했나요? (max connections, timeout 등)

**답안**:

**조정 기준**:

1. **서비스별 사용 패턴 분석**:
   - Chat: 실시간 메시지 처리, SSE 연결 → 높은 부하
   - User: 사용자 정보 조회 (Redis 캐싱) → 중간 부하
   - Auth: 토큰 검증 (JWT 기반, DB는 RefreshToken만) → 낮은 부하
   - Store: 보관함 조회 (Redis 캐싱) → 낮은 부하
   - Batch: 하루 한 번 실행 → 최소 부하

2. **최종 설정**:

```yaml
# Chat Service
spring:
  datasource:
    hikari:
      maximum-pool-size: 20      # SSE 연결 고려
      minimum-idle: 5            # 항상 5개 대기
      connection-timeout: 20000  # 20초
      idle-timeout: 300000       # 5분
      max-lifetime: 600000       # 10분
      leak-detection-threshold: 10000  # 10초

# User Service
spring:
  datasource:
    hikari:
      maximum-pool-size: 15
      minimum-idle: 5
      connection-timeout: 20000
      idle-timeout: 300000
      max-lifetime: 600000

# Auth Service
spring:
  datasource:
    hikari:
      maximum-pool-size: 10      # 토큰 검증은 JWT 기반
      minimum-idle: 5
      connection-timeout: 20000

# Store Service
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5

# Batch Service
spring:
  datasource:
    hikari:
      maximum-pool-size: 5       # 하루 한 번 실행
      minimum-idle: 1            # 평소 최소화
```

**총 연결 수**: 60개
**PostgreSQL max_connections**: 100개 (여유 40개)

**설정 근거**:

1. **maximum-pool-size**:
   - 동시 처리 요청 수 추정
   - Chat: SSE 연결 고려하여 20개
   - 다른 서비스: 평균 동시 요청 수 기반

2. **minimum-idle**:
   - 항상 대기할 연결 수
   - 응답성 향상 (연결 생성 비용 절감)

3. **connection-timeout**:
   - 연결 획득 대기 시간
   - 20초: 일반적인 요청 처리 시간 고려

4. **idle-timeout**:
   - 유휴 연결 유지 시간
   - 5분: 짧은 유휴 후 반환하여 리소스 절약

5. **max-lifetime**:
   - 연결 최대 수명
   - 10분: 주기적 갱신으로 연결 안정성 확보

6. **leak-detection-threshold**:
   - 연결 누수 감지 시간
   - 10초: 조기 감지 및 경고

**모니터링**:
- Prometheus로 연결 풀 사용률 추적
- 80% 이상 사용 시 알림

---

### Q20. Redis 캐시를 어디에 붙였고, 어떤 기준으로 캐시 키를 설계했나요?

**답안**:

**캐시 적용 위치**:

1. **메시지 히스토리** (`ChatService`):
```java
@Cacheable(value = "messageHistory", key = "#chatroomId")
private List<Map<String, String>> buildMessageHistory(UUID chatroomId) {
    // 최근 10개 메시지 조회
    // OpenAI API 호출 시 사용
}
```
- **캐시 키**: `messageHistory::<chatroomId>`
- **TTL**: 10분 (기본값)
- **용도**: ConversationAgent가 메시지 히스토리를 매번 조회하지 않도록

2. **친밀도 레벨** (`IntimacyProgressService`):
```java
@Cacheable(value = "intimacy", key = "#chatroomId")
private int getCurrentIntimacyLevel(UUID chatroomId) {
    // 현재 친밀도 레벨 조회
}
```
- **캐시 키**: `intimacy::<chatroomId>`
- **TTL**: 10분
- **용도**: 친밀도 레벨 조회 빈도 높음

3. **토큰 블랙리스트** (`TokenBlacklistService`):
```java
// Redis에 토큰 해시 저장
redisTemplate.opsForValue().set(
    "blacklist:" + tokenHash, 
    "revoked", 
    Duration.ofMillis(ttl)
);
```
- **캐시 키**: `blacklist:<tokenHash>`
- **TTL**: Access Token 만료 시간
- **용도**: 로그아웃된 토큰 빠른 조회

**캐시 키 설계 기준**:

1. **네임스페이스 구분**:
   - `messageHistory::<chatroomId>`
   - `intimacy::<chatroomId>`
   - `blacklist:<tokenHash>`
   - 콜론(`::`) 또는 콜론(`:`)으로 구분

2. **고유성 보장**:
   - 채팅방별, 사용자별로 고유한 키
   - UUID 사용으로 충돌 방지

3. **조회 패턴 고려**:
   - 채팅방별 조회 → `chatroomId` 기반
   - 사용자별 조회 → `userId` 기반

**캐시 전략**:

1. **Cache-Aside 패턴**:
   - 애플리케이션에서 캐시 확인 → 없으면 DB 조회 → 캐시 저장

2. **TTL 설정**:
   - 메시지 히스토리: 10분 (변경 빈도 높음)
   - 친밀도 레벨: 10분 (변경 빈도 낮음)
   - 토큰 블랙리스트: 토큰 만료 시간

3. **캐시 무효화**:
   - 메시지 저장 시: `@CacheEvict(value = "messageHistory", key = "#chatroomId")`
   - 친밀도 업데이트 시: `@CacheEvict(value = "intimacy", key = "#chatroomId")`

**Redis 설정**:
```yaml
spring:
  data:
    redis:
      host: redis
      port: 6379
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
```

---

### Q21. Aurora RDS → 단일 PostgreSQL + 멀티 스키마로 옮길 때 가장 신경 쓴 리스크는 무엇이었나요?

**답안**:

**주요 리스크**:

1. **데이터 손실 위험**
   - 마이그레이션 중 데이터 불일치
   - 백업/복원 실패

2. **다운타임 발생**
   - 마이그레이션 중 서비스 중단
   - 롤백 시나리오 부재

3. **성능 저하**
   - 단일 인스턴스로 인한 성능 병목
   - 연결 풀 부족

4. **운영 복잡도 증가**
   - EC2에서 직접 DB 관리
   - 백업/모니터링 수동 설정

**대응 전략**:

1. **데이터 손실 방지**:
   - **백업 전략**:
     ```bash
     # RDS 백업
     pg_dump -h <rds-endpoint> -U doran -d dorandoran > rds_backup.dump
     
     # 스키마만 백업 (검증용)
     pg_dump -h <rds-endpoint> -U doran -d dorandoran --schema-only > schema_dump.sql
     ```
   - **검증 절차**:
     - 로컬 환경에서 백업 복원 테스트
     - 데이터 일치성 검증 (레코드 수, 샘플 데이터)

2. **다운타임 최소화**:
   - **점진적 마이그레이션**:
     - 1단계: 새 PostgreSQL 인스턴스 구축
     - 2단계: 데이터 복원 및 검증
     - 3단계: 애플리케이션 연결 변경 (환경변수)
     - 4단계: 트래픽 점진적 전환
   - **롤백 계획**:
     - 환경변수만 변경하여 RDS로 즉시 복귀 가능
     - RDS는 삭제하지 않고 일정 기간 유지

3. **성능 검증**:
   - **부하 테스트**:
     - 동일 트래픽으로 성능 비교
     - 쿼리 응답 시간, 연결 수 모니터링
   - **연결 풀 최적화**:
     - 서비스별 연결 풀 크기 조정
     - PostgreSQL `max_connections` 설정

4. **운영 자동화**:
   - **Docker Compose**: 컨테이너 자동 관리
   - **백업 스크립트**: 주기적 백업 자동화
   - **모니터링**: Prometheus + Grafana로 메트릭 수집

**실제 마이그레이션 과정**:

1. **준비 단계** (1일):
   - RDS 백업 생성
   - 로컬 환경에서 복원 테스트
   - 스키마 검증

2. **마이그레이션 단계** (2시간):
   - 새 PostgreSQL 인스턴스 구축
   - 데이터 복원
   - 애플리케이션 연결 변경
   - 기능 테스트

3. **검증 단계** (1일):
   - 성능 모니터링
   - 에러 로그 확인
   - 사용자 피드백 수집

**결과**:
- 다운타임: 2시간 (계획된 유지보수 시간)
- 데이터 손실: 없음
- 성능: 동일 또는 개선 (인덱스 최적화)
- 비용: 50% 절감

---

### Q22. 다운사이징 해도 괜찮다는 걸 어떻게 "검증"했나요? 어떤 지표를 기준으로 삼았나요?

**답안**:

**검증 지표**:

1. **리소스 사용률** (Aurora RDS 기준):
   - **CPU 사용률**: 평균 15-20% (피크 40%)
   - **메모리 사용률**: 평균 30-40%
   - **연결 수**: 평균 20-30개 (최대 50개)
   - **결론**: 오버프로비저닝 확인

2. **트래픽 분석**:
   - **동시 사용자 수**: 평균 10-20명, 피크 50명
   - **일일 요청 수**: 평균 5,000-10,000건
   - **피크 시간대**: 오후 2-4시
   - **결론**: t3.medium으로 충분

3. **쿼리 성능**:
   - **평균 응답 시간**: 50ms
   - **p95 응답 시간**: 200ms
   - **p99 응답 시간**: 500ms
   - **결론**: 단일 인스턴스로도 충분

**검증 방법**:

1. **부하 테스트**:
   ```bash
   # Apache Bench로 부하 테스트
   ab -n 1000 -c 10 http://api.doran-chat.com/api/chat/rooms
   
   # 결과: 평균 응답 시간 30ms, 99% 요청 성공
   ```

2. **모니터링 데이터 분석**:
   - Prometheus 메트릭으로 1주일간 데이터 수집
   - CPU, 메모리, 연결 수 추세 분석
   - 피크 시간대 리소스 사용률 확인

3. **로컬 환경 테스트**:
   - 동일 스펙(EC2 t3.medium)에서 로컬 테스트
   - 실제 트래픽 수준으로 부하 테스트
   - 성능 비교

**검증 결과**:

| 지표 | Aurora RDS | 단일 PostgreSQL | 결과 |
|------|-----------|----------------|------|
| CPU 사용률 | 15-20% | 20-25% | ✅ 여유 있음 |
| 메모리 사용률 | 30-40% | 40-50% | ✅ 여유 있음 |
| 연결 수 | 20-30개 | 30-40개 | ✅ 여유 있음 (max 100) |
| 평균 응답 시간 | 50ms | 30ms | ✅ 개선 (인덱스 최적화) |
| 비용 | $150-200/월 | $75-100/월 | ✅ 50% 절감 |

**안전 마진**:
- 연결 풀: 60개 사용 / 100개 최대 (40% 여유)
- CPU: 25% 사용 / 100% 최대 (75% 여유)
- 메모리: 50% 사용 / 100% 최대 (50% 여유)

**결론**:
- 현재 트래픽 수준에서는 단일 PostgreSQL로 충분
- 향후 트래픽 증가 시 스케일 업 또는 서비스별 DB 분리 가능

---

### Q23. User ↔ ChatRoom 양방향 + Lombok @Data로 어떤 문제가 발생했나요?

**답안**:

**문제 상황**:

```java
@Entity
@Data  // @ToString, @EqualsAndHashCode, @Getter, @Setter 자동 생성
public class User {
    @OneToMany(mappedBy = "user")
    private List<ChatRoom> chatRooms;
}

@Entity
@Data
public class ChatRoom {
    @ManyToOne
    private User user;
    
    @OneToMany(mappedBy = "chatRoom")
    private List<Message> messages;
}
```

**발생한 문제**:

1. **순환 참조 (Circular Reference)**:
   - `User.toString()` → `chatRooms` 포함 → `ChatRoom.toString()` → `user` 포함 → 무한 루프
   - `User.equals()` → `chatRooms` 비교 → `ChatRoom.equals()` → `user` 비교 → 스택 오버플로우

2. **실제 증상**:
   ```
   java.lang.StackOverflowError
       at com.dorandoran.chat.entity.User.toString()
       at com.dorandoran.chat.entity.ChatRoom.toString()
       at com.dorandoran.chat.entity.User.toString()
       ...
   ```

3. **로그 출력 시**:
   - `log.info("User: {}", user)` 실행 시 스택 오버플로우
   - 디버깅 불가능

**해결 방법**:

```java
@Entity
@ToString(exclude = {"chatRooms"})  // 순환 참조 필드 제외
@EqualsAndHashCode(exclude = {"chatRooms"})
public class User {
    @OneToMany(mappedBy = "user")
    private List<ChatRoom> chatRooms;
}

@Entity
@ToString(exclude = {"user", "chatbot", "messages", "lastMessage"})
public class ChatRoom {
    @ManyToOne
    private User user;
    
    @OneToMany(mappedBy = "chatRoom")
    private List<Message> messages;
}
```

**다른 해결책과 선택 이유**:

1. **@ToString(exclude = ...)** ✅ 선택
   - **장점**: 간단, 명시적
   - **단점**: 필드 추가 시마다 exclude 업데이트 필요

2. **DTO 변환**:
   - **장점**: 엔티티와 DTO 분리, 순환 참조 완전 제거
   - **단점**: 변환 코드 추가 필요, 개발 속도 저하

3. **@JsonIgnore**:
   - **장점**: JSON 직렬화 시 순환 참조 방지
   - **단점**: toString/equals 문제는 해결 안 됨

4. **양방향 관계 제거**:
   - **장점**: 근본적 해결
   - **단점**: 편의성 저하 (user.getChatRooms() 불가)

**선택 이유**:
- 빠른 해결 (코드 변경 최소화)
- 명시적 제어 (어떤 필드를 제외하는지 명확)
- 향후 DTO 변환으로 개선 가능

---

### Q24. @ToString(exclude = ...) 말고 다른 해결책은 어떤 것들이 있고, 왜 그 방법을 선택했나요?

**답안**:

**가능한 해결책**:

1. **@ToString(exclude = ...)** ✅ 선택
   - **구현**: `@ToString(exclude = {"chatRooms"})`
   - **장점**: 간단, 명시적, 즉시 적용 가능
   - **단점**: 필드 추가 시 exclude 업데이트 필요

2. **DTO 변환**:
   - **구현**: 엔티티 → DTO 변환 (MapStruct, ModelMapper)
   - **장점**: 엔티티와 API 응답 분리, 순환 참조 완전 제거, 보안 강화
   - **단점**: 변환 코드 추가, 개발 속도 저하, 유지보수 비용

3. **@JsonIgnore (Jackson)**:
   - **구현**: `@JsonIgnore` 또는 `@JsonManagedReference`/`@JsonBackReference`
   - **장점**: JSON 직렬화 시 순환 참조 방지
   - **단점**: toString/equals 문제는 해결 안 됨, 로깅 시 여전히 문제

4. **양방향 관계 제거 (단방향으로 변경)**:
   - **구현**: `User`에서 `chatRooms` 제거, `ChatRoom`에서만 `user` 참조
   - **장점**: 근본적 해결, 순환 참조 완전 제거
   - **단점**: 편의성 저하 (`user.getChatRooms()` 불가), 쿼리 변경 필요

5. **Lombok @Data 대신 명시적 어노테이션**:
   - **구현**: `@Getter`, `@Setter`만 사용, `toString()`, `equals()` 수동 구현
   - **장점**: 완전한 제어
   - **단점**: 보일러플레이트 코드 증가

6. **Custom toString() 구현**:
   - **구현**: `toString()` 메서드 수동 구현
   - **장점**: 완전한 제어
   - **단점**: 유지보수 비용, Lombok 장점 상실

**선택 이유**:

1. **빠른 해결**:
   - 당시 장애 상황에서 즉시 적용 가능
   - 코드 변경 최소화

2. **명시적 제어**:
   - 어떤 필드를 제외하는지 명확
   - 향후 필드 추가 시 exclude 업데이트로 관리 가능

3. **점진적 개선**:
   - 향후 DTO 변환으로 개선 가능
   - 현재는 MVP 단계이므로 빠른 해결 우선

**향후 개선 계획**:
- DTO 변환 도입 (MapStruct)
- 엔티티는 도메인 모델로만 사용
- API 응답은 DTO로 변환

---

### Q25. 해외 IP에서 HTTP/1.0/0.9로 들어오는 요청을 어떻게 감지했나요? (로그 패턴)

**답안**:

**감지 방법**:

1. **로그 패턴 분석**:
   - Gateway 로그에서 HTTP 프로토콜 버전 확인
   - `HTTP/1.0`, `HTTP/0.9` 등 비정상 프로토콜 버전 감지

2. **에러 로그 패턴**:
   ```
   DecodingException: Failed to decode request
   ProtocolException: Unsupported HTTP version
   ```

3. **Nginx 로그 분석**:
   ```bash
   # Nginx access.log에서 HTTP 버전 확인
   tail -f /var/log/nginx/access.log | grep "HTTP/1.0\|HTTP/0.9"
   
   # 예시 로그
   172.104.24.172 - - [12/Nov/2025:10:23:45 +0000] 
   "GET / HTTP/1.0" 400 0 "-" "-"
   ```

4. **Spring Cloud Gateway 로그**:
   - Gateway는 HTTP/1.0/0.9 요청을 처리하지 못함
   - `400 Bad Request` 또는 `505 HTTP Version Not Supported` 응답
   - 로그에 프로토콜 버전 기록

**실제 감지 사례**:

```
2025-11-13: 61.138.228.132 (13건, 가장 많은 공격)
- HTTP/1.0 프로토콜 혼동
- 제어 문자 주입 시도

2025-11-15: 3.132.23.201 (3건, HTTP/0.9 프로토콜 혼동)
- HTTP/0.9는 1991년 표준, 현재는 사용 안 함
- 공격 시도로 판단

2025-11-19: 45.79.149.214 (10건, 가장 많은 공격)
- RTSP/1.0, SIP/2.0, HTTP/0.9 프로토콜 혼동
- 다양한 프로토콜로 스캔 시도
```

**대응 방법**:

1. **IP 블랙리스트 추가**:
   ```yaml
   gateway:
     security:
       blacklist:
         ips: 61.138.228.132,3.132.23.201,45.79.149.214,...
   ```

2. **Gateway 필터에서 차단**:
   ```java
   // IpBlacklistFilter에서 차단
   if (isBlacklisted(clientIp)) {
       return handleBlockedRequest(exchange, clientIp);
   }
   ```

3. **AWS Security Group에서 차단**:
   - 인바운드 규칙에서 특정 IP 차단
   - Gateway 필터와 이중 방어

**모니터링**:
- Prometheus로 400/505 에러율 추적
- 특정 IP에서 반복적인 실패 요청 시 알림

---

### Q26. IP 차단을 AWS SG에서만 하지 않고, Gateway GlobalFilter로도 구현한 이유가 뭔가요?

**답안**:

**이중 방어 전략**:

1. **AWS Security Group (인프라 레벨)**:
   - **장점**: 네트워크 레벨에서 차단되어 애플리케이션까지 요청이 도달하지 않음
   - **단점**: 설정 변경 시 EC2 재시작 필요, 동적 차단 어려움
   - **용도**: 명확한 공격 IP 영구 차단

2. **Gateway GlobalFilter (애플리케이션 레벨)**:
   - **장점**: 
     - 동적 차단 가능 (런타임에 IP 추가/제거)
     - 설정 파일 변경만으로 즉시 적용 (재시작 불필요)
     - 로그 기반 자동 차단 가능 (향후)
     - 세밀한 제어 (경로별, 메서드별 차단)
   - **단점**: 요청이 Gateway까지 도달 (리소스 소모)
   - **용도**: 실시간 차단, 패턴 기반 차단

**구현 이유**:

1. **운영 유연성**:
   - 공격 IP 발견 시 즉시 차단 (코드 배포 없이 설정만 변경)
   - AWS 콘솔 접근 없이 차단 가능

2. **자동화 가능성**:
   - 향후 로그 분석 기반 자동 차단 구현 가능
   - 특정 패턴(HTTP/0.9, 제어 문자 등) 감지 시 자동 차단

3. **세밀한 제어**:
   - 경로별 차단 (예: `/api/admin/**`만 차단)
   - Actuator 엔드포인트는 차단 제외

4. **이중 방어**:
   - SG 차단 실패 시 Gateway에서 추가 차단
   - 보안 강화

**필터 우선순위**:
- `IpBlacklistFilter`: Order = -100 (가장 먼저 실행)
- `JwtAuthFilter`: Order = 0 (IP 차단 후 인증)

---

### Q27. 필터 우선순위를 어떻게 설정했는지, 그리고 그 이유는요?

**답안**:

**필터 우선순위 설정**:

```java
// IpBlacklistFilter
@Override
public int getOrder() {
    return -100;  // 가장 높은 우선순위
}

// JwtAuthFilter
@Override
public Mono<Void> filter(...) {
    // Order 기본값: 0
}
```

**우선순위 순서**:

1. **IpBlacklistFilter (Order: -100)**
   - 가장 먼저 실행
   - 차단된 IP는 즉시 거부 (403 Forbidden)
   - 다른 필터 실행 안 함 (리소스 절약)

2. **JwtAuthFilter (Order: 0)**
   - IP 차단 통과 후 실행
   - JWT 토큰 검증
   - HMAC 헤더 주입

3. **CorsResponseFilter (Order: 낮은 값)**
   - 응답 시 CORS 헤더 추가

**설정 이유**:

1. **성능 최적화**:
   - 차단된 IP는 즉시 거부하여 불필요한 처리 방지
   - JWT 검증 등 비용이 큰 작업을 차단된 IP에 대해 수행하지 않음

2. **보안 강화**:
   - 공격 IP는 가능한 한 빨리 차단
   - 인증 로직 노출 최소화

3. **리소스 절약**:
   - 차단된 IP의 요청은 Gateway 리소스 소모 최소화
   - Auth Service 호출 없이 차단

**실제 동작**:

```
요청 → IpBlacklistFilter (-100) → 차단? → 403 반환 (종료)
                              ↓ 통과
                         JwtAuthFilter (0) → 검증 → 다음 필터
```

---

## 5. 인프라 / 배포 / 운영

### Q28. 왜 t3.medium 한 대에 모든 서비스를 올리는 단일 EC2 전략을 택했나요? 장단점을 말해보세요.

**답안**:

**선택 이유**:

1. **비용 최적화**:
   - 초기 단계에서 비용 최소화
   - t3.medium (2 vCPU, 4GB RAM)으로 충분한 트래픽 처리 가능
   - 여러 인스턴스로 분리 시 비용 증가

2. **운영 단순성**:
   - 단일 인스턴스로 관리 포인트 단일화
   - 배포, 모니터링, 로그 수집 단순
   - 네트워크 구성 단순 (서비스 간 통신이 localhost)

3. **트래픽 수준**:
   - 현재 트래픽: 평균 10-20명 동시 사용자, 피크 50명
   - t3.medium으로 충분한 처리 능력

4. **개발 속도**:
   - 빠른 프로토타이핑 및 배포
   - 향후 스케일 아웃 시 마이그레이션 가능

**장점**:

1. **비용 효율성**:
   - 월 비용: 약 $30-40 (t3.medium)
   - 여러 인스턴스 대비 50% 이상 절감

2. **운영 단순성**:
   - 단일 인스턴스 관리
   - 배포 스크립트 단순
   - 모니터링 포인트 단일화

3. **서비스 간 통신**:
   - localhost 통신으로 지연 시간 최소화
   - 네트워크 비용 없음

4. **개발/테스트 용이성**:
   - 로컬 환경과 유사한 구조
   - 디버깅 용이

**단점**:

1. **단일 장애점 (SPOF)**:
   - EC2 인스턴스 장애 시 전체 서비스 중단
   - 하드웨어 장애 시 복구 시간 필요

2. **리소스 경쟁**:
   - 모든 서비스가 동일한 CPU/메모리 공유
   - 한 서비스의 부하가 다른 서비스에 영향

3. **스케일링 제한**:
   - 수직 스케일링만 가능 (인스턴스 크기 증가)
   - 수평 스케일링 불가 (서비스별 독립 스케일링 불가)

4. **배포 영향**:
   - 한 서비스 배포 시 전체 재시작 가능성
   - 무중단 배포 어려움

**완화 전략**:

1. **백업/복원 전략**:
   - 주기적 스냅샷 생성
   - 빠른 복구 가능

2. **모니터링**:
   - Prometheus + Grafana로 리소스 사용률 추적
   - 80% 이상 사용 시 알림

3. **향후 전략**:
   - 트래픽 증가 시 서비스별 독립 인스턴스로 분리
   - 또는 ECS/EKS로 컨테이너 오케스트레이션

**결론**:
- 현재 트래픽 수준에서는 단일 EC2 전략이 최적
- 비용 대비 효율성과 운영 단순성 확보
- 향후 트래픽 증가 시 점진적 분리 가능

---

### Q29. Docker Compose 구조를 간단히 설명해보세요. (서비스, 네트워크, 볼륨 구분)

**답안**:

**Docker Compose 구조**:

```yaml
services:
  # 데이터베이스
  shared-db:
    image: postgres:17-alpine
    volumes:
      - shared_db_data:/var/lib/postgresql/data
    ports: ["5432:5432"]
  
  # 캐시
  redis:
    image: redis:7-alpine
    volumes:
      - redis_data:/data
    ports: ["6379:6379"]
  
  # 마이크로서비스
  api-gateway:
    build: docker/Dockerfile.gateway
    ports: ["8080:8080"]
    depends_on: [auth-service, user-service, chat-service]
  
  auth-service:
    build: docker/Dockerfile.auth
    ports: ["8081:8081"]
    depends_on: [shared-db, redis]
  
  # ... (user, chat, store, batch 서비스)
  
  # 모니터링 스택
  prometheus:
    image: prom/prometheus:latest
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus_data:/prometheus
    ports: ["9090:9090"]
  
  grafana:
    image: grafana/grafana:latest
    volumes:
      - grafana_data:/var/lib/grafana
    ports: ["3000:3000"]
  
  loki:
    image: grafana/loki:latest
    volumes:
      - loki_data:/loki
    ports: ["3100:3100"]

volumes:
  shared_db_data:    # PostgreSQL 데이터 영구 저장
  redis_data:        # Redis 데이터 영구 저장
  prometheus_data:   # Prometheus 메트릭 데이터
  grafana_data:      # Grafana 대시보드 설정
  loki_data:         # Loki 로그 데이터

networks:
  default:
    name: dorandoran-msa-network
```

**서비스 구성**:

1. **데이터 레이어**:
   - `shared-db`: PostgreSQL (5개 스키마)
   - `redis`: 캐싱 및 세션 관리

2. **애플리케이션 레이어**:
   - `api-gateway`: Gateway Service
   - `auth-service`: Auth Service
   - `user-service`: User Service
   - `chat-service`: Chat Service
   - `store-service`: Store Service
   - `batch-service`: Batch Service

3. **모니터링 레이어**:
   - `prometheus`: 메트릭 수집
   - `grafana`: 대시보드 시각화
   - `loki`: 로그 수집
   - `promtail`: 로그 수집 에이전트
   - `postgres-exporter`: PostgreSQL 메트릭
   - `redis-exporter`: Redis 메트릭
   - `alertmanager`: 알림 관리

**네트워크**:

- **단일 네트워크**: `dorandoran-msa-network`
- **서비스 간 통신**: 컨테이너 이름으로 통신
  - 예: `http://dorandoran-auth:8081`
- **포트 노출**: 외부 접근이 필요한 서비스만 포트 노출
  - Gateway: 8080
  - 각 서비스: 8081-8085
  - 모니터링: 3000, 9090, 3100

**볼륨**:

1. **데이터 영구 저장**:
   - `shared_db_data`: PostgreSQL 데이터
   - `redis_data`: Redis 데이터
   - 컨테이너 재생성 시에도 데이터 유지

2. **모니터링 데이터**:
   - `prometheus_data`: 메트릭 데이터 (30일 보관)
   - `grafana_data`: 대시보드 설정
   - `loki_data`: 로그 데이터

3. **설정 파일 마운트**:
   - `./prometheus.yml`: Prometheus 설정
   - `./loki-config.yml`: Loki 설정
   - 설정 변경 시 컨테이너 재시작으로 적용

**의존성 관리**:

```yaml
auth-service:
  depends_on:
    shared-db:
      condition: service_healthy  # 헬스체크 통과 후 시작
    redis:
      condition: service_started   # 시작 후 시작
```

**장점**:
- 단일 명령어로 전체 스택 실행: `docker-compose up`
- 서비스 간 의존성 자동 관리
- 볼륨으로 데이터 영구 저장
- 네트워크 격리로 보안 강화

---

### Q30. Nginx 리버스 프록시 설정에서 SSE/WebSocket 지원을 위해 어떤 설정이 필요했나요?

**답안**:

**SSE (Server-Sent Events) 지원 설정**:

```nginx
server {
    listen 443 ssl http2;
    server_name api.doran-chat.com;
    
    location /api/chat/stream {
        proxy_pass http://localhost:8080;
        
        # SSE를 위한 핵심 설정
        proxy_read_timeout 600s;        # 10분 타임아웃 (SSE는 long-running)
        proxy_send_timeout 600s;
        proxy_buffering off;             # 버퍼링 비활성화 (실시간 스트리밍)
        proxy_cache off;                 # 캐싱 비활성화
        
        # 헤더 설정
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # SSE 전용 헤더
        proxy_set_header Connection '';
        proxy_http_version 1.1;
        chunked_transfer_encoding off;
        
        # CORS 헤더 (필요시)
        add_header Cache-Control "no-cache";
        add_header X-Accel-Buffering "no";  # Nginx 버퍼링 비활성화
    }
    
    # 일반 API 요청
    location /api {
        proxy_pass http://localhost:8080;
        proxy_read_timeout 45s;          # 일반 요청은 45초
        proxy_buffering on;              # 일반 요청은 버퍼링 활성화
    }
}
```

**WebSocket 지원 설정**:

```nginx
server {
    listen 443 ssl http2;
    server_name chat.doran-chat.com;
    
    location /ws {
        proxy_pass http://localhost:8083;
        
        # WebSocket 업그레이드
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        
        # 타임아웃 설정
        proxy_read_timeout 3600s;       # 1시간 (WebSocket은 더 길게)
        proxy_send_timeout 3600s;
        
        # 헤더 설정
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

**핵심 설정 설명**:

1. **`proxy_read_timeout 600s`**:
   - SSE는 long-running 연결이므로 타임아웃을 길게 설정
   - 기본값(60초)보다 훨씬 길게 설정

2. **`proxy_buffering off`**:
   - 버퍼링을 끄면 실시간으로 데이터 전송
   - 버퍼링이 켜져 있으면 청크가 모여서 전송되어 지연 발생

3. **`proxy_cache off`**:
   - SSE는 캐싱하면 안 됨 (실시간 데이터)
   - 캐싱 활성화 시 오래된 데이터 전송 가능

4. **`Connection ''` 및 `proxy_http_version 1.1`**:
   - HTTP/1.1로 업그레이드하여 keep-alive 지원
   - SSE는 HTTP/1.1 이상 필요

5. **`X-Accel-Buffering: no`**:
   - Nginx의 내부 버퍼링 비활성화
   - 애플리케이션에서 설정한 헤더

**WebSocket vs SSE 차이**:

| 설정 | WebSocket | SSE |
|------|-----------|-----|
| Upgrade 헤더 | 필요 (`Upgrade: websocket`) | 불필요 |
| Connection 헤더 | `Connection: upgrade` | `Connection: ''` |
| 타임아웃 | 3600s (1시간) | 600s (10분) |
| 양방향 | 지원 | 단방향 (서버→클라이언트) |

**문제 해결**:

1. **연결 끊김 문제**:
   - 타임아웃을 길게 설정하여 해결
   - `proxy_read_timeout` 증가

2. **지연 문제**:
   - `proxy_buffering off`로 실시간 전송
   - `X-Accel-Buffering: no` 헤더 추가

3. **캐싱 문제**:
   - `proxy_cache off`로 캐싱 비활성화
   - `Cache-Control: no-cache` 헤더 추가

---

### Q31. 장애가 발생했을 때, 로그와 메트릭을 어떤 순서로 확인하나요? 본인만의 점검 플로우가 있나요?

**답안**:

**점검 플로우 (체계적 접근)**:

**1단계: 증상 파악 (1-2분)**
```
1. 사용자 리포트 확인
   - 어떤 기능이 안 되는가?
   - 에러 메시지는?
   - 언제부터 발생?

2. 서비스 상태 확인
   - 컨테이너 상태: docker ps
   - 헬스체크: curl http://localhost:8080/actuator/health
   - Gateway 응답: curl http://localhost:8080/api/health
```

**2단계: 메트릭 확인 (2-3분)**
```
1. Grafana 대시보드 확인
   - 서비스별 헬스 상태
   - HTTP 에러율 (4xx, 5xx)
   - 응답 시간 (p95, p99)
   - JVM 메모리 사용률

2. 데이터베이스 메트릭
   - PostgreSQL 연결 수
   - 쿼리 응답 시간
   - 활성 연결 수

3. Redis 메트릭
   - 메모리 사용률
   - 연결 수
   - 명령어 통계

4. 리소스 사용률
   - CPU 사용률
   - 메모리 사용률
   - 디스크 I/O
```

**3단계: 로그 확인 (3-5분)**
```
1. 최근 에러 로그
   docker logs --tail 100 --since 10m dorandoran-chat
   
2. 특정 패턴 검색
   docker logs dorandoran-chat | grep -i "error\|exception\|timeout"
   
3. Loki에서 로그 쿼리
   {service="chat-service"} |= "error"
   {service="gateway"} |= "503"
```

**4단계: 원인 분석 (5-10분)**
```
1. 메트릭과 로그 상관관계 분석
   - 특정 시간대에 에러율 증가?
   - 특정 엔드포인트에서만 발생?
   - 리소스 부족과 연관?

2. 스택 트레이스 분석
   - 예외 타입 확인
   - 발생 위치 확인
   - 원인 파악

3. 의존성 확인
   - 데이터베이스 연결 상태
   - Redis 연결 상태
   - 외부 API (OpenAI) 상태
```

**5단계: 해결 및 검증 (10-15분)**
```
1. 임시 조치
   - 컨테이너 재시작
   - 설정 변경
   - 트래픽 제한

2. 근본 원인 해결
   - 코드 수정
   - 설정 최적화
   - 리소스 증설

3. 검증
   - 기능 테스트
   - 메트릭 모니터링
   - 로그 확인
```

**실제 사례: 커넥션 풀 고갈 (2025-10-14)**

**1단계: 증상 파악**
- 사용자 리포트: "API 응답 안 됨"
- 컨테이너 상태: `unhealthy`
- 헬스체크: 500 Internal Server Error

**2단계: 메트릭 확인**
- Grafana: HikariCP 연결 풀 `active=10, idle=0`
- HTTP 에러율: 100% (모든 요청 실패)
- 응답 시간: 30초 타임아웃

**3단계: 로그 확인**
```
HikariPool-1 - Connection is not available, request timed out after 30000ms
Connection leak detection triggered
```

**4단계: 원인 분석**
- SSE 연결 + open-in-view 조합
- DB 연결이 SSE 연결 동안 점유
- 연결 풀 고갈

**5단계: 해결**
- `open-in-view=false` 설정
- 연결 풀 크기 증가 (10 → 20)
- 검증: 헬스체크 정상, 메트릭 정상

**체크리스트 (빠른 진단)**:

```
□ 컨테이너 상태 확인
□ 헬스체크 응답 확인
□ 에러 로그 최근 100줄 확인
□ 메트릭 대시보드 확인 (에러율, 응답 시간)
□ 데이터베이스 연결 상태 확인
□ Redis 연결 상태 확인
□ 리소스 사용률 확인 (CPU, 메모리)
□ 특정 패턴 로그 검색
```

**자동화된 모니터링**:
- Prometheus AlertManager로 자동 알림
- 에러율 10% 초과 시 알림
- 응답 시간 p95가 1초 초과 시 알림
- 연결 풀 사용률 80% 초과 시 알림

---

## 6. 모니터링 & Observability

### Q32. Spring Actuator + Micrometer + Prometheus + Grafana + Loki 조합을 선택한 이유는?

**답안**:

**기술 스택 선택 이유**:

1. **Spring Actuator**:
   - **이유**: Spring Boot 기본 제공, 설정 간단
   - **기능**: `/actuator/health`, `/actuator/metrics`, `/actuator/prometheus` 엔드포인트
   - **장점**: 코드 변경 없이 메트릭 수집 가능

2. **Micrometer**:
   - **이유**: Spring Actuator와 통합, 벤더 중립적
   - **기능**: JVM, HTTP, 데이터베이스 메트릭 자동 수집
   - **장점**: Prometheus 외 다른 백엔드로 전환 가능 (유연성)

3. **Prometheus**:
   - **이유**: 
     - 시계열 데이터베이스로 메트릭 저장
     - Pull 모델로 안정적 수집
     - 쿼리 언어 (PromQL)로 강력한 분석
     - 오픈소스, 커뮤니티 활발
   - **장점**: 
     - 30일간 메트릭 보관
     - 알림 규칙 설정 가능
     - 확장성 우수

4. **Grafana**:
   - **이유**:
     - Prometheus와 완벽 통합
     - 강력한 시각화 기능
     - 대시보드 템플릿 풍부
     - 알림 설정 가능
   - **장점**:
     - 직관적인 UI
     - 메트릭과 로그 통합 (Loki 연동)
     - 커스텀 대시보드 생성 용이

5. **Loki**:
   - **이유**:
     - Grafana와 통합 (단일 UI)
     - 로그 수집 및 쿼리
     - Prometheus와 유사한 쿼리 언어 (LogQL)
   - **장점**:
     - 메트릭과 로그를 한 곳에서 확인
     - 인덱싱으로 빠른 검색
     - 저장 공간 효율적

**통합 아키텍처**:

```
애플리케이션 (Spring Boot)
  ↓ Actuator + Micrometer
/actuator/prometheus 엔드포인트
  ↓ Pull (15초마다)
Prometheus (메트릭 수집 및 저장)
  ↓ 쿼리
Grafana (시각화 및 알림)

애플리케이션 로그
  ↓ Promtail (수집)
Loki (로그 저장)
  ↓ 쿼리
Grafana (로그 시각화)
```

**선택 기준**:

1. **오픈소스**: 비용 없음, 커뮤니티 지원
2. **통합성**: Grafana에서 메트릭과 로그 통합 확인
3. **확장성**: 향후 서비스 추가 시 쉽게 확장
4. **표준화**: 업계 표준 도구로 학습 자료 풍부

**대안과 비교**:

| 도구 | 장점 | 단점 | 선택 이유 |
|------|------|------|----------|
| **Datadog** | 강력한 기능, SaaS | 비용 ($15/호스트) | 비용 문제 |
| **New Relic** | APM 기능 강력 | 비용, 복잡도 | 오버킬 |
| **ELK Stack** | 로그 분석 강력 | 리소스 많이 사용 | 리소스 제약 |
| **Prometheus + Grafana** | 오픈소스, 가벼움 | 설정 필요 | ✅ 선택 |

**실제 효과**:
- 장애 발생 시 5분 내 원인 파악 가능
- 메트릭 기반 성능 최적화
- 알림으로 사전 예방

---

### Q33. 서비스당 어떤 메트릭을 가장 중요하게 봤나요? (예: 레이턴시, 에러율, 커넥션 수 등)

**답안**:

**서비스별 핵심 메트릭**:

**1. Gateway Service**:
- **HTTP 요청 수** (`http_server_requests_seconds_count`):
  - 전체 트래픽 파악
  - 엔드포인트별 요청 수
- **에러율** (`http_server_requests_seconds_count{status=~"5.."}`):
  - 5xx 에러 비율 (목표: < 1%)
  - 4xx 에러 비율 (인증 실패 등)
- **응답 시간** (`http_server_requests_seconds{quantile="0.95"}`):
  - p95 응답 시간 (목표: < 500ms)
  - p99 응답 시간 (목표: < 1s)
- **JWT 검증 실패율**:
  - 커스텀 메트릭으로 추적
  - 인증 문제 조기 발견

**2. Chat Service**:
- **SSE 연결 수**:
  - 활성 SSE 연결 수
  - 연결 풀과 연관성 확인
- **AI API 호출 지연시간**:
  - OpenAI API 응답 시간
  - 토큰 사용량
- **메시지 처리 시간**:
  - 메시지 저장부터 AI 응답까지 시간
- **에러율**:
  - AI API 실패율
  - 메시지 저장 실패율

**3. Auth Service**:
- **토큰 검증 성공/실패율**:
  - JWT 검증 성능
  - 블랙리스트 조회 성능
- **로그인 시도 수**:
  - 정상 로그인 vs 실패 로그인
  - 보안 이슈 조기 발견
- **Redis 연결 상태**:
  - 토큰 블랙리스트 조회 성능

**4. User Service**:
- **사용자 조회 응답 시간**:
  - Redis 캐시 히트율과 연관
  - DB 쿼리 성능
- **캐시 히트율**:
  - Redis 캐시 효과 측정
  - 목표: > 70%

**5. 공통 메트릭 (모든 서비스)**:
- **JVM 메모리** (`jvm_memory_used_bytes{area="heap"}`):
  - 힙 메모리 사용률 (목표: < 80%)
  - GC 시간 (`jvm_gc_pause_seconds`)
- **데이터베이스 연결 풀**:
  - `hikaricp_connections_active`: 활성 연결 수
  - `hikaricp_connections_idle`: 유휴 연결 수
  - `hikaricp_connections_pending`: 대기 중인 요청 수
- **HTTP 요청 메트릭**:
  - 요청 수, 에러율, 응답 시간

**데이터베이스 메트릭**:
- **연결 수** (`pg_stat_activity_count`):
  - 총 연결 수 (목표: < 80% of max_connections)
  - 활성 연결 수
- **쿼리 성능** (`pg_stat_statements`):
  - 느린 쿼리 (1초 이상)
  - 쿼리 실행 횟수
- **트랜잭션 수**:
  - 초당 트랜잭션 수

**Redis 메트릭**:
- **메모리 사용률** (`redis_memory_used_bytes`):
  - 목표: < 80%
- **명령어 통계**:
  - 초당 명령어 수
  - 캐시 히트율

**알림 임계값**:

```yaml
# Prometheus Alert Rules
- alert: HighErrorRate
  expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.1
  # 에러율 10% 초과 시 알림

- alert: HighLatency
  expr: http_server_requests_seconds{quantile="0.95"} > 1
  # p95 응답 시간 1초 초과 시 알림

- alert: ConnectionPoolExhaustion
  expr: hikaricp_connections_active / hikaricp_connections_max > 0.8
  # 연결 풀 사용률 80% 초과 시 알림

- alert: HighMemoryUsage
  expr: jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.8
  # 힙 메모리 사용률 80% 초과 시 알림
```

**모니터링 우선순위**:

1. **즉시 확인** (장애 시):
   - 에러율
   - 연결 풀 상태
   - 서비스 헬스

2. **일일 확인**:
   - 응답 시간 추세
   - 리소스 사용률
   - 캐시 히트율

3. **주간 확인**:
   - 성능 추세 분석
   - 용량 계획
   - 최적화 포인트

---

### Q34. 로그는 어떤 포맷(JSON 스키마)으로 남겼나요? 필드 설계를 직접 했다면 어떤 기준으로 잡았는지 설명해보세요.

**답안**:

**로그 포맷**: JSON (구조화된 로깅)

**로그 스키마 설계**:

```json
{
  "timestamp": "2025-01-15T10:23:45.123Z",
  "level": "INFO",
  "service": "chat-service",
  "thread": "http-nio-8083-exec-1",
  "logger": "com.dorandoran.chat.controller.ChatController",
  "message": "메시지 저장 완료",
  "context": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "chatroomId": "660e8400-e29b-41d4-a716-446655440000",
    "messageId": "770e8400-e29b-41d4-a716-446655440000"
  },
  "exception": null,
  "traceId": "abc123def456",
  "spanId": "def456"
}
```

**필드 설계 기준**:

1. **필수 필드**:
   - `timestamp`: 로그 발생 시간 (ISO 8601 형식)
   - `level`: 로그 레벨 (DEBUG, INFO, WARN, ERROR)
   - `service`: 서비스 이름 (필터링 용이)
   - `message`: 로그 메시지

2. **컨텍스트 필드**:
   - `context`: 비즈니스 컨텍스트 (userId, chatroomId 등)
   - **설계 이유**: 
     - 특정 사용자/채팅방 관련 로그만 필터링 가능
     - 문제 발생 시 빠른 추적
   - **예시**:
     ```json
     "context": {
       "userId": "...",
       "chatroomId": "...",
       "operation": "sendMessage"
     }
     ```

3. **추적 필드** (분산 추적):
   - `traceId`: 요청 추적 ID (전체 요청 추적)
   - `spanId`: 스팬 ID (서비스 내 작업 추적)
   - **설계 이유**: 
     - 마이크로서비스 간 요청 추적
     - Gateway → Auth → User → Chat 흐름 추적

4. **예외 필드**:
   - `exception`: 예외 정보 (에러 발생 시)
   - **구조**:
     ```json
     "exception": {
       "type": "java.sql.SQLException",
       "message": "Connection timeout",
       "stackTrace": "..."
     }
     ```

5. **성능 필드** (선택적):
   - `duration`: 처리 시간 (밀리초)
   - `requestSize`: 요청 크기
   - `responseSize`: 응답 크기

**설정 예시** (Logback):

```xml
<configuration>
  <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
      <providers>
        <timestamp/>
        <logLevel/>
        <loggerName/>
        <message/>
        <mdc/>
        <stackTrace/>
      </providers>
    </encoder>
  </appender>
  
  <root level="INFO">
    <appender-ref ref="JSON" />
  </root>
</configuration>
```

**필드 선택 기준**:

1. **검색 가능성**:
   - `service`, `level`로 빠른 필터링
   - `context.userId`로 사용자별 로그 조회

2. **분석 용이성**:
   - JSON 형식으로 파싱 용이
   - Loki에서 LogQL로 쿼리 가능

3. **저장 효율성**:
   - 불필요한 필드 제거
   - 스택 트레이스는 에러 시만 포함

4. **보안**:
   - 민감 정보(비밀번호, 토큰) 제외
   - PII 마스킹 (이메일 등)

**실제 사용 예시**:

```java
// 구조화된 로깅
log.info("메시지 저장 완료", 
    Map.of(
        "userId", userId.toString(),
        "chatroomId", chatroomId.toString(),
        "messageId", messageId.toString()
    )
);

// Loki 쿼리
{service="chat-service"} 
  |= "메시지 저장 완료"
  | json
  | context_userId = "550e8400-..."
```

**Loki 인덱싱 전략**:
- 인덱스: `service`, `level`, `timestamp`
- 라벨: `service`, `level`
- 쿼리 성능 최적화

---

### Q35. Alerting(알람)은 어떤 조건으로 걸어두었는지, 실제로 알람이 울렸던 사례가 있었는지?

**답안**:

**알람 규칙 설정** (Prometheus AlertManager):

```yaml
groups:
  - name: service_alerts
    rules:
      # 1. 서비스 다운
      - alert: ServiceDown
        expr: up{job=~"chat-service|auth-service|user-service"} == 0
        for: 1m
        annotations:
          summary: "{{ $labels.job }} 서비스가 다운되었습니다"
          description: "서비스가 1분 이상 응답하지 않습니다"
      
      # 2. 높은 에러율
      - alert: HighErrorRate
        expr: |
          rate(http_server_requests_seconds_count{status=~"5.."}[5m]) 
          / 
          rate(http_server_requests_seconds_count[5m]) > 0.1
        for: 5m
        annotations:
          summary: "{{ $labels.job }} 에러율이 높습니다"
          description: "에러율이 10%를 초과했습니다"
      
      # 3. 높은 응답 시간
      - alert: HighLatency
        expr: |
          http_server_requests_seconds{quantile="0.95"} > 1
        for: 5m
        annotations:
          summary: "{{ $labels.job }} 응답 시간이 느립니다"
          description: "p95 응답 시간이 1초를 초과했습니다"
      
      # 4. 연결 풀 고갈
      - alert: ConnectionPoolExhaustion
        expr: |
          (hikaricp_connections_active / hikaricp_connections_max) > 0.8
        for: 2m
        annotations:
          summary: "{{ $labels.job }} 연결 풀이 부족합니다"
          description: "연결 풀 사용률이 80%를 초과했습니다"
      
      # 5. 높은 메모리 사용률
      - alert: HighMemoryUsage
        expr: |
          (jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) > 0.8
        for: 5m
        annotations:
          summary: "{{ $labels.job }} 메모리 사용률이 높습니다"
          description: "힙 메모리 사용률이 80%를 초과했습니다"
      
      # 6. 데이터베이스 연결 수 초과
      - alert: HighDatabaseConnections
        expr: |
          pg_stat_activity_count / pg_settings_max_connections > 0.8
        for: 5m
        annotations:
          summary: "PostgreSQL 연결 수가 많습니다"
          description: "데이터베이스 연결 수가 80%를 초과했습니다"
      
      # 7. Redis 메모리 부족
      - alert: RedisMemoryHigh
        expr: |
          (redis_memory_used_bytes / redis_memory_max_bytes) > 0.8
        for: 5m
        annotations:
          summary: "Redis 메모리 사용률이 높습니다"
          description: "Redis 메모리 사용률이 80%를 초과했습니다"
```

**실제 알람 사례**:

**사례 1: 연결 풀 고갈 (2025-10-14)**

**알람 발생**:
```
Alert: ConnectionPoolExhaustion
Service: chat-service
Time: 2025-10-14 15:20
Message: 연결 풀 사용률이 80%를 초과했습니다 (100%)
```

**대응 과정**:
1. 알람 수신 (15:20)
2. Grafana 대시보드 확인 → 연결 풀 100% 사용
3. 로그 확인 → SSE + open-in-view 문제 발견
4. 해결: `open-in-view=false` 설정
5. 검증: 연결 풀 정상화 확인

**사례 2: 높은 에러율 (2025-11-12)**

**알람 발생**:
```
Alert: HighErrorRate
Service: gateway
Time: 2025-11-12 10:15
Message: 에러율이 10%를 초과했습니다 (15%)
```

**대응 과정**:
1. 알람 수신 (10:15)
2. 로그 확인 → HTTP/0.9 프로토콜 혼동 에러 다수
3. IP 분석 → 해외 IP에서 공격 시도
4. 해결: IP 블랙리스트 추가
5. 검증: 에러율 정상화

**사례 3: 높은 응답 시간 (2025-11-15)**

**알람 발생**:
```
Alert: HighLatency
Service: chat-service
Time: 2025-11-15 14:30
Message: p95 응답 시간이 1초를 초과했습니다 (2.5초)
```

**대응 과정**:
1. 알람 수신 (14:30)
2. 메트릭 확인 → OpenAI API 응답 시간 증가
3. 원인: OpenAI API 일시적 지연
4. 해결: 재시도 로직 강화
5. 검증: 응답 시간 정상화

**알람 채널**:

1. **이메일** (기본):
   - 즉시 알림
   - 상세 정보 포함

2. **Slack** (향후):
   - 팀 채널에 알림
   - 빠른 공유 및 대응

**알람 관리**:

1. **중복 제거**:
   - AlertManager가 동일 알람 중복 제거
   - 그룹화하여 관련 알람 통합

2. **알람 억제**:
   - 유지보수 시간대 알람 억제
   - 관련 알람 그룹화

3. **알람 심각도**:
   - Critical: 서비스 다운
   - Warning: 성능 저하
   - Info: 정보성 알람

**알람 효과**:
- 장애 조기 발견 (평균 5분 내)
- 사전 예방 (리소스 부족 사전 감지)
- 빠른 대응 (알람 기반 즉시 조치)

---

## 7. LLM / 멀티 에이전트 아키텍처

### Q36. 단일 LLM 콜이 아니라 멀티 에이전트 구조를 택한 이유를, 사용자 경험 관점에서 설명해보세요.

**답안**:

**사용자 경험 개선**:

1. **점진적 피드백 (Progressive Feedback)**:
   - **단일 LLM**: 사용자 메시지 → 긴 대기 → 최종 응답만 받음
   - **멀티 에이전트**: 
     - 즉시 친밀도 분석 결과 표시 (1-2초)
     - 실시간 대화 스트리밍 (토큰 단위)
     - 어휘 추출 결과 표시 (대화 완료 후)
   - **효과**: 사용자가 기다리는 동안 중간 결과를 보며 진행 상황 파악

2. **다양한 학습 지원**:
   - **친밀도 분석**: 실시간 교정 및 피드백
   - **어휘 추출**: 어려운 단어 설명 및 번역
   - **대화 생성**: 자연스러운 대화 연습
   - **효과**: 단순 대화가 아닌 종합적인 학습 경험

3. **응답 속도 최적화**:
   - **병렬 처리**: IntimacyAgent와 ConversationAgent 동시 실행
   - **스트리밍**: 대화 응답을 실시간으로 전송
   - **효과**: 전체 응답 시간 단축 (순차 처리 대비 30-40% 단축)

4. **맥락 유지**:
   - 각 에이전트가 독립적으로 실행되지만, 공통 컨텍스트(채팅방, 친밀도 레벨) 공유
   - **효과**: 일관된 학습 경험 제공

**비즈니스 가치**:

- **사용자 만족도**: 빠른 피드백으로 학습 효과 향상
- **재방문율**: 다양한 기능으로 학습 동기 유지
- **차별화**: 단순 챗봇이 아닌 종합 학습 플랫폼

---

### Q37. IntimacyAgent, VocabularyAgent, ConversationAgent, TranslationAgent 각각의 역할을 한 줄로 정리해보세요.

**답안**:

1. **IntimacyAgent**: 사용자 메시지의 친밀도 레벨(1-3)을 감지하고, 컨셉에 맞지 않는 표현을 교정하여 실시간 피드백을 제공합니다.

2. **VocabularyAgent**: 챗봇 응답에서 어려운 단어를 추출하고, 로마자 표기, 한국어 설명, 영어 번역을 포함한 상세 설명을 생성합니다.

3. **ConversationAgent**: 사용자 메시지에 대한 자연스러운 대화 응답을 생성하며, 친밀도 레벨과 컨셉에 맞는 톤으로 실시간 스트리밍합니다.

4. **SummarizerAgent**: 최근 20개 메시지를 요약하고, 키워드를 추출하여 학습 진척도를 추적합니다.

**참고**: TranslationAgent는 현재 VocabularyAgent에 통합되어 별도 에이전트가 아닙니다.

---

### Q38. Phase 1 병렬 호출, Phase 2 순차 호출 구조를 코드/스레드 관점에서 어떻게 구현했나요?

**답안**:

**구현 방식** (Reactor Mono/Flux):

```java
// Phase 1: 병렬 실행
Mono<IntimacyAgentResponse> intimacyMono = intimacyAgent.analyze(chatroomId, content)
    .doOnNext(resp -> {
        // SSE로 즉시 전송
        sseManager.send(chatroomId, "intimacy_analysis", data);
        updateIntimacyProgress(chatroomId, userId, resp);
    })
    .cache(); // 결과 캐싱

// 즉시 구독하여 병렬 실행 시작
intimacyMono.subscribe();

// ConversationAgent는 독립적으로 실행
conversationAgent.generateResponse(chatroomId, content, usageHolder)
    .collectList()
    .doOnSuccess(chunks -> {
        String fullResponse = String.join("", chunks);
        
        // Phase 2: 순차 실행 (IntimacyAgent 결과 수집 후 VocabularyAgent 실행)
        intimacyMono
            .doOnNext(intimacyResp -> {
                // VocabularyAgent 실행 (IntimacyAgent 결과 필요)
                vocabularyAgent.extractDifficultWords(fullResponse, chatroomId)
                    .doOnNext(vocabResp -> {
                        // 메시지 저장 및 SSE 전송
                        Message botMessage = chatService.sendMessage(...);
                        sseManager.send(chatroomId, "conversation_complete", payload);
                    })
                    .subscribe();
            })
            .subscribe();
    })
    .subscribe();
```

**스레드 관점**:

1. **Phase 1 (병렬)**:
   - **IntimacyAgent**: 별도 스레드에서 실행 (Reactor Scheduler)
   - **ConversationAgent**: 별도 스레드에서 실행
   - **결과**: 두 에이전트가 동시에 실행되어 전체 시간 단축

2. **Phase 2 (순차)**:
   - **IntimacyAgent 결과 대기**: `intimacyMono.doOnNext()`로 결과 수집
   - **VocabularyAgent 실행**: IntimacyAgent 결과를 받은 후 실행
   - **결과**: 의존성 있는 작업은 순차 실행

**Reactor의 비동기 처리**:

- **논블로킹 I/O**: HTTP 요청이 블로킹되지 않음
- **백프레셔**: 다운스트림이 느리면 업스트림 속도 조절
- **에러 격리**: 한 에이전트 실패해도 다른 에이전트 계속 실행

**실제 실행 흐름**:

```
시간축:
0초: IntimacyAgent 시작 (스레드 1)
0초: ConversationAgent 시작 (스레드 2)
2초: IntimacyAgent 완료 → SSE 전송
5초: ConversationAgent 완료 → IntimacyAgent 결과 대기
5초: VocabularyAgent 시작 (스레드 3)
7초: VocabularyAgent 완료 → SSE 전송
```

**성능 효과**:
- 순차 처리 시: 2초 + 5초 + 2초 = 9초
- 병렬 처리 시: max(2초, 5초) + 2초 = 7초 (약 22% 단축)

---

### Q39. OpenAI Streaming API를 어떻게 SSE와 연결했는지, 스트림 끊김이나 재시도 처리는 어떻게 했는지?

**답안**:

**연결 구조**:

```java
// ConversationAgent
public Flux<String> generateResponse(UUID chatroomId, String userMessage, ...) {
    return openAIClient.streamRawCompletionWithHistory(systemPrompt, messageHistory, userMessage, chatroomId)
        .map(raw -> {
            // OpenAI 스트림에서 텍스트 추출
            return extractText(raw);
        })
        .doOnNext(chunk -> {
            // 각 청크를 SSE로 즉시 전송
            sseManager.send(chatroomId, "conversation_chunk", chunk);
        })
        .doOnError(error -> {
            // 에러 발생 시 SSE로 에러 이벤트 전송
            sseManager.send(chatroomId, "conversation_error", error.getMessage());
        })
        .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
            .transientErrors(true));
}
```

**OpenAI → SSE 연결**:

1. **OpenAI Streaming API 호출**:
   ```java
   return webClient()
       .post()
       .uri("/v1/chat/completions")
       .body(BodyInserters.fromValue(request))
       .accept(MediaType.TEXT_EVENT_STREAM)
       .retrieve()
       .bodyToFlux(String.class)  // 스트림을 Flux로 변환
       .filter(s -> s != null && !s.isEmpty())
       .takeWhile(s -> !"[DONE]".equals(s.trim()));
   ```

2. **SSE로 전송**:
   ```java
   .doOnNext(chunk -> {
       sseManager.send(chatroomId, "conversation_chunk", chunk);
   })
   ```

**스트림 끊김 처리**:

1. **클라이언트 측 재연결**:
   ```typescript
   eventSource.onerror = (event) => {
     if (eventSource.readyState === EventSource.CLOSED) {
       if (retryCount < maxRetries) {
         setTimeout(() => {
           retryCount++;
           retryDelay = Math.min(retryDelay * 2, 30000);
           connect(); // 재연결
         }, retryDelay);
       }
     }
   };
   ```

2. **서버 측 재시도**:
   ```java
   .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
       .transientErrors(true))
   ```
   - 최대 3회 재시도
   - 지수 백오프 (1초 → 2초 → 4초)
   - 일시적 에러만 재시도

3. **에러 이벤트 전송**:
   ```java
   .doOnError(error -> {
       sseManager.send(chatroomId, "conversation_error", error.getMessage());
   })
   ```

**재시도 전략**:

1. **일시적 에러**: 네트워크 오류, 타임아웃 → 재시도
2. **영구적 에러**: 인증 실패, 잘못된 요청 → 재시도 안 함
3. **부분 실패**: 스트림 중간에 끊김 → 클라이언트가 재연결

**모니터링**:
- OpenAI API 호출 실패율 추적
- 재시도 횟수 추적
- 스트림 평균 길이 추적

---

### Q40. 토큰/비용 로깅을 위해 설계한 ai_usage_events, monthly_user_costs 테이블 구조와 사용 용도를 설명해보세요.

**답안**:

**테이블 구조**:

1. **`billing.ai_usage_events`** (원본 이벤트):

```sql
CREATE TABLE billing.ai_usage_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_time TIMESTAMP NOT NULL DEFAULT NOW(),
    user_id UUID NOT NULL,
    chatroom_id UUID NOT NULL,
    provider TEXT NOT NULL,              -- 'openai'
    model TEXT NOT NULL,                 -- 'gpt-4o', 'gpt-4o-mini'
    request_id TEXT UNIQUE,              -- OpenAI request_id
    input_tokens INT NOT NULL DEFAULT 0,
    output_tokens INT NOT NULL DEFAULT 0,
    cost_in NUMERIC(18,6) NOT NULL DEFAULT 0,   -- 입력 토큰 비용
    cost_out NUMERIC(18,6) NOT NULL DEFAULT 0,  -- 출력 토큰 비용
    meta JSONB                           -- 추가 메타데이터
);

CREATE INDEX idx_usage_events_user_time 
ON billing.ai_usage_events(user_id, event_time);
CREATE INDEX idx_usage_events_chatroom 
ON billing.ai_usage_events(chatroom_id);
```

2. **`billing.monthly_user_costs`** (월별 집계):

```sql
CREATE TABLE billing.monthly_user_costs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    year_month VARCHAR(7) NOT NULL,      -- '2025-01'
    total_input_tokens BIGINT NOT NULL DEFAULT 0,
    total_output_tokens BIGINT NOT NULL DEFAULT 0,
    total_cost_in NUMERIC(18,6) NOT NULL DEFAULT 0,
    total_cost_out NUMERIC(18,6) NOT NULL DEFAULT 0,
    total_cost NUMERIC(18,6) NOT NULL DEFAULT 0,  -- 총 비용
    request_count INT NOT NULL DEFAULT 0,
    UNIQUE(user_id, year_month)
);

CREATE INDEX idx_monthly_costs_user 
ON billing.monthly_user_costs(user_id, year_month);
```

**사용 용도**:

1. **`ai_usage_events`**:
   - **목적**: 모든 AI API 호출의 상세 기록
   - **용도**:
     - 개별 요청 추적 (디버깅, 분석)
     - 비용 상세 분석 (모델별, 시간대별)
     - 사용 패턴 분석
   - **저장 시점**: AI API 호출 직후
   - **예시**:
     ```sql
     INSERT INTO billing.ai_usage_events 
     (user_id, chatroom_id, provider, model, input_tokens, output_tokens, cost_in, cost_out)
     VALUES (?, ?, 'openai', 'gpt-4o', 150, 200, 0.000225, 0.0008);
     ```

2. **`monthly_user_costs`**:
   - **목적**: 사용자별 월별 비용 집계
   - **용도**:
     - 사용자 비용 대시보드
     - 과금 정책 수립
     - 비용 예측
   - **생성 시점**: 배치 작업으로 월별 집계 (매일 새벽)
   - **예시**:
     ```sql
     INSERT INTO billing.monthly_user_costs 
     (user_id, year_month, total_cost, request_count)
     VALUES (?, '2025-01', 12.50, 150)
     ON CONFLICT (user_id, year_month) 
     DO UPDATE SET total_cost = monthly_user_costs.total_cost + EXCLUDED.total_cost;
     ```

**비용 계산 로직**:

```java
// BillingService
public void recordUsage(UUID userId, UUID chatroomId, String provider, 
                       String model, Integer inputTokens, Integer outputTokens) {
    // 모델별 단가 조회
    double pricePer1kInput = getPricePer1kInput(model);
    double pricePer1kOutput = getPricePer1kOutput(model);
    
    // 비용 계산
    double costIn = (inputTokens / 1000.0) * pricePer1kInput;
    double costOut = (outputTokens / 1000.0) * pricePer1kOutput;
    
    // 이벤트 저장
    AiUsageEvent event = new AiUsageEvent(...);
    aiUsageEventRepository.save(event);
    
    // 월별 집계 업데이트 (비동기)
    updateMonthlyCosts(userId, costIn + costOut);
}
```

**분석 쿼리 예시**:

```sql
-- 사용자별 월별 비용
SELECT year_month, total_cost, request_count
FROM billing.monthly_user_costs
WHERE user_id = ?
ORDER BY year_month DESC;

-- 모델별 비용 분석
SELECT model, 
       SUM(cost_in + cost_out) as total_cost,
       COUNT(*) as request_count
FROM billing.ai_usage_events
WHERE event_time >= NOW() - INTERVAL '30 days'
GROUP BY model;

-- 시간대별 사용 패턴
SELECT DATE_TRUNC('hour', event_time) as hour,
       COUNT(*) as request_count,
       AVG(input_tokens + output_tokens) as avg_tokens
FROM billing.ai_usage_events
WHERE event_time >= NOW() - INTERVAL '7 days'
GROUP BY hour
ORDER BY hour;
```

---

### Q41. ConversationAgent 실패 시 Fallback 응답은 어떤 식으로 만들었나요? (서비스 품질 vs 비용/안정성 트레이드오프)

**답안**:

**Fallback 전략**:

1. **재시도 후 실패 시**:
   ```java
   conversationAgent.generateResponse(chatroomId, content, usageHolder)
       .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
           .transientErrors(true))
       .onErrorResume(error -> {
           // Fallback 응답 생성
           return Mono.just(generateFallbackResponse(chatroomId, content));
       })
   ```

2. **Fallback 응답 생성**:
   ```java
   private String generateFallbackResponse(UUID chatroomId, String userMessage) {
       // 1. 간단한 템플릿 기반 응답
       String template = getFallbackTemplate(chatroomId);
       
       // 2. 사용자 메시지 키워드 추출 (간단한 로직)
       String keyword = extractSimpleKeyword(userMessage);
       
       // 3. 템플릿에 키워드 삽입
       return String.format(template, keyword);
   }
   ```

**Fallback 템플릿 예시**:

```java
// 친밀도 레벨별 템플릿
Map<Integer, String> templates = Map.of(
    1, "네, %s에 대해 이야기하고 싶으시군요. 더 자세히 말씀해 주실 수 있나요?",
    2, "아, %s 말씀이시군요. 흥미롭네요!",
    3, "%s? 좋아! 더 얘기해봐!"
);
```

**트레이드오프 분석**:

| 전략 | 서비스 품질 | 비용 | 안정성 | 선택 |
|------|------------|------|--------|------|
| **재시도만** | 중간 (일시적 오류 해결) | 낮음 | 높음 | ✅ 기본 |
| **Fallback 템플릿** | 낮음 (단순 응답) | 매우 낮음 | 매우 높음 | ✅ 현재 |
| **저비용 모델 사용** | 중간 (품질 저하) | 중간 | 높음 | ⚠️ 향후 |
| **에러 메시지** | 매우 낮음 | 없음 | 높음 | ❌ 사용자 경험 저하 |

**현재 선택: Fallback 템플릿**

**이유**:
1. **안정성 우선**: 서비스가 항상 응답 (사용자 경험 유지)
2. **비용 절감**: OpenAI API 호출 없음
3. **빠른 응답**: 템플릿 기반으로 즉시 응답

**단점**:
- 응답 품질이 낮음 (자연스럽지 않음)
- 맥락 이해 부족

**향후 개선 계획**:
1. **저비용 모델 Fallback**: `gpt-4o-mini` 사용 (비용 1/10)
2. **캐싱된 응답**: 유사한 메시지에 대해 캐시된 응답 사용
3. **부분 실패 처리**: 스트림 중간에 끊김 시 완성된 부분만 전송

**실제 구현**:

```java
.onErrorResume(error -> {
    log.error("ConversationAgent 실패, Fallback 사용: chatroomId={}", chatroomId, error);
    
    // Fallback 응답 생성
    String fallback = generateFallbackResponse(chatroomId, content);
    
    // SSE로 Fallback 전송
    sseManager.send(chatroomId, "conversation_fallback", fallback);
    
    // Fallback도 메시지로 저장
    chatService.sendMessage(chatroomId, null, "bot", fallback, "text", null);
    
    return Mono.just(fallback);
})
```

---

## 8. 아키텍처 전반 / 설계 의도

### Q42. Shared Database + 멀티 스키마 패턴 대신 서비스별 DB(DDD 스타일)를 쓰지 않은 이유는 무엇인가요?

**답안**:

**선택 이유**:

1. **비용 최적화**:
   - 단일 PostgreSQL 인스턴스로 운영 비용 절감
   - 서비스별 DB 분리 시 인스턴스 수 증가 → 비용 증가
   - 현재 트래픽 수준에서는 오버엔지니어링

2. **운영 단순성**:
   - 백업/복원이 단일 인스턴스로 간단
   - 모니터링 포인트 단일화
   - 트랜잭션 경계 명확 (같은 DB 내)

3. **개발 속도**:
   - 초기 구축 시간 단축
   - 스키마 분리만으로 서비스 독립성 확보
   - 마이그레이션 복잡도 감소

4. **트랜잭션 지원**:
   - 서비스 간 트랜잭션이 필요한 경우 (현재는 없지만 향후 가능성)
   - 같은 DB 내에서 트랜잭션 경계 명확

**DDD 스타일(서비스별 DB)를 쓰지 않은 이유**:

1. **초기 단계**:
   - MVP 단계에서 복잡도 최소화
   - 서비스 경계가 명확하지 않은 초기에는 스키마 분리로 충분

2. **트래픽 수준**:
   - 현재 트래픽으로는 단일 DB로 충분
   - 서비스별 독립 스케일링 필요성 낮음

3. **팀 규모**:
   - BE 3명으로 서비스별 DB 관리 부담
   - 운영 복잡도 증가

**트레이드오프**:

| 측면 | Shared DB + 스키마 | 서비스별 DB |
|------|-------------------|------------|
| **비용** | ✅ 낮음 | ❌ 높음 |
| **운영** | ✅ 단순 | ❌ 복잡 |
| **독립성** | ⚠️ 스키마 레벨 | ✅ 완전 독립 |
| **스케일링** | ⚠️ 수직만 가능 | ✅ 수평 가능 |
| **트랜잭션** | ✅ 지원 | ❌ 분산 트랜잭션 필요 |

**향후 전략**:

1. **트래픽 증가 시**:
   - 서비스별 독립 DB로 분리
   - 스키마 → DB 마이그레이션 스크립트 준비

2. **점진적 분리**:
   - Chat Service부터 분리 (가장 높은 부하)
   - 나머지 서비스는 유지

3. **마이그레이션 계획**:
   - 데이터 복제 전략
   - 무중단 마이그레이션
   - 롤백 계획

**결론**:
- 현재 단계에서는 Shared DB + 스키마가 최적
- 비용, 운영 단순성, 개발 속도 우선
- 향후 필요 시 점진적 분리 가능

---

### Q43. 이 프로젝트 아키텍처를 "한 문장으로 요약"한다면?

**답안**:

**요약**: Spring Cloud Gateway를 통한 단일 진입점, JWT + HMAC 이중 인증, PostgreSQL 단일 인스턴스에 스키마로 분리된 5개 마이크로서비스, Reactor 기반 비동기 멀티 에이전트 AI 시스템, SSE 실시간 스트리밍, Prometheus + Grafana 모니터링 스택으로 구성된 한국어 학습 챗봇 플랫폼입니다.

**핵심 키워드**:
- MSA (마이크로서비스 아키텍처)
- JWT + HMAC 이중 인증
- Shared Database + 스키마 분리
- 멀티 에이전트 AI 시스템
- SSE 실시간 통신
- 모니터링 스택

---

### Q44. 지금 다시 처음부터 설계한다면 바꾸고 싶은 부분 2~3개와 그 이유는?

**답안**:

**1. DTO 변환 레이어 도입**

**현재 문제**:
- 엔티티를 직접 반환하여 `@ToString(exclude = ...)` 같은 임시 해결책 사용
- 순환 참조 문제
- API 응답 구조 변경 시 엔티티 수정 필요

**개선 방안**:
- MapStruct 또는 ModelMapper로 DTO 변환
- 엔티티와 API 응답 분리
- 순환 참조 완전 제거

**이유**:
- 유지보수성 향상
- API 버전 관리 용이
- 보안 강화 (민감 정보 제외)

**2. 서비스 간 통신을 비동기 메시징으로 전환**

**현재 문제**:
- Feign Client로 동기 통신 → 서비스 간 결합도 높음
- 한 서비스 장애 시 다른 서비스 영향
- 확장성 제한

**개선 방안**:
- RabbitMQ 또는 Kafka 도입
- 이벤트 기반 아키텍처
- 서비스 간 느슨한 결합

**이유**:
- 장애 격리 강화
- 확장성 향상
- 비동기 처리로 성능 개선

**3. 테스트 코드 작성**

**현재 문제**:
- 통합 테스트 부족
- 단위 테스트 부족
- 회귀 테스트 어려움

**개선 방안**:
- 서비스별 단위 테스트 작성
- 통합 테스트 작성 (TestContainers)
- E2E 테스트 작성

**이유**:
- 리팩토링 안정성
- 버그 조기 발견
- 문서화 효과

**우선순위**:
1. DTO 변환 레이어 (즉시 적용 가능)
2. 테스트 코드 (장기적 안정성)
3. 비동기 메시징 (트래픽 증가 시)

---

### Q45. 트래픽이 지금보다 10배, 100배 늘어난다면, 어떤 순서로 어디부터 바꿀 건가요?

**답안**:

**10배 트래픽 증가 시 (100-200명 동시 사용자)**:

**1단계: 데이터베이스 최적화** (즉시)
- 연결 풀 크기 증가 (60 → 100)
- 인덱스 최적화
- 쿼리 최적화 (N+1 문제 해결)
- 읽기 전용 복제본 추가 (선택적)

**2단계: 캐싱 강화** (1주일)
- Redis 캐싱 범위 확대
- 캐시 TTL 조정
- 캐시 전략 개선 (Cache-Aside → Write-Through)

**3단계: 서비스 스케일 아웃** (2주일)
- Chat Service만 독립 인스턴스로 분리
- 로드 밸런서 도입 (Nginx 또는 ALB)
- 서비스별 독립 스케일링

**100배 트래픽 증가 시 (1,000-2,000명 동시 사용자)**:

**1단계: 인프라 분리** (즉시)
- 서비스별 독립 DB로 분리
- Chat Service: 독립 DB + 독립 인스턴스
- Auth/User: 공유 DB 유지

**2단계: 비동기 메시징 도입** (1개월)
- RabbitMQ 또는 Kafka 도입
- 이벤트 기반 아키텍처 전환
- 서비스 간 느슨한 결합

**3단계: 컨테이너 오케스트레이션** (2개월)
- ECS 또는 EKS 도입
- 자동 스케일링 설정
- 무중단 배포 파이프라인

**4단계: CDN 및 정적 자원 최적화** (2개월)
- CloudFront 또는 Cloudflare 도입
- 정적 자원 CDN 배포
- 이미지 최적화

**우선순위 기준**:

1. **병목 지점 우선**:
   - 데이터베이스 → 캐싱 → 서비스 분리

2. **비용 효율성**:
   - 캐싱 (낮은 비용, 높은 효과)
   - 서비스 분리 (높은 비용, 높은 효과)

3. **구현 복잡도**:
   - 간단한 것부터 (캐싱 → 스케일 아웃 → 메시징)

**예상 비용**:

| 단계 | 현재 | 10배 | 100배 |
|------|------|------|-------|
| 인프라 | $75/월 | $150/월 | $500/월 |
| 데이터베이스 | 단일 | 단일 + 복제본 | 서비스별 분리 |
| 서비스 | 단일 EC2 | 2개 EC2 | ECS/EKS |

---

### Q46. MSA로 시작했을 때의 장점/단점과, 모놀리식으로 시작했다가 분리하는 전략과 비교해 본인의 생각은?

**답안**:

**MSA로 시작한 장점**:

1. **서비스 독립성**:
   - 서비스별 독립 배포 가능
   - 기술 스택 다양화 (Gateway는 Reactive, 다른 서비스는 MVC)
   - 장애 격리 (Circuit Breaker)

2. **팀 확장성**:
   - 향후 팀 확장 시 서비스별 담당 가능
   - 병렬 개발 가능

3. **스케일링 유연성**:
   - Chat Service만 독립적으로 스케일 아웃 가능
   - 리소스 효율적 사용

**MSA로 시작한 단점**:

1. **복잡도 증가**:
   - 서비스 간 통신 복잡 (Feign Client, HMAC 인증)
   - 배포 파이프라인 복잡
   - 모니터링 포인트 증가

2. **개발 속도 저하**:
   - 초기 구축 시간 증가
   - 서비스 간 통신 설계 필요
   - 디버깅 어려움

3. **운영 부담**:
   - 서비스별 모니터링 필요
   - 로그 분산
   - 트러블슈팅 복잡

**모놀리식 → MSA 전략**:

**장점**:
- 초기 개발 속도 빠름
- 단순한 구조로 빠른 프로토타이핑
- 필요 시 점진적 분리

**단점**:
- 분리 시 리팩토링 비용 높음
- 데이터 마이그레이션 복잡
- 서비스 경계 불명확 시 분리 어려움

**본인의 생각**:

**MSA로 시작한 것이 올바른 선택이었던 이유**:

1. **서비스 경계가 명확**:
   - Auth, User, Chat, Store, Batch로 명확히 분리 가능
   - 도메인 독립성이 높음

2. **기술적 요구사항**:
   - Gateway는 Reactive 필요 (SSE 지원)
   - Chat Service는 AI 처리로 높은 부하
   - 서비스별 다른 요구사항

3. **팀 구조**:
   - BE 3명이지만 향후 확장 가능성
   - 서비스별 담당 가능

**하지만 개선할 점**:

1. **초기에는 더 단순하게**:
   - Gateway 없이 직접 라우팅
   - 서비스 수 최소화 (Auth+User 통합 가능)

2. **점진적 복잡도 증가**:
   - MVP는 모놀리식으로 시작
   - 트래픽 증가 시 분리

**결론**:
- 현재 프로젝트에서는 MSA가 적절했음
- 서비스 경계가 명확하고 기술적 요구사항이 다양
- 다만 초기 복잡도를 더 낮출 수 있었음

---

## 9. 협업/역할 관련 질문

### Q47. PM/UI/UX/FE와 논의하면서, "기술적으로 안 되는 것/시간 내 어려운 것"을 어떻게 설득했나요?

**답안**:

**설득 전략**:

1. **구체적인 기술적 제약 설명**:
   - "안 된다"가 아닌 "왜 안 되는지" 설명
   - 대안 제시
   - 예상 소요 시간 명시

2. **우선순위 제안**:
   - MVP 기능 우선
   - 향후 개선 계획 제시
   - 단계적 구현 제안

**실제 사례**:

**사례 1: 실시간 타이핑 인디케이터**

**요구사항**: 사용자가 타이핑 중일 때 챗봇도 타이핑 중 표시

**기술적 제약**:
- 현재는 SSE (단방향), WebSocket 필요 (양방향)
- WebSocket 구현 시 1-2주 추가 소요

**설득 과정**:
1. 현재 SSE 구조 설명
2. WebSocket 구현 복잡도 설명
3. 대안 제시: AI 응답 스트리밍으로 실시간 느낌 제공
4. 향후 개선 계획: WebSocket 도입 검토

**결과**: SSE 스트리밍으로 대체, 사용자 만족도 높음

**사례 2: 다중 채팅방 동시 대화**

**요구사항**: 여러 채팅방에서 동시에 대화

**기술적 제약**:
- SSE 연결은 채팅방별 1개
- 다중 연결 시 리소스 부담

**설득 과정**:
1. 현재 SSE 구조 설명
2. 다중 연결 시 성능 영향 설명
3. 단계적 구현 제안:
   - 1단계: 단일 채팅방 (현재)
   - 2단계: 채팅방 전환 시 재연결
   - 3단계: 다중 연결 지원 (향후)

**결과**: 단계적 구현 합의

**설득 원칙**:

1. **투명성**: 기술적 제약을 숨기지 않음
2. **대안 제시**: "안 된다"가 아닌 "이렇게 하면 된다"
3. **우선순위**: MVP 우선, 향후 개선
4. **데이터 기반**: 성능 데이터, 비용 데이터 제시

---

### Q48. API 스펙 충돌이나 SSE 연동 이슈가 있었을 때, 구체적으로 어떤 문제였고 어떻게 해결했나요?

**답안**:

**사례 1: SSE 이벤트 타입 불일치**

**문제**:
- 백엔드: `intimacy_analysis` 이벤트 전송
- 프론트엔드: `intimacyAnalysis` (camelCase) 기대
- 이벤트 수신 실패

**원인**:
- 백엔드와 프론트엔드 간 네이밍 컨벤션 불일치
- API 스펙 문서 부재

**해결**:
1. 이벤트 타입 통일 (snake_case로 통일)
2. API 스펙 문서 작성 (OpenAPI/Swagger)
3. 프론트엔드 코드 수정

**예방**:
- API 스펙을 먼저 정의
- 공유 문서로 동기화

**사례 2: SSE 연결 타임아웃**

**문제**:
- 프론트엔드: SSE 연결이 60초 후 끊김
- 백엔드: 타임아웃 설정 없음

**원인**:
- Gateway 기본 타임아웃 60초
- SSE는 long-running 연결 필요

**해결**:
1. Gateway 타임아웃 증가 (600초)
2. Nginx 타임아웃 증가 (600초)
3. 클라이언트 재연결 로직 추가

**예방**:
- 장기 연결 요구사항 사전 공유
- 타임아웃 설정 문서화

**사례 3: 메시지 순서 보장**

**문제**:
- 여러 에이전트가 병렬 실행되어 이벤트 순서 불일치
- 프론트엔드에서 이벤트 순서 혼란

**원인**:
- 병렬 처리로 인한 비동기 이벤트 전송
- 이벤트 간 순서 보장 없음

**해결**:
1. 이벤트에 타임스탬프 추가
2. 프론트엔드에서 타임스탬프 기준 정렬
3. 이벤트 타입별 우선순위 설정

**예방**:
- 비동기 처리 시 순서 보장 방법 사전 논의
- 이벤트 스펙에 순서 정보 포함

**협업 개선**:

1. **API 스펙 문서화**:
   - OpenAPI/Swagger 사용
   - 이벤트 타입 명시

2. **정기 회의**:
   - 주간 회의로 이슈 사전 발견
   - 기술적 제약 사전 공유

3. **공유 문서**:
   - API 스펙, 이벤트 타입 공유
   - 변경 사항 즉시 공유

---

### Q49. 팀원 실수나 누락을 대신 커버했던 경험이 있다면, 그 사례를 자세히 말해보세요.

**답안**:

**사례: 배포 시 환경변수 누락**

**상황**:
- 팀원이 Chat Service 배포 시 OpenAI API 키 환경변수 누락
- 서비스는 정상 시작되었으나 AI 기능 동작 안 함
- 사용자 리포트로 발견

**대응 과정**:

1. **즉시 조치** (5분):
   - 로그 확인 → OpenAI API 키 누락 확인
   - 환경변수 추가하여 재배포
   - 기능 정상화 확인

2. **근본 원인 분석** (30분):
   - 배포 스크립트에 환경변수 체크 로직 없음
   - 헬스체크에서 OpenAI 연결 확인 안 함

3. **개선 조치** (1일):
   - 배포 스크립트에 필수 환경변수 체크 추가
   - 헬스체크에 OpenAI 연결 확인 추가
   - 배포 체크리스트 작성

**개선 결과**:
- 배포 시 자동으로 환경변수 검증
- 헬스체크에서 외부 의존성 확인
- 재발 방지

**교훈**:
- 자동화로 휴먼 에러 방지
- 체크리스트로 누락 방지
- 팀원 비난보다 시스템 개선

**협업 원칙**:
- 비난하지 않고 문제 해결에 집중
- 시스템 개선으로 재발 방지
- 지식 공유로 팀 전체 성장

---

**문서 작성 완료**: 2025-01-15