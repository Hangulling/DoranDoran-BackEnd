# 🚀 DoranDoran MSA 개발자 온보딩 매뉴얼

## 📋 목차

1. [프로젝트 개요](#1-프로젝트-개요)
2. [아키텍처 구조](#2-아키텍처-구조)
3. [개발 환경 설정](#3-개발-환경-설정)
4. [서비스 간 통신 구조](#4-서비스-간-통신-구조)
5. [API 사용법](#5-api-사용법)
6. [개발 가이드라인](#6-개발-가이드라인)
7. [모니터링 및 디버깅](#7-모니터링-및-디버깅)
8. [문제 해결](#8-문제-해결)

---

## 1. 프로젝트 개요

### DoranDoran MSA 프로젝트
- **목적**: 모듈러 모놀리스에서 진정한 마이크로서비스 아키텍처로 전환
- **기술 스택**: Spring Boot 3.3.4, Java 21, PostgreSQL, Redis, Docker
- **아키텍처**: MSA (Microservices Architecture)
- **현재 상태**: Gateway, Auth, User 서비스 정상 운영 중

### 현재 운영 중인 서비스

| 서비스 | 포트 | 역할 | 상태 |
|--------|------|------|------|
| **API Gateway** | 8080 | 모든 외부 요청의 단일 진입점 | ✅ 운영 중 |
| **Auth Service** | 8081 | JWT 인증/인가, 토큰 관리 | ✅ 운영 중 |
| **User Service** | 8082 | 사용자 관리, 프로필 관리 | ✅ 운영 중 |
| **Chat Service** | 8083 | 채팅 관리 | ⚠️ 개발 중 |
| **Batch Service** | 8085 | 배치 작업 | ⚠️ 개발 중 |

---

## 2. 아키텍처 구조

### 🌐 전체 아키텍처 다이어그램

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   API Gateway   │    │  Auth Service   │    │  User Service   │
│   (Port 8080)   │◄──►│   (Port 8081)   │◄──►│   (Port 8082)   │
│                 │    │                 │    │                 │
│ • 라우팅        │    │ • JWT 인증      │    │ • 사용자 관리   │
│ • Rate Limiting │    │ • 토큰 검증     │    │ • 프로필 관리   │
│ • CORS 설정     │    │ • 권한 관리     │    │ • 이벤트 발행   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  Chat Service   │    │ Store Service   │    │ Batch Service   │
│   (Port 8083)   │    │   (Port 8084)   │    │   (Port 8085)   │
│                 │    │                 │    │                 │
│ • 채팅 관리     │    │ • 상품 관리     │    │ • 배치 작업     │
│ • 메시지 처리   │    │ • 주문 처리     │    │ • 스케줄링     │
│ • 실시간 통신   │    │ • 결제 처리     │    │ • 데이터 처리   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

### 🗄️ 데이터베이스 구조

- **PostgreSQL (포트 5432)**: 공유 데이터베이스
  - `auth` 스키마: 인증 관련 데이터
  - `user` 스키마: 사용자 관련 데이터
  - `chat` 스키마: 채팅 관련 데이터
  - `store` 스키마: 상점 관련 데이터
  - `batch` 스키마: 배치 관련 데이터

- **Redis (포트 6379)**: 캐싱 및 Rate Limiting

---

## 3. 개발 환경 설정

### 🛠️ 필수 도구

1. **Java 21** - JDK 설치
2. **Docker Desktop** - 컨테이너 실행
3. **PostgreSQL** - 데이터베이스 (선택사항, Docker 사용 시 불필요)
4. **IDE** - IntelliJ IDEA, VS Code 등

### 🚀 프로젝트 시작하기

```bash
# 1. 프로젝트 클론
git clone [repository-url]
cd DoranDoran

# 2. Docker Desktop 실행 확인
docker --version
docker compose --version

# 3. MSA 환경 시작
.\start-dev.bat

# 4. 서비스 상태 확인
docker compose -f docker/docker-compose.yml ps
```

### 📊 서비스 접속 URL

- **API Gateway**: http://localhost:8080
- **Auth Service**: http://localhost:8081
- **User Service**: http://localhost:8082
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin123)

---

## 4. 서비스 간 통신 구조

### 🔄 통신 패턴

#### 1. **API Gateway를 통한 외부 요청**
```
클라이언트 → API Gateway → 각 서비스
```

#### 2. **서비스 간 직접 통신 (Feign Client)**
```
Auth Service → User Service (REST API)
```

#### 3. **이벤트 기반 통신**
```
User Service → 이벤트 발행 → Auth Service (이벤트 리스닝)
```

### 🌐 Gateway 라우팅 규칙

| 경로 | 대상 서비스 | 설명 |
|------|-------------|------|
| `/api/auth/**` | Auth Service (8081) | 인증 관련 API |
| `/api/users/**` | User Service (8082) | 사용자 관리 API |
| `/api/chat/**` | Chat Service (8083) | 채팅 관련 API |
| `/api/store/**` | Store Service (8084) | 상점 관련 API |
| `/api/batch/**` | Batch Service (8085) | 배치 관련 API |

### 🔧 Rate Limiting 설정

| 서비스 | 초당 요청 수 | 버스트 용량 |
|--------|-------------|------------|
| Auth | 10 | 20 |
| User | 20 | 40 |
| Chat | 30 | 60 |
| Store | 15 | 30 |
| Batch | 5 | 10 |

---

## 5. API 사용법

### 🔐 Auth Service API

#### 로그인
```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

**응답:**
```json
{
  "success": true,
  "message": "로그인에 성공했습니다.",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "userId": "123e4567-e89b-12d3-a456-426614174000",
    "email": "user@example.com",
    "name": "홍길동"
  }
}
```

#### 토큰 검증
```http
GET http://localhost:8080/api/auth/validate
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

#### 토큰 갱신
```http
POST http://localhost:8080/api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### 👤 User Service API

#### 사용자 생성
```http
POST http://localhost:8080/api/users
Content-Type: application/json

{
  "email": "newuser@example.com",
  "password": "password123",
  "firstName": "홍",
  "lastName": "길동",
  "picture": "https://example.com/profile.jpg",
  "info": "안녕하세요!"
}
```

#### 사용자 조회
```http
GET http://localhost:8080/api/users/{userId}
```

#### 이메일로 사용자 조회
```http
GET http://localhost:8080/api/users/email/{email}
```

#### 사용자 업데이트
```http
PUT http://localhost:8080/api/users/{userId}
Content-Type: application/json

{
  "firstName": "김",
  "lastName": "철수",
  "picture": "https://example.com/new-profile.jpg",
  "info": "정보 업데이트!"
}
```

#### 사용자 상태 변경
```http
PATCH http://localhost:8080/api/users/{userId}/status?status=INACTIVE
```

### 🏠 Gateway API

#### 서비스 정보 조회
```http
GET http://localhost:8080/
```

**응답:**
```json
{
  "message": "DoranDoran MSA API Gateway",
  "status": "running",
  "version": "1.0.0",
  "endpoints": {
    "actuator": "/actuator",
    "auth": "/api/auth/**",
    "user": "/api/users/**",
    "chat": "/api/chat/**",
    "store": "/api/store/**"
  }
}
```

---

## 6. 개발 가이드라인

### 📁 프로젝트 구조

```
DoranDoran/
├── auth/                   # 인증 서비스
│   ├── src/main/java/
│   │   └── com/dorandoran/auth/
│   │       ├── controller/     # REST API 컨트롤러
│   │       ├── service/        # 비즈니스 로직
│   │       ├── client/         # Feign 클라이언트
│   │       └── dto/           # 데이터 전송 객체
│   └── src/main/resources/
│       └── application.yml    # 설정 파일
├── user/                   # 사용자 서비스
├── gateway/                # API Gateway
├── shared/                 # 공통 DTO 및 이벤트
├── common/                 # 공통 유틸리티
└── infra/                  # 인프라 모듈
```

### 🔧 개발 규칙

#### 1. **API 설계 원칙**
- RESTful API 설계 준수
- HTTP 상태 코드 적절히 사용
- 일관된 응답 형식 (`ApiResponse` 사용)

#### 2. **에러 처리**
```java
// 공통 예외 사용
throw new DoranDoranException(ErrorCode.USER_NOT_FOUND);

// 컨트롤러에서 예외 처리
try {
    // 비즈니스 로직
} catch (DoranDoranException e) {
    return ResponseEntity.badRequest()
        .body(ApiResponse.error(e.getMessage(), e.getErrorCode().getCode()));
}
```

#### 3. **로깅**
```java
@Slf4j
public class UserService {
    
    public UserDto createUser(CreateUserRequest request) {
        log.info("사용자 생성 요청: email={}", request.email());
        
        try {
            // 비즈니스 로직
            log.info("사용자 생성 완료: id={}, email={}", user.getId(), user.getEmail());
        } catch (Exception e) {
            log.error("사용자 생성 실패: email={}, error={}", request.email(), e.getMessage());
            throw e;
        }
    }
}
```

#### 4. **서비스 간 통신**
```java
// Feign 클라이언트 사용
@FeignClient(name = "user-service", url = "${user.service.url}")
public interface UserServiceClient {
    
    @GetMapping("/api/users/email/{email}")
    UserDto getUserByEmail(@PathVariable String email);
}

// Circuit Breaker 적용
@CircuitBreaker(name = "user-service", fallbackMethod = "getUserByEmailFallback")
public UserDto getUserByEmail(String email) {
    return userServiceClient.getUserByEmail(email);
}
```

### 🧪 테스트 작성

#### 단위 테스트
```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private UserService userService;
    
    @Test
    void 사용자_생성_성공() {
        // Given
        CreateUserRequest request = new CreateUserRequest(
            "test@example.com", "password123", "홍", "길동", null, null
        );
        
        // When & Then
        assertThatCode(() -> userService.createUser(request))
            .doesNotThrowAnyException();
    }
}
```

#### 통합 테스트
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb"
})
class UserControllerIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void 사용자_생성_API_테스트() {
        // Given
        CreateUserRequest request = new CreateUserRequest(
            "test@example.com", "password123", "홍", "길동", null, null
        );
        
        // When
        ResponseEntity<UserDto> response = restTemplate.postForEntity(
            "/api/users", request, UserDto.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
```

---

## 7. 모니터링 및 디버깅

### 📊 Grafana 모니터링

#### 접속 정보
- **URL**: http://localhost:3000
- **사용자명**: admin
- **비밀번호**: admin123

#### 주요 메트릭
- **서비스 상태**: Health Check
- **응답 시간**: HTTP 요청 응답 시간
- **메모리 사용량**: JVM 메모리 사용률
- **데이터베이스 연결**: DB 연결 수

### 🔍 로그 확인

#### Docker 로그
```bash
# 특정 서비스 로그 확인
docker compose -f docker/docker-compose.yml logs -f auth-service

# 모든 서비스 로그 확인
docker compose -f docker/docker-compose.yml logs -f
```

#### 서비스별 로그 레벨
- **Auth Service**: DEBUG
- **User Service**: DEBUG
- **Gateway**: DEBUG

### 🐛 디버깅 팁

#### 1. **서비스 상태 확인**
```bash
# 헬스체크
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
```

#### 2. **API 테스트**
```bash
# Gateway를 통한 API 호출
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'
```

#### 3. **데이터베이스 확인**
```sql
-- PostgreSQL 접속
psql -h localhost -p 5432 -U doran -d dorandoran

-- 스키마별 테이블 확인
\dt auth.*
\dt user.*
```

---

## 8. 문제 해결

### 🚨 자주 발생하는 문제

#### 1. **서비스 시작 실패**
```bash
# 해결 방법
docker compose -f docker/docker-compose.yml down
docker compose -f docker/docker-compose.yml build --no-cache
docker compose -f docker/docker-compose.yml up -d
```

#### 2. **데이터베이스 연결 실패**
- PostgreSQL 컨테이너 상태 확인
- 포트 충돌 확인 (5432)
- 환경 변수 확인

#### 3. **서비스 간 통신 실패**
- 네트워크 연결 확인
- Feign 클라이언트 설정 확인
- Circuit Breaker 상태 확인

#### 4. **Rate Limiting 오류**
- Redis 연결 확인
- Rate Limiting 설정 확인
- 요청 빈도 조정

#### 5. **403 Forbidden 오류 (API 접근 거부)**
```bash
# User Service 보안 설정 확인
# SecurityConfig.java에서 API 경로 허용 설정 확인
.requestMatchers("/api/**").permitAll()

# 서비스 재시작
docker compose -f docker/docker-compose.yml restart user-service
```

#### 6. **CORS 오류**
```yaml
# Gateway 설정에서 CORS 설정 확인
globalcors:
  cors-configurations:
    '[/**]':
      allowedOriginPatterns: "*"  # allowedOrigins 대신 사용
      allowedMethods: "*"
      allowedHeaders: "*"
      allowCredentials: true
```

### 📞 지원 및 문의

- **프로젝트 문서**: `MSA_ARCHITECTURE.md`, `PROJECT_STATUS_REPORT.md`
- **Grafana 매뉴얼**: `grafana_manual.md`
- **개발 로드맵**: `DEVELOPMENT_ROADMAP.md`

---

## 🎯 다음 단계

### 📋 개발 우선순위

1. **Chat Service 완성** - 채팅 기능 구현
2. **Store Service 완성** - 상품/주문 기능 구현
3. **Batch Service 완성** - 배치 작업 구현
4. **테스트 코드 작성** - 단위/통합 테스트
5. **모니터링 강화** - 알림 시스템 구축

### 🚀 새로운 기능 개발

1. **서비스 추가 시**:
   - `settings.gradle.kts`에 모듈 추가
   - `docker-compose.yml`에 서비스 추가
   - Gateway 라우팅 규칙 추가

2. **API 추가 시**:
   - 컨트롤러에 엔드포인트 추가
   - DTO 클래스 생성
   - 서비스 로직 구현
   - 테스트 코드 작성

---

**🎉 온보딩을 완료하셨습니다! 이제 DoranDoran MSA 프로젝트에서 개발을 시작할 수 있습니다.**

*문서 업데이트: 2024년 12월 19일*
*버전: 1.0*
