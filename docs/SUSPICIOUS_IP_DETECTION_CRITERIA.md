# 위험 IP 식별 기준 및 프로세스

**작성일**: 2025-01-15  
**적용 범위**: DoranDoran 프로젝트 전체

---

## 1. 위험 IP 식별 기준

### 1.1 자동 감지 기준 (로그 패턴 기반)

현재 프로젝트에서는 **Gateway 로그에서 특정 에러 패턴**을 감지하여 위험 IP를 식별합니다.

#### 1.1.1 프로토콜 혼동 공격 (Protocol Confusion Attack)

**감지 패턴**:
```
- "invalid version format: HTTP/0.9"
- "invalid version format: HTTP/1.0"
- "invalid version format: HTTP/2.0"
- "invalid version format: RTSP/1.0"
- "invalid version format: SIP/2.0"
- "text is empty (possibly HTTP/0.9)"
```

**의미**:
- 비표준 HTTP 프로토콜 버전 사용
- 오래된 프로토콜(HTTP/0.9) 사용 시도
- 다른 프로토콜(RTSP, SIP)을 HTTP 서버에 전송

**위험도**: 🟡 중간

**실제 사례**:
- `172.104.24.172`: RTSP/1.0, SIP/2.0 프로토콜 혼동
- `61.138.228.132`: HTTP/0.9 프로토콜 혼동 (13건)
- `206.168.34.122`: HTTP/2.0 프로토콜 혼동

#### 1.1.2 제어 문자 주입 공격 (Control Character Injection)

**감지 패턴**:
```
- "Illegal character in request line: 0x0" (NULL 바이트)
- "Illegal character in request line: 0x1" (SOH)
- "Illegal character in request line: 0x3" (ETX)
- "Illegal character in request line: 0x7"
```

**의미**:
- HTTP 요청 라인에 제어 문자 주입
- 버퍼 오버플로우 취약점 탐지 시도
- 파서 취약점 탐지 시도

**위험도**: 🔴 높음

**실제 사례**:
- `45.79.149.214`: NULL(0x0), SOH(0x1), ETX(0x3) 주입 (5건)
- `134.122.4.76`: 제어 문자 사용 (2건)
- `3.130.96.91`: 제어 문자 0x7 사용 (3건)

#### 1.1.3 Command Injection 시도

**감지 패턴**:
```
- "/cgi-bin/;wget${IFS}-qO-${IFS}http://..."
- "Command injection" 키워드
- 쉘 명령어 패턴 (wget, curl, sh 등)
```

**의미**:
- 서버에서 명령어 실행 시도
- 원격 코드 실행(RCE) 공격 시도
- 매우 심각한 공격 유형

**위험도**: 🔴 매우 높음

**실제 사례**:
- `198.235.24.209`: Command injection 시도
- `193.26.115.195`: Command injection 시도 (3건 - 심각)

#### 1.1.4 Decoding Failed 오류

**감지 패턴**:
```
- "Decoding failed: invalid version format"
- "Decoding failed: ..."
```

**의미**:
- Gateway에서 HTTP 요청 디코딩 실패
- 비정상적인 요청 형식
- 공격 시도 또는 스캔 시도

**위험도**: 🟡 중간

**실제 사례**:
- `142.93.3.113`: Decoding failed attacks
- `3.137.73.221`: Decoding failed (1건)
- `23.162.40.89`: Decoding failed (1건)

---

### 1.2 수동 분석 기준 (빈도 및 패턴 기반)

#### 1.2.1 공격 빈도 기준

**높은 위험도** (즉시 차단):
- **10건 이상**: 짧은 시간 내 집중 공격
- **5건 이상**: 다양한 공격 패턴 혼용
- **3건 이상**: Command injection 등 심각한 공격

**중간 위험도** (모니터링 후 차단):
- **3건 이상**: 동일한 공격 패턴 반복
- **2건 이상**: 제어 문자 주입 등 위험한 패턴

