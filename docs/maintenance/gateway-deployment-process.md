# Gateway 서비스 빌드 및 배포 프로세스

## 개요

Gateway 서비스는 Spring Cloud Gateway 기반의 API Gateway로, IP 블랙리스트 필터링 기능을 포함하고 있습니다. 이 문서는 Gateway 서비스를 빌드하고 EC2 서버에 배포하는 전체 프로세스를 설명합니다.

---

## 배포 방식

현재 프로젝트에는 두 가지 배포 방식이 있습니다:

### 1. 자동화 배포 스크립트 (권장)

**파일**: `scripts/deploy/deploy-gateway-service.ps1`

이 스크립트는 다음 단계를 자동화합니다:
1. 로컬에서 Gradle 빌드
2. Docker 이미지 생성 및 태깅
3. 이미지 압축 및 EC2 전송
4. EC2에서 이미지 로드 및 컨테이너 재시작
5. 배포 검증 (CORS 헤더 확인)

**사용법**:
```powershell
.\scripts\deploy\deploy-gateway-service.ps1
```

### 2. 수동 배포 (최근 사용 방식)

최근 IP 블랙리스트 업데이트 시 사용한 방식입니다.

#### 단계별 프로세스

##### 1단계: 블랙리스트 IP 추가
- **파일**: 
  - `gateway/src/main/resources/application.yml`
  - `gateway/src/main/resources/application-docker.yml`
- **작업**: 의심 IP를 `gateway.security.blacklist.ips` 속성에 추가

##### 2단계: 로컬 빌드
```powershell
# 프로젝트 루트에서 실행
cd gateway
.\gradlew.bat clean build -x test

# 또는 루트에서
.\gradlew.bat :gateway:clean :gateway:build -x test
```

**빌드 산출물**:
- `gateway/build/libs/gateway-0.1.0-SNAPSHOT.jar` (약 56MB)

##### 3단계: EC2 서버로 JAR 파일 전송
```powershell
scp -i "$env:USERPROFILE\Downloads\dorandoran-key.pem" `
    gateway/build/libs/gateway-0.1.0-SNAPSHOT.jar `
    ec2-user@3.21.177.186:/tmp/gateway.jar
```

##### 4단계: EC2에서 Docker 이미지 빌드 및 배포

**방법 A: Dockerfile을 사용한 빌드** (이론적, 현재 문제 있음)
```bash
# EC2 서버에서 실행
cd /tmp
docker build -f Dockerfile.gateway -t dorandoran-gateway:latest .
```

**주의**: EC2의 `/tmp/Dockerfile.gateway`는 `openjdk:17-jdk-slim` 이미지를 사용하는데, 이 이미지가 Docker Hub에서 더 이상 제공되지 않아 빌드가 실패합니다.

**방법 B: 직접 실행** (최근 사용 방식 - 권장)
```bash
# EC2 서버에서 실행
# 1. 기존 컨테이너 중지 및 제거
docker stop dorandoran-gateway
docker rm dorandoran-gateway

# 2. 새 컨테이너 실행 (기존 이미지 사용)
docker run -d \
  --name dorandoran-gateway \
  --network dorandoran-network \
  -p 8080:8080 \
  --restart=unless-stopped \
  -e SPRING_PROFILES_ACTIVE=docker \
  -e SPRING_REDIS_HOST='dorandoran-redis' \
  -e SPRING_REDIS_PORT='6379' \
  --entrypoint java \
  dorandoran-gateway:latest \
  -jar app.jar \
  --spring.profiles.active=docker
```

**참고**: 최근 배포에서는 `openjdk:17-jdk-slim` 이미지가 없어서 Dockerfile 빌드가 실패했고, 대신 기존 이미지를 사용하여 직접 실행하는 방식을 사용했습니다.

##### 5단계: 배포 검증
```bash
# 컨테이너 상태 확인
docker ps | grep dorandoran-gateway

# 로그 확인
docker logs dorandoran-gateway --tail 50

# 블랙리스트 초기화 확인
docker logs dorandoran-gateway | grep "IP 블랙리스트에 추가됨"

# 서비스 시작 확인
docker logs dorandoran-gateway | grep "Started GatewayApplication"
```

---

## Docker 이미지 구조

### 로컬 Dockerfile

**파일**: `gateway/Dockerfile`

