# 문제 해결 기록

## Hibernate 프록시 객체 순환 참조 문제 해결

### 문제 발생 일시
2025-10-26

### 문제 상황
- 애플리케이션 실행 중 `NullPointerException`과 스택 오버플로우 발생
- 로그에서 `User$HibernateProxy$Dyp9SYgE.toString` 무한 반복 확인
- `greeting_guide_message` SSE 이벤트 전송 실패

### 문제 원인 분석

#### 1. 양방향 관계 설정
```java
// User 엔티티
@OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
private List<ChatRoom> chatRooms = new ArrayList<>();

// ChatRoom 엔티티  
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id", nullable = false)
private User user;
```

#### 2. Lombok @Data의 toString() 생성 문제
`@Data` 어노테이션이 자동으로 생성하는 `toString()` 메서드에서 순환 참조 발생:

```java
// Lombok이 생성하는 toString() 메서드 (의사코드)
public String toString() {
    return "User{" +
           "id=" + id +
           ", email=" + email +
           ", chatRooms=" + chatRooms +  // ← 순환 참조 원인
           "}";
}

public String toString() {
    return "ChatRoom{" +
           "id=" + id +
           ", name=" + name +
           ", user=" + user +  // ← 순환 참조 원인
           "}";
}
```

#### 3. 순환 참조 발생 과정
1. **User.toString()** 호출
2. `chatRooms` 필드를 문자열로 변환하려고 시도
3. `chatRooms`는 `List<ChatRoom>`이므로 각 `ChatRoom`의 `toString()` 호출
4. **ChatRoom.toString()** 호출
5. `user` 필드를 문자열로 변환하려고 시도
6. `user`는 `User` 객체이므로 `User.toString()` 호출
7. **1번으로 돌아가서 무한 루프!** 🔄

#### 4. Hibernate 프록시 객체의 역할
- Hibernate는 **지연 로딩(Lazy Loading)**을 위해 프록시 객체 사용
- 실제 데이터베이스에서 데이터를 가져오지 않고 가상의 객체를 만들어서 참조만 유지
- 실제로 필드에 접근할 때만 데이터베이스를 조회
- 프록시 객체의 `toString()` 호출 시 실제 객체를 로드하면서 순환 참조 발생

### 해결 방법

#### @ToString(exclude = {...}) 어노테이션 추가

**User 엔티티 수정:**
```java
@Entity
@Table(name = "app_user", schema = "user_schema")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"chatRooms"})  // 순환 참조 방지
public class User {
    // chatRooms 필드가 toString()에서 제외됨
    private List<ChatRoom> chatRooms = new ArrayList<>();
}
```

**ChatRoom 엔티티 수정:**
```java
@Entity
@Table(name = "chatrooms", schema = "chat_schema")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"user", "chatbot", "messages", "lastMessage"})  // 모든 연관 엔티티 제외
public class ChatRoom {
    // 모든 연관 엔티티 필드들이 toString()에서 제외됨
    private User user;
    private Chatbot chatbot;
    private List<Message> messages;
    private Message lastMessage;
}
```

### 해결 원리

#### 1. 순환 참조 차단
- `User.toString()`에서 `chatRooms` 제외 → `ChatRoom.toString()` 호출 안 함
- `ChatRoom.toString()`에서 `user` 제외 → `User.toString()` 호출 안 함

#### 2. 필요한 정보만 출력
```java
// User 출력 예시
User{id=123e4567-e89b-12d3-a456-426614174000, email=user@example.com, firstName=John, lastName=Doe}

// ChatRoom 출력 예시  
ChatRoom{id=456e7890-e89b-12d3-a456-426614174001, name=My Chat Room, description=Test room}
```

#### 3. 성능 향상
- 불필요한 연관 객체 로딩 방지
- `toString()` 호출 시 데이터베이스 쿼리 최소화
- 메모리 사용량 감소

### 결과

#### 문제 발생 시 로그
```
at com.dorandoran.chat.entity.User$HibernateProxy$Dyp9SYgE.toString(Unknown Source)
at com.dorandoran.chat.entity.ChatRoom.toString(ChatRoom.java:25)
at com.dorandoran.chat.entity.User$HibernateProxy$Dyp9SYgE.toString(Unknown Source)
at com.dorandoran.chat.entity.ChatRoom.toString(ChatRoom.java:25)
// 무한 반복
...
```

#### 해결 후 로그
```
2025-10-26 15:13:41 - Started ChatApplication in 39.536 seconds
// 정상적인 애플리케이션 시작 로그
```

