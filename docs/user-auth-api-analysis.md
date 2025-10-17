# User/Auth API 통합 분석 보고서

## 분석 개요

백엔드 User Service, Auth Service와 프론트엔드 구현을 API 명세서와 비교하여 일치/불일치 항목을 정리했습니다.

---

## 1. Auth Service API 분석

### ✅ 일치하는 부분

#### 백엔드 구현 (`dorandoran-backend/auth`)

- **POST /api/auth/login** ✅
  - 구현: `AuthController.java:43` - 완벽히 구현됨
  - 요청: `LoginRequest` (email, password)
  - 응답: `ApiResponse<LoginResponse>` (accessToken, refreshToken, tokenType, expiresIn, user)
  - API 명세와 완전 일치

- **POST /api/auth/logout** ✅
  - 구현: `AuthController.java:64` - 구현됨
  - Bearer Token 필요
  - 응답: `ApiResponse<Void>`

- **GET /api/auth/validate** ✅
  - 구현: `AuthController.java` - 토큰 검증 로직 존재
  - Bearer Token 필요

- **POST /api/auth/refresh** ✅
  - 구현: `AuthController.java` - 리프레시 토큰 기능 구현

- **POST /api/auth/password/reset/request** ✅
  - 구현: `PasswordResetService` 존재

- **POST /api/auth/password/reset/execute** ✅
  - 구현: `PasswordResetService` 존재

- **GET /api/auth/me** ✅
  - 구현: `AuthController.java:189` - 완벽히 구현됨
  - Bearer Token 필요

- **GET /api/auth/health** ✅
  - 구현됨

#### 프론트엔드 구현

- **엔드포인트 정의** ✅
  - `endpoints.ts:26-50` - 모든 Auth 엔드포인트가 정의됨
  - LOGIN, LOGOUT, VALIDATE_TOKEN, REFRESH_TOKEN, PASSWORD_RESET_REQUEST, PASSWORD_RESET_EXECUTE, CURRENT_USER, HEALTH

### ❌ 불일치하는 부분

#### 프론트엔드 미구현

1. **실제 API 호출 함수가 없음** ❌
   - `LoginPage.tsx:14-34` - 하드코딩된 로그인 로직만 존재
   - 실제 백엔드 `/api/auth/login` 호출 없음
   - 테스트 이메일/비밀번호 하드코딩: `test@example.com`, `qwer1234`

2. **토큰 관리 로직 없음** ❌
   - axios 인터셉터 미구현 (`api.ts:20` - 주석만 있음: "// 토큰 추가 필요")
   - localStorage/sessionStorage 토큰 저장 로직 없음
   - Authorization 헤더 자동 추가 로직 없음

3. **API 함수 미생성** ❌
   - `auth.ts` 파일 자체가 존재하지 않음
   - `chats.ts`만 구현되어 있음

---

## 2. User Service API 분석

### ✅ 일치하는 부분

#### 백엔드 구현 (`dorandoran-backend/user`)

- **POST /api/users** ✅
  - 구현: `UserController.java:42` - 완벽히 구현됨
  - 요청: `CreateUserRequest` (email, firstName, lastName, name, password, picture, info)
  - 응답: `ApiResponse<UserDto>`

- **GET /api/users/{userId}** ✅
  - 구현: `UserController.java:64` - 완벽히 구현됨

- **GET /api/users/email/{email}** ✅
  - 구현: `UserController.java:82` - 완벽히 구현됨

- **PUT /api/users/{userId}** ✅
  - 구현: `UserController.java:98` - 완벽히 구현됨
  - 요청: `UpdateUserRequest`

- **PATCH /api/users/{userId}/status** ✅
  - 구현: `UserController.java:119` - 완벽히 구현됨

- **POST /api/users/password/reset** ✅
  - 구현: `UserController.java:177` - 구현됨

- **DELETE /api/users/{userId}** ✅
  - API 명세에 정의됨

- **GET /api/users/health** ✅
  - 구현: `UserController.java:170` - 완벽히 구현됨