```dockerfile
# Multi-stage build
# 1. 빌더 스테이지: Gradle + JDK 21
FROM gradle:8.5-jdk21 AS build
WORKDIR /workspace
COPY settings.gradle build.gradle ./
COPY common ./common
COPY shared ./shared
COPY gateway ./gateway
RUN --mount=type=cache,target=/home/gradle/.gradle \
    gradle :gateway:bootJar --no-daemon

# 2. 런타임 스테이지: 경량 JRE
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/gateway/build/libs/*.jar ./app.jar
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1
ENTRYPOINT ["java","-jar","/app/app.jar"]
```

### EC2 서버의 Dockerfile (실제)

EC2 서버의 `/tmp/Dockerfile.gateway` 내용:

```dockerfile
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY gateway.jar app.jar
ENTRYPOINT [" java, -jar, app.jar, --spring.profiles.active=docker]
```

**문제점**:
1. `openjdk:17-jdk-slim` 이미지가 Docker Hub에서 더 이상 제공되지 않음
2. ENTRYPOINT 형식이 잘못됨 (JSON 배열 형식이 아님)

**해결책**: 최근 배포에서는 이 Dockerfile을 사용하지 않고, 기존 이미지를 사용하여 직접 실행하는 방식을 사용했습니다.

---

## 배포 환경 설정

### Spring Profile

배포 시 `docker` 프로파일을 사용합니다:
- **환경 변수**: `SPRING_PROFILES_ACTIVE=docker`
- **설정 파일**: `application-docker.yml`

### 네트워크 설정

- **Docker 네트워크**: `dorandoran-network`
- **포트 매핑**: `8080:8080`
- **Redis 연결**: `dorandoran-redis:6379` (Docker 서비스명 사용)

### 환경 변수

```bash
SPRING_PROFILES_ACTIVE=docker
SPRING_REDIS_HOST=dorandoran-redis
SPRING_REDIS_PORT=6379
```

---

## 배포 이력

### 최근 배포 (2025-12-21)

**목적**: 12월 19-20일 로그 분석에서 식별된 의심 IP 6개 블랙리스트 추가

**추가된 IP**:
- `20.81.45.34` (HTTP/0.9 프로토콜 혼동)
- `66.132.153.124` (HTTP/2.0 프로토콜 혼동)
- `20.29.19.243` (HTTP/0.9 프로토콜 혼동)
- `205.210.31.129` (HTTP/0.9 프로토콜 혼동)
- `172.202.104.157` (HTTP/0.9 프로토콜 혼동)
- `162.142.125.32` (HTTP/2.0 프로토콜 혼동)

**배포 결과**:
- ✅ 빌드 성공
- ✅ JAR 파일 전송 완료 (56MB)
- ✅ 컨테이너 정상 실행
- ✅ 블랙리스트 초기화 완료

---

## 문제 해결

### 1. Docker 이미지 빌드 실패

**문제**: `openjdk:17-jdk-slim: not found`

**원인**: Docker Hub에서 해당 이미지를 찾을 수 없음

**해결**: 기존 이미지를 사용하여 직접 실행하는 방식으로 우회

### 2. PowerShell 명령어 호환성

**문제**: `&&` 연산자 사용 시 오류

**해결**: PowerShell에서는 `;`를 사용하거나 별도 명령으로 분리

### 3. 한글 인코딩 문제

**문제**: `grep` 명령에서 한글 검색 실패

**해결**: 영어 키워드 사용 (`blacklist`, `blocked` 등)

---

## 개선 제안

### 1. EC2 Dockerfile 업데이트

현재 EC2의 Dockerfile이 오래되었을 가능성이 있습니다. 최신 버전으로 업데이트:

```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY gateway.jar app.jar
ENV TZ=Asia/Seoul
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 2. 배포 스크립트 통합

수동 배포 방식을 스크립트로 자동화하여 일관성 유지

### 3. CI/CD 파이프라인 구축

GitHub Actions 또는 AWS CodePipeline을 사용한 자동 배포

---

## 참고 자료

- **배포 스크립트**: `scripts/deploy/deploy-gateway-service.ps1`
- **Dockerfile**: `gateway/Dockerfile`, `docker/Dockerfile.gateway`
- **설정 파일**: 
  - `gateway/src/main/resources/application.yml`
  - `gateway/src/main/resources/application-docker.yml`
- **블랙리스트 필터**: `gateway/src/main/java/com/dorandoran/gateway/filter/IpBlacklistFilter.java`

---

**작성일**: 2025-12-21  
**최종 업데이트**: 2025-12-21

