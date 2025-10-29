# Redis 캐시 성능 측정 가이드

## 개요

Redis 캐시 성능 측정 시스템은 Spring Boot Actuator와 커스텀 엔드포인트를 활용하여 캐시의 효율성을 정량적으로 측정하고 모니터링할 수 있는 시스템입니다.

## 측정 방법

### 1. 커스텀 캐시 통계 엔드포인트

#### 엔드포인트
```
GET /actuator/custom/cache-stats
```

#### 응답 예시
```json
{
  "summary": {
    "totalGets": 1234,
    "totalHits": 1056,
    "totalPuts": 567,
    "avgHitRatio": "85.58%",
    "cacheCount": 5
  },
  "cacheDetails": {
    "prompts": {
      "gets": 500,
      "hits": 425,
      "misses": 75,
      "puts": 75,
      "evictions": 0,
      "hitRatio": "85.00%",
      "missRatio": "15.00%"
    },
    "intimacy": {
      "gets": 300,
      "hits": 270,
      "misses": 30,
      "puts": 30,
      "evictions": 0,
      "hitRatio": "90.00%",
      "missRatio": "10.00%"
    },
    "messageHistory": {
      "gets": 434,
      "hits": 361,
      "misses": 73,
      "puts": 73,
      "evictions": 5,
      "hitRatio": "83.18%",
      "missRatio": "16.82%"
    }
  },
  "timestamp": 1703123456789
}
```

### 2. 캐시 성능 로깅

#### 로그 출력 예시
```
[CACHE-HIT] PromptService.buildSystemPrompt(chatroomId=7a817861-dfff-4611-bb88-b1fb51af7261) - 2ms - Cache: prompts
[CACHE-MISS] PromptService.buildSystemPrompt(chatroomId=8b928972-eeff-5722-cc99-c2gc62bg8372) - 85ms - Cache: prompts
[CACHE-MISS] 캐시 저장됨 - PromptService.buildSystemPrompt(chatroomId=8b928972-eeff-5722-cc99-c2gc62bg8372) - 85ms - Cache: prompts
```

#### 로그 레벨별 정보
- **INFO**: 기본 캐시 성능 정보
- **DEBUG**: 상세 실행 정보 (나노초 단위, 인자 상세)
- **WARN**: 느린 캐시 작업 경고 (100ms 초과)

### 3. 성능 테스트 스크립트

#### 실행 방법
```powershell
# 기본 실행
.\scripts\test-cache-performance.ps1

# 커스텀 파라미터로 실행
.\scripts\test-cache-performance.ps1 -chatroomId "your-chatroom-id" -testCount 10 -baseUrl "https://your-api-url.com"
```

#### 출력 예시
```
=== Redis 캐시 성능 테스트 시작 ===
채팅방 ID: 7a817861-dfff-4611-bb88-b1fb51af7261
테스트 횟수: 5
API URL: https://api.doran-chat.com

요청 #1 실행 중...
  요청 #1 (CACHE-MISS): 156ms
요청 #2 실행 중...
  요청 #2 (CACHE-HIT): 3ms
요청 #3 실행 중...
  요청 #3 (CACHE-HIT): 2ms
요청 #4 실행 중...
  요청 #4 (CACHE-HIT): 2ms
요청 #5 실행 중...
  요청 #5 (CACHE-HIT): 3ms

=== 테스트 결과 분석 ===
총 요청 수: 5
성공한 요청 수: 5
실패한 요청 수: 0
평균 응답 시간: 33.2 ms

=== 캐시 성능 분석 ===
첫 번째 요청 (캐시 미스): 156 ms
평균 캐시 히트 시간: 2.5 ms
성능 개선율: 98.4%
성능 등급: 우수 (A)

=== 요청별 상세 결과 ===
요청# | 응답시간(ms) | 상태      | 성공
------|-------------|-----------|-----
    1 |         156 | CACHE-MISS| ✓
    2 |           3 | CACHE-HIT | ✓
    3 |           2 | CACHE-HIT | ✓
    4 |           2 | CACHE-HIT | ✓
    5 |           3 | CACHE-HIT | ✓
```

## 캐시별 성능 지표

### 1. System Prompts (`prompts`)

#### 목표 지표
- **캐시 히트율**: 85% 이상
- **응답 시간 개선**: 95% 이상 단축
- **TTL**: 10분

