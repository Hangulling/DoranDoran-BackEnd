# 🚀 DoranDoran 개발자 온보딩 메뉴얼

## 📋 목차
1. [프로젝트 개요](#프로젝트-개요)
2. [아키텍처](#아키텍처)
3. [개발 환경 설정](#개발-환경-설정)
4. [서비스별 가이드](#서비스별-가이드)
5. [API 문서](#api-문서)
6. [개발 워크플로우](#개발-워크플로우)
7. [트러블슈팅](#트러블슈팅)
8. [기여 가이드](#기여-가이드)

---

## 🎯 프로젝트 개요

### 프로젝트명
**DoranDoran** - 마이크로서비스 아키텍처 기반의 웹 애플리케이션

### 기술 스택
- **Backend**: Spring Boot 3.3.4, Java 21
- **Database**: PostgreSQL
- **Cache**: Redis
- **Build Tool**: Gradle
- **API Gateway**: Spring Cloud Gateway
- **Security**: Spring Security
- **Testing**: JUnit 5, Mockito

### 주요 기능
- 사용자 관리 (User Service)
- 인증/인가 (Auth Service)
- 채팅 (Chat Service)
- 배치 처리 (Batch Service)
- API Gateway를 통한 통합 관리

---

## 🏗️ 아키텍처

### 마이크로서비스 구성
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

### 서비스별 역할
- **API Gateway**: 모든 외부 요청의 진입점, 라우팅 및 인증
- **Auth Service**: JWT 토큰 기반 인증/인가 처리
- **User Service**: 사용자 정보 관리 및 프로필 관리
- **Chat Service**: 실시간 채팅 기능
- **Batch Service**: 스케줄링된 작업 처리

---

## 🛠️ 개발 환경 설정

### 필수 요구사항
- **Java**: 21 이상
- **Gradle**: 8.0 이상
- **PostgreSQL**: 13 이상
- **Redis**: 6.0 이상
- **Git**: 2.30 이상

### 1. 저장소 클론
```bash
git clone https://github.com/[YOUR_USERNAME]/DoranDoran.git
cd DoranDoran
```

### 2. 데이터베이스 설정
```sql
-- PostgreSQL 데이터베이스 생성
CREATE DATABASE dorandoran_local;
CREATE USER doran WITH PASSWORD 'doran';
GRANT ALL PRIVILEGES ON DATABASE dorandoran_local TO doran;
```

### 3. 환경 변수 설정
```bash
# .env 파일 생성
cp env.sample .env

# .env 파일 편집
DATABASE_URL=jdbc:postgresql://localhost:5432/dorandoran_local
DATABASE_USERNAME=doran
DATABASE_PASSWORD=doran
REDIS_HOST=localhost
REDIS_PORT=6379
```

### 4. 의존성 설치
```bash
./gradlew build
```

---

## 🔧 서비스별 가이드

### User Service (포트: 8082)
**역할**: 사용자 정보 관리, 프로필 관리

**주요 엔드포인트**:
- `GET /api/users/health` - 헬스체크
- `POST /api/users/register` - 사용자 등록
- `GET /api/users/{id}` - 사용자 정보 조회
- `PUT /api/users/{id}` - 사용자 정보 수정

**개발 실행**:
```bash
./gradlew :user:bootRun
```

**테스트 실행**:
```bash
./gradlew :user:test
```

### Auth Service (포트: 8081)
**역할**: JWT 토큰 기반 인증/인가

**주요 엔드포인트**:
- `GET /api/auth/health` - 헬스체크 (Gateway용)
- `GET /api/v1/auth/health` - 헬스체크 (직접 접근)
- `POST /api/v1/auth/login` - 로그인
- `POST /api/v1/auth/register` - 회원가입
- `GET /api/v1/auth/validate` - 토큰 검증

**개발 실행**:
```bash
./gradlew :auth:bootRun
```

### Chat Service (포트: 8083)
**역할**: 실시간 채팅 기능

**개발 실행**:
```bash
./gradlew :chat:bootRun
```

### Batch Service (포트: 8085)
**역할**: 스케줄링된 작업 처리

**개발 실행**:
```bash
./gradlew :batch:bootRun
```

### API Gateway (포트: 8080)
**역할**: 모든 외부 요청의 진입점

**라우팅 규칙**:
- `/api/users/**` → User Service
- `/api/auth/**` → Auth Service
- `/api/chat/**` → Chat Service
- `/api/batch/**` → Batch Service

**개발 실행**:
```bash
./gradlew :gateway:bootRun
```

---

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

**응답**:
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 3600
  }
}
```

#### 토큰 검증
```http
GET /api/v1/auth/validate
Authorization: Bearer {accessToken}
```

### 사용자 API

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

#### 사용자 정보 조회
```http
GET /api/users/{id}
Authorization: Bearer {accessToken}
```

---

## 🔄 개발 워크플로우

### 1. 브랜치 전략
- **main**: 프로덕션 배포용
- **develop**: 개발 통합용
- **feature/기능명**: 새로운 기능 개발
- **hotfix/버그명**: 긴급 버그 수정

### 2. 개발 프로세스
1. **브랜치 생성**: `git checkout -b feature/새기능`
2. **개발 및 테스트**: 로컬에서 개발 및 테스트
3. **커밋**: `git commit -m "feat: 새 기능 추가"`
4. **푸시**: `git push origin feature/새기능`
5. **Pull Request**: GitHub에서 PR 생성
6. **코드 리뷰**: 팀원들의 코드 리뷰
7. **머지**: 승인 후 develop 브랜치로 머지

### 3. 커밋 메시지 규칙
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

---

## 🚨 트러블슈팅

### 자주 발생하는 문제들

#### 1. 포트 충돌 오류
**문제**: `Port 8080 is already in use`
**해결**:
```bash
# Windows
netstat -ano | findstr :8080
taskkill /PID {PID} /F

# Linux/Mac
lsof -ti:8080 | xargs kill -9
```

#### 2. 데이터베이스 연결 오류
**문제**: `Connection refused`
**해결**:
1. PostgreSQL 서비스 확인
2. 데이터베이스 생성 확인
3. 연결 정보 확인

#### 3. Redis 연결 오류
**문제**: `Redis connection failed`
**해결**:
1. Redis 서비스 확인
2. 포트 6379 확인
3. 방화벽 설정 확인

#### 4. 403 Forbidden 오류
**문제**: API 접근 시 403 오류
**해결**:
1. SecurityConfig 설정 확인
2. 엔드포인트 경로 확인
3. 인증 토큰 확인

### 로그 확인 방법
```bash
# 특정 서비스 로그 확인
./gradlew :user:bootRun --info

# 모든 서비스 상태 확인
netstat -an | findstr :808
```

---

## 🤝 기여 가이드

### 코드 스타일
- **Java**: Google Java Style Guide 준수
- **들여쓰기**: 4칸 스페이스
- **네이밍**: camelCase (변수), PascalCase (클래스)
- **주석**: Javadoc 형식 사용

### 테스트 작성
- **단위 테스트**: 모든 서비스 로직에 대해 작성
- **통합 테스트**: API 엔드포인트에 대해 작성
- **테스트 커버리지**: 80% 이상 유지

### 문서화
- **API 문서**: Swagger/OpenAPI 사용
- **코드 주석**: 복잡한 로직에 대한 설명
- **README**: 프로젝트 설정 및 실행 방법

---

## 📞 지원 및 문의

### 개발팀 연락처
- **프로젝트 매니저**: [이름] ([이메일])
- **기술 리드**: [이름] ([이메일])
- **DevOps**: [이름] ([이메일])

### 유용한 링크
- **프로젝트 저장소**: https://github.com/[YOUR_USERNAME]/DoranDoran
- **이슈 트래커**: https://github.com/[YOUR_USERNAME]/DoranDoran/issues
- **위키**: https://github.com/[YOUR_USERNAME]/DoranDoran/wiki

---

## 📝 체크리스트

### 개발 환경 설정 완료 확인
- [ ] Java 21 설치 및 설정
- [ ] PostgreSQL 설치 및 데이터베이스 생성
- [ ] Redis 설치 및 실행
- [ ] 프로젝트 클론 및 빌드
- [ ] 모든 서비스 정상 실행 확인

### 개발 시작 전 확인사항
- [ ] 브랜치 전략 이해
- [ ] 커밋 메시지 규칙 숙지
- [ ] 테스트 작성 방법 학습
- [ ] 코드 리뷰 프로세스 이해

---

**🎉 온보딩을 완료하셨습니다! 이제 DoranDoran 프로젝트의 개발에 참여하실 수 있습니다.**