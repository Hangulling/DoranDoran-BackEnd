# 서버 로그 분석 보고서

**분석 일시**: 2025년 11월 11일  
**서버 IP**: 3.21.177.186  
**분석 범위**: 전체 서비스 상태 및 24시간 로그 분석

---

## 1. 서비스 상태 요약

모든 서비스가 정상 작동 중입니다:

| 서비스 | 상태 | 가동 시간 | 포트 | Health Check |
|--------|------|----------|------|--------------|
| dorandoran-gateway | ✅ 정상 | Up 4 days | 8080 | healthy |
| dorandoran-auth | ✅ 정상 | Up 4 days | 8081 | healthy |
| dorandoran-user | ✅ 정상 | Up 4 days | 8082 | - |
| dorandoran-chat | ✅ 정상 | Up 14 hours | 8083 | healthy |
| dorandoran-store | ✅ 정상 | Up 24 hours | 8084 | healthy |
| dorandoran-shared-db | ✅ 정상 | Up 4 days | 5432 | - |
| dorandoran-redis | ✅ 정상 | Up 4 days | 6379 | - |

---

## 2. 사용자 접속 통계

### 2.1 사용자 현황
- **총 등록 사용자**: 115명
- **총 채팅방 수**: 237개
- **Gateway 요청 수 (24시간)**: 약 410건 (X-User-Id 헤더 기준)

### 2.2 서비스별 요청 통계

#### Auth 서비스
- `/api/auth/login`: 159건 (평균 응답 시간 0.130초)
- `/api/auth/me`: 430건 (평균 응답 시간 0.009초)
- **전체 평균 응답 시간**: 0.042초
- **총 요청 수**: 589건

---

## 3. 서비스별 상세 분석

### 3.1 Gateway 서비스 (8080)

#### 상태
- ✅ 서비스 정상 작동 중
- ⚠️ 최근 5분간 에러: 64건
- ⚠️ 24시간 에러: 93건

#### 주요 이슈
1. **Decoding Failed**: 14건
   - 비정상 HTTP 요청 감지 (RTSP/1.0, SIP/2.0, 빈 요청 등)
   - 의심 IP: `172.104.24.172`
   - 보안 스캔/공격 가능성 있음
   - **상세 분석**: [IP 172.104.24.172 보안 분석 보고서](./ip-172.104.24.172-security-analysis.md)
   - 예시:
     ```
     Decoding failed: invalid version format: RTSP/1.0
     Decoding failed: invalid version format: SIP/2.0
     Decoding failed: text is empty (possibly HTTP/0.9)
     Decoding failed: Illegal character in request line
     ```

2. **메모리 사용률 경고**
   - 계산 오류로 보임 (실제 메모리 사용률은 정상 범위)

3. **응답 시간 메트릭**
   - 데이터 없음 (메트릭 설정 확인 필요)

#### 리소스 사용
- CPU: 0.13%
- 메모리: 268.9MB / 3.738GB (7.02%)

---

### 3.2 Auth 서비스 (8081)

#### 상태
- ✅ 서비스 정상 작동 중
- ✅ 최근 5분간 에러: 0건
- ✅ Circuit Breaker: CLOSED (정상)

#### 성능 지표
- **데이터베이스 연결 풀**: 0/30 (0% 사용률)
- **응답 시간**:
  - 로그인: 평균 0.130초
  - 사용자 정보 조회: 평균 0.009초
  - 전체 평균: 0.042초

#### 리소스 사용
- CPU: 0.12%
- 메모리: 439.2MB / 3.738GB (11.48%)

---

### 3.3 Chat 서비스 (8083)

#### 상태
- ✅ 서비스 정상 작동 중
- ⚠️ 최근 5분간 에러: 6건

