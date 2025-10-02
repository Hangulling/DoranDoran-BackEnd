# 🏗️ DoranDoran 마이크로서비스 아키텍처 가이드

## 📋 목차

1. [프로젝트 개요](#프로젝트-개요)
2. [아키텍처 구조](#아키텍처-구조)
3. [서비스 구성](#서비스-구성)
4. [서비스 간 통신](#서비스-간-통신)
5. [공통 모듈](#공통-모듈)
6. [인프라 구성](#인프라-구성)
7. [보안 및 안정성](#보안-및-안정성)
8. [Chat Service 개발자 온보딩 가이드](#chat-service-개발자-온보딩-가이드)

---

## 🎯 프로젝트 개요

DoranDoran은 Spring Boot와 마이크로서비스 아키텍처를 기반으로 한 웹 애플리케이션입니다. 사용자 관리, 인증/인가, 실시간 채팅, 배치 처리 등의 기능을 제공합니다.

### 주요 특징

- 🏗️ **마이크로서비스 아키텍처**: 독립적인 서비스들로 구성
- 🔐 **JWT 기반 인증**: 안전한 사용자 인증 시스템
- 🚪 **API Gateway**: 통합된 API 엔드포인트 제공
- 💬 **실시간 채팅**: WebSocket 기반 실시간 통신
- 📊 **모니터링**: Grafana를 통한 시스템 모니터링
- 🐳 **Docker 지원**: 컨테이너화된 배포 환경

---

## 🏗️ 아키텍처 구조

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   API Gateway   │    │   Auth Service  │    │   User Service  │
│   (Port: 8080)  │◄──►│   (Port: 8081)  │◄──►│   (Port: 8082)  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  Chat Service   │    │  Batch Service  │    │   PostgreSQL    │
│   (Port: 8083)  │    │   (Port: 8085)  │    │   (Port: 5432)  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │
         ▼                       ▼
┌─────────────────┐    ┌─────────────────┐
│     Redis       │    │   Monitoring    │
│   (Port: 6379)  │    │   (Grafana)     │
└─────────────────┘    └─────────────────┘
```

---

## 🔧 서비스 구성

### 1. API Gateway (포트: 8080)

**역할**: 모든 외부 요청의 진입점

**기술 스택**:
- Spring Cloud Gateway (Reactive)
- Redis (Rate Limiting)

**라우팅 규칙**:
```yaml
routes:
  - id: auth-service
    uri: http://localhost:8081
    predicates:
      - Path=/api/auth/**
  
  - id: user-service
    uri: http://localhost:8082
    predicates:
      - Path=/api/users/**
  
  - id: chat-service
    uri: http://localhost:8083
    predicates:
      - Path=/api/chat/**
  
  - id: batch-service
    uri: http://localhost:8085
    predicates:
      - Path=/api/batch/**
```

**주요 기능**:
- 요청 라우팅 및 로드밸런싱
- Rate Limiting (Redis 기반)
- CORS 설정
- 요청/응답 로깅

### 2. Auth Service (포트: 8081)

**역할**: JWT 기반 인증/인가 처리

**데이터베이스**: PostgreSQL (auth 스키마)

**주요 기능**:
- 사용자 로그인/로그아웃
- JWT 토큰 생성/검증
- 사용자 권한 관리
- 이벤트 기반 사용자 상태 동기화

### 3. User Service (포트: 8082)

**역할**: 사용자 정보 및 프로필 관리

**데이터베이스**: PostgreSQL (user_schema)

**주요 기능**:
- 사용자 CRUD 작업
- 프로필 관리
- 사용자 설정 관리
- 이벤트 발행 (사용자 상태 변경)

### 4. Chat Service (포트: 8083)

**역할**: 실시간 채팅 및 메시지 관리

**데이터베이스**: PostgreSQL (chat 스키마)

**주요 기능**:
- 채팅방 생성/관리
- 메시지 전송/수신
- 메시지 검색
- 실시간 통신 (WebSocket)

### 5. Batch Service (포트: 8085)

**역할**: 스케줄링된 작업 및 배치 처리

**주요 기능**:
- 스케줄링된 작업 실행
- 데이터 처리
- 리포트 생성

---

## 🔄 서비스 간 통신

### 1. 동기 통신 (HTTP/REST)

#### Feign Client 사용
```java
@FeignClient(
    name = "user-service",
    url = "${user.service.url}",
    fallback = UserServiceClientFallback.class
)
public interface UserServiceClient {
    @GetMapping("/api/users/{userId}")
    UserDto getUserById(@PathVariable("userId") String userId);
    
    @GetMapping("/api/users/email/{email}")
    UserDto getUserByEmail(@PathVariable("email") String email);
}
```

#### Circuit Breaker 패턴
```java
@Service
public class UserIntegrationService {
    
    @CircuitBreaker(name = "user-service", fallbackMethod = "getUserByIdFallback")
    @Retry(name = "user-service")
    public UserDto getUserById(String userId) {
        return userServiceClient.getUserById(userId);
    }
    
    public UserDto getUserByIdFallback(String userId, Exception ex) {
        log.error("User Service 호출 실패: {}", ex.getMessage());
        return null;
    }
}
```

### 2. 이벤트 기반 통신 (Spring Events)

#### 이벤트 발행 (User Service)
```java
@Service
public class UserService {
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    public void updateUserStatus(UUID userId, UserStatus newStatus) {
        // 사용자 상태 업데이트
        UserStatus oldStatus = user.getStatus();
        user.setStatus(newStatus);
        userRepository.save(user);
        
        // 이벤트 발행
        UserStatusChangedEvent event = UserStatusChangedEvent.of(
            userId, user.getEmail(), oldStatus, newStatus
        );
        eventPublisher.publishEvent(event);
    }
}
```

#### 이벤트 구독 (Auth Service)
```java
@Component
public class UserEventListener {
    
    @EventListener
    public void handleUserStatusChanged(UserStatusChangedEvent event) {
        log.info("사용자 상태 변경 이벤트 수신: userId={}, newStatus={}", 
                event.userId(), event.newStatus());
        
        // Auth 서비스에서 수행할 작업들
        applyAuthPolicyForStatus(event.userId(), event.newStatus());
        manageActiveSessions(event.userId(), event.newStatus());
    }
}
```

### 3. 공유 데이터베이스

**PostgreSQL** 단일 인스턴스 사용, 스키마로 분리:

- `auth`: 인증 관련 테이블
- `user_schema`: 사용자 프로필 관련 테이블  
- `chat`: 채팅 관련 테이블
- `store`: 저장소 관련 테이블

### 4. Redis 캐싱

**용도**:
- 세션 관리
- 캐싱
- Rate Limiting
- 실시간 데이터 저장

---

## 📦 공통 모듈

### 1. Shared 모듈

**UserDto**: 사용자 정보 공통 DTO
```java
public record UserDto(
    UUID id,
    String email,
    String firstName,
    String lastName,
    String name,
    String passwordHash,
    String picture,
    String info,
    LocalDateTime lastConnTime,
    UserStatus status,
    RoleName role,
    boolean coachCheck,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public enum UserStatus {
        ACTIVE("active"),
        INACTIVE("inactive"),
        SUSPENDED("suspended");
    }
    
    public enum RoleName {
        ROLE_USER,
        ROLE_ADMIN
    }
}
```

**이벤트 클래스들**:
- `UserCreatedEvent`: 사용자 생성
- `UserStatusChangedEvent`: 사용자 상태 변경
- `UserDeletedEvent`: 사용자 삭제

### 2. Common 모듈

**ApiResponse**: 공통 API 응답 형식
```java
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private LocalDateTime timestamp;
}
```

---

## 🐳 인프라 구성

### Docker Compose

```yaml
services:
  # 공유 데이터베이스
  shared-db:
    image: postgres:17-alpine
    environment:
      POSTGRES_DB: dorandoran
      POSTGRES_USER: doran
      POSTGRES_PASSWORD: doran
    ports: ["5432:5432"]
  
  # Redis
  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]
  
  # 서비스들
  auth-service:
    build: ./docker/Dockerfile.auth
    ports: ["8081:8081"]
    depends_on: [shared-db, redis]
  
  user-service:
    build: ./docker/Dockerfile.user
    ports: ["8082:8082"]
    depends_on: [shared-db, redis]
  
  chat-service:
    build: ./docker/Dockerfile.chat
    ports: ["8083:8083"]
    depends_on: [shared-db, redis]
  
  api-gateway:
    build: ./docker/Dockerfile.gateway
    ports: ["8080:8080"]
    depends_on: [auth-service, user-service, chat-service]
```

### 모니터링

- **Prometheus**: 메트릭 수집 (포트: 9090)
- **Grafana**: 대시보드 (포트: 3000)
- **Actuator**: 각 서비스의 헬스체크 엔드포인트

---

## 🔒 보안 및 안정성

### 1. Circuit Breaker 패턴

- **Resilience4j** 사용
- **Fallback 메서드** 구현
- **재시도 로직** 적용

### 2. Rate Limiting

**Redis 기반** Rate Limiter, 서비스별 차등 적용:
- Auth: 10 req/s (burst: 20)
- User: 20 req/s (burst: 40)
- Chat: 30 req/s (burst: 60)
- Batch: 5 req/s (burst: 10)

### 3. JWT 인증

- **토큰 기반 인증**
- **역할 기반 접근 제어 (RBAC)**
- **토큰 갱신 메커니즘**

---

## 💬 Chat Service 개발자 온보딩 가이드

### 🚀 개발 환경 설정

#### 1. 필수 의존성 설치

**build.gradle.kts**에 추가할 의존성:

```kotlin
dependencies {
    // 기존 의존성들...
    
    // Feign Client (서비스 간 통신)
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    
    // Resilience4j (Circuit Breaker)
    implementation("io.github.resilience4j:resilience4j-spring-boot3")
    implementation("io.github.resilience4j:resilience4j-circuitbreaker")
    implementation("io.github.resilience4j:resilience4j-retry")
    implementation("io.github.resilience4j:resilience4j-timelimiter")
    
    // WebSocket (실시간 통신)
    implementation("org.springframework:spring-websocket")
    implementation("org.springframework:spring-messaging")
    
    // Redis (캐싱 및 세션 관리)
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    
    // JWT (토큰 검증)
    implementation("io.jsonwebtoken:jjwt-api:0.11.5")
    implementation("io.jsonwebtoken:jjwt-impl:0.11.5")
    implementation("io.jsonwebtoken:jjwt-jackson:0.11.5")
    
    // 공통 모듈
    implementation(project(":shared"))
    implementation(project(":common"))
}
```

#### 2. 설정 파일 구성

**application.yml** 설정:

```yaml
server:
  port: 8083

spring:
  application:
    name: chat-service
  
  # 데이터베이스 설정
  datasource:
    url: jdbc:postgresql://localhost:5432/dorandoran
    username: doran
    password: doran
    driver-class-name: org.postgresql.Driver
  
  # JPA 설정
  jpa:
    hibernate:
      ddl-auto: validate
      default_schema: chat
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
  
  # Redis 설정
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0

# Feign Client 설정
feign:
  client:
    config:
      default:
        connectTimeout: 5000
        readTimeout: 10000
        loggerLevel: basic

# Resilience4j 설정
resilience4j:
  circuitbreaker:
    instances:
      user-service:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10s
        sliding-window-size: 10
  retry:
    instances:
      user-service:
        max-attempts: 3
        wait-duration: 1s
  timelimiter:
    instances:
      user-service:
        timeout-duration: 5s

# JWT 설정
jwt:
  secret: your-secret-key
  expiration: 86400000 # 24시간
```

### 🔗 서비스 간 통신 구현

#### 1. Feign Client 설정

**UserServiceClient.java**:
```java
@FeignClient(
    name = "user-service",
    url = "${user.service.url:http://localhost:8082}",
    fallback = UserServiceClientFallback.class
)
public interface UserServiceClient {
    
    @GetMapping("/api/users/{userId}")
    UserDto getUserById(@PathVariable("userId") String userId);
    
    @GetMapping("/api/users/email/{email}")
    UserDto getUserByEmail(@PathVariable("email") String email);
    
    @GetMapping("/api/users/health")
    String healthCheck();
}
```

**UserServiceClientFallback.java**:
```java
@Component
public class UserServiceClientFallback implements UserServiceClient {
    
    @Override
    public UserDto getUserById(String userId) {
        log.error("User Service 호출 실패 - getUserById: userId={}", userId);
        return null;
    }
    
    @Override
    public UserDto getUserByEmail(String email) {
        log.error("User Service 호출 실패 - getUserByEmail: email={}", email);
        return null;
    }
    
    @Override
    public String healthCheck() {
        return "User Service is unavailable";
    }
}
```

#### 2. 통합 서비스 구현

**UserIntegrationService.java**:
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class UserIntegrationService {
    
    private final UserServiceClient userServiceClient;
    
    @CircuitBreaker(name = "user-service", fallbackMethod = "getUserByIdFallback")
    @Retry(name = "user-service")
    @TimeLimiter(name = "user-service")
    public CompletableFuture<UserDto> getUserByIdAsync(String userId) {
        log.info("User Service 호출 - getUserById: userId={}", userId);
        return CompletableFuture.completedFuture(userServiceClient.getUserById(userId));
    }
    
    @CircuitBreaker(name = "user-service", fallbackMethod = "getUserByIdFallback")
    @Retry(name = "user-service")
    public UserDto getUserById(String userId) {
        log.info("User Service 호출 - getUserById: userId={}", userId);
        return userServiceClient.getUserById(userId);
    }
    
    public UserDto getUserByIdFallback(String userId, Exception ex) {
        log.error("User Service 호출 실패 - getUserById: userId={}, error={}", userId, ex.getMessage());
        return null;
    }
}
```

### 🔐 JWT 토큰 처리

#### 1. JWT 유틸리티 클래스

**JwtUtil.java**:
```java
@Component
@Slf4j
public class JwtUtil {
    
    @Value("${jwt.secret}")
    private String secret;
    
    @Value("${jwt.expiration}")
    private Long expiration;
    
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    
    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    public Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
    
    public Boolean validateToken(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username) && !isTokenExpired(token));
    }
}
```

#### 2. JWT 필터 구현

**JwtAuthenticationFilter.java**:
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtUtil jwtUtil;
    private final UserIntegrationService userIntegrationService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        jwt = authHeader.substring(7);
        try {
            username = jwtUtil.extractUsername(jwt);
            
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDto user = userIntegrationService.getUserByEmail(username);
                
                if (user != null && jwtUtil.validateToken(jwt, username)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        user, null, getAuthorities(user.getRole())
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            log.error("JWT 토큰 처리 중 오류 발생", e);
        }
        
        filterChain.doFilter(request, response);
    }
    
    private Collection<? extends GrantedAuthority> getAuthorities(UserDto.RoleName role) {
        return Collections.singletonList(new SimpleGrantedAuthority(role.name()));
    }
}
```

### 📡 이벤트 기반 통신

#### 1. 이벤트 발행

**ChatService.java**에서 이벤트 발행:
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {
    
    private final ApplicationEventPublisher eventPublisher;
    private final ChatRoomRepository chatRoomRepository;
    private final UserIntegrationService userIntegrationService;
    
    @Transactional
    public ChatRoom createChatRoom(UUID userId, String roomName, UUID botId) {
        log.info("채팅방 생성 요청 - 사용자: {}, 방이름: {}", userId, roomName);
        
        // 사용자 정보 검증
        UserDto user = userIntegrationService.getUserById(userId.toString());
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId);
        }
        
        // 채팅방 생성
        ChatRoom chatRoom = ChatRoom.builder()
                .roomId(UUID.randomUUID())
                .userId(userId)
                .botId(botId)
                .roomName(roomName)
                .createAt(LocalDateTime.now())
                .isDeleted(false)
                .settings("{}")
                .build();
        
        ChatRoom savedRoom = chatRoomRepository.save(chatRoom);
        
        // 채팅방 생성 이벤트 발행
        ChatRoomCreatedEvent event = ChatRoomCreatedEvent.of(
            savedRoom.getRoomId(), 
            userId, 
            roomName, 
            LocalDateTime.now()
        );
        eventPublisher.publishEvent(event);
        
        log.info("채팅방 생성 완료 - ID: {}", savedRoom.getRoomId());
        return savedRoom;
    }
}
```

#### 2. 이벤트 구독

**UserEventListener.java**:
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventListener {
    
    private final ChatService chatService;
    
    @EventListener
    public void handleUserStatusChanged(UserStatusChangedEvent event) {
        log.info("사용자 상태 변경 이벤트 수신: userId={}, newStatus={}", 
                event.userId(), event.newStatus());
        
        // 사용자가 비활성화되면 채팅방 접근 제한
        if (event.newStatus() == UserDto.UserStatus.INACTIVE) {
            chatService.restrictUserChatRooms(event.userId());
        }
        
        // 사용자가 정지되면 모든 채팅방 비활성화
        if (event.newStatus() == UserDto.UserStatus.SUSPENDED) {
            chatService.deactivateUserChatRooms(event.userId());
        }
    }
    
    @EventListener
    public void handleUserDeleted(UserDeletedEvent event) {
        log.info("사용자 삭제 이벤트 수신: userId={}", event.getUserId());
        
        // 사용자 삭제 시 관련 채팅방 정리
        chatService.cleanupUserChatRooms(UUID.fromString(event.getUserId()));
    }
}
```

### 🌐 WebSocket 구현

#### 1. WebSocket 설정

**WebSocketConfig.java**:
```java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new ChatWebSocketHandler(), "/ws/chat")
                .setAllowedOrigins("*")
                .withSockJS();
    }
}
```

#### 2. WebSocket 핸들러

**ChatWebSocketHandler.java**:
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketHandler extends TextWebSocketHandler {
    
    private final ChatService chatService;
    private final UserIntegrationService userIntegrationService;
    private final SimpMessagingTemplate messagingTemplate;
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket 연결 성립: {}", session.getId());
        
        // 사용자 인증 확인
        String userId = getUserIdFromSession(session);
        if (userId != null) {
            UserDto user = userIntegrationService.getUserById(userId);
            if (user != null) {
                session.getAttributes().put("userId", userId);
                log.info("사용자 인증 완료: userId={}", userId);
            } else {
                session.close();
            }
        } else {
            session.close();
        }
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String userId = (String) session.getAttributes().get("userId");
        if (userId == null) {
            session.close();
            return;
        }
        
        // 메시지 처리
        ChatMessage chatMessage = parseMessage(message.getPayload());
        Message savedMessage = chatService.sendMessage(
            chatMessage.getRoomId(),
            UUID.fromString(userId),
            chatMessage.getBotId(),
            chatMessage.getContent(),
            "user"
        );
        
        // 실시간 메시지 브로드캐스트
        messagingTemplate.convertAndSend(
            "/topic/chat/" + chatMessage.getRoomId(),
            savedMessage
        );
    }
    
    private String getUserIdFromSession(WebSocketSession session) {
        // JWT 토큰에서 사용자 ID 추출 로직
        return null; // 구현 필요
    }
    
    private ChatMessage parseMessage(String payload) {
        // JSON 파싱 로직
        return null; // 구현 필요
    }
}
```

### 🔄 모놀리식 스타일 개발 시 고려사항

#### 1. API Gateway를 통한 통신

**장점**:
- 중앙화된 라우팅 관리
- Rate Limiting 적용
- 로깅 및 모니터링 용이

**구현 방법**:
```java
@RestController
@RequestMapping("/api/chat")
public class ChatController {
    
    @PostMapping("/rooms")
    public ResponseEntity<ApiResponse<ChatRoom>> createChatRoom(
            @RequestBody CreateChatRoomRequest request,
            @RequestHeader("Authorization") String token) {
        
        // JWT 토큰에서 사용자 정보 추출
        String userId = extractUserIdFromToken(token);
        
        // 사용자 정보 검증 (User Service 호출)
        UserDto user = userIntegrationService.getUserById(userId);
        if (user == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("사용자를 찾을 수 없습니다"));
        }
        
        // 채팅방 생성
        ChatRoom chatRoom = chatService.createChatRoom(
            UUID.fromString(userId),
            request.getRoomName(),
            request.getBotId()
        );
        
        return ResponseEntity.ok(ApiResponse.success(chatRoom));
    }
}
```

#### 2. 직접 서비스 간 통신

**장점**:
- 빠른 응답 시간
- 네트워크 홉 감소

**구현 방법**:
```java
@Service
public class ChatService {
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${user.service.url}")
    private String userServiceUrl;
    
    public UserDto getUserById(String userId) {
        try {
            String url = userServiceUrl + "/api/users/" + userId;
            ResponseEntity<UserDto> response = restTemplate.getForEntity(url, UserDto.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("User Service 호출 실패: {}", e.getMessage());
            return null;
        }
    }
}
```

### 📊 모니터링 및 로깅

#### 1. Actuator 설정

**application.yml**:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
    prometheus:
      enabled: true
```

#### 2. 커스텀 메트릭

**ChatMetrics.java**:
```java
@Component
@RequiredArgsConstructor
public class ChatMetrics {
    
    private final MeterRegistry meterRegistry;
    private final Counter messagesSentCounter;
    private final Timer messageProcessingTimer;
    
    public ChatMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.messagesSentCounter = Counter.builder("chat.messages.sent")
                .description("Number of messages sent")
                .register(meterRegistry);
        this.messageProcessingTimer = Timer.builder("chat.message.processing.time")
                .description("Message processing time")
                .register(meterRegistry);
    }
    
    public void incrementMessagesSent() {
        messagesSentCounter.increment();
    }
    
    public void recordMessageProcessingTime(Duration duration) {
        messageProcessingTimer.record(duration);
    }
}
```

### 🚀 배포 및 운영

#### 1. Docker 설정

**Dockerfile.chat**:
```dockerfile
FROM openjdk:21-jdk-slim

WORKDIR /app

COPY chat/build/libs/chat-*.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### 2. 헬스체크

**HealthIndicator.java**:
```java
@Component
@RequiredArgsConstructor
public class ChatHealthIndicator implements HealthIndicator {
    
    private final UserIntegrationService userIntegrationService;
    private final ChatRoomRepository chatRoomRepository;
    
    @Override
    public Health health() {
        try {
            // User Service 연결 확인
            String userServiceHealth = userIntegrationService.healthCheck();
            if (!"User Service is running".equals(userServiceHealth)) {
                return Health.down()
                        .withDetail("user-service", "unavailable")
                        .build();
            }
            
            // 데이터베이스 연결 확인
            chatRoomRepository.count();
            
            return Health.up()
                    .withDetail("user-service", "available")
                    .withDetail("database", "connected")
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
```

### 📝 개발 체크리스트

#### 필수 구현 사항
- [ ] Feign Client 설정 및 구현
- [ ] JWT 토큰 검증 로직
- [ ] Circuit Breaker 패턴 적용
- [ ] 이벤트 기반 통신 구현
- [ ] WebSocket 실시간 통신
- [ ] Redis 캐싱 적용
- [ ] 헬스체크 엔드포인트
- [ ] 로깅 및 모니터링

#### 권장 구현 사항
- [ ] 메시지 암호화
- [ ] 채팅방 권한 관리
- [ ] 메시지 히스토리 페이징
- [ ] 실시간 사용자 상태 표시
- [ ] 파일 업로드 기능
- [ ] 메시지 검색 최적화

---

## 📚 참고 자료

- [Spring Cloud Gateway 공식 문서](https://spring.io/projects/spring-cloud-gateway)
- [Feign Client 가이드](https://spring.io/projects/spring-cloud-openfeign)
- [Resilience4j 문서](https://resilience4j.readme.io/)
- [Spring WebSocket 가이드](https://spring.io/guides/gs/messaging-stomp-websocket/)
- [JWT with Spring Security](https://spring.io/guides/tutorials/spring-security-and-angular-js/)

---

**문서 버전**: v1.0  
**최종 업데이트**: 2025-01-27  
**작성자**: DoranDoran 개발팀
