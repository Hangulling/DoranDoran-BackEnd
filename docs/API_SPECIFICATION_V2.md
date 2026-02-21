# DoranDoran API 명세서 V2

## 📋 개요

DoranDoran은 마이크로서비스 아키텍처 기반의 AI 채팅 플랫폼입니다. 이 문서는 최신 컨트롤러 코드를 기반으로 한 모든 REST API 엔드포인트의 상세한 명세를 제공합니다.

### 🏗️ 아키텍처

- **API Gateway**: `http://localhost:8080` (모든 요청의 진입점)
- **Auth Service**: `http://localhost:8081` (JWT 토큰 발급/검증 전담)
- **User Service**: `http://localhost:8082` (사용자 관리)
- **Chat Service**: `http://localhost:8083` (채팅 기능)
- **Database**: PostgreSQL (공유 데이터베이스)
- **Cache**: Redis

### 🔐 MSA 인증 구조

- **Auth Service**에서만 JWT 토큰 발급 및 검증 처리
- **User/Chat 서비스**는 Gateway를 통해 전달받은 토큰의 유효성만 확인
- **Bearer Token** 방식 사용: `Authorization: Bearer {token}`
- 인증이 필요한 API는 Gateway에서 토큰 검증 후 서비스로 전달

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

**Base URL:** `http://localhost:8080/api/users` (Gateway를 통한 접근)  
**Direct URL:** `http://localhost:8082/api/users` (직접 접근)

### 2.1 사용자 생성
```http
POST /api/users
Content-Type: application/json
```

**요청 본문 (CreateUserRequest):**
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

**필드 제약조건:**
- `email`: 필수, 이메일 형식 검증
- `firstName`: 필수, 1-50자
- `lastName`: 필수, 1-50자  
- `name`: 필수, 1-50자
- `password`: 필수, 8-100자
- `picture`: 선택
- `info`: 선택, 최대 100자

**응답 (ApiResponse<UserDto>):**
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
    "preferences": "도란도란",
    "lastConnTime": "2024-01-01T12:00:00",
    "status": "ACTIVE",
    "role": "ROLE_USER",
    "coachCheck": false,
    "createdAt": "2024-01-01T12:00:00",
    "updatedAt": "2024-01-01T12:00:00"
  },
  "message": "사용자가 성공적으로 생성되었습니다.",
  "errorCode": null,
  "timestamp": "2024-01-01T12:00:00"
}
```

### 2.2 사용자 조회 (ID)
```http
GET /api/users/{userId}
```

**경로 변수:**
- `userId` (string): 사용자 UUID

**응답 (UserDto):**
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
  "preferences": "도란도란",
  "lastConnTime": "2024-01-01T12:00:00",
  "status": "ACTIVE",
  "role": "ROLE_USER",
  "coachCheck": false,
  "createdAt": "2024-01-01T12:00:00",
  "updatedAt": "2024-01-01T12:00:00"
}
```

### 2.3 사용자 조회 (이메일)
```http
GET /api/users/email/{email}
```

**경로 변수:**
- `email` (string): 사용자 이메일

**응답:** 2.2와 동일 (UserDto)

### 2.4 사용자 정보 업데이트
```http
PUT /api/users/{userId}
Content-Type: application/json
```

**경로 변수:**
- `userId` (string): 사용자 UUID

**요청 본문 (UpdateUserRequest):**
```json
{
  "email": "newemail@example.com",
  "firstName": "Jane",
  "lastName": "Smith",
  "name": "Jane Smith",
  "picture": "https://example.com/new-profile.jpg",
  "info": "업데이트된 소개",
  "status": "ACTIVE",
  "coachCheck": true
}
```

**필드 제약조건:**
- `email`: 선택, 이메일 형식 검증
- `firstName`: 선택, 1-50자
- `lastName`: 선택, 1-50자
- `name`: 선택, 1-50자
- `picture`: 선택
- `info`: 선택, 최대 100자
- `status`: 선택, UserStatus enum (ACTIVE, INACTIVE, SUSPENDED)
- `coachCheck`: 선택, Boolean

**응답:** 2.2와 동일 (UserDto)

### 2.5 사용자 상태 업데이트
```http
PATCH /api/users/{userId}/status?status={status}
```

**경로 변수:**
- `userId` (string): 사용자 UUID

