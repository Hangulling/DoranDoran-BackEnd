# 🚀 DoranDoran MSA 배치 파일 가이드

이 문서는 DoranDoran MSA 프로젝트의 핵심 배치 파일들에 대한 설명을 제공합니다.

## 📋 배치 파일 목록

| 파일명 | 기능 | 사용 시기 | 권한 |
|--------|------|-----------|------|
| `gradlew.bat` | Gradle 래퍼 | 빌드/테스트 시 | 일반 사용자 |
| `start-dev.bat` | 개발 환경 시작 | 개발 시작 시 | 일반 사용자 |
| `stop-dev.bat` | 개발 환경 중지 | 개발 종료 시 | 일반 사용자 |

---

## 🛠️ 핵심 배치 파일

### 1. `gradlew.bat` - Gradle 래퍼

**기능**: Gradle 빌드 도구를 실행합니다.

**주요 명령어**:
```bash
# 전체 빌드
gradlew build

# 테스트 실행
gradlew test

# 캐시 정리
gradlew clean

# 특정 서비스 빌드
gradlew :auth:build
gradlew :user:build
gradlew :chat:build
gradlew :store:build
gradlew :gateway:build
```

---

### 2. `start-dev.bat` - 개발 환경 시작

**기능**: MSA 전체 환경을 한 번에 시작합니다.

**실행 과정**:
1. 기존 컨테이너 정리
2. Gradle 빌드 (테스트 제외)
3. Docker 이미지 빌드
4. 모든 서비스 시작

**사용법**:
```bash
start-dev.bat
```

**시작되는 서비스**:
- API Gateway (8080)
- Auth Service (8081)
- User Service (8082)
- Chat Service (8083)
- Store Service (8084)
- PostgreSQL (5432)
- Redis (6379)
- Prometheus (9090)
- Grafana (3000)

---

### 3. `stop-dev.bat` - 개발 환경 중지

**기능**: 모든 서비스를 안전하게 중지하고 리소스를 정리합니다.

**실행 과정**:
1. MSA 서비스 중지
2. 사용하지 않는 Docker 리소스 정리
3. Gradle 캐시 정리

**사용법**:
```bash
stop-dev.bat
```

---

## 🔧 유용한 명령어

### Docker 명령어

```bash
# 서비스 상태 확인
docker compose -f docker/docker-compose.yml ps

# 로그 확인 (전체)
docker compose -f docker/docker-compose.yml logs -f

# 특정 서비스 로그 확인
docker compose -f docker/docker-compose.yml logs -f auth-service
docker compose -f docker/docker-compose.yml logs -f user-service
docker compose -f docker/docker-compose.yml logs -f chat-service
docker compose -f docker/docker-compose.yml logs -f store-service
docker compose -f docker/docker-compose.yml logs -f api-gateway

# 서비스 재시작
docker compose -f docker/docker-compose.yml restart auth-service

# 컨테이너 내부 접속
docker exec -it dd-auth-service bash
docker exec -it dd-user-service bash
```

### Gradle 명령어

```bash
# 전체 빌드
gradlew build

# 테스트 실행
gradlew test

# 특정 서비스 테스트
gradlew :auth:test
gradlew :user:test

# 캐시 정리
gradlew clean

# 의존성 새로고침
gradlew build --refresh-dependencies

# JAR 파일만 빌드
gradlew bootJar
```

---

## 📊 모니터링 URL

### 개발 환경
- **API Gateway**: http://localhost:8080
- **Auth Service**: http://localhost:8081
- **User Service**: http://localhost:8082
- **Chat Service**: http://localhost:8083
- **Store Service**: http://localhost:8084
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin123)

### 헬스체크 URL
- API Gateway: http://localhost:8080/actuator/health
- Auth Service: http://localhost:8081/actuator/health
- User Service: http://localhost:8082/actuator/health
- Chat Service: http://localhost:8083/actuator/health
- Store Service: http://localhost:8084/actuator/health

---

## 🚨 문제 해결

### 자주 발생하는 문제

1. **포트 충돌**
   ```bash
   netstat -ano | findstr :8080
   taskkill /f /pid {PID}
   ```

2. **Docker 컨테이너 문제**
   ```bash
   docker compose -f docker/docker-compose.yml down
   docker compose -f docker/docker-compose.yml up --build
   ```

3. **데이터베이스 연결 실패**
   ```bash
   docker ps | grep postgres
   docker exec -it dd-shared-db psql -U doran -d dorandoran
   ```

4. **Gradle 빌드 실패**
   ```bash
   gradlew --stop
   gradlew clean
   gradlew build --refresh-dependencies
   ```

5. **Docker 리소스 부족**
   ```bash
   docker system prune -a
   docker volume prune
   ```

---

## 📝 일반적인 개발 워크플로우

### 1. 개발 시작
```bash
# 1. 개발 환경 시작
start-dev.bat

# 2. 코드 수정 후 빌드
gradlew build

# 3. 테스트 실행
gradlew test
```

### 2. 디버깅
```bash
# 1. 서비스 상태 확인
docker compose -f docker/docker-compose.yml ps

# 2. 로그 확인
docker compose -f docker/docker-compose.yml logs -f auth-service

# 3. 컨테이너 내부 접속
docker exec -it dd-auth-service bash
```

### 3. 개발 종료
```bash
# 1. 서비스 중지 및 정리
stop-dev.bat

# 2. 필요시 추가 정리
docker system prune -a
```

---

## 🔄 CI/CD 대체 명령어

### 빌드 및 테스트
```bash
# 전체 빌드 및 테스트
gradlew build

# 특정 서비스만 빌드
gradlew :auth:build
```

### 배포 (Docker)
```bash
# 이미지 빌드
docker compose -f docker/docker-compose.yml build

# 서비스 시작
docker compose -f docker/docker-compose.yml up -d
```

### 정리
```bash
# Docker 리소스 정리
docker system prune -a

# Gradle 캐시 정리
gradlew clean
```

---

## 📞 지원

문제가 발생하거나 추가 기능이 필요한 경우:

1. **로그 확인**: `docker compose -f docker/docker-compose.yml logs -f`
2. **상태 확인**: `docker compose -f docker/docker-compose.yml ps`
3. **정리 실행**: `stop-dev.bat` 실행
4. **개발팀 문의**: 관련 로그와 함께 문의

---

*문서 생성일: 2024년 10월 1일*  
*버전: 2.0*  
*작성자: AI Assistant*