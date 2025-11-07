# 로그 기반 병목 현상 측정 가이드 (Grafana 없이)

## 개요

Grafana 대시보드가 아직 구성되지 않았거나 빠른 문제 파악이 필요한 경우, 로그와 Actuator 엔드포인트를 직접 조회하여 병목 현상을 측정할 수 있습니다.

---

## 방법 1: Actuator 메트릭 엔드포인트 직접 조회

### 1.1 응답 시간 확인

```bash
# Auth 서비스 - 로그인 API 응답 시간 (p95)
curl -s http://localhost:8081/actuator/metrics/http.server.requests | \
  jq '.measurements[] | select(.statistic == "TOTAL_TIME") | .value'

# 평균 응답 시간
curl -s http://localhost:8081/actuator/metrics/http.server.requests?tag=uri:/api/auth/login | \
  jq '.measurements[] | select(.statistic == "TOTAL_TIME") | .value'

# 특정 엔드포인트의 요청 수 및 총 시간
curl -s "http://localhost:8081/actuator/metrics/http.server.requests?tag=uri:/api/auth/login" | \
  jq '{count: .measurements[] | select(.statistic == "COUNT") | .value, 
       totalTime: .measurements[] | select(.statistic == "TOTAL_TIME") | .value}'
```

**평균 응답 시간 계산**:
```bash
# 평균 응답 시간 = 총 시간 / 요청 수
COUNT=$(curl -s "http://localhost:8081/actuator/metrics/http.server.requests?tag=uri:/api/auth/login" | \
  jq '.measurements[] | select(.statistic == "COUNT") | .value')
TOTAL_TIME=$(curl -s "http://localhost:8081/actuator/metrics/http.server.requests?tag=uri:/api/auth/login" | \
  jq '.measurements[] | select(.statistic == "TOTAL_TIME") | .value')
AVG_TIME=$(echo "scale=3; $TOTAL_TIME / $COUNT" | bc)
echo "평균 응답 시간: ${AVG_TIME}초"
```

### 1.2 데이터베이스 연결 풀 상태

```bash
# HikariCP 활성 연결 수
curl -s http://localhost:8081/actuator/metrics/hikaricp.connections.active | \
  jq '.measurements[] | select(.statistic == "VALUE") | .value'

# HikariCP 최대 연결 수
curl -s http://localhost:8081/actuator/metrics/hikaricp.connections.max | \
  jq '.measurements[] | select(.statistic == "VALUE") | .value'

# HikariCP 대기 중인 연결 요청
curl -s http://localhost:8081/actuator/metrics/hikaricp.connections.pending | \
  jq '.measurements[] | select(.statistic == "VALUE") | .value'

# 연결 풀 사용률 계산
ACTIVE=$(curl -s http://localhost:8081/actuator/metrics/hikaricp.connections.active | \
  jq '.measurements[] | select(.statistic == "VALUE") | .value')
MAX=$(curl -s http://localhost:8081/actuator/metrics/hikaricp.connections.max | \
  jq '.measurements[] | select(.statistic == "VALUE") | .value')
USAGE=$(echo "scale=2; $ACTIVE / $MAX * 100" | bc)
echo "연결 풀 사용률: ${USAGE}%"
```

### 1.3 Circuit Breaker 상태

```bash
# Circuit Breaker 상태 (OPEN=1, CLOSED=0)
curl -s http://localhost:8081/actuator/metrics/resilience4j.circuitbreaker.state | \
  jq '.measurements[] | select(.statistic == "VALUE") | .value'

# Circuit Breaker 실패율
curl -s http://localhost:8081/actuator/metrics/resilience4j.circuitbreaker.calls | \
  jq '.measurements[] | select(.statistic == "VALUE") | .value'
```

### 1.4 JVM 메모리 상태

```bash
# 힙 메모리 사용량 (bytes)
curl -s http://localhost:8081/actuator/metrics/jvm.memory.used?tag=area:heap | \
  jq '.measurements[] | select(.statistic == "VALUE") | .value'

# 힙 메모리 최대 크기 (bytes)
curl -s http://localhost:8081/actuator/metrics/jvm.memory.max?tag=area:heap | \
  jq '.measurements[] | select(.statistic == "VALUE") | .value'

# 힙 메모리 사용률 계산
USED=$(curl -s http://localhost:8081/actuator/metrics/jvm.memory.used?tag=area:heap | \
  jq '.measurements[] | select(.statistic == "VALUE") | .value')
MAX=$(curl -s http://localhost:8081/actuator/metrics/jvm.memory.max?tag=area:heap | \
  jq '.measurements[] | select(.statistic == "VALUE") | .value')
USAGE=$(echo "scale=2; $USED / $MAX * 100" | bc)
echo "힙 메모리 사용률: ${USAGE}%"
```