### 추가 개선사항

이 문제 해결과 함께 이전에 수정한 **GreetingService 프롬프트 개선사항**도 함께 적용됨:

1. **JSON 형식 수정**: `intimacyLevel`, `detectedLevel` 필드 제거
2. **예시 시나리오 수정**: 모든 예시에서 올바른 JSON 형식 적용  
3. **안전성 강화**: null 체크 및 예외 처리 개선
4. **`greeting_guide_message` 이벤트 전송 문제 해결**

### 교훈

1. **양방향 관계에서 toString() 주의**: Lombok `@Data` 사용 시 순환 참조 가능성 고려
2. **@ToString(exclude = {...}) 활용**: 연관 엔티티 필드는 toString()에서 제외하는 것이 안전
3. **Hibernate 프록시 객체 이해**: 지연 로딩과 프록시 객체의 동작 방식 이해 필요
4. **체계적인 문제 해결**: 로그 분석 → 근본 원인 파악 → 체계적 수정 → 안전한 배포

### 관련 파일
- `chat/src/main/java/com/dorandoran/chat/entity/User.java`
- `chat/src/main/java/com/dorandoran/chat/entity/ChatRoom.java`
- `chat/src/main/java/com/dorandoran/chat/service/GreetingService.java`

---

## GreetingService 프롬프트 개선

### 문제 발생 일시
2025-10-26

### 문제 상황
- `greeting_guide_message` SSE 이벤트가 클라이언트에 전달되지 않음
- AI 응답 파싱 실패로 인한 `NullPointerException` 발생
- 프롬프트 JSON 형식 불일치

### 문제 원인 분석

#### 1. 프롬프트 JSON 형식 오류
```java
// 잘못된 형식 (문제 원인)
{ 
"intimacyLevel": "AI가 감지한 친밀도(0~3)",
    "botMessage": 인트로 메시지  // 따옴표 없음
    "guideMessage": 대화 문구를 제안  // 따옴표 없음
}
```

#### 2. 불필요한 필드 요구
- `intimacyLevel`: AI가 받는 입력 파라미터이므로 출력에 포함할 필요 없음
- `detectedLevel`: 사용되지 않는 필드이므로 제거 필요

#### 3. 예시 시나리오 불일치
```java
// 잘못된 예시
{
    "botMessage": 지금 뭐해?,  // 따옴표 없음
    "guideMessage": Let's continue...,  // 따옴표 없음
    "detectedLevel": 0  // 불필요한 필드
}
```

### 해결 방법

#### 1. JSON 형식 통일
```java
// 모든 프롬프트에서 동일한 형식 사용
- 다음 JSON 형식으로 정확히 답변:
{
    "botMessage": "인트로 메시지",
    "guideMessage": "대화 문구를 제안"
}
```

#### 2. 예시 시나리오 수정
```java
// 올바른 예시
{
    "botMessage": "지금 뭐해?",
    "guideMessage": "Let's continue the conversation about what fun or interesting things you're doing right now!"
}
```

#### 3. null 체크 및 안전성 강화
```java
private GreetingResponse parseAIResponse(String aiResponse) {
    try {
        JsonNode jsonNode = objectMapper.readTree(aiResponse);
        String botMessage = jsonNode.get("botMessage").asText();
        String guideMessage = jsonNode.get("guideMessage").asText();
        
        // null 체크 추가
        if (botMessage == null || guideMessage == null) {
            log.warn("AI 응답에서 null 값 발견: botMessage={}, guideMessage={}", botMessage, guideMessage);
            throw new RuntimeException("AI 응답에 null 값이 포함됨");
        }
        
        return new GreetingResponse(botMessage, guideMessage);
    } catch (Exception e) {
        log.error("AI 응답 파싱 실패: {}", aiResponse, e);
        throw new RuntimeException("AI 응답 파싱 실패", e);
    }
}
```

### 결과

#### 해결 전
- AI가 잘못된 JSON 형식으로 응답
- 파싱 실패로 인한 `NullPointerException`
- `greeting_guide_message` 이벤트 전송 실패

#### 해결 후
- AI가 올바른 JSON 형식으로 응답
- 파싱 성공으로 정상적인 `GreetingResponse` 생성
- `greeting_bot_message`와 `greeting_guide_message` 이벤트 모두 정상 전송

### 교훈

