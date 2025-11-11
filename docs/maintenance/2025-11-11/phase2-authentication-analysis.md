# Phase 2 인증 구조 분석 보고서

**작성일**: 2025년 11월 11일  
**대상**: Gateway IP 블랙리스트 필터 및 관리 API 구현  
**목적**: 인증 구조 충돌 가능성 상세 분석

---

## 1. 현재 인증 구조 개요

### 1.1 필터 체인 구조

Spring Cloud Gateway는 두 가지 필터 체인을 사용합니다:

1. **WebFilterChain** (Spring WebFlux)
   - SecurityWebFilterChain (Spring Security)
   - WebFilter 구현체들 (JwtAuthFilter, CorsWebFilter)

2. **GatewayFilterChain** (Spring Cloud Gateway)
   - GlobalFilter 구현체들 (IpBlacklistFilter, CorsResponseFilter)
   - Route별 Filter들

### 1.2 필터 실행 순서

```
요청 흐름:
1. WebFilterChain (Spring WebFlux 레벨)
   ├─ CorsWebFilter (SecurityConfig에서 Bean 등록)
   └─ JwtAuthFilter (WebFilter, Order 없음 - 기본 순서)
   
2. SecurityWebFilterChain (Spring Security)
   └─ authorizeExchange() - permitAll() 설정
   
3. GatewayFilterChain (Spring Cloud Gateway)
   ├─ IpBlacklistFilter (GlobalFilter, Order: -100) ← 가장 먼저
   ├─ CorsResponseFilter (GlobalFilter, Order: -1)
   └─ Route Filters (Retry, RewritePath 등)
```

---

## 2. 필터별 상세 분석

### 2.1 IpBlacklistFilter (신규 추가)

**타입**: `GlobalFilter`, `Ordered`  
**Order**: `-100` (가장 높은 우선순위)  
**실행 위치**: GatewayFilterChain

**기능**:
- IP 블랙리스트 체크
- 차단된 IP는 HTTP 403 반환
- 제외 경로: `/actuator`, `/api/admin/blacklist`

**충돌 가능성**: 낮음
- GatewayFilterChain에서 가장 먼저 실행
- 다른 필터보다 우선하여 차단

### 2.2 JwtAuthFilter (기존)

**타입**: `WebFilter`  
**Order**: 없음 (기본 순서)  
**실행 위치**: WebFilterChain

**기능**:
- JWT 토큰 검증
- Auth 서비스와 통신하여 토큰 검증
- HMAC 헤더 주입
- 제외 경로: `/actuator`, `/api/auth/login`, `/api/users` 등

**충돌 가능성**: 중간
- SecurityConfig의 `permitAll()`과 중복 체크
- `/api/admin/blacklist` 경로가 제외 목록에 없음 → 인증 필요

### 2.3 SecurityConfig (기존)

**타입**: `SecurityWebFilterChain`  
**실행 위치**: WebFilterChain

**설정**:
```java
.pathMatchers("/api/**").permitAll()  // 모든 /api/** 경로 허용
```

**충돌 가능성**: 높음 ⚠️
- **문제**: SecurityConfig는 모든 `/api/**` 경로를 `permitAll()`로 설정
- **영향**: JwtAuthFilter가 인증을 체크하지만, SecurityConfig는 이미 허용
- **결과**: JwtAuthFilter는 작동하지만, SecurityConfig와 정책이 불일치

### 2.4 CorsWebFilter (기존)

**타입**: `CorsWebFilter` (Bean)  
**실행 위치**: WebFilterChain

**기능**:
- CORS 헤더 설정
- 모든 경로에 적용

**충돌 가능성**: 낮음
- CorsResponseFilter와 중복 가능하지만, CorsResponseFilter가 중복 체크함

### 2.5 CorsResponseFilter (기존)

**타입**: `GlobalFilter`, `Ordered`  
**Order**: `-1`  
**실행 위치**: GatewayFilterChain

**기능**:
- SSE 요청에만 CORS 헤더 추가
- CorsWebFilter와 중복 방지 로직 포함

**충돌 가능성**: 낮음
- 중복 체크 로직으로 인해 안전

---

## 3. 발견된 충돌 및 문제점

### 3.1 심각도: 높음 🔴

