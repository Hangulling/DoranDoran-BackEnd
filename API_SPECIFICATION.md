# DoranDoran API 명세서

## 📋 개요

DoranDoran은 마이크로서비스 아키텍처 기반의 AI 채팅 플랫폼입니다. 이 문서는 모든 REST API 엔드포인트의 상세한 명세를 제공합니다.

### 🏗️ 아키텍처

- **API Gateway**: `http://localhost:8080` (모든 요청의 진입점)
- **User Service**: `http://localhost:8082` (사용자 관리)
- **Auth Service**: `http://localhost:8081` (인증/인가)
- **Chat Service**: `http://localhost:8083` (채팅 기능)
- **Database**: PostgreSQL (공유 데이터베이스)
- **Cache**: Redis

### 🔐 인증 방식

- **JWT (JSON Web Token)** 기반 인증
- **Bearer Token** 방식 사용
- **Authorization** 헤더에 `Bearer {token}` 형식으로 전송

---

## 📚 API 엔드포인트

## 1. 🏠 Gateway API

### 1.1 홈페이지
```http
GET /
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
    "batch": "/api/batch/**"
  }
}
```

---

## 2. 👤 User Service API

**Base URL:** `http://localhost:8080/api/users`

### 2.1 사용자 생성
```http
POST /api/users
Content-Type: application/json
```

**요청 본문:**
```json
{
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "name": "John Doe",
  "password": "password123",
  "picture": "https://example.com/profile.jpg",
  "info": "사용자 소개"
}
```

**응답:**
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "name": "John Doe",
  "passwordHash": "$2a$10$...",
  "picture": "https://example.com/profile.jpg",
  "info": "사용자 소개",
  "lastConnTime": "2024-01-01T12:00:00",
  "status": "ACTIVE",
  "role": "ROLE_USER",
  "coachCheck": false,
  "createdAt": "2024-01-01T12:00:00",
  "updatedAt": "2024-01-01T12:00:00"
}
```

### 2.2 사용자 조회 (ID)
```http
GET /api/users/{userId}
```

**경로 변수:**
- `userId` (string): 사용자 UUID

**응답:** UserDto 객체

### 2.3 사용자 조회 (이메일)
```http
GET /api/users/email/{email}
```

**경로 변수:**
- `email` (string): 사용자 이메일

**응답:** UserDto 객체

### 2.4 사용자 정보 수정
```http
PUT /api/users/{userId}
Content-Type: application/json
```

**요청 본문:**
```json
{
  "email": "newemail@example.com",
  "firstName": "Jane",
  "lastName": "Smith",
  "name": "Jane Smith",
  "picture": "https://example.com/new-profile.jpg",
  "info": "수정된 소개",
  "status": "ACTIVE",
  "coachCheck": true
}
```

**응답:** UserDto 객체

### 2.5 사용자 상태 수정
```http
PATCH /api/users/{userId}/status?status={status}
```

**쿼리 파라미터:**
- `status` (string): `ACTIVE`, `INACTIVE`, `SUSPENDED` 중 하나

**응답:** UserDto 객체

### 2.6 비밀번호 재설정
```http
POST /api/users/password/reset
Content-Type: application/json
```

**요청 본문:**
```json
{
  "email": "user@example.com",
  "newPassword": "newpassword123"
}
```

**응답:** `200 OK` (빈 본문)

### 2.7 비밀번호 업데이트
```http
PUT /api/users/{userId}/password
Content-Type: application/json
```

**요청 본문:**
```json
"newpassword123"
```

**응답:** `200 OK` (빈 본문)

### 2.8 사용자 삭제 (소프트 삭제)
```http
DELETE /api/users/{userId}
```

**응답:** `200 OK` (빈 본문)

### 2.9 헬스체크
```http
GET /api/users/health
```

**응답:**
```
User service is running
```

---

## 3. 🔐 Auth Service API

**Base URL:** `http://localhost:8080/api/auth`

### 3.1 로그인
```http
POST /api/auth/login
Content-Type: application/json
```

