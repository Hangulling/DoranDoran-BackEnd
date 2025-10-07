# DoranDoran API 테스트 가이드

이 가이드는 DoranDoran 백엔드 API의 기본 플로우를 테스트하는 방법을 설명합니다.

## 🚀 빠른 시작

### 1. 서비스 실행

먼저 모든 서비스를 실행해야 합니다:

```bash
# PostgreSQL 데이터베이스 실행 (Docker 사용)
docker-compose up -d shared-db

# 또는 개별 서비스 실행
./gradlew :user:bootRun &
./gradlew :auth:bootRun &
./gradlew :chat:bootRun &
```

### 2. API 테스트 실행

#### PowerShell 사용 (Windows)
```powershell
.\test-api-flow.ps1
```

#### Bash 사용 (Linux/Mac)
```bash
chmod +x test-api-flow.sh
./test-api-flow.sh
```

#### 수동 테스트 (curl 사용)

1. **사용자 생성**
```bash
curl -X POST http://localhost:8082/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "firstName": "Test",
    "lastName": "User", 
    "name": "Test User",
    "password": "password123",
    "profileImageUrl": "https://example.com/profile.jpg",
    "bio": "Test user for API testing"
  }'
```

2. **로그인**
```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }'
```

3. **토큰 검증**
```bash
curl -X GET http://localhost:8081/api/auth/validate \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

4. **채팅방 생성**
```bash
curl -X POST http://localhost:8080/api/chat/chatrooms \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "userId": "YOUR_USER_ID",
    "chatbotId": "YOUR_CHATBOT_ID",
    "name": "API 테스트 채팅방"
  }'
```

5. **메시지 전송**
```bash
curl -X POST http://localhost:8080/api/chat/chatrooms/YOUR_CHATROOM_ID/messages \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "content": "안녕하세요! API 테스트 메시지입니다.",
    "senderType": "user",
    "contentType": "text/plain"
  }'
```

## 📋 API 엔드포인트 목록

### User Service (포트 8082)
- `POST /api/users` - 사용자 생성
- `GET /api/users/{userId}` - 사용자 조회
- `GET /api/users/email/{email}` - 이메일로 사용자 조회
- `PUT /api/users/{userId}` - 사용자 정보 업데이트
- `GET /api/users/health` - 헬스체크

### Auth Service (포트 8081)
- `POST /api/auth/login` - 로그인
- `POST /api/auth/logout` - 로그아웃
- `GET /api/auth/validate` - 토큰 검증
- `POST /api/auth/refresh` - 토큰 갱신
- `GET /api/auth/me` - 현재 사용자 정보
- `GET /api/auth/health` - 헬스체크

### Chat Service (포트 8080)
- `POST /api/chat/chatrooms` - 채팅방 생성
- `GET /api/chat/chatrooms` - 채팅방 목록 조회
- `GET /api/chat/chatrooms/{chatroomId}/messages` - 메시지 목록 조회
- `POST /api/chat/chatrooms/{chatroomId}/messages` - 메시지 전송
- `GET /api/chat/stream/{chatroomId}` - SSE 스트림 연결

## 🔧 문제 해결

### 서비스가 시작되지 않는 경우
1. PostgreSQL이 실행 중인지 확인
2. 포트가 사용 중이지 않은지 확인 (8080, 8081, 8082)
3. 데이터베이스 스키마가 생성되었는지 확인

### 인증 오류가 발생하는 경우
1. JWT 토큰이 유효한지 확인
2. Authorization 헤더가 올바르게 설정되었는지 확인
3. 토큰이 만료되지 않았는지 확인

### 채팅 관련 오류가 발생하는 경우
1. 사용자가 존재하는지 확인
2. 채팅방에 접근 권한이 있는지 확인
3. 챗봇 ID가 유효한지 확인

## 📝 테스트 데이터

기본 테스트 사용자:
- 이메일: `test@example.com`
- 비밀번호: `password123`
- 이름: `Test User`

## 🎯 다음 단계

API 테스트가 성공하면:
1. 프론트엔드 애플리케이션과 연동
2. WebSocket을 통한 실시간 채팅 테스트
3. AI 응답 기능 테스트
4. 사용량 및 빌링 기능 테스트
