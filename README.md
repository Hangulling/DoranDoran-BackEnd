# 🚀 DoranDoran

> 마이크로서비스 아키텍처 기반의 웹 애플리케이션

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-13+-blue.svg)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-6.0+-red.svg)](https://redis.io/)
[![Gradle](https://img.shields.io/badge/Gradle-8.0+-green.svg)](https://gradle.org/)

## 📋 목차

- [프로젝트 개요](#프로젝트-개요)
- [아키텍처](#아키텍처)
- [빠른 시작](#빠른-시작)
- [서비스 구성](#서비스-구성)
- [API 문서](#api-문서)
- [개발 가이드](#개발-가이드)
- [기여하기](#기여하기)

## 🎯 프로젝트 개요

DoranDoran은 Spring Boot와 마이크로서비스 아키텍처를 기반으로 한 웹 애플리케이션입니다. 사용자 관리, 인증/인가, 실시간 채팅, 배치 처리 등의 기능을 제공합니다.

### 주요 특징

- 🏗️ **마이크로서비스 아키텍처**: 독립적인 서비스들로 구성
- 🔐 **JWT 기반 인증**: 안전한 사용자 인증 시스템
- 🚪 **API Gateway**: 통합된 API 엔드포인트 제공
- 💬 **실시간 채팅**: WebSocket 기반 실시간 통신
- 📊 **모니터링**: Grafana를 통한 시스템 모니터링
- 🐳 **Docker 지원**: 컨테이너화된 배포 환경

## 🏗️ 아키텍처

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   API Gateway   │    │   Auth Service  │    │   User Service  │
│   (Port: 8080)  │◄──►│   (Port: 8081)  │◄──►│   (Port: 8082)  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  Chat Service   │    │  Batch Service  │    │   PostgreSQL    │
│   (Port: 8083)  │    │   (Port: 8085)  │    │   (Port: 5432)  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │
         ▼                       ▼
┌─────────────────┐    ┌─────────────────┐
│     Redis       │    │   Monitoring    │
│   (Port: 6379)  │    │   (Grafana)     │
└─────────────────┘    └─────────────────┘
```

## 🚀 빠른 시작

### 필수 요구사항

- Java 21+
- Gradle 8.0+
- PostgreSQL 13+
- Redis 6.0+

### 1. 저장소 클론

```bash
git clone https://github.com/[YOUR_USERNAME]/DoranDoran.git
cd DoranDoran
```

### 2. 데이터베이스 설정

```sql
CREATE DATABASE dorandoran_local;
CREATE USER doran WITH PASSWORD 'doran';
GRANT ALL PRIVILEGES ON DATABASE dorandoran_local TO doran;
```

### 3. 프로젝트 빌드

```bash
./gradlew build -x test
```

### 4. 서비스 시작

```bash
# 모든 서비스 시작
./gradlew :user:bootRun &
./gradlew :auth:bootRun &
./gradlew :chat:bootRun &
./gradlew :batch:bootRun &
./gradlew :gateway:bootRun &
```

### 5. 서비스 확인

```bash
# API 테스트
curl http://localhost:8080/api/users/health
curl http://localhost:8080/api/auth/health
```

자세한 설정 방법은 [빠른 시작 가이드](QUICK_START_GUIDE.md)를 참조하세요.

## 🔧 서비스 구성

### User Service (포트: 8082)
- 사용자 정보 관리
- 프로필 관리
- 사용자 CRUD 작업

### Auth Service (포트: 8081)
- JWT 토큰 기반 인증
- 사용자 로그인/로그아웃
- 토큰 갱신

### Chat Service (포트: 8083)
- 실시간 채팅
- WebSocket 통신
- 메시지 관리

### Batch Service (포트: 8085)
- 스케줄링된 작업
- 데이터 처리
- 리포트 생성

### API Gateway (포트: 8080)
- 모든 외부 요청의 진입점
- 서비스 라우팅
- 인증/인가 처리

## 📖 API 문서

### 인증 API

#### 로그인
```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

#### 사용자 등록
```http
POST /api/users/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123",
  "name": "홍길동"
}
```

### 헬스체크 API

```bash
# User Service
GET /api/users/health

# Auth Service
GET /api/auth/health

# Chat Service
GET /api/chat/health

# Batch Service
GET /api/batch/health
```

## 🛠️ 개발 가이드

### 개발 환경 설정

1. [개발자 온보딩 메뉴얼](DEVELOPER_ONBOARDING_MANUAL.md) 참조
2. [프로젝트 상태 보고서](PROJECT_STATUS_REPORT.md) 확인
3. [빠른 시작 가이드](QUICK_START_GUIDE.md) 따라하기

### 개발 워크플로우

1. **브랜치 생성**: `git checkout -b feature/새기능`
2. **개발 및 테스트**: 로컬에서 개발 및 테스트
3. **커밋**: `git commit -m "feat: 새 기능 추가"`
4. **푸시**: `git push origin feature/새기능`
5. **Pull Request**: GitHub에서 PR 생성
6. **코드 리뷰**: 팀원들의 코드 리뷰
7. **머지**: 승인 후 develop 브랜치로 머지

### 테스트 실행

```bash
# 전체 테스트
./gradlew test

# 특정 서비스 테스트
./gradlew :user:test
./gradlew :auth:test
```

## 🐳 Docker 지원

### Docker Compose로 실행

```bash
cd docker
docker-compose up -d
```

### 개별 서비스 빌드

```bash
# User Service
docker build -f docker/Dockerfile.user -t dorandoran-user .

# Auth Service
docker build -f docker/Dockerfile.auth -t dorandoran-auth .
```

## 📊 모니터링

### Grafana 대시보드

- **URL**: http://localhost:3000
- **사용자명**: admin
- **비밀번호**: admin

### Prometheus 메트릭

- **URL**: http://localhost:9090

## 🤝 기여하기

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

### 커밋 메시지 규칙

```
type(scope): description

feat: 새로운 기능 추가
fix: 버그 수정
docs: 문서 수정
style: 코드 포맷팅
refactor: 코드 리팩토링
test: 테스트 추가/수정
chore: 빌드 설정 변경
```

## 📝 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다. 자세한 내용은 [LICENSE](LICENSE) 파일을 참조하세요.

## 📞 문의

- **프로젝트 이슈**: [GitHub Issues](https://github.com/[YOUR_USERNAME]/DoranDoran/issues)
- **개발팀**: [이메일]
- **문서**: [위키](https://github.com/[YOUR_USERNAME]/DoranDoran/wiki)

## 🙏 감사의 말

이 프로젝트는 다음 오픈소스 프로젝트들의 도움을 받았습니다:

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Spring Cloud Gateway](https://spring.io/projects/spring-cloud-gateway)
- [PostgreSQL](https://www.postgresql.org/)
- [Redis](https://redis.io/)
- [Gradle](https://gradle.org/)

---

**⭐ 이 프로젝트가 도움이 되었다면 Star를 눌러주세요!**