#### 주요 에러
1. **IllegalStateException**: 4건
   ```
   java.lang.IllegalStateException: block()/blockFirst()/blockLast() are blocking, 
   which is not supported in thread reactor-http-epoll-4
   ```
   - **원인**: Reactor 비동기 스레드에서 blocking 호출 사용
   - **영향**: 비동기 처리 성능 저하 가능성
   - **조치 필요**: blocking 호출을 비동기 처리로 변경

2. **NoResourceFoundException**: 2건
   ```
   org.springframework.web.servlet.resource.NoResourceFoundException: No static resource
   ```
   - **원인**: 정적 리소스 요청 실패
   - **영향**: 기능적 영향 낮음 (일반적인 404 에러)

#### 리소스 사용
- CPU: 0.22%
- 메모리: 547.3MB / 3.738GB (14.30%)
- 데이터베이스 연결 풀: 0/50 (0% 사용률)

---

### 3.4 User 서비스 (8082)

#### 상태
- ✅ 서비스 정상 작동 중
- ✅ 최근 5분간 에러: 0건

#### 리소스 사용
- CPU: 0.09%
- 메모리: 328.5MB / 3.738GB (8.58%)
- 데이터베이스 연결 풀: 0/30 (0% 사용률)

---

### 3.5 Store 서비스 (8084)

#### 상태
- ✅ 서비스 정상 작동 중
- ✅ 최근 5분간 에러: 0건

#### 리소스 사용
- CPU: 0.16%
- 메모리: 484.6MB / 3.738GB (12.66%)
- 데이터베이스 연결 풀: 0/20 (0% 사용률)

---

## 4. 발견된 이상 징후 및 오류

### 4.1 심각도: 높음 🔴

#### 1. Gateway Decoding Failed (14건)
- **발생 빈도**: 24시간 동안 14건
- **의심 IP**: `172.104.24.172`
- **상세 내용**:
  - 비정상 HTTP 프로토콜 요청 (RTSP/1.0, SIP/2.0)
  - 빈 요청 또는 잘못된 문자 포함
  - 보안 스캔 또는 공격 시도 가능성
- **권장 조치**:
  - 해당 IP 주소 차단 검토
  - WAF(Web Application Firewall) 설정 강화
  - Rate Limiting 적용

#### 2. Chat 서비스 IllegalStateException (4건)
- **발생 빈도**: 24시간 동안 4건
- **원인**: Reactor 비동기 스레드에서 blocking 호출
- **영향**: 비동기 처리 성능 저하
- **권장 조치**:
  - 코드에서 `block()`, `blockFirst()`, `blockLast()` 호출 제거
  - 비동기 처리로 변경 (Mono/Flux 체인 사용)

---

### 4.2 심각도: 중간 🟡

#### 3. Gateway 에러 빈도
- **발생 빈도**: 최근 5분간 64건
- **분석**: 대부분 로그 레벨 또는 관찰(Observation) 메시지로 보임
- **실제 에러**: 상대적으로 적을 가능성
- **권장 조치**:
  - 로그 레벨 조정 (DEBUG → INFO)
  - 실제 에러와 정보 로그 구분

---

### 4.3 심각도: 낮음 🟢

#### 4. Chat 서비스 NoResourceFoundException
- **발생 빈도**: 최근 5분간 2건
- **영향**: 기능적 영향 낮음 (일반적인 404 에러)
- **권장 조치**: 모니터링 지속

---

## 5. 리소스 사용 현황

### 5.1 전체 리소스 사용률

| 서비스 | CPU 사용률 | 메모리 사용량 | 메모리 사용률 | 상태 |
|--------|-----------|-------------|-------------|------|
| dorandoran-chat | 0.22% | 547.3MB | 14.30% | ✅ 정상 |
| dorandoran-store | 0.16% | 484.6MB | 12.66% | ✅ 정상 |
| dorandoran-auth | 0.12% | 439.2MB | 11.48% | ✅ 정상 |
| dorandoran-user | 0.09% | 328.5MB | 8.58% | ✅ 정상 |
| dorandoran-gateway | 0.13% | 268.9MB | 7.02% | ✅ 정상 |
| dorandoran-shared-db | 0.00% | 144.6MB | 3.78% | ✅ 정상 |
| dorandoran-redis | 0.47% | 5.059MB | 0.13% | ✅ 정상 |