---

## 방법 2: Docker 로그 패턴 분석

### 2.1 응답 시간 급증 패턴 찾기

```bash
# 최근 10분간 로그에서 느린 요청 찾기 (응답 시간 > 1초)
docker logs --since 10m dorandoran-auth 2>&1 | \
  grep -E "completed|duration|took" | \
  awk '{if ($NF > 1000) print $0}'

# 로그에서 특정 패턴 추출 (예: "took 1234ms")
docker logs --since 10m dorandoran-auth 2>&1 | \
  grep -oE "took [0-9]+ms" | \
  sed 's/took //; s/ms//' | \
  awk '{sum+=$1; count++} END {if(count>0) print "평균: " sum/count "ms, 최대: " max}'
```

### 2.2 에러 패턴 분석

```bash
# 최근 10분간 에러 발생 빈도
docker logs --since 10m dorandoran-auth 2>&1 | \
  grep -i "error\|exception\|failed" | \
  wc -l

# 에러 타입별 분류
docker logs --since 10m dorandoran-auth 2>&1 | \
  grep -i "error\|exception" | \
  awk '{print $NF}' | \
  sort | uniq -c | sort -rn

# Circuit Breaker OPEN 상태 확인
docker logs --since 10m dorandoran-auth 2>&1 | \
  grep -i "circuit.*open\|circuit.*open" | \
  tail -n 20
```

### 2.3 데이터베이스 연결 관련 로그

```bash
# HikariCP 연결 풀 관련 로그
docker logs --since 10m dorandoran-auth 2>&1 | \
  grep -i "hikari\|connection.*pool\|connection.*timeout" | \
  tail -n 30

# 타임아웃 발생 확인
docker logs --since 10m dorandoran-auth 2>&1 | \
  grep -i "timeout\|timed out" | \
  wc -l
```

### 2.4 처리량 추정 (로그 기반)

```bash
# 최근 5분간 로그인 요청 수 추정
docker logs --since 5m dorandoran-auth 2>&1 | \
  grep -c "/api/auth/login"

# 성공 요청 vs 실패 요청
SUCCESS=$(docker logs --since 5m dorandoran-auth 2>&1 | grep -c "200 OK")
FAILED=$(docker logs --since 5m dorandoran-auth 2>&1 | grep -cE "4[0-9]{2}|5[0-9]{2}")
echo "성공: $SUCCESS, 실패: $FAILED"
```

---

## 방법 3: 간단한 모니터링 스크립트

### 3.1 종합 병목 체크 스크립트

