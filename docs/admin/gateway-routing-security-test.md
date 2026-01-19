# 게이트웨이 라우팅/보안 규칙 테스트 가이드

**작성일**: 2026-01-17  
**대상**: Gateway, Auth/User/Chat 서비스, 내부 HMAC 인증

## 1. 사전 조건

- `gateway`, `auth`, `user`, `chat` 서비스가 모두 실행 중
- 게이트웨이 기본 주소: `http://localhost:8080`
- 테스트 계정: `ROLE_ADMIN` 권한 보유

---

## 2. 라우팅 테스트 (Admin)

### 2.1 라우팅 정상 확인

```
GET /api/admin/prompts/options/agent-types
```

**기대 결과**
- 200 OK
- User Service 응답 형식 (DTO 단독 JSON)

```
GET /api/admin/conversations
```

**기대 결과**
- 200 OK
- User Service → Chat Service 프록시 응답

### 2.2 게이트웨이 미스매치 확인

```
GET /api/admin/unknown-endpoint
```

**기대 결과**
- 404 Not Found

---

## 3. 보안 규칙 테스트 (JWT)

### 3.1 토큰 없이 접근

```
GET /api/admin/prompts/options/agent-types
```

**기대 결과**
- 401 또는 403

### 3.2 관리자 토큰으로 접근

1) 로그인
```
POST /api/auth/login
```
2) 토큰으로 재요청
```
GET /api/admin/prompts/options/agent-types
Authorization: Bearer <accessToken>
```

**기대 결과**
- 200 OK

---

## 4. 내부 호출(HMAC) 테스트

### 4.1 User -> Chat 내부 호출

- `POST /api/admin/prompts/test` 호출 시
- User Service가 Chat Service 내부 API 호출

**기대 결과**
- HMAC 검증 통과
- 200 OK

### 4.2 HMAC 위변조

- `X-Auth-Sign` 임의 변경

**기대 결과**
- 401 Unauthorized

---

## 5. 확인 포인트

- `/api/admin/**`가 **User Service**로 라우팅되는지
- Auth API는 `data` 래핑, Admin API는 DTO 단독 응답인지
- HMAC 헤더(`X-User-Id`, `X-Auth-Ts`, `X-Auth-Sign`) 검증 정상 동작 여부
