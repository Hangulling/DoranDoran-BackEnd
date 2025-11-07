# 이메일 인증 API 테스트 가이드

## 재배포 완료 ✅
- **문제**: `LocalDateTime` 직렬화 오류 (Jackson JSR310 모듈 누락)
- **해결**: `ObjectMapper`에 `JavaTimeModule` 등록 완료

## Postman 테스트 방법

### 1. 요청 설정
- **Method**: `POST`
- **URL**: `http://3.21.177.186:8080/api/auth/email/request-verification`
- **Headers**:
  - `Content-Type`: `application/json`
  - `Origin`: `http://localhost:3000` (선택사항)

### 2. Request Body (raw JSON)
```json
{
  "email": "test@example.com"
}
```

### 3. 예상 응답

**성공 (200 OK):**
```json
{
  "success": true,
  "data": "sent",
  "message": "인증 메일이 발송되었습니다."
}
```

**실패 케이스:**
- **400 Bad Request**: 이메일이 이미 사용 중
- **500 Internal Server Error**: 서버 오류 (로깅 확인 필요)

## 테스트 후 확인 사항
1. 서버 로그에서 "이메일 인증 요청 완료" 메시지 확인
2. 이메일 수신 확인
3. Redis에 데이터 저장 확인