```bash
#!/bin/bash
# check_bottleneck.sh

SERVICE_NAME=${1:-dorandoran-auth}
SERVICE_PORT=${2:-8081}
BASE_URL="http://localhost:${SERVICE_PORT}"

echo "=== 병목 현상 체크: $SERVICE_NAME ==="
echo "시간: $(date)"
echo ""

# 1. 응답 시간 체크
echo "1. 응답 시간 체크"
COUNT=$(curl -s "${BASE_URL}/actuator/metrics/http.server.requests?tag=uri:/api/auth/login" 2>/dev/null | \
  jq -r '.measurements[] | select(.statistic == "COUNT") | .value' 2>/dev/null || echo "0")
TOTAL_TIME=$(curl -s "${BASE_URL}/actuator/metrics/http.server.requests?tag=uri:/api/auth/login" 2>/dev/null | \
  jq -r '.measurements[] | select(.statistic == "TOTAL_TIME") | .value' 2>/dev/null || echo "0")
if [ "$COUNT" != "0" ] && [ "$COUNT" != "null" ]; then
  AVG_TIME=$(echo "scale=3; $TOTAL_TIME / $COUNT" | bc 2>/dev/null || echo "N/A")
  echo "  평균 응답 시간: ${AVG_TIME}초"
  if (( $(echo "$AVG_TIME > 1.2" | bc -l 2>/dev/null || echo 0) )); then
    echo "  ⚠️  경고: 응답 시간이 1.2초를 초과했습니다"
  fi
else
  echo "  데이터 없음"
fi
echo ""

# 2. 연결 풀 체크
echo "2. 데이터베이스 연결 풀 체크"
ACTIVE=$(curl -s "${BASE_URL}/actuator/metrics/hikaricp.connections.active" 2>/dev/null | \
  jq -r '.measurements[] | select(.statistic == "VALUE") | .value' 2>/dev/null || echo "0")
MAX=$(curl -s "${BASE_URL}/actuator/metrics/hikaricp.connections.max" 2>/dev/null | \
  jq -r '.measurements[] | select(.statistic == "VALUE") | .value' 2>/dev/null || echo "10")
PENDING=$(curl -s "${BASE_URL}/actuator/metrics/hikaricp.connections.pending" 2>/dev/null | \
  jq -r '.measurements[] | select(.statistic == "VALUE") | .value' 2>/dev/null || echo "0")

if [ "$ACTIVE" != "null" ] && [ "$MAX" != "null" ]; then
  USAGE=$(echo "scale=2; $ACTIVE / $MAX * 100" | bc 2>/dev/null || echo "0")
  echo "  활성 연결: $ACTIVE / 최대: $MAX (사용률: ${USAGE}%)"
  if [ "$PENDING" != "null" ] && [ "$PENDING" != "0" ]; then
    echo "  ⚠️  경고: 대기 중인 연결 요청: $PENDING"
  fi
  if (( $(echo "$USAGE > 90" | bc -l 2>/dev/null || echo 0) )); then
    echo "  🚨 심각: 연결 풀 사용률이 90%를 초과했습니다"
  fi
else
  echo "  데이터 없음"
fi
echo ""

# 3. Circuit Breaker 상태
echo "3. Circuit Breaker 상태"
CB_STATE=$(curl -s "${BASE_URL}/actuator/metrics/resilience4j.circuitbreaker.state" 2>/dev/null | \
  jq -r '.measurements[] | select(.statistic == "VALUE") | .value' 2>/dev/null || echo "0")
if [ "$CB_STATE" = "1" ]; then
  echo "  🚨 심각: Circuit Breaker가 OPEN 상태입니다"
else
  echo "  정상: Circuit Breaker가 CLOSED 상태입니다"
fi
echo ""

# 4. 힙 메모리 체크
echo "4. 힙 메모리 사용률"
USED=$(curl -s "${BASE_URL}/actuator/metrics/jvm.memory.used?tag=area:heap" 2>/dev/null | \
  jq -r '.measurements[] | select(.statistic == "VALUE") | .value' 2>/dev/null || echo "0")
MAX=$(curl -s "${BASE_URL}/actuator/metrics/jvm.memory.max?tag=area:heap" 2>/dev/null | \
  jq -r '.measurements[] | select(.statistic == "VALUE") | .value' 2>/dev/null || echo "1")
if [ "$USED" != "null" ] && [ "$MAX" != "null" ] && [ "$MAX" != "0" ]; then
  USAGE=$(echo "scale=2; $USED / $MAX * 100" | bc 2>/dev/null || echo "0")
  echo "  힙 메모리 사용률: ${USAGE}%"
  if (( $(echo "$USAGE > 85" | bc -l 2>/dev/null || echo 0) )); then
    echo "  ⚠️  경고: 힙 메모리 사용률이 85%를 초과했습니다"
  fi
else
  echo "  데이터 없음"
fi
echo ""

# 5. 로그 기반 에러 체크
echo "5. 최근 5분간 에러 발생"
ERROR_COUNT=$(docker logs --since 5m "$SERVICE_NAME" 2>&1 | grep -icE "error|exception|failed" || echo "0")
echo "  에러 발생 건수: $ERROR_COUNT"
if [ "$ERROR_COUNT" -gt 10 ]; then
  echo "  ⚠️  경고: 에러 발생 빈도가 높습니다"
fi
echo ""

echo "=== 체크 완료 ==="
```

**사용법**:
```bash
# Auth 서비스 체크
./check_bottleneck.sh dorandoran-auth 8081

# Chat 서비스 체크
./check_bottleneck.sh dorandoran-chat 8083

# User 서비스 체크
./check_bottleneck.sh dorandoran-user 8082
```

### 3.2 주기적 모니터링 스크립트 (watch 모드)

```bash
#!/bin/bash
# watch_bottleneck.sh

SERVICE_NAME=${1:-dorandoran-auth}
SERVICE_PORT=${2:-8081}

echo "병목 현상 모니터링 시작 (Ctrl+C로 종료)"
echo "서비스: $SERVICE_NAME"
echo ""

while true; do
  clear
  echo "=== $(date) ==="
  ./check_bottleneck.sh "$SERVICE_NAME" "$SERVICE_PORT"
  sleep 30  # 30초마다 체크
done
```

---

## 방법 4: Docker exec를 통한 직접 확인

### 4.1 컨테이너 내부에서 메트릭 확인

```bash
# 컨테이너 내부에서 Actuator 메트릭 조회
docker exec dorandoran-auth curl -s http://localhost:8081/actuator/metrics/http.server.requests | \
  jq '.measurements[]'

# Health 체크
docker exec dorandoran-auth curl -s http://localhost:8081/actuator/health | jq

# 모든 메트릭 목록
docker exec dorandoran-auth curl -s http://localhost:8081/actuator/metrics | jq '.names[]'
```

