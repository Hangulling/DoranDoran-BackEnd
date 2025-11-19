# DoranDoran 프로젝트 회고

**작성일**: 2025년 11월  
**프로젝트 기간**: 2024년 ~ 2025년  
**프로젝트 유형**: 마이크로서비스 아키텍처 기반 웹 애플리케이션

---

## 목차

1. [프로젝트 개요](#1-프로젝트-개요)
2. [구현한 기능](#2-구현한-기능)
3. [해결한 주요 문제들](#3-해결한-주요-문제들)
4. [인프라 및 배포](#4-인프라-및-배포)
5. [성능 최적화](#5-성능-최적화)
6. [보안 구현](#6-보안-구현)
7. [개발 도구 및 스크립트](#7-개발-도구-및-스크립트)
8. [교훈 및 개선 사항](#8-교훈-및-개선-사항)
9. [프로젝트 통계](#9-프로젝트-통계)

---

## 1. 프로젝트 개요

### 1.1 프로젝트 소개

DoranDoran은 Spring Boot와 마이크로서비스 아키텍처를 기반으로 한 웹 애플리케이션입니다. 사용자 관리, 인증/인가, 실시간 채팅, AI 기반 대화 생성 등의 기능을 제공하는 종합적인 플랫폼입니다.

**프로젝트 목적**:
- 마이크로서비스 아키텍처 설계 및 구현 경험
- 실시간 채팅 시스템 구축
- AI 기반 멀티 에이전트 시스템 개발
- 클라우드 인프라 운영 경험

### 1.2 기술 스택

#### 백엔드
- **언어**: Java 21
- **프레임워크**: Spring Boot 3.3.4
- **아키텍처**: 마이크로서비스 (MSA)
- **API Gateway**: Spring Cloud Gateway (Reactive)
- **빌드 도구**: Gradle 8.0+

#### 데이터베이스
- **주 데이터베이스**: PostgreSQL 17 (Shared Database)
- **캐시**: Redis 6.0+
- **마이그레이션**: Flyway

#### 인프라
- **컨테이너화**: Docker
- **클라우드**: AWS EC2
- **모니터링**: Prometheus + Grafana

#### 외부 서비스
- **AI 서비스**: OpenAI API (GPT-4o-mini)
- **이메일**: Gmail SMTP

#### 주요 라이브러리
- **서비스 통신**: Spring Cloud OpenFeign
- **회로 차단기**: Resilience4j
- **비동기 처리**: Project Reactor (WebFlux)
- **인증**: JWT (JSON Web Token)
- **문서화**: SpringDoc OpenAPI (Swagger)

### 1.3 아키텍처 개요

DoranDoran은 **마이크로서비스 아키텍처(MSA)**를 채택하여 각 서비스를 독립적으로 개발, 배포, 운영할 수 있도록 설계되었습니다.

#### 전체 아키텍처 구조

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   API Gateway   │    │   Auth Service  │    │   User Service  │
│   (Port: 8080)  │◄──►│   (Port: 8081)  │◄──►│   (Port: 8082)  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  Chat Service   │    │  Store Service  │    │  Batch Service  │
│   (Port: 8083)  │    │   (Port: 8084)  │    │   (Port: 8085)  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   PostgreSQL    │    │     Redis       │    │   Monitoring   │
│   (Port: 5432)  │    │   (Port: 6379)  │    │  (Grafana)      │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

#### 서비스 구성

1. **API Gateway (8080)**: 모든 외부 요청의 진입점, 라우팅 및 인증 처리
2. **Auth Service (8081)**: JWT 기반 인증/인가 처리
3. **User Service (8082)**: 사용자 정보 및 프로필 관리
4. **Chat Service (8083)**: 실시간 채팅 및 AI 멀티 에이전트 시스템
5. **Store Service (8084)**: 표현 보관함 기능
6. **Batch Service (8085)**: 스케줄링 작업 및 배치 처리

#### 데이터베이스 구조

- **Shared Database 패턴**: 모든 서비스가 하나의 PostgreSQL 인스턴스를 공유하되, 스키마로 분리
  - `auth_schema`: 인증 관련 데이터
  - `user_schema`: 사용자 정보
  - `chat_schema`: 채팅방 및 메시지
  - `store_schema`: 보관함 데이터
  - `batch_schema`: 배치 작업 데이터

#### 서비스 간 통신

- **동기 통신**: Spring Cloud OpenFeign (HTTP/REST)
- **비동기 통신**: Server-Sent Events (SSE) - 실시간 채팅용
- **회로 차단기**: Resilience4j를 통한 장애 격리

### 1.4 개발 규모

- **총 서비스 수**: 6개 (Gateway, Auth, User, Chat, Store, Batch)
- **총 코드 라인 수**: 약 10,000+ 라인 (Java)
- **데이터베이스 테이블**: 17개 (5개 스키마)
- **API 엔드포인트**: 50+ 개
- **배포 스크립트**: 15+ 개 (PowerShell/Shell)

---

## 2. 구현한 기능

### 2.1 Auth Service

#### JWT 기반 인증/인가 시스템
- **로그인/로그아웃**: 이메일과 비밀번호 기반 인증
- **토큰 생성/검증**: Access Token 및 Refresh Token 발급
- **토큰 갱신**: Refresh Token을 통한 자동 토큰 갱신
- **HMAC 헤더 검증**: 서비스 간 통신 시 보안 강화

#### 이메일 인증 기능
- **이메일 인증 코드 발송**: 회원가입 시 이메일 인증
- **인증 코드 검증**: 6자리 인증 코드 확인
- **비밀번호 재설정**: 이메일을 통한 비밀번호 재설정 링크 발송

#### 주요 구현 내용
```java
// JWT 토큰 생성 예시
public String generateToken(UserDetails userDetails) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("userId", userDetails.getUserId());
    claims.put("email", userDetails.getEmail());
    
    return Jwts.builder()
        .setClaims(claims)
        .setSubject(userDetails.getEmail())
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + JWT_EXPIRATION))
        .signWith(SignatureAlgorithm.HS512, secretKey)
        .compact();
}
```

### 2.2 User Service

#### 사용자 관리
- **회원가입**: 이메일 인증 포함 사용자 등록
- **프로필 조회/수정**: 사용자 정보 관리
- **사용자 검색**: 이메일 기반 사용자 조회

#### 주요 기능
- 사용자 CRUD 작업
- 프로필 이미지 관리
- 사용자 상태 관리 (활성/비활성)

### 2.3 Chat Service (핵심 기능)

Chat Service는 DoranDoran의 핵심 서비스로, 실시간 채팅과 AI 기반 멀티 에이전트 시스템을 제공합니다.

#### 실시간 채팅 (SSE 기반)
- **Server-Sent Events (SSE)**: 실시간 메시지 스트리밍
- **채팅방 관리**: 채팅방 생성, 조회, 삭제
- **메시지 전송/수신**: 텍스트 메시지 처리
- **메시지 히스토리**: 과거 메시지 조회

#### Multi-Agent AI 시스템

여러 AI 에이전트를 병렬로 실행하여 사용자의 대화를 분석하고 응답을 생성합니다.

**1. IntimacyAgent (친밀도 분석)**
- 사용자의 대화 내용을 분석하여 친밀도 레벨(0~3) 감지
- 문법 오류 교정 및 피드백 제공
- 친밀도 진척도 추적 및 저장

**2. VocabularyAgent (어휘 추출)**
- 대화에서 중요한 어휘 추출
- 어휘의 의미 및 사용 예시 제공
- 번역 기능 포함

**3. ConversationAgent (대화 생성)**
- 사용자 메시지에 대한 AI 응답 생성
- 스트리밍 방식으로 응답 전송 (청크 단위)
- 컨텍스트 기반 대화 생성

**4. SummarizerAgent (요약)**
- 대화 내용 요약
- 키워드 추출

#### AI 인사말 자동 발송 (GreetingService)
- 채팅방 생성 시 자동으로 AI 인사말 발송
- 친밀도 레벨에 따른 맞춤형 인사말 생성
- 프롬프트 기반 AI 응답 생성

#### 프롬프트 관리 시스템
- **PromptService**: 동적 프롬프트 생성
- 컨셉, 친밀도 레벨, 채팅방 컨텍스트를 반영한 시스템 프롬프트 생성
- Redis 캐싱을 통한 성능 최적화

#### 주요 구현 패턴

```java
// Multi-Agent Orchestrator 예시
public void processUserMessage(UUID chatroomId, UUID userId, Message userMessage) {
    // Phase 1: 병렬 실행
    Mono<IntimacyAgentResponse> intimacyMono = intimacyAgent.analyze(chatroomId, content);
    Mono<VocabularyAgentResponse> vocabularyMono = vocabularyAgent.extract(chatroomId, content);
    
    // Phase 2: Conversation (독립적 스트림)
    conversationAgent.generateResponse(chatroomId, content)
        .subscribe(chunk -> {
            sseManager.send(chatroomId, "conversation_chunk", chunk);
        });
}
```

### 2.4 Store Service

#### 표현 보관함 기능
- **표현 저장**: 사용자가 좋아하는 표현을 보관함에 저장
- **보관함 조회**: 저장된 표현 목록 조회 (페이징 지원)
- **방별 조회**: 특정 채팅방의 표현만 조회
- **표현 삭제**: 소프트 삭제 방식으로 표현 제거

#### 성능 최적화
- **Redis 캐싱**: 채팅방 이름 조회 결과 캐싱 (TTL: 10분)
- **Feign Client**: Chat Service와의 통신을 통한 채팅방 정보 조회
- **예외 처리**: FeignException 처리 및 폴백 메시지 제공

#### 주요 구현 내용
```java
// Redis 캐싱을 통한 채팅방 이름 조회
private String getChatroomNameWithCache(UUID chatroomId, UUID userId) {
    String cacheKey = CHATROOM_NAME_PREFIX + chatroomId;
    String cachedName = redisTemplate.opsForValue().get(cacheKey);
    
    if (cachedName != null) {
        return cachedName; // Cache HIT
    }
    
    // Cache MISS - Feign Client로 조회
    ChatRoomDto chatRoom = chatServiceClient.getChatRoom(chatroomId, userId, true);
    String chatroomName = chatRoom != null ? chatRoom.getName() : "Deleted Room";
    
    // 캐시 저장
    redisTemplate.opsForValue().set(cacheKey, chatroomName, CACHE_TTL);
    return chatroomName;
}
```

### 2.5 Gateway Service

#### API 라우팅 및 로드밸런싱
- **경로 기반 라우팅**: `/api/auth/**`, `/api/users/**` 등 경로별 서비스 라우팅
- **Retry 메커니즘**: GET 요청에 대한 자동 재시도 (최대 2회)
- **Backoff 전략**: 지수 백오프를 통한 재시도 간격 조정

#### JWT 인증 필터
- **JwtAuthFilter**: 모든 요청에 대한 JWT 토큰 검증
- **제외 경로**: `/api/auth/login`, `/api/users` 등 공개 API 제외
- **HMAC 헤더 주입**: 인증된 요청에 HMAC 헤더 추가

#### IP 블랙리스트 필터
- **IpBlacklistFilter**: 차단된 IP 주소 자동 차단 (HTTP 403)
- **Redis 기반 저장**: 블랙리스트 IP를 Redis에 저장
- **관리자 API 제외**: `/api/admin/blacklist` 경로는 블랙리스트 체크 제외

#### CORS 처리
- **CorsWebFilter**: 모든 요청에 CORS 헤더 추가
- **CorsResponseFilter**: SSE 요청에 대한 추가 CORS 처리

#### SSE 지원
- **긴 타임아웃 설정**: SSE 스트림을 위한 10분 응답 타임아웃
- **연결 풀 최적화**: 최대 500개 동시 연결 지원

### 2.6 Batch Service

#### 스케줄링 작업
- **Cron 기반 스케줄링**: Spring @Scheduled 어노테이션 사용
- **데이터 처리**: 주기적인 데이터 정리 및 집계
- **리포트 생성**: 통계 데이터 생성

---

## 3. 해결한 주요 문제들

### 3.1 인프라 문제

#### 3.1.1 RDS OOM 문제 (2025-10-14)

**문제 상황**:
- API 응답 500 오류 발생
- 컨테이너 Health `unhealthy` 상태
- `/actuator/health` 엔드포인트 500 반환

**원인 분석**:
- 컨테이너 로그에서 `java.lang.OutOfMemoryError: unable to create native thread` 다수 발생
- Hikari 커넥션 풀 스레드 생성 실패
- JVM/프로세스의 스레드/메모리 한도 초과

**해결 방법**:
1. **JVM 메모리 옵션 조정**:
   ```bash
   JAVA_TOOL_OPTIONS="-Xms256m -Xmx512m -XX:MaxDirectMemorySize=96m -XX:MaxMetaspaceSize=128m -XX:MaxRAMPercentage=60"
   ```

2. **Tomcat 스레드 제한**:
   ```bash
   SERVER_TOMCAT_THREADS_MAX=40
   SERVER_TOMCAT_ACCEPT_COUNT=50
   ```

3. **Hikari 커넥션 풀 크기 조정**:
   ```bash
   SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=5
   ```

**결과**:
- 컨테이너 Health `healthy` 상태로 복구
- `/actuator/health` 정상 응답
- API 정상화

**교훈**:
- JVM/풀 기본값은 환경에 따라 과도할 수 있음
- 명시적으로 리소스 한도를 설정하는 것이 중요
- 헬스체크 실패는 오케스트레이션에 악영향을 미침

#### 3.1.2 Connection Pool Exhaustion (2025-10-14)

**문제 상황**:
- 503 Service Unavailable 오류 발생
- 모든 API 요청 30초 타임아웃 후 실패
- HikariCP 연결 풀 고갈 (10개 연결 모두 사용 중)

**원인 분석**:
- **핵심 원인**: Connection Leak (연결 누수)
- **트리거**: SSE + `spring.jpa.open-in-view=true` 조합
  - SSE 스트림이 오래 유지되는 동안 DB 연결도 계속 점유
  - 타임아웃 발생 시 비정상 종료 → 연결 반환 지연
  - 10개 연결이 모두 누수되어 새 요청 처리 불가

**해결 방법**:
1. **open-in-view 비활성화**:
   ```bash
   SPRING_JPA_OPEN_IN_VIEW=false
   ```
   - HTTP 요청 전체 생명주기 동안 DB 연결 유지 → 트랜잭션 종료 시 즉시 연결 반환

2. **연결 풀 최적화**:
   ```bash
   SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=20  # 10 → 20
   SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE=5         # 0 → 5
   SPRING_DATASOURCE_HIKARI_LEAK_DETECTION_THRESHOLD=10000  # 10초
   SPRING_DATASOURCE_HIKARI_MAX_LIFETIME=600000    # 30분 → 10분
   ```

**결과**:
- SSE 스트림이 DB 연결을 불필요하게 점유하지 않음
- 연결 풀 20개로 여유 확보
- 누수 10초 이상 시 즉시 감지 및 경고

**교훈**:
- Long-running 요청에서는 `open-in-view=false` 필수
- 연결 생명주기 관리가 중요
- 연결 풀 모니터링 설정 필수

#### 3.1.3 RDS → Local DB 마이그레이션

**배경**:
- AWS RDS Aurora 사용으로 인한 높은 비용 ($59.04/월)
- 비용 절감을 위한 로컬 PostgreSQL 컨테이너 전환

**마이그레이션 과정**:
1. **데이터 백업**: RDS Aurora 데이터 덤프 (386KB)
2. **컨테이너 교체**: `shared-db` → `dorandoran-shared-db`
3. **PostgreSQL 업그레이드**: 15 → 17
4. **데이터 복원**: 5개 스키마, 17개 테이블 복원
5. **배포 스크립트 수정**: 모든 서비스의 DB 연결을 localhost로 변경

**비용 절감 효과**:
- **Before**: $118-143/월 (RDS 포함)
- **After**: $50-64/월 (로컬 DB)
- **절감액**: 월 $58-79 (50-60% 절감)
- **연간 절감**: $696-948

**주의사항**:
- 리소스 사용량 증가 (PostgreSQL 메모리: ~500MB-1GB)
- 동시 사용자 수 소폭 감소 (80-120명 → 70-100명)
- 백업 전략 수립 필요

### 3.2 코드 레벨 문제

#### 3.2.1 Hibernate 프록시 순환 참조 문제

**문제 상황**:
- 애플리케이션 실행 중 `NullPointerException`과 스택 오버플로우 발생
- 로그에서 `User$HibernateProxy$Dyp9SYgE.toString` 무한 반복 확인
- `greeting_guide_message` SSE 이벤트 전송 실패

**원인 분석**:
1. **양방향 관계 설정**:
   ```java
   // User 엔티티
   @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
   private List<ChatRoom> chatRooms;
   
   // ChatRoom 엔티티
   @ManyToOne(fetch = FetchType.LAZY)
   private User user;
   ```

2. **Lombok @Data의 toString() 생성 문제**:
   - `@Data` 어노테이션이 자동으로 생성하는 `toString()` 메서드에서 순환 참조 발생
   - `User.toString()` → `chatRooms` → `ChatRoom.toString()` → `user` → 무한 루프

**해결 방법**:
```java
// User 엔티티
@ToString(exclude = {"chatRooms"})  // 순환 참조 방지
public class User {
    private List<ChatRoom> chatRooms = new ArrayList<>();
}

// ChatRoom 엔티티
@ToString(exclude = {"user", "chatbot", "messages", "lastMessage"})
public class ChatRoom {
    private User user;
    private Chatbot chatbot;
    private List<Message> messages;
    private Message lastMessage;
}
```

**결과**:
- 순환 참조 문제 해결
- 불필요한 연관 객체 로딩 방지
- 메모리 사용량 감소

**교훈**:
- 양방향 관계에서 `toString()` 주의 필요
- Lombok `@Data` 사용 시 순환 참조 가능성 고려
- `@ToString(exclude = {...})` 활용 권장

#### 3.2.2 GreetingService 프롬프트 개선

**문제 상황**:
- `greeting_guide_message` SSE 이벤트가 클라이언트에 전달되지 않음
- AI 응답 파싱 실패로 인한 `NullPointerException` 발생
- 프롬프트 JSON 형식 불일치

**원인 분석**:
1. **프롬프트 JSON 형식 오류**: 따옴표 없는 필드 값
2. **불필요한 필드 요구**: `intimacyLevel`, `detectedLevel` (입력 파라미터이므로 출력 불필요)
3. **예시 시나리오 불일치**: 잘못된 JSON 형식 예시

**해결 방법**:
1. **JSON 형식 통일**:
   ```java
   // 모든 프롬프트에서 동일한 형식 사용
   {
       "botMessage": "인트로 메시지",
       "guideMessage": "대화 문구를 제안"
   }
   ```

2. **예시 시나리오 수정**: 올바른 JSON 형식 예시 제공

3. **null 체크 및 안전성 강화**:
   ```java
   private GreetingResponse parseAIResponse(String aiResponse) {
       JsonNode jsonNode = objectMapper.readTree(aiResponse);
       String botMessage = jsonNode.get("botMessage").asText();
       String guideMessage = jsonNode.get("guideMessage").asText();
       
       if (botMessage == null || guideMessage == null) {
           throw new RuntimeException("AI 응답에 null 값이 포함됨");
       }
       
       return new GreetingResponse(botMessage, guideMessage);
   }
   ```

**결과**:
- AI가 올바른 JSON 형식으로 응답
- 파싱 성공으로 정상적인 `GreetingResponse` 생성
- `greeting_bot_message`와 `greeting_guide_message` 이벤트 모두 정상 전송

### 3.3 보안 문제

#### 3.3.1 IP 블랙리스트 시스템 구현

**문제 상황**:
- 의심 IP (172.104.24.172)로부터 비정상 HTTP 요청 다수 발생
- RTSP/1.0, SIP/2.0 등 비정상 프로토콜 요청
- 보안 스캔/공격 시도 가능성

**해결 방법**:
1. **IpBlacklistFilter 구현**:
   ```java
   @Component
   public class IpBlacklistFilter implements GlobalFilter, Ordered {
       @Override
       public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
           String clientIp = getClientIp(exchange);
           
           if (isBlacklisted(clientIp) && !isExcludedPath(exchange.getRequest().getPath().value())) {
               return createForbiddenResponse(exchange);
           }
           
           return chain.filter(exchange);
       }
   }
   ```

2. **Redis 기반 저장**: 블랙리스트 IP를 Redis에 저장

3. **관리자 API 구현**: `/api/admin/blacklist` 엔드포인트를 통한 IP 관리

**결과**:
- 차단된 IP는 HTTP 403으로 즉시 차단
- 관리자는 자신의 IP가 차단되어 있어도 블랙리스트 관리 가능
- 보안 위협 감소

#### 3.3.2 SecurityConfig 정책 일관성 개선

**문제 상황**:
- `SecurityConfig`: 모든 `/api/**` 경로를 `permitAll()`로 설정
- `JwtAuthFilter`: 제외 목록에 없는 경로는 JWT 인증 필요
- 정책 불일치로 인한 혼란

**해결 방법**:
```java
// SecurityConfig 수정
.pathMatchers("/actuator/**").permitAll()
.pathMatchers("/api/auth/login").permitAll()
.pathMatchers("/api/users").permitAll()  // POST 회원가입
.pathMatchers("/api/admin/**").authenticated()  // 관리자 API는 인증 필요
.pathMatchers("/api/**").authenticated()  // 나머지는 인증 필요
```

**결과**:
- JwtAuthFilter의 정책과 일치
- SecurityConfig의 의도 명확화
- 향후 Spring Security 기능 확장 시 일관성 유지

---

## 4. 인프라 및 배포

### 4.1 인프라 구성

#### AWS EC2 기반 배포
- **인스턴스 타입**: t3.medium
- **운영체제**: Amazon Linux 2
- **네트워크**: VPC 내부 배포

#### Docker 컨테이너화
- **모든 서비스 컨테이너화**: 각 서비스별 Dockerfile 작성
- **Docker Network**: `dorandoran-network`를 통한 서비스 간 통신
- **컨테이너 이름 규칙**: `dorandoran-{service-name}`

#### 데이터베이스 구성
- **PostgreSQL**: Shared Database 패턴
  - 컨테이너 이름: `dorandoran-shared-db`
  - 포트: 5432
  - 버전: PostgreSQL 17
  - 스키마 분리: 5개 스키마 (auth, user, chat, store, batch)

#### Redis 구성
- **컨테이너 이름**: `dorandoran-redis`
- **포트**: 6379
- **용도**: 캐싱 및 세션 관리

### 4.2 배포 자동화

#### PowerShell/Shell 배포 스크립트
- **서비스별 개별 배포 스크립트**:
  - `deploy-auth-service.ps1`
  - `deploy-user-service.ps1`
  - `deploy-chat-service.ps1`
  - `deploy-store-service.ps1`
  - `deploy-gateway-service.ps1`
  - `deploy-batch-service.ps1`

#### 배포 스크립트 주요 기능
1. **이미지 빌드**: Gradle 빌드 후 Docker 이미지 생성
2. **컨테이너 중지/제거**: 기존 컨테이너 정리
3. **컨테이너 실행**: 환경변수 설정 및 네트워크 연결
4. **Health Check**: `/actuator/health` 엔드포인트 확인

#### 배포 스크립트 예시
```powershell
# Chat Service 배포 스크립트 예시
docker stop dorandoran-chat
docker rm dorandoran-chat

docker run -d --name dorandoran-chat \
  --network dorandoran-network \
  -p 8083:8083 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://dorandoran-shared-db:5432/dorandoran \
  -e SPRING_DATASOURCE_USERNAME=doran \
  -e SPRING_DATASOURCE_PASSWORD=$DB_PASSWORD \
  -e SPRING_REDIS_HOST=dorandoran-redis \
  dorandoran-chat:latest

# Health Check
Start-Sleep -Seconds 10
$health = Invoke-RestMethod -Uri "http://localhost:8083/actuator/health"
Write-Host "Health Status: $($health.status)"
```

### 4.3 모니터링

#### Grafana 대시보드
- **URL**: http://localhost:3000
- **기본 계정**: admin/admin
- **대시보드**: 서비스별 메트릭 시각화

#### Prometheus 메트릭 수집
- **URL**: http://localhost:9090
- **수집 주기**: 15초
- **메트릭 종류**:
  - HTTP 요청 수 및 응답 시간
  - 데이터베이스 연결 풀 상태
  - JVM 메모리 사용량
  - CPU 사용률

#### 로그 분석 스크립트
- **`check_bottleneck.sh`**: 서비스별 병목 현상 체크
- **로그 분석**: Docker logs를 통한 에러 추적
- **리소스 모니터링**: CPU, 메모리, 네트워크 사용량 추적

#### 모니터링 지표
- **서비스 상태**: Health Check 엔드포인트 모니터링
- **에러율**: 5분/24시간 단위 에러 발생 추적
- **응답 시간**: 평균 응답 시간 측정
- **리소스 사용률**: CPU, 메모리, 연결 풀 사용률

---

## 5. 성능 최적화

### 5.1 Redis 캐싱 전략

#### ChatRoom 캐싱
- **키 형식**: `chatroom::{chatroomId}`
- **TTL**: 1800초 (30분)
- **용도**: 채팅방 정보 조회 성능 향상
- **무효화**: 채팅방 수정/삭제 시 캐시 제거

```java
@Cacheable(value = "chatrooms", key = "#chatroomId", unless = "#result == null")
public ChatRoom getChatRoomById(UUID chatroomId) {
    return chatRoomRepository.findById(chatroomId)
        .orElseThrow(() -> new ChatRoomNotFoundException());
}
```

#### Prompt 캐싱
- **키 형식**: `prompts::{chatroomId}`
- **TTL**: 3600초 (1시간)
- **용도**: 시스템 프롬프트 생성 비용 절감
- **효과**: 프롬프트 생성 시간 단축

#### IntimacyProgress 캐싱
- **키 형식**: `intimacy::{chatroomId}`
- **TTL**: 1800초 (30분)
- **용도**: 친밀도 진척도 조회 성능 향상
- **Write-Through 패턴**: 업데이트 시 DB와 캐시 동시 갱신

#### Store Service 채팅방 이름 캐싱
- **키 형식**: `chatroom:name::{chatroomId}`
- **TTL**: 600초 (10분)
- **용도**: Feign Client 호출 비용 절감
- **폴백**: 404 오류 시 "Deleted Room"으로 캐시하여 반복 호출 방지

### 5.2 데이터베이스 최적화

#### 연결 풀 최적화 (HikariCP)
- **최대 연결 수**: 서비스별 최적화
  - Auth: 30개
  - User: 30개
  - Chat: 50개 (높은 동시성)
  - Store: 20개
- **최소 유휴 연결**: 5개 (응답성 향상)
- **연결 수명**: 10분 (주기적 갱신)
- **누수 감지**: 10초 이상 점유 시 경고

#### N+1 문제 해결
- **Fetch Join 사용**: 연관 엔티티 한 번에 조회
- **@EntityGraph 활용**: 필요한 연관 관계만 로딩
- **DTO 변환**: 엔티티 직접 반환 대신 DTO 사용

#### 인덱스 최적화
- **기본 키 인덱스**: UUID 기반 기본 키
- **외래 키 인덱스**: 연관 관계 조회 성능 향상
- **조회 조건 인덱스**: 자주 사용되는 조회 조건에 인덱스 추가

### 5.3 비동기 처리

#### Reactor 기반 비동기 처리
- **WebFlux 사용**: Chat Service의 SSE 스트리밍
- **Mono/Flux 체인**: 비동기 파이프라인 구성
- **Scheduler 분리**: CPU 집약적 작업과 I/O 작업 분리

#### Multi-Agent 병렬 처리
- **병렬 실행**: IntimacyAgent, VocabularyAgent 동시 실행
- **독립적 스트림**: ConversationAgent는 별도 스트림으로 실행
- **결과 캐싱**: `Mono.cache()`를 통한 중복 호출 방지

```java
// 병렬 처리 예시
Mono<IntimacyAgentResponse> intimacyMono = intimacyAgent.analyze(chatroomId, content).cache();
Mono<VocabularyAgentResponse> vocabularyMono = vocabularyAgent.extract(chatroomId, content).cache();

// 즉시 구독하여 SSE 전송 및 progress 업데이트 실행
intimacyMono.subscribe();
vocabularyMono.subscribe();
```

---

## 6. 보안 구현

### 6.1 JWT 토큰 기반 인증

#### 토큰 구조
- **Access Token**: 짧은 수명 (예: 1시간)
- **Refresh Token**: 긴 수명 (예: 7일)
- **HMAC 서명**: HS512 알고리즘 사용

#### 토큰 검증 프로세스
1. Gateway의 `JwtAuthFilter`에서 토큰 검증
2. Auth Service와 통신하여 토큰 유효성 확인
3. 유효한 토큰인 경우 HMAC 헤더 주입
4. 백엔드 서비스에서 HMAC 헤더 검증

### 6.2 IP 블랙리스트 필터

#### 구현 내용
- **GlobalFilter**: Gateway 필터 체인에서 가장 먼저 실행 (Order: -100)
- **Redis 저장**: 블랙리스트 IP를 Redis에 저장
- **제외 경로**: `/actuator/**`, `/api/admin/blacklist` 제외

#### 관리자 API
- **GET `/api/admin/blacklist`**: 블랙리스트 IP 목록 조회
- **POST `/api/admin/blacklist`**: IP 추가
- **DELETE `/api/admin/blacklist/{ip}`**: IP 제거

### 6.3 Security Group 최적화

#### AWS Security Group 규칙
- **SSH (22/tcp)**: 관리자 IP만 허용
- **HTTP (80/tcp)**: 모든 IP 허용 (필요 시)
- **HTTPS (443/tcp)**: 모든 IP 허용 (필요 시)
- **애플리케이션 포트**: 내부 네트워크만 허용

### 6.4 HMAC 헤더 검증

#### 목적
- 서비스 간 통신 시 추가 보안 계층 제공
- 위조된 요청 방지

#### 구현
```java
// Gateway에서 HMAC 헤더 주입
String hmac = generateHMAC(requestBody, secretKey);
exchange.getRequest().mutate()
    .header("X-HMAC", hmac)
    .build();

// 백엔드 서비스에서 검증
String receivedHmac = request.getHeader("X-HMAC");
if (!isValidHMAC(requestBody, receivedHmac, secretKey)) {
    throw new UnauthorizedException();
}
```

---

## 7. 개발 도구 및 스크립트

### 7.1 배포 스크립트

#### PowerShell 스크립트
- **`deploy-auth-service.ps1`**: Auth Service 배포
- **`deploy-user-service.ps1`**: User Service 배포
- **`deploy-chat-service.ps1`**: Chat Service 배포
- **`deploy-store-service.ps1`**: Store Service 배포
- **`deploy-gateway-service.ps1`**: Gateway Service 배포
- **`deploy-batch-service.ps1`**: Batch Service 배포
- **`deploy-all-services.ps1`**: 모든 서비스 일괄 배포

#### Shell 스크립트
- **`deploy-aws.sh`**: AWS 환경 배포
- **`deploy-simple.sh`**: 간단한 배포 스크립트
- **`deploy-chat-only.sh`**: Chat Service만 배포

### 7.2 모니터링 스크립트

#### `check_bottleneck.sh`
- 서비스별 병목 현상 체크
- 데이터베이스 연결 풀 상태 확인
- 메모리 사용량 추적
- 에러 로그 분석

### 7.3 보안 스크립트

#### `block-ip-aws.ps1`
- AWS Security Group을 통한 IP 차단
- 의심 IP 자동 차단

#### `monitor-suspicious-ips.sh`
- 의심스러운 IP 모니터링
- 비정상 요청 패턴 감지

### 7.4 테스트 스크립트

#### `test-api-flow.ps1` / `test-api-flow.sh`
- 전체 API 플로우 테스트
- 인증 → 사용자 조회 → 채팅방 생성 → 메시지 전송

#### `test-cache-performance.ps1`
- Redis 캐시 성능 테스트
- 캐시 히트율 측정

#### `test-integration.ps1`
- 서비스 간 통신 테스트
- Feign Client 동작 확인

### 7.5 유틸리티 스크립트

#### `update_bot_prompts.ps1` / `update_bot_prompts.py`
- 챗봇 프롬프트 일괄 업데이트
- Excel 파일 기반 프롬프트 관리

#### `migrate-to-local-db.ps1`
- RDS에서 로컬 DB로 마이그레이션
- 데이터 백업 및 복원

---

## 8. 교훈 및 개선 사항

### 8.1 배운 점

#### 마이크로서비스 아키텍처 설계 경험
- **서비스 분리 원칙**: 도메인별로 서비스를 분리하여 독립적 개발/배포 가능
- **공통 모듈 관리**: `shared`, `common` 모듈을 통한 코드 재사용
- **서비스 간 통신**: Feign Client를 통한 동기 통신, SSE를 통한 비동기 통신

#### SSE와 JPA 조합 시 주의사항
- **open-in-view=false 필수**: Long-running 요청에서는 DB 연결을 즉시 반환해야 함
- **연결 풀 모니터링**: 연결 누수 감지 설정 필수
- **타임아웃 관리**: SSE 스트림 타임아웃과 DB 연결 타임아웃 조정 필요

#### 연결 풀 관리의 중요성
- **적절한 풀 크기**: 서비스별 트래픽에 맞는 연결 풀 크기 설정
- **누수 감지**: `LEAK_DETECTION_THRESHOLD` 설정으로 조기 발견
- **연결 수명 관리**: `MAX_LIFETIME` 설정으로 주기적 갱신

#### 캐싱 전략의 효과
- **성능 향상**: Redis 캐싱을 통한 응답 시간 단축
- **비용 절감**: Feign Client 호출 비용 절감
- **TTL 관리**: 적절한 TTL 설정으로 데이터 일관성 유지

### 8.2 개선이 필요한 부분

#### SecurityConfig 정책 일관성
- **현재 상태**: JwtAuthFilter와 SecurityConfig 정책 불일치
- **개선 방안**: SecurityConfig를 기준으로 JwtAuthFilter 제외 목록 동기화
- **우선순위**: 중간

#### 비동기 처리 패턴 표준화
- **현재 상태**: 일부 코드에서 `block()` 호출 사용 (Reactor 비동기 스레드에서 문제 발생)
- **개선 방안**: 모든 비동기 처리를 Mono/Flux 체인으로 변경
- **우선순위**: 높음

#### 모니터링 알림 시스템
- **현재 상태**: 수동 로그 확인
- **개선 방안**: Slack, Email 알림 시스템 구축
- **우선순위**: 중간

#### 테스트 커버리지 향상
- **현재 상태**: 단위 테스트 부족
- **개선 방안**: 서비스별 단위 테스트 작성, 통합 테스트 추가
- **우선순위**: 낮음

#### 관리자 권한 체크
- **현재 상태**: JWT 토큰만 있으면 관리자 API 접근 가능
- **개선 방안**: JWT 토큰에서 역할(role) 클레임 확인, `ROLE_ADMIN` 권한 체크
- **우선순위**: 중간

---

## 9. 프로젝트 통계

### 9.1 사용자 및 활동 통계

- **총 등록 사용자**: 115명
- **총 채팅방 수**: 237개
- **Gateway 요청 수 (24시간)**: 약 410건
- **Auth Service 요청 수 (24시간)**: 589건
  - 로그인: 159건 (평균 응답 시간 0.130초)
  - 사용자 정보 조회: 430건 (평균 응답 시간 0.009초)

### 9.2 서비스 구성

- **총 서비스 수**: 6개
  - Gateway Service
  - Auth Service
  - User Service
  - Chat Service
  - Store Service
  - Batch Service

- **데이터베이스 스키마**: 5개
  - auth_schema
  - user_schema
  - chat_schema
  - store_schema
  - batch_schema

- **총 테이블 수**: 17개

### 9.3 해결한 문제 통계

- **인프라 문제**: 3건
  - RDS OOM 문제
  - Connection Pool Exhaustion
  - RDS → Local DB 마이그레이션

- **코드 레벨 문제**: 2건
  - Hibernate 프록시 순환 참조
  - GreetingService 프롬프트 개선

- **보안 문제**: 2건
  - IP 블랙리스트 시스템 구현
  - SecurityConfig 정책 일관성 개선

**총 해결한 주요 문제**: 7건 이상

### 9.4 비용 절감

- **RDS 제거**: 월 $59.04 절감
- **전체 비용 절감**: 월 $58-79 (50-60% 절감)
- **연간 절감**: $696-948

### 9.5 리소스 사용 현황

#### 서비스별 리소스 사용률 (정상 운영 시)
- **Chat Service**: CPU 0.22%, 메모리 547.3MB (14.30%)
- **Store Service**: CPU 0.16%, 메모리 484.6MB (12.66%)
- **Auth Service**: CPU 0.12%, 메모리 439.2MB (11.48%)
- **User Service**: CPU 0.09%, 메모리 328.5MB (8.58%)
- **Gateway Service**: CPU 0.13%, 메모리 268.9MB (7.02%)
- **PostgreSQL**: 메모리 144.6MB (3.78%)
- **Redis**: CPU 0.47%, 메모리 5.059MB (0.13%)

**전체 리소스 사용률**: 정상 범위 내

### 9.6 데이터베이스 연결 풀 상태

- **Auth Service**: 0/30 (0% 사용률)
- **Chat Service**: 0/50 (0% 사용률)
- **User Service**: 0/30 (0% 사용률)
- **Store Service**: 0/20 (0% 사용률)

**연결 풀 상태**: 모든 서비스 정상

---

## 결론

DoranDoran 프로젝트를 통해 마이크로서비스 아키텍처 설계 및 구현, 실시간 채팅 시스템 구축, AI 기반 멀티 에이전트 시스템 개발, 클라우드 인프라 운영 등 다양한 경험을 쌓을 수 있었습니다.

특히 인프라 문제 해결 과정에서 연결 풀 관리의 중요성, SSE와 JPA 조합 시 주의사항, 캐싱 전략의 효과 등을 배울 수 있었고, 이를 통해 안정적이고 성능이 우수한 시스템을 구축할 수 있었습니다.

앞으로는 비동기 처리 패턴 표준화, 모니터링 알림 시스템 구축, 테스트 커버리지 향상 등의 개선 사항을 통해 더욱 견고한 시스템을 만들어 나가겠습니다.

---

**작성자**: AI Assistant  
**최종 수정일**: 2025년 11월

