# ⚡ DoranDoran 빠른 시작 가이드

## 🚀 5분 만에 시작하기

### 1. 필수 요구사항 확인
```bash
# Java 버전 확인
java -version

# Gradle 버전 확인
./gradlew --version

# PostgreSQL 확인
psql --version

# Redis 확인
redis-cli --version
```

### 2. 프로젝트 클론 및 빌드
```bash
# 저장소 클론
git clone https://github.com/[YOUR_USERNAME]/DoranDoran.git
cd DoranDoran

# 의존성 설치
./gradlew build -x test
```

### 3. 데이터베이스 설정
```sql
-- PostgreSQL에 접속
psql -U postgres

-- 데이터베이스 생성
CREATE DATABASE dorandoran_local;
CREATE USER doran WITH PASSWORD 'doran';
GRANT ALL PRIVILEGES ON DATABASE dorandoran_local TO doran;
\q
```

### 4. Redis 시작
```bash
# Windows
redis-server

# Linux/Mac
sudo systemctl start redis
# 또는
redis-server
```

### 5. 모든 서비스 시작
```bash
# 터미널 1: User Service
./gradlew :user:bootRun

# 터미널 2: Auth Service
./gradlew :auth:bootRun

# 터미널 3: Chat Service
./gradlew :chat:bootRun

# 터미널 4: Batch Service
./gradlew :batch:bootRun

# 터미널 5: API Gateway
./gradlew :gateway:bootRun
```

### 6. 서비스 상태 확인
```bash
# 포트 확인
netstat -an | findstr :808

# API 테스트
curl http://localhost:8080/api/users/health
curl http://localhost:8080/api/auth/health
```

---

## 🔧 문제 해결

### 포트 충돌
```bash
# Windows
netstat -ano | findstr :8080
taskkill /PID {PID} /F

# Linux/Mac
lsof -ti:8080 | xargs kill -9
```

### 데이터베이스 연결 오류
1. PostgreSQL 서비스 확인
2. 데이터베이스 생성 확인
3. 연결 정보 확인

### Redis 연결 오류
1. Redis 서비스 확인
2. 포트 6379 확인

---

## 📱 API 테스트

### 사용자 등록
```bash
curl -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123",
    "name": "홍길동"
  }'
```

### 로그인
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }'
```

### 헬스체크
```bash
# User Service
curl http://localhost:8080/api/users/health

# Auth Service
curl http://localhost:8080/api/auth/health

# Chat Service
curl http://localhost:8080/api/chat/health

# Batch Service
curl http://localhost:8080/api/batch/health
```

---

## 🎯 다음 단계

1. **개발자 온보딩 메뉴얼** 읽기
2. **프로젝트 상태 보고서** 확인
3. **API 문서** 참조
4. **개발 워크플로우** 학습

---

**🎉 축하합니다! DoranDoran 프로젝트가 성공적으로 실행되었습니다!**
