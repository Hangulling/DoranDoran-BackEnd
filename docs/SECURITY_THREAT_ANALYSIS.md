# DoranDoran 프로젝트 보안 위협 검증 보고서

> 작성일: 2025-01-XX  
> 검증 범위: 비밀번호 유출, XSS, HTTP 혼동 공격, 기타 보안 위협

---

## 목차

1. [비밀번호 보안 위협](#1-비밀번호-보안-위협)
2. [XSS (Cross-Site Scripting) 공격](#2-xss-cross-site-scripting-공격)
3. [HTTP 혼동 공격 (Protocol Confusion)](#3-http-혼동-공격-protocol-confusion)
4. [CSRF (Cross-Site Request Forgery)](#4-csrf-cross-site-request-forgery)
5. [SQL Injection](#5-sql-injection)
6. [Rate Limiting 및 Brute Force 공격](#6-rate-limiting-및-brute-force-공격)
7. [에러 메시지 정보 노출](#7-에러-메시지-정보-노출)
8. [CORS 설정](#8-cors-설정)
9. [HTTPS 강제](#9-https-강제)
10. [기타 보안 위협](#10-기타-보안-위협)
11. [종합 평가 및 개선 권장사항](#11-종합-평가-및-개선-권장사항)

---

## 1. 비밀번호 보안 위협

### 1.1 현재 구현 상태

**✅ 양호한 점:**
- BCrypt 해싱 사용 (`BCryptPasswordEncoder`)
- 비밀번호는 해시로만 저장됨
- JPA를 통한 안전한 데이터베이스 저장

**❌ 심각한 취약점:**

#### 1.1.1 비밀번호 평문 로그 노출 (심각)

```66:68:auth/src/main/java/com/dorandoran/auth/service/AuthService.java
log.info("비밀번호 검증: 입력된 비밀번호={}, 저장된 해시={}", request.getPassword(), user.passwordHash());
boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.passwordHash());
log.info("비밀번호 일치 여부: {}", passwordMatches);
```

**위험도:** 🔴 **심각 (Critical)**

**문제점:**
- 평문 비밀번호가 로그에 기록됨
- 로그 파일이 유출되면 모든 사용자 비밀번호 노출
- GDPR, 개인정보보호법 위반 가능

**개선 방안:**
```java
// 비밀번호는 절대 로그에 기록하지 않음
log.info("비밀번호 검증 시도: email={}", request.getEmail());
boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.passwordHash());
log.info("비밀번호 검증 결과: email={}, success={}", request.getEmail(), passwordMatches);
```

### 1.2 비밀번호 정책

**현재 상태:**
- 최소 8자, 영문/숫자 포함 검증 존재
- 복잡도 요구사항은 기본 수준

**개선 권장사항:**
- 최소 12자 이상 권장
- 특수문자 포함 요구사항 추가
- 일반적인 비밀번호 목록(Common Password List) 검증

---

## 2. XSS (Cross-Site Scripting) 공격

### 2.1 입력 검증

**✅ 양호한 점:**
- `@Size`, `@Pattern`, `@NotBlank` 등 입력 검증 어노테이션 사용
- 메시지 길이 제한 (최대 10,000자)

```15:17:chat/src/main/java/com/dorandoran/chat/service/dto/MessageSendRequest.java
@NotBlank(message = "메시지 내용은 필수입니다")
@Size(min = 1, max = 10000, message = "메시지 내용은 1-10000자 사이여야 합니다")
private String content;
```

### 2.2 출력 인코딩

**❌ 취약점:**

**문제점:**
- 프론트엔드에서 사용자 입력을 그대로 렌더링할 경우 XSS 위험
- 백엔드에서 HTML 이스케이프 처리 확인 필요
- JSON 응답의 경우 클라이언트 측에서 적절히 처리해야 함

**검증 필요:**
- React에서 `dangerouslySetInnerHTML` 사용 여부 확인
- 사용자 입력이 HTML로 렌더링되는 경로 확인
- SSE 스트림에서 전송되는 데이터의 이스케이프 처리

**개선 권장사항:**
1. 백엔드에서 HTML 특수문자 이스케이프 처리
2. 프론트엔드에서 React의 기본 이스케이프 활용
3. Content Security Policy (CSP) 헤더 추가

---

## 3. HTTP 혼동 공격 (Protocol Confusion)

### 3.1 현재 대응 상태

**✅ 양호한 점:**
- IP 블랙리스트 필터 구현
- HTTP/0.9, HTTP/2.0, RTSP/1.0, SIP/2.0 등 프로토콜 혼동 공격 IP 차단

```158:167:gateway/src/main/resources/application.yml
security:
    blacklist:
      # 차단할 IP 주소 목록 (쉼표로 구분)
      # 2025-11-11: 172.104.24.172 (Protocol confusion attack)
      # 2025-11-12: 142.93.3.113 (Decoding failed attacks)
      # 2025-11-13: 61.138.228.132 (13건, 가장 많은 공격), 206.168.34.122 (HTTP/2.0 프로토콜 혼동), 198.235.24.209 (Command injection 시도), 3.143.33.63, 45.82.78.105, 216.180.246.183, 138.68.168.181
      # 2025-11-14: 193.26.115.195 (Command injection 시도, 3건 - 심각)
      # 2025-11-15: 3.132.23.201 (3건, HTTP/0.9 프로토콜 혼동), 167.94.138.174 (3건, HTTP/2.0 프로토콜 혼동), 134.122.4.76 (2건, 제어 문자 사용), 20.168.7.10, 195.184.76.243, 148.113.214.206, 148.113.214.202, 148.113.211.131, 135.237.126.210
      # 2025-11-16: 134.209.1.122 (8건, 가장 많은 공격), 3.130.96.91 (3건, 제어 문자 0x7 사용), 167.94.138.203 (3건), 205.210.31.139 (2건), 64.62.156.108, 139.99.35.43
      # 2025-11-17: 66.132.153.131 (3건, HTTP/0.9 및 HTTP/2.0 프로토콜 혼동), 64.62.156.192 (1건, HTTP/0.9 프로토콜 혼동), 3.137.73.221 (1건, Decoding failed), 23.162.40.89 (1건, Decoding failed)
      # 2025-11-19: 45.79.149.214 (10건, 가장 많은 공격 - RTSP/1.0, SIP/2.0, HTTP/0.9, 제어 문자 주입), 20.84.68.210 (1건, HTTP/0.9 프로토콜 혼동), 27.0.238.69 (1건, 프로토콜 혼동), 27.0.238.187 (1건, 프로토콜 혼동), 135.233.112.115 (1건, HTTP/0.9 프로토콜 혼동)
```

**❌ 개선 필요:**

1. **수동 블랙리스트 관리의 한계**
   - 공격 IP를 수동으로 추가하는 방식
   - 자동 탐지 및 차단 시스템 부재

2. **프로토콜 검증 부재**
   - HTTP 요청의 프로토콜 버전 검증 로직 없음
   - 비정상적인 프로토콜 요청을 사전에 차단하지 못함

**개선 권장사항:**
1. Spring Cloud Gateway에서 프로토콜 버전 검증 필터 추가
2. 비정상적인 HTTP 메서드/헤더 차단
3. 자동 IP 블랙리스트 시스템 (실패 횟수 기반)
4. WAF (Web Application Firewall) 도입 검토

---

## 4. CSRF (Cross-Site Request Forgery)

### 4.1 현재 상태

**현재 구현:**
- 모든 서비스에서 CSRF 보호 비활성화

```33:33:auth/src/main/java/com/dorandoran/auth/config/SecurityConfig.java
.csrf(csrf -> csrf.disable())
```

**평가:**
- ✅ JWT 기반 인증 사용으로 CSRF 위험 낮음
- ✅ SameSite 쿠키 미사용 (토큰 기반)
- ⚠️ 하지만 CSRF 보호를 완전히 비활성화하는 것은 권장되지 않음

**개선 권장사항:**
- JWT 사용 시에도 CSRF 토큰 검증 추가 고려
- 또는 SameSite 쿠키 정책 적용 (쿠키 사용 시)

---

## 5. SQL Injection

### 5.1 현재 상태

**✅ 양호한 점:**
- JPA/Hibernate 사용으로 Prepared Statement 자동 적용
- Native Query 사용 없음 (검색 결과 없음)
- 파라미터 바인딩을 통한 안전한 쿼리 실행

**검증 결과:**
- SQL Injection 위험도: 🟢 **낮음**

**주의사항:**
- 향후 Native Query 사용 시 반드시 파라미터 바인딩 사용
- 동적 쿼리 생성 시 주의 필요

---

## 6. Rate Limiting 및 Brute Force 공격

### 6.1 현재 상태

**설정 존재:**
```63:67:src/main/resources/application.properties
security.rate-limit.enabled=true
security.rate-limit.login-attempts=5
security.rate-limit.login-window=60  # ? ??
security.rate-limit.message-qps=20
security.account-lock-duration=30    # ? ??
```

**❌ 문제점:**
- 설정은 있으나 실제 구현 코드 확인 필요
- Rate Limiting 필터/인터셉터 구현 여부 불명확

**검증 필요:**
- 로그인 시도 제한 로직 구현 여부
- API 요청 Rate Limiting 구현 여부
- Redis를 활용한 분산 Rate Limiting 구현 여부

**개선 권장사항:**
1. Spring Cloud Gateway Rate Limiting 필터 추가
2. Redis 기반 분산 Rate Limiting 구현
3. IP 기반 및 사용자 기반 Rate Limiting 모두 적용
4. 로그인 실패 횟수 추적 및 계정 잠금

---

## 7. 에러 메시지 정보 노출

### 7.1 현재 상태

**❌ 취약점:**

```163:174:store/src/main/java/com/dorandoran/store/exception/GlobalExceptionHandler.java
log.error("Unexpected Exception: ", ex);

ErrorResponse error = ErrorResponse.builder()
    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
    .code("INTERNAL_SERVER_ERROR")
    .message("서버 내부 오류가 발생했습니다")
    .detail(ex.getMessage())  // ⚠️ 예외 메시지 노출
    .path(request.getRequestURI())
    .build();
```

**문제점:**
- 예외 메시지(`ex.getMessage()`)가 클라이언트에 노출됨
- 스택 트레이스나 내부 구현 세부사항 노출 가능
- 데이터베이스 스키마 정보, 파일 경로 등 노출 위험

**개선 권장사항:**
1. 프로덕션 환경에서는 상세 에러 메시지 숨김
2. 개발 환경에서만 상세 정보 제공
3. 에러 로깅은 서버 측에서만 수행
4. 클라이언트에는 일반적인 에러 메시지만 전달

---

## 8. CORS 설정

### 8.1 현재 상태

**✅ 양호한 점:**
- 허용된 Origin 목록 명시
- Credentials 허용 설정

**⚠️ 주의사항:**

```56:58:gateway/src/main/java/com/dorandoran/gateway/config/SecurityConfig.java
// 와일드카드 도메인 허용 (Spring 5.3+)
// setAllowedOriginPatterns()를 사용하여 패턴 기반 허용
corsConfig.addAllowedOriginPattern("https://*.doran-chat.com");
```

**위험도:** 🟡 **중간**

**문제점:**
- 와일드카드 패턴(`*.doran-chat.com`) 사용
- 서브도메인 탈취 시 공격 가능
- `*.vercel.app` 패턴도 매우 넓은 범위

**개선 권장사항:**
1. 가능한 한 구체적인 도메인만 허용
2. 와일드카드 사용 시 신뢰할 수 있는 서브도메인만 허용
3. CORS 정책 정기적 검토

---

## 9. HTTPS 강제

### 9.1 현재 상태

**✅ 양호한 점:**
- HTTPS 도메인 사용 (`https://doran-chat.com`)
- SSL/TLS 인증서 설정 (Let's Encrypt)

**❌ 개선 필요:**

**문제점:**
- HTTP에서 HTTPS로 리다이렉트 강제 여부 불명확
- HSTS (HTTP Strict Transport Security) 헤더 설정 여부 확인 필요
- Secure 쿠키 플래그 설정 확인 필요

**개선 권장사항:**
1. Nginx에서 HTTP → HTTPS 리다이렉트 설정
2. HSTS 헤더 추가: `Strict-Transport-Security: max-age=31536000; includeSubDomains`
3. Secure 쿠키 플래그 설정 (쿠키 사용 시)

---

## 10. 기타 보안 위협

### 10.1 JWT 토큰 보안

**✅ 양호한 점:**
- HS256 서명 알고리즘 사용
- Access Token + Refresh Token 패턴
- 토큰 블랙리스트 구현 (Redis)

**⚠️ 주의사항:**
- JWT Secret 키 강도 확인 필요
- 토큰 만료 시간 적절성 검토 (Access: 1시간, Refresh: 7일)

### 10.2 HMAC 서비스 간 인증

**✅ 양호한 점:**
- 서비스 간 통신에 HMAC 서명 사용
- 타임스탬프 기반 리플레이 공격 방지 (60초)

```63:67:auth/src/main/java/com/dorandoran/auth/config/HmacAuthInterceptor.java
if (Math.abs(now - t) > skewMs) { 
    log.debug("타임스탬프 만료: now={}, ts={}, diff={}", now, t, Math.abs(now - t));
    response.setStatus(401); 
    return false; 
}
```

### 10.3 파일 업로드 보안

**현재 설정:**
```97:99:src/main/resources/application.properties
spring.servlet.multipart.enabled=true
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
```

**⚠️ 검증 필요:**
- 파일 타입 검증 로직 확인
- 파일 확장자 화이트리스트 확인
- 악성 파일 스캔 여부

### 10.4 세션 관리

**현재 상태:**
- JWT 기반 인증으로 서버 세션 미사용
- Stateless 아키텍처

**✅ 양호:** 서버 세션 관련 취약점 없음

### 10.5 의존성 보안

**검증 필요:**
- Spring Boot 버전 보안 패치 적용 여부
- 의존성 취약점 스캔 (OWASP Dependency-Check)
- 정기적인 보안 업데이트

---

## 11. 종합 평가 및 개선 권장사항

### 11.1 위험도 요약

| 위협 유형 | 위험도 | 상태 | 우선순위 |
|---------|--------|------|---------|
| 비밀번호 로그 노출 | 🔴 심각 | ❌ 취약 | **P0 (즉시 수정)** |
| XSS 공격 | 🟡 중간 | ⚠️ 검증 필요 | P1 |
| HTTP 혼동 공격 | 🟡 중간 | ⚠️ 부분 대응 | P1 |
| Rate Limiting | 🟡 중간 | ❌ 구현 불명확 | P1 |
| 에러 메시지 노출 | 🟡 중간 | ❌ 취약 | P2 |
| CORS 설정 | 🟡 중간 | ⚠️ 주의 필요 | P2 |
| HTTPS 강제 | 🟢 낮음 | ⚠️ 검증 필요 | P2 |
| SQL Injection | 🟢 낮음 | ✅ 안전 | - |
| CSRF | 🟢 낮음 | ✅ JWT 사용 | - |

### 11.2 즉시 수정 필요 (P0)

1. **비밀번호 로그 노출 제거**
   ```java
   // auth/src/main/java/com/dorandoran/auth/service/AuthService.java
   // 66-68번 라인 수정
   log.info("비밀번호 검증 시도: email={}", request.getEmail());
   boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.passwordHash());
   log.info("비밀번호 검증 결과: email={}, success={}", request.getEmail(), passwordMatches);
   ```

### 11.3 단기 개선 사항 (P1)

1. **Rate Limiting 구현**
   - Spring Cloud Gateway Rate Limiting 필터 추가
   - Redis 기반 분산 Rate Limiting
   - 로그인 시도 제한 강화

2. **XSS 방어 강화**
   - 백엔드 HTML 이스케이프 처리
   - Content Security Policy (CSP) 헤더 추가
   - 프론트엔드 입력 검증 강화

3. **HTTP 혼동 공격 대응**
   - 프로토콜 버전 검증 필터
   - 자동 IP 블랙리스트 시스템
   - 비정상 요청 패턴 탐지

### 11.4 중기 개선 사항 (P2)

1. **에러 메시지 보안**
   - 프로덕션 환경에서 상세 에러 숨김
   - 환경별 에러 처리 분리

2. **CORS 정책 강화**
   - 와일드카드 패턴 축소
   - 구체적인 도메인만 허용

3. **HTTPS 강제**
   - HTTP → HTTPS 리다이렉트
   - HSTS 헤더 추가

### 11.5 장기 개선 사항

1. **보안 모니터링**
   - 보안 이벤트 로깅 및 알림
   - 이상 행위 탐지 시스템

2. **정기 보안 감사**
   - 의존성 취약점 스캔
   - 보안 코드 리뷰
   - 침투 테스트

3. **보안 문서화**
   - 보안 정책 문서화
   - 사고 대응 계획 수립

---

## 결론

DoranDoran 프로젝트는 전반적으로 **양호한 보안 수준**을 유지하고 있으나, 몇 가지 **심각한 취약점**이 발견되었습니다. 특히 **비밀번호 로그 노출** 문제는 즉시 수정이 필요합니다.

**주요 강점:**
- BCrypt 비밀번호 해싱
- JWT 기반 인증
- HMAC 서비스 간 인증
- SQL Injection 방어 (JPA 사용)

**주요 취약점:**
- 비밀번호 평문 로그 노출 (심각)
- Rate Limiting 구현 불명확
- 에러 메시지 정보 노출
- XSS 방어 검증 필요

위 개선 사항을 단계적으로 적용하여 보안 수준을 향상시키는 것을 권장합니다.