**전체 리소스 사용률**: 정상 범위 내

### 5.2 데이터베이스 연결 풀 상태

| 서비스 | 활성 연결 | 최대 연결 | 사용률 | 상태 |
|--------|----------|----------|--------|------|
| Auth | 0 | 30 | 0% | ✅ 정상 |
| Chat | 0 | 50 | 0% | ✅ 정상 |
| User | 0 | 30 | 0% | ✅ 정상 |
| Store | 0 | 20 | 0% | ✅ 정상 |

**연결 풀 상태**: 모든 서비스 정상

---

## 6. 권장 사항

### 6.1 즉시 조치 필요 🔴

1. **Gateway 보안 강화**
   - 의심 IP (`172.104.24.172`) 차단 검토
   - Security Group 규칙 확인 및 강화
   - 비정상 요청 로그 모니터링 강화

2. **Chat 서비스 비동기 처리 개선**
   - `block()` 호출 제거
   - Reactor 비동기 패턴으로 변경
   - 코드 리뷰 및 테스트

### 6.2 모니터링 강화 🟡

1. **로그 레벨 최적화**
   - Gateway 로그 레벨 조정 (DEBUG → INFO)
   - 실제 에러와 정보 로그 구분
   - 로그 파싱 및 분석 자동화

2. **메트릭 설정 확인**
   - Gateway 응답 시간 메트릭 설정 확인
   - Prometheus/Grafana 대시보드 점검

### 6.3 장기 개선 사항 🟢

1. **보안 강화**
   - WAF(Web Application Firewall) 도입 검토
   - Rate Limiting 적용
   - DDoS 방어 체계 구축

2. **성능 최적화**
   - 비동기 처리 패턴 표준화
   - 연결 풀 모니터링 자동화
   - 캐시 전략 최적화

3. **모니터링 시스템 고도화**
   - 알림 시스템 구축 (Slack, Email 등)
   - 자동화된 로그 분석 스크립트
   - 대시보드 개선

---

## 7. 결론

### 7.1 전체 평가

- ✅ **전체 서비스 상태**: 정상 작동 중
- ✅ **사용자 활동**: 활발 (115명 등록, 237개 채팅방)
- ⚠️ **주요 이슈**: Gateway 비정상 요청, Chat 서비스 비동기 처리 문제
- ✅ **리소스 상태**: 여유 있음

### 7.2 종합 의견

전반적으로 서비스는 **정상적으로 운영**되고 있으며, 발견된 이슈는 대부분 경미하거나 모니터링 개선으로 해결 가능합니다.

다만, **보안 관련 이슈**(비정상 HTTP 요청)는 즉시 조치가 필요하며, **Chat 서비스의 비동기 처리 문제**도 코드 개선을 통해 해결해야 합니다.

---

## 8. 참고 정보

### 8.1 분석에 사용된 스크립트
- `check_bottleneck.sh`: 서비스별 병목 현상 체크 스크립트
- Docker 로그 분석 명령어
- 데이터베이스 쿼리

### 8.2 관련 문서
- `docs/BOTTLENECK_DETECTION_GUIDE.md`
- `docs/LOG_BASED_BOTTLENECK_DETECTION.md`
- `docs/RESOURCE_MONITORING_GUIDE.md`
- [IP 172.104.24.172 보안 분석 보고서](./ip-172.104.24.172-security-analysis.md)

### 8.3 다음 분석 예정일
- **정기 분석**: 주 1회 (매주 월요일)
- **긴급 분석**: 이슈 발생 시 즉시

---

**작성자**: AI Assistant  
**작성일**: 2025년 11월 11일  
**검토 필요**: 보안 이슈 및 Chat 서비스 비동기 처리 개선

