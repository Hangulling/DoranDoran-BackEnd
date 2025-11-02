# 이메일 인증 API 401 에러 수정

## 문제 상황

- **에러**: `POST /api/auth/email/request-verification` 요청 시 401 Unauthorized 에러 발생
- **원인**: Gateway의 JwtAuthFilter가 이메일 인증 엔드포인트를 인증 필요 경로로 처리
- **CORS 에러**: 브라우저에서 CORS 정책 위반 메시지 표시

## 해결 방법

### 1. Gateway 설정 파일 수정

**파일**: `gateway/src/main/resources/application.yml`, `gateway/src/main/resources/application-docker.yml`

**변경 내용**: `gateway.auth.exclusions`에 이메일 인증 관련 엔드포인트 추가

```yaml
gateway:
  auth:
    exclusions:
      - /actuator
      - /
      - /api/auth/login
      - /api/auth/refresh
      - /api/auth/password/reset
      - /api/auth/health
      - /api/auth/email/request-verification  # 추가
      - /api/auth/email/verify               # 추가
      - /api/auth/email/check                # 추가
      - /api/users/health
      - /api/users
      - /api/batch
```

### 2. JwtAuthFilter 수정

**파일**: `gateway/src/main/java/com/dorandoran/gateway/filter/JwtAuthFilter.java`

**변경 내용**: `isExcludedPath()` 메서드에 이메일 인증 경로 추가

```java
private boolean isExcludedPath(String path) {
    return path.startsWith("/actuator") || 
           path.equals("/") ||
           path.startsWith("/api/auth/login") ||
           path.startsWith("/api/auth/refresh") ||
           path.startsWith("/api/auth/password/reset") ||
           path.startsWith("/api/auth/health") ||
           path.startsWith("/api/auth/email/request-verification") ||  // 추가
           path.startsWith("/api/auth/email/verify") ||                 // 추가
           path.startsWith("/api/auth/email/check") ||                 // 추가
           // ... 나머지 경로
}
```

## 배포 필요 사항

1. Gateway 서비스 재빌드
   ```bash
   cd gateway
   ./gradlew clean build
   docker build -t dorandoran-gateway:latest .
   ```

2. AWS EC2에서 Gateway 컨테이너 재시작
   ```bash
   ssh -i "dorandoran-key.pem" ec2-user@3.21.177.186
   docker restart dorandoran-gateway
   ```

## 테스트

인증 없이 다음 엔드포인트에 접근 가능해야 합니다:

- ✅ `POST /api/auth/email/request-verification`
- ✅ `GET /api/auth/email/verify?token=...&email=...`
- ✅ `GET /api/auth/email/check?email=...`

## 참고

- CORS 설정은 이미 `http://localhost:3000`을 허용하고 있어 별도 수정 불필요
- 이메일 인증은 회원가입 전 단계이므로 인증 없이 접근 가능해야 함


