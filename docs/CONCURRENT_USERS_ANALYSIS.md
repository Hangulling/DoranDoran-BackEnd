# DoranDoran 동시 접속자 수 평가 리포트

## 📋 분석 개요

본 문서는 DoranDoran MSA 프로젝트의 동시 접속자 처리 능력을 종합적으로 분석합니다.
각 서비스의 제한사항을 파악하고 병목 지점을 식별합니다.

**분석 기준일**: 2025년 현재  
**분석 대상**: 6개 마이크로서비스 (Auth, User, Chat, Store, Batch, Gateway)

---

## 🔍 서비스별 현재 설정값

### 1. Database Connection Pool (HikariCP)

**최종 적용된 설정** (메모리 제약 + 사용 패턴 고려):
| 서비스 | Port | 최대 풀 크기 | 최소 유휴 | 설명 |
|--------|------|-------------|----------|------|
| **Chat Service** | 8083 | **20** | 5 | 실시간 메시지 처리 중심 |
| **User Service** | 8082 | **15** | 5 | 사용자 정보 조회 (Redis 캐싱 가능) |
| **Auth Service** | 8081 | **10** | 5 | 토큰 검증만 (Redis 블랙리스트 사용) |
| **Store Service** | 8084 | **10** | 5 | 보관함 조회 (Redis 캐싱 가능) |
| **Batch Service** | 8085 | **5** | 1 | 하루 한 번 실행, 평소 최소화 |

**총 DB 연결**: **60개** (PostgreSQL max_connections=100, 여유 40개 ✅)

**Redis 캐싱 전략**:
- **User Service**: 사용자 정보 캐싱 (userId → UserDto, TTL: 10분)
- **Auth Service**: 토큰 블랙리스트 (Redis 기반)
- **Store Service**: 보관함 목록 캐싱 (userId → List<Store>, TTL: 5분)
- **Chat Service**: 채팅방 메타데이터 캐싱 (chatroomId → ChatRoom, TTL: 10분)

### 2. Redis Connection Pool (Lettuce)

모든 서비스 공통:
- `max-active: 8`
- `max-idle: 8`
- `min-idle: 0`
- `timeout: 2000ms`

### 3. 서버 스레드 풀 (Tomcat)

**현재 설정**: 명시적 설정 없음 → Spring Boot 기본값 사용

**기본값**:
- `server.tomcat.threads.max: 200`
- `server.tomcat.threads.min-spare: 10`
- `accept-count: 100` (대기열 크기)

**Gateway (Reactive)**: 
- Spring Cloud Gateway는 Reactive 기반이므로 스레드 제한 없음
- 백엔드 서비스가 병목

### 4. PostgreSQL 데이터베이스

**postgres:17-alpine 기본 설정**:
- `max_connections: 100` (공유 DB에 모든 서비스가 연결)
- 전체 예상 연결: Chat(20) + User(10) + Auth(10) + Store(10) + Batch(10) = **60개 연결**

---

## 🎯 동시 접속자 수 추정

### 이론적 최대 동시 접속자 수

```
실제 동시 처리 가능 접속자 수 = min(
  DB 커넥션 풀 크기,
  서버 스레드 풀 크기,
  PostgreSQL max_connections ÷ 서비스 수,
  파일 디스크립터 제한,
  메모리/CPU 여유 기준
)
```

### 서비스별 동시 접속자 한계

| 서비스 | 제한 요소 | 추정 동시 접속자 | 병목 원인 |
|--------|----------|-----------------|---------|
| **Chat Service** | DB 커넥션 풀 (20) | **최대 20명** | ⚠️ DB 연결이 주요 병목 |
| **User Service** | DB 커넥션 풀 (10) | **최대 10명** | ⚠️ DB 연결이 주요 병목 |
| **Auth Service** | DB 커넥션 풀 (10) | **최대 10명** | ⚠️ DB 연결이 주요 병목 |
| **Store Service** | DB 커넥션 풀 (10) | **최대 10명** | ⚠️ DB 연결이 주요 병목 |
| **Batch Service** | DB 커넥션 풀 (10) | **최대 10명** | ⚠️ DB 연결이 주요 병목 |
| **Gateway** | 제한 없음 (Reactive) | - | 백엔드 서비스가 병목 |

