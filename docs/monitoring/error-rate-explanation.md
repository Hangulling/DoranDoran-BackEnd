# Error Rate 설명 및 실제 오류 확인 방법

## Error Rate란?

### 정의
**Error Rate**는 HTTP 요청 중 에러 상태 코드를 반환한 요청의 비율입니다.

### 현재 대시보드에서 수집하는 정보

#### 1. 5xx 에러 (서버 에러)
- **메트릭**: `rate(http_server_requests_seconds_count{status=~"5.."}[5m])`
- **의미**: 서버 내부 오류
- **상태 코드 예시**:
  - **500**: Internal Server Error (서버 내부 오류)
  - **502**: Bad Gateway (게이트웨이 오류)
  - **503**: Service Unavailable (서비스 사용 불가)
  - **504**: Gateway Timeout (게이트웨이 타임아웃)

#### 2. 4xx 에러 (클라이언트 에러)
- **메트릭**: `rate(http_server_requests_seconds_count{status=~"4.."}[5m])`
- **의미**: 클라이언트 요청 문제
- **상태 코드 예시**:
  - **400**: Bad Request (잘못된 요청)
  - **401**: Unauthorized (인증 실패)
  - **403**: Forbidden (권한 없음)
  - **404**: Not Found (리소스 없음)

### ⚠️ 현재 제한사항

**Grafana 대시보드에서는 다음 정보만 볼 수 있습니다:**
- ✅ 에러 발생 횟수 (초당)
- ✅ 에러율 (전체 요청 대비)
- ✅ 어떤 서비스에서 에러가 발생했는지
- ✅ 어떤 엔드포인트에서 에러가 발생했는지
- ✅ HTTP 상태 코드 (4xx, 5xx)

**다음 정보는 볼 수 없습니다:**
- ❌ 실제 오류 메시지
- ❌ 스택 트레이스
- ❌ 예외 클래스 이름
- ❌ 오류 발생 시점의 상세 로그
- ❌ 요청 파라미터나 헤더 정보

## 실제 오류 내용을 보는 방법

### 방법 1: Docker 로그 확인 (현재 가능)

각 서비스의 로그를 직접 확인:

```bash
# Auth Service 로그
docker logs dorandoran-auth --tail 100

# User Service 로그
docker logs dorandoran-user --tail 100

# Chat Service 로그
docker logs dorandoran-chat --tail 100

# Store Service 로그
docker logs dorandoran-store --tail 100

# Gateway 로그
docker logs dorandoran-gateway --tail 100

# 실시간 로그 확인
docker logs -f dorandoran-auth

# 에러만 필터링
docker logs dorandoran-auth 2>&1 | grep -i error
docker logs dorandoran-auth 2>&1 | grep -i exception
```

### 방법 2: 로그 파일 직접 확인

서비스 로그가 파일로 저장되어 있다면:

```bash
# 로그 파일 위치 확인 (서비스별로 다를 수 있음)
docker exec dorandoran-auth ls -la /app/logs/

# 로그 파일 내용 확인
docker exec dorandoran-auth cat /app/logs/application.log | tail -100
```

### 방법 3: Loki 통합 (향후 개선)

현재는 Loki가 설정되어 있지 않지만, 향후 추가하면:
- 모든 서비스 로그를 중앙 집중화
- Grafana에서 로그 쿼리 및 검색 가능
- 에러 메시지, 스택 트레이스 확인 가능
- 메트릭과 로그 연동 분석

## Error Rate 해석 방법

### 대시보드에서 보는 정보

#### 예시: Error Rate 패널
```
5xx Errors - auth-service: 0.5 req/s
4xx Errors - auth-service: 2.3 req/s
```

**의미**:
- 초당 0.5개의 5xx 에러 발생 (서버 문제)
- 초당 2.3개의 4xx 에러 발생 (클라이언트 요청 문제)

### 실제 오류 확인 절차

1. **대시보드에서 에러 감지**
   - Error Rate 패널에서 에러 발생 확인
   - 어떤 서비스에서 에러가 발생했는지 확인
   - 어떤 엔드포인트에서 에러가 발생했는지 확인

2. **해당 서비스 로그 확인**
   ```bash
   # 예: auth-service에서 에러 발생
   docker logs dorandoran-auth --tail 200 | grep -A 10 -B 10 "ERROR\|Exception"
   ```

3. **상세 분석**
   - 에러 메시지 확인
   - 스택 트레이스 확인
   - 발생 시점의 컨텍스트 확인

## 실전 예시

### 시나리오: Error Rate가 높게 나타남

#### 1단계: 대시보드 확인
```
Error Rate 패널:
- 5xx Errors - auth-service: 5 req/s
- 발생 엔드포인트: /api/auth/login
```

#### 2단계: 로그 확인
```bash
docker logs dorandoran-auth --tail 100 | grep "/api/auth/login"
```

#### 3단계: 상세 로그 확인
```bash
docker logs dorandoran-auth 2>&1 | grep -A 20 "ERROR\|Exception" | tail -50
```

**예상되는 로그 내용**:
```
2025-11-11 21:30:15 ERROR [http-nio-8081-exec-5] c.d.auth.controller.AuthController : Login failed
java.sql.SQLException: Connection timeout
    at com.zaxxer.hikari.pool.HikariPool.getConnection(HikariPool.java:200)
    at com.dorandoran.auth.service.AuthService.authenticate(AuthService.java:45)
    ...
```

## 현재 모니터링 시스템의 한계

### ✅ 볼 수 있는 것
- 에러 발생 빈도
- 에러 발생 서비스/엔드포인트
- HTTP 상태 코드
- 에러 발생 트렌드

### ❌ 볼 수 없는 것
- 실제 오류 메시지
- 스택 트레이스
- 예외 상세 정보
- 요청/응답 본문

## 개선 방안

### 1. Loki 통합 (권장)
- 모든 서비스 로그를 중앙 집중화
- Grafana에서 로그 쿼리 및 검색
- 메트릭과 로그 연동

### 2. 로그 수집 스크립트
```bash
# 에러 로그 수집 스크립트
#!/bin/bash
for service in auth user chat store gateway; do
    echo "=== $service Service Errors ==="
    docker logs dorandoran-$service 2>&1 | grep -i "error\|exception" | tail -20
    echo ""
done
```

### 3. 로그 파일 마운트
- 서비스 로그를 호스트에 저장
- 로그 분석 도구 사용 가능

## 요약

### Error Rate란?
- HTTP 상태 코드 기반의 에러 발생 비율
- 4xx (클라이언트 에러)와 5xx (서버 에러)로 구분
- 초당 에러 발생 횟수로 표시

### Grafana에서 볼 수 있는 것
- ✅ 에러 발생 횟수 및 비율
- ✅ 에러 발생 서비스/엔드포인트
- ✅ HTTP 상태 코드

### Grafana에서 볼 수 없는 것
- ❌ 실제 오류 메시지
- ❌ 스택 트레이스
- ❌ 예외 상세 정보

### 실제 오류 확인 방법
- **Docker 로그**: `docker logs <service-name>`
- **로그 파일**: 서비스 로그 파일 직접 확인
- **향후**: Loki 통합으로 Grafana에서 로그 확인 가능

---

**작성일**: 2025-11-11  
**버전**: 1.0