#### 문제 1: SecurityConfig와 JwtAuthFilter의 정책 불일치

**현재 상황**:
- `SecurityConfig`: `/api/**` 경로를 `permitAll()`로 설정
- `JwtAuthFilter`: 제외 목록에 없는 경로는 JWT 인증 필요

**영향**:
- SecurityConfig는 인증을 요구하지 않음
- JwtAuthFilter는 인증을 요구함
- **결과**: JwtAuthFilter가 실제 인증을 담당하지만, SecurityConfig는 허용

**관리자 API (`/api/admin/blacklist`) 영향**:
- SecurityConfig: `permitAll()` → 허용
- JwtAuthFilter: 제외 목록에 없음 → 인증 필요
- **실제 동작**: JWT 토큰이 없으면 401 반환 (JwtAuthFilter가 차단)
- **문제**: SecurityConfig의 `permitAll()` 설정이 무의미함

**권장 조치**:
```java
// SecurityConfig 수정 필요
.pathMatchers("/api/admin/**").authenticated()  // 관리자 API는 인증 필요
.pathMatchers("/api/auth/**").permitAll()      // Auth API는 허용
.pathMatchers("/api/users").permitAll()        // 회원가입 허용
.pathMatchers("/api/**").authenticated()        // 나머지는 인증 필요
```

### 3.2 심각도: 중간 🟡

#### 문제 2: 관리자 API 인증 경로 불일치

**현재 상황**:
- `IpBlacklistFilter`: `/api/admin/blacklist` 제외 (블랙리스트 체크 안 함)
- `JwtAuthFilter`: `/api/admin/blacklist` 제외 목록에 없음 (인증 필요)

**영향**:
- 관리자가 자신의 IP가 차단되어 있어도 블랙리스트 관리 가능 ✅
- 하지만 JWT 토큰은 여전히 필요 ✅
- **문제 없음**: 의도한 대로 동작

#### 문제 3: CORS 필터 중복

**현재 상황**:
- `CorsWebFilter`: 모든 요청에 CORS 헤더 추가
- `CorsResponseFilter`: SSE 요청에만 CORS 헤더 추가 (중복 체크)

**영향**:
- 중복 체크 로직으로 인해 안전
- **문제 없음**: 의도한 대로 동작

### 3.3 심각도: 낮음 🟢

#### 문제 4: 필터 실행 순서 명확성

**현재 상황**:
- `IpBlacklistFilter`: Order -100 (가장 먼저)
- `CorsResponseFilter`: Order -1
- `JwtAuthFilter`: Order 없음 (WebFilterChain에서 실행)

**영향**:
- GlobalFilter와 WebFilter는 다른 체인에서 실행되므로 직접적인 순서 충돌 없음
- **문제 없음**: 의도한 대로 동작

---

## 4. 필터 실행 흐름 분석

### 4.1 정상 요청 흐름 (인증된 사용자)

```
1. 요청 도착
   ↓
2. WebFilterChain 실행
   ├─ CorsWebFilter: CORS 헤더 추가
   └─ JwtAuthFilter: JWT 검증 → 통과 → HMAC 헤더 주입
   ↓
3. SecurityWebFilterChain 실행
   └─ authorizeExchange: permitAll() → 통과
   ↓
4. GatewayFilterChain 실행
   ├─ IpBlacklistFilter (Order: -100): IP 체크 → 통과
   ├─ CorsResponseFilter (Order: -1): SSE면 CORS 헤더 추가
   └─ Route Filters: 라우팅 처리
   ↓
5. 백엔드 서비스로 요청 전달
```

### 4.2 차단된 IP 요청 흐름

```
1. 요청 도착 (IP: 172.104.24.172)
   ↓
2. WebFilterChain 실행
   ├─ CorsWebFilter: CORS 헤더 추가
   └─ JwtAuthFilter: (실행되지 않음 - GatewayFilterChain에서 먼저 차단)
   ↓
3. GatewayFilterChain 실행
   └─ IpBlacklistFilter (Order: -100): IP 체크 → 차단 → HTTP 403 반환
   ↓
4. 응답 반환 (다른 필터 실행 안 됨)
```

### 4.3 관리자 API 요청 흐름 (인증된 관리자)

