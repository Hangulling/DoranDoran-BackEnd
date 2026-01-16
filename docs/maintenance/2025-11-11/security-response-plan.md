# 보안 인시던트 대응 절차

**작성일**: 2025년 11월 11일  
**대상**: IP 172.104.24.172 보안 위협 대응  
**버전**: 1.0

---

## 1. 개요

이 문서는 보안 인시던트 발생 시 즉시 대응할 수 있는 절차를 정의합니다. 특히 의심스러운 IP 주소로부터의 공격 시도에 대한 대응 방법을 포함합니다.

---

## 2. 보안 인시던트 분류

### 2.1 심각도: 높음 🔴

**특징**:
- 프로토콜 혼합 공격 (RTSP, SIP 등)
- 비정상 문자 주입 (NULL 바이트, 제어 문자)
- 짧은 시간 내 다수의 비정상 요청
- 자동화된 스캐너/봇 사용 가능성

**대응 시간**: 즉시 (1시간 이내)

### 2.2 심각도: 중간 🟡

**특징**:
- 빈 요청 시도
- 단일 비정상 요청
- 의심스러운 패턴이지만 명확하지 않음

**대응 시간**: 24시간 이내

### 2.3 심각도: 낮음 🟢

**특징**:
- 일반적인 404 에러
- 정상적인 사용자 오류
- 일회성 이벤트

**대응 시간**: 모니터링 지속

---

## 3. 즉시 대응 절차

### 3.1 1단계: 상황 파악 (5분)

```bash
# 1. 의심스러운 IP 확인
docker logs --since 24h dorandoran-gateway 2>&1 | grep "172.104.24.172"

# 2. 공격 패턴 분석
docker logs --since 24h dorandoran-gateway 2>&1 | grep -i "Decoding failed" | grep "172.104.24.172"

# 3. 공격 빈도 확인
docker logs --since 24h dorandoran-gateway 2>&1 | grep "172.104.24.172" | wc -l
```

**확인 사항**:
- [ ] 공격 유형 (프로토콜 혼합, 문자 주입, 빈 요청 등)
- [ ] 공격 빈도 (시간당 요청 수)
- [ ] 영향 범위 (어떤 서비스에 영향)
- [ ] 현재 서비스 상태 (정상 작동 여부)

### 3.2 2단계: 임시 차단 (10분)

#### 방법 1: Gateway IP 블랙리스트 필터 (권장)
```bash
# IP 블랙리스트 관리 API 사용 (구현 후)
curl -X POST http://localhost:8080/api/admin/blacklist \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{"ip": "172.104.24.172", "reason": "Protocol confusion attack"}'
```

#### 방법 2: AWS Security Group 차단
```powershell
# Windows PowerShell
.\scripts\security\block-ip-aws.ps1 -IpAddress "172.104.24.172"
```

#### 방법 3: 서버 레벨 방화벽 차단 (Linux)
```bash
# iptables 사용
sudo iptables -A INPUT -s 172.104.24.172 -j DROP
sudo iptables-save
```

### 3.3 3단계: 로그 보존 (5분)

```bash
# 공격 관련 로그 백업
docker logs --since 24h dorandoran-gateway > /tmp/attack-logs-$(date +%Y%m%d-%H%M%S).log

# 의심스러운 IP 리포트 생성
./scripts/security/monitor-suspicious-ips.sh 24 5
```

### 3.4 4단계: 영향 평가 (10분)

**확인 사항**:
- [ ] 서비스 가용성 (정상 작동 여부)
- [ ] 리소스 사용량 (CPU, 메모리)
- [ ] 데이터베이스 연결 풀 상태
- [ ] 다른 서비스 영향 여부

```bash
# 서비스 상태 확인
docker ps --format 'table {{.Names}}\t{{.Status}}'

# 리소스 사용량 확인
docker stats --no-stream

# Gateway 서비스 체크
~/check_bottleneck.sh dorandoran-gateway 8080
```

---

## 4. 단기 대응 절차 (24시간 이내)

### 4.1 상세 분석

```bash
# 보안 모니터링 스크립트 실행
./scripts/security/monitor-suspicious-ips.sh 168 5  # 최근 7일, 임계값 5건

# 유사 패턴의 다른 IP 확인
docker logs --since 7d dorandoran-gateway 2>&1 | \
  grep -i "Decoding failed" | \
  grep -oE 'R:/[0-9.]+:[0-9]+' | \
  cut -d: -f2 | \
  sort | uniq -c | sort -rn | head -20
```