1. **프롬프트 일관성**: 모든 프롬프트에서 동일한 JSON 형식 사용
2. **예시 품질**: 예시 시나리오가 실제 요구사항과 정확히 일치해야 함
3. **불필요한 필드 제거**: 사용하지 않는 필드는 프롬프트에서 제거
4. **안전성 고려**: null 체크와 예외 처리를 통한 방어적 프로그래밍

### 관련 파일
- `chat/src/main/java/com/dorandoran/chat/service/GreetingService.java`

---

## 2025-10-28: Redis 캐싱 무한 순환 참조 문제 해결

### 문제 발생 일시
2025-10-28

### 문제 상황
- 채팅방 생성/조회 시 500 Internal Server Error 발생
- Jackson 직렬화 중 무한 순환 참조로 인한 중첩 깊이 초과
- Hibernate 프록시 객체가 `@JsonIgnore`를 무시하여 순환 참조 발생

### 문제 원인 분석

#### 1. Jackson 순환 참조 무한 루프
**오류 로그**:
```
Document nesting depth (1001) exceeds the maximum allowed (1000, from `StreamWriteConstraints.getMaxNestingDepth()`)
```

**순환 참조 경로**:
```
User["chatRooms"] → ChatRoom["user"] → User["chatRooms"] → ChatRoom["user"] → ...
```

#### 2. Hibernate 프록시 객체 문제
- `@JsonIgnore` 어노테이션이 Hibernate 프록시 객체에서 무시됨
- `org.hibernate.collection.spi.PersistentBag`가 실제 엔티티가 아닌 프록시 객체
- Jackson 직렬화 시 프록시 객체의 필드에 직접 접근하여 순환 참조 생성

#### 3. Redis 캐싱 구현 과정에서 발생한 문제들

**문제 1: Redis 의존성 인식 오류**
```
RedisConnectionFactory cannot be resolved to a type
Jackson2JsonRedisSerializer cannot be resolved to a type
```
- IDE/컴파일러가 새로 추가된 Redis 의존성을 인식하지 못함

**문제 2: enableStatistics() 메서드 오류**
```
cannot find symbol .enableStatistics()
```
- `RedisCacheConfiguration`에 직접 `enableStatistics()` 메서드가 없음

**문제 3: SpEL 표현식 오류**
```
org.springframework.expression.spel.SpelEvaluationException: 
EL1004E: Method call: Method getCurrentIntimacyLevel(java.util.UUID) cannot be found
```
- SpEL에서 private 메서드 호출 시도

**문제 4: LocalDateTime 직렬화 오류**
```
com.fasterxml.jackson.databind.exc.InvalidDefinitionException: 
Java 8 date/time type java.time.LocalDateTime not supported by default
```
- Jackson이 LocalDateTime 직렬화 모듈이 없음

### 해결 과정

#### 1단계: 기본 Redis 캐싱 구현
- `build.gradle`에 Redis 의존성 추가
- `RedisCacheConfig` 클래스 생성
- Entity 레벨 캐싱 적용 (`@Cacheable` 어노테이션)

#### 2단계: 발생한 문제들 해결
1. **의존성 인식 문제**: 프로젝트 재빌드로 해결
2. **enableStatistics() 오류**: `RedisCacheManager.builder`에서 호출하도록 수정
3. **SpEL 오류**: 메서드를 public으로 변경하고 키 단순화
4. **LocalDateTime 오류**: `JavaTimeModule` 등록

#### 3단계: 순환 참조 문제 근본 해결
**핵심 문제**: Entity 캐싱 시 Hibernate 프록시가 `@JsonIgnore`를 무시하여 순환 참조 발생

**해결 전략**: Entity 캐싱 → DTO 캐싱 전환

1. **Repository 레벨 캐싱 제거**
   - `UserRepository.findByEmail()`, `findById()`의 `@Cacheable` 제거
   - `ChatbotRepository.findById()`의 `@Cacheable` 제거

2. **새로운 DTO 클래스 생성**
   - `UserCacheDto`: User 엔티티의 필요한 필드만 포함
   - `ChatbotCacheDto`: Chatbot 엔티티의 필요한 필드만 포함
   - `ChatRoomResponse`: `Serializable` 구현

3. **Service 레벨 DTO 기반 캐싱 메서드 추가**
   - `ChatService.getUserCache()`: UserCacheDto 반환
   - `ChatService.getChatbotCache()`: ChatbotCacheDto 반환
   - `ChatService.getChatRoomCache()`: ChatRoomResponse 반환
   - `ChatService.listRoomsCache()`: List<ChatRoomResponse> 반환

