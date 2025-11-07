# 병목 현상 감지 가이드 (Bottleneck Detection Guide)

## 개요

병목 현상은 시스템의 성능 저하를 일으키는 주요 원인입니다. 이 가이드는 Prometheus 메트릭을 기반으로 병목 현상을 감지하고 측정하는 방법을 제시합니다.

## 병목 현상의 정의

병목 현상은 다음 조건 중 하나 이상이 지속적으로 발생할 때 감지됩니다:

1. **응답 시간 급증**: 평소보다 3배 이상 느려짐
2. **처리량 감소**: 동시 요청 수는 증가했는데 처리 성공 건수는 감소
3. **리소스 포화**: CPU, 메모리, 스레드 풀, DB 커넥션 풀 등이 80% 이상 사용
4. **큐 대기 시간 증가**: 요청이 큐에서 오래 대기
5. **에러율 증가**: 정상보다 높은 실패율 발생

---

## 측정 지표 및 Prometheus 쿼리

### 1. 응답 시간 지표 (Response Time Metrics)

#### 1.1 API 엔드포인트별 응답 시간 (p95, p99)

**핵심 엔드포인트 모니터링**:
```promql
# 로그인 API p95 응답 시간
histogram_quantile(0.95, 
  sum by (le, uri) (rate(http_server_requests_seconds_bucket{
    uri="/api/auth/login"
  }[5m]))
)

# 프로필 조회 API p95 응답 시간
histogram_quantile(0.95, 
  sum by (le, uri) (rate(http_server_requests_seconds_bucket{
    uri="/api/auth/me"
  }[5m]))
)

# 채팅방 목록 조회 API p95 응답 시간
histogram_quantile(0.95, 
  sum by (le, uri) (rate(http_server_requests_seconds_bucket{
    uri="/api/chat/chatrooms/all"
  }[5m]))
)

# p99 응답 시간 (극단적인 지연)
histogram_quantile(0.99, 
  sum by (le, uri) (rate(http_server_requests_seconds_bucket{
    uri=~"/api/.*"
  }[5m]))
)
```

**병목 감지 임계값**:
- **정상**: p95 < 500ms
- **주의**: p95 >= 500ms && p95 < 1.2s
- **경고**: p95 >= 1.2s && p95 < 3s
- **심각**: p95 >= 3s

#### 1.2 평균 응답 시간 추이

```promql
# 평균 응답 시간 (5분 평균)
rate(http_server_requests_seconds_sum[5m]) / 
rate(http_server_requests_seconds_count[5m])
```

**병목 감지**: 평소 대비 3배 이상 증가

---

### 2. 처리량 지표 (Throughput Metrics)

#### 2.1 초당 요청 수 (RPS)

```promql
# 초당 요청 수
sum by (uri, status) (rate(http_server_requests_seconds_count[1m]))

# 성공 요청만 (2xx)
sum by (uri) (rate(http_server_requests_seconds_count{
  status=~"2.."
}[1m]))

# 실패 요청 (4xx, 5xx)
sum by (uri) (rate(http_server_requests_seconds_count{
  status=~"[45].."
}[1m]))
```

**병목 감지 조건**:
- 동시 사용자 수는 증가했는데 **성공 요청 수는 감소** 또는 **정체**
- 실패 요청 수가 급증 (5분 평균 대비 2배 이상)

#### 2.2 처리량 대비 응답 시간

```promql
# 처리량 증가 시 응답 시간도 함께 증가하는지 확인
# (처리량 증가, 응답 시간 급증 = 병목 가능성)
(
  sum(rate(http_server_requests_seconds_count[5m])) > 
  sum(rate(http_server_requests_seconds_count[15m offset 5m])) * 1.5
) and (
  histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket[5m]))) > 
  histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket[15m offset 5m]))) * 2
)
```

---

### 3. 리소스 사용률 지표 (Resource Utilization)

#### 3.1 데이터베이스 연결 풀 (HikariCP)

