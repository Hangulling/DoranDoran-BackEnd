# 테스트 사용자 vs 실제 사용자 구분 방법 분석

**작성일**: 2025년 11월 19일  
**목적**: 로그 분석에서 테스트 요청과 실제 사용자 요청을 구분하는 방법 제안

---

## 현재 로그에서 확인 가능한 정보

### 1. 사용자 식별 정보

Gateway의 `JwtAuthFilter`에서 JWT 토큰을 파싱하여 다음 헤더를 추가합니다:
- `X-User-Id`: 사용자 UUID
- `X-User-Email`: 사용자 이메일
- `X-User-Name`: 사용자 이름

이 정보를 통해 특정 사용자(개발자 계정)의 요청을 필터링할 수 있습니다.

### 2. 클라이언트 정보

- `User-Agent`: 클라이언트 애플리케이션 정보
- `X-Real-IP`: 실제 클라이언트 IP 주소
- `X-Forwarded-For`: 프록시를 통한 경우 원본 IP

### 3. 요청 패턴

- API 엔드포인트
- 요청 시간대
- 요청 빈도
- HTTP 메서드

---

## 테스트 vs 실제 사용 구분 방법

### 방법 1: User-Agent 기반 구분 (가장 간단)

**테스트 도구 특징**:
- `curl/8.x.x`: 명령줄 도구
- `PostmanRuntime/x.x.x`: Postman
- `insomnia/x.x.x`: Insomnia
- `HTTPie/x.x.x`: HTTPie
- `Mozilla/5.0 ... (compatible; Googlebot/...)`: 봇

**실제 사용자 특징**:
- 브라우저 User-Agent (Chrome, Safari, Firefox 등)
- 모바일 앱 User-Agent (특정 패턴)

**구현 예시**:
```bash
# 테스트 요청 필터링
docker logs dorandoran-gateway | grep -iE "(curl|postman|insomnia|httpie)" | wc -l

# 실제 사용자 요청만 카운트
docker logs dorandoran-gateway | grep -v -iE "(curl|postman|insomnia|httpie)" | grep "Mapping \[Exchange:" | wc -l
```

### 방법 2: 특정 사용자 ID 필터링

**개발자 계정 식별**:
1. 데이터베이스에서 가장 오래된 사용자 계정 확인 (초기 개발자 계정)
2. 특정 이메일 도메인 필터링 (예: `@test.com`, `@dev.com`)
3. 특정 사용자 ID를 화이트리스트로 관리

**구현 예시**:
```bash
# 특정 사용자 ID의 요청만 필터링
docker logs dorandoran-gateway | grep "X-User-Id.*ef341a6c-62f3-4e42-8ad0-e59587ec3417"

# 특정 사용자 제외하고 카운트
docker logs dorandoran-gateway | grep "Mapping \[Exchange:" | grep -v "X-User-Id.*ef341a6c-62f3-4e42-8ad0-e59587ec3417" | wc -l
```

### 방법 3: IP 주소 기반 구분

**개발 환경 IP**:
- 로컬 개발 환경 IP (예: `127.0.0.1`, `192.168.x.x`)
- 개발자 VPN IP
- 테스트 서버 IP

**실제 사용자 IP**:
- 외부 공인 IP
- 모바일 네트워크 IP

**구현 예시**:
```bash
# 특정 IP 대역 제외
docker logs dorandoran-gateway | grep "X-Real-IP" | grep -v "192.168\|127.0.0\|10.0.0" | wc -l
```

### 방법 4: 요청 패턴 분석

**테스트 요청 특징**:
- 반복적인 패턴 (같은 엔드포인트 반복 호출)
- 비정상적인 시간대 (새벽 시간대 집중)
- 짧은 시간 내 많은 요청
- 특정 엔드포인트만 집중 호출

**실제 사용자 특징**:
- 다양한 엔드포인트 호출
- 자연스러운 시간대 분포
- 일정한 간격의 요청