**요청 본문:**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**응답:**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "userId": "123e4567-e89b-12d3-a456-426614174000",
    "email": "user@example.com",
    "name": "John Doe"
  },
  "message": "로그인에 성공했습니다.",
  "errorCode": null,
  "timestamp": "2024-01-01T12:00:00"
}
```

### 3.2 로그아웃
```http
POST /api/auth/logout
Authorization: Bearer {accessToken}
```

**응답:**
```json
{
  "success": true,
  "data": null,
  "message": "로그아웃에 성공했습니다.",
  "errorCode": null,
  "timestamp": "2024-01-01T12:00:00"
}
```

### 3.3 토큰 검증
```http
GET /api/auth/validate
Authorization: Bearer {accessToken}
```

**응답:**
```json
{
  "success": true,
  "data": {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "name": "John Doe",
    "passwordHash": "$2a$10$...",
    "picture": "https://example.com/profile.jpg",
    "info": "사용자 소개",
    "lastConnTime": "2024-01-01T12:00:00",
    "status": "ACTIVE",
    "role": "ROLE_USER",
    "coachCheck": false,
    "createdAt": "2024-01-01T12:00:00",
    "updatedAt": "2024-01-01T12:00:00"
  },
  "message": "토큰이 유효합니다.",
  "errorCode": null,
  "timestamp": "2024-01-01T12:00:00"
}
```

### 3.4 토큰 갱신
```http
POST /api/auth/refresh
Content-Type: application/json
```

**요청 본문:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**응답:** LoginResponse 객체 (로그인 응답과 동일)

### 3.5 비밀번호 재설정 요청
```http
POST /api/auth/password/reset/request?email={email}
```

**쿼리 파라미터:**
- `email` (string): 사용자 이메일

**응답:**
```json
{
  "success": true,
  "data": "reset-token-12345",
  "message": "비밀번호 재설정 토큰이 생성되었습니다. 토큰: reset-token-12345",
  "errorCode": null,
  "timestamp": "2024-01-01T12:00:00"
}
```

### 3.6 비밀번호 재설정 실행
```http
POST /api/auth/password/reset/execute?token={token}&newPassword={newPassword}
```

**쿼리 파라미터:**
- `token` (string): 재설정 토큰
- `newPassword` (string): 새 비밀번호

**응답:**
```json
{
  "success": true,
  "data": null,
  "message": "비밀번호가 성공적으로 재설정되었습니다.",
  "errorCode": null,
  "timestamp": "2024-01-01T12:00:00"
}
```

### 3.7 현재 사용자 정보 조회
```http
GET /api/auth/me
Authorization: Bearer {accessToken}
```

**응답:** UserDto 객체 (토큰 검증 응답과 동일)

### 3.8 헬스체크
```http
GET /api/auth/health
```

**응답:**
```
Auth service is running
```

---

## 4. 💬 Chat Service API

**Base URL:** `http://localhost:8080/api/chat`

### 4.1 채팅방 생성/조회
```http
POST /api/chat/chatrooms
Content-Type: application/json
Authorization: Bearer {accessToken}
```

**요청 본문:**
```json
{
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "chatbotId": "456e7890-e89b-12d3-a456-426614174001",
  "name": "새로운 대화"
}
```

**응답:**
```json
{
  "id": "789e0123-e89b-12d3-a456-426614174002",
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "chatbotId": "456e7890-e89b-12d3-a456-426614174001",
  "name": "새로운 대화",
  "description": null,
  "lastMessageAt": null,
  "lastMessageId": null,
  "isArchived": false,
  "isDeleted": false,
  "createdAt": "2024-01-01T12:00:00",
  "updatedAt": "2024-01-01T12:00:00"
}
```

### 4.2 채팅방 목록 조회
```http
GET /api/chat/chatrooms?page={page}&size={size}
Authorization: Bearer {accessToken}
```

**쿼리 파라미터:**
- `page` (int, optional): 페이지 번호 (기본값: 0)
- `size` (int, optional): 페이지 크기 (기본값: 20)

**응답:**
```json
{
  "content": [
    {
      "id": "789e0123-e89b-12d3-a456-426614174002",
      "userId": "123e4567-e89b-12d3-a456-426614174000",
      "chatbotId": "456e7890-e89b-12d3-a456-426614174001",
      "name": "새로운 대화",
      "description": null,
      "lastMessageAt": "2024-01-01T12:30:00",
      "lastMessageId": "abc12345-e89b-12d3-a456-426614174003",
      "isArchived": false,
      "isDeleted": false,
      "createdAt": "2024-01-01T12:00:00",
      "updatedAt": "2024-01-01T12:30:00"
    }
  ],
  "pageable": {
    "sort": {
      "empty": true,
      "sorted": false,
      "unsorted": true
    },
    "offset": 0,
    "pageSize": 20,
    "pageNumber": 0,
    "paged": true,
    "unpaged": false
  },
  "last": true,
  "totalPages": 1,
  "totalElements": 1,
  "size": 20,
  "number": 0,
  "sort": {
    "empty": true,
    "sorted": false,
    "unsorted": true
  },
  "first": true,
  "numberOfElements": 1,
  "empty": false
}
```