---

## 방법 5: 로그 기반 응답 시간 분석

### 5.1 Spring Boot 로깅 패턴 추가

`application-docker.yml`에 응답 시간 로깅 추가:

```yaml
# 로깅 설정
logging:
  level:
    org.springframework.web: INFO
    org.springframework.web.filter.CommonsRequestLoggingFilter: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
```

### 5.2 로그에서 응답 시간 추출

```bash
# 로그에서 HTTP 요청 완료 시간 추출
# (Spring Boot는 기본적으로 요청 완료 시간을 로그에 남기지 않으므로,
#  커스텀 로깅 필터가 필요하거나, 로그 패턴을 분석해야 함)

# 예: 로그에 "took 1234ms" 같은 패턴이 있다면
docker logs --since 10m dorandoran-auth 2>&1 | \
  grep -oE "took [0-9]+ms" | \
  sed 's/took //; s/ms//' | \
  awk '{
    sum+=$1
    count++
    if($1 > max) max=$1
    if(count==1 || $1 < min) min=$1
  } 
  END {
    if(count>0) {
      avg=sum/count
      print "평균: " avg "ms"
      print "최소: " min "ms"
      print "최대: " max "ms"
      print "총 요청: " count
    }
  }'
```

---

## 방법 6: 간단한 성능 테스트와 로그 비교

### 6.1 부하 테스트 후 로그 분석

```bash
# Apache Bench로 간단한 부하 테스트
ab -n 100 -c 10 http://localhost:8081/api/auth/health

# 테스트 중 로그 모니터링
docker logs -f dorandoran-auth 2>&1 | \
  grep -E "completed|duration|took|error" | \
  tee /tmp/auth_test.log

# 테스트 후 분석
grep -i "error" /tmp/auth_test.log | wc -l
grep -oE "[0-9]+ms" /tmp/auth_test.log | \
  awk '{sum+=$1; count++} END {print "평균: " sum/count "ms"}'
```

---

## 병목 감지 체크리스트

### 빠른 체크 (1분 이내)

```bash
# 1. Circuit Breaker 상태
docker exec dorandoran-auth curl -s http://localhost:8081/actuator/metrics/resilience4j.circuitbreaker.state | jq

# 2. 연결 풀 상태
docker exec dorandoran-auth curl -s http://localhost:8081/actuator/metrics/hikaricp.connections.active | jq

# 3. 최근 에러 로그
docker logs --tail 50 dorandoran-auth 2>&1 | grep -i error | tail -10
```

### 상세 체크 (5분)

```bash
# 종합 스크립트 실행
./check_bottleneck.sh dorandoran-auth 8081

# 로그 패턴 분석
docker logs --since 5m dorandoran-auth 2>&1 | \
  grep -E "timeout|exception|error|circuit" | \
  sort | uniq -c | sort -rn
```

---

## 실용적인 모니터링 명령어 조합

### 한 줄 명령어로 빠른 체크

```bash
# Auth 서비스 전체 상태 한눈에 보기
echo "=== Auth 서비스 상태 ===" && \
echo "연결 풀: $(docker exec dorandoran-auth curl -s http://localhost:8081/actuator/metrics/hikaricp.connections.active | jq -r '.measurements[0].value')/$(docker exec dorandoran-auth curl -s http://localhost:8081/actuator/metrics/hikaricp.connections.max | jq -r '.measurements[0].value')" && \
echo "CB 상태: $(docker exec dorandoran-auth curl -s http://localhost:8081/actuator/metrics/resilience4j.circuitbreaker.state | jq -r '.measurements[0].value' | sed 's/0/CLOSED/; s/1/OPEN/')" && \
echo "최근 에러: $(docker logs --tail 100 dorandoran-auth 2>&1 | grep -ic error)건"
```

---

## 주의사항

1. **Actuator 접근**: `/actuator` 엔드포인트가 외부에 노출되지 않도록 방화벽 설정 확인
2. **jq 설치**: JSON 파싱을 위해 `jq`가 필요합니다 (`apt-get install jq` 또는 `yum install jq`)
3. **bc 설치**: 수치 계산을 위해 `bc`가 필요합니다 (`apt-get install bc`)
4. **로그 볼륨**: 로그가 너무 많으면 `--since` 옵션으로 시간 범위 제한
5. **메트릭 리셋**: Actuator 메트릭은 애플리케이션 재시작 시 리셋됨

---

## 다음 단계

로그 기반 모니터링으로 문제를 파악한 후:
1. Grafana 대시보드 구성 (장기적 모니터링)
2. AlertManager 설정 (자동 알림)
3. 성능 프로파일링 도구 사용 (JProfiler, VisualVM 등)