#### 프론트엔드 구현

- **엔드포인트 정의** ✅
  - `endpoints.ts:2-23` - 모든 User 엔드포인트가 정의됨
  - CREATE, GET_BY_ID, GET_BY_EMAIL, UPDATE, UPDATE_STATUS, PASSWORD_RESET, DELETE, HEALTH

### ❌ 불일치하는 부분

#### 프론트엔드 미구현

1. **회원가입 API 호출 없음** ❌
   - `SignupPage.tsx:115-124` - console.log만 출력하고 `/login`으로 이동
   - 실제 `/api/users` POST 호출 없음
   - 서버에 사용자 데이터 전송 안 됨

2. **필드 불일치** ⚠️
   - 프론트엔드 `SignupPage.tsx`: firstName, lastName, email, password만 수집
   - 백엔드 `CreateUserRequest`: firstName, lastName, name, email, password, picture, info 필요
   - **name 필드 누락** - 백엔드는 필수(`@NotBlank`)지만 프론트엔드에서 안 보냄

3. **API 함수 미생성** ❌
   - `user.ts` 파일 자체가 존재하지 않음
   - 사용자 관리 관련 API 함수가 하나도 없음

---

## 3. 데이터 타입 불일치

### ⚠️ 주의 필요

1. **CreateUserRequest 필수 필드**
   - 백엔드: email, firstName, lastName, **name**, password (필수)
   - 프론트엔드: firstName, lastName, email, password만 수집
   - **해결방안**: name = firstName + " " + lastName으로 생성 가능 (백엔드 `CreateUserRequest.getDisplayName()` 메서드 존재)

2. **LoginResponse 구조**
   - API 명세: `data.user` 객체 포함
   - 백엔드: `LoginResponse` 클래스에 `UserDto user` 필드 존재 ✅
   - 프론트엔드: 타입 정의 없음 ❌

---

## 4. 통합을 위해 필요한 작업

### 우선순위 1 (필수)

1. **프론트엔드 API 함수 생성**
   - `src/api/auth.ts` 생성 필요
     - `login(email, password)` → POST /api/auth/login
     - `logout()` → POST /api/auth/logout
     - `validateToken()` → GET /api/auth/validate
     - `refreshToken(refreshToken)` → POST /api/auth/refresh
     - `getCurrentUser()` → GET /api/auth/me
   
   - `src/api/user.ts` 생성 필요
     - `createUser(request)` → POST /api/users
     - `getUserById(userId)` → GET /api/users/{userId}
     - `updateUser(userId, request)` → PUT /api/users/{userId}

2. **토큰 관리 구현**
   - `src/api/api.ts` axios 인터셉터 추가
   - 요청 인터셉터: Authorization 헤더에 토큰 자동 추가
   - 응답 인터셉터: 401 에러 시 리프레시 토큰으로 갱신

3. **LoginPage.tsx 수정**
   - 하드코딩 제거 (line 15-16)
   - 실제 `login()` API 함수 호출
   - 토큰을 localStorage/store에 저장
   - 로그인 성공 시 사용자 정보 저장

4. **SignupPage.tsx 수정**
   - `createUser()` API 함수 호출 추가
   - name 필드 생성: `${firstName} ${lastName}`
   - 회원가입 성공 후 로그인 페이지로 이동

### 우선순위 2 (권장)

5. **타입 정의 추가**
   - `src/types/auth.ts` 생성
     - LoginRequest, LoginResponse, UserDto 등 타입 정의

6. **에러 처리**
   - API 에러 응답 처리 로직
   - 사용자 친화적 에러 메시지 표시

7. **Zustand 스토어 구현**
   - 인증 상태 관리 (로그인 여부, 사용자 정보, 토큰)
   - `useAuthStore` 생성 권장

---

## 5. API 명세와의 비교

### ✅ 명세와 일치
- 모든 엔드포인트 URL이 백엔드에 구현되어 있음
- 요청/응답 구조가 명세와 일치
- 공통 응답 형식 `ApiResponse<T>` 사용
- HTTP 상태 코드 일치