**쿼리 파라미터:**
- `status` (string): ACTIVE, INACTIVE, SUSPENDED

**응답:** 2.2와 동일 (UserDto)

### 2.6 비밀번호 재설정
```http
POST /api/users/password/reset
Content-Type: application/json
```

**요청 본문 (ResetPasswordRequest):**
```json
{
  "email": "user@example.com",
  "newPassword": "newpassword123"
}
```

**필드 제약조건:**
- `email`: 필수
- `newPassword`: 필수

**응답:**
```http
200 OK
```

### 2.7 사용자 비밀번호 업데이트
```http
PUT /api/users/{userId}/password
Content-Type: application/json
```

**경로 변수:**
- `userId` (string): 사용자 UUID

**요청 본문:**
```json
"newpassword123"
```

**응답:**
```http
200 OK
```

### 2.8 회원탈퇴 (소프트 삭제)
```http
DELETE /api/users/{userId}
```

**경로 변수:**
- `userId` (string): 사용자 UUID

**응답:**
```http
200 OK
```

### 2.9 헬스체크
```http
GET /api/users/health
```

**응답:**
```http
200 OK
User service is running
```

---

## 3. 🔐 Auth Service API

**Base URL:** `http://localhost:8080/api/auth` (Gateway를 통한 접근)  
**Direct URL:** `http://localhost:8081/api/auth` (직접 접근)

### 3.1 로그인
```http
POST /api/auth/login
Content-Type: application/json
```

**요청 본문 (LoginRequest):**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**필드 제약조건:**
- `email`: 필수, 이메일 형식 검증
- `password`: 필수

**응답 (ApiResponse<LoginResponse>):**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "id": "123e4567-e89b-12d3-a456-426614174000",
      "email": "user@example.com",
      "firstName": "John",
      "lastName": "Doe",
      "name": "John Doe",
      "passwordHash": "$2a$10$...",
      "picture": "https://example.com/profile.jpg",
      "info": "사용자 소개",
      "preferences": "도란도란",
      "lastConnTime": "2024-01-01T12:00:00",
      "status": "ACTIVE",
      "role": "ROLE_USER",
      "coachCheck": false,
      "createdAt": "2024-01-01T12:00:00",
      "updatedAt": "2024-01-01T12:00:00"
    }
  },
  "message": "로그인에 성공했습니다.",
  "errorCode": null,
  "timestamp": "2024-01-01T12:00:00"
}
```

### 3.2 로그아웃
```http
POST /api/auth/logout
Authorization: Bearer {token}
```

**헤더:**
- `Authorization`: Bearer JWT 토큰

**응답 (ApiResponse<Void>):**
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
Authorization: Bearer {token}
```

**헤더:**
- `Authorization`: Bearer JWT 토큰

**응답 (ApiResponse<UserDto>):**
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
    "preferences": "도란도란",
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

**요청 본문 (RefreshTokenRequest):**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**필드 제약조건:**
- `refreshToken`: 필수

**응답:** 3.1과 동일 (ApiResponse<LoginResponse>)

### 3.5 비밀번호 재설정 요청
```http
POST /api/auth/password/reset/request?email={email}
```

**쿼리 파라미터:**
- `email` (string): 사용자 이메일

**응답 (ApiResponse<String>):**
```json
{
  "success": true,
  "data": "reset-token-string",
  "message": "비밀번호 재설정 토큰이 생성되었습니다. 토큰: reset-token-string",
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

**응답 (ApiResponse<Void>):**
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
Authorization: Bearer {token}
```

**헤더:**
- `Authorization`: Bearer JWT 토큰

**응답 (ApiResponse<UserDto>):**
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
    "preferences": "도란도란",
    "lastConnTime": "2024-01-01T12:00:00",
    "status": "ACTIVE",
    "role": "ROLE_USER",
    "coachCheck": false,
    "createdAt": "2024-01-01T12:00:00",
    "updatedAt": "2024-01-01T12:00:00"
  },
  "message": "사용자 정보를 성공적으로 조회했습니다.",
  "errorCode": null,
  "timestamp": "2024-01-01T12:00:00"
}
```

### 3.8 헬스체크
```http
GET /api/auth/health
```

**응답:**
```http
200 OK
Auth service is running
```

---

## 4. 💬 Chat Service API

**Base URL:** `http://localhost:8080/api/chat` (Gateway를 통한 접근)  
**Direct URL:** `http://localhost:8083/api/chat` (직접 접근)

