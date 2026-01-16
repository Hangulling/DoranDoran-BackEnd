# DoranDoran 프로젝트 아키텍처 분석 문서

> 면접 대비를 위한 프로젝트 전체 분석 문서

## 목차

1. [프로젝트 개요](#1-프로젝트-개요)
2. [전체 아키텍처](#2-전체-아키텍처)
3. [서비스별 상세 분석](#3-서비스별-상세-분석)
4. [기술 스택 및 라이브러리](#4-기술-스택-및-라이브러리)
5. [데이터베이스 설계](#5-데이터베이스-설계)
6. [인증/인가 흐름](#6-인증인가-흐름)
7. [실시간 통신 구현](#7-실시간-통신-구현)
8. [멀티 에이전트 시스템](#8-멀티-에이전트-시스템)
9. [성능 최적화](#9-성능-최적화)
10. [장애 처리](#10-장애-처리)
11. [배포 및 운영](#11-배포-및-운영)
12. [면접 대비 핵심 포인트](#12-면접-대비-핵심-포인트)

---

## 1. 프로젝트 개요

### 1.1 프로젝트 소개

**DoranDoran**은 마이크로서비스 아키텍처 기반의 한국어 학습 챗봇 플랫폼입니다. 외국인 사용자가 AI 챗봇과 대화하며 한국어 친밀도(존댓말/반말)를 학습하고, 어휘를 습득할 수 있는 서비스를 제공합니다.

### 1.2 주요 특징

- **마이크로서비스 아키텍처**: 독립적인 서비스들로 구성 (Gateway, Auth, User, Chat, Store, Batch)
- **실시간 통신**: SSE(Server-Sent Events) 기반 실시간 메시지 스트리밍
- **멀티 에이전트 시스템**: 친밀도 분석, 어휘 추출, 대화 생성, 요약 등 여러 AI 에이전트의 협업
- **JWT 기반 인증**: Access Token + Refresh Token 패턴
- **서비스 간 보안**: HMAC 서명 기반 인증
- **모니터링**: Prometheus + Grafana 기반 시스템 모니터링

### 1.3 기술 스택 요약

**백엔드**
- Java 21, Spring Boot 3.3.4
- Spring Cloud Gateway (Reactive)
- Spring Data JPA, PostgreSQL
- Redis (캐싱, 토큰 블랙리스트)
- Reactor (비동기 프로그래밍)
- OpenAI API (GPT-4o)

**프론트엔드**
- React 19, TypeScript
- Vite (빌드 도구)
- Zustand (상태 관리)
- React Query (서버 상태 관리)
- Tailwind CSS, DaisyUI

**인프라**
- Docker, Docker Compose
- PostgreSQL 17
- Redis 7
- Prometheus, Grafana, Loki

---

## 2. 전체 아키텍처

### 2.1 시스템 아키텍처 다이어그램

```
┌─────────────────────────────────────────────────────────────┐
│                        Client (Browser)                      │
│                    React + TypeScript                         │
└───────────────────────────┬─────────────────────────────────┘
                            │ HTTPS
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                    API Gateway (8080)                       │
│              Spring Cloud Gateway (Reactive)                │
│  - JWT 인증 필터                                            │
│  - HMAC 헤더 주입                                           │
│  - 라우팅                                                   │
│  - CORS 처리                                                │
└───┬──────┬──────┬──────┬──────┬──────┬────────────────────┘
    │      │      │      │      │      │
    ▼      ▼      ▼      ▼      ▼      ▼
┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐
│Auth │ │User │ │Chat │ │Store│ │Batch│ │Redis│
│8081 │ │8082 │ │8083 │ │8084 │ │8085 │ │6379 │
└──┬──┘ └──┬──┘ └──┬──┘ └──┬──┘ └──┬──┘ └──┬──┘
   │       │       │       │       │       │
   └───────┴───────┴───────┴───────┴───────┘
                    │
                    ▼
         ┌──────────────────────┐
         │   PostgreSQL (5432)  │
         │  - auth_schema       │
         │  - user_schema       │
         │  - chat_schema       │
         │  - store_schema      │
         │  - batch_schema      │
         └──────────────────────┘
```

### 2.2 서비스 포트 구성

| 서비스 | 포트 | 역할 |
|--------|------|------|
| Gateway | 8080 | API 게이트웨이, 모든 외부 요청 진입점 |
| Auth | 8081 | 인증/인가, JWT 토큰 관리 |
| User | 8082 | 사용자 정보 관리 |
| Chat | 8083 | 채팅, 멀티 에이전트 오케스트레이션 |
| Store | 8084 | 표현 보관함 관리 |
| Batch | 8085 | 스케줄링된 배치 작업 |

### 2.3 통신 패턴

- **동기 통신**: HTTP/REST (서비스 간 Feign Client)
- **비동기 통신**: Reactor Mono/Flux (Chat Service 내부)
- **실시간 통신**: SSE (Server-Sent Events)
- **서비스 간 인증**: HMAC 서명 기반

---

## 3. 서비스별 상세 분석

### 3.1 Gateway Service (포트: 8080)

#### 기술 스택
- **Spring Cloud Gateway 4.1.0** (Reactive)
- **Netty** (비동기 네트워크 프레임워크)
- **WebFlux** (Reactive Web)

#### 주요 기능

**1. JWT 인증 필터 (`JwtAuthFilter`)**

```java
// Gateway에서 JWT 토큰 검증 후 HMAC 헤더 주입
public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    // 1. Authorization 헤더에서 토큰 추출
    String token = authHeader.substring(7);
    
    // 2. Auth 서비스를 통한 토큰 검증
    return validateTokenWithAuthService(token, exchange, chain)
        .flatMap(response -> {
            // 3. JWT 페이로드에서 사용자 정보 추출
            String userId = extractJsonValue(payloadJson, "sub");
            String email = extractJsonValue(payloadJson, "email");
            
            // 4. HMAC 서명 생성 및 헤더 주입
            String hmacSignature = generateHmacSignature(hmacSecret, message);
            return addHmacHeadersAndContinue(token, exchange, chain);
        });
}
```

**구현 상세:**
- JWT 토큰을 Auth 서비스(`/api/auth/validate`)로 전달하여 검증
- 검증 성공 시 JWT 페이로드를 파싱하여 사용자 정보 추출
- `userId|timestamp` 형식으로 HMAC-SHA256 서명 생성
- `X-User-Id`, `X-Auth-Ts`, `X-Auth-Sign` 헤더를 하위 서비스로 전달

**2. 라우팅 설정**

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: auth-service
          uri: http://dorandoran-auth:8081
          predicates:
            - Path=/api/auth/**
          filters:
            - RewritePath=/api/auth/(?<segment>.*), /api/auth/${segment}
            - name: Retry
              args:
                retries: 2
                methods: GET
                backoff:
                  firstBackoff: 200ms
                  factor: 2
                  maxBackoff: 500ms
```

**특징:**
- Path 기반 라우팅
- Retry 필터로 일시적 장애 대응
- SSE 스트리밍 지원 (600초 타임아웃)

**3. IP 블랙리스트 필터 (`IpBlacklistFilter`)**

- 공격 IP 자동 차단
- 설정 파일 기반 IP 목록 관리

**4. CORS 처리**

- `CorsResponseFilter`로 CORS 헤더 추가
- 개발/프로덕션 환경별 Origin 허용 목록 관리

#### 성능 최적화

- **Reactive WebFlux**: 논블로킹 I/O로 높은 동시성 처리
- **Connection Pool**: 최대 500개 연결, Elastic 타입
- **타임아웃 설정**: SSE용 600초, 일반 요청 45초

---

### 3.2 Auth Service (포트: 8081)

#### 기술 스택
- **Spring Boot 3.3.4**
- **JWT (jjwt 0.12.3)**: 토큰 생성/검증
- **Redis**: 토큰 블랙리스트, 이메일 인증 토큰
- **PostgreSQL**: Refresh Token, 인증 이벤트 저장
- **Feign Client**: User 서비스 통신
- **Resilience4j**: Circuit Breaker

#### 주요 기능

**1. JWT 토큰 생성/검증 (`JwtService`)**

```java
public String generateAccessToken(String userId, String email, String name) {
    return Jwts.builder()
        .setSubject(userId)
        .claim("email", email)
        .claim("name", name)
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
        .signWith(getSignInKey(), SignatureAlgorithm.HS256)
        .compact();
}
```

**토큰 구성:**
- **Access Token**: 1시간 유효, 사용자 정보 포함
- **Refresh Token**: 7일 유효, `type: "refresh"` 클레임 포함

**토큰 검증 프로세스:**
1. 만료 시간 확인
2. Redis 블랙리스트 확인 (로그아웃된 토큰)
3. 서명 검증

**2. 로그인 프로세스 (`AuthService`)**

```java
public LoginResponse login(LoginRequest request) {
    // 1. User 서비스에서 사용자 정보 조회 (Feign Client)
    UserWithPasswordDto user = userIntegrationService.getUserByEmailForAuth(request.getEmail());
    
    // 2. 비밀번호 검증
    boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.passwordHash());
    
    // 3. JWT 토큰 생성
    String accessToken = jwtService.generateAccessToken(user.id().toString(), user.email(), user.name());
    String refreshToken = jwtService.generateRefreshToken(user.id().toString(), user.email(), user.name());
    
    // 4. Refresh Token 저장 (DB)
    refreshTokenService.saveRefreshToken(user.id(), refreshToken);
    
    // 5. 로그인 시도 기록
    recordLoginAttempt(user.id(), request.getEmail(), true);
    
    return new LoginResponse(accessToken, refreshToken);
}
```

**3. 토큰 블랙리스트 (`TokenBlacklistService`)**

- 로그아웃 시 Access Token을 Redis에 저장 (TTL = 토큰 만료 시간)
- 토큰 검증 시 Redis에서 블랙리스트 확인
- 이중 저장: Redis (빠른 조회) + DB (영구 저장)

**4. 이메일 인증**

- 인증 토큰 생성 후 Redis에 저장 (5분 TTL)
- 이메일 발송 (Spring Mail)
- 프론트엔드에서 토큰 검증

**5. Google OAuth 2.0**

- Google API Client 라이브러리 사용
- OAuth 토큰 검증 후 사용자 정보 조회
- 기존 사용자 매칭 또는 신규 가입

**6. HMAC 인증 인터셉터 (`HmacAuthInterceptor`)**

```java
public boolean preHandle(HttpServletRequest request, ...) {
    String userId = request.getHeader("X-User-Id");
    String ts = request.getHeader("X-Auth-Ts");
    String sign = request.getHeader("X-Auth-Sign");
    
    // 1. 타임스탬프 검증 (60초 이내)
    if (Math.abs(now - t) > skewMs) return false;
    
    // 2. HMAC 서명 검증
    String message = userId + "|" + ts;
    String expected = HmacVerifier.hmacSha256Hex(hmacSecret, message);
    if (!expected.equalsIgnoreCase(sign)) return false;
    
    return true;
}
```

**목적**: Gateway에서 주입한 HMAC 헤더를 검증하여 서비스 간 통신 보안 강화

#### Circuit Breaker 설정

```yaml
resilience4j:
  circuitbreaker:
    instances:
      userServiceClient:
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
```

- User 서비스 호출 실패 시 Circuit Breaker로 장애 격리
- Fallback 메서드로 기본 응답 반환

---

### 3.3 User Service (포트: 8082)

#### 기술 스택
- **Spring Boot 3.3.4**
- **Spring Data JPA**
- **PostgreSQL** (user_schema)

#### 주요 기능

**1. 사용자 CRUD**
- 사용자 생성, 조회, 수정, 삭제
- 이메일 중복 검사

**2. 프로필 관리**
- 사용자 프로필 정보 저장 (JSONB)
- 아바타 URL 관리

**3. 설정 관리**
- 사용자별 설정 키-값 저장

**4. Auth 서비스 통합**
- `AuthServiceIntegration`: Auth 서비스와 사용자 정보 동기화
- HMAC 인증 인터셉터로 보호

---

### 3.4 Chat Service (포트: 8083)

#### 기술 스택
- **Spring Boot 3.3.4**
- **Reactor** (Mono/Flux): 비동기 프로그래밍
- **SSE (Server-Sent Events)**: 실시간 스트리밍
- **OpenAI API**: GPT-4o 모델
- **Redis**: 캐싱
- **PostgreSQL** (chat_schema)

#### 주요 기능

**1. 실시간 채팅 (SSE)**

**SSE 연결 생성 (`SSEController`)**

```java
@GetMapping(value = "/stream/{chatroomId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public ResponseEntity<SseEmitter> stream(@PathVariable UUID chatroomId) {
    // 1. 사용자 인증 확인
    UUID uid = extractUserIdFromSecurityContext();
    
    // 2. 채팅방 접근 권한 확인
    if (!chatRoomRepository.existsByUser_IdAndIdAndIsDeletedFalse(uid, chatroomId)) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    
    // 3. SSE Emitter 생성
    SseEmitter emitter = sseManager.create(chatroomId);
    
    // 4. 첫 연결 시 AI 인사 발송
    checkAndSendGreeting(chatroomId, uid);
    
    return ResponseEntity.ok()
        .header("Cache-Control", "no-cache")
        .header("Connection", "keep-alive")
        .body(emitter);
}
```

**SSE 이벤트 브로드캐스트 (`SSEManager`)**

```java
public void send(UUID chatroomId, String eventName, Object data) {
    List<SseEmitter> list = emitters.get(chatroomId);
    if (list == null || list.isEmpty()) return;
    
    for (SseEmitter emitter : list) {
        try {
            String jsonData = convertMapToJson((Map<?, ?>) data);
            emitter.send(SseEmitter.event().name(eventName).data(jsonData));
        } catch (IOException e) {
            remove(chatroomId, emitter);
        }
    }
}
```

**이벤트 타입:**
- `intimacy_analysis`: 친밀도 분석 완료
- `vocabulary_extracted`: 어휘 추출 완료
- `conversation_complete`: 대화 생성 완료
- `greeting_bot_message`: AI 인사 메시지
- `greeting_guide_message`: 가이드 메시지

**2. 멀티 에이전트 오케스트레이션 (`MultiAgentOrchestrator`)**

**아키텍처 패턴: 병렬 + Aggregator**

```java
public void processUserMessage(UUID chatroomId, UUID userId, Message userMessage) {
    String content = userMessage.getContent();
    
    // Phase 1: IntimacyAgent 병렬 실행
    Mono<IntimacyAgentResponse> intimacyMono = intimacyAgent.analyze(chatroomId, content)
        .doOnNext(resp -> {
            // SSE로 즉시 전송
            sseManager.send(chatroomId, "intimacy_analysis", Map.of(...));
            updateIntimacyProgress(chatroomId, userId, resp);
        })
        .cache(); // 결과 캐싱
    
    intimacyMono.subscribe(); // 즉시 구독하여 실행
    
    // Phase 2: ConversationAgent 독립 스트림
    conversationAgent.generateResponse(chatroomId, content, usageHolder)
        .collectList()
        .doOnSuccess(chunks -> {
            String fullResponse = String.join("", chunks);
            String actualContent = extractContentFromJson(fullResponse);
            
            // Phase 3: IntimacyAgent 결과 수집 후 VocabularyAgent 실행
            intimacyMono
                .doOnNext(intimacyResp -> {
                    vocabularyAgent.extractDifficultWords(actualContent, chatroomId)
                        .doOnNext(vocabResp -> {
                            // 메타데이터 생성 및 봇 메시지 저장
                            String metadataJson = buildBotMetadata(userMessage.getId(), intimacyResp, vocabResp, usage);
                            Message botMessage = chatService.sendMessage(chatroomId, null, "bot", actualContent, "text", metadataJson);
                            
                            // SSE로 완료 이벤트 전송
                            sseManager.send(chatroomId, "conversation_complete", payload);
                        })
                        .subscribe();
                })
                .subscribe();
            
            // Phase 4: SummarizerAgent 비동기 후처리
            SummarizerAgent.SummaryResult sr = summarizerAgent.summarize(chatroomId, 20, previousSummaryCompact);
            // progress_data 병합 저장
        })
        .subscribe();
}
```

**에이전트 구성:**

1. **IntimacyAgent** (친밀도 분석)
   - 사용자 메시지의 친밀도 레벨 감지 (1-3)
   - 컨셉(FRIEND, COWORKER, BOSS 등)에 맞는 교정 문장 생성
   - 피드백 제공 (한국어/영어)

2. **ConversationAgent** (대화 생성)
   - OpenAI Streaming API 호출
   - 메시지 히스토리 포함 (최근 10개)
   - 실시간 스트리밍 응답

3. **VocabularyAgent** (어휘 추출)
   - 챗봇 응답에서 어려운 단어 추출
   - 어휘 설명 생성 (로마자 표기, 한국어, 영어)

4. **SummarizerAgent** (요약)
   - 최근 20개 메시지 요약
   - 키워드 추출 (상위 10개)
   - PII 마스킹 적용

**3. OpenAI 통합 (`OpenAIClient`)**

```java
public Flux<String> streamRawCompletionWithHistory(
        String systemPrompt,
        List<Map<String, String>> messageHistory,
        String userContent,
        UUID chatroomId) {
    
    String model = getModelForChatRoom(chatroomId); // 테스트 모델 지원
    
    // 모델별 파라미터 분기 (gpt-4o는 max_completion_tokens 사용)
    Map<String, Object> req = Map.of(
        "model", model,
        "stream", true,
        "max_completion_tokens", aiConfig.getMaxOutputTokens(),
        "temperature", 0.85,
        "messages", messages.toArray()
    );
    
    return webClient()
        .post()
        .uri("/v1/chat/completions")
        .body(BodyInserters.fromValue(req))
        .accept(MediaType.TEXT_EVENT_STREAM)
        .retrieve()
        .bodyToFlux(String.class)
        .filter(s -> s != null && !s.isEmpty())
        .takeWhile(s -> !"[DONE]".equals(s.trim()));
}
```

**특징:**
- 스트리밍 응답 처리
- 토큰 사용량 추적 (`Usage` record)
- 모델별 파라미터 분기 (gpt-4o는 `max_completion_tokens` 사용)

**4. 프롬프트 관리 (`PromptService`)**

- DB 기반 동적 프롬프트
- 컨셉별 교정 지침 (FRIEND, COWORKER, BOSS, SENIOR, HONEY)
- 친밀도 레벨별 상세 지침

**5. Redis 캐싱**

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

**캐시 전략:**
- 메시지 히스토리: 채팅방별 캐싱
- 친밀도 레벨: 채팅방별 캐싱
- TTL: 기본값 사용

**6. 비용 추적 (`BillingService`)**

- OpenAI API 호출 시 토큰 사용량 기록
- 월별 사용자 비용 집계
- `AiUsageEvent` 테이블에 이벤트 저장

---

### 3.5 Store Service (포트: 8084)

#### 기술 스택
- **Spring Boot 3.3.4**
- **Spring Data JPA**
- **Feign Client**: Chat 서비스 통신
- **Redis**: 캐싱

#### 주요 기능

**1. 표현 보관함 관리**
- 사용자가 저장한 표현 관리
- 북마크 기능

**2. Chat 서비스 통신**
- Feign Client로 Chat 서비스 호출
- Circuit Breaker로 장애 격리

---

### 3.6 Batch Service (포트: 8085)

#### 기술 스택
- **Spring Boot 3.3.4**
- **Spring Scheduling**: `@Scheduled` 어노테이션

#### 주요 기능

**1. 스케줄링된 작업**

```java
@Scheduled(cron = "0 0 2 * * *") // 매일 새벽 2시
public void cleanupInactiveChatrooms() {
    chatroomCleanupService.cleanupInactiveChatrooms();
}

@Scheduled(cron = "0 0 3 * * *") // 매일 새벽 3시
public void archiveOldMessages() {
    messageArchivingService.archiveMessagesOlderThan(90);
}

@Scheduled(cron = "0 0 4 * * *") // 매일 새벽 4시
public void cleanupExpiredTokens() {
    tokenCleanupService.cleanupExpiredTokens();
}
```

**작업 목록:**
- 채팅방 정리 (비활성 채팅방 삭제)
- 메시지 아카이빙 (90일 이상 메시지)
- 토큰 정리 (만료된 Refresh Token)
- 일일 리포트 생성
- 비활성 사용자 처리

---

## 4. 기술 스택 및 라이브러리

### 4.1 백엔드 의존성

#### 공통 모듈 (common)
```gradle
api 'org.springframework.boot:spring-boot-starter'
api 'org.springframework.boot:spring-boot-starter-web'
api 'org.springframework.boot:spring-boot-starter-data-jpa'
api 'org.springframework.boot:spring-boot-starter-security'
api 'org.springframework.boot:spring-boot-starter-validation'
api 'org.springframework.boot:spring-boot-starter-actuator'
api 'org.projectlombok:lombok:1.18.30'
```

#### Gateway
```gradle
implementation 'org.springframework.cloud:spring-cloud-starter-gateway:4.1.0'
implementation 'org.springframework.cloud:spring-cloud-starter-loadbalancer:4.1.0'
implementation 'org.springframework.boot:spring-boot-starter-data-redis'
```

#### Auth
```gradle
implementation 'io.jsonwebtoken:jjwt-api:0.12.3'
implementation 'io.jsonwebtoken:jjwt-impl:0.12.3'
implementation 'io.jsonwebtoken:jjwt-jackson:0.12.3'
implementation 'org.springframework.cloud:spring-cloud-starter-openfeign'
implementation 'org.springframework.cloud:spring-cloud-starter-circuitbreaker-resilience4j'
implementation 'org.springframework.boot:spring-boot-starter-mail'
implementation 'com.google.api-client:google-api-client:2.2.0'
```

#### Chat
```gradle
implementation 'org.springframework.boot:spring-boot-starter-websocket'
implementation 'org.springframework.boot:spring-boot-starter-webflux'
implementation 'org.springframework.boot:spring-boot-starter-data-redis'
implementation 'org.springframework.boot:spring-boot-starter-cache'
implementation 'io.github.resilience4j:resilience4j-spring-boot3:2.2.0'
implementation 'org.apache.poi:poi:5.2.5' // Excel 파일 읽기
```

### 4.2 프론트엔드 의존성

```json
{
  "dependencies": {
    "react": "^19.1.1",
    "react-dom": "^19.1.1",
    "typescript": "~5.8.3",
    "vite": "^7.1.12",
    "@tanstack/react-query": "^5.90.7",
    "zustand": "^5.0.8",
    "axios": "^1.12.0",
    "event-source-polyfill": "^1.0.31",
    "react-router-dom": "^7.8.2",
    "tailwindcss": "^4.1.13",
    "daisyui": "^5.1.10"
  }
}
```

### 4.3 주요 라이브러리 선택 이유

**1. Spring Cloud Gateway (Reactive)**
- 높은 동시성 처리 (논블로킹 I/O)
- SSE 스트리밍 지원
- 필터 체인으로 인증/인가 처리

**2. Reactor (Mono/Flux)**
- 비동기 프로그래밍으로 성능 최적화
- 백프레셔(Backpressure) 지원
- 여러 에이전트 병렬 실행

**3. Resilience4j**
- Circuit Breaker로 장애 격리
- Retry로 일시적 장애 대응
- Rate Limiting 지원

**4. Zustand**
- Redux보다 간단한 상태 관리
- 타입스크립트 지원 우수
- 번들 크기 작음

**5. React Query**
- 서버 상태 관리 자동화
- 캐싱, 리프레시, 재시도 내장
- 로딩/에러 상태 관리

---

## 5. 데이터베이스 설계

### 5.1 스키마 분리 전략

각 서비스는 독립적인 스키마를 사용하여 데이터 격리를 보장합니다:

```sql
CREATE SCHEMA IF NOT EXISTS auth_schema;
CREATE SCHEMA IF NOT EXISTS user_schema;
CREATE SCHEMA IF NOT EXISTS chat_schema;
CREATE SCHEMA IF NOT EXISTS store_schema;
CREATE SCHEMA IF NOT EXISTS batch_schema;
```

**장점:**
- 서비스별 데이터 격리
- 독립적인 스키마 변경 가능
- 권한 관리 용이

### 5.2 주요 엔티티

#### Auth Schema
- `refresh_tokens`: Refresh Token 저장
- `token_blacklist`: 로그아웃된 토큰
- `login_attempts`: 로그인 시도 기록
- `email_verifications`: 이메일 인증 토큰
- `password_reset_tokens`: 비밀번호 재설정 토큰
- `auth_events`: 인증 이벤트 로그

#### User Schema
- `app_user`: 사용자 기본 정보
- `profiles`: 사용자 프로필 (JSONB)
- `settings`: 사용자 설정 (키-값)

#### Chat Schema
- `chatbots`: 챗봇 메타 정보
- `chatrooms`: 채팅방 정보 (settings JSONB)
- `messages`: 메시지 내용
- `intimacy_progress`: 친밀도 진척도 (progress_data JSONB)
- `user_chatbot_last_interaction`: 사용자-챗봇 상호작용 기록
- `ai_usage_events`: AI 사용 이벤트
- `monthly_user_costs`: 월별 사용자 비용

#### Store Schema
- `stores`: 표현 보관함

### 5.3 JSONB 활용

**1. `chatrooms.settings`**
```json
{
  "concept": "FRIEND",
  "testModel": "a"
}
```

**2. `intimacy_progress.progress_data`**
```json
{
  "summaryHistory": [...],
  "keywordIndex": {
    "items": [...]
  },
  "correctionsHistory": [...],
  "lastContextSnapshot": {...}
}
```

**3. `messages.metadata`**
```json
{
  "userMessageAnalysis": {
    "intimacy": {
      "detectedLevel": 1,
      "correctedSentence": "...",
      "feedback": {"ko": "...", "en": "..."},
      "corrections": "...",
      "alternativeExpressions": [...]
    }
  },
  "botResponseAnalysis": {
    "vocabulary": {
      "words": [...]
    }
  },
  "usage": {
    "inputTokens": 100,
    "outputTokens": 50,
    "total": 150
  }
}
```

**JSONB 사용 이유:**
- 유연한 스키마 변경
- 부분 업데이트 가능
- 인덱싱 지원 (GIN 인덱스)

---

## 6. 인증/인가 흐름

### 6.1 로그인 흐름

```
1. Client → Gateway: POST /api/auth/login
   { email, password }

2. Gateway → Auth Service: POST /api/auth/login
   (인증 제외 경로이므로 JWT 필터 통과)

3. Auth Service:
   - User Service에서 사용자 정보 조회 (Feign Client)
   - 비밀번호 검증
   - JWT 토큰 생성 (Access + Refresh)
   - Refresh Token 저장 (DB)
   - 로그인 시도 기록

4. Auth Service → Gateway: LoginResponse
   { accessToken, refreshToken }

5. Gateway → Client: LoginResponse
```

### 6.2 API 요청 흐름

```
1. Client → Gateway: GET /api/chat/rooms
   Authorization: Bearer <accessToken>

2. Gateway (JwtAuthFilter):
   - JWT 토큰 추출
   - Auth Service로 토큰 검증
   - 검증 성공 시:
     * JWT 페이로드 파싱
     * HMAC 서명 생성
     * X-User-Id, X-Auth-Ts, X-Auth-Sign 헤더 주입

3. Gateway → Chat Service: GET /api/chat/rooms
   X-User-Id: <userId>
   X-Auth-Ts: <timestamp>
   X-Auth-Sign: <hmacSignature>

4. Chat Service (HmacAuthInterceptor):
   - HMAC 헤더 검증
   - 타임스탬프 검증 (60초 이내)
   - 서명 검증

5. Chat Service → Gateway: 응답

6. Gateway → Client: 응답
```

### 6.3 토큰 갱신 흐름

```
1. Client → Gateway: POST /api/auth/refresh
   { refreshToken }

2. Gateway → Auth Service: POST /api/auth/refresh

3. Auth Service:
   - Refresh Token 검증
   - DB에서 Refresh Token 조회
   - 새로운 Access Token + Refresh Token 생성
   - 기존 Refresh Token 삭제, 새 토큰 저장

4. Auth Service → Gateway: LoginResponse

5. Gateway → Client: LoginResponse
```

### 6.4 로그아웃 흐름

```
1. Client → Gateway: POST /api/auth/logout
   Authorization: Bearer <accessToken>

2. Gateway → Auth Service: POST /api/auth/logout

3. Auth Service:
   - Access Token을 Redis 블랙리스트에 추가 (TTL = 토큰 만료 시간)
   - DB에도 블랙리스트 저장
   - Refresh Token 삭제

4. 이후 해당 Access Token으로 요청 시:
   - JWT 검증 시 Redis 블랙리스트 확인
   - 블랙리스트에 있으면 401 Unauthorized 반환
```

---

## 7. 실시간 통신 구현

### 7.1 SSE (Server-Sent Events)

**서버 측 구현 (`SSEController`)**

```java
@GetMapping(value = "/stream/{chatroomId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public ResponseEntity<SseEmitter> stream(@PathVariable UUID chatroomId) {
    SseEmitter emitter = sseManager.create(chatroomId);
    
    emitter.onCompletion(() -> remove(chatroomId, emitter));
    emitter.onTimeout(() -> remove(chatroomId, emitter));
    emitter.onError((ex) -> remove(chatroomId, emitter));
    
    return ResponseEntity.ok()
        .header("Cache-Control", "no-cache")
        .header("Connection", "keep-alive")
        .body(emitter);
}
```

**이벤트 전송 (`SSEManager`)**

```java
public void send(UUID chatroomId, String eventName, Object data) {
    List<SseEmitter> list = emitters.get(chatroomId);
    for (SseEmitter emitter : list) {
        try {
            String jsonData = convertMapToJson((Map<?, ?>) data);
            emitter.send(SseEmitter.event().name(eventName).data(jsonData));
        } catch (IOException e) {
            remove(chatroomId, emitter);
        }
    }
}
```

**클라이언트 측 구현 (`useChatStream`)**

```typescript
export function useChatStream<T = unknown>(
  chatroomId: string,
  userId?: string,
  accessToken?: string,
  onEventReceived?: (eventType: string, data: T) => void
): UseChatStreamResult {
  useEffect(() => {
    const eventSource = new EventSourcePolyfill(sseUrl, {
      headers: {
        Authorization: accessToken ? `Bearer ${accessToken}` : '',
      },
      heartbeatTimeout: 3600000, // 1시간
    });

    eventSource.onopen = () => {
      console.log('[SSE] Connection opened');
    };

    eventNames.forEach(name => {
      eventSource.addEventListener(name, (event: MessageEvent) => {
        const parsedData = JSON.parse(event.data);
        if (onEventReceived) {
          onEventReceived(name, parsedData);
        }
      });
    });

    eventSource.onerror = event => {
      // 자동 재연결 로직 (최대 5회, 지수 백오프)
      if (retryCount < maxRetries) {
        setTimeout(() => {
          retryCount++;
          connect();
        }, retryDelay);
      }
    };

    return () => {
      eventSource.close();
    };
  }, [chatroomId, userId, accessToken]);
}
```

### 7.2 SSE vs WebSocket 선택 이유

**SSE 선택 이유:**
- 단방향 통신 (서버 → 클라이언트)으로 충분
- HTTP 기반으로 구현 간단
- 자동 재연결 지원
- Gateway에서 프록시 용이

**WebSocket 사용 케이스:**
- 양방향 실시간 통신 필요 시
- 현재는 사용자 메시지는 HTTP POST로 전송

---

## 8. 멀티 에이전트 시스템

### 8.1 아키텍처 패턴

**병렬 + Aggregator 패턴**

```
사용자 메시지
    │
    ├─→ IntimacyAgent (병렬 실행)
    │   └─→ SSE: intimacy_analysis
    │
    ├─→ ConversationAgent (독립 스트림)
    │   └─→ OpenAI Streaming
    │
    └─→ VocabularyAgent (봇 응답 후 실행)
        └─→ SSE: vocabulary_extracted
            │
            └─→ SSE: conversation_complete
                │
                └─→ SummarizerAgent (비동기 후처리)
```

### 8.2 에이전트 상세

#### 1. IntimacyAgent

**역할**: 사용자 메시지의 친밀도 레벨 감지 및 교정

**프로세스:**
1. `IntimacyAnalysisAgent`: 친밀도 레벨 감지 (1 또는 3)
2. `IntimacyCorrectionAgent`: 컨셉에 맞는 교정 문장 생성

**컨셉별 제약:**
- **FRIEND**: 반말 필수 (Level 1, 3)
- **COWORKER/SENIOR/BOSS**: 존댓말 필수
- **HONEY**: Level 3은 반말 필수

**응답 형식:**
```json
{
  "detectedLevel": 1,
  "correctedSentence": "오늘 밥 먹었어?",
  "feedback": {
    "ko": "친구 사이에서는 반말을 사용하는 것이 자연스럽습니다.",
    "en": "Using informal speech is natural between friends."
  },
  "corrections": "존댓말을 반말로 변경",
  "alternativeExpressions": [
    {
      "expression": "밥 먹었어?",
      "tone": "casual",
      "example": "친구에게 사용하는 표현"
    }
  ]
}
```

#### 2. ConversationAgent

**역할**: 자연스러운 대화 생성

**프로세스:**
1. 메시지 히스토리 조회 (최근 10개)
2. System Prompt 생성 (챗봇별, 컨셉별)
3. OpenAI Streaming API 호출
4. 스트리밍 응답 수집
5. 토큰 사용량 추적

**프롬프트 구성:**
- Base Prompt: 챗봇 성격, 역할
- Context: 친밀도 레벨, 컨셉
- History: 최근 대화 맥락

#### 3. VocabularyAgent

**역할**: 챗봇 응답에서 어려운 단어 추출 및 설명

**프로세스:**
1. `VocabularyExtractionAgent`: 어휘 추출
2. `VocabularyExplanationAgent`: 어휘 설명 생성

**응답 형식:**
```json
{
  "words": [
    {
      "word": "일어나다",
      "difficulty": "intermediate",
      "context": {
        "roma": "ireonada",
        "ko": "일어나다: 침대나 자리에서 벗어나다",
        "en": "ireonada: to get up, to rise from bed or seat"
      }
    }
  ]
}
```

#### 4. SummarizerAgent

**역할**: 대화 요약 및 키워드 추출

**프로세스:**
1. 최근 20개 메시지 조회
2. PII 마스킹 적용
3. 이전 요약 참고
4. OpenAI API 호출
5. JSON 파싱 (summary, keywords)
6. `progress_data`에 병합 저장

**응답 형식:**
```json
{
  "summary": {
    "participants": ["user", "bot"],
    "decisions": [],
    "tasks": [],
    "preferences": [],
    "facts": []
  },
  "keywords": ["키워드1", "키워드2", ...]
}
```

### 8.3 에러 처리

- 각 에이전트는 독립적으로 실행되므로, 하나가 실패해도 다른 에이전트는 계속 실행
- 에러 발생 시 빈 응답 반환 또는 기본값 사용
- 로깅으로 디버깅 용이

---

## 9. 성능 최적화

### 9.1 캐싱 전략

**Redis 캐싱:**
- 메시지 히스토리: `@Cacheable(value = "messageHistory", key = "#chatroomId")`
- 친밀도 레벨: `@Cacheable(value = "intimacy", key = "#chatroomId")`

**캐시 설정:**
```java
@Configuration
@EnableCaching
public class RedisCacheConfig {
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
        
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .build();
    }
}
```

### 9.2 비동기 처리

**Reactor Mono/Flux 활용:**
- 여러 에이전트 병렬 실행
- 논블로킹 I/O로 높은 동시성 처리
- 백프레셔 지원

**예시:**
```java
Mono<IntimacyAgentResponse> intimacyMono = intimacyAgent.analyze(chatroomId, content)
    .cache(); // 결과 캐싱

intimacyMono.subscribe(); // 즉시 구독하여 실행

// 다른 작업과 병렬 실행
conversationAgent.generateResponse(chatroomId, content, usageHolder)
    .collectList()
    .doOnSuccess(chunks -> {
        // IntimacyAgent 결과 수집
        intimacyMono
            .doOnNext(intimacyResp -> {
                // VocabularyAgent 실행
            })
            .subscribe();
    })
    .subscribe();
```

### 9.3 데이터베이스 최적화

**인덱싱:**
- `chatrooms.user_id`, `chatrooms.id` 복합 인덱스
- `messages.chatroom_id`, `messages.sequence_number` 인덱스
- `intimacy_progress.chatroom_id` UNIQUE 인덱스

**JSONB 인덱싱:**
```sql
CREATE INDEX idx_chatrooms_settings_concept ON chatrooms USING GIN ((settings->>'concept'));
```

**연결 풀 설정:**
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 20000
      idle-timeout: 600000
      max-lifetime: 1800000
```

### 9.4 프론트엔드 최적화

**React Query 캐싱:**
- 서버 상태 자동 캐싱
- Stale-while-revalidate 전략
- 백그라운드 리프레시

**코드 스플리팅:**
- Vite의 자동 코드 스플리팅
- 라우트별 청크 분리

---

## 10. 장애 처리

### 10.1 Circuit Breaker

**Resilience4j 설정:**

```yaml
resilience4j:
  circuitbreaker:
    instances:
      userServiceClient:
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
        halfOpenMaxCalls: 3
```

**사용 예시:**

```java
@CircuitBreaker(name = "userServiceClient", fallbackMethod = "getUserFallback")
public UserDto getUserById(UUID userId) {
    return userServiceClient.getUserById(userId);
}

public UserDto getUserFallback(UUID userId, Exception e) {
    log.error("User Service 호출 실패, Fallback 실행: userId={}", userId, e);
    return UserDto.defaultUser();
}
```

### 10.2 Retry

**Gateway Retry 설정:**

```yaml
filters:
  - name: Retry
    args:
      retries: 2
      methods: GET
      backoff:
        firstBackoff: 200ms
        factor: 2
        maxBackoff: 500ms
        basedOnPreviousValue: true
```

### 10.3 SSE 재연결

**클라이언트 측 자동 재연결:**

```typescript
let retryCount = 0;
const maxRetries = 5;
let retryDelay = 3000;

eventSource.onerror = event => {
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

### 10.4 에러 응답 형식

```json
{
  "success": false,
  "message": "에러 메시지",
  "errorCode": "ERROR_CODE",
  "data": null
}
```

---

## 11. 배포 및 운영

### 11.1 Docker 구성

**Docker Compose 서비스:**

```yaml
services:
  shared-db:
    image: postgres:17-alpine
    environment:
      POSTGRES_DB: dorandoran
      POSTGRES_USER: doran
      POSTGRES_PASSWORD: doran
  
  redis:
    image: redis:7-alpine
    command: redis-server --appendonly yes
  
  api-gateway:
    build:
      dockerfile: docker/Dockerfile.gateway
    ports: ["8080:8080"]
    depends_on:
      - auth-service
      - user-service
      - chat-service
  
  auth-service:
    build:
      dockerfile: docker/Dockerfile.auth
    ports: ["8081:8081"]
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://shared-db:5432/dorandoran
      SPRING_JPA_HIBERNATE_DEFAULT_SCHEMA: auth_schema
```

### 11.2 모니터링 스택

**Prometheus:**
- 메트릭 수집
- `/actuator/prometheus` 엔드포인트

**Grafana:**
- 대시보드 시각화
- 알림 설정

**Loki:**
- 로그 수집
- Promtail로 로그 전송

**AlertManager:**
- 알림 관리
- 이메일/Slack 알림

### 11.3 헬스체크

**Actuator 엔드포인트:**

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
```

**헬스체크 API:**
- `GET /actuator/health`: 서비스 상태 확인
- `GET /api/{service}/health`: 서비스별 헬스체크

### 11.4 로깅

**로깅 설정:**

```yaml
logging:
  level:
    com.dorandoran: INFO
    org.springframework.web: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
```

**구조화된 로깅:**
- Logstash Encoder로 JSON 형식 로그 출력
- Loki로 중앙 집중식 로그 수집

---

## 12. 면접 대비 핵심 포인트

### 12.1 아키텍처 설계

**Q: 왜 마이크로서비스 아키텍처를 선택했나요?**
- 서비스별 독립적 배포 및 스케일링
- 기술 스택 다양화 가능 (Gateway는 Reactive, 다른 서비스는 MVC)
- 장애 격리 (Circuit Breaker)
- 팀별 독립적 개발 가능

**Q: 서비스 간 통신은 어떻게 하나요?**
- 동기: Feign Client (HTTP/REST)
- 비동기: Reactor Mono/Flux (서비스 내부)
- 실시간: SSE (서버 → 클라이언트)

**Q: 데이터베이스는 어떻게 관리하나요?**
- 스키마 분리 전략 (서비스별 독립 스키마)
- 공유 데이터베이스 (PostgreSQL 단일 인스턴스)
- 향후 서비스별 독립 DB로 분리 가능

### 12.2 인증/인가

**Q: JWT 토큰을 어떻게 관리하나요?**
- Access Token (1시간) + Refresh Token (7일)
- 로그아웃 시 Redis 블랙리스트에 Access Token 저장
- 토큰 검증은 Auth 서비스에서 수행

**Q: 서비스 간 인증은 어떻게 하나요?**
- Gateway에서 JWT 검증 후 HMAC 서명 생성
- 하위 서비스에서 HMAC 헤더 검증
- 타임스탬프 기반 리플레이 공격 방지

### 12.3 실시간 통신

**Q: SSE를 선택한 이유는?**
- 단방향 통신 (서버 → 클라이언트)으로 충분
- HTTP 기반으로 구현 간단
- 자동 재연결 지원
- Gateway 프록시 용이

**Q: SSE 연결은 어떻게 관리하나요?**
- 채팅방별 `SseEmitter` 리스트 관리 (`ConcurrentHashMap`)
- 연결 종료 시 자동 제거
- 여러 클라이언트 동시 연결 지원

### 12.4 멀티 에이전트 시스템

**Q: 여러 에이전트를 어떻게 조율하나요?**
- 병렬 + Aggregator 패턴
- Reactor Mono/Flux로 비동기 실행
- 각 에이전트는 독립적으로 실행, 결과를 집계

**Q: 에이전트 간 의존성은 어떻게 처리하나요?**
- IntimacyAgent는 독립 실행
- ConversationAgent는 독립 실행
- VocabularyAgent는 ConversationAgent 결과 필요
- SummarizerAgent는 모든 메시지 처리 후 실행

**Q: 에러 처리는 어떻게 하나요?**
- 각 에이전트는 독립적으로 실행되므로, 하나가 실패해도 다른 에이전트는 계속 실행
- 에러 발생 시 빈 응답 또는 기본값 반환
- 로깅으로 디버깅

### 12.5 성능 최적화

**Q: 캐싱 전략은?**
- Redis로 메시지 히스토리, 친밀도 레벨 캐싱
- `@Cacheable` 어노테이션 사용
- TTL 10분

**Q: 비동기 처리는 어떻게 하나요?**
- Reactor Mono/Flux로 논블로킹 I/O
- 여러 에이전트 병렬 실행
- 백프레셔 지원

**Q: 데이터베이스 최적화는?**
- 인덱싱 (복합 인덱스, JSONB GIN 인덱스)
- 연결 풀 설정 (HikariCP)
- JSONB 활용으로 유연한 스키마

### 12.6 장애 처리

**Q: Circuit Breaker는 어떻게 사용하나요?**
- Resilience4j로 User 서비스 호출 보호
- 실패율 50% 초과 시 Circuit Open
- Fallback 메서드로 기본 응답 반환

**Q: Retry는 어떻게 설정하나요?**
- Gateway에서 GET 요청에 대해 2회 재시도
- 지수 백오프 (200ms → 400ms → 500ms)

**Q: SSE 재연결은 어떻게 처리하나요?**
- 클라이언트 측 자동 재연결 (최대 5회)
- 지수 백오프 (3초 → 6초 → ... → 최대 30초)

### 12.7 기술 선택 이유

**Q: Spring Cloud Gateway를 선택한 이유?**
- Reactive WebFlux로 높은 동시성
- SSE 스트리밍 지원
- 필터 체인으로 인증/인가 처리

**Q: Reactor를 선택한 이유?**
- 비동기 프로그래밍으로 성능 최적화
- 백프레셔 지원
- 여러 에이전트 병렬 실행 용이

**Q: Zustand를 선택한 이유?**
- Redux보다 간단한 상태 관리
- 타입스크립트 지원 우수
- 번들 크기 작음

### 12.8 데이터베이스 설계

**Q: JSONB를 사용한 이유?**
- 유연한 스키마 변경
- 부분 업데이트 가능
- 인덱싱 지원 (GIN 인덱스)

**Q: 스키마 분리 전략은?**
- 서비스별 독립 스키마
- 데이터 격리 보장
- 향후 서비스별 독립 DB로 분리 가능

---

## 결론

DoranDoran 프로젝트는 마이크로서비스 아키텍처 기반의 한국어 학습 챗봇 플랫폼으로, 다음과 같은 특징을 가집니다:

1. **확장 가능한 아키텍처**: 서비스별 독립적 배포 및 스케일링
2. **실시간 통신**: SSE 기반 실시간 메시지 스트리밍
3. **멀티 에이전트 시스템**: 여러 AI 에이전트의 협업으로 정확한 분석 및 응답
4. **안전한 인증/인가**: JWT + HMAC 기반 이중 보안
5. **장애 대응**: Circuit Breaker, Retry로 안정성 확보
6. **성능 최적화**: 캐싱, 비동기 처리, 데이터베이스 최적화

이 문서는 면접 대비를 위해 프로젝트의 전체 아키텍처와 구현 상세를 정리한 것입니다. 각 서비스의 역할, 기술 스택, 핵심 로직을 이해하고 설명할 수 있도록 구성했습니다.