**낮은 위험도** (일회성 스캔):
- **1건**: 단일 시도, 추가 공격 없음

**실제 사례**:
- `45.79.149.214`: 10건 (가장 많은 공격) → 즉시 차단
- `61.138.228.132`: 13건 (가장 많은 공격) → 즉시 차단
- `138.68.143.86`: 12건 (가장 많은 공격) → 즉시 차단
- `161.35.56.30`: 12건 (대량 공격) → 즉시 차단

#### 1.2.2 공격 패턴 다양성

**높은 위험도**:
- 여러 공격 기법 혼용 (프로토콜 혼동 + 제어 문자 주입)
- 짧은 시간 내 다양한 패턴 시도
- 자동화된 공격 도구 사용 의심

**실제 사례**:
- `45.79.149.214`: 
  - RTSP/1.0, SIP/2.0, HTTP/0.9 프로토콜 혼동
  - 제어 문자 주입 (NULL, SOH, ETX)
  - 약 1분간 10건의 집중 공격
  - → **자동화된 공격 도구 사용 의심**

#### 1.2.3 시간대별 집중 공격

**높은 위험도**:
- 짧은 시간 내 집중 공격 (예: 1분 내 10건)
- 짧은 간격으로 다양한 패턴 시도

**실제 사례**:
- `45.79.149.214`: 14:49:48 ~ 14:50:39 (약 1분간 10건)
- `61.138.228.132`: 22시 시간대에 13건 집중

---

## 2. IP 식별 프로세스

### 2.1 현재 프로세스 (수동)

**1단계: 로그 수집**
```bash
# Gateway 로그에서 Decoding failed 오류 추출
docker logs --since 24h dorandoran-gateway 2>&1 | grep -i "Decoding failed"

# 특정 패턴 검색
docker logs --since 24h dorandoran-gateway 2>&1 | grep -E "HTTP/0\.9|HTTP/1\.0|HTTP/2\.0|RTSP|SIP|Illegal character|Command injection"
```

**2단계: IP 추출 및 통계**
```bash
# IP별 발생 건수 집계
docker logs --since 24h dorandoran-gateway 2>&1 | grep -i "Decoding failed" | \
  grep -oE '[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}' | \
  sort | uniq -c | sort -rn
```

**3단계: 패턴 분석**
- 각 IP의 공격 유형 분류
- 공격 빈도 및 시간대 분석
- 위험도 평가

**4단계: 블랙리스트 추가**
- `application.yml`에 IP 추가
- Gateway 재배포

### 2.2 로그 패턴 예시

**실제 로그 예시**:
```
2025-11-19 14:49:48 - Decoding failed: invalid version format: RTSP/1.0
  IP: 45.79.149.214

2025-11-19 14:49:48 - Decoding failed: Illegal character in request line: 0x3
  IP: 45.79.149.214

2025-11-19 14:50:08 - Decoding failed: text is empty (possibly HTTP/0.9)
  IP: 45.79.149.214
```

**분석 결과**:
- IP: `45.79.149.214`
- 공격 건수: 10건
- 공격 유형: 프로토콜 혼동 + 제어 문자 주입
- 위험도: 🔴 매우 높음
- 조치: 블랙리스트 추가

---

## 3. 위험도 평가 매트릭스

### 3.1 위험도 계산 기준

| 공격 유형 | 단일 시도 | 2-4건 | 5-9건 | 10건 이상 |
|----------|----------|-------|-------|-----------|
| **Command Injection** | 🔴 높음 | 🔴 매우 높음 | 🔴 매우 높음 | 🔴 매우 높음 |
| **제어 문자 주입** | 🟡 중간 | 🟡 중간 | 🔴 높음 | 🔴 매우 높음 |
| **프로토콜 혼동** | 🟢 낮음 | 🟡 중간 | 🟡 중간 | 🔴 높음 |
| **Decoding Failed** | 🟢 낮음 | 🟡 중간 | 🟡 중간 | 🔴 높음 |