**구현 예시**:
```bash
# 같은 엔드포인트를 10회 이상 호출한 사용자 찾기
docker logs dorandoran-gateway | grep "Mapping \[Exchange:" | grep -oE "X-User-Id:\"[^\"]+\"" | sort | uniq -c | sort -rn | awk '$1 > 10'
```

### 방법 5: 시간대 기반 구분

**테스트 요청 시간대**:
- 새벽 시간대 (03:00 ~ 08:00) 집중
- 평일 업무 시간 외 집중

**실제 사용자 시간대**:
- 자연스러운 시간대 분포
- 저녁 시간대 집중 (일반적인 사용 패턴)

---

## 권장 구현 방법

### 단계 1: User-Agent 기반 필터링 (즉시 적용 가능)

가장 간단하고 효과적인 방법입니다. 테스트 도구는 대부분 특정 User-Agent를 사용합니다.

**스크립트 예시**:
```bash
#!/bin/bash
# test-vs-production-filter.sh

DATE="2025-11-19"

echo "=== 테스트 요청 분석 ==="
TEST_REQUESTS=$(docker logs --since 24h dorandoran-gateway 2>&1 | strings | grep "$DATE" | grep "Mapping \[Exchange:" | grep -iE "(curl|postman|insomnia|httpie)" | wc -l)
echo "테스트 요청 수: $TEST_REQUESTS"

echo ""
echo "=== 실제 사용자 요청 분석 ==="
PROD_REQUESTS=$(docker logs --since 24h dorandoran-gateway 2>&1 | strings | grep "$DATE" | grep "Mapping \[Exchange:" | grep -v -iE "(curl|postman|insomnia|httpie)" | wc -l)
echo "실제 사용자 요청 수: $PROD_REQUESTS"

echo ""
echo "=== 비율 ==="
TOTAL=$((TEST_REQUESTS + PROD_REQUESTS))
if [ $TOTAL -gt 0 ]; then
    TEST_PERCENT=$((TEST_REQUESTS * 100 / TOTAL))
    PROD_PERCENT=$((PROD_REQUESTS * 100 / TOTAL))
    echo "테스트: ${TEST_PERCENT}%"
    echo "실제 사용자: ${PROD_PERCENT}%"
fi
```

### 단계 2: 사용자 ID 기반 필터링 (추가 구현)

개발자 계정을 식별하여 해당 계정의 요청을 필터링합니다.

**구현 방법**:
1. 데이터베이스에서 초기 사용자 계정 확인
2. 설정 파일에 테스트 사용자 ID 목록 추가
3. 로그 분석 시 해당 사용자 제외

### 단계 3: 종합 분석 스크립트

여러 방법을 조합하여 더 정확한 분석을 수행합니다.

---

## 향후 개선 사항

### 1. 로깅 강화

현재 Gateway 로그에 User-Agent 정보가 포함되어 있는지 확인이 필요합니다. 포함되지 않는다면 로깅을 강화해야 합니다.

**제안**: `ObservedRequestHttpHeadersFilter`에서 이미 헤더를 로깅하고 있으므로, User-Agent 정보를 확인할 수 있을 것입니다.

### 2. 테스트 사용자 마킹

애플리케이션 레벨에서 테스트 계정을 명시적으로 마킹:
- 사용자 테이블에 `is_test_account` 플래그 추가
- 테스트 계정 요청에 `X-Test-Request: true` 헤더 추가

### 3. 자동 분석 도구

로그 분석 스크립트를 자동화하여 일일 리포트에 테스트/실제 사용자 비율을 포함:
- 테스트 요청 비율
- 실제 사용자 요청 수
- 주요 실제 사용자 활동

---

## 결론

현재 가장 간단하고 효과적인 방법은 **User-Agent 기반 필터링**입니다. 테스트 도구들은 대부분 특정 User-Agent를 사용하므로 이를 통해 대부분의 테스트 요청을 구분할 수 있습니다.

추가로 **사용자 ID 기반 필터링**을 구현하면 더 정확한 분석이 가능합니다.

---

**다음 단계**: 실제 로그를 분석하여 테스트 요청과 실제 사용자 요청의 비율을 확인하는 스크립트를 작성하겠습니다.

