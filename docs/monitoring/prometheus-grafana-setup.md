# Prometheus & Grafana 모니터링 시스템 구축 가이드

## 📋 목차
1. [개요](#개요)
2. [시스템 아키텍처](#시스템-아키텍처)
3. [구축 단계별 상세](#구축-단계별-상세)
4. [배포 스크립트](#배포-스크립트)
5. [설정 파일](#설정-파일)
6. [접근 정보](#접근-정보)
7. [대시보드 가이드](#대시보드-가이드)
8. [알림 규칙](#알림-규칙)
9. [리소스 사용량](#리소스-사용량)
10. [트러블슈팅](#트러블슈팅)

---

## 개요

DoranDoran MSA 프로젝트에 Prometheus와 Grafana를 활용한 통합 모니터링 시스템을 구축했습니다. 이 시스템은 모든 마이크로서비스의 메트릭을 수집하고, 시각화하며, 이상 상황을 감지하여 알림을 제공합니다.

### 주요 기능
- ✅ 실시간 메트릭 수집 (서비스, 데이터베이스, Redis)
- ✅ 4개의 대시보드를 통한 시각화
- ✅ 7개의 알림 규칙으로 이상 상황 감지
- ✅ 30일간 메트릭 데이터 보존
- ✅ 보안 강화 설정 적용

### 기술 스택
- **Prometheus**: 메트릭 수집 및 저장
- **Grafana**: 데이터 시각화 및 대시보드
- **AlertManager**: 알림 관리
- **PostgreSQL Exporter**: PostgreSQL 메트릭 수집
- **Redis Exporter**: Redis 메트릭 수집

---

## 시스템 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│                    Application Services                      │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  │
│  │ Gateway  │  │   Auth   │  │   User   │  │   Chat   │  │
│  │  :8080   │  │  :8081   │  │  :8082   │  │  :8083   │  │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘  │
│       │             │              │              │        │
│       └─────────────┴──────────────┴──────────────┘        │
│                          │                                   │
│                    /actuator/prometheus                      │
└──────────────────────────┼───────────────────────────────────┘
                           │
┌──────────────────────────┼───────────────────────────────────┐
│                    Prometheus                                 │
│              (dorandoran-prometheus:9090)                     │
│                          │                                    │
│       ┌──────────────────┼──────────────────┐                │
│       │                  │                  │                │
│  ┌────▼────┐      ┌─────▼─────┐     ┌─────▼─────┐          │
│  │Postgres │      │  Redis    │     │  Services │          │
│  │Exporter │      │ Exporter  │     │  Metrics  │          │
│  │  :9187  │      │  :9121    │     │           │          │
│  └─────────┘      └───────────┘     └───────────┘          │
└──────┬──────────────────────────────────────┬───────────────┘
       │                                      │
       │                                      │
┌──────▼──────────┐                  ┌───────▼──────────┐
│   Grafana      │                  │  AlertManager    │
│  :3000         │                  │     :9093        │
│                │                  │                  │
│  - Dashboards  │                  │  - Alerts        │
│  - Visualization│                  │  - Notifications │
└────────────────┘                  └──────────────────┘
```

---

## 구축 단계별 상세

### Phase 1: Prometheus 배포

#### 목표
- Prometheus 서버 배포 및 기본 설정
- 모든 서비스의 메트릭 수집 시작

#### 작업 내용
1. **Prometheus 컨테이너 배포**
   - 컨테이너명: `dorandoran-prometheus`
   - 포트: `9090`
   - 네트워크: `dorandoran-network`
   - 데이터 보존: 30일

2. **서비스 메트릭 수집 설정**
   - API Gateway (`dorandoran-gateway:8080`)
   - Auth Service (`dorandoran-auth:8081`)
   - User Service (`dorandoran-user:8082`)
   - Chat Service (`dorandoran-chat:8083`)
   - Store Service (`dorandoran-store:8084`)

3. **검증**
   - Prometheus UI 접근 확인
   - 모든 서비스 타겟 상태 확인 (up/down)
   - 메트릭 수집 확인

#### 배포 명령어
```bash
.\scripts\deploy\deploy-prometheus.ps1
```

---

### Phase 2: Grafana 배포

#### 목표
- Grafana 서버 배포
- Prometheus 데이터소스 자동 연결

#### 작업 내용
1. **Grafana 컨테이너 배포**
   - 컨테이너명: `dorandoran-grafana`
   - 포트: `3000`
   - 기본 계정: `admin / admin123`

2. **보안 설정**
   - 사용자 가입 비활성화 (`GF_USERS_ALLOW_SIGN_UP=false`)
   - Gravatar 비활성화 (`GF_SECURITY_DISABLE_GRAVATAR=true`)
   - 분석 및 업데이트 체크 비활성화

3. **Prometheus 데이터소스 자동 연결**
   - Provisioning을 통한 자동 설정
   - URL: `http://dorandoran-prometheus:9090`

#### 배포 명령어
```bash
.\scripts\deploy\deploy-grafana.ps1
```

---

### Phase 3: 인프라 메트릭 수집

#### 목표
- PostgreSQL 및 Redis 메트릭 수집
- Exporter 배포 및 Prometheus 연동

#### 작업 내용
1. **PostgreSQL Exporter 배포**
   - 컨테이너명: `dorandoran-postgres-exporter`
   - 포트: `9187`
   - 연결 정보: `postgresql://doran:doran@dorandoran-shared-db:5432/dorandoran`

2. **Redis Exporter 배포**
   - 컨테이너명: `dorandoran-redis-exporter`
   - 포트: `9121`
   - Redis 주소: `dorandoran-redis:6379`

3. **Prometheus 설정 업데이트**
   - `postgres` job 추가
   - `redis` job 추가

#### 배포 명령어
```bash
.\scripts\deploy\deploy-exporters.ps1
```

#### 수집되는 메트릭 예시
- PostgreSQL: 연결 수, 트랜잭션, 데이터베이스 크기
- Redis: 메모리 사용량, 명령어 통계, 연결된 클라이언트 수

---

### Phase 4: 대시보드 생성

#### 목표
- 서비스 모니터링을 위한 대시보드 생성
- 인증 모니터링 전용 대시보드
- 인프라 통합 대시보드

#### 생성된 대시보드

##### 1. DoranDoran MSA Overview
- **목적**: 전체 서비스 개요 및 헬스 체크
- **주요 패널**:
  - Service Health Status
  - HTTP Request Rate
  - Response Time (p95, p50)
  - Error Rate
  - JVM Memory Usage
  - Database Connection Pool

##### 2. Service Details
- **목적**: 개별 서비스 상세 메트릭
- **특징**: 서비스 선택 템플릿 변수 제공
- **주요 패널**:
  - HTTP Request Rate (서비스별)
  - Response Time (p95, p50)
  - Error Rate (4xx, 5xx)
  - JVM Memory Usage
  - Database Connection Pool
  - Service Health

##### 3. Authentication Monitoring
- **목적**: 인증 관련 모니터링
- **주요 패널**:
  - Login Attempts
  - Login Success/Failure Rate
  - JWT Token Validation
  - Token Validation Success/Failure
  - HMAC Authentication Failures

##### 4. Infrastructure Overview
- **목적**: 인프라 리소스 모니터링
- **주요 패널**:
  - PostgreSQL Connections
  - PostgreSQL Database Size
  - PostgreSQL Transactions
  - Redis Memory Usage
  - Redis Commands
  - Redis Connected Clients
  - All Services Health

#### 배포 명령어
```bash
.\scripts\deploy\deploy-grafana-dashboards.ps1
```

---

### Phase 5: 알림 시스템 구축

#### 목표
- AlertManager 배포
- 알림 규칙 정의 및 적용

#### 작업 내용
1. **AlertManager 배포**
   - 컨테이너명: `dorandoran-alertmanager`
   - 포트: `9093`
   - Prometheus와 연동

2. **알림 규칙 정의**

##### Service Alerts
- **ServiceDown**: 서비스가 1분 이상 다운된 경우
- **HighErrorRate**: 5xx 에러율이 5%를 초과하는 경우
- **HighResponseTime**: p95 응답 시간이 2초를 초과하는 경우

##### Database Alerts
- **DatabaseConnectionPoolExhausted**: DB 연결 풀 사용률이 90%를 초과하는 경우
- **PostgreSQLTooManyConnections**: PostgreSQL 활성 연결 수가 80개를 초과하는 경우

##### Infrastructure Alerts
- **HighJVMMemoryUsage**: JVM 힙 메모리 사용률이 85%를 초과하는 경우
- **HighRedisMemoryUsage**: Redis 메모리 사용률이 90%를 초과하는 경우

#### 배포 명령어
```bash
.\scripts\deploy\deploy-alertmanager.ps1
```

---

### Phase 6: 보안 및 최적화

#### 목표
- Grafana 보안 강화
- Prometheus 데이터 보존 정책 설정

#### 작업 내용
1. **Grafana 보안 설정**
   - 사용자 가입 비활성화
   - 세션 타임아웃 설정
   - 분석 및 업데이트 체크 비활성화

2. **Prometheus 최적화**
   - 데이터 보존 기간: 30일
   - 스크랩 간격 최적화

---

## 배포 스크립트

### 배포 스크립트 목록

#### 1. deploy-prometheus.ps1
Prometheus 서버 배포
```powershell
.\scripts\deploy\deploy-prometheus.ps1
```

#### 2. deploy-grafana.ps1
Grafana 서버 배포
```powershell
.\scripts\deploy\deploy-grafana.ps1
```

#### 3. deploy-exporters.ps1
PostgreSQL 및 Redis Exporter 배포
```powershell
.\scripts\deploy\deploy-exporters.ps1
```

#### 4. deploy-grafana-dashboards.ps1
Grafana 대시보드 배포
```powershell
.\scripts\deploy\deploy-grafana-dashboards.ps1
```

#### 5. deploy-alertmanager.ps1
AlertManager 배포
```powershell
.\scripts\deploy\deploy-alertmanager.ps1
```

---

## 설정 파일

### Prometheus 설정 (`docker/prometheus-server.yml`)

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s
  external_labels:
    cluster: 'dorandoran-msa'
    environment: 'production'

rule_files:
  - "prometheus-alerts.yml"

alerting:
  alertmanagers:
    - static_configs:
        - targets:
            - dorandoran-alertmanager:9093

scrape_configs:
  # 서비스 메트릭 수집
  - job_name: 'api-gateway'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['dorandoran-gateway:8080']
    scrape_interval: 5s
  
  # ... (다른 서비스들)
  
  # 인프라 메트릭 수집
  - job_name: 'postgres'
    static_configs:
      - targets: ['dorandoran-postgres-exporter:9187']
    scrape_interval: 15s
  
  - job_name: 'redis'
    static_configs:
      - targets: ['dorandoran-redis-exporter:9121']
    scrape_interval: 15s
```

### AlertManager 설정 (`docker/alertmanager.yml`)

```yaml
global:
  resolve_timeout: 5m

route:
  group_by: ['alertname', 'cluster', 'service']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 12h
  receiver: 'default'
  routes:
    - match:
        severity: critical
      receiver: 'critical'
    - match:
        severity: warning
      receiver: 'default'

receivers:
  - name: 'default'
  - name: 'critical'

inhibit_rules:
  - source_match:
      severity: 'critical'
    target_match:
      severity: 'warning'
    equal: ['alertname', 'cluster', 'service']
```

### 알림 규칙 (`docker/prometheus-alerts.yml`)

```yaml
groups:
  - name: service_alerts
    interval: 30s
    rules:
      - alert: ServiceDown
        expr: up{job=~"api-gateway|auth-service|user-service|chat-service|store-service"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "서비스 {{ $labels.job }}이(가) 다운되었습니다"
          description: "{{ $labels.job }} 서비스가 1분 이상 응답하지 않습니다."
      
      # ... (다른 알림 규칙들)
```

---

## 접근 정보

### 웹 UI 접근

| 서비스 | URL | 인증 정보 |
|--------|-----|----------|
| Prometheus | http://3.21.177.186:9090 | 없음 |
| Grafana | http://3.21.177.186:3000 | admin / admin123 |
| AlertManager | http://3.21.177.186:9093 | 없음 |

### 컨테이너 정보

| 컨테이너명 | 포트 | 용도 |
|-----------|------|------|
| dorandoran-prometheus | 9090 | 메트릭 수집 및 저장 |
| dorandoran-grafana | 3000 | 대시보드 및 시각화 |
| dorandoran-alertmanager | 9093 | 알림 관리 |
| dorandoran-postgres-exporter | 9187 | PostgreSQL 메트릭 |
| dorandoran-redis-exporter | 9121 | Redis 메트릭 |

---

## 대시보드 가이드

### 대시보드 접근 방법

1. Grafana에 로그인 (http://3.21.177.186:3000)
2. 왼쪽 메뉴에서 "Dashboards" 클릭
3. 원하는 대시보드 선택

### 주요 메트릭 설명

#### HTTP 메트릭
- `http_server_requests_seconds_count`: HTTP 요청 수
- `http_server_requests_seconds_bucket`: 응답 시간 히스토그램
- `http_server_requests_seconds_count{status="5.."}`: 5xx 에러 수

#### JVM 메트릭
- `jvm_memory_used_bytes{area="heap"}`: 힙 메모리 사용량
- `jvm_memory_max_bytes{area="heap"}`: 힙 메모리 최대값

#### 데이터베이스 메트릭
- `hikaricp_connections_active`: 활성 DB 연결 수
- `hikaricp_connections_idle`: 유휴 DB 연결 수
- `hikaricp_connections_max`: 최대 DB 연결 수

#### PostgreSQL 메트릭
- `pg_stat_database_numbackends`: 활성 연결 수
- `pg_database_size_bytes`: 데이터베이스 크기
- `pg_stat_database_xact_commit`: 커밋된 트랜잭션 수

#### Redis 메트릭
- `redis_memory_used_bytes`: 사용 중인 메모리
- `redis_memory_max_bytes`: 최대 메모리
- `redis_commands_total`: 명령어 실행 수
- `redis_connected_clients`: 연결된 클라이언트 수

---

## 알림 규칙

### 알림 규칙 목록

#### Critical 알림
- **ServiceDown**: 서비스가 다운된 경우 (1분 이상)

#### Warning 알림
- **HighErrorRate**: 에러율이 5%를 초과하는 경우 (5분 이상)
- **HighResponseTime**: p95 응답 시간이 2초를 초과하는 경우 (5분 이상)
- **DatabaseConnectionPoolExhausted**: DB 연결 풀 사용률이 90%를 초과하는 경우 (5분 이상)
- **PostgreSQLTooManyConnections**: PostgreSQL 연결 수가 80개를 초과하는 경우 (5분 이상)
- **HighJVMMemoryUsage**: JVM 힙 메모리 사용률이 85%를 초과하는 경우 (5분 이상)
- **HighRedisMemoryUsage**: Redis 메모리 사용률이 90%를 초과하는 경우 (5분 이상)

### 알림 확인 방법

1. **Prometheus UI에서 확인**
   - http://3.21.177.186:9090/alerts 접근
   - 활성화된 알림 확인

2. **AlertManager UI에서 확인**
   - http://3.21.177.186:9093 접근
   - 알림 그룹 및 상태 확인

3. **Grafana에서 확인**
   - 대시보드에서 알림 상태 확인
   - 알림 히스토리 확인

---

## 리소스 사용량

### 최종 리소스 현황

#### CPU 사용률
- **사용률**: 약 9.1% (user + system)
- **유휴**: 약 87.9%
- **상태**: 여유

#### 메모리 사용량
- **총 메모리**: 3.7GB
- **사용 중**: 2.5GB (67.6%)
- **사용 가능**: 392MB
- **Swap 사용**: 1.0GB (전체)
- **상태**: 주의 필요 (Swap 사용 중)

#### 디스크 사용량
- **총 용량**: 30GB
- **사용 중**: 24GB (79%)
- **사용 가능**: 6.4GB
- **상태**: 주의 필요

### 모니터링 시스템 리소스

| 컨테이너 | CPU % | 메모리 | 메모리 % |
|---------|-------|--------|----------|
| dorandoran-grafana | 0.29% | 119.7MB | 3.13% |
| dorandoran-prometheus | 0.51% | 44.97MB | 1.17% |
| dorandoran-alertmanager | 0.07% | 12.12MB | 0.32% |
| dorandoran-redis-exporter | 1.06% | 9.49MB | 0.25% |
| dorandoran-postgres-exporter | 0.00% | 7.61MB | 0.20% |

**총 모니터링 시스템 메모리 사용량**: 약 194MB

---

## 트러블슈팅

### 일반적인 문제

#### 1. Prometheus가 메트릭을 수집하지 않는 경우

**증상**: Prometheus UI에서 타겟이 "down" 상태

**해결 방법**:
```bash
# 타겟 상태 확인
curl http://localhost:9090/api/v1/targets

# 서비스 엔드포인트 확인
curl http://dorandoran-auth:8081/actuator/prometheus

# 네트워크 연결 확인
docker network inspect dorandoran-network
```

#### 2. Grafana에서 데이터가 표시되지 않는 경우

**증상**: 대시보드에 "No data" 표시

**해결 방법**:
1. Prometheus 데이터소스 연결 확인
2. 메트릭 쿼리 문법 확인
3. 시간 범위 확인 (최근 데이터가 있는지)

#### 3. AlertManager가 알림을 받지 않는 경우

**증상**: 알림이 발생하지 않음

**해결 방법**:
```bash
# Prometheus와 AlertManager 연결 확인
curl http://localhost:9090/api/v1/alertmanagers

# 알림 규칙 로드 확인
curl http://localhost:9090/api/v1/rules

# AlertManager 로그 확인
docker logs dorandoran-alertmanager
```

#### 4. 대시보드가 로드되지 않는 경우

**증상**: Grafana에서 대시보드가 보이지 않음

**해결 방법**:
```bash
# 대시보드 파일 확인
ls -la /opt/grafana/dashboards/

# Grafana 로그 확인
docker logs dorandoran-grafana | grep dashboard

# Provisioning 설정 확인
cat /opt/grafana/provisioning/dashboards/dashboard.yml
```

### 로그 확인 명령어

```bash
# Prometheus 로그
docker logs dorandoran-prometheus --tail 50

# Grafana 로그
docker logs dorandoran-grafana --tail 50

# AlertManager 로그
docker logs dorandoran-alertmanager --tail 50

# Exporter 로그
docker logs dorandoran-postgres-exporter --tail 50
docker logs dorandoran-redis-exporter --tail 50
```

### 컨테이너 재시작

```bash
# Prometheus 재시작
docker restart dorandoran-prometheus

# Grafana 재시작
docker restart dorandoran-grafana

# AlertManager 재시작
docker restart dorandoran-alertmanager
```

---

## 유지보수

### 정기 점검 항목

1. **주간 점검**
   - 디스크 사용량 확인
   - 메모리 사용량 확인
   - 알림 규칙 동작 확인

2. **월간 점검**
   - Prometheus 데이터 보존 정책 확인
   - 대시보드 성능 확인
   - 알림 규칙 임계값 조정 검토

3. **분기별 점검**
   - 보안 설정 업데이트
   - 버전 업그레이드 검토
   - 리소스 사용량 분석

### 백업

#### Prometheus 데이터 백업
```bash
# 데이터 디렉토리 백업
tar -czf prometheus-backup-$(date +%Y%m%d).tar.gz /opt/prometheus/data
```

#### Grafana 설정 백업
```bash
# 대시보드 및 설정 백업
tar -czf grafana-backup-$(date +%Y%m%d).tar.gz /opt/grafana
```

---

## 향후 개선 사항

1. **로그 수집 시스템 추가**
   - Loki 통합 검토
   - 로그 분석 대시보드 추가

2. **알림 채널 확장**
   - 이메일 알림 설정
   - Slack/Discord 연동
   - SMS 알림 (중요 알림만)

3. **성능 최적화**
   - 메트릭 수집 간격 최적화
   - 데이터 압축 설정
   - 장기 저장소 연동 (Thanos 등)

4. **보안 강화**
   - Grafana 인증 강화 (LDAP/OAuth)
   - Prometheus 인증 추가
   - 네트워크 격리 강화

---

## 참고 자료

### 공식 문서
- [Prometheus 공식 문서](https://prometheus.io/docs/)
- [Grafana 공식 문서](https://grafana.com/docs/)
- [AlertManager 공식 문서](https://prometheus.io/docs/alerting/latest/alertmanager/)

### 유용한 링크
- [Prometheus 쿼리 예제](https://prometheus.io/docs/prometheus/latest/querying/examples/)
- [Grafana 대시보드 템플릿](https://grafana.com/grafana/dashboards/)
- [PostgreSQL Exporter 문서](https://github.com/prometheus-community/postgres_exporter)
- [Redis Exporter 문서](https://github.com/oliver006/redis_exporter)

---

## 변경 이력

| 날짜 | 버전 | 변경 내용 |
|------|------|----------|
| 2025-11-11 | 1.0 | 초기 구축 완료 |
| | | - Prometheus 배포 |
| | | - Grafana 배포 |
| | | - Exporter 배포 |
| | | - 대시보드 생성 |
| | | - AlertManager 배포 |
| | | - 보안 설정 적용 |

---

**작성일**: 2025-11-11  
**작성자**: DoranDoran 개발팀  
**문서 버전**: 1.0