### 4.2 영구 차단 적용

1. **Gateway IP 블랙리스트에 추가**
   - `application.yml`에 IP 추가
   - 또는 Redis에 영구 저장

2. **AWS Security Group 규칙 업데이트**
   - Network ACL에 Deny 규칙 추가
   - 또는 기존 Allow 규칙에서 제외

3. **모니터링 강화**
   - 자동 알림 설정
   - 정기적인 로그 검토

### 4.3 문서화

- [ ] 공격 패턴 분석 리포트 작성
- [ ] 대응 조치 내역 기록
- [ ] 향후 예방 조치 계획 수립

---

## 5. 장기 대응 절차 (1주일 이내)

### 5.1 보안 강화

1. **Rate Limiting 적용**
   - IP별 요청 수 제한
   - 비정상 패턴 자동 차단

2. **WAF 도입 검토**
   - AWS WAF 또는 Cloudflare
   - 비정상 요청 자동 차단

3. **모니터링 시스템 구축**
   - 실시간 보안 이벤트 모니터링
   - 자동화된 알림 시스템

### 5.2 정기 점검

- **주간**: 보안 로그 검토
- **월간**: 보안 정책 업데이트
- **분기**: 보안 감사 수행

---

## 6. IP 차단/해제 절차

### 6.1 IP 차단

#### Gateway 레벨 차단
```bash
# 방법 1: API 사용 (구현 후)
curl -X POST http://localhost:8080/api/admin/blacklist \
  -H "Authorization: Bearer <admin-token>" \
  -d '{"ip": "172.104.24.172", "reason": "Security threat"}'

# 방법 2: 설정 파일 수정
# gateway/src/main/resources/application.yml
security:
  blacklist:
    ips:
      - 172.104.24.172
```

#### AWS Security Group 차단
```powershell
.\scripts\security\block-ip-aws.ps1 -IpAddress "172.104.24.172"
```

### 6.2 IP 해제

```bash
# Gateway 블랙리스트에서 제거
curl -X DELETE http://localhost:8080/api/admin/blacklist/172.104.24.172 \
  -H "Authorization: Bearer <admin-token>"
```

**주의**: IP 해제 전 반드시 재발 여부 확인

---

## 7. 모니터링 및 알림

### 7.1 정기 모니터링

```bash
# 일일 모니터링 스크립트 실행
./scripts/security/monitor-suspicious-ips.sh 24 5

# 주간 리포트 생성
./scripts/security/monitor-suspicious-ips.sh 168 10 > weekly-security-report.txt
```

### 7.2 알림 설정

**알림 조건**:
- 임계값 초과 IP 발견 시
- 새로운 공격 패턴 탐지 시
- 서비스 장애 발생 시

**알림 채널** (구현 후):
- Slack
- Email
- SMS (긴급 시)

---

## 8. 관련 문서

- [IP 172.104.24.172 보안 분석 보고서](./ip-172.104.24.172-security-analysis.md)
- [서버 로그 분석 보고서](./server-log-analysis-report.md)
- [보안 그룹 설정 가이드](../../SECURITY_GROUP_SETUP.md)

---

## 9. 연락처 및 에스컬레이션

### 9.1 담당자

- **보안 담당**: [담당자 정보]
- **인프라 담당**: [담당자 정보]
- **개발 담당**: [담당자 정보]

### 9.2 에스컬레이션 기준

- **Level 1**: 일반적인 공격 시도 → 자동 차단
- **Level 2**: 서비스 영향 발생 → 담당자 알림
- **Level 3**: 대규모 공격 또는 데이터 유출 → 긴급 대응팀 소집

---

## 10. 체크리스트

### 즉시 대응 체크리스트

- [ ] 상황 파악 완료
- [ ] 임시 차단 적용
- [ ] 로그 보존 완료
- [ ] 영향 평가 완료
- [ ] 담당자 알림 완료

### 단기 대응 체크리스트

- [ ] 상세 분석 완료
- [ ] 영구 차단 적용
- [ ] 문서화 완료
- [ ] 모니터링 강화

### 장기 대응 체크리스트

- [ ] 보안 강화 조치 완료
- [ ] 정기 점검 일정 수립
- [ ] 보안 정책 업데이트

---

**작성자**: AI Assistant  
**최종 수정일**: 2025년 11월 11일  
**다음 검토일**: 2025년 12월 11일