### 4.3 메시지 목록 조회
```http
GET /api/chat/chatrooms/{chatroomId}/messages?page={page}&size={size}
Authorization: Bearer {accessToken}
```

**경로 변수:**
- `chatroomId` (UUID): 채팅방 ID

**쿼리 파라미터:**
- `page` (int, optional): 페이지 번호 (기본값: 0)
- `size` (int, optional): 페이지 크기 (기본값: 50)

**응답:**
```json
{
  "content": [
    {
      "id": "abc12345-e89b-12d3-a456-426614174003",
      "chatroomId": "789e0123-e89b-12d3-a456-426614174002",
      "senderType": "user",
      "senderId": "123e4567-e89b-12d3-a456-426614174000",
      "content": "안녕하세요!",
      "contentType": "text",
      "sequenceNumber": 1,
      "isEdited": false,
      "isDeleted": false,
      "createdAt": "2024-01-01T12:00:00"
    },
    {
      "id": "def67890-e89b-12d3-a456-426614174004",
      "chatroomId": "789e0123-e89b-12d3-a456-426614174002",
      "senderType": "bot",
      "senderId": "456e7890-e89b-12d3-a456-426614174001",
      "content": "안녕하세요! 무엇을 도와드릴까요?",
      "contentType": "text",
      "sequenceNumber": 2,
      "isEdited": false,
      "isDeleted": false,
      "createdAt": "2024-01-01T12:00:30"
    }
  ],
  "pageable": {
    "sort": {
      "empty": true,
      "sorted": false,
      "unsorted": true
    },
    "offset": 0,
    "pageSize": 50,
    "pageNumber": 0,
    "paged": true,
    "unpaged": false
  },
  "last": true,
  "totalPages": 1,
  "totalElements": 2,
  "size": 50,
  "number": 0,
  "sort": {
    "empty": true,
    "sorted": false,
    "unsorted": true
  },
  "first": true,
  "numberOfElements": 2,
  "empty": false
}
```

### 4.4 메시지 전송
```http
POST /api/chat/chatrooms/{chatroomId}/messages
Content-Type: application/json
Authorization: Bearer {accessToken}
X-User-Id: {userId}  // 선택적 헤더
```

**경로 변수:**
- `chatroomId` (UUID): 채팅방 ID

**요청 본문:**
```json
{
  "senderType": "user",
  "content": "안녕하세요!",
  "contentType": "text"
}
```

**응답:**
```json
{
  "id": "abc12345-e89b-12d3-a456-426614174003",
  "chatroomId": "789e0123-e89b-12d3-a456-426614174002",
  "senderType": "user",
  "senderId": "123e4567-e89b-12d3-a456-426614174000",
  "content": "안녕하세요!",
  "contentType": "text",
  "sequenceNumber": 1,
  "isEdited": false,
  "isDeleted": false,
  "createdAt": "2024-01-01T12:00:00"
}
```

### 4.5 실시간 메시지 스트림 (SSE)
```http
GET /api/chat/stream/{chatroomId}
Authorization: Bearer {accessToken}
Accept: text/event-stream
```

**경로 변수:**
- `chatroomId` (UUID): 채팅방 ID

**응답:** Server-Sent Events 스트림
```
data: {"event": "message", "data": {"id": "...", "content": "AI 응답", "senderType": "bot"}}

data: {"event": "typing", "data": {"isTyping": true}}

data: {"event": "error", "data": {"message": "오류 메시지"}}
```

---

## 📊 데이터 모델

### UserDto
```json
{
  "id": "UUID",
  "email": "string",
  "firstName": "string",
  "lastName": "string", 
  "name": "string",
  "passwordHash": "string",
  "picture": "string",
  "info": "string",
  "lastConnTime": "LocalDateTime",
  "status": "ACTIVE | INACTIVE | SUSPENDED",
  "role": "ROLE_USER | ROLE_ADMIN",
  "coachCheck": "boolean",
  "createdAt": "LocalDateTime",
  "updatedAt": "LocalDateTime"
}
```