4. **캐시 무효화 로직 추가**
   - `updateRoom()`: `@CacheEvict(value = {"chatrooms", "roomList"})`
   - `softDeleteRoom()`: `@CacheEvict(value = {"chatrooms", "roomList"})`
   - `updateIntimacyLevel()`: `@CacheEvict(value = {"intimacy", "chatrooms", "roomList"})`

5. **Entity 정리**
   - `Chatbot` 엔티티에서 `@JsonIgnore` 제거 (더 이상 직렬화하지 않음)
   - `RedisCacheConfig`에 "DTO 캐싱으로 전환 완료" 주석 추가

#### 4단계: 최종 정리
- DTO 기반 캐싱 메서드들을 제거 (사용자 요청)
- Repository 레벨 캐싱만 제거하여 순환 참조 문제 해결
- 기본 Redis 캐싱 설정은 유지

### 해결 원리

#### 1. 순환 참조 차단
- Repository 레벨에서 Entity 캐싱 제거
- Entity 직렬화 시점에서 순환 참조 발생 방지

#### 2. 안전한 캐싱 유지
- String, Integer 등 단순 타입은 안전하게 캐싱
- `PromptService.buildSystemPrompt()` 캐싱 유지
- `MultiAgentOrchestrator.getCurrentIntimacyLevel()` 캐싱 유지
- `ConversationAgent.buildMessageHistory()` 캐싱 유지

### 결과

#### 해결 전
- 채팅방 생성/조회 시 500 에러 발생
- Jackson 순환 참조 무한 루프
- 중첩 깊이 1000 초과 오류

#### 해결 후
- ✅ 모든 컴파일 오류 해결
- ✅ Redis 캐싱 정상 작동
- ✅ **순환 참조 문제 근본 해결**
- ✅ 빌드 성공 확인
- ✅ 채팅방 생성/조회 정상 작동

### 교훈

1. **Entity 캐싱의 위험성**: Hibernate 프록시 객체와 Jackson 직렬화의 상호작용 주의
2. **@JsonIgnore의 한계**: Hibernate 프록시에서는 무시될 수 있음
3. **DTO 캐싱의 안전성**: 연관관계가 없는 DTO는 순환 참조 불가능
4. **단계적 문제 해결**: 작은 문제부터 해결하고 근본 원인 파악
5. **캐싱 전략의 중요성**: 어떤 데이터를 캐싱할지 신중히 결정

### 관련 파일
- `chat/src/main/java/com/dorandoran/chat/repository/UserRepository.java`
- `chat/src/main/java/com/dorandoran/chat/repository/ChatbotRepository.java`
- `chat/src/main/java/com/dorandoran/chat/service/dto/UserCacheDto.java`
- `chat/src/main/java/com/dorandoran/chat/service/dto/ChatbotCacheDto.java`
- `chat/src/main/java/com/dorandoran/chat/service/dto/ChatRoomResponse.java`
- `chat/src/main/java/com/dorandoran/chat/config/RedisCacheConfig.java`
- `chat/src/main/java/com/dorandoran/chat/entity/Chatbot.java`

---

## 2025-11-02: 이메일 인증 Redis 저장 시 LocalDateTime 직렬화 문제 해결

### 문제 발생 일시
2025-11-02

### 문제 상황
- 이메일 인증 요청 시 500 Internal Server Error 발생
- Redis에 인증 요청 정보 저장 시 `InvalidDefinitionException` 발생
- Jackson이 `LocalDateTime`을 직렬화하지 못함

### 문제 원인 분석

#### 1. LocalDateTime 직렬화 오류
**오류 로그**:
```
com.fasterxml.jackson.databind.exc.InvalidDefinitionException: 
Java 8 date/time type `java.time.LocalDateTime` not supported by default: 
add Module "com.fasterxml.jackson.datatype:jackson-datatype-jsr310" to enable handling 
(through reference chain: com.dorandoran.auth.service.EmailVerificationRedisService$VerificationData["createdAt"])
```

**문제 원인**:
- `EmailVerificationRedisService`에서 `VerificationData` 객체에 `LocalDateTime` 필드(`createdAt`, `expiresAt`) 포함
- `ObjectMapper`를 기본 생성자로 생성하여 Java 8 시간 타입 지원 모듈 미등록
- Redis에 JSON으로 저장할 때 `LocalDateTime` 직렬화 실패

#### 2. Gmail SMTP 인증 실패 (추가 문제)
- `Authentication failed: 535-5.7.8 Username and Password not accepted`
- 잘못된 Gmail 계정 또는 App Password 사용