#### 측정 방법
```java
@Cacheable(value = "prompts", key = "#chatroomId", unless = "#result == null || #result.isEmpty()")
public String buildSystemPrompt(UUID chatroomId) {
    // 복잡한 프롬프트 빌드 로직
}
```

#### 예상 성능
- **캐시 미스**: 80-120ms (DB 조회 + 프롬프트 빌드)
- **캐시 히트**: 2-5ms (Redis 조회)
- **성능 개선**: 95-98%

### 2. Intimacy Progress (`intimacy`)

#### 목표 지표
- **캐시 히트율**: 90% 이상
- **응답 시간 개선**: 90% 이상 단축
- **TTL**: 15분

#### 측정 방법
```java
@Cacheable(value = "intimacy", key = "#chatroomId", unless = "#result == null")
public int getCurrentIntimacyLevel(UUID chatroomId) {
    return intimacyProgressRepository.findByChatRoomId(chatroomId)
        .map(IntimacyProgress::getIntimacyLevel)
        .orElse(2);
}
```

#### 예상 성능
- **캐시 미스**: 20-40ms (DB 조회)
- **캐시 히트**: 1-3ms (Redis 조회)
- **성능 개선**: 90-95%

### 3. Message History (`messageHistory`)

#### 목표 지표
- **캐시 히트율**: 70% 이상
- **응답 시간 개선**: 95% 이상 단축
- **TTL**: 3분

#### 측정 방법
```java
@Cacheable(value = "messageHistory", key = "#chatroomId", unless = "#result == null || #result.isEmpty()")
private List<Map<String, String>> buildMessageHistory(UUID chatroomId) {
    // 메시지 히스토리 빌드 로직
}
```

#### 예상 성능
- **캐시 미스**: 50-100ms (DB 조회 + 메시지 변환)
- **캐시 히트**: 2-5ms (Redis 조회)
- **성능 개선**: 95-98%

## 주요 지표 해석

### 1. 캐시 히트율 (Cache Hit Ratio)

#### 정의
```
히트율 = 캐시 히트 수 / (캐시 히트 수 + 캐시 미스 수) × 100
```

#### 해석 기준
- **90% 이상**: 우수 - 캐시가 매우 효과적으로 작동
- **80-89%**: 양호 - 캐시가 잘 작동하고 있음
- **70-79%**: 보통 - 캐시 효과가 있지만 개선 여지 있음
- **60-69%**: 미흡 - 캐시 설정이나 TTL 조정 필요
- **60% 미만**: 불량 - 캐시 전략 재검토 필요

### 2. 응답 시간 개선율

#### 정의
```
개선율 = (캐시 미스 시간 - 캐시 히트 시간) / 캐시 미스 시간 × 100
```

#### 해석 기준
- **95% 이상**: 우수 - 거의 즉시 응답
- **90-94%**: 양호 - 상당한 성능 개선
- **80-89%**: 보통 - 의미 있는 개선
- **70-79%**: 미흡 - 개선 효과 제한적
- **70% 미만**: 불량 - 캐시 효과 미미

### 3. 캐시 메모리 사용량

#### 모니터링 방법
```bash
# Redis 메모리 사용량 확인
redis-cli info memory

# 특정 캐시 키 개수 확인
redis-cli --scan --pattern "prompts:*" | wc -l
```

#### 최적화 기준
- **메모리 사용률**: 70% 이하 유지
- **키 개수**: 캐시별 적절한 수준 유지
- **만료율**: 과도한 만료 방지

## TTL 최적화 가이드

### 1. 현재 TTL 설정

| 캐시 이름 | TTL | 이유 |
|-----------|-----|------|
| `prompts` | 10분 | 프롬프트 변경 빈도 낮음 |
| `intimacy` | 15분 | 친밀도 변화 속도 고려 |
| `messageHistory` | 3분 | 실시간성 중요 |
| `users` | 2시간 | 사용자 정보 안정적 |
| `chatbots` | 2시간 | 챗봇 정보 안정적 |

### 2. TTL 조정 기준

#### 증가 고려사항
- 캐시 히트율이 목표치 미달
- 데이터 변경 빈도가 낮음
- 메모리 사용량이 여유로움

#### 감소 고려사항
- 데이터 일관성 문제 발생
- 메모리 사용량이 높음
- 실시간성 요구사항 강화