### 4.1 채팅방 생성/조회
```http
POST /api/chat/chatrooms
Authorization: Bearer {token}
Content-Type: application/json
```

**헤더:**
- `Authorization`: Bearer JWT 토큰 (인증 필요)

**요청 본문 (ChatRoomCreateRequest):**
```json
{
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "chatbotId": "456e7890-e89b-12d3-a456-426614174001",
  "name": "새로운 채팅방",
  "concept": "FRIEND",
  "intimacyLevel": 2
}
```

**필드 제약조건:**
- `userId`: 필수, UUID
- `chatbotId`: 필수, UUID
- `name`: 필수, 1-100자 (기본값: "대화")
- `concept`: 선택, FRIEND|HONEY|COWORKER|SENIOR (기본값: "FRIEND")
- `intimacyLevel`: 선택, 1-3 (기본값: 2)

**응답 (ChatRoomResponse):**
```json
{
  "id": "789e0123-e89b-12d3-a456-426614174002",
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "chatbotId": "456e7890-e89b-12d3-a456-426614174001",
  "name": "새로운 채팅방",
  "description": null,
  "lastMessageAt": null,
  "lastMessageId": null,
  "isArchived": false,
  "isDeleted": false,
  "createdAt": "2024-01-01T12:00:00",
  "updatedAt": "2024-01-01T12:00:00",
  "concept": "FRIEND",
  "intimacyLevel": 2
}
```

### 4.2 채팅방 목록 조회
```http
GET /api/chat/chatrooms?userId={userId}&page={page}&size={size}
Authorization: Bearer {token}
```

**헤더:**
- `Authorization`: Bearer JWT 토큰 (인증 필요)

**쿼리 파라미터:**
- `userId` (UUID, 선택): 사용자 ID
- `page` (int, 기본값: 0): 페이지 번호
- `size` (int, 기본값: 20): 페이지 크기

**응답 (Page<ChatRoomResponse>):**
```json
{
  "content": [
    {
      "id": "789e0123-e89b-12d3-a456-426614174002",
      "userId": "123e4567-e89b-12d3-a456-426614174000",
      "chatbotId": "456e7890-e89b-12d3-a456-426614174001",
      "name": "새로운 채팅방",
      "description": null,
      "lastMessageAt": "2024-01-01T12:00:00",
      "lastMessageId": "abc12345-e89b-12d3-a456-426614174003",
      "isArchived": false,
      "isDeleted": false,
      "createdAt": "2024-01-01T12:00:00",
      "updatedAt": "2024-01-01T12:00:00",
      "concept": "FRIEND",
      "intimacyLevel": 2
    }
  ],
  "pageable": {
    "sort": {
      "sorted": false,
      "unsorted": true
    },
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 1,
  "totalPages": 1,
  "last": true,
  "first": true,
  "numberOfElements": 1
}
```

### 4.3 채팅방 단건 조회
```http
GET /api/chat/chatrooms/{chatroomId}?userId={userId}
Authorization: Bearer {token}
```

**헤더:**
- `Authorization`: Bearer JWT 토큰 (인증 필요)

**경로 변수:**
- `chatroomId` (UUID): 채팅방 ID

**쿼리 파라미터:**
- `userId` (UUID, 선택): 사용자 ID

**응답:** 4.1과 동일 (ChatRoomResponse)

### 4.4 채팅방 목록 (최대 4개)
```http
GET /api/chat/chatrooms/all?userId={userId}
Authorization: Bearer {token}
```

**헤더:**
- `Authorization`: Bearer JWT 토큰 (인증 필요)

**쿼리 파라미터:**
- `userId` (UUID, 선택): 사용자 ID

**응답 (List<ChatRoomResponse>):**
```json
[
  {
    "id": "789e0123-e89b-12d3-a456-426614174002",
    "userId": "123e4567-e89b-12d3-a456-426614174000",
    "chatbotId": "456e7890-e89b-12d3-a456-426614174001",
    "name": "새로운 채팅방",
    "description": null,
    "lastMessageAt": "2024-01-01T12:00:00",
    "lastMessageId": "abc12345-e89b-12d3-a456-426614174003",
    "isArchived": false,
    "isDeleted": false,
    "createdAt": "2024-01-01T12:00:00",
    "updatedAt": "2024-01-01T12:00:00",
    "concept": "FRIEND",
    "intimacyLevel": 2
  }
]
```