### 전체 시스템 동시 접속자 추정

**가정**: 
- 사용자는 주로 Chat Service를 사용
- User/Auth는 로그인 시에만 호출
- Store/Batch는 간헐적 사용

**추정**:
- **일반적인 경우**: 동시 **20-30명** (Chat Service 중심)
- **최대 사용 시**: 동시 **50-60명** (전 서비스 동시 사용)

---

## ⚠️ 주요 병목 지점 분석

### 1. Database Connection Pool Exhaustion (우선순위: 높음)

**현재 상황**:
- Chat Service는 20개 커넥션만 제공
- SSE(Long-running) 연결은 연결을 오래 점유
- 과거 2025-10-14에 이미 발생한 문제

**영향**:
- 커넥션 풀 고갈 시 30초 타임아웃 후 실패
- 전체 서비스 다운 가능

**해결책**:
- ✅ Chat Service: 풀 크기 20으로 증가 (이미 적용)
- ✅ `open-in-view=false` 설정으로 SSE 연결 누수 방지
- ⚠️ 필요시 Store/Batch도 풀 크기 증가 검토

### 2. Tomcat Thread Pool 제한 (우선순위: 중)

**현재 상황**:
- 명시적 설정 없음 → 기본 200 스레드
- HTTP 요청 수 > DB 커넥션 풀 크기인 경우, 대부분의 스레드는 대기

**추정 스레드 사용률**:
```
Chat Service: 활성 스레드 ~ 20개 (DB 커넥션 수와 동일)
User/Auth: 활성 스레드 ~ 10개
```

**해결책**:
- 일단 불필요 (DB 커넥션이 먼저 병목)
- 사용률 모니터링 후 필요시 증가

### 3. PostgreSQL max_connections (우선순위: 중)

**현재 상황** (적용됨):
- 전체 **150개** 연결 제한 (docker-compose.yml에서 설정)
- 예상 사용: 100개 (67%)
- 여유: 50개

**변경 사항**:
```yaml
# docker-compose.yml
command: postgres -c max_connections=150 -c shared_buffers=256MB
```

**모니터링 필요**:
```sql
-- PostgreSQL에서 실시간 확인
SELECT count(*) as total_connections 
FROM pg_stat_activity;

SELECT application_name, count(*) as connections
FROM pg_stat_activity 
WHERE state = 'active'
GROUP BY application_name;
```

**해결책**:
- ✅ `max_connections` 150으로 증가 (적용됨)
- ✅ `shared_buffers` 256MB로 증가 (메모리 최적화)
- 80% 이상 사용 시 추가 증가 검토

### 4. Redis Connection Pool (우선순위: 낮음)

**현재 상황**:
- 모든 서비스가 `max-active: 8` 사용
- 세션 관리/캐싱 용도로는 충분

**문제 발생 가능성**:
- 과도한 캐싱 정책 변경 시 부족 가능
- Redis 연결 부족 시 `Could not get a resource from the pool` 에러

**해결책**:
- 현재는 적정 수준
- 문제 발생 시 `max-active: 16-32`로 증가

### 5. 메모리 및 CPU (우선순위: 낮음)

**Docker 기본 설정**:
- 메모리 제한 없음 (호스트 메모리 사용)
- CPU 제한 없음

**예상 사용량**:
- Spring Boot 기본 JVM: `-Xmx512m` (512MB)
- 6개 서비스: 약 3GB RAM
- CPU는 사용률에 따라 달라짐

**모니터링**:
```bash
# Docker 컨테이너별 리소스 사용 확인
docker stats
```

---

## 📊 모니터링 지표

### 1. 실시간 모니터링 (Grafana)

다음 메트릭을 지속적으로 관찰:

