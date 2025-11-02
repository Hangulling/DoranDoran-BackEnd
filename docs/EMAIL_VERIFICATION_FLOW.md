# 이메일 인증 선행 회원가입 플로우 상세 가이드

## 목차

1. [개요](#개요)
2. [단계별 상세 플로우](#단계별-상세-플로우)
3. [플로우 다이어그램](#플로우-다이어그램)
4. [주요 변경점](#주요-변경점)
5. [에러 시나리오](#에러-시나리오)

---

## 개요

### 기존 플로우 (변경 전)
1. 회원가입 → INACTIVE 상태로 계정 생성
2. 이메일 인증 링크 발송
3. 인증 완료 → ACTIVE로 변경

### 새로운 플로우 (변경 후)
1. 회원가입 폼 작성 (서버에 저장하지 않음)
2. 이메일 인증 요청 → 인증 링크 발송
3. 이메일 링크 클릭 → 인증 완료 (Redis에 저장)
4. SignupPage로 리다이렉트
5. 인증 완료 확인 후 → 회원가입 API 호출 → ACTIVE 상태로 바로 생성

### 장점
- ✅ 인증된 이메일만 가입 가능 (더미 계정 방지)
- ✅ 상태 관리 단순화 (INACTIVE 불필요, ACTIVE만 관리)
- ✅ 보안 강화

---

## 단계별 상세 플로우

### Phase 1: 회원가입 페이지 진입 및 정보 입력

#### Step 1: 사용자가 SignupPage 접속
- **URL**: `/signup`
- **상태**: 사용자가 회원가입 폼에 접근
- **입력 필드**: First Name, Last Name, Email, Password, Password Check, 약관 동의

#### Step 2: 사용자 정보 입력
- 이름, 이메일, 비밀번호 등 입력
- **중요**: 아직 서버에 저장하지 않음 (로컬 상태만 유지)

---

### Phase 2: 이메일 인증 요청

#### Step 3: 이메일 입력 및 "Send Code" 버튼 클릭
- 프론트엔드에서 이메일 형식 유효성 검사
- 버튼 클릭 시 `POST /api/auth/email/request-verification` API 호출

#### Step 4: 백엔드 처리 (Auth 서비스)

**API 요청:**
```
POST /api/auth/email/request-verification
Content-Type: application/json

{
  "email": "user@example.com"
}
```

**백엔드 처리 순서:**

1. **이메일 중복 확인**
   - `UserIntegrationService.isEmailDuplicate()` 호출
   - → User 서비스의 `GET /api/users/check-email/{email}` 호출
   - 중복인 경우 에러 반환: "이미 사용 중인 이메일입니다"

2. **토큰 생성**
   - UUID 기반 토큰 생성: `abc123-def456-ghi789...`

3. **Redis에 인증 대기 상태 저장**
   ```json
   Key: email:verification:user@example.com
   Value: {
     "email": "user@example.com",
     "token": "abc123-def456-...",
     "verified": false,
     "createdAt": "2025-01-20T10:25:00",
     "expiresAt": "2025-01-20T10:30:00"  // 5분 후
   }
   TTL: 5분 (자동 삭제)
   ```

4. **이메일 발송**
   - Gmail SMTP를 통해 인증 링크 발송
   - 링크 형식: `{backend-url}/api/auth/email/verify?token={token}&email={email}`
   - 예시: `http://localhost:8081/api/auth/email/verify?token=abc123...&email=user@example.com`

**API 응답:**
```json
{
  "success": true,
  "data": "sent",
  "message": "인증 메일이 발송되었습니다."
}
```

#### Step 5: 프론트엔드 반응
- 성공 시: "인증 메일이 발송되었습니다. 이메일을 확인해주세요." 메시지 표시
- `emailVerified` 상태는 아직 `false` (인증 전)
- "Send Code" 버튼 비활성화 (이미 전송됨)

---

### Phase 3: 이메일 인증 완료

#### Step 6: 사용자가 이메일 확인
- 이메일 클라이언트에서 인증 메일 확인
- 메일 제목: "[DoranDoran] 이메일 인증을 완료하세요"

#### Step 7: 이메일의 인증 링크 클릭
- 링크: `http://backend:8081/api/auth/email/verify?token=abc123...&email=user@example.com`
- 브라우저가 백엔드 API로 직접 요청

#### Step 8: 백엔드 인증 처리 (Auth 서비스)

**API 요청:**
```
GET /api/auth/email/verify?token=abc123...&email=user@example.com
```

**백엔드 처리 순서:**

1. **토큰 검증**
   - Redis에서 `email:verification:user@example.com` 조회
   - 토큰 일치 여부 확인
   - 만료 시간 확인 (5분 내 유효)
   - 실패 시: `/signup?email=user@example.com&verified=false&error=인증 링크가 유효하지 않거나 만료되었습니다.`로 리다이렉트

2. **인증 상태 업데이트**
   - Redis에서 `verified: true`로 업데이트
   ```json
   {
     "email": "user@example.com",
     "token": "abc123-def456-...",
     "verified": true,  // ← 변경됨
     "createdAt": "2025-01-20T10:25:00",
     "expiresAt": "2025-01-20T10:30:00"
   }
   ```

3. **프론트엔드로 리다이렉트**
   - `HTTP 302 Redirect` → `/signup?email=user@example.com&verified=true`

#### Step 9: SignupPage 복귀 및 인증 완료 확인

**프론트엔드 처리:**
```typescript
// URL 파라미터 파싱
const params = new URLSearchParams(location.search)
const emailParam = params.get('email')      // "user@example.com"
const verifiedParam = params.get('verified') // "true"
```

**인증 완료 확인 방법:**

1. **URL 파라미터 확인 (즉시)**
   - `verified=true`이고 `emailParam === email`이면
   - `emailVerified = true` 설정
   - 성공 메시지 표시: "이메일 인증이 완료되었습니다."

2. **폴링 방식 (백업)**
   - 3초마다 `GET /api/auth/email/check?email=user@example.com` 호출
   - 응답 확인: `{ "data": { "verified": true } }`
   - `verified: true` 확인되면 인증 완료 처리

---

### Phase 4: 회원가입 완료

#### Step 10: 인증 완료 후 "Sign up" 버튼 활성화

**버튼 활성화 조건:**
- ✅ First Name 입력됨
- ✅ Last Name 입력됨
- ✅ Email 입력 및 형식 유효
- ✅ Password 입력 및 형식 유효 (8-20자, 영문+숫자)
- ✅ Password Check 일치
- ✅ 약관 동의 완료
- ✅ **이메일 인증 완료 (`emailVerified === true`)** ← 새로 추가된 조건

#### Step 11: "Sign up" 버튼 클릭

**프론트엔드 동작:**
1. 최종 유효성 검사 수행
2. 확인 모달 표시: "Sign Up Complete - Welcome! Ready to start Chatting?"
3. "Start" 클릭 시 회원가입 API 호출

#### Step 12: 회원가입 API 호출

**API 요청:**
```
POST /api/users
Content-Type: application/json

{
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "name": "John Doe",
  "password": "password123"
}
```

#### Step 13: 백엔드 처리 (User 서비스)

**`UserService.createUser()` 처리 순서:**

1. **이메일 중복 검사**
   ```java
   if (userRepository.existsByEmail(request.email())) {
       throw new DoranDoranException(ErrorCode.EMAIL_ALREADY_EXISTS);
   }
   ```

2. **이메일 인증 완료 여부 확인**
   ```java
   boolean isEmailVerified = authServiceIntegration.isEmailVerified(request.email());
   // → Auth 서비스의 GET /api/auth/email/check?email=... 호출
   // → Redis에서 verified 상태 확인
   
   if (!isEmailVerified) {
       throw new DoranDoranException(ErrorCode.INVALID_REQUEST, 
           "이메일 인증을 먼저 완료해주세요.");
   }
   ```

3. **비밀번호 검증 및 암호화**
   ```java
   validateBasicPasswordPolicy(request.password());
   String encodedPassword = passwordEncoder.encode(request.password());
   ```

4. **사용자 생성 (ACTIVE 상태로 바로 생성)**
   ```java
   User user = User.builder()
       .id(UUID.randomUUID())
       .email(request.email())
       .firstName(request.firstName())
       .lastName(request.lastName())
       .passwordHash(encodedPassword)
       .status(User.UserStatus.ACTIVE)  // ← INACTIVE가 아닌 ACTIVE!
       // ...
       .build();
   ```

5. **데이터베이스 저장**
   ```java
   User savedUser = userRepository.save(user);
   ```

6. **이메일 인증 데이터 삭제 (선택적)**
   ```java
   authServiceIntegration.deleteEmailVerification(request.email());
   // Redis TTL로 자동 삭제되지만 명시적으로 처리
   ```

7. **사용자 생성 이벤트 발행**
   ```java
   UserCreatedEvent event = UserCreatedEvent.of(...);
   eventPublisher.publishEvent(event);
   // → Auth 서비스의 UserEventListener가 수신
   // → 인증 정보 초기화, 기본 권한 설정 등 처리
   ```

#### Step 14: 회원가입 완료

**프론트엔드 동작:**
- 성공 응답 수신
- 폼 초기화: `resetForm()`, `resetAgreements()`
- 로그인 페이지로 이동: `navigate('/login', { replace: true })`

---

## 플로우 다이어그램

```
[사용자]                          [프론트엔드]                    [Auth 서비스]              [User 서비스]
   │                                   │                              │                          │
   ├─ SignupPage 접속 ────────────────►│                              │                          │
   │                                   │                              │                          │
   ├─ 정보 입력 ───────────────────────►│                              │                          │
   │                                   │                              │                          │
   ├─ "Send Code" 클릭 ────────────────►│                              │                          │
   │                                   ├─ POST /email/request-verification ──►│                          │
   │                                   │                              ├─ 이메일 중복 확인 ────────►│
   │                                   │                              ├─ 토큰 생성                 │
   │                                   │                              ├─ Redis 저장 (verified: false)│
   │                                   │                              ├─ 이메일 발송              │
   │                                   │◄─ 성공 응답 ──────────────────│                          │
   │                                   ├─ "인증 메일 발송됨" 표시      │                          │
   │                                   │                              │                          │
   ├─ 이메일 확인 ────────────────────────────────────────────────────────────────────────────►│
   │                                                                                          │
   ├─ 인증 링크 클릭 ──────────────────────────────────────►│                          │
   │                                   │                    ├─ GET /email/verify?token=...&email=...│
   │                                   │                    ├─ 토큰 검증                │
   │                                   │                    ├─ Redis 업데이트 (verified: true)│
   │                                   │◄─ Redirect /signup?verified=true ───────────│                          │
   │                                   │                              │                          │
   │                                   ├─ URL 파라미터 확인           │                          │
   │                                   ├─ emailVerified = true        │                          │
   │                                   ├─ "인증 완료" 표시            │                          │
   │                                   │                              │                          │
   ├─ "Sign up" 클릭 ──────────────────►│                              │                          │
   │                                   ├─ POST /api/users ──────────────────────────────────────►│
   │                                   │                              │          ├─ 이메일 중복 확인│
   │                                   │                              │          ├─ 인증 확인 요청 ──►│
   │                                   │                              │◄─ verified: true ──────────┤
   │                                   │                              │          ├─ ACTIVE 상태로 생성│
   │                                   │◄─ 성공 응답 ────────────────────────────────────────────┤
   │                                   ├─ /login으로 이동              │                          │
   │                                   │                              │                          │
```

---

## 주요 변경점 요약

### 기존 플로우
1. 회원가입 → INACTIVE 생성
2. 이메일 인증 → ACTIVE로 변경
3. 문제: 인증하지 않아도 계정 생성됨

### 새로운 플로우
1. 이메일 인증 먼저 → Redis에 인증 상태 저장
2. 회원가입 시 인증 확인 → ACTIVE로 바로 생성
3. 장점: 인증된 이메일만 가입 가능, INACTIVE 상태 불필요, 보안 강화

---

## 에러 시나리오

### 1. 이메일 중복 시
- **상황**: 이미 가입된 이메일로 인증 요청
- **응답**: "이미 사용 중인 이메일입니다"
- **프론트엔드**: 버튼 비활성화, 에러 메시지 표시

### 2. 인증 링크 만료 (5분 초과)
- **상황**: 5분 후 링크 클릭
- **응답**: 리다이렉트 `/signup?email=...&verified=false&error=인증 링크가 만료되었습니다`
- **프론트엔드**: 에러 메시지 표시, 다시 "Send Code" 클릭 필요

### 3. 인증 미완료 상태에서 회원가입 시도
- **상황**: 인증 없이 회원가입 API 호출
- **응답**: `401 Bad Request`, "이메일 인증을 먼저 완료해주세요"
- **프론트엔드**: 에러 메시지 표시

### 4. 이메일 발송 실패 (SMTP 오류)
- **상황**: Gmail SMTP 연결 실패 또는 일일 발송 한도 초과
- **응답**: "인증 메일 발송에 실패했습니다"
- **프론트엔드**: 재시도 가능

### 5. 토큰 불일치
- **상황**: 잘못된 토큰으로 인증 링크 클릭
- **응답**: 리다이렉트 `/signup?email=...&verified=false&error=인증 링크가 유효하지 않습니다`
- **프론트엔드**: 에러 메시지 표시

---

## 기술 세부사항

### Redis 데이터 구조

**Key 형식:**
```
email:verification:{email}
```

**Value 형식 (JSON):**
```json
{
  "email": "user@example.com",
  "token": "abc123-def456-ghi789-jkl012-mno345",
  "verified": false,
  "createdAt": "2025-01-20T10:25:00",
  "expiresAt": "2025-01-20T10:30:00"
}
```

**TTL:** 5분 (자동 삭제)

### API 엔드포인트

#### POST /api/auth/email/request-verification
- **요청**: `{ "email": "user@example.com" }`
- **응답**: `{ "success": true, "data": "sent", "message": "인증 메일이 발송되었습니다." }`
- **에러**: 이메일 중복, SMTP 오류

#### GET /api/auth/email/verify?token=...&email=...
- **요청**: Query parameters
- **응답**: HTTP 302 Redirect → `/signup?email=...&verified=true`
- **에러**: 토큰 만료, 토큰 불일치 → `/signup?email=...&verified=false&error=...`

#### GET /api/auth/email/check?email=...
- **요청**: Query parameter `email`
- **응답**: `{ "success": true, "data": { "verified": true/false }, "message": "..." }`
- **용도**: 프론트엔드 폴링 (3초마다 호출)

### 프론트엔드 상태 관리

**주요 상태 변수:**
- `emailVerified: boolean` - 이메일 인증 완료 여부
- `emailError: string | null` - 이메일 관련 에러 메시지
- `emailSuccess: string | null` - 이메일 관련 성공 메시지
- `verifyLoading: boolean` - 인증 요청 중 로딩 상태

**폴링 설정:**
- 주기: 3초마다
- 조건: `emailVerified === false` && `email` 형식 유효
- 종료: `emailVerified === true` 또는 이메일 변경 시

---

## 환경 변수

### Auth 서비스
- `SPRING_MAIL_HOST` - SMTP 호스트 (기본: smtp.gmail.com)
- `SPRING_MAIL_PORT` - SMTP 포트 (기본: 587)
- `SPRING_MAIL_USERNAME` - Gmail 주소
- `SPRING_MAIL_PASSWORD` - Gmail 앱 비밀번호
- `FRONTEND_URL` - 프론트엔드 URL (리다이렉트용)
- `BACKEND_URL` - 백엔드 URL (이메일 링크용)
- `SPRING_REDIS_HOST` - Redis 호스트
- `SPRING_REDIS_PORT` - Redis 포트

### User 서비스
- `AUTH_SERVICE_URL` - Auth 서비스 URL (기본: http://localhost:8081)

---

## 구현 파일 목록

### 백엔드
- `auth/src/main/java/com/dorandoran/auth/service/EmailVerificationRedisService.java`
- `auth/src/main/java/com/dorandoran/auth/controller/AuthController.java`
- `auth/src/main/java/com/dorandoran/auth/service/UserIntegrationService.java`
- `user/src/main/java/com/dorandoran/user/service/AuthServiceIntegration.java`
- `user/src/main/java/com/dorandoran/user/service/UserService.java`
- `user/src/main/java/com/dorandoran/user/config/RestTemplateConfig.java`

### 프론트엔드
- `dorandoran-frontend/src/pages/SignupPage.tsx`
- `dorandoran-frontend/src/api/auth.ts`
- `dorandoran-frontend/src/api/endpoints.ts`

---

## 참고 문서

- [이메일 인증 선행 회원가입 플로우 구현 계획](./smtp-------------.plan.md)
- [API 명세서](./API_SPECIFICATION.md)

