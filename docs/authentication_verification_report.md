# 인증 구조 검증 보고서

## 개요
비밀번호 찾기 및 이메일 찾기 기능 구현 후, Gateway의 JWT 토큰 인증 구조와 각 서비스의 HMAC 헤더 인증 구조를 통과하는지 검증한 결과입니다.

## 검증 대상 엔드포인트

### 비밀번호 찾기 API
1. `POST /api/auth/password/reset/request-code` - 비밀번호 재설정 코드 요청
2. `POST /api/auth/password/reset/verify-code` - 비밀번호 재설정 코드 검증
3. `POST /api/auth/password/reset/execute` - 비밀번호 재설정 실행

### 이메일 찾기 API
4. `POST /api/users/find-email` - 이메일 찾기

## 인증 구조 개요

### 1. Gateway 레벨 (JWT 인증)
- **필터**: `JwtAuthFilter` (WebFilter)
- **역할**: 
  - JWT 토큰 검증 (Auth 서비스와 통신)
  - HMAC 헤더 주입 (X-User-Id, X-Auth-Ts, X-Auth-Sign)
- **제외 경로**: 인증 없이 접근 가능한 공개 API

### 2. 서비스 레벨 (HMAC 인증)
- **인터셉터**: `HmacAuthInterceptor` (각 서비스별)
- **역할**: Gateway에서 주입한 HMAC 헤더 검증
- **제외 경로**: 공개 엔드포인트는 HMAC 검증 생략

## 검증 결과

### ✅ Gateway JWT 인증 필터

**파일**: `gateway/src/main/java/com/dorandoran/gateway/filter/JwtAuthFilter.java`

**제외 경로 확인**:
- ✅ `/api/auth/password/reset`로 시작하는 모든 경로 제외됨
  - `/api/auth/password/reset/request-code` ✅
  - `/api/auth/password/reset/verify-code` ✅
  - `/api/auth/password/reset/execute` ✅
- ✅ `/api/users/find-email` 제외 경로 추가됨

**결과**: 모든 새 엔드포인트가 Gateway 레벨에서 인증 없이 통과 가능

### ✅ Auth 서비스 HMAC 인터셉터

**파일**: `auth/src/main/java/com/dorandoran/auth/config/HmacAuthInterceptor.java`

**제외 경로 확인**:
- ✅ `/api/auth/password/reset`로 시작하는 모든 경로 제외됨
  - `/api/auth/password/reset/request-code` ✅
  - `/api/auth/password/reset/verify-code` ✅
  - `/api/auth/password/reset/execute` ✅

**결과**: 비밀번호 재설정 관련 엔드포인트가 HMAC 검증 없이 통과 가능

### ✅ User 서비스 HMAC 인터셉터

**파일**: `user/src/main/java/com/dorandoran/user/config/HmacAuthInterceptor.java`

**제외 경로 확인**:
- ✅ `/api/users/find-email` 제외 경로 추가됨

**결과**: 이메일 찾기 엔드포인트가 HMAC 검증 없이 통과 가능

## CORS 설정 검증

### ✅ Gateway CORS 설정

**파일**: `gateway/src/main/java/com/dorandoran/gateway/config/SecurityConfig.java`

**설정 내용**:
- 허용 Origin:
  - 로컬 개발: `http://localhost:3000`, `http://localhost:3001`
  - 프로덕션: `https://doran-chat.com`, `https://www.doran-chat.com`, `https://doran-chat.vercel.app`
  - 와일드카드: `https://*.doran-chat.com`, `https://*.vercel.app`
- 허용 메서드: `*` (모든 HTTP 메서드)
- 허용 헤더: `*` (모든 헤더)
- Credentials: `true`

**결과**: 모든 새 엔드포인트가 CORS 정책을 통과 가능

### 서비스 레벨 CORS
- 각 서비스(User, Auth 등)는 Gateway를 통해서만 접근하므로 별도 CORS 설정 불필요
- Gateway에서 CORS 처리 후 서비스로 전달

## 요청 흐름 검증

### 비밀번호 재설정 코드 요청 예시

```
1. 클라이언트 → Gateway (POST /api/auth/password/reset/request-code)
   ├─ JwtAuthFilter: isExcludedPath() → true (제외 경로)
   └─ 인증 없이 통과 ✅

2. Gateway → Auth 서비스
   └─ 라우팅 (Gateway Filter Chain)

3. Auth 서비스 HmacAuthInterceptor
   ├─ isExcludedPath() → true (제외 경로)
   └─ HMAC 검증 생략, 통과 ✅

4. AuthController.requestPasswordResetCode()
   └─ 정상 처리 ✅
```

### 이메일 찾기 요청 예시

```
1. 클라이언트 → Gateway (POST /api/users/find-email)
   ├─ JwtAuthFilter: isExcludedPath() → true (제외 경로)
   └─ 인증 없이 통과 ✅

2. Gateway → User 서비스
   └─ 라우팅 (Gateway Filter Chain)

3. User 서비스 HmacAuthInterceptor
   ├─ isExcludedPath() → true (제외 경로)
   └─ HMAC 검증 생략, 통과 ✅

4. UserController.findEmail()
   └─ 정상 처리 ✅
```

## 수정 사항

### 1. Gateway JwtAuthFilter
**파일**: `gateway/src/main/java/com/dorandoran/gateway/filter/JwtAuthFilter.java`
- ✅ `/api/users/find-email` 제외 경로 추가

### 2. User 서비스 HmacAuthInterceptor
**파일**: `user/src/main/java/com/dorandoran/user/config/HmacAuthInterceptor.java`
- ✅ `/api/users/find-email` 제외 경로 추가

## 최종 검증 결과

| 엔드포인트 | Gateway JWT | Auth HMAC | User HMAC | CORS | 상태 |
|-----------|------------|-----------|-----------|------|------|
| POST /api/auth/password/reset/request-code | ✅ 제외 | ✅ 제외 | N/A | ✅ 통과 | ✅ 정상 |
| POST /api/auth/password/reset/verify-code | ✅ 제외 | ✅ 제외 | N/A | ✅ 통과 | ✅ 정상 |
| POST /api/auth/password/reset/execute | ✅ 제외 | ✅ 제외 | N/A | ✅ 통과 | ✅ 정상 |
| POST /api/users/find-email | ✅ 제외 | N/A | ✅ 제외 | ✅ 통과 | ✅ 정상 |

## 결론

✅ **모든 새로 추가한 엔드포인트가 인증 구조를 정상적으로 통과합니다.**

1. **Gateway 레벨**: JWT 인증 필터에서 모든 엔드포인트가 제외 경로로 등록됨
2. **서비스 레벨**: 각 서비스의 HMAC 인터셉터에서 해당 엔드포인트가 제외 경로로 등록됨
3. **CORS**: Gateway의 CORS 설정으로 모든 엔드포인트 접근 가능
4. **인증 없이 접근**: 비밀번호 찾기와 이메일 찾기는 인증 없이 접근 가능한 공개 API로 정상 동작

## 추가 권장사항

1. **Rate Limiting**: 공개 API이므로 Rate Limiting 적용 고려
2. **로깅**: 인증 없이 접근하는 엔드포인트이므로 접근 로그 기록 권장
3. **모니터링**: 비정상적인 접근 패턴 감지를 위한 모니터링 설정