### 4.5 메시지 목록 조회
```http
GET /api/chat/chatrooms/{chatroomId}/messages?userId={userId}&page={page}&size={size}
Authorization: Bearer {token}
```

**헤더:**
- `Authorization`: Bearer JWT 토큰 (인증 필요)

**경로 변수:**
- `chatroomId` (UUID): 채팅방 ID

**쿼리 파라미터:**
- `userId` (UUID, 선택): 사용자 ID
- `page` (int, 기본값: 0): 페이지 번호
- `size` (int, 기본값: 50): 페이지 크기

**응답 (Page<MessageResponse>):**
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
    }
  ],
  "pageable": {
    "sort": {
      "sorted": false,
      "unsorted": true
    },
    "pageNumber": 0,
    "pageSize": 50
  },
  "totalElements": 1,
  "totalPages": 1,
  "last": true,
  "first": true,
  "numberOfElements": 1
}
```

### 4.6 메시지 전송
```http
POST /api/chat/chatrooms/{chatroomId}/messages
Authorization: Bearer {token}
Content-Type: application/json
```

**헤더:**
- `Authorization`: Bearer JWT 토큰 (인증 필요)

**경로 변수:**
- `chatroomId` (UUID): 채팅방 ID

**요청 본문 (MessageSendRequest):**
```json
{
  "content": "안녕하세요!",
  "contentType": "text",
  "senderType": "user"
}
```

**필드 제약조건:**
- `content`: 필수, 1-10000자
- `contentType`: 선택, text|code|system (기본값: "text")
- `senderType`: 선택, user|bot|system (기본값: "user", API에서는 user만 허용)

**응답 (MessageResponse):**
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

### 4.7 실시간 메시지 스트림 (SSE)
```http
GET /api/chat/stream/{chatroomId}?userId={userId}
Authorization: Bearer {token}
Accept: text/event-stream
```

**헤더:**
- `Authorization`: Bearer JWT 토큰 (인증 필요)
- `Accept`: text/event-stream

**경로 변수:**
- `chatroomId` (UUID): 채팅방 ID

**쿼리 파라미터:**
- `userId` (UUID, 선택): 사용자 ID

**응답:** Server-Sent Events 스트림 (SseEmitter)

### 4.8 친밀도 레벨 변경
```http
PATCH /api/chat/chatrooms/{chatroomId}/intimacy
Authorization: Bearer {token}
Content-Type: application/json
```

**헤더:**
- `Authorization`: Bearer JWT 토큰 (인증 필요)

**경로 변수:**
- `chatroomId` (UUID): 채팅방 ID

**요청 본문 (IntimacyUpdateRequest):**
```json
{
  "intimacyLevel": 3
}
```

**필드 제약조건:**
- `intimacyLevel`: 필수, 1-3

**응답:**
```json
{
  "success": true,
  "message": "친밀도가 변경되었습니다",
  "intimacyLevel": 3
}
```

### 4.9 채팅방 나가기 (소프트 삭제)
```http
POST /api/chat/chatrooms/{chatroomId}/leave?userId={userId}
Authorization: Bearer {token}
```

**헤더:**
- `Authorization`: Bearer JWT 토큰 (인증 필요)

**경로 변수:**
- `chatroomId` (UUID): 채팅방 ID

**쿼리 파라미터:**
- `userId` (UUID, 선택): 사용자 ID

**응답:**
```http
204 No Content
```

### 4.10 이메일로 사용자 조회
```http
GET /api/chat/users/by-email?email={email}
```

**쿼리 파라미터:**
- `email` (string): 사용자 이메일

**응답:**
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "email": "user@example.com",
  "name": "John Doe",
  "first_name": "John",
  "last_name": "Doe"
}
```

### 4.11 챗봇 조회
```http
GET /api/chat/chatbots/{chatbotId}
```

**경로 변수:**
- `chatbotId` (string): 챗봇 ID