```promql
# 활성 연결 수
hikaricp_connections_active{pool="HikariPool-1"}

# 최대 연결 수
hikaricp_connections_max{pool="HikariPool-1"}

# 사용률 (활성/최대)
hikaricp_connections_active{pool="HikariPool-1"} / 
hikaricp_connections_max{pool="HikariPool-1"}

# 대기 중인 연결 요청
hikaricp_connections_pending{pool="HikariPool-1"}

# 타임아웃된 연결 요청
hikaricp_connections_timeout_total{pool="HikariPool-1"}
```

**병목 감지 임계값**:
- **정상**: 사용률 < 60%
- **주의**: 사용률 >= 60% && < 80%
- **경고**: 사용률 >= 80% && < 90%
- **심각**: 사용률 >= 90% 또는 `pending > 0` 지속

**심각한 병목 쿼리**:
```promql
# 연결 풀 포화 상태
(hikaricp_connections_active / hikaricp_connections_max) > 0.9

# 대기 중인 연결 요청이 있음
hikaricp_connections_pending > 0

# 타임아웃 발생
rate(hikaricp_connections_timeout_total[5m]) > 0
```

#### 3.2 스레드 풀 상태

```promql
# Tomcat 스레드 풀 (Spring Boot 기본)
tomcat_threads_current{name="http-nio-8081"}  # 현재 활성 스레드
tomcat_threads_busy{name="http-nio-8081"}      # 작업 중인 스레드
tomcat_threads_max{name="http-nio-8081"}      # 최대 스레드

# 스레드 풀 사용률
tomcat_threads_busy / tomcat_threads_max

# 스레드 풀 포화 (대기 중인 요청)
tomcat_threads_current - tomcat_threads_busy
```

**병목 감지 임계값**:
- **정상**: 사용률 < 70%
- **주의**: 사용률 >= 70% && < 85%
- **경고**: 사용률 >= 85% && < 95%
- **심각**: 사용률 >= 95% 또는 `current >= max`

#### 3.3 JVM 힙 메모리

```promql
# 힙 메모리 사용률
jvm_memory_used_bytes{area="heap"} / 
jvm_memory_max_bytes{area="heap"}

# GC 시간 증가 (GC가 자주 발생하면 병목)
rate(jvm_gc_pause_seconds_sum[5m])

# GC 빈도
rate(jvm_gc_pause_seconds_count[5m])
```

**병목 감지 임계값**:
- **정상**: 힙 사용률 < 70%
- **주의**: 힙 사용률 >= 70% && < 85%
- **경고**: 힙 사용률 >= 85% && GC 시간 증가
- **심각**: 힙 사용률 >= 90% 또는 OOM 발생

---

### 4. 큐/대기 시간 지표 (Queue/Wait Time)

#### 4.1 요청 큐 대기 시간

```promql
# Circuit Breaker에서 대기 중인 요청
resilience4j_circuitbreaker_calls_total{kind="not_permitted"}

# Circuit Breaker OPEN 상태 시간
resilience4j_circuitbreaker_state{state="OPEN"}

# 리트라이 중인 요청
resilience4j_retry_calls_total{kind="retried"}
```

**병목 감지**: 
- Circuit Breaker가 OPEN 상태로 전이 → 하위 서비스 병목 가능
- `not_permitted` 호출 증가 → 하위 서비스가 요청을 처리하지 못함

#### 4.2 데이터베이스 쿼리 대기 시간

```promql
# HikariCP에서 대기 중인 연결 요청
hikaricp_connections_pending

# 타임아웃 발생
rate(hikaricp_connections_timeout_total[5m])
```

**병목 감지**: 
- `pending > 0` 지속 → DB 연결 풀 포화
- 타임아웃 발생 → DB 쿼리가 너무 오래 걸림

---

### 5. 에러율 지표 (Error Rate)

#### 5.1 HTTP 에러율