### 3.2 종합 위험도 평가

**매우 높음 (즉시 차단)**:
- Command Injection 시도 (1건 이상)
- 제어 문자 주입 5건 이상
- 프로토콜 혼동 + 제어 문자 주입 혼용 (3건 이상)
- 10건 이상의 공격 시도

**높음 (모니터링 후 차단)**:
- 제어 문자 주입 3건 이상
- 프로토콜 혼동 5건 이상
- 다양한 공격 패턴 혼용 (3건 이상)

**중간 (모니터링)**:
- 프로토콜 혼동 2-4건
- Decoding Failed 3건 이상
- 단일 제어 문자 주입

**낮음 (일회성 스캔)**:
- 단일 프로토콜 혼동 시도
- 단일 Decoding Failed
- 추가 공격 시도 없음

---

## 4. 실제 식별 사례

### 4.1 사례 1: `45.79.149.214` (2025-11-19)

**발견 경로**:
- Gateway 로그에서 "Decoding failed" 오류 다수 발견
- 시간대별 집중 공격 확인

**공격 패턴**:
- RTSP/1.0 프로토콜 혼동: 1건
- SIP/2.0 프로토콜 혼동: 1건
- HTTP/0.9 프로토콜 혼동: 2건
- 제어 문자 주입 (NULL, SOH, ETX): 5건
- 총 10건 (약 1분간 집중 공격)

**위험도 평가**: 🔴 매우 높음
- 가장 많은 공격 시도 (10건)
- 다양한 공격 패턴 혼용
- 자동화된 공격 도구 사용 의심

**조치**: 즉시 블랙리스트 추가

### 4.2 사례 2: `193.26.115.195` (2025-11-14)

**발견 경로**:
- Gateway 로그에서 Command Injection 패턴 발견

**공격 패턴**:
- Command Injection 시도: 3건
- 경로: `/cgi-bin/;wget${IFS}-qO-${IFS}http://...`

**위험도 평가**: 🔴 매우 높음
- Command Injection은 매우 심각한 공격
- 3건의 반복 시도

**조치**: 즉시 블랙리스트 추가

### 4.3 사례 3: `61.138.228.132` (2025-11-13)

**발견 경로**:
- 일일 로그 분석 리포트에서 발견
- IP별 통계에서 가장 많은 공격 시도 확인

**공격 패턴**:
- HTTP/0.9 프로토콜 혼동: 다수
- 총 13건 (가장 많은 공격)

**위험도 평가**: 🔴 높음
- 가장 많은 공격 시도 (13건)
- 지속적인 공격 시도

**조치**: 즉시 블랙리스트 추가

---

## 5. 자동화 개선 방안

### 5.1 현재 한계

**수동 프로세스**:
- 로그 분석 후 수동으로 IP 식별
- `application.yml`에 수동 추가
- Gateway 재배포 필요

**문제점**:
- 실시간 대응 어려움
- 공격 발생 후 사후 대응
- 누락 가능성

### 5.2 자동화 제안

**1. 실시간 로그 모니터링**:
```java
// Gateway 필터에서 실시간 감지
@Slf4j
@Component
public class SuspiciousIpDetectionFilter implements GlobalFilter {
    
    private final Map<String, Integer> suspiciousIpCount = new ConcurrentHashMap<>();
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, ...) {
        // Decoding failed 오류 발생 시
        if (isDecodingFailedError(exchange)) {
            String clientIp = extractClientIp(exchange);
            int count = suspiciousIpCount.merge(clientIp, 1, Integer::sum);
            
            // 5건 이상 시 자동 차단
            if (count >= 5) {
                ipBlacklistFilter.addToBlacklist(clientIp);
                log.warn("의심 IP 자동 차단: {} ({}건)", clientIp, count);
            }
        }
        return chain.filter(exchange);
    }
}
```