**응답:**
```json
{
  "success": true,
  "chatbot": {
    "id": "456e7890-e89b-12d3-a456-426614174001",
    "name": "도란도란",
    "displayName": "도란도란 AI",
    "description": "친근한 AI 어시스턴트",
    "systemPrompt": "당신은 친근한 AI 어시스턴트입니다...",
    "intimacySystemPrompt": "친밀도에 따른 시스템 프롬프트...",
    "intimacyUserPrompt": "친밀도에 따른 사용자 프롬프트...",
    "vocabularySystemPrompt": "어휘력 시스템 프롬프트...",
    "vocabularyUserPrompt": "어휘력 사용자 프롬프트...",
    "translationSystemPrompt": "번역 시스템 프롬프트...",
    "translationUserPrompt": "번역 사용자 프롬프트...",
    "intimacyLevel": 2,
    "isActive": true
  }
}
```

### 4.12 챗봇 프롬프트 조회
```http
GET /api/chat/chatbots/prompt?chatbotId={chatbotId}&agentType={agentType}
```

**쿼리 파라미터:**
- `chatbotId` (string): 챗봇 ID
- `agentType` (string): 에이전트 타입

**응답:**
```json
{
  "success": true,
  "chatbotId": "456e7890-e89b-12d3-a456-426614174001",
  "agentType": "intimacy",
  "prompt": "친밀도 에이전트 프롬프트..."
}
```

### 4.13 챗봇 프롬프트 수정
```http
POST /api/chat/chatbots/prompt
Content-Type: application/json
```

**요청 본문 (ChatbotUpdateRequest):**
```json
{
  "chatbotId": "456e7890-e89b-12d3-a456-426614174001",
  "agentType": "intimacy",
  "prompt": "수정된 프롬프트..."
}
```

**응답:**
```json
{
  "success": true,
  "message": "챗봇 프롬프트가 성공적으로 업데이트되었습니다.",
  "chatbotId": "456e7890-e89b-12d3-a456-426614174001",
  "agentType": "intimacy"
}
```

### 4.14 챗봇 프롬프트 리셋
```http
POST /api/chat/chatbots/reset?chatbotId={chatbotId}&agentType={agentType}
```

**쿼리 파라미터:**
- `chatbotId` (string): 챗봇 ID
- `agentType` (string): 에이전트 타입

**응답:**
```json
{
  "success": true,
  "message": "챗봇 프롬프트가 기본값으로 리셋되었습니다.",
  "chatbotId": "456e7890-e89b-12d3-a456-426614174001",
  "agentType": "intimacy"
}
```

### 4.15 전체 프롬프트 조회
```http
GET /api/chat/chatbots/prompt/full?chatbotId={chatbotId}&agentType={agentType}&chatroomId={chatroomId}
```

**쿼리 파라미터:**
- `chatbotId` (string): 챗봇 ID
- `agentType` (string): 에이전트 타입
- `chatroomId` (string, 선택): 채팅방 ID

**응답:**
```json
{
  "success": true,
  "type": "full",
  "fullPrompt": "Base Prompt + Dynamic Directives...",
  "basePrompt": "Base Prompt...",
  "message": "Concept와 Intimacy Level이 반영된 전체 프롬프트입니다."
}
```

### 4.16 Agent 프롬프트 조회
```http
GET /api/chat/chatbots/{chatbotId}/agents/{agentType}
```

**경로 변수:**
- `chatbotId` (string): 챗봇 ID
- `agentType` (string): 에이전트 타입

**응답:**
```json
{
  "success": true,
  "chatbotId": "456e7890-e89b-12d3-a456-426614174001",
  "agentType": "intimacy",
  "prompts": {
    "system": "시스템 프롬프트...",
    "user": "사용자 프롬프트..."
  }
}
```

### 4.17 챗봇 Dynamic Directives 설정
```http
POST /api/chat/chatbots/{chatbotId}/directives
Content-Type: application/json
```

**경로 변수:**
- `chatbotId` (string): 챗봇 ID

**요청 본문 (ChatbotDirectivesRequest):**
```json
{
  "concept": "FRIEND",
  "intimacyLevel": 2,
  "customDirectives": "사용자 정의 지시사항..."
}
```

**응답:**
```json
{
  "success": true,
  "message": "Directives 설정이 업데이트되었습니다.",
  "chatbotId": "456e7890-e89b-12d3-a456-426614174001"
}
```

### 4.18 챗봇 Dynamic Directives 조회
```http
GET /api/chat/chatbots/{chatbotId}/directives
```

**경로 변수:**
- `chatbotId` (string): 챗봇 ID