```promql
# 전체 에러율 (4xx + 5xx)
sum(rate(http_server_requests_seconds_count{
  status=~"[45].."
}[5m])) / 
sum(rate(http_server_requests_seconds_count[5m]))

# 서버 에러율 (5xx만)
sum(rate(http_server_requests_seconds_count{
  status=~"5.."
}[5m])) / 
sum(rate(http_server_requests_seconds_count[5m]))

# 엔드포인트별 에러율
sum by (uri) (rate(http_server_requests_seconds_count{
  status=~"[45].."
}[5m])) / 
sum by (uri) (rate(http_server_requests_seconds_count[5m]))
```

**병목 감지 임계값**:
- **정상**: 에러율 < 1%
- **주의**: 에러율 >= 1% && < 5%
- **경고**: 에러율 >= 5% && < 10%
- **심각**: 에러율 >= 10%

#### 5.2 Circuit Breaker 실패율

```promql
# Circuit Breaker 실패율
sum(rate(resilience4j_circuitbreaker_calls_total{
  kind="failed"
}[5m])) / 
sum(rate(resilience4j_circuitbreaker_calls_total[5m]))

# Circuit Breaker 상태 전이
rate(resilience4j_circuitbreaker_state_transitions_total{
  state="OPEN"
}[10m])
```

**병목 감지**:
- 실패율이 임계값(70%)을 초과 → Circuit Breaker OPEN → 하위 서비스 병목

---

### 6. 통합 병목 감지 쿼리

#### 6.1 종합 병목 지표 (Composite Bottleneck Score)

```promql
# 각 지표를 0-1 스코어로 정규화하여 합산
(
  # 응답 시간 점수 (p95 > 1.2s면 1점)
  clamp_max(histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket[5m]))) / 1.2, 1)
  +
  # 연결 풀 사용률 (80% 이상이면 1점)
  clamp_max(hikaricp_connections_active / hikaricp_connections_max / 0.8, 1)
  +
  # 스레드 풀 사용률 (85% 이상이면 1점)
  clamp_max(tomcat_threads_busy / tomcat_threads_max / 0.85, 1)
  +
  # 에러율 (5% 이상이면 1점)
  clamp_max(
    sum(rate(http_server_requests_seconds_count{status=~"[45].."}[5m])) / 
    sum(rate(http_server_requests_seconds_count[5m])) / 0.05, 1
  )
) / 4
```

**병목 감지**:
- **정상**: 점수 < 0.5
- **주의**: 점수 >= 0.5 && < 0.7
- **경고**: 점수 >= 0.7 && < 0.9
- **심각**: 점수 >= 0.9

---

## 실시간 병목 감지 대시보드 패널 구성

### 패널 1: 응답 시간 추이
```
- p50, p95, p99 응답 시간 (5분 윈도우)
- 기준선: p95 < 500ms (녹색), 500ms-1.2s (노란색), >1.2s (빨간색)
```

### 패널 2: 처리량 vs 응답 시간
```
- 처리량 (RPS)과 응답 시간을 같은 그래프에 표시
- 처리량 증가 시 응답 시간 급증 여부 확인
```

### 패널 3: 리소스 사용률
```
- HikariCP 연결 풀 사용률 (활성/최대)
- 스레드 풀 사용률
- 힙 메모리 사용률
- 기준선: 80% (경고), 90% (심각)
```

### 패널 4: 에러율 및 Circuit Breaker 상태
```
- HTTP 에러율 (4xx, 5xx)
- Circuit Breaker 상태 (OPEN/CLOSED/HALF_OPEN)
- Circuit Breaker 실패율
```

### 패널 5: 대기 시간
```
- HikariCP pending connections
- Circuit Breaker not_permitted calls
- Retry 중인 요청 수
```

---

## 알림 규칙 (Alert Rules)

### Alert 1: 응답 시간 급증
```yaml
- alert: HighResponseTime
  expr: |
    histogram_quantile(0.95, 
      sum by (le, uri) (rate(http_server_requests_seconds_bucket{
        uri=~"/api/.*"
      }[5m]))
    ) > 1.2
  for: 5m
  annotations:
    summary: "{{ $labels.uri }} 응답 시간이 1.2초를 초과했습니다"
```