**2. 패턴 기반 자동 감지**:
- Command Injection 패턴: 즉시 차단
- 제어 문자 주입 3건 이상: 자동 차단
- 프로토콜 혼동 10건 이상: 자동 차단

**3. Redis 기반 분산 카운터**:
- IP별 공격 횟수를 Redis에 저장
- TTL 설정으로 시간 윈도우 관리
- 임계값 초과 시 자동 차단

**4. 알림 시스템**:
- 의심 IP 발견 시 즉시 알림 (Slack, Email)
- 위험도별 알림 레벨 구분

---

## 6. 블랙리스트 관리

### 6.1 현재 관리 방식

**설정 파일 기반**:
```yaml
gateway:
  security:
    blacklist:
      ips: "172.104.24.172, 142.93.3.113, ..."
```

**특징**:
- 메모리에만 저장 (서버 재시작 시 유지)
- 설정 파일 변경 후 재배포 필요
- 영구 저장 안 됨

### 6.2 향후 개선 방안

**1. Redis 기반 영구 저장**:
- 블랙리스트 IP를 Redis에 저장
- TTL 설정으로 자동 만료
- 동적 추가/제거 가능

**2. 데이터베이스 저장**:
- `auth_schema.ip_blacklist` 테이블 생성
- 영구 저장 및 이력 관리
- 관리자 API로 동적 관리

**3. 관리자 API**:
- IP 추가/제거 API
- 블랙리스트 조회 API
- 차단 이력 조회 API

---

## 7. 모니터링 및 알림

### 7.1 현재 모니터링

**수동 모니터링**:
- 일일 로그 분석 리포트
- 수동 로그 검색

**자동 모니터링**:
- Prometheus 메트릭: 400/505 에러율 추적
- Grafana 대시보드: 에러율 시각화

### 7.2 개선 방안

**1. 실시간 알림**:
- 의심 IP 발견 시 즉시 알림
- 위험도별 알림 레벨

**2. 대시보드 구축**:
- 의심 IP 목록 대시보드
- 공격 패턴 시각화
- 시간대별 공격 통계

**3. 자동 리포트**:
- 일일 의심 IP 리포트 자동 생성
- 주간 보안 리포트

---

## 8. 식별 기준 요약

### 8.1 즉시 차단 기준

1. **Command Injection 시도** (1건 이상)
2. **제어 문자 주입 5건 이상**
3. **10건 이상의 공격 시도**
4. **프로토콜 혼동 + 제어 문자 주입 혼용** (3건 이상)

### 8.2 모니터링 후 차단 기준

1. **제어 문자 주입 3-4건**
2. **프로토콜 혼동 5-9건**
3. **다양한 공격 패턴 혼용** (3건 이상)

### 8.3 모니터링 대상

1. **프로토콜 혼동 2-4건**
2. **Decoding Failed 3건 이상**
3. **단일 제어 문자 주입**

---

## 9. 실제 차단된 IP 통계

**총 차단 IP 수**: 약 80개 (2025-11-30 기준)

**공격 유형별 통계**:
- HTTP/0.9 프로토콜 혼동: 약 60%
- 제어 문자 주입: 약 20%
- Command Injection: 약 5%
- 기타 (HTTP/2.0, RTSP, SIP 등): 약 15%

**지역별 통계**:
- 해외 IP: 약 90%
- 국내 IP: 약 10%

---

## 10. 참고 자료

- [IP 블랙리스트 필터 구현](../gateway/src/main/java/com/dorandoran/gateway/filter/IpBlacklistFilter.java)
- [보안 위협 분석](./SECURITY_THREAT_ANALYSIS.md)
- [서버 로그 분석 리포트](./maintenance/2025-11-19/suspicious-ip-activity-report.md)

---

**문서 작성 완료**: 2025-01-15
















