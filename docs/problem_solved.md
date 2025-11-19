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

## 보안 위협 대응 및 IP 블랙리스트 시스템 구현

### 문제 발생 일시
2025-11-11

### 문제 상황
- Gateway 로그에서 의심스러운 IP 주소 `172.104.24.172`에서 반복적인 공격 시도 감지
- 비정상 HTTP 요청 (RTSP/1.0, SIP/2.0, 빈 요청 등) 발생
- `Decoding failed: invalid version format` 오류 14건 발생
- 프로토콜 혼합 공격 및 Fuzzing 공격 시도 확인

### 문제 원인 분석

#### 1. 공격 유형 분석
```
- 프로토콜 혼합 공격 (Protocol Confusion Attack)
- Fuzzing 공격 (불법 문자 주입)
- 빈 요청 공격
```

#### 2. 공격 패턴
- **발생 기간**: 2025년 11월 11일 05:02:29 ~ 05:03:10 (약 41초)
- **총 로그 건수**: 9건
- **공격 목적**: 서버 취약점 스캔 및 프로토콜 탐지 시도

### 해결 방법

#### Phase 1: 즉시 대응 (난이도: 매우 낮음)

**1. AWS Security Group IP 차단 스크립트 작성**
- `scripts/security/block-ip-aws.ps1` 생성
- AWS CLI를 통한 Security Group 관리 자동화

**2. 보안 대응 절차 문서화**
- `docs/maintenance/2025-11-11/security-response-plan.md` 작성
- 즉시, 단기, 장기 대응 방안 정리

**3. 보안 모니터링 스크립트 작성**
- `scripts/security/monitor-suspicious-ips.sh` 생성
- Gateway 로그에서 의심스러운 IP 패턴 자동 탐지

#### Phase 2: Gateway 레벨 보안 강화 (난이도: 낮음)

**1. IP 블랙리스트 필터 구현**
```java
// IpBlacklistFilter.java
@Component
public class IpBlacklistFilter implements GlobalFilter, Ordered {
    private final Set<String> blacklistedIps;
    
    @Override
    public int getOrder() {
        return -100; // 가장 먼저 실행
    }
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String clientIp = extractClientIp(exchange.getRequest());
        if (isBlacklisted(clientIp)) {
            return handleBlockedRequest(exchange, clientIp);
        }
        return chain.filter(exchange);
    }
}
```

**2. 블랙리스트 관리 API 구현**
```java
// BlacklistController.java
@RestController
@RequestMapping("/api/admin/blacklist")
public class BlacklistController {
    // GET /api/admin/blacklist - 블랙리스트 조회
    // POST /api/admin/blacklist - IP 추가
    // DELETE /api/admin/blacklist/{ip} - IP 제거
    // GET /api/admin/blacklist/{ip} - 특정 IP 차단 여부 확인
}
```

**3. 설정 파일 업데이트**
```yaml
# application.yml
gateway:
  security:
    blacklist:
      ips: "172.104.24.172"
```

### 발견된 추가 문제점

#### 1. 인증 구조 충돌
- **문제**: SecurityConfig의 `permitAll()` 설정과 JwtAuthFilter의 정책 불일치
- **영향**: 설정이 혼란스럽고 향후 충돌 가능성
- **해결**: SecurityConfig 정책을 JwtAuthFilter와 일치시키도록 수정 필요

#### 2. 관리자 권한 체크 없음
- **문제**: 관리자 API에 일반 사용자도 접근 가능 (JWT 토큰만 있으면 OK)
- **영향**: 심각한 보안 취약점
- **해결**: JWT 토큰에서 역할(role) 클레임 확인 및 `ROLE_ADMIN` 권한 체크 추가 필요

#### 3. 블랙리스트 영구 저장 없음
- **문제**: 런타임에 추가한 IP는 서버 재시작 시 사라짐
- **영향**: 관리자가 추가한 IP가 유지되지 않음
- **해결**: Redis 또는 데이터베이스를 통한 영구 저장 기능 추가 필요

### 결과

#### 해결 전
- 의심스러운 IP에서 지속적인 공격 시도
- Gateway 레벨에서 차단 기능 없음
- 수동으로만 IP 차단 가능

#### 해결 후
- Gateway 레벨에서 자동 IP 차단
- 관리자 API를 통한 런타임 블랙리스트 관리
- 보안 모니터링 스크립트로 자동 탐지

### 교훈

1. **보안 로그 모니터링**: 정기적인 로그 분석을 통한 위협 조기 발견
2. **다층 방어**: AWS Security Group + Gateway 필터 + 애플리케이션 레벨 보안
3. **자동화**: 스크립트를 통한 보안 대응 자동화
4. **문서화**: 보안 대응 절차를 문서화하여 일관된 대응 보장
5. **권한 관리**: 관리자 API는 반드시 권한 체크 필요