### LoginResponse
```json
{
  "accessToken": "string",
  "refreshToken": "string", 
  "tokenType": "string",
  "expiresIn": "number",
  "userId": "string",
  "email": "string",
  "name": "string"
}
```

### ChatRoomResponse
```json
{
  "id": "UUID",
  "userId": "UUID",
  "chatbotId": "UUID",
  "name": "string",
  "description": "string",
  "lastMessageAt": "LocalDateTime",
  "lastMessageId": "UUID",
  "isArchived": "boolean",
  "isDeleted": "boolean",
  "createdAt": "LocalDateTime",
  "updatedAt": "LocalDateTime"
}
```

### MessageResponse
```json
{
  "id": "UUID",
  "chatroomId": "UUID",
  "senderType": "user | bot | system",
  "senderId": "UUID",
  "content": "string",
  "contentType": "text | code | system",
  "sequenceNumber": "number",
  "isEdited": "boolean",
  "isDeleted": "boolean",
  "createdAt": "LocalDateTime"
}
```

### ApiResponse<T>
```json
{
  "success": "boolean",
  "data": "T",
  "message": "string",
  "errorCode": "string",
  "timestamp": "LocalDateTime"
}
```

---

## 🚨 에러 코드

### HTTP 상태 코드
- `200 OK`: 성공
- `201 Created`: 생성 성공
- `400 Bad Request`: 잘못된 요청
- `401 Unauthorized`: 인증 실패
- `403 Forbidden`: 권한 없음
- `404 Not Found`: 리소스 없음
- `500 Internal Server Error`: 서버 오류

### 비즈니스 에러 코드
- `AUTH_TOKEN_INVALID`: 토큰이 유효하지 않음
- `AUTH_TOKEN_EXPIRED`: 토큰이 만료됨
- `INVALID_PASSWORD`: 비밀번호가 틀림
- `USER_NOT_FOUND`: 사용자를 찾을 수 없음
- `EMAIL_ALREADY_EXISTS`: 이메일이 이미 존재함
- `INTERNAL_SERVER_ERROR`: 내부 서버 오류

---

## 🔧 개발 환경 설정

### 1. 서비스 실행
```bash
# Docker Compose로 모든 서비스 실행
cd docker
docker-compose up -d shared-db redis user-service auth-service chat-service api-gateway
```

### 2. 포트 정보
- **API Gateway**: 8080
- **Auth Service**: 8081  
- **User Service**: 8082
- **Chat Service**: 8083
- **PostgreSQL**: 5432
- **Redis**: 6379

### 3. 데이터베이스 스키마
- `user_schema`: 사용자 관련 테이블
- `auth_schema`: 인증 관련 테이블
- `chat_schema`: 채팅 관련 테이블
- `billing`: 빌링 관련 테이블

---

## 📝 사용 예시

### 1. 사용자 등록 및 로그인 플로우
```bash
# 1. 사용자 생성
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "firstName": "Test",
    "lastName": "User",
    "name": "Test User",
    "password": "password123",
    "picture": "https://example.com/profile.jpg",
    "info": "Test user"
  }'

# 2. 로그인
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }'

# 3. 토큰으로 사용자 정보 조회
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer {accessToken}"
```

### 2. 채팅 플로우
```bash
# 1. 채팅방 생성
curl -X POST http://localhost:8080/api/chat/chatrooms \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {accessToken}" \
  -d '{
    "userId": "{userId}",
    "chatbotId": "{chatbotId}",
    "name": "새로운 대화"
  }'

# 2. 메시지 전송
curl -X POST http://localhost:8080/api/chat/chatrooms/{chatroomId}/messages \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {accessToken}" \
  -d '{
    "senderType": "user",
    "content": "안녕하세요!",
    "contentType": "text"
  }'

# 3. 메시지 목록 조회
curl -X GET http://localhost:8080/api/chat/chatrooms/{chatroomId}/messages \
  -H "Authorization: Bearer {accessToken}"
```

---

## 📞 지원

API 사용 중 문제가 발생하면 다음을 확인하세요:

1. **서비스 상태**: 모든 마이크로서비스가 실행 중인지 확인
2. **인증 토큰**: JWT 토큰이 유효하고 만료되지 않았는지 확인
3. **요청 형식**: Content-Type과 요청 본문이 올바른지 확인
4. **권한**: 해당 리소스에 대한 접근 권한이 있는지 확인

---

**문서 버전**: 1.0.0  
**최종 업데이트**: 2024-01-01  
**작성자**: DoranDoran Development Team