#### DB 커넥션 사용률
```promql
hikari_connections_active / hikari_connections_max * 100
```
- **경고 기준**: 80% 이상
- **위험 기준**: 95% 이상

#### HTTP 요청률 (QPS)
```promql
rate(http_server_requests_seconds_count[1m])
```

#### 응답 시간
```promql
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))
```
- **목표**: P95 < 1초

#### 에러율
```promql
rate(http_server_requests_seconds_count{status=~"5.."}[5m])
```

#### 서버 스레드 사용률
```promql
tomcat_threads_busy / tomcat_threads_max * 100
```

#### JVM 메모리 사용률
```promql
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100
```

### 2. PostgreSQL 직접 조회

```sql
-- 현재 활성 연결 수
SELECT count(*) as active_connections 
FROM pg_stat_activity 
WHERE state = 'active';

-- 서비스별 연결 수
SELECT 
  application_name as service,
  count(*) as connections,
  max(now() - state_change) as max_duration
FROM pg_stat_activity 
WHERE state = 'active'
GROUP BY application_name
ORDER BY connections DESC;

-- 유휴 연결 수 (연결 누수 감지)
SELECT count(*) as idle_connections
FROM pg_stat_activity 
WHERE state = 'idle'
AND now() - state_change > interval '30 seconds';
```

---

## 💾 리소스 사용량 변화 분석

### 변경 사항

**PostgreSQL**:
- `max_connections`: 100 → 150개 (+50개)
- `shared_buffers`: 128MB → 256MB (+128MB)

**애플리케이션 풀 크기**:
- 60개 → 100개 (+40개)

### 리소스 증가 상세

#### 1. 메모리 사용량

**주요 변경사항**:
- PostgreSQL: max_connections 100 → 150개
- PostgreSQL: shared_buffers 128MB → 256MB
- 애플리케이션 풀: 총 60개 → 100개

**실제 증가량 계산**:
| 구성 요소 | 이전 | 이후 | 증가량 |
|----------|------|------|--------|
| **PostgreSQL** | ~1.0GB | ~1.6GB | **~600MB** |
| │ - 연결 메모리 (백엔드 프로세스) | 800MB | 1.2GB | +400MB |
| │ - shared_buffers | 128MB | 256MB | +128MB |
| │ - 기타 메모리 | 72MB | 144MB | +72MB |
| **애플리케이션 서비스** | ~3.0GB | ~3.0GB | **거의 변화 없음** |
| │ - JVM 힙 메모리 (변화 없음) | 3.0GB | 3.0GB | - |
| │ - 풀 증가는 운영 시 메모리에 영향 적음 | - | - | - |
| **전체 시스템** | ~4.0GB | ~4.6GB | **~600MB** |

**합계**: 약 **600MB 메모리 증가** (15% 증가)

#### 2. 파일 디스크립터

| 구성 요소 | 이전 | 이후 | 증가량 |
|----------|------|------|--------|
| **PostgreSQL** | ~300개 | ~450개 | **+150개** |
| **각 애플리케이션** | ~50개 | ~80개 | **+30개** |
| **전체** | ~550개 | ~850개 | **+300개** |

**공식**: 연결 1개당 약 3개 파일 디스크립터 사용

#### 3. CPU 사용량

| 항목 | 영향 | 설명 |
|------|------|------|
| **컨텍스트 스위칭** | 소폭 증가 | 연결 수 증가로 스케줄링 복잡도 ↑ |
| **쿼리 처리** | 미미 | 동시 쿼리 수 증가 (약 5-10% 성능 감소 가능) |
| **커넥션 관리** | 증가 | HikariCP의 풀 관리 오버헤드 약간 증가 |

**추정**: CPU 사용률 **10-15% 증가** (낮은 부하에서 높은 부하로 변경 시)

### 리소스 사용량 계산식