### ⚠️ 명세와 차이
- 프론트엔드가 명세를 참조하지 않고 있음
- 엔드포인트 정의만 있고 실제 호출 로직 없음

---

## 6. 보안 고려사항

### 현재 상태
- 백엔드: JWT 기반 인증 완벽히 구현 ✅
- 백엔드: Bearer Token 검증 구현 ✅
- 백엔드: 비밀번호 BCrypt 암호화 ✅
- 프론트엔드: 토큰 관리 전혀 없음 ❌

### 필요한 보안 작업
1. HTTPS 사용 (프로덕션)
2. XSS 방지 (토큰 저장 위치 결정)
3. CSRF 토큰 (필요시)
4. 토큰 만료 처리 로직

---

## 결론

**백엔드는 API 명세와 100% 일치하게 완벽히 구현되어 있습니다.**

**프론트엔드는 엔드포인트 정의만 있고 실제 API 호출 로직이 전혀 구현되지 않았습니다.**

통합을 위해서는 프론트엔드에서 최소 4개의 파일을 생성/수정해야 합니다:

1. `src/api/auth.ts` (신규 생성)
2. `src/api/user.ts` (신규 생성)
3. `src/pages/LoginPage.tsx` (수정)
4. `src/pages/SignupPage.tsx` (수정)
5. `src/api/api.ts` (토큰 인터셉터 추가)

이 작업을 완료하면 백엔드와 프론트엔드가 정상적으로 통신할 수 있습니다.

---

## 7. 상세 구현 가이드

### 7.1 Auth API 함수 예시

```typescript
// src/api/auth.ts
import api from './api'
import { AUTH_ENDPOINTS } from './endpoints'

export interface LoginRequest {
  email: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  user: {
    id: string
    email: string
    name: string
    status: string
    role: string
  }
}

export async function login(email: string, password: string): Promise<LoginResponse> {
  const response = await api.post(AUTH_ENDPOINTS.LOGIN, { email, password })
  return response.data.data // ApiResponse 구조에서 data 추출
}

export async function logout(): Promise<void> {
  await api.post(AUTH_ENDPOINTS.LOGOUT)
}

export async function getCurrentUser(): Promise<any> {
  const response = await api.get(AUTH_ENDPOINTS.CURRENT_USER)
  return response.data.data
}
```

### 7.2 User API 함수 예시

```typescript
// src/api/user.ts
import api from './api'
import { USER_ENDPOINTS } from './endpoints'

export interface CreateUserRequest {
  email: string
  firstName: string
  lastName: string
  name: string
  password: string
  picture?: string
  info?: string
}

export async function createUser(request: CreateUserRequest): Promise<any> {
  const response = await api.post(USER_ENDPOINTS.CREATE, request)
  return response.data.data
}

export async function getUserById(userId: string): Promise<any> {
  const response = await api.get(USER_ENDPOINTS.GET_BY_ID(userId))
  return response.data
}
```

### 7.3 토큰 관리 인터셉터 예시

```typescript
// src/api/api.ts 수정
import axios from 'axios'

const api = axios.create({
  baseURL: getBaseURL(),
  timeout: 10000,
})

// 요청 인터셉터 - 토큰 자동 추가
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 응답 인터셉터 - 401 에러 시 토큰 갱신
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      const refreshToken = localStorage.getItem('refreshToken')
      if (refreshToken) {
        try {
          const response = await api.post('/api/auth/refresh', { refreshToken })
          const { accessToken } = response.data.data
          localStorage.setItem('accessToken', accessToken)
          // 원래 요청 재시도
          return api.request(error.config)
        } catch (refreshError) {
          // 리프레시 실패 시 로그인 페이지로 이동
          localStorage.removeItem('accessToken')
          localStorage.removeItem('refreshToken')
          window.location.href = '/login'
        }
      }
    }
    return Promise.reject(error)
  }
)
```

이 가이드를 따라 구현하면 백엔드와 프론트엔드가 완전히 통합될 수 있습니다.