```
1. 요청 도착 (GET /api/admin/blacklist, JWT 토큰 포함)
   ↓
2. WebFilterChain 실행
   ├─ CorsWebFilter: CORS 헤더 추가
   └─ JwtAuthFilter: JWT 검증 → 통과 → HMAC 헤더 주입
   ↓
3. SecurityWebFilterChain 실행
   └─ authorizeExchange: permitAll() → 통과
   ↓
4. GatewayFilterChain 실행
   ├─ IpBlacklistFilter: /api/admin/blacklist 제외 → 통과
   └─ Route Filters: BlacklistController로 라우팅
   ↓
5. BlacklistController 실행 → 응답 반환
```

### 4.4 관리자 API 요청 흐름 (인증 없는 요청)

```
1. 요청 도착 (GET /api/admin/blacklist, JWT 토큰 없음)
   ↓
2. WebFilterChain 실행
   ├─ CorsWebFilter: CORS 헤더 추가
   └─ JwtAuthFilter: JWT 없음 → HTTP 401 반환
   ↓
3. 응답 반환 (다른 필터 실행 안 됨)
```

---

## 5. 충돌 가능성 종합 평가

### 5.1 필터 간 충돌

| 필터 조합 | 충돌 가능성 | 설명 |
|-----------|------------|------|
| IpBlacklistFilter ↔ JwtAuthFilter | 낮음 | 다른 체인에서 실행, 순서 문제 없음 |
| IpBlacklistFilter ↔ CorsResponseFilter | 없음 | 순서가 명확함 (-100 vs -1) |
| JwtAuthFilter ↔ SecurityConfig | 높음 ⚠️ | 정책 불일치 (permitAll vs 인증 필요) |
| CorsWebFilter ↔ CorsResponseFilter | 낮음 | 중복 체크 로직으로 안전 |

### 5.2 경로 매칭 충돌

| 경로 | SecurityConfig | JwtAuthFilter | IpBlacklistFilter | 결과 |
|------|---------------|---------------|-------------------|------|
| `/actuator/**` | permitAll | 제외 | 제외 | ✅ 허용 |
| `/api/auth/login` | permitAll | 제외 | 체크 | ✅ 허용 |
| `/api/users` | permitAll | 제외 | 체크 | ✅ 허용 |
| `/api/admin/blacklist` | permitAll ⚠️ | 인증 필요 | 제외 | ⚠️ 정책 불일치 |
| `/api/chat/**` | permitAll | 인증 필요 | 체크 | ⚠️ 정책 불일치 |

### 5.3 인증/인가 충돌

**문제점**:
1. SecurityConfig는 모든 `/api/**`를 `permitAll()`로 설정
2. JwtAuthFilter는 제외 목록에 없는 경로를 인증 필요로 처리
3. **결과**: SecurityConfig의 설정이 무의미함 (JwtAuthFilter가 실제 인증 담당)

**영향**:
- 현재는 JwtAuthFilter가 인증을 담당하므로 기능적으로는 문제 없음
- 하지만 SecurityConfig의 설정이 혼란스러울 수 있음
- 향후 Spring Security 기능 추가 시 충돌 가능성

---

## 6. 권장 수정 사항

### 6.1 즉시 수정 필요 🔴

#### SecurityConfig 정책 일관성 개선

**현재 코드**:
```java
.pathMatchers("/api/**").permitAll()  // 모든 API 허용
```

**권장 수정**:
```java
.pathMatchers("/actuator/**").permitAll()
.pathMatchers("/").permitAll()
.pathMatchers("/api/auth/login").permitAll()
.pathMatchers("/api/auth/refresh").permitAll()
.pathMatchers("/api/auth/password/reset").permitAll()
.pathMatchers("/api/auth/health").permitAll()
.pathMatchers("/api/auth/email/**").permitAll()
.pathMatchers("/api/users").permitAll()  // POST 회원가입
.pathMatchers("/api/users/register").permitAll()
.pathMatchers("/api/users/health").permitAll()
.pathMatchers("/api/users/email/**").permitAll()
.pathMatchers("/api/batch/**").permitAll()
.pathMatchers("/api/admin/**").authenticated()  // 관리자 API는 인증 필요
.pathMatchers("/api/**").authenticated()  // 나머지 API는 인증 필요
```