**응답:**
```json
{
  "success": true,
  "directives": {
    "concept": "FRIEND",
    "intimacyLevel": 2,
    "customDirectives": "사용자 정의 지시사항..."
  }
}
```

---

## 5. 🏠 Home/Support/Notification 추가 API

### 5.1 메인홈 게시글 목록
```http
GET /api/home/posts
```

**응답 예시:**
```json
[
  {
    "externalId": "178437...",
    "title": "게시글 제목",
    "imageUrl": "https://...",
    "description": "게시글 설명",
    "permalink": "https://instagram.com/p/...",
    "publishedAt": "2026-01-19T10:00:00"
  }
]
```

### 5.2 메인홈 게시글 상세
```http
GET /api/home/posts/{externalId}
```

### 5.3 문의/신고 등록
```http
POST /api/support
```

**요청 본문:**
```json
{
  "type": "INQUIRY",
  "category": "APP",
  "content": "문의 내용",
  "replyRequested": true,
  "replyEmail": "reply@example.com",
  "chatroomId": "uuid",
  "messageId": "uuid",
  "messageContent": "신고 대상 메시지",
  "aiResponseSnapshot": { "content": "AI 응답 요약" }
}
```

**비고:**
- `type=REPORT`인 경우 `messageId`, `messageContent` 필수
- 로그인 토큰의 이메일을 서버가 저장 (Gateway가 `X-User-Email` 전달)

### 5.4 FCM 토큰 등록
```http
POST /api/notifications/register
```

**요청 본문:**
```json
{
  "token": "fcm-token",
  "platform": "ios"
}
```

### 5.5 푸시 발송(내부용)
```http
POST /api/notifications/send
```

**요청 본문:**
```json
{
  "userId": "uuid",
  "title": "채팅방",
  "body": "메시지",
  "chatroomId": "uuid",
  "messageId": "uuid"
}
```

**FCM data payload (서버 전송):**
```json
{
  "deeplink": "dorandoran://chat?roomId=...&messageId=...",
  "universalLink": "https://www.doran-chat.com/chat?roomId=...&messageId=...",
  "chatroomId": "...",
  "messageId": "...",
  "sentAt": "2026-01-19T10:00:00+09:00"
}
```

**발송 정책:**
- 사용자 알림 설정 ON
- 하루에 **채팅방당 1회**

### 5.6 푸시 발송 로그 조회
```http
GET /api/notifications/logs?userId={userId}&chatroomId={chatroomId}&sentDate=YYYY-MM-DD&limit=100
```

---

## 6. 👤 User 설정/통계 추가 API

### 6.1 관심 주제 조회/변경
```http
GET /api/users/{userId}/interests
PUT /api/users/{userId}/interests
```

**변경 요청 본문:**
```json
{
  "topicKeys": ["friendship", "travel"]
}
```

### 6.2 알림 설정 조회/변경
```http
GET /api/users/{userId}/notifications
PUT /api/users/{userId}/notifications
```

**변경 요청 본문:**
```json
{
  "pushEnabled": true
}
```

### 6.3 사용자 통계 조회/퍼펙트 증가
```http
GET /api/users/{userId}/stats
POST /api/users/{userId}/stats/perfect
```

### 6.4 사용자 이메일 조회
```http
GET /api/users/{userId}/email
```

---

## 7. 💬 Chat 추가 API

### 7.1 메시지 전송 취소
```http
POST /api/chat/messages/{messageId}/cancel
POST /api/chat/chatrooms/{chatroomId}/messages/{messageId}/cancel
```

**비고:**
- SSE 연결이 끊긴 경우 서버가 처리 중단

---

## 8. 🔧 공통 응답 형식

### Auth Service 응답 형식 (ApiResponse<T>)
```json
{
  "success": true,
  "data": { /* 실제 데이터 */ },
  "message": "요청이 성공적으로 처리되었습니다.",
  "errorCode": null,
  "timestamp": "2024-01-01T12:00:00"
}
```

### User Service 응답 형식
- **일부 엔드포인트**: ApiResponse<UserDto> 래퍼 사용
- **대부분 엔드포인트**: UserDto 직접 반환

### Chat Service 응답 형식
- **대부분 엔드포인트**: DTO 직접 반환 (ChatRoomResponse, MessageResponse 등)
- **일부 엔드포인트**: Map<String, Object> 형태

