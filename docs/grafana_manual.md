# Grafana 사용 매뉴얼

DoranDoran MSA 프로젝트의 Grafana 모니터링 대시보드 사용 가이드입니다.

## 📋 목차

1. [Grafana 접속하기](#1-grafana-접속하기)
2. [기본 설정](#2-기본-설정)
3. [대시보드 사용법](#3-대시보드-사용법)
4. [메트릭 확인하기](#4-메트릭-확인하기)
5. [알림 설정하기](#5-알림-설정하기)
6. [고급 기능](#6-고급-기능)
7. [문제 해결](#7-문제-해결)

## 1. Grafana 접속하기

### 1.1 웹 브라우저 접속

1. 웹 브라우저를 열고 다음 주소로 접속합니다:
   ```
   http://localhost:3000
   ```

2. 로그인 화면이 나타나면 다음 정보를 입력합니다:
   - **사용자명**: `admin`
   - **비밀번호**: `admin123`

3. "Log in" 버튼을 클릭합니다.

### 1.2 첫 로그인 시 설정

처음 로그인하면 비밀번호 변경을 요구할 수 있습니다:
- 현재 비밀번호: `admin123`
- 새 비밀번호: 원하는 비밀번호 입력
- 확인: 같은 비밀번호 재입력

## 2. 기본 설정

### 2.1 데이터소스 확인

1. 왼쪽 메뉴에서 **Configuration** (⚙️) → **Data Sources** 클릭
2. **Prometheus** 데이터소스가 이미 설정되어 있는지 확인
3. **URL**: `http://prometheus:9090`으로 설정되어 있어야 함
4. **Save & Test** 버튼을 클릭하여 연결 테스트

### 2.2 대시보드 확인

1. 왼쪽 메뉴에서 **Dashboards** (📊) 클릭
2. **Browse** 탭에서 사용 가능한 대시보드 목록 확인
3. 기본적으로 Spring Boot 관련 대시보드가 제공됨

## 3. 대시보드 사용법

### 3.1 대시보드 열기

1. **Dashboards** → **Browse** 클릭
2. 원하는 대시보드 이름을 클릭
3. 대시보드가 새 탭에서 열림

### 3.2 시간 범위 설정

1. 대시보드 상단의 시간 선택기 클릭
2. 다음 중 하나 선택:
   - **Last 5 minutes**: 최근 5분
   - **Last 15 minutes**: 최근 15분
   - **Last 1 hour**: 최근 1시간
   - **Last 6 hours**: 최근 6시간
   - **Last 24 hours**: 최근 24시간
   - **Custom**: 사용자 정의 시간 범위

### 3.3 새로고침 설정

1. 대시보드 상단의 새로고침 버튼 옆 화살표 클릭
2. 새로고침 간격 선택:
   - **Off**: 자동 새로고침 비활성화
   - **5s**: 5초마다
   - **10s**: 10초마다
   - **30s**: 30초마다
   - **1m**: 1분마다

## 4. 메트릭 확인하기

### 4.1 서비스 상태 확인

1. **Dashboards** → **Spring Boot 2.1 Statistics** 클릭
2. 다음 메트릭들을 확인할 수 있습니다:
   - **JVM Memory**: 메모리 사용량
   - **JVM Threads**: 스레드 수
   - **HTTP Requests**: HTTP 요청 수
   - **Database Connections**: 데이터베이스 연결 수

### 4.2 개별 서비스 메트릭

#### API Gateway (8080)
- **URL**: http://localhost:8080/actuator/prometheus
- **주요 메트릭**:
  - `http_server_requests_seconds`: HTTP 요청 응답 시간
  - `jvm_memory_used_bytes`: JVM 메모리 사용량
  - `jvm_threads_live`: 활성 스레드 수

#### Auth Service (8081)
- **URL**: http://localhost:8081/actuator/prometheus
- **주요 메트릭**:
  - `http_server_requests_seconds`: HTTP 요청 응답 시간
  - `jvm_memory_used_bytes`: JVM 메모리 사용량
  - `hibernate_connections_active`: 데이터베이스 연결 수

#### User Service (8082)
- **URL**: http://localhost:8082/actuator/prometheus
- **주요 메트릭**:
  - `http_server_requests_seconds`: HTTP 요청 응답 시간
  - `jvm_memory_used_bytes`: JVM 메모리 사용량
  - `hibernate_connections_active`: 데이터베이스 연결 수

### 4.3 Prometheus에서 직접 확인

1. **Configuration** → **Data Sources** 클릭
2. **Prometheus** 데이터소스의 **Explore** 버튼 클릭
3. 쿼리 입력창에 다음 중 하나 입력:

```
# 모든 서비스의 HTTP 요청 수
http_server_requests_seconds_count

# API Gateway의 메모리 사용량
jvm_memory_used_bytes{instance="api-gateway:8080"}

# Auth Service의 데이터베이스 연결 수
hibernate_connections_active{instance="auth-service:8081"}

# User Service의 활성 스레드 수
jvm_threads_live{instance="user-service:8082"}
```

4. **Run Query** 버튼 클릭하여 결과 확인

## 5. 알림 설정하기

### 5.1 알림 채널 설정

1. **Configuration** → **Alerting** → **Notification channels** 클릭
2. **Add channel** 버튼 클릭
3. 다음 정보 입력:
   - **Name**: `Email Alerts`
   - **Type**: `Email`
   - **Email addresses**: `admin@example.com`
4. **Test** 버튼으로 테스트 후 **Save**

### 5.2 알림 규칙 생성

1. **Configuration** → **Alerting** → **Alert rules** 클릭
2. **New rule** 버튼 클릭
3. 다음 설정:
   - **Rule name**: `High Memory Usage`
   - **Query**: `jvm_memory_used_bytes / jvm_memory_max_bytes > 0.8`
   - **Condition**: `IS ABOVE 0.8`
   - **Notification channel**: 위에서 생성한 채널 선택
4. **Save** 버튼 클릭

## 6. 고급 기능

### 6.1 커스텀 대시보드 생성

1. **Dashboards** → **New** → **New Dashboard** 클릭
2. **Add visualization** 클릭
3. **Query** 탭에서 데이터소스와 쿼리 설정
4. **Visualization** 탭에서 차트 타입 선택
5. **Panel options** 탭에서 제목과 설명 추가
6. **Save** 버튼으로 대시보드 저장

### 6.2 변수 사용

1. 대시보드 설정에서 **Variables** 탭 클릭
2. **Add variable** 클릭
3. 다음 설정:
   - **Name**: `service`
   - **Type**: `Query`
   - **Query**: `label_values(jvm_memory_used_bytes, instance)`
4. 패널에서 `$service` 변수 사용

### 6.3 임포트/익스포트

#### 대시보드 익스포트
1. 대시보드 설정 → **JSON Model** 클릭
2. JSON 내용 복사하여 파일로 저장

#### 대시보드 임포트
1. **Dashboards** → **Import** 클릭
2. JSON 파일 업로드 또는 JSON 내용 붙여넣기
3. **Load** 버튼 클릭

## 7. 문제 해결

### 7.1 데이터가 표시되지 않는 경우

1. **Prometheus 연결 확인**:
   - http://localhost:9090 접속
   - **Status** → **Targets** 클릭
   - 모든 서비스가 "UP" 상태인지 확인

2. **서비스 메트릭 엔드포인트 확인**:
   ```bash
   curl http://localhost:8080/actuator/prometheus
   curl http://localhost:8081/actuator/prometheus
   curl http://localhost:8082/actuator/prometheus
   ```

3. **시간 범위 확인**:
   - 대시보드의 시간 범위가 적절한지 확인
   - 최근 데이터가 있는지 확인

### 7.2 로그인 문제

1. **기본 계정 정보 확인**:
   - 사용자명: `admin`
   - 비밀번호: `admin123`

2. **Docker 컨테이너 재시작**:
   ```bash
   docker compose -f docker/docker-compose.msa.yml restart grafana
   ```

### 7.3 성능 문제

1. **메모리 사용량 확인**:
   ```bash
   docker stats dd-grafana
   ```

2. **로그 확인**:
   ```bash
   docker compose -f docker/docker-compose.msa.yml logs grafana
   ```

## 📞 추가 도움말

- **Grafana 공식 문서**: https://grafana.com/docs/
- **Prometheus 쿼리 가이드**: https://prometheus.io/docs/prometheus/latest/querying/
- **Spring Boot Actuator**: https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html

## 8. 동시 접속자 모니터링

### 8.1 동시 접속자 수 추정

DoranDoran MSA 프로젝트의 동시 접속자 수는 데이터베이스 커넥션 풀 사용률로 추정할 수 있습니다.

**대시보드 확인**:
1. **DoranDoran MSA Overview** 대시보드 접속
2. **Estimated Concurrent Users** 패널 확인
3. 각 서비스별 활성 DB 연결 수 표시

**추정 공식**:
```
동시 접속자 수 ≈ 활성 DB 커넥션 수
```

### 8.2 주요 모니터링 메트릭

#### DB 커넥션 풀 사용률
```promql
(hikaricp_connections_active / hikaricp_connections_max) * 100
```

**임계값**:
- 🟢 정상: 0-80%
- 🟡 경고: 80-95%
- 🔴 위험: 95% 이상

**병목 발생 시 조치**:
1. 풀 크기 증가 검토
2. 장시간 실행 쿼리 확인
3. Connection Leak 로그 확인

#### 서버 스레드 사용률
```promql
(tomcat_threads_busy / tomcat_threads_max) * 100
```

**병목 발생 시 조치**:
1. `server.tomcat.threads.max` 증가
2. 부하 분산(Load Balancer) 고려

### 8.3 병목 현상 조기 감지

#### 1. DB 커넥션 풀 경고 감지
```promql
# 80% 이상 사용 중인 서비스 식별
(hikaricp_connections_active / hikaricp_connections_max) * 100 > 80
```

#### 2. 응답 시간 증가 감지
```promql
# P95 응답 시간이 1초 초과
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m])) > 1
```

#### 3. 에러율 증가 감지
```promql
# 에러율이 1% 초과
(rate(http_server_requests_seconds_count{status=~"5.."}[5m]) / rate(http_server_requests_seconds_count[5m])) * 100 > 1
```

### 8.4 서비스별 동시 접속자 한계

| 서비스 | 최대 동시 접속자 | 병목 원인 |
|--------|-----------------|----------|
| Chat Service | 20명 | DB 커넥션 풀 (20) |
| User/Auth Service | 10명 | DB 커넥션 풀 (10) |
| Store/Batch Service | 10명 | DB 커넥션 풀 (10) |
| Gateway | 제한 없음 | 백엔드 서비스 병목 |

**참고**: 전체 시스템은 일반적으로 동시 20-30명, 최대 50-60명까지 처리 가능

자세한 내용은 [동시 접속자 수 평가 리포트](./CONCURRENT_USERS_ANALYSIS.md)를 참조하세요.

## 🔧 유용한 Prometheus 쿼리

```promql
# 서비스별 HTTP 요청 수
sum(rate(http_server_requests_seconds_count[5m])) by (instance)

# 서비스별 메모리 사용률
jvm_memory_used_bytes / jvm_memory_max_bytes * 100

# 서비스별 응답 시간 (95th percentile)
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# 데이터베이스 연결 수
hibernate_connections_active

# JVM 가비지 컬렉션 시간
rate(jvm_gc_pause_seconds_sum[5m])

# 동시 접속자 추정 (활성 DB 연결 수)
hikaricp_connections_active

# DB 커넥션 풀 사용률
(hikaricp_connections_active / hikaricp_connections_max) * 100

# 서버 스레드 사용률
(tomcat_threads_busy / tomcat_threads_max) * 100
```

이 매뉴얼을 통해 Grafana를 효과적으로 활용하여 DoranDoran MSA 프로젝트를 모니터링할 수 있습니다.
