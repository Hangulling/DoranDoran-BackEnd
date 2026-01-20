# DoranDoran 프로젝트 기술 스택 및 개발 경험 분석
## 신입 개발자를 위한 상세 가이드

> 이 문서는 신입 개발자가 프로젝트의 기술 스택과 개발 경험을 이해할 수 있도록 쉽게 풀어서 설명합니다.

## 목차
1. [사용 기술 스택 및 라이브러리](#1-사용-기술-스택-및-라이브러리)
2. [기술 선택 이유](#2-기술-선택-이유)
3. [힘들었던 점](#3-힘들었던-점)
4. [개선한 점](#4-개선한-점)
5. [트러블슈팅](#5-트러블슈팅)

---

## 1. 사용 기술 스택 및 라이브러리

### 1.1 백엔드 핵심 기술

#### 언어 및 프레임워크

##### Java 21
**간단히 말하면:** 서버 애플리케이션을 만드는 프로그래밍 언어

**왜 사용했나요?**
- Java는 기업용 애플리케이션 개발에 가장 널리 사용되는 언어입니다
- Java 21은 최신 장기 지원(LTS) 버전으로, 안정적이면서도 최신 기능을 제공합니다
- 예를 들어, `Record`라는 기능으로 간단하게 데이터 클래스를 만들 수 있습니다:
  ```java
  // 예전 방식 (Java 8)
  public class User {
      private String name;
      private String email;
      // getter, setter, constructor, equals, hashCode 등 수십 줄...
  }
  
  // Java 21 방식
  public record User(String name, String email) { }
  // 끝! 자동으로 getter, equals, hashCode 등이 생성됨
  ```

**실제 사용 예시:**
- 모든 서비스의 백엔드 코드 작성
- 데이터베이스와 통신하는 코드
- API 엔드포인트 구현

---

##### Spring Boot 3.3.4
**간단히 말하면:** Java로 웹 애플리케이션을 쉽게 만들 수 있게 해주는 프레임워크

**왜 사용했나요?**
- Spring Boot는 "설정보다 관례(Convention over Configuration)" 원칙을 따릅니다
  - 즉, 복잡한 설정 없이도 기본적으로 잘 작동하도록 만들어져 있습니다
- 예를 들어, 데이터베이스 연결 설정을 `application.yml` 파일에만 작성하면 자동으로 연결이 됩니다:
  ```yaml
  spring:
    datasource:
      url: jdbc:postgresql://localhost:5432/dorandoran
      username: doran
      password: doran
  ```
  이렇게만 작성하면 Spring Boot가 알아서 데이터베이스 연결을 관리합니다!

**마이크로서비스 아키텍처란?**
- 큰 애플리케이션을 작은 서비스들로 나눈 것입니다
- 예를 들어:
  - **Auth Service**: 로그인/회원가입만 담당
  - **Chat Service**: 채팅 기능만 담당
  - **User Service**: 사용자 정보만 담당
- 각 서비스를 독립적으로 개발하고 배포할 수 있어서 유지보수가 쉬워집니다

**Actuator란?**
- 애플리케이션의 상태를 확인할 수 있는 도구입니다
- 예를 들어, `/actuator/health` 엔드포인트를 호출하면 서비스가 정상 작동하는지 확인할 수 있습니다:
  ```json
  {
    "status": "UP",
    "components": {
      "db": {"status": "UP"},
      "redis": {"status": "UP"}
    }
  }
  ```

---

##### Spring Cloud Gateway 4.1.0
**간단히 말하면:** 모든 요청이 들어오는 "문지기" 역할을 하는 서비스

**왜 필요한가요?**
- 우리 프로젝트는 6개의 서비스(Auth, User, Chat, Store, Batch, Gateway)로 나뉘어 있습니다
- 사용자는 이 6개 서비스의 주소를 모두 알 필요가 없습니다
- Gateway가 모든 요청을 받아서 적절한 서비스로 전달해줍니다

**실제 동작 예시:**
```
사용자 요청: GET https://api.doran-chat.com/api/chat/rooms
                ↓
         [Gateway Service]
                ↓
    "이 요청은 Chat Service로 가야겠다"
                ↓
         [Chat Service]
                ↓
    "채팅방 목록을 조회해서 반환"
                ↓
         [Gateway Service]
                ↓
    사용자에게 응답 전달
```

**Reactive WebFlux란?**
- **기존 방식 (블로킹 I/O)**: 한 요청을 처리하는 동안 다른 요청을 기다려야 함
  - 예: 식당에서 한 테이블의 주문을 완전히 처리한 후 다음 테이블을 받음
- **Reactive 방식 (논블로킹 I/O)**: 여러 요청을 동시에 처리할 수 있음
  - 예: 식당에서 여러 테이블의 주문을 동시에 받아서 처리
- **결과**: 더 많은 사용자를 동시에 처리할 수 있습니다!

**SSE (Server-Sent Events)란?**
- 실시간으로 서버에서 클라이언트로 데이터를 보낼 수 있는 기술입니다
- 채팅 메시지가 도착하면 즉시 화면에 표시되도록 하는 데 사용했습니다
- WebSocket과 달리 서버 → 클라이언트만 가능하지만, 구현이 더 간단합니다

#### 빌드 도구
##### Gradle 8.0+
**간단히 말하면:** 코드를 컴파일하고 실행 가능한 파일로 만들어주는 도구

**왜 필요한가요?**
- Java 코드는 사람이 읽을 수 있는 텍스트 파일입니다
- 하지만 컴퓨터가 실행하려면 바이너리 파일로 변환해야 합니다
- Gradle이 이 변환 작업을 자동으로 해줍니다

**실제 사용 예시:**
```bash
# 프로젝트 빌드 (컴파일 + 패키징)
./gradlew build

# 특정 서비스만 빌드
./gradlew :chat:build

# 테스트 실행
./gradlew test
```

**멀티 모듈 프로젝트란?**
- 하나의 큰 프로젝트를 여러 개의 작은 모듈로 나눈 것입니다
- 예를 들어:
  - `common` 모듈: 모든 서비스가 공통으로 사용하는 코드
  - `chat` 모듈: Chat Service만의 코드
  - `auth` 모듈: Auth Service만의 코드
- 이렇게 나누면 코드 재사용이 쉬워집니다

---

### 1.2 데이터베이스 및 캐싱

#### 데이터베이스

##### PostgreSQL 17
**간단히 말하면:** 데이터를 저장하고 관리하는 저장소

**왜 PostgreSQL을 선택했나요?**
- MySQL보다 더 강력한 기능을 제공합니다
- 특히 **JSONB**라는 기능이 있어서 유연한 데이터 저장이 가능합니다

**Shared Database 패턴이란?**
- 여러 서비스가 하나의 데이터베이스를 공유하되, **스키마**로 분리한 것입니다
- 스키마는 데이터베이스 안의 "폴더" 같은 개념입니다
- 예를 들어:
  ```
  PostgreSQL 데이터베이스
  ├── auth_schema (인증 관련 데이터)
  │   ├── refresh_tokens 테이블
  │   └── token_blacklist 테이블
  ├── user_schema (사용자 정보)
  │   └── app_user 테이블
  ├── chat_schema (채팅 데이터)
  │   ├── chatrooms 테이블
  │   └── messages 테이블
  └── ...
  ```
- 이렇게 하면 서비스별로 데이터가 분리되어 관리가 쉬워집니다

**JSONB란?**
- JSON 형식의 데이터를 데이터베이스에 저장할 수 있게 해주는 타입입니다
- 예를 들어, 채팅방 설정을 저장할 때:
  ```sql
  -- 일반적인 방식: 각 설정을 별도 컬럼으로
  CREATE TABLE chatrooms (
      id UUID,
      name VARCHAR,
      concept VARCHAR,  -- FRIEND, COWORKER 등
      test_model VARCHAR
  );
  
  -- JSONB 방식: 모든 설정을 하나의 컬럼에
  CREATE TABLE chatrooms (
      id UUID,
      name VARCHAR,
      settings JSONB  -- {"concept": "FRIEND", "testModel": "a"}
  );
  ```
- JSONB의 장점:
  - 나중에 새로운 설정을 추가해도 테이블 구조를 변경할 필요가 없습니다
  - 유연하게 데이터를 저장할 수 있습니다

---

##### Flyway 10.8.1
**간단히 말하면:** 데이터베이스 스키마 변경을 버전 관리하는 도구

**왜 필요한가요?**
- 코드는 Git으로 버전 관리하지만, 데이터베이스 스키마는 어떻게 관리하나요?
- Flyway가 데이터베이스 변경 사항을 파일로 관리해줍니다

**실제 사용 예시:**
```sql
-- V1__create_users_table.sql
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL
);

-- V2__add_name_to_users.sql
ALTER TABLE users ADD COLUMN name VARCHAR(255);
```
- 이렇게 파일로 관리하면:
  - 누가 언제 어떤 변경을 했는지 추적 가능
  - 새로운 환경에 배포할 때 자동으로 스키마 생성
  - 롤백도 가능

---

#### 캐싱

##### Redis 7
**간단히 말하면:** 매우 빠른 메모리 기반 저장소 (데이터베이스보다 훨씬 빠름)

**왜 필요한가요?**
- 데이터베이스는 하드디스크에 저장되어 있어서 읽기/쓰기가 느립니다
- Redis는 메모리에 저장되어 있어서 매우 빠릅니다
- 하지만 메모리는 비싸고 용량이 제한적이므로, 자주 사용하는 데이터만 저장합니다

**실제 사용 예시:**

**1. 사용자 정보 캐싱**
```
사용자가 "내 정보 조회" 요청
    ↓
Redis에 캐시가 있는지 확인
    ↓
[캐시 있음] → 즉시 반환 (0.5ms) ✅ 빠름!
[캐시 없음] → 데이터베이스 조회 (3-4ms) → Redis에 저장
```

**2. 토큰 블랙리스트**
- 사용자가 로그아웃하면 Access Token을 Redis에 저장합니다
- 다른 요청에서 이 토큰이 오면 Redis에서 확인하여 차단합니다
- TTL(Time To Live)을 설정하여 자동으로 만료되도록 합니다

**3. 채팅방 정보 캐싱**
- 채팅방 정보를 조회할 때마다 데이터베이스를 조회하면 느립니다
- 한 번 조회한 결과를 Redis에 30분간 저장해두면, 다음 조회 시 매우 빠르게 응답할 수 있습니다

**Spring Cache Abstraction이란?**
- Redis를 직접 사용하는 대신, Spring이 제공하는 간단한 방법을 사용합니다
- 예를 들어:
  ```java
  // 이렇게만 작성하면 자동으로 Redis에 캐싱됨!
  @Cacheable(value = "users", key = "#userId")
  public User findById(UUID userId) {
      return userRepository.findById(userId);
  }
  ```
- `@Cacheable` 어노테이션만 붙이면:
  1. 메서드 실행 전에 Redis에서 확인
  2. 있으면 그대로 반환 (데이터베이스 조회 안 함)
  3. 없으면 메서드 실행 후 결과를 Redis에 저장

### 1.3 서비스 간 통신

#### 동기 통신

##### Spring Cloud OpenFeign
**간단히 말하면:** 서비스 간 HTTP 통신을 쉽게 해주는 도구

**왜 필요한가요?**
- 우리 프로젝트는 여러 서비스로 나뉘어 있습니다
- 예를 들어, Auth Service가 User Service의 사용자 정보를 가져와야 할 때가 있습니다
- Feign을 사용하면 복잡한 HTTP 요청 코드를 작성할 필요가 없습니다

**실제 사용 예시:**

**Feign 없이 (복잡함):**
```java
// 매번 이렇게 작성해야 함
RestTemplate restTemplate = new RestTemplate();
HttpHeaders headers = new HttpHeaders();
headers.set("Authorization", "Bearer " + token);
HttpEntity<String> entity = new HttpEntity<>(headers);
ResponseEntity<UserDto> response = restTemplate.exchange(
    "http://user-service:8082/api/users/" + userId,
    HttpMethod.GET,
    entity,
    UserDto.class
);
UserDto user = response.getBody();
```

**Feign 사용 (간단함):**
```java
// 1. 인터페이스만 정의
@FeignClient(name = "user-service", url = "http://user-service:8082")
public interface UserServiceClient {
    @GetMapping("/api/users/{userId}")
    UserDto getUserById(@PathVariable UUID userId);
}

// 2. 사용
@Autowired
private UserServiceClient userServiceClient;

public void someMethod() {
    UserDto user = userServiceClient.getUserById(userId);  // 끝!
}
```

**선언적이란?**
- "어떻게"가 아니라 "무엇을"만 선언하면 됩니다
- 즉, HTTP 요청의 세부 사항을 작성할 필요 없이, 메서드 시그니처만 작성하면 Feign이 알아서 처리합니다

**HMAC 인증 헤더 자동 주입이란?**
- 서비스 간 통신 시 보안을 위해 HMAC 서명을 추가해야 합니다
- Feign을 사용하면 이 작업을 자동으로 해줍니다
- 개발자는 신경 쓸 필요가 없습니다!

---

##### Spring Cloud LoadBalancer 4.1.0
**간단히 말하면:** 여러 서버 중 하나를 선택해서 요청을 보내주는 도구

**왜 필요한가요?**
- 나중에 Chat Service를 여러 개 실행할 수 있습니다 (부하 분산)
- 예: Chat Service 1, Chat Service 2, Chat Service 3
- LoadBalancer가 자동으로 적절한 서버를 선택해줍니다

**실제 동작:**
```
요청: GET http://chat-service/api/chat/rooms
         ↓
[LoadBalancer]
    "Chat Service가 3개 있네? 1번으로 보내자"
         ↓
[Chat Service 1]
```

---

#### 비동기 통신

##### Project Reactor (WebFlux)
**간단히 말하면:** 비동기 프로그래밍을 쉽게 해주는 라이브러리

**동기 vs 비동기란?**

**동기 방식 (기존):**
```java
// 순차적으로 실행됨
String result1 = callAPI1();  // 1초 걸림
String result2 = callAPI2();  // 1초 걸림
String result3 = callAPI3();  // 1초 걸림
// 총 3초 걸림
```

**비동기 방식 (Reactor):**
```java
// 병렬로 실행됨
Mono<String> result1 = callAPI1Async();  // 시작
Mono<String> result2 = callAPI2Async();  // 시작
Mono<String> result3 = callAPI3Async();  // 시작

// 모든 결과를 기다림
Mono.zip(result1, result2, result3)
    .subscribe(results -> {
        // 3개 모두 완료되면 실행
        // 총 1초 걸림 (가장 오래 걸리는 것 기준)
    });
```

**Mono/Flux란?**
- **Mono**: 0개 또는 1개의 결과를 비동기로 처리
  - 예: 사용자 정보 조회 (한 명만 반환)
- **Flux**: 0개 이상의 결과를 비동기로 처리
  - 예: 메시지 목록 조회 (여러 개 반환)

**실제 사용 예시 (멀티 에이전트 시스템):**
```java
// 친밀도 분석과 어휘 추출을 동시에 실행
Mono<IntimacyResponse> intimacyMono = intimacyAgent.analyze(chatroomId, content);
Mono<VocabularyResponse> vocabularyMono = vocabularyAgent.extract(chatroomId, content);

// 두 작업이 동시에 실행되고, 모두 완료되면 결과를 합침
Mono.zip(intimacyMono, vocabularyMono)
    .subscribe(results -> {
        // 두 결과를 모두 받아서 처리
    });
```

**백프레셔(Backpressure)란?**
- 데이터를 보내는 속도가 받는 속도보다 빠를 때 발생하는 문제
- Reactor가 자동으로 속도를 조절해줍니다
- 예: 물이 너무 빠르게 흐르면 밸브를 조절하는 것처럼

---

##### Server-Sent Events (SSE)
**간단히 말하면:** 서버에서 클라이언트로 실시간으로 데이터를 보내는 기술

**왜 필요한가요?**
- 일반적인 HTTP 요청은 클라이언트가 서버에 요청해야 응답을 받을 수 있습니다
- 하지만 채팅 메시지는 서버가 먼저 보내야 합니다
- SSE를 사용하면 서버가 클라이언트에게 자동으로 메시지를 보낼 수 있습니다

**실제 동작:**
```
1. 클라이언트: "SSE 연결 요청"
2. 서버: "연결 수락, 연결 유지"
3. [사용자가 메시지 전송]
4. 서버: "새 메시지가 도착했어요!" → 클라이언트에게 자동 전송
5. 클라이언트: 메시지 수신 및 화면에 표시
```

**WebSocket과의 차이:**
- **SSE**: 서버 → 클라이언트만 가능 (단방향)
- **WebSocket**: 서버 ↔ 클라이언트 모두 가능 (양방향)
- 우리는 사용자 메시지는 HTTP POST로 보내고, 서버 메시지만 SSE로 보내므로 SSE가 더 간단합니다

**자동 재연결이란?**
- 네트워크가 끊어지면 자동으로 다시 연결을 시도합니다
- 개발자가 별도로 처리할 필요가 없습니다

### 1.4 인증/인가

#### JWT (JSON Web Token)

##### jjwt 0.12.3
**간단히 말하면:** 사용자 인증 정보를 안전하게 전달하는 방법

**왜 필요한가요?**
- 사용자가 로그인하면, 이후 요청에서 "나는 로그인한 사용자야"를 증명해야 합니다
- 매번 사용자명/비밀번호를 보내면 보안상 위험합니다
- JWT 토큰을 사용하면 한 번 로그인한 후 토큰만 보내면 됩니다

**JWT 토큰이란?**
- 사용자 정보를 암호화한 문자열입니다
- 예: `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c`
- 이 토큰에는 사용자 ID, 이메일, 만료 시간 등이 포함되어 있습니다

**Access Token vs Refresh Token:**
- **Access Token (1시간)**: 실제 API 요청에 사용하는 토큰
  - 짧은 수명으로 보안 강화 (도난당해도 1시간 후 만료)
- **Refresh Token (7일)**: Access Token을 갱신할 때 사용
  - Access Token이 만료되면 Refresh Token으로 새 Access Token 발급

**실제 동작:**
```
1. 사용자: "로그인 요청" (이메일, 비밀번호)
2. 서버: "인증 성공! Access Token + Refresh Token 발급"
3. 사용자: 이후 모든 요청에 "Authorization: Bearer <Access Token>" 헤더 추가
4. 서버: "토큰 검증 → 사용자 정보 추출 → 요청 처리"
5. [1시간 후 Access Token 만료]
6. 사용자: "Refresh Token으로 새 Access Token 요청"
7. 서버: "새 Access Token 발급"
```

**HS256 알고리즘이란?**
- JWT 토큰을 서명하는 방법입니다
- 서명이 있으면 토큰이 위조되지 않았음을 확인할 수 있습니다
- 비밀키를 사용하여 서명하므로, 비밀키를 모르면 위조할 수 없습니다

---

#### 보안

##### Spring Security
**간단히 말하면:** 애플리케이션 보안을 처리해주는 프레임워크

**HMAC 서명 기반 서비스 간 인증이란?**
- Gateway에서 다른 서비스로 요청을 보낼 때, 이 요청이 진짜 Gateway에서 온 것인지 확인해야 합니다
- HMAC 서명을 사용하면 요청이 위조되지 않았음을 확인할 수 있습니다

**실제 동작:**
```
1. Gateway: "사용자 요청을 받음, JWT 검증 완료"
2. Gateway: "HMAC 서명 생성 (비밀키 + 사용자ID + 타임스탬프)"
3. Gateway → Chat Service: "요청 전달 + HMAC 헤더 추가"
4. Chat Service: "HMAC 서명 검증 → 통과하면 요청 처리"
```

**CORS 처리란?**
- 브라우저 보안 정책으로 인해, 다른 도메인에서 API를 호출할 수 없습니다
- 예: `https://www.doran-chat.com`에서 `https://api.doran-chat.com`으로 요청
- CORS 설정을 통해 허용된 도메인만 API를 호출할 수 있도록 합니다

**IP 블랙리스트 필터란?**
- 공격을 시도하는 IP 주소를 차단하는 기능입니다
- Gateway에서 모든 요청을 받을 때, IP 주소를 확인하여 블랙리스트에 있으면 차단합니다

---

### 1.5 장애 처리

#### Resilience4j 2.2.0
**간단히 말하면:** 서비스가 장애가 나도 전체 시스템이 멈추지 않도록 해주는 도구

##### Circuit Breaker (회로 차단기)
**간단히 말하면:** 문제가 있는 서비스 호출을 자동으로 차단하는 기능

**왜 필요한가요?**
- User Service가 다운되면, Auth Service가 User Service를 계속 호출하려고 시도합니다
- 이렇게 되면 Auth Service도 응답이 느려지고, 결국 전체 시스템이 느려집니다
- Circuit Breaker가 "User Service가 문제가 있네, 일단 호출을 멈추자"라고 판단합니다

**실제 동작:**
```
[정상 상태]
Auth Service → User Service: "사용자 정보 조회" ✅ 성공

[User Service 다운]
Auth Service → User Service: "사용자 정보 조회" ❌ 실패
Auth Service → User Service: "사용자 정보 조회" ❌ 실패
Auth Service → User Service: "사용자 정보 조회" ❌ 실패
... (5번 연속 실패)

[Circuit Breaker 작동]
Circuit Breaker: "User Service가 문제가 있네, 호출 차단!"
Auth Service → User Service: (호출 안 함, 바로 폴백 메서드 실행)
```

**폴백(Fallback)이란?**
- 원래 서비스 호출이 실패했을 때 대신 실행되는 메서드입니다
- 예: User Service 호출 실패 시 기본 사용자 정보를 반환

**실제 코드 예시:**
```java
@CircuitBreaker(name = "userService", fallbackMethod = "getUserFallback")
public UserDto getUserById(UUID userId) {
    return userServiceClient.getUserById(userId);  // User Service 호출
}

// 폴백 메서드
public UserDto getUserFallback(UUID userId, Exception e) {
    log.error("User Service 호출 실패, 기본값 반환", e);
    return UserDto.defaultUser();  // 기본 사용자 정보 반환
}
```

---

##### Retry (재시도)
**간단히 말하면:** 일시적인 오류일 때 자동으로 다시 시도하는 기능

**왜 필요한가요?**
- 네트워크가 일시적으로 불안정하거나, 서비스가 잠깐 과부하 상태일 수 있습니다
- 이런 경우 바로 실패 처리하는 대신, 몇 번 더 시도하면 성공할 수 있습니다

**지수 백오프 전략이란?**
- 재시도 간격을 점점 늘리는 전략입니다
- 예:
  - 1차 시도: 즉시
  - 2차 시도: 200ms 후
  - 3차 시도: 400ms 후
  - 4차 시도: 800ms 후
- 이렇게 하면 서비스 부하를 줄이면서 재시도할 수 있습니다

**실제 사용 예시:**
```java
@Retry(name = "userService")
public UserDto getUserById(UUID userId) {
    return userServiceClient.getUserById(userId);
}
// 실패하면 자동으로 3번 재시도 (설정에 따라)
```

### 1.6 모니터링 및 관찰성

**간단히 말하면:** 시스템이 잘 작동하는지 확인하고 문제를 찾는 도구들

#### 메트릭

##### Micrometer + Prometheus
**간단히 말하면:** 시스템의 상태를 숫자로 측정하고 저장하는 도구

**왜 필요한가요?**
- 서버가 잘 작동하는지 확인하려면 여러 지표를 봐야 합니다
- 예:
  - 초당 몇 개의 요청을 처리하는가?
  - 평균 응답 시간은 얼마인가?
  - 데이터베이스 연결 풀은 몇 개나 사용 중인가?
  - 메모리는 얼마나 사용 중인가?

**실제 수집하는 메트릭:**
- **HTTP 요청 수**: `http_requests_total{method="GET", status="200"} = 1234`
- **응답 시간**: `http_request_duration_seconds{quantile="0.5"} = 0.05` (중간값 50ms)
- **DB 연결 풀**: `hikari_connections_active = 5` (현재 5개 연결 사용 중)
- **JVM 메모리**: `jvm_memory_used_bytes{area="heap"} = 256000000` (256MB 사용 중)

**Prometheus란?**
- 메트릭을 수집하고 저장하는 도구입니다
- 시간에 따른 변화를 추적할 수 있습니다
- 예: "어제 이 시간보다 요청이 2배 늘었네?"

---

#### 로깅

##### Logback + Logstash Encoder
**간단히 말하면:** 애플리케이션에서 발생하는 모든 일을 기록하는 도구

**왜 필요한가요?**
- 문제가 발생했을 때, 로그를 보면 무엇이 잘못되었는지 알 수 있습니다
- 예:
  ```
  2025-12-07 10:30:15 ERROR [ChatService] 사용자 정보 조회 실패: userId=123
  ```
- 이 로그를 보면 "10시 30분 15초에 ChatService에서 사용자 정보 조회가 실패했구나"를 알 수 있습니다

**구조화된 JSON 로그란?**
- 일반 로그: `2025-12-07 10:30:15 ERROR 사용자 정보 조회 실패`
- JSON 로그: `{"timestamp":"2025-12-07T10:30:15","level":"ERROR","service":"ChatService","message":"사용자 정보 조회 실패","userId":"123"}`
- JSON 형식이면 프로그램으로 파싱하기 쉬워서 로그 분석이 편합니다

**Loki란?**
- 여러 서비스의 로그를 한 곳에 모아서 검색할 수 있게 해주는 도구입니다
- 예: "에러가 발생한 모든 로그를 찾아줘" → Loki가 모든 서비스의 로그를 검색

---

#### 대시보드

##### Grafana
**간단히 말하면:** 메트릭과 로그를 예쁘게 그래프로 보여주는 도구

**왜 필요한가요?**
- 숫자만 보면 이해하기 어렵습니다
- 그래프로 보면 한눈에 파악할 수 있습니다
- 예:
  - "응답 시간 그래프가 갑자기 올라갔네? 문제가 있나보다"
  - "메모리 사용량이 계속 증가하고 있네? 메모리 누수가 있나보다"

**실제 대시보드 예시:**
```
┌─────────────────────────────────────┐
│  HTTP 요청 수 (초당)                │
│  ▁▃▅▇█▇▅▃▁                          │
│  0    100   200   300   400         │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│  평균 응답 시간 (ms)                │
│  ▁▂▃▄▅▆▇█                           │
│  0    50    100   150   200         │
└─────────────────────────────────────┘
```

---

### 1.7 AI 통합

#### OpenAI API

##### GPT-4o-mini / GPT-4o
**간단히 말하면:** 인공지능 챗봇을 만드는 데 사용한 AI 모델

**왜 필요한가요?**
- 우리 프로젝트는 한국어 학습 챗봇입니다
- 사용자가 한국어로 대화하면, AI가 자연스럽게 응답해야 합니다
- OpenAI의 GPT 모델을 사용하면 고품질의 대화를 생성할 수 있습니다

**멀티 에이전트 시스템이란?**
- 하나의 AI가 모든 일을 하는 게 아니라, 여러 AI 에이전트가 각자 역할을 나눠서 처리합니다
- 예:
  - **IntimacyAgent**: 사용자의 말투(존댓말/반말)를 분석
  - **VocabularyAgent**: 어려운 단어를 추출하고 설명
  - **ConversationAgent**: 자연스러운 대화 생성
  - **SummarizerAgent**: 대화 내용 요약

**실제 동작:**
```
사용자: "오늘 날씨가 좋네요"
    ↓
[병렬 실행]
    ├─→ IntimacyAgent: "존댓말 사용, 친밀도 레벨 2"
    └─→ ConversationAgent: "네, 정말 좋은 날씨네요! 산책하기 좋을 것 같아요"
         ↓
    VocabularyAgent: "산책"이라는 단어 설명 생성
         ↓
[결과 합치기]
    → 사용자에게 응답 전송
```

**스트리밍 응답 처리란?**
- 일반적으로 AI 응답을 기다리면 전체 응답이 완성될 때까지 기다려야 합니다
- 스트리밍을 사용하면 AI가 응답을 생성하는 대로 실시간으로 보여줄 수 있습니다
- 예:
  - 일반 방식: "생각 중..." → 3초 후 → "네, 정말 좋은 날씨네요!"
  - 스트리밍: "네," → "정말" → "좋은" → "날씨네요!" (실시간으로 표시)

**토큰 사용량 추적이란?**
- OpenAI API는 사용한 토큰(단어 단위)만큼 비용을 청구합니다
- 토큰 사용량을 추적하여 비용을 관리합니다
- 예: "이번 달에 100만 토큰을 사용했네, 비용은 $10이구나"

### 1.8 기타 라이브러리

#### 유틸리티
- **Lombok 1.18.30**
  - 보일러플레이트 코드 제거
  - `@Data`, `@Builder` 등 활용

- **Jackson**
  - JSON 직렬화/역직렬화
  - JSONB 데이터 처리

#### 파일 처리
- **Apache POI 5.2.5**
  - Excel 파일 읽기
  - 챗봇 프롬프트 관리

#### 이메일
- **Spring Mail**
  - Gmail SMTP 연동
  - 이메일 인증 코드 발송

#### OAuth
- **Google API Client 2.2.0**
  - Google OAuth 2.0 로그인
  - 토큰 검증 및 사용자 정보 조회

### 1.9 문서화

- **SpringDoc OpenAPI 2.2.0**
  - Swagger UI 자동 생성
  - API 문서화

### 1.10 인프라

#### 컨테이너화
- **Docker**
  - 서비스별 Dockerfile 작성
  - Docker Compose로 로컬 개발 환경 구성

#### 클라우드
- **AWS EC2 (t3.medium)**
  - 프로덕션 배포
  - Amazon Linux 2

---

## 2. 기술 선택 이유

> 이 섹션에서는 왜 이 기술을 선택했는지, 다른 기술과 비교했을 때 어떤 장단점이 있는지 설명합니다.

### 2.1 마이크로서비스 아키텍처

**마이크로서비스 아키텍처란?**
- 큰 애플리케이션을 작은 서비스들로 나누는 아키텍처 패턴입니다
- 예: 쇼핑몰 애플리케이션을
  - 사용자 서비스 (회원가입, 로그인)
  - 상품 서비스 (상품 조회, 검색)
  - 주문 서비스 (주문, 결제)
  - 배송 서비스 (배송 조회)
  로 나누는 것과 같습니다

**왜 선택했나요?**

**1. 서비스별 독립적 배포 및 스케일링**
- 각 서비스를 독립적으로 배포할 수 있습니다
- 예: Chat Service만 업데이트하고 싶을 때, 다른 서비스는 그대로 두고 Chat Service만 배포 가능
- 트래픽이 많은 서비스만 스케일링(서버 추가) 가능
  - 예: Chat Service만 서버를 3대로 늘리고, Batch Service는 1대로 유지

**2. 기술 스택 다양화 가능**
- 각 서비스마다 다른 기술을 사용할 수 있습니다
- 예: 
  - Gateway: Reactive (WebFlux) - 높은 동시성 필요
  - 다른 서비스: MVC - 일반적인 요청 처리
- 모놀리식 아키텍처에서는 전체가 같은 기술을 사용해야 함

**3. 장애 격리**
- 한 서비스가 다운되어도 다른 서비스는 계속 작동합니다
- 예: Chat Service가 다운되어도, 사용자는 로그인하고 사용자 정보를 조회할 수 있음
- Circuit Breaker를 사용하여 문제가 있는 서비스 호출을 자동으로 차단

**4. 팀별 독립적 개발**
- 각 팀이 서로 다른 서비스를 담당하여 독립적으로 개발 가능
- 예: Auth 팀은 Auth Service만, Chat 팀은 Chat Service만 담당

**대안과 비교:**

**모놀리식 아키텍처 (선택하지 않은 이유):**
- 모든 기능이 하나의 애플리케이션에 있음
- 장점: 개발 초기에는 간단함
- 단점:
  - 확장성 제한: 일부 기능만 트래픽이 많아도 전체를 스케일링해야 함
  - 기술 스택 고정: 전체가 같은 기술을 사용해야 함
  - 배포 시 전체를 다시 배포해야 함

**서버리스 (선택하지 않은 이유):**
- AWS Lambda, Google Cloud Functions 등
- 장점: 서버 관리 불필요, 사용한 만큼만 비용 지불
- 단점:
  - Cold Start 문제: 첫 요청이 느림
  - 비용 예측 어려움: 트래픽이 많아지면 비용이 급증할 수 있음
  - 장기 실행 작업에 부적합: SSE처럼 오래 유지되는 연결은 어려움

### 2.2 Spring Cloud Gateway (Reactive)

**선택 이유:**
- 높은 동시성 처리 (논블로킹 I/O)
- SSE 스트리밍 지원
- 필터 체인으로 인증/인가 처리
- Spring 생태계 통합

**대안 고려:**
- Nginx: 설정 복잡, 동적 라우팅 어려움
- Kong: 추가 인프라 필요, 학습 곡선

### 2.3 Reactor (Mono/Flux)

**선택 이유:**
- 비동기 프로그래밍으로 성능 최적화
- 백프레셔 지원
- 여러 에이전트 병렬 실행 용이
- 논블로킹 I/O로 높은 처리량

**대안 고려:**
- CompletableFuture: 체인 구성 복잡, 에러 처리 어려움
- RxJava: Spring 생태계와 통합 어려움

### 2.4 SSE vs WebSocket

**SSE 선택 이유:**
- 단방향 통신 (서버 → 클라이언트)으로 충분
- HTTP 기반으로 구현 간단
- 자동 재연결 지원
- Gateway에서 프록시 용이

**WebSocket 미선택 이유:**
- 양방향 통신이 불필요 (사용자 메시지는 HTTP POST)
- 구현 복잡도 증가
- Gateway 프록시 설정 복잡

### 2.5 PostgreSQL + JSONB

**선택 이유:**
- 유연한 스키마 변경 (JSONB)
- 부분 업데이트 가능
- 인덱싱 지원 (GIN 인덱스)
- 관계형 데이터와 비정형 데이터 혼합 사용

**대안 고려:**
- MongoDB: 트랜잭션 지원 제한, 관계형 데이터 처리 어려움
- MySQL: JSON 타입 지원 제한적

### 2.6 Redis 캐싱

**선택 이유:**
- 빠른 읽기 성능 (메모리 기반)
- TTL 자동 관리
- Spring Cache Abstraction 통합
- 토큰 블랙리스트 저장

**대안 고려:**
- Caffeine: 로컬 캐시, 분산 환경에서 동기화 어려움
- Memcached: 데이터 구조 제한적

### 2.7 Resilience4j

**선택 이유:**
- Spring Boot 통합 용이
- Circuit Breaker, Retry, Rate Limiting 통합
- 메트릭 수집 지원
- 가벼운 의존성

**대안 고려:**
- Hystrix: 유지보수 중단
- Sentinel: Alibaba 제품, 문서 부족

---

## 3. 힘들었던 점

> 이 섹션에서는 실제로 겪었던 문제들과 해결 과정을 자세히 설명합니다.

### 3.1 인프라 문제

#### 3.1.1 RDS OOM 문제 (2025-10-14)

**문제 상황 (쉽게 설명):**
- 갑자기 모든 API가 500 에러를 반환하기 시작했습니다
- 서버가 "메모리 부족" 에러를 내면서 죽어버렸습니다
- 사용자들이 서비스를 사용할 수 없게 되었습니다

**구체적인 증상:**
```
사용자: "채팅방 목록 조회 요청"
서버: "500 Internal Server Error" ❌

로그 확인:
java.lang.OutOfMemoryError: unable to create native thread
  at java.base/java.lang.Thread.start0(Native Method)
  at java.base/java.lang.Thread.start(Thread.java:1533)
```

**왜 이런 일이 발생했나요?**
- **OOM (Out Of Memory)**: 메모리가 부족해서 발생한 에러입니다
- **"unable to create native thread"**: 스레드(작업 단위)를 만들 메모리가 없어서 발생한 에러입니다
- **원인 분석**:
  1. **Hikari 커넥션 풀**: 데이터베이스 연결을 관리하는 도구인데, 너무 많은 연결을 만들려고 했습니다
  2. **JVM 메모리 설정**: Java가 사용할 수 있는 메모리 한도가 너무 작았습니다
  3. **기본 설정값 문제**: Spring Boot의 기본 설정이 우리 서버 환경에는 너무 과했습니다

**해결 과정 (단계별):**

**1단계: 문제 파악**
```bash
# 로그 확인
docker logs dorandoran-chat | grep -i "outofmemory"

# 메모리 사용량 확인
docker stats dorandoran-chat
# 결과: 메모리 사용량이 계속 증가하고 있음
```

**2단계: JVM 메모리 옵션 조정**
```bash
# 이전 (기본값, 너무 큼)
# -Xmx: 최대 힙 메모리 (기본값: 시스템 메모리의 25%)
# 문제: 서버 메모리가 4GB인데 1GB를 사용하려고 함

# 수정 후
JAVA_TOOL_OPTIONS="-Xms256m -Xmx512m"
# 의미:
# -Xms256m: 시작 시 256MB 메모리 할당
# -Xmx512m: 최대 512MB까지만 사용
```

**3단계: Tomcat 스레드 제한**
```bash
# 이전 (기본값: 200개)
# 문제: 동시에 200개의 요청을 처리하려고 스레드를 만들려고 함

# 수정 후
SERVER_TOMCAT_THREADS_MAX=40
# 의미: 최대 40개의 스레드만 사용
```

**4단계: Hikari 커넥션 풀 크기 조정**
```bash
# 이전 (기본값: 10개)
# 문제: 데이터베이스 연결 10개를 만들려고 했지만 메모리 부족

# 수정 후
SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=5
# 의미: 최대 5개의 데이터베이스 연결만 사용
```

**결과:**
- ✅ 메모리 사용량이 안정적으로 유지됨
- ✅ 500 에러가 사라지고 정상 작동
- ✅ 서버가 안정적으로 실행됨

**배운 점:**
1. **기본값이 항상 최적인 것은 아니다**: 환경에 맞게 조정해야 합니다
2. **리소스 한도를 명시적으로 설정하자**: 메모리, 스레드, 연결 수 등을 명확히 지정해야 합니다
3. **모니터링이 중요하다**: 문제가 발생하기 전에 미리 감지할 수 있어야 합니다

#### 3.1.2 Connection Pool Exhaustion (2025-10-14)

**문제 상황 (쉽게 설명):**
- 모든 API 요청이 30초 동안 기다리다가 실패했습니다
- "503 Service Unavailable" 에러가 발생했습니다
- 데이터베이스 연결이 부족해서 발생한 문제였습니다

**구체적인 증상:**
```
사용자: "채팅방 목록 조회 요청"
서버: [30초 대기...]
서버: "503 Service Unavailable" ❌

로그 확인:
HikariPool - Connection is not available, request timed out after 30000ms
```

**Connection Pool이란?**
- 데이터베이스 연결을 미리 만들어서 "풀"에 보관해두는 것입니다
- 요청이 오면 풀에서 연결을 빌려서 사용하고, 사용이 끝나면 다시 풀에 반환합니다
- 예를 들어, 연결 풀에 10개의 연결이 있다면:
  ```
  [연결1] [연결2] [연결3] ... [연결10]  ← 사용 가능한 연결들
  
  요청1 → 연결1 사용 → 작업 완료 → 연결1 반환
  요청2 → 연결2 사용 → 작업 완료 → 연결2 반환
  ...
  요청11 → [모든 연결 사용 중] → 대기...
  ```

**왜 이런 일이 발생했나요?**
- **핵심 원인**: Connection Leak (연결 누수)
  - 연결을 빌려갔는데 반환하지 않아서, 연결이 계속 줄어들었습니다
  - 마치 도서관에서 책을 빌려갔는데 반납하지 않아서, 책이 점점 없어지는 것과 같습니다

**구체적인 원인 분석:**

**1. SSE (Server-Sent Events)의 특성**
- SSE는 실시간 통신을 위해 연결을 오래 유지합니다
- 예: 채팅방에 접속하면 연결이 몇 분, 몇 시간 동안 유지될 수 있습니다

**2. `spring.jpa.open-in-view=true` 설정의 문제**
- 이 설정은 HTTP 요청이 시작될 때 데이터베이스 연결을 열고, 요청이 끝날 때 닫습니다
- 일반적인 요청 (예: 100ms): 연결을 빌려서 사용하고 바로 반환 ✅
- SSE 요청 (예: 10분): 연결을 빌려서 10분 동안 계속 점유 ❌

**3. 실제 발생 시나리오:**
```
[시작] 연결 풀: [1][2][3][4][5][6][7][8][9][10] (10개 사용 가능)

사용자1: SSE 연결 시작 → 연결1 사용 (10분 동안 점유)
사용자2: SSE 연결 시작 → 연결2 사용 (10분 동안 점유)
사용자3: SSE 연결 시작 → 연결3 사용 (10분 동안 점유)
...
사용자10: SSE 연결 시작 → 연결10 사용 (10분 동안 점유)

[10분 후] 연결 풀: [] (0개 사용 가능, 모두 점유 중)

사용자11: "채팅방 목록 조회" 요청
  → 연결이 없음! → 30초 대기 → 타임아웃 → 503 에러 ❌
```

**해결 과정 (단계별):**

**1단계: 문제 파악**
```bash
# 연결 풀 상태 확인
curl http://localhost:8083/actuator/metrics/hikari.connections.active
# 결과: 10/10 (모든 연결 사용 중)

# 로그 확인
docker logs dorandoran-chat | grep -i "connection"
# 결과: "Connection is not available" 메시지 다수 발견
```

**2단계: `open-in-view` 비활성화**
```yaml
# 이전 (application.yml)
spring:
  jpa:
    open-in-view: true  # 기본값

# 수정 후
spring:
  jpa:
    open-in-view: false
```

**이것이 왜 해결책인가요?**
- `open-in-view=false`로 설정하면:
  - HTTP 요청 전체가 아닌, **트랜잭션 범위 내에서만** 연결을 유지합니다
  - 트랜잭션이 끝나면 즉시 연결을 반환합니다
  - SSE 연결이 오래 유지되어도, 데이터베이스 작업이 끝나면 연결을 바로 반환합니다

**3단계: 연결 풀 크기 증가**
```yaml
# 이전
spring:
  datasource:
    hikari:
      maximum-pool-size: 10

# 수정 후
spring:
  datasource:
    hikari:
      maximum-pool-size: 20  # 10 → 20으로 증가
      minimum-idle: 5        # 최소 5개는 항상 유지
```

**4단계: 연결 누수 감지 설정**
```yaml
spring:
  datasource:
    hikari:
      leak-detection-threshold: 10000  # 10초 이상 점유 시 경고
```

**이것이 무엇을 하나요?**
- 연결이 10초 이상 점유되면 로그에 경고를 남깁니다
- 예: `Connection leak detection: connection was acquired 15 seconds ago`
- 이렇게 하면 문제를 조기에 발견할 수 있습니다

**결과:**
- ✅ 연결 누수 문제 해결
- ✅ 503 에러 사라짐
- ✅ 더 많은 사용자를 동시에 처리 가능

**배운 점:**
1. **Long-running 요청에서는 `open-in-view=false` 필수**: SSE, WebSocket 등 오래 유지되는 연결에서는 반드시 설정해야 합니다
2. **연결 생명주기 관리가 중요**: 연결을 빌렸으면 반드시 반환해야 합니다
3. **연결 풀 모니터링 설정 필수**: 누수 감지 설정으로 문제를 조기에 발견할 수 있습니다

#### 3.1.3 RDS → Local DB 마이그레이션

**배경:**
- AWS RDS Aurora 사용으로 인한 높은 비용 ($59.04/월)
- 비용 절감을 위한 로컬 PostgreSQL 컨테이너 전환

**마이그레이션 과정:**
1. RDS Aurora 데이터 덤프 (386KB)
2. 컨테이너 교체 및 PostgreSQL 업그레이드 (15 → 17)
3. 데이터 복원 (5개 스키마, 17개 테이블)
4. 배포 스크립트 수정

**결과:**
- 월 $58-79 절감 (50-60% 절감)
- 연간 절감: $696-948
- 리소스 사용량 증가 (PostgreSQL 메모리: ~500MB-1GB)

### 3.2 코드 레벨 문제

#### 3.2.1 Hibernate 프록시 순환 참조 문제

**문제 상황 (쉽게 설명):**
- 애플리케이션이 실행 중 갑자기 멈추고 에러가 발생했습니다
- "스택 오버플로우" 에러가 발생했습니다
- 무한 루프가 발생한 것입니다

**구체적인 증상:**
```
애플리케이션 시작 중...
[정상 작동]
[갑자기 멈춤]

에러 메시지:
java.lang.StackOverflowError
  at com.dorandoran.chat.domain.User.toString(User.java:45)
  at com.dorandoran.chat.domain.ChatRoom.toString(ChatRoom.java:52)
  at com.dorandoran.chat.domain.User.toString(User.java:45)
  at com.dorandoran.chat.domain.ChatRoom.toString(ChatRoom.java:52)
  ... (무한 반복)
```

**왜 이런 일이 발생했나요?**

**1. 양방향 관계 설정**
- `User` 엔티티와 `ChatRoom` 엔티티가 서로 참조하고 있었습니다:
  ```java
  // User 엔티티
  public class User {
      @OneToMany(mappedBy = "user")
      private List<ChatRoom> chatRooms;  // "내가 가진 채팅방들"
  }
  
  // ChatRoom 엔티티
  public class ChatRoom {
      @ManyToOne
      private User user;  // "이 채팅방의 주인"
  }
  ```
- 이것을 "양방향 관계"라고 합니다
- 예: "사용자는 여러 채팅방을 가지고 있고, 채팅방은 한 사용자를 가진다"

**2. Lombok `@Data`의 `toString()` 메서드**
- Lombok의 `@Data` 어노테이션은 자동으로 `toString()` 메서드를 생성합니다
- `toString()`은 객체의 모든 필드를 문자열로 변환합니다

**3. 무한 루프 발생 과정:**
```java
// User 객체를 문자열로 변환하려고 시도
User user = ...;
String str = user.toString();  // 시작

// Lombok이 생성한 toString() 메서드 (의사 코드)
public String toString() {
    return "User(" +
        "id=" + id + ", " +
        "email=" + email + ", " +
        "chatRooms=" + chatRooms.toString() +  // ← 여기서 문제!
        ")";
}

// chatRooms는 List<ChatRoom>이므로...
// List의 toString()이 각 ChatRoom의 toString()을 호출
chatRooms.toString()
  → ChatRoom.toString() 호출

// ChatRoom의 toString() (의사 코드)
public String toString() {
    return "ChatRoom(" +
        "id=" + id + ", " +
        "name=" + name + ", " +
        "user=" + user.toString() +  // ← 다시 User.toString() 호출!
        ")";
}

// 다시 User.toString() 호출 → chatRooms.toString() 호출 → ...
// 무한 루프! 🔄
```

**시각적으로 표현:**
```
User.toString() 시작
  ↓
chatRooms.toString() 호출
  ↓
ChatRoom.toString() 호출
  ↓
user.toString() 호출  ← 다시 User.toString()!
  ↓
chatRooms.toString() 호출
  ↓
... (무한 반복)
```

**해결 방법:**

**방법 1: `@ToString(exclude = {...})` 사용 (권장)**
```java
// User 엔티티
@ToString(exclude = {"chatRooms"})  // chatRooms를 toString()에서 제외
public class User {
    @OneToMany(mappedBy = "user")
    private List<ChatRoom> chatRooms;
}

// ChatRoom 엔티티
@ToString(exclude = {"user", "messages"})  // user와 messages를 제외
public class ChatRoom {
    @ManyToOne
    private User user;
    
    @OneToMany(mappedBy = "chatRoom")
    private List<Message> messages;
}
```

**이것이 왜 해결책인가요?**
- `toString()`에서 순환 참조를 일으키는 필드를 제외하면 됩니다
- User의 `toString()`에서 `chatRooms`를 제외하면, ChatRoom의 `toString()`을 호출하지 않습니다
- 따라서 무한 루프가 발생하지 않습니다

**방법 2: `@Data` 대신 필요한 어노테이션만 사용**
```java
// @Data 대신
@Getter
@Setter
@EqualsAndHashCode
// @ToString은 제외하거나 수동으로 작성
public class User {
    // ...
}
```

**결과:**
- ✅ 무한 루프 문제 해결
- ✅ 애플리케이션이 정상 작동
- ✅ `toString()` 메서드가 안전하게 작동

**배운 점:**
1. **양방향 관계에서 `toString()` 주의**: 순환 참조가 발생할 수 있습니다
2. **Lombok `@Data` 사용 시 주의**: 자동 생성되는 메서드가 문제를 일으킬 수 있습니다
3. **`@ToString(exclude = {...})` 활용**: 순환 참조를 방지할 수 있습니다

#### 3.2.2 GreetingService 프롬프트 개선

**문제 상황:**
- `greeting_guide_message` SSE 이벤트가 클라이언트에 전달되지 않음
- AI 응답 파싱 실패로 인한 `NullPointerException` 발생
- 프롬프트 JSON 형식 불일치

**원인:**
1. 프롬프트 JSON 형식 오류: 따옴표 없는 필드 값
2. 불필요한 필드 요구: `intimacyLevel`, `detectedLevel`
3. 예시 시나리오 불일치: 잘못된 JSON 형식 예시

**해결:**
1. JSON 형식 통일 (`{"botMessage": "...", "guideMessage": "..."}`)
2. 예시 시나리오 수정
3. null 체크 및 안전성 강화

### 3.3 보안 문제

#### 3.3.1 IP 블랙리스트 시스템 구현

**문제 상황:**
- 의심 IP (172.104.24.172)로부터 비정상 HTTP 요청 다수 발생
- RTSP/1.0, SIP/2.0 등 비정상 프로토콜 요청
- 보안 스캔/공격 시도 가능성

**해결:**
1. `IpBlacklistFilter` 구현 (Gateway 필터)
2. Redis 기반 저장
3. 관리자 API 구현 (`/api/admin/blacklist`)

**결과:**
- 차단된 IP는 HTTP 403으로 즉시 차단
- 보안 위협 감소

#### 3.3.2 SecurityConfig 정책 일관성 개선

**문제 상황:**
- `SecurityConfig`: 모든 `/api/**` 경로를 `permitAll()`로 설정
- `JwtAuthFilter`: 제외 목록에 없는 경로는 JWT 인증 필요
- 정책 불일치로 인한 혼란

**해결:**
```java
.pathMatchers("/api/admin/**").authenticated()  // 관리자 API는 인증 필요
.pathMatchers("/api/auth/**").permitAll()       // Auth API는 허용
.pathMatchers("/api/**").authenticated()         // 나머지는 인증 필요
```

### 3.4 성능 문제

#### 3.4.1 N+1 문제 (Store Service)

**문제 상황 (쉽게 설명):**
- 보관함 목록을 조회할 때, 각 항목마다 채팅방 이름을 조회하는 API를 호출했습니다
- 20개 항목이 있으면 20번의 API 호출이 발생했습니다
- 같은 채팅방의 이름을 여러 번 조회하는 비효율이 있었습니다

**구체적인 예시:**

**사용자 요청:**
```
GET /api/store/bookmarks
→ "내가 저장한 표현 목록을 보여줘"
```

**문제가 있는 코드:**
```java
// 1번의 쿼리: Store 리스트 조회
List<Store> stores = storeRepository.findByUserId(userId);  // 20개 조회

// 20번의 API 호출: 각 Store마다 채팅방 이름 조회
for (Store store : stores) {
    String chatroomName = getChatroomName(store.getChatroomId());  // Feign API 호출
    // Store 1 → API 호출 1
    // Store 2 → API 호출 2
    // ...
    // Store 20 → API 호출 20
}
// 총: 1번의 DB 쿼리 + 20번의 API 호출 = 21번의 호출
```

**실제 발생 시나리오:**
```
사용자가 20개의 표현을 저장했고, 이 중 15개가 같은 채팅방에서 온 경우:

Store 1 (chatroomId: A) → Chat Service: "채팅방 A 이름 조회" → "친구 채팅방"
Store 2 (chatroomId: B) → Chat Service: "채팅방 B 이름 조회" → "동료 채팅방"
Store 3 (chatroomId: A) → Chat Service: "채팅방 A 이름 조회" → "친구 채팅방" (중복!)
Store 4 (chatroomId: C) → Chat Service: "채팅방 C 이름 조회" → "상사 채팅방"
Store 5 (chatroomId: A) → Chat Service: "채팅방 A 이름 조회" → "친구 채팅방" (중복!)
...
Store 20 (chatroomId: O) → Chat Service: "채팅방 O 이름 조회" → "가족 채팅방"

총 API 호출: 20번
중복 호출: 채팅방 A를 3번 조회 (비효율!)
```

**왜 문제인가요?**
1. **네트워크 오버헤드**: 각 API 호출마다 네트워크 왕복 시간이 발생합니다 (예: 30ms)
2. **응답 시간 증가**: 20번 × 30ms = 600ms (순차 호출 시)
3. **서버 부하**: 불필요한 반복 호출로 인한 리소스 낭비
4. **확장성 저하**: 데이터가 많아질수록 성능 저하가 선형적으로 증가

**해결 방법: 배치 조회 패턴**

**개선된 코드:**
```java
// 1번의 쿼리: Store 리스트 조회
List<Store> stores = storeRepository.findByUserId(userId);  // 20개 조회

// 1단계: 모든 chatroomId를 수집 (중복 제거)
Set<UUID> uniqueChatroomIds = stores.stream()
    .map(Store::getChatroomId)
    .collect(Collectors.toSet());  // 15개의 고유 chatroomId

// 2단계: 각 고유 chatroomId에 대해 한 번만 조회
Map<UUID, String> chatroomNameMap = new HashMap<>();
for (UUID chatroomId : uniqueChatroomIds) {
    String name = getChatroomName(chatroomId);  // Feign API 호출
    chatroomNameMap.put(chatroomId, name);
    // chatroomId A → 1번만 호출
    // chatroomId B → 1번만 호출
    // ...
    // chatroomId O → 1번만 호출
}
// 총: 15번의 API 호출 (중복 제거!)

// 3단계: Map에서 조회하여 재사용
for (Store store : stores) {
    String chatroomName = chatroomNameMap.get(store.getChatroomId());
    // Store 1 (chatroomId: A) → Map에서 조회 → "친구 채팅방"
    // Store 2 (chatroomId: B) → Map에서 조회 → "동료 채팅방"
    // Store 3 (chatroomId: A) → Map에서 조회 → "친구 채팅방" (API 호출 없음!)
    // ...
}
```

**성능 개선 효과:**

| 항목 | 개선 전 | 개선 후 | 개선율 |
|------|---------|---------|--------|
| DB 쿼리 | 1번 | 1번 | - |
| API 호출 | 20번 | 15번 | **25% 감소** |
| 총 호출 | 21번 | 16번 | **23.8% 감소** |
| 응답 시간 | 600ms | 450ms | **25% 개선** |

**더 극단적인 경우:**
- 100개 Store, 10개 고유 chatroomId인 경우:
  - 개선 전: 100번의 API 호출
  - 개선 후: 10번의 API 호출
  - **90% 감소!**

**배운 점:**
1. **배치 조회 패턴을 사용하자**: 개별 조회 대신 한 번에 조회
2. **중복을 제거하자**: Set을 사용하여 고유한 값만 수집
3. **결과를 재사용하자**: Map으로 캐싱하여 반복 조회 방지

---

## 4. 개선한 점

> 이 섹션에서는 성능과 안정성을 개선하기 위해 적용한 최적화 방법들을 설명합니다.

### 4.1 성능 최적화

#### 4.1.1 Redis 캐싱 전략

**캐싱이란?**
- 자주 사용하는 데이터를 빠른 저장소(메모리)에 저장해두는 것입니다
- 예: 자주 읽는 책을 책상 위에 두는 것과 같습니다

**왜 캐싱이 필요한가요?**

**캐싱 없이 (문제):**
```
사용자1: "채팅방 정보 조회"
  → 데이터베이스 조회 (5ms) ✅

사용자2: "같은 채팅방 정보 조회"
  → 데이터베이스 조회 (5ms) ❌ (똑같은 데이터를 또 조회)

사용자3: "같은 채팅방 정보 조회"
  → 데이터베이스 조회 (5ms) ❌ (똑같은 데이터를 또 조회)

문제: 같은 데이터를 반복해서 조회 → 데이터베이스 부하 증가
```

**캐싱 적용 후 (개선):**
```
사용자1: "채팅방 정보 조회"
  → 데이터베이스 조회 (5ms) → Redis에 저장 ✅

사용자2: "같은 채팅방 정보 조회"
  → Redis에서 조회 (0.5ms) ✅ (매우 빠름!)

사용자3: "같은 채팅방 정보 조회"
  → Redis에서 조회 (0.5ms) ✅ (매우 빠름!)

개선: 같은 데이터는 Redis에서 조회 → 데이터베이스 부하 감소
```

**구현한 캐싱 전략:**

**1. ChatRoom 캐싱 (TTL: 30분)**
```java
@Cacheable(value = "chatrooms", key = "#chatroomId")
public ChatRoom getChatRoomById(UUID chatroomId) {
    return chatRoomRepository.findById(chatroomId);
}
```
- **용도**: 채팅방 정보를 조회할 때 사용
- **TTL (Time To Live)**: 30분 동안 캐시에 저장
- **효과**: 같은 채팅방을 여러 번 조회할 때 데이터베이스를 조회하지 않음

**2. IntimacyProgress 캐싱 (TTL: 15분)**
```java
@Cacheable(value = "intimacy", key = "#chatroomId")
public Integer getIntimacyLevel(UUID chatroomId) {
    return intimacyProgressRepository.findByChatroomId(chatroomId)
        .map(IntimacyProgress::getCurrentLevel)
        .orElse(0);
}
```
- **용도**: 친밀도 레벨을 조회할 때 사용
- **효과**: 멀티 에이전트 시스템에서 여러 번 조회할 때 성능 향상

**3. SystemPrompt 캐싱 (TTL: 10분)**
```java
@Cacheable(value = "prompts", key = "#chatroomId + ':' + #intimacyLevel")
public String buildSystemPrompt(UUID chatroomId, int intimacyLevel) {
    // 프롬프트 생성 로직 (15-20ms 소요)
    return prompt;
}
```
- **용도**: AI 프롬프트를 생성할 때 사용
- **효과**: 프롬프트 생성은 비용이 많이 드는 작업이므로, 캐싱으로 성능 향상

**4. 메시지 히스토리 캐싱 (TTL: 3분)**
```java
@Cacheable(value = "messageHistory", key = "#chatroomId")
private List<Map<String, String>> buildMessageHistory(UUID chatroomId) {
    // 최근 10개 메시지 조회
    return messages;
}
```
- **용도**: 대화 히스토리를 조회할 때 사용
- **TTL이 짧은 이유**: 메시지는 자주 추가되므로 최신성을 보장하기 위해

**5. User/Chatbot 캐싱 (TTL: 2시간)**
```java
@Cacheable(value = "users", key = "#userId")
public Optional<User> findById(UUID userId) {
    return userRepository.findById(userId);
}
```
- **용도**: 사용자 정보와 챗봇 정보를 조회할 때 사용
- **TTL이 긴 이유**: 사용자 정보는 자주 변경되지 않으므로 오래 캐싱 가능

**성과 측정:**

**Before (캐싱 적용 전):**
- 평균 메시지 처리 시간: 85-120ms
- 데이터베이스 동시 연결 수: 평균 15개
- 동시 사용자 처리 한계: 약 50명

**After (캐싱 적용 후):**
- 평균 메시지 처리 시간: 45-70ms (**40% 개선**)
- 데이터베이스 동시 연결 수: 평균 6개 (**60% 감소**)
- 동시 사용자 처리 능력: 약 120명 (**140% 증가**)

**캐시 히트율:**
- ChatRoom: 85-92% (100번 조회 중 85-92번은 캐시에서 조회)
- IntimacyProgress: 78-88%
- User/Chatbot: 95-98%
- SystemPrompt: 82-90%

**캐시 히트율이란?**
- 전체 요청 중 캐시에서 데이터를 찾은 비율
- 예: 100번 조회 중 85번이 캐시에서 조회되면 히트율 85%
- 높을수록 좋습니다 (데이터베이스 조회가 적어짐)

#### 4.1.2 Connection Pool 최적화

**Connection Pool이란?**
- 데이터베이스 연결을 미리 만들어서 "풀"에 보관해두는 것입니다
- 요청이 오면 풀에서 연결을 빌려서 사용하고, 사용이 끝나면 다시 풀에 반환합니다

**왜 최적화가 필요한가요?**

**문제 상황:**
- 모든 서비스가 같은 크기의 연결 풀을 사용하고 있었습니다
- 예: 모든 서비스가 10개씩 사용 → 총 50개 연결
- 하지만 각 서비스의 특성이 다릅니다:
  - Chat Service: 실시간 메시지 처리로 연결을 자주 사용
  - Batch Service: 하루 한 번만 실행되므로 연결을 거의 사용하지 않음

**최적화 전략:**

**1. 서비스별 역할 분석**

**Chat Service (20개 연결)**
- **역할**: 실시간 메시지 처리
- **특성**: 
  - 메시지 저장이 빈번함
  - SSE 연결로 인해 긴 연결 보유
  - 동시 사용자가 많음
- **이유**: 가장 많은 연결이 필요

**User Service (15개 연결)**
- **역할**: 사용자 정보 조회
- **특성**:
  - 조회가 빈번하지만 Redis 캐싱으로 실제 DB 부하는 낮음
  - 읽기 중심
- **이유**: 캐싱으로 부하가 감소했지만, 여전히 충분한 연결 필요

**Auth Service (10개 연결)**
- **역할**: 토큰 검증
- **특성**:
  - JWT 기반 인증으로 대부분의 검증은 토큰만 확인
  - DB는 RefreshToken 저장/조회만 수행
- **이유**: DB 사용이 적으므로 적은 연결로 충분

**Store Service (10개 연결)**
- **역할**: 보관함 조회
- **특성**:
  - 조회 중심
  - Redis 캐싱으로 실질적 DB 부하 낮음
- **이유**: 캐싱으로 부하가 감소

**Batch Service (5개 연결)**
- **역할**: 새벽 배치 작업
- **특성**:
  - 하루 한 번만 실행 (새벽 2시)
  - 평소에는 거의 사용하지 않음
- **이유**: 최소한의 연결만 유지

**최종 설정:**
```
Chat Service:    20개 연결 (실시간 처리)
User Service:    15개 연결 (사용자 정보)
Auth Service:    10개 연결 (토큰 검증)
Store Service:   10개 연결 (보관함)
Batch Service:   5개 연결 (배치 작업)
─────────────────────────────
총 연결:         60개
PostgreSQL 최대 연결: 100개
여유:            40개
```

**최적화 효과:**

**Before (모든 서비스 10개씩):**
- 총 연결: 50개
- 문제: 
  - Chat Service는 부족할 수 있음
  - Batch Service는 낭비

**After (서비스별 최적화):**
- 총 연결: 60개
- 개선:
  - Chat Service는 충분한 연결 확보
  - Batch Service는 최소한만 사용
  - 메모리 효율적 사용

**Redis 캐싱과의 시너지:**
- Redis 캐싱으로 실제 DB 조회가 감소
- 따라서 연결 풀 크기를 줄여도 충분히 처리 가능
- 예: User Service는 15개 연결로도 충분 (캐싱으로 부하 감소)

**실제 성능:**
- 동시 사용자 처리 능력: 100-150명 (캐싱 고려 시)
- 연결 풀 사용률: 평균 30-40% (여유 있음)
- 메모리 사용: 효율적 (필요한 만큼만 사용)

#### 4.1.3 N+1 문제 해결

**구현 내용:**
- 배치 조회 패턴 적용
- 고유 `chatroomId`만 수집하여 한 번만 조회
- 결과를 Map으로 캐싱하여 재사용

**성과:**
- Feign 호출 25-90% 감소
- 응답 시간 25-90% 개선
- 네트워크 트래픽 감소

### 4.2 아키텍처 개선

#### 4.2.1 멀티 에이전트 시스템

**구현 내용:**
- 병렬 + Aggregator 패턴
- IntimacyAgent, VocabularyAgent 병렬 실행
- ConversationAgent 독립 스트림
- SummarizerAgent 비동기 후처리

**성과:**
- 여러 에이전트 병렬 실행으로 응답 시간 단축
- Reactor Mono/Flux로 비동기 처리
- 에러 격리 (하나 실패해도 다른 에이전트 계속 실행)

#### 4.2.2 서비스 간 통신 개선

**구현 내용:**
- Feign Client로 동기 통신
- Circuit Breaker로 장애 격리
- HMAC 서명 기반 인증
- Retry 메커니즘 (지수 백오프)

**성과:**
- 서비스 간 통신 안정성 향상
- 장애 전파 방지
- 보안 강화

### 4.3 모니터링 개선

#### 4.3.1 Prometheus + Grafana

**구현 내용:**
- Micrometer로 메트릭 수집
- Grafana 대시보드 구성
- 서비스별 메트릭 시각화

**성과:**
- 실시간 시스템 상태 모니터링
- 성능 지표 추적
- 장애 조기 발견

#### 4.3.2 로깅 개선

**구현 내용:**
- 구조화된 JSON 로그
- Loki로 중앙 집중식 로그 수집
- 로그 레벨 최적화

**성과:**
- 디버깅 시간 단축
- 로그 분석 용이
- 문제 추적 효율성 향상

### 4.4 보안 개선

#### 4.4.1 IP 블랙리스트 시스템

**구현 내용:**
- Gateway 필터로 IP 차단
- Redis 기반 저장
- 관리자 API 제공

**성과:**
- 보안 위협 자동 차단
- 관리 편의성 향상

#### 4.4.2 HMAC 인증

**구현 내용:**
- Gateway에서 HMAC 서명 생성
- 하위 서비스에서 검증
- 타임스탬프 기반 리플레이 공격 방지

**성과:**
- 서비스 간 통신 보안 강화
- 위조된 요청 방지

### 4.5 비용 최적화

#### 4.5.1 RDS → Local DB 마이그레이션

**구현 내용:**
- RDS Aurora 제거
- 로컬 PostgreSQL 컨테이너 전환
- 데이터 마이그레이션

**성과:**
- 월 $58-79 절감 (50-60% 절감)
- 연간 절감: $696-948
- 리소스 사용량 증가 (PostgreSQL 메모리: ~500MB-1GB)

---

## 5. 트러블슈팅

### 5.1 인프라 문제

#### 5.1.1 RDS OOM 문제

**증상:**
- API 응답 500 오류
- 컨테이너 Health `unhealthy`
- `OutOfMemoryError: unable to create native thread`

**해결:**
1. JVM 메모리 옵션 조정
2. Tomcat 스레드 제한
3. Hikari 커넥션 풀 크기 조정

**예방:**
- 리소스 한도 명시적 설정
- 모니터링으로 조기 발견

#### 5.1.2 Connection Pool Exhaustion

**증상:**
- 503 Service Unavailable
- 모든 API 요청 타임아웃
- 연결 풀 고갈

**해결:**
1. `open-in-view=false` 설정
2. 연결 풀 크기 증가
3. 누수 감지 설정

**예방:**
- Long-running 요청에서 `open-in-view=false` 필수
- 연결 풀 모니터링

### 5.2 코드 레벨 문제

#### 5.2.1 Hibernate 프록시 순환 참조

**증상:**
- `NullPointerException` 및 스택 오버플로우
- `User$HibernateProxy$Dyp9SYgE.toString` 무한 반복

**해결:**
- `@ToString(exclude = {...})` 사용
- 양방향 관계에서 순환 참조 방지

**예방:**
- 양방향 관계에서 `toString()` 주의
- Lombok 사용 시 순환 참조 고려

#### 5.2.2 AI 프롬프트 파싱 오류

**증상:**
- SSE 이벤트 전송 실패
- `NullPointerException` 발생
- JSON 파싱 실패

**해결:**
1. JSON 형식 통일
2. 예시 시나리오 수정
3. null 체크 추가

**예방:**
- 프롬프트 형식 검증
- 예시 시나리오 정확성 확인

### 5.3 보안 문제

#### 5.3.1 IP 블랙리스트 관리

**증상:**
- 비정상 HTTP 요청 다수 발생
- 프로토콜 혼동 공격
- 보안 스캔 시도

**해결:**
1. `IpBlacklistFilter` 구현
2. Redis 기반 저장
3. 관리자 API 제공

**예방:**
- 로그 모니터링
- 자동 차단 시스템

#### 5.3.2 OAuth 로그인 401 에러

**증상:**
- `POST /api/auth/oauth/login` 401 에러
- CORS/COOP 오류
- Google OAuth 로그인 실패

**해결:**
1. Gateway `JwtAuthFilter` 제외 경로 추가
2. Auth 서비스 `HmacAuthInterceptor` 제외 경로 추가
3. CORS 설정 개선
4. COOP 헤더 설정

**예방:**
- OAuth 엔드포인트는 인증 제외
- CORS 설정 정확성 확인

### 5.4 성능 문제

#### 5.4.1 N+1 문제

**증상:**
- 보관함 목록 조회 시 다수 Feign 호출
- 응답 시간 증가
- 서버 부하 증가

**해결:**
1. 배치 조회 패턴 적용
2. 고유 `chatroomId`만 수집
3. 결과 Map으로 캐싱

**예방:**
- 배치 조회 패턴 적용
- 캐싱 전략 수립

### 5.5 일반적인 디버깅 방법

#### 5.5.1 로그 확인
```bash
# 실시간 로그 모니터링
docker logs -f dorandoran-chat

# 특정 시간대 로그 확인
docker logs --since 1h dorandoran-chat

# 오류 로그만 필터링
docker logs dorandoran-chat 2>&1 | grep -i error
```

#### 5.5.2 네트워크 연결 테스트
```bash
# 포트 연결 확인
telnet localhost 8083

# 외부에서 접근 테스트
curl http://localhost:8083/actuator/health
```

#### 5.5.3 리소스 사용량 확인
```bash
# 컨테이너 리소스 사용량
docker stats dorandoran-chat

# 서버 리소스 사용량
top
df -h
free -h
```

#### 5.5.4 데이터베이스 연결 테스트
```bash
# PostgreSQL 클라이언트로 직접 연결 테스트
psql -h localhost -U doran -d dorandoran
```

---

## 결론

### 프로젝트를 통해 배운 것들

DoranDoran 프로젝트를 통해 다음과 같은 경험을 쌓았습니다:

#### 1. 마이크로서비스 아키텍처 설계 및 구현
- **서비스 분리**: 큰 애플리케이션을 작은 서비스로 나누는 방법
- **독립적 배포**: 각 서비스를 독립적으로 배포하고 업데이트하는 방법
- **서비스 간 통신**: Feign Client, SSE 등을 사용한 서비스 간 통신 방법

**배운 점:**
- 서비스를 어떻게 나눌지 설계하는 것이 중요합니다
- 서비스 간 통신은 성능과 안정성에 큰 영향을 미칩니다

#### 2. 성능 최적화
- **Redis 캐싱**: 자주 사용하는 데이터를 메모리에 저장하여 성능 향상
- **Connection Pool 최적화**: 서비스별 특성에 맞게 연결 풀 크기 조정
- **N+1 문제 해결**: 배치 조회 패턴을 사용하여 불필요한 호출 제거

**배운 점:**
- 캐싱은 성능 향상에 매우 효과적입니다 (40% 개선)
- 문제를 정확히 파악하면 해결 방법을 찾을 수 있습니다

#### 3. 장애 처리
- **Circuit Breaker**: 문제가 있는 서비스 호출을 자동으로 차단
- **Retry**: 일시적인 오류일 때 자동으로 재시도
- **모니터링**: Prometheus, Grafana를 사용한 시스템 상태 모니터링

**배운 점:**
- 장애는 피할 수 없으므로, 장애가 발생해도 전체 시스템이 멈추지 않도록 해야 합니다
- 모니터링을 통해 문제를 조기에 발견할 수 있습니다

#### 4. 보안 구현
- **JWT 인증**: 사용자 인증을 위한 토큰 기반 인증
- **HMAC 서명**: 서비스 간 통신 보안 강화
- **IP 블랙리스트**: 공격 IP 자동 차단

**배운 점:**
- 보안은 여러 계층으로 구성해야 합니다
- 공격을 사전에 차단하는 것이 중요합니다

#### 5. 인프라 운영
- **Docker 컨테이너화**: 서비스를 컨테이너로 배포하여 환경 일관성 유지
- **모니터링**: 시스템 상태를 실시간으로 확인
- **비용 최적화**: RDS 제거로 월 $58-79 절감

**배운 점:**
- 인프라 비용도 중요한 고려 사항입니다
- 모니터링을 통해 비용을 최적화할 수 있습니다

### 신입 개발자에게 전하는 메시지

이 프로젝트를 진행하면서 많은 문제를 겪었고, 그 과정에서 많은 것을 배웠습니다:

1. **문제를 두려워하지 마세요**: 문제가 발생하면 정확히 파악하고 단계별로 해결하면 됩니다
2. **기본을 이해하세요**: 기술을 사용하기 전에 그 기술이 왜 필요한지, 어떻게 작동하는지 이해하는 것이 중요합니다
3. **모니터링을 습관화하세요**: 문제가 발생하기 전에 미리 감지할 수 있도록 모니터링을 설정하세요
4. **문서화를 게을리하지 마세요**: 문제를 해결한 과정을 기록해두면 나중에 도움이 됩니다

### 앞으로의 계획

이러한 경험을 바탕으로:
- 더 안정적이고 성능이 우수한 시스템을 구축하겠습니다
- 지속적인 개선을 통해 더욱 견고한 시스템을 만들어 나가겠습니다
- 배운 지식을 공유하여 팀 전체의 성장에 기여하겠습니다

---

**작성일**: 2025-12-07  
**작성자**: AI Assistant  
**프로젝트**: DoranDoran  
**대상 독자**: 신입 개발자 및 프로젝트 이해를 원하는 모든 분들












