# Grafana 대시보드 해석 가이드

## 목차
1. [DoranDoran MSA Overview](#dorandoran-msa-overview)
2. [Service Details](#service-details)
3. [Authentication Monitoring](#authentication-monitoring)
4. [Infrastructure Overview](#infrastructure-overview)
5. [일반적인 해석 방법](#일반적인-해석-방법)
6. [문제 상황 판단 기준](#문제-상황-판단-기준)

---

## DoranDoran MSA Overview

**목적**: 전체 마이크로서비스의 전반적인 상태를 한눈에 파악

### 패널 설명

#### 1. Service Health Status (서비스 헬스 상태)
- **메트릭**: `up{job=~"api-gateway|auth-service|user-service|chat-service|store-service"}`
- **의미**: 각 서비스가 실행 중인지 여부
- **해석 방법**:
  - 🟢 **1 (녹색)**: 서비스 정상 실행 중
  - 🔴 **0 (빨간색)**: 서비스 다운 또는 응답 불가
- **주의사항**: 빨간색이 보이면 즉시 확인 필요

#### 2. HTTP Request Rate (HTTP 요청률)
- **메트릭**: `rate(http_server_requests_seconds_count[5m])`
- **의미**: 초당 HTTP 요청 수
- **해석 방법**:
  - **높은 값**: 트래픽이 많음 (정상적인 경우)
  - **갑작스러운 증가**: 트래픽 급증 또는 공격 가능성
  - **0에 가까움**: 서비스 사용량이 적음
- **정상 범위**: 서비스 특성에 따라 다름 (일반적으로 0.1 ~ 100 req/s)

#### 3. Response Time (응답 시간)
- **메트릭**: 
  - p95: `histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))`
  - p50: `histogram_quantile(0.50, rate(http_server_requests_seconds_bucket[5m]))`
- **의미**: 
  - **p95**: 95%의 요청이 이 시간 이내에 처리됨
  - **p50**: 중간값 (중앙값)
- **해석 방법**:
  - **p95 < 500ms**: 매우 좋음 ✅
  - **p95 < 1s**: 양호 ✅
  - **p95 < 2s**: 보통 ⚠️
  - **p95 > 2s**: 느림 ❌ (성능 개선 필요)
- **주의사항**: p95가 p50보다 크게 벌어지면 일부 요청이 매우 느림

#### 4. Error Rate (에러율)
- **메트릭**: 
  - 5xx: `rate(http_server_requests_seconds_count{status=~"5.."}[5m])`
  - 4xx: `rate(http_server_requests_seconds_count{status=~"4.."}[5m])`
- **의미**: 
  - **5xx**: 서버 에러 (500, 502, 503 등)
  - **4xx**: 클라이언트 에러 (400, 401, 404 등)
- **해석 방법**:
  - **5xx 에러**: 서버 문제 (즉시 조치 필요) ❌
  - **4xx 에러**: 클라이언트 요청 문제 (일부는 정상)
  - **에러율 < 1%**: 정상 ✅
  - **에러율 > 5%**: 문제 발생 ⚠️
- **주의사항**: 5xx 에러가 지속되면 서비스 장애 가능성

#### 5. JVM Memory Usage (JVM 메모리 사용량)
- **메트릭**: 
  - `jvm_memory_used_bytes{area="heap"}`
  - `jvm_memory_max_bytes{area="heap"}`
- **의미**: Java 애플리케이션의 힙 메모리 사용량
- **해석 방법**:
  - **사용률 < 70%**: 여유 있음 ✅
  - **사용률 70-85%**: 주의 필요 ⚠️
  - **사용률 > 85%**: 메모리 부족 위험 ❌
- **주의사항**: 
  - 메모리 사용량이 계속 증가하면 메모리 누수 가능성
  - GC(Garbage Collection) 빈도 확인 필요

#### 6. Database Connection Pool (데이터베이스 연결 풀)
- **메트릭**: 
  - `hikaricp_connections_active`: 활성 연결 수
  - `hikaricp_connections_idle`: 유휴 연결 수
  - `hikaricp_connections_max`: 최대 연결 수
- **의미**: 데이터베이스 연결 풀의 사용 현황
- **해석 방법**:
  - **활성 연결 < 최대의 50%**: 여유 있음 ✅
  - **활성 연결 50-80%**: 정상 ✅
  - **활성 연결 > 80%**: 주의 필요 ⚠️
  - **활성 연결 ≈ 최대**: 연결 풀 고갈 위험 ❌
- **주의사항**: 
  - 연결 풀이 가득 차면 새로운 요청이 대기
  - 연결 누수 확인 필요

---

## Service Details

**목적**: 특정 서비스의 상세한 성능 메트릭 분석

### 특징
- **템플릿 변수**: 상단에서 서비스를 선택할 수 있음
- **서비스별 비교**: 각 서비스의 성능을 개별적으로 분석

### 패널 설명

#### 1. HTTP Request Rate (HTTP 요청률)
- **메트릭**: `rate(http_server_requests_seconds_count{job="$service"}[5m])`
- **의미**: 선택한 서비스의 초당 요청 수 (엔드포인트별)
- **해석 방법**:
  - **패턴 분석**: 특정 시간대에 요청이 집중되는지 확인
  - **엔드포인트별 비교**: 어떤 API가 많이 호출되는지 확인
  - **갑작스러운 변화**: 트래픽 급증/급감 원인 파악

#### 2. Response Time (p95, p50) (응답 시간)
- **메트릭**: 
  - p95: `histogram_quantile(0.95, rate(http_server_requests_seconds_bucket{job="$service"}[5m]))`
  - p50: `histogram_quantile(0.50, rate(http_server_requests_seconds_bucket{job="$service"}[5m]))`
- **의미**: 선택한 서비스의 응답 시간 분포
- **해석 방법**:
  - **p95와 p50 차이**: 분산 정도 (차이가 크면 일부 요청이 매우 느림)
  - **트렌드**: 시간에 따른 응답 시간 변화
  - **비교**: 다른 서비스와의 성능 비교

#### 3. Error Rate (에러율)
- **메트릭**: 
  - 5xx: `rate(http_server_requests_seconds_count{job="$service",status=~"5.."}[5m])`
  - 4xx: `rate(http_server_requests_seconds_count{job="$service",status=~"4.."}[5m])`
- **의미**: 선택한 서비스의 에러 발생률
- **해석 방법**:
  - **5xx 에러**: 서비스 내부 문제 (코드, DB, 외부 API 등)
  - **4xx 에러**: 잘못된 요청 (일부는 정상, 과다하면 문제)
  - **에러 패턴**: 특정 시간대나 특정 엔드포인트에서 집중되는지 확인

#### 4. JVM Memory Usage (JVM 메모리 사용량)
- **메트릭**: 
  - `jvm_memory_used_bytes{job="$service",area="heap"}`
  - `jvm_memory_max_bytes{job="$service",area="heap"}`
- **의미**: 선택한 서비스의 메모리 사용 현황
- **해석 방법**:
  - **메모리 증가 추세**: 계속 증가하면 메모리 누수 가능성
  - **서비스별 비교**: 어떤 서비스가 메모리를 많이 쓰는지
  - **GC 영향**: 메모리 사용량이 높으면 GC 빈도 증가 → 성능 저하

#### 5. Database Connection Pool (데이터베이스 연결 풀)
- **메트릭**: 
  - `hikaricp_connections_active{job="$service"}`
  - `hikaricp_connections_idle{job="$service"}`
  - `hikaricp_connections_max{job="$service"}`
- **의미**: 선택한 서비스의 DB 연결 풀 사용 현황
- **해석 방법**:
  - **활성 연결**: 실제로 사용 중인 연결
  - **유휴 연결**: 대기 중인 연결
  - **사용률**: (활성 / 최대) × 100
  - **연결 풀 고갈**: 활성 연결이 최대에 근접하면 성능 저하

#### 6. Service Health (서비스 헬스)
- **메트릭**: `up{job="$service"}`
- **의미**: 선택한 서비스의 실행 상태
- **해석 방법**:
  - **1**: 정상 실행 중 ✅
  - **0**: 다운 또는 응답 불가 ❌

---

## Authentication Monitoring

**목적**: 인증 관련 메트릭을 집중 모니터링하여 보안 및 성능 이슈 파악

### 패널 설명

#### 1. Login Attempts (로그인 시도)
- **메트릭**: `rate(http_server_requests_seconds_count{job="auth-service",uri="/api/auth/login"}[5m])`
- **의미**: 초당 로그인 시도 횟수
- **해석 방법**:
  - **정상 패턴**: 사용자 활동 시간대에 증가
  - **비정상 패턴**: 
    - 갑작스러운 급증: 공격 가능성 ⚠️
    - 비정상적인 시간대 증가: 자동화된 공격 가능성 ❌
  - **비교**: 평소 대비 급증 여부 확인

#### 2. Login Success/Failure Rate (로그인 성공/실패율)
- **메트릭**: 
  - 성공: `rate(http_server_requests_seconds_count{job="auth-service",uri="/api/auth/login",status="200"}[5m])`
  - 실패(4xx): `rate(http_server_requests_seconds_count{job="auth-service",uri="/api/auth/login",status=~"4.."}[5m])`
  - 에러(5xx): `rate(http_server_requests_seconds_count{job="auth-service",uri="/api/auth/login",status=~"5.."}[5m])`
- **의미**: 로그인 시도의 성공/실패 비율
- **해석 방법**:
  - **정상**: 성공률이 높고 실패율이 낮음 ✅
  - **주의**: 실패율이 높으면
    - 잘못된 비밀번호 입력 (정상)
    - 무차별 대입 공격 가능성 ⚠️
  - **에러**: 5xx 에러는 서버 문제 ❌
- **정상 범위**: 
  - 성공률: 70-90% (서비스 특성에 따라 다름)
  - 실패율: 10-30% (일부는 정상)

#### 3. JWT Token Validation (JWT 토큰 검증)
- **메트릭**: `rate(http_server_requests_seconds_count{job="auth-service",uri="/api/auth/validate"}[5m])`
- **의미**: 초당 JWT 토큰 검증 요청 수
- **해석 방법**:
  - **높은 값**: 많은 API 요청이 발생 중 (정상)
  - **패턴**: 사용자 활동과 일치하는지 확인
  - **비교**: 로그인 시도와의 비율 확인

#### 4. Token Validation Success/Failure (토큰 검증 성공/실패)
- **메트릭**: 
  - 성공: `rate(http_server_requests_seconds_count{job="auth-service",uri="/api/auth/validate",status="200"}[5m])`
  - 실패(4xx): `rate(http_server_requests_seconds_count{job="auth-service",uri="/api/auth/validate",status=~"4.."}[5m])`
- **의미**: JWT 토큰 검증의 성공/실패 비율
- **해석 방법**:
  - **정상**: 성공률이 매우 높음 (95% 이상) ✅
  - **주의**: 실패율이 높으면
    - 만료된 토큰 (일부는 정상)
    - 잘못된 토큰 (보안 문제 가능성) ⚠️
  - **비정상**: 실패율이 10% 이상이면 문제 가능성 ❌

#### 5. HMAC Authentication Failures (HMAC 인증 실패)
- **메트릭**: `rate(http_server_requests_seconds_count{job=~"user-service|chat-service|store-service",status="401"}[5m])`
- **의미**: 서비스 간 통신에서 HMAC 인증 실패 횟수
- **해석 방법**:
  - **정상**: 실패가 거의 없음 (0에 가까움) ✅
  - **주의**: 실패가 발생하면
    - 서비스 간 통신 문제 ⚠️
    - HMAC 키 불일치 가능성 ❌
    - 네트워크 문제 가능성
  - **비정상**: 지속적인 실패는 서비스 간 통신 장애 ❌

---

## Infrastructure Overview

**목적**: 인프라 리소스(PostgreSQL, Redis) 상태 모니터링

### 패널 설명

#### 1. PostgreSQL Connections (PostgreSQL 연결 수)
- **메트릭**: `pg_stat_database_numbackends`
- **의미**: 데이터베이스별 활성 연결 수
- **해석 방법**:
  - **정상**: 연결 수가 안정적이고 최대치 이하 ✅
  - **주의**: 연결 수가 계속 증가하면
    - 연결 누수 가능성 ⚠️
    - 트래픽 증가 (정상일 수도 있음)
  - **비정상**: 최대 연결 수에 근접하면 성능 저하 ❌
- **PostgreSQL 기본 최대 연결 수**: 100 (설정에 따라 다름)

#### 2. PostgreSQL Database Size (PostgreSQL 데이터베이스 크기)
- **메트릭**: `pg_database_size_bytes`
- **의미**: 데이터베이스별 디스크 사용량
- **해석 방법**:
  - **트렌드**: 시간에 따른 증가 추세 확인
  - **비교**: 데이터베이스별 크기 비교
  - **주의**: 급격한 증가는 데이터 누적 또는 문제 가능성 ⚠️
- **정상**: 일정한 증가율 (비즈니스 로직에 따라)

#### 3. PostgreSQL Transactions (PostgreSQL 트랜잭션)
- **메트릭**: 
  - 커밋: `rate(pg_stat_database_xact_commit[5m])`
  - 롤백: `rate(pg_stat_database_xact_rollback[5m])`
- **의미**: 초당 트랜잭션 커밋/롤백 수
- **해석 방법**:
  - **커밋**: 정상적인 트랜잭션 처리 ✅
  - **롤백**: 
    - 낮은 롤백: 정상 (일부는 정상)
    - 높은 롤백: 문제 발생 가능성 ⚠️
  - **비율**: 롤백/커밋 비율이 높으면 데이터 무결성 문제 가능성 ❌
- **정상 범위**: 롤백률 < 5%

#### 4. Redis Memory Usage (Redis 메모리 사용량)
- **메트릭**: 
  - `redis_memory_used_bytes`: 사용 중인 메모리
  - `redis_memory_max_bytes`: 최대 메모리
- **의미**: Redis의 메모리 사용 현황
- **해석 방법**:
  - **사용률 < 70%**: 여유 있음 ✅
  - **사용률 70-90%**: 주의 필요 ⚠️
  - **사용률 > 90%**: 메모리 부족 위험 ❌
- **주의사항**: 
  - 메모리 부족 시 Redis가 데이터를 제거하거나 에러 발생
  - maxmemory-policy 설정 확인 필요

#### 5. Redis Commands (Redis 명령어)
- **메트릭**: `rate(redis_commands_total[5m])`
- **의미**: 초당 Redis 명령어 실행 수 (명령어별)
- **해석 방법**:
  - **패턴**: 어떤 명령어가 많이 사용되는지 확인
  - **비교**: 명령어별 사용 빈도 비교
  - **주의**: 비효율적인 명령어 패턴 확인
- **일반적인 명령어**:
  - GET, SET: 캐시 조회/저장
  - DEL: 키 삭제
  - KEYS: 키 조회 (성능 저하 가능)

#### 6. Redis Connected Clients (Redis 연결된 클라이언트)
- **메트릭**: `redis_connected_clients`
- **의미**: Redis에 연결된 클라이언트 수
- **해석 방법**:
  - **정상**: 서비스 수와 일치하는 연결 수 ✅
  - **주의**: 연결 수가 계속 증가하면
    - 연결 누수 가능성 ⚠️
    - 불필요한 연결 생성
  - **비정상**: 예상보다 훨씬 많은 연결은 문제 ❌

#### 7. All Services Health (모든 서비스 헬스)
- **메트릭**: `up{job=~"api-gateway|auth-service|user-service|chat-service|store-service"}`
- **의미**: 모든 서비스의 실행 상태
- **해석 방법**:
  - **모두 1**: 모든 서비스 정상 ✅
  - **일부 0**: 해당 서비스 다운 ❌
- **주의사항**: 즉시 확인 및 조치 필요

---

## 일반적인 해석 방법

### 1. 트렌드 분석
- **시간대별 패턴**: 특정 시간대에 증가/감소하는 패턴 확인
- **일일/주간 패턴**: 평소와 다른 패턴 발견 시 원인 파악
- **급격한 변화**: 갑작스러운 증가/감소는 문제 신호

### 2. 임계값 기준
- **정상**: 초록색 또는 안정적인 값
- **주의**: 노란색 또는 임계값 근처
- **위험**: 빨간색 또는 임계값 초과

### 3. 상관관계 분석
- **여러 메트릭 연관성**: 예) 요청 증가 → 응답 시간 증가 → 에러 증가
- **원인 파악**: 어떤 메트릭이 먼저 변화하는지 확인

### 4. 비교 분석
- **서비스 간 비교**: 어떤 서비스가 문제인지 파악
- **시간대 비교**: 평소 대비 현재 상태 비교

---

## 문제 상황 판단 기준

### 🔴 Critical (즉시 조치 필요)

1. **서비스 다운**
   - `up{job="xxx"} == 0` 지속
   - 조치: 서비스 재시작, 로그 확인

2. **높은 에러율 (5xx)**
   - 에러율 > 10% 지속
   - 조치: 로그 확인, 코드 검토

3. **메모리 부족**
   - JVM 메모리 사용률 > 90%
   - 조치: 메모리 증가, GC 튜닝

4. **연결 풀 고갈**
   - DB 연결 풀 사용률 > 95%
   - 조치: 연결 풀 크기 증가, 연결 누수 확인

### ⚠️ Warning (주의 필요)

1. **응답 시간 증가**
   - p95 > 2초 지속
   - 조치: 성능 최적화, 병목 지점 확인

2. **에러율 증가**
   - 에러율 1-5% 지속
   - 조치: 모니터링 강화, 원인 분석

3. **메모리 사용률 증가**
   - 메모리 사용률 70-85%
   - 조치: 트렌드 모니터링, 메모리 누수 확인

4. **트래픽 급증**
   - 평소 대비 2배 이상 증가
   - 조치: 정상 트래픽인지 확인, 스케일링 검토

### ✅ Normal (정상)

1. **서비스 정상 실행**
   - 모든 서비스 `up == 1`

2. **응답 시간 양호**
   - p95 < 1초

3. **에러율 낮음**
   - 에러율 < 1%

4. **리소스 여유**
   - 메모리, 연결 풀 사용률 < 70%

---

## 실전 활용 팁

### 1. 일일 점검 체크리스트
- [ ] 모든 서비스 헬스 확인
- [ ] 에러율 확인 (5xx 에러 없음)
- [ ] 응답 시간 확인 (p95 < 2초)
- [ ] 메모리 사용률 확인 (< 85%)
- [ ] DB 연결 풀 확인 (< 80%)

### 2. 주간 리뷰
- [ ] 트래픽 트렌드 분석
- [ ] 성능 변화 추세 확인
- [ ] 리소스 사용량 추세 확인
- [ ] 알림 발생 패턴 분석

### 3. 문제 발생 시 확인 순서
1. **Service Health Status**: 어떤 서비스가 문제인가?
2. **Error Rate**: 에러가 발생하는가?
3. **Response Time**: 응답이 느린가?
4. **Resource Usage**: 리소스 부족인가?
5. **Infrastructure**: 인프라 문제인가?

---

**작성일**: 2025-11-11  
**버전**: 1.0