### 에러 응답 (Auth Service)
```json
{
  "success": false,
  "data": null,
  "message": "오류 메시지",
  "errorCode": "ERROR_CODE",
  "timestamp": "2024-01-01T12:00:00"
}
```

### 에러 응답 (User/Chat Service)
```http
400 Bad Request
404 Not Found
403 Forbidden
500 Internal Server Error
```

---

## 9. 📊 HTTP 상태 코드

| 코드 | 의미 | 설명 |
|------|------|------|
| 200 | OK | 요청 성공 |
| 201 | Created | 리소스 생성 성공 |
| 204 | No Content | 성공하지만 반환할 내용 없음 |
| 400 | Bad Request | 잘못된 요청 |
| 401 | Unauthorized | 인증 실패 |
| 403 | Forbidden | 권한 없음 |
| 404 | Not Found | 리소스 없음 |
| 500 | Internal Server Error | 서버 오류 |

---

## 10. 🔒 보안 고려사항

### MSA 인증 구조
- **Auth Service (8081)**: JWT 토큰 발급/검증 전담
- **Gateway (8080)**: 모든 요청의 진입점, 토큰 검증 후 서비스로 라우팅
- **User Service (8082)**: 토큰 유효성만 확인 (Gateway를 통해)
- **Chat Service (8083)**: 토큰 유효성만 확인 (Gateway를 통해)

### 인증이 필요한 엔드포인트
- **Auth Service**: `/logout`, `/validate`, `/me` (Bearer Token 필요)
- **User Service**: 대부분 인증 불필요 (공개 API)
- **Chat Service**: 거의 모든 API (Bearer Token 필요)

### CORS 설정
- Gateway에서 글로벌 CORS 설정
- 모든 Origin 허용 (`*`)
- 모든 HTTP 메서드 허용
- Credentials 허용

### Rate Limiting (Gateway)
- **Auth Service**: 10 req/s, burst 20
- **User Service**: 20 req/s, burst 40  
- **Chat Service**: 30 req/s, burst 60
- **Batch Service**: 5 req/s, burst 10

---

## 8. 🧪 테스트 방법

### 1. Swagger UI 사용
각 서비스별로 Swagger UI가 제공됩니다:
- **Gateway**: `http://localhost:8080/swagger-ui.html` (모든 API 통합)
- **Auth Service**: `http://localhost:8081/swagger-ui.html` (직접 접근)
- **User Service**: `http://localhost:8082/swagger-ui.html` (직접 접근)
- **Chat Service**: `http://localhost:8083/swagger-ui.html` (직접 접근)

### 2. cURL 예제
```bash
# 1. 로그인 (Auth Service)
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com", "password": "password123"}'

# 2. 사용자 생성 (User Service)
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "name": "John Doe",
    "password": "password123",
    "picture": "https://example.com/profile.jpg",
    "info": "사용자 소개"
  }'

# 3. 사용자 조회 (User Service)
curl -X GET http://localhost:8080/api/users/123e4567-e89b-12d3-a456-426614174000

# 4. 채팅방 생성 (Chat Service)
curl -X POST http://localhost:8080/api/chat/chatrooms \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "123e4567-e89b-12d3-a456-426614174000",
    "chatbotId": "456e7890-e89b-12d3-a456-426614174001",
    "name": "새로운 채팅방",
    "concept": "FRIEND",
    "intimacyLevel": 2
  }'

# 5. 메시지 전송 (Chat Service)
curl -X POST http://localhost:8080/api/chat/chatrooms/789e0123-e89b-12d3-a456-426614174002/messages \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"content": "안녕하세요!", "contentType": "text", "senderType": "user"}'

# 6. 토큰 검증 (Auth Service)
curl -X GET http://localhost:8080/api/auth/validate \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

## 9. 📝 변경 이력

| 버전 | 날짜 | 변경사항 |
|------|------|----------|
| 2.0.0 | 2024-01-01 | 최신 컨트롤러 코드 기반 완전 재작성 |
| 2.0.1 | 2024-01-01 | MSA 인증 구조 명확화, 응답 형식 차이점 구분 |

---

## 10. 📞 문의

- **개발팀**: [이메일]
- **이슈 리포트**: GitHub Issues
- **문서**: 프로젝트 Wiki

---

**⭐ 이 API 명세서가 도움이 되었다면 Star를 눌러주세요!**