### 해결 과정

#### 1단계: LocalDateTime 직렬화 문제 해결

**문제**: 기본 `ObjectMapper`는 Java 8 시간 타입을 직렬화하지 못함

**해결 방법**:
```java
// EmailVerificationRedisService.java
@Service
@Slf4j
public class EmailVerificationRedisService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    
    // ObjectMapper 초기화 (생성자에서 JavaTimeModule 등록)
    public EmailVerificationRedisService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
```

**필요한 의존성**:
- `com.fasterxml.jackson.datatype:jackson-datatype-jsr310` (Spring Boot에 기본 포함됨)

#### 2단계: 이메일 전송 실패 시 데이터 보존

**문제**: 이메일 전송 실패 시 Redis에 저장된 데이터도 함께 실패 처리

**해결 방법**:
```java
// AuthController.java
@PostMapping("/email/request-verification")
public ResponseEntity<ApiResponse<String>> requestEmailVerification(@RequestBody Map<String, String> request) {
    try {
        // 1. 이메일 중복 확인
        // 2. 토큰 생성
        // 3. Redis에 인증 요청 정보 저장 (이메일 전송 전에 저장)
        emailVerificationRedisService.saveVerificationRequest(email, token);
        
        // 4. 이메일 발송 (실패해도 Redis 데이터는 유지)
        try {
            emailService.sendVerificationEmail(email, verifyLink);
            return ResponseEntity.ok(ApiResponse.success("sent", "인증 메일이 발송되었습니다."));
        } catch (Exception e) {
            // 이메일 전송 실패해도 Redis에는 저장되어 있으므로 사용자가 나중에 재시도 가능
            log.error("이메일 전송 실패 (Redis에는 저장됨): email={}, error={}", email, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("이메일 전송에 실패했습니다. 잠시 후 다시 시도해주세요. (인증 요청은 저장되었습니다.)", ErrorCode.INTERNAL_SERVER_ERROR.getCode()));
        }
    } catch (Exception e) {
        // ...
    }
}
```

#### 3단계: Gmail SMTP 설정 수정

**문제**: 잘못된 Gmail 계정 사용

**해결 방법**:
- Gmail 계정을 `kdhdaniel0506@gmail.com`에서 `zipizigy121@gmail.com`으로 변경
- Docker 컨테이너 환경 변수 업데이트

### 해결 원리

#### 1. JavaTimeModule의 역할
- `JavaTimeModule`: Java 8 시간 타입(`LocalDateTime`, `LocalDate`, `ZonedDateTime` 등)을 JSON으로 직렬화/역직렬화하는 모듈
- 기본 `ObjectMapper`에는 포함되지 않으므로 명시적으로 등록 필요

#### 2. WRITE_DATES_AS_TIMESTAMPS 비활성화
- 기본값: `true` (날짜를 타임스탬프 숫자로 저장)
- 비활성화 시: ISO 8601 형식 문자열로 저장 (`"2025-11-02T10:36:31"`)
- 가독성 향상 및 다른 시스템과의 호환성 개선

#### 3. 이메일 전송 실패 대응 전략
- Redis 저장을 이메일 전송 전에 수행
- 이메일 전송 실패 시에도 Redis 데이터는 유지
- 사용자가 나중에 재시도 가능

### 결과

#### 해결 전
- 이메일 인증 요청 시 500 에러 발생
- Redis 저장 실패로 인한 `InvalidDefinitionException`
- `LocalDateTime` 직렬화 불가

#### 해결 후
- ✅ `LocalDateTime` 직렬화 정상 작동
- ✅ Redis에 인증 요청 정보 정상 저장
- ✅ 이메일 전송 실패 시에도 데이터 보존
- ✅ Gmail SMTP 정상 작동

### 교훈

1. **Java 8 시간 타입 직렬화**: `ObjectMapper`에 `JavaTimeModule` 등록 필수
2. **의존성 확인**: Spring Boot에 기본 포함되어 있지만 모듈 등록은 필요
3. **오류 처리 전략**: 외부 서비스(SMTP) 실패 시에도 내부 데이터는 보존
4. **단계별 문제 해결**: 직렬화 문제 → SMTP 문제 순서로 해결

### 관련 파일
- `auth/src/main/java/com/dorandoran/auth/service/EmailVerificationRedisService.java`
- `auth/src/main/java/com/dorandoran/auth/controller/AuthController.java`
- `auth/src/main/java/com/dorandoran/auth/service/EmailService.java`