**이유**:
- JwtAuthFilter의 정책과 일치
- SecurityConfig의 의도 명확화
- 향후 Spring Security 기능 확장 시 일관성 유지

### 6.2 단기 개선 사항 🟡

#### JwtAuthFilter 제외 목록과 SecurityConfig 동기화

**현재 문제**:
- JwtAuthFilter의 `isExcludedPath()`와 SecurityConfig의 `permitAll()` 경로가 불일치

**권장 조치**:
- 공통 설정 파일로 관리
- 또는 SecurityConfig를 기준으로 JwtAuthFilter 제외 목록 업데이트

### 6.3 장기 개선 사항 🟢

#### 관리자 권한 체크 추가

**현재 상황**:
- JWT 토큰만 있으면 관리자 API 접근 가능
- 관리자 권한 체크 없음

**권장 조치**:
- JWT 토큰에서 역할(role) 클레임 확인
- `ROLE_ADMIN` 권한이 있는 사용자만 접근 허용

---

## 7. 테스트 시나리오

### 7.1 테스트 케이스

#### 케이스 1: 차단된 IP의 일반 요청
- **요청**: `GET /api/chat/chatrooms/all` (IP: 172.104.24.172)
- **예상 결과**: HTTP 403 (IpBlacklistFilter에서 차단)
- **충돌 가능성**: 없음

#### 케이스 2: 차단된 IP의 관리자 API 요청
- **요청**: `GET /api/admin/blacklist` (IP: 172.104.24.172, JWT 토큰 포함)
- **예상 결과**: HTTP 200 (IpBlacklistFilter에서 제외)
- **충돌 가능성**: 없음 (의도한 동작)

#### 케이스 3: 인증 없는 관리자 API 요청
- **요청**: `GET /api/admin/blacklist` (JWT 토큰 없음)
- **예상 결과**: HTTP 401 (JwtAuthFilter에서 차단)
- **충돌 가능성**: 없음

#### 케이스 4: 정상 사용자의 일반 요청
- **요청**: `GET /api/chat/chatrooms/all` (JWT 토큰 포함)
- **예상 결과**: HTTP 200 (정상 처리)
- **충돌 가능성**: 없음

#### 케이스 5: 인증 없는 공개 API 요청
- **요청**: `POST /api/auth/login`
- **예상 결과**: HTTP 200 (JwtAuthFilter에서 제외)
- **충돌 가능성**: 없음

---

## 8. 결론

### 8.1 전체 평가

**현재 상태**: 기능적으로는 정상 작동하지만, 정책 일관성 문제가 있음

**주요 발견 사항**:
1. ✅ **필터 실행 순서**: 문제 없음
2. ⚠️ **SecurityConfig 정책**: JwtAuthFilter와 불일치
3. ✅ **IP 블랙리스트 필터**: 정상 작동
4. ✅ **관리자 API**: 의도한 대로 동작

### 8.2 충돌 가능성 요약

| 항목 | 충돌 가능성 | 심각도 | 조치 필요 |
|------|------------|--------|----------|
| 필터 실행 순서 | 없음 | - | - |
| SecurityConfig vs JwtAuthFilter | 있음 | 높음 | 즉시 수정 권장 |
| IP 블랙리스트 vs 인증 | 없음 | - | - |
| CORS 필터 중복 | 없음 | - | - |
| 관리자 API 인증 | 없음 | - | - |

### 8.3 권장 조치

1. **즉시 조치**: SecurityConfig 정책 수정 (6.1 참조)
2. **단기 조치**: JwtAuthFilter 제외 목록과 SecurityConfig 동기화
3. **장기 조치**: 관리자 권한 체크 추가

---

## 9. 참고 자료

- [Spring Cloud Gateway Filter Order](https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/#gateway-combined-global-filter-and-gatewayfilter-ordering)
- [Spring Security WebFlux](https://docs.spring.io/spring-security/reference/reactive/configuration/webflux.html)
- [IP 172.104.24.172 보안 분석 보고서](./ip-172.104.24.172-security-analysis.md)

---

**작성자**: AI Assistant  
**작성일**: 2025년 11월 11일  
**검토 필요**: SecurityConfig 정책 수정