#### PostgreSQL 메모리
```bash
# PostgreSQL 프로세스 메모리 계산
총 메모리 = (활성_연결수 × 연결당_메모리) + shared_buffers + 기타_프로세스

비교:
이전 (100개 연결):
= (60개 실제사용 × 8MB) + 128MB + 100MB
≈ 700MB 실제 사용

이후 (150개 연결):
= (100개 실제사용 × 8MB) + 256MB + 150MB  
≈ 1,200MB 실제 사용

증가량: 약 500MB
```

#### 애플리케이션 메모리
```bash
# 각 서비스별 예상 메모리 (거의 변화 없음)
# 풀 크기 증가는 실제 메모리 사용에 큰 영향 없음
# 실제 연결만 메모리를 사용함

이전: Chat Service = 512MB (JVM 힙)
이후:  Chat Service = 512MB (동일)

설명:
- HikariCP 풀은 연결 객체를 보관하지만 메모리 영향 미미
- 실제 PostgreSQL 연결(소켓)은 PostgreSQL 백엔드 프로세스에서 관리
- 애플리케이션은 연결 객체 참조만 보관 (각각 ~1KB)
```

### 요약

| 리소스 | 이전 | 이후 | 증가량 | 증가율 |
|--------|------|------|--------|--------|
| **메모리** | 4.0GB | 4.6GB | **+600MB** | +15% |
| **파일 디스크립터** | 550개 | 850개 | +300개 | +55% |
| **CPU 사용률** | 낮음 | 중간 | - | ~+15% |
| **동시 접속자** | 20-30명 | **50-60명** | - | **+100%** |

### ⚠️ 주의사항

1. **메모리 모니터링 필수**
   ```bash
   # Docker 메모리 사용 확인
   docker stats dd-shared-db
   
   # PostgreSQL 실제 사용량 확인
   docker exec dd-shared-db psql -U doran -d dorandoran -c \
     "SELECT pg_size_pretty(pg_total_relation_size('pg_stat_activity'))"
   ```

2. **파일 디스크립터 제한 확인**
   ```bash
   # 호스트 시스템 제한 확인
   ulimit -n
   
   # PostgreSQL 컨테이너 제한 확인
   docker exec dd-shared-db sh -c "ulimit -n"
   ```

3. **권장 시스템 스펙**
   - 현재 서버 (3.7GB RAM): **부적합** ⚠️
   - 최소 RAM: **6GB** (변경사항 적용 가능)
   - 권장 RAM: **8-12GB** (편안한 사용)
   - 디스크: 최소 **20GB** 여유 공간

4. **현재 서버 (3.7GB RAM) 상황**
   - 메모리 사용률: 65% (이미 Swap 490MB 사용 중)
   - 예상 추가 필요: 500-600MB
   - 예상 결과: Swap 사용량 증가, 성능 저하
   - **권장**: 변경사항 적용 보류 또는 하드웨어 업그레이드

### 🔧 현재 서버 (3.7GB) 대응 방안

#### 옵션 1: 연결 풀만 증가 (권장) ✅ **최종 적용됨**
```yaml
# PostgreSQL 변경 취소
# docker-compose.yml에서 command 라인 제거

# 애플리케이션 풀 적절히 증가
Chat: 20개, User: 15개, Auth: 15개, Store: 10개, Batch: 10개
총 70개 풀 (PostgreSQL max_connections=100, 여유 30개)

효과:
- 메모리 증가: ~100MB 이하
- 동시 접속자: 20-30명 → 30-40명
- 안전 여유: 30% 확보
```

#### 옵션 2: PostgreSQL 설정만 적용
```yaml
# PostgreSQL 변경 유지
max_connections: 150, shared_buffers: 256MB

# 하지만 애플리케이션 풀은 변경 전으로 되돌림
Chat: 20개, User: 10개, Auth: 10개, Store: 10개, Batch: 10개
총 60개만 사용

효과: 메모리 증가 ~400MB, 여유 공간 확인 후 적용
```

#### 옵션 3: 하드웨어 업그레이드
```
현재: 3.7GB RAM → 목표: 최소 6GB RAM (권장 8GB)
결과: 안전하게 모든 변경사항 적용 가능
```

---

## 🚀 성능 개선 권장사항