### 관련 파일
- `gateway/src/main/java/com/dorandoran/gateway/filter/IpBlacklistFilter.java`
- `gateway/src/main/java/com/dorandoran/gateway/controller/BlacklistController.java`
- `scripts/security/block-ip-aws.ps1`
- `scripts/security/monitor-suspicious-ips.sh`
- `docs/maintenance/2025-11-11/ip-172.104.24.172-security-analysis.md`
- `docs/maintenance/2025-11-11/security-response-plan.md`
- `docs/maintenance/2025-11-11/phase2-authentication-analysis.md`
- `docs/maintenance/2025-11-11/security-status-check.md`
- `docs/maintenance/2025-11-11/blacklist-storage-location.md`

---

## 인증 구조 분석 및 개선

### 문제 발생 일시
2025-11-11

### 문제 상황
- SecurityConfig와 JwtAuthFilter의 정책 불일치
- 관리자 API에 일반 사용자도 접근 가능
- 블랙리스트 IP 저장 위치 및 영구 저장 여부 불명확

### 문제 원인 분석

#### 1. SecurityConfig 정책 불일치
```java
// 현재 설정
.pathMatchers("/api/**").permitAll()  // 모든 API 허용

// 실제 동작
// JwtAuthFilter가 제외 목록에 없는 경로는 인증 필요
// SecurityConfig 설정이 무의미함
```

#### 2. 관리자 권한 체크 없음
- JWT 토큰만 있으면 누구나 관리자 API 접근 가능
- 역할(role) 기반 권한 체크 없음

#### 3. 블랙리스트 저장 위치
- 초기 로드: `application.yml` 설정 파일
- 런타임 저장: 메모리 (HashSet)
- 영구 저장: 없음 (서버 재시작 시 사라짐)

### 해결 방법

#### 1. 인증 구조 분석 문서 작성
- `docs/maintenance/2025-11-11/phase2-authentication-analysis.md`
- 필터 실행 순서 및 충돌 가능성 상세 분석

#### 2. 보안 상태 점검 문서 작성
- `docs/maintenance/2025-11-11/security-status-check.md`
- API 접근 제어 및 관리자 권한 상태 확인

#### 3. 블랙리스트 저장 위치 분석 문서 작성
- `docs/maintenance/2025-11-11/blacklist-storage-location.md`
- 현재 저장 방식 및 영구 저장 필요성 분석

### 권장 개선 사항

#### 1. SecurityConfig 정책 수정 (즉시 필요)
```java
.pathMatchers("/api/admin/**").authenticated()  // 관리자 API는 인증 필요
.pathMatchers("/api/auth/**").permitAll()       // Auth API는 허용
.pathMatchers("/api/**").authenticated()        // 나머지는 인증 필요
```

#### 2. 관리자 권한 체크 추가 (즉시 필요)
- JWT 토큰에서 역할(role) 클레임 확인
- `ROLE_ADMIN` 권한이 있는 사용자만 접근 허용

#### 3. 블랙리스트 영구 저장 (단기 개선)
- Redis를 활용한 영구 저장 기능 추가
- 서버 재시작 후에도 런타임에 추가한 IP 유지

### 결과

#### 분석 전
- 인증 구조의 충돌 가능성 불명확
- 관리자 권한 체크 필요성 인지하지 못함
- 블랙리스트 저장 위치 불명확

#### 분석 후
- 인증 구조의 충돌 지점 명확히 파악
- 보안 취약점 발견 및 개선 방향 제시
- 블랙리스트 저장 방식 및 개선 필요성 확인

### 교훈

1. **정기적인 보안 점검**: 인증/인가 구조를 정기적으로 검토
2. **다층 보안**: 설정 파일, 필터, 컨트롤러 레벨에서 일관된 정책 적용
3. **권한 관리**: 역할 기반 접근 제어(RBAC) 구현 필요
4. **데이터 영구성**: 중요한 보안 설정은 영구 저장 필요

### 관련 파일
- `gateway/src/main/java/com/dorandoran/gateway/config/SecurityConfig.java`
- `gateway/src/main/java/com/dorandoran/gateway/filter/JwtAuthFilter.java`
- `gateway/src/main/java/com/dorandoran/gateway/filter/IpBlacklistFilter.java`
- `docs/maintenance/2025-11-11/phase2-authentication-analysis.md`
- `docs/maintenance/2025-11-11/security-status-check.md`
- `docs/maintenance/2025-11-11/blacklist-storage-location.md`