### Alert 2: 연결 풀 포화
```yaml
- alert: DatabaseConnectionPoolSaturated
  expr: |
    (hikaricp_connections_active / hikaricp_connections_max) > 0.9
    or
    hikaricp_connections_pending > 0
  for: 2m
  annotations:
    summary: "데이터베이스 연결 풀이 포화 상태입니다"
```

### Alert 3: 스레드 풀 포화
```yaml
- alert: ThreadPoolSaturated
  expr: |
    (tomcat_threads_busy / tomcat_threads_max) > 0.95
  for: 3m
  annotations:
    summary: "스레드 풀이 포화 상태입니다"
```

### Alert 4: 에러율 급증
```yaml
- alert: HighErrorRate
  expr: |
    sum(rate(http_server_requests_seconds_count{
      status=~"[45].."
    }[5m])) / 
    sum(rate(http_server_requests_seconds_count[5m])) > 0.05
  for: 5m
  annotations:
    summary: "에러율이 5%를 초과했습니다"
```

### Alert 5: Circuit Breaker OPEN
```yaml
- alert: CircuitBreakerOpen
  expr: |
    resilience4j_circuitbreaker_state{state="OPEN"} == 1
  for: 1m
  annotations:
    summary: "Circuit Breaker {{ $labels.name }}가 OPEN 상태입니다"
```

### Alert 6: 처리량 감소 (트래픽 증가 대비)
```yaml
- alert: ThroughputDecreased
  expr: |
    sum(rate(http_server_requests_seconds_count{
      status=~"2.."
    }[5m])) < 
    sum(rate(http_server_requests_seconds_count{
      status=~"2.."
    }[15m offset 5m])) * 0.7
    and
    sum(rate(http_server_requests_seconds_count[5m])) > 
    sum(rate(http_server_requests_seconds_count[15m offset 5m])) * 1.2
  for: 5m
  annotations:
    summary: "트래픽은 증가했지만 성공 요청은 감소했습니다 (병목 가능성)"
```

---

## 병목 현상 분석 절차

### 1단계: 증상 확인
1. 사용자 불만 제기 또는 알림 발생
2. 대시보드에서 이상 지표 확인
3. 어떤 서비스/엔드포인트에서 발생하는지 확인

### 2단계: 지표 분석
1. **응답 시간 확인**: p95, p99가 임계값 초과하는지
2. **처리량 확인**: RPS 증가 추이와 성공률 확인
3. **리소스 사용률 확인**: 연결 풀, 스레드 풀, 메모리
4. **에러율 확인**: 4xx, 5xx 증가 여부

### 3단계: 원인 파악
1. **응답 시간 급증 + 연결 풀 포화** → DB 병목
2. **응답 시간 급증 + 스레드 풀 포화** → 애플리케이션 병목
3. **Circuit Breaker OPEN** → 하위 서비스 병목
4. **처리량 감소 + 에러율 증가** → 전체 시스템 병목

### 4단계: 해결 방안
1. **DB 병목**: 쿼리 최적화, 인덱스 추가, 연결 풀 크기 증가
2. **애플리케이션 병목**: 로직 최적화, 캐싱 강화, 스레드 풀 크기 조정
3. **하위 서비스 병목**: Circuit Breaker 설정 조정, 서비스 확장
4. **전체 시스템 병목**: 수평 확장, 로드 밸런싱, 트래픽 제한

---

## 모범 사례

1. **베이스라인 설정**: 정상 운영 시 각 지표의 평균값을 기록
2. **트렌드 모니터링**: 단일 임계값보다 트렌드 변화를 더 중요시
3. **다중 지표 확인**: 하나의 지표만으로 판단하지 말고 여러 지표 종합 판단
4. **정기적 리뷰**: 주간/월간 지표 리뷰로 병목 패턴 파악
5. **자동화**: 알림 규칙을 통해 병목 발생 시 즉시 대응

---

## 관련 파일
- `docker/prometheus.yml`: Prometheus 설정
- `docker/grafana/dashboards/*.json`: Grafana 대시보드
- `auth/src/main/resources/application-docker.yml`: Actuator 설정
- `chat/src/main/resources/application-docker.yml`: Actuator 설정