### 단기 (즉시 적용 가능)

1. **Store Service 풀 크기 명시 설정**
   - 현재 명시적 설정 없음
   - `maximum-pool-size: 10` 추가 권장

2. **Connection Leak 감지 강화**
   - 모든 서비스에 `leak-detection-threshold: 10000` 적용
   - 로그 모니터링 자동화

3. **Grafana 대시보드 구성**
   - 동시 접속자 모니터링 패널 추가
   - 임계값 기반 알림 설정

### 중기 (성능 테스트 후 결정)

1. **부하 테스트 수행**
   - k6로 실제 동시 접속자 처리 능력 측정
   - 목표: 동시 50-100명 처리 가능 여부 확인

2. **풀 크기 조정**
   - 테스트 결과에 따라 Chat Service 풀을 30-50으로 증가
   - User/Auth 풀을 15-20으로 증가

3. **서버 스레드 명시 설정**
   ```yaml
   server:
     tomcat:
       threads:
         max: 200
         min-spare: 20
   ```
   - 대부분 서비스에 적용 (Gateway 제외)

### 장기 (시스템 확장)

1. **수평 스케일링 (Horizontal Scaling)**
   - 동일한 서비스를 여러 인스턴스로 실행
   - Load Balancer로 트래픽 분산
   - 예: Chat Service 인스턴스 2-3개

2. **Database Connection Pooling 개선**
   - PgBouncer 도입 (Connection Pool 최적화)
   - Shared Pool → Transaction Pool 모드 전환

3. **Caching 강화**
   - Redis 캐싱 전략 확대
   - Redis Cluster 구성 (고가용성)

4. **비동기 처리 패턴 도입**
   - Chat 메시지를 Kafka/RabbitMQ로 비동기 처리
   - DB 부하 감소

---

## 📈 현재 동시 접속자 수 모니터링 방법

### 1. Grafana 대시보드에서 확인

```
1. http://localhost:3000 접속 (admin/admin123)
2. "DoranDoran MSA Overview" 대시보드 확인
3. "Database Connections" 패널에서 실시간 모니터링
```

### 2. Prometheus에서 직접 쿼리

```promql
# Chat Service의 활성 DB 연결 수
hikari_connections_active{job="chat-service"}

# 전체 서비스의 DB 연결 수 합계
sum(hikari_connections_active) by (job)

# 서비스별 DB 커넥션 사용률
(hikari_connections_active / hikari_connections_max) * 100
```

### 3. Docker 컨테이너 로그 확인

```bash
# Chat Service 로그에서 커넥션 이벤트 확인
docker logs dorandoran-chat | grep -i connection

# HikariCP 커넥션 누수 경고 확인
docker logs dorandoran-chat | grep "Connection leak detection"
```

---

## 🔗 관련 문서

- [Grafana 모니터링 가이드](./grafana_manual.md)
- [부하 테스트 가이드](./scripts/test/README_LOAD_TEST.md)
- [SSE 연결 누수 문제 해결](./problem_solved/2025-10-14-connection-pool-exhaustion-sse.md)
- [API 사양서](./API_SPECIFICATION.md)

---

## 📝 최종 결론

### 현재 동시 접속자 처리 능력

- **일반적인 사용**: 동시 **20-30명** (Chat Service 중심)
- **최대 사용**: 동시 **50-60명** (전 서비스 동시 사용)
- **병목 지점**: Database Connection Pool (특히 Chat Service)

### 권장 조치

1. ✅ **단기**: Store Service 풀 크기 명시 설정
2. ✅ **중기**: 부하 테스트 후 풀 크기 조정
3. ✅ **장기**: 수평 스케일링 또는 PgBouncer 도입

### 모니터링 우선순위

1. **DB 커넥션 풀 사용률** (80% 경고)
2. **HTTP 응답 시간** (P95 > 1초 경고)
3. **에러율** (5xx > 1% 경고)

---

**작성일**: 2025년  
**담당자**: AI Assistant  
**검토 상태**: 모니터링 진행 중