### 3. 동적 TTL 설정

```java
// 캐시별 동적 TTL 설정 예시
@Cacheable(value = "prompts", key = "#chatroomId")
public String buildSystemPrompt(UUID chatroomId) {
    // 활성 채팅방은 더 긴 TTL 적용
    boolean isActive = isActiveChatRoom(chatroomId);
    return buildPrompt(chatroomId, isActive);
}
```

## 모니터링 방법

### 1. 실시간 모니터링

#### Spring Boot Actuator 활용
```bash
# 캐시 통계 조회
curl http://localhost:8080/actuator/custom/cache-stats

# 캐시 헬스 체크
curl http://localhost:8080/actuator/custom/cache-health

# 전체 메트릭 조회
curl http://localhost:8080/actuator/metrics
```

### 2. 로그 기반 모니터링

#### 로그 패턴 분석
```bash
# 캐시 히트율 분석
grep "CACHE-HIT" application.log | wc -l
grep "CACHE-MISS" application.log | wc -l

# 느린 캐시 작업 분석
grep "PERFORMANCE-WARNING" application.log
```

### 3. 알림 설정

#### 성능 임계값 설정
- 캐시 히트율 < 70%: 경고
- 응답 시간 > 100ms: 경고
- 캐시 에러 발생: 즉시 알림

## 성능 최적화 권장사항

### 1. 캐시 키 최적화

#### 현재 키 구조
```java
// 단순한 키 구조 사용
@Cacheable(value = "prompts", key = "#chatroomId")
```

#### 개선 방안
```java
// 복합 키 구조 (필요시)
@Cacheable(value = "prompts", key = "#chatroomId + ':' + #intimacyLevel")
```

### 2. 캐시 전략 최적화

#### Cache-Aside 패턴
```java
@Cacheable(value = "prompts", key = "#chatroomId")
public String getPrompt(UUID chatroomId) {
    // 캐시에서 먼저 조회, 없으면 DB에서 조회 후 캐시 저장
    return promptRepository.findByChatRoomId(chatroomId);
}
```

#### Write-Through 패턴
```java
@CacheEvict(value = "prompts", key = "#chatroomId")
public void updatePrompt(UUID chatroomId, String newPrompt) {
    // DB 업데이트와 동시에 캐시 무효화
    promptRepository.updateByChatRoomId(chatroomId, newPrompt);
}
```

### 3. 메모리 최적화

#### 직렬화 최적화
```java
// Jackson 설정으로 메모리 사용량 최적화
objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
```

#### 압축 설정
```yaml
# application.yml
spring:
  redis:
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
```

## 문제 해결

### 1. 캐시 히트율이 낮은 경우

#### 원인 분석
- TTL이 너무 짧음
- 캐시 키가 너무 구체적
- 데이터 변경 빈도가 높음

#### 해결 방안
- TTL 증가
- 캐시 키 단순화
- 캐시 전략 재검토

### 2. 메모리 사용량이 높은 경우

#### 원인 분석
- TTL이 너무 김
- 캐시된 데이터가 너무 큼
- 불필요한 데이터 캐싱

#### 해결 방안
- TTL 감소
- 캐시 데이터 크기 최적화
- 캐시 대상 재선정

### 3. 캐시 일관성 문제

#### 원인 분석
- 캐시 무효화 로직 부족
- 동시성 문제
- 네트워크 지연

#### 해결 방안
- `@CacheEvict` 적절히 활용
- 분산 락 사용
- 캐시 무효화 전략 개선

## 향후 개선 계획

### 1. 고급 모니터링
- Grafana 대시보드 구축
- Prometheus 메트릭 수집
- 실시간 알림 시스템

### 2. 캐시 최적화
- 캐시 워밍업 전략
- 예측적 캐싱
- 캐시 계층화

### 3. 성능 분석
- A/B 테스트를 통한 캐시 효과 검증
- 부하 테스트를 통한 캐시 성능 측정
- 비용-효과 분석

## 참고 자료

- [Spring Cache Documentation](https://docs.spring.io/spring-framework/docs/current/reference/html/integration.html#cache)
- [Redis Performance Tuning](https://redis.io/docs/management/optimization/)
- [Micrometer Metrics](https://micrometer.io/docs/concepts)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
