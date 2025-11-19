# 블랙리스트 IP 관리 Redis 개선 방안

**작성일**: 2025년 11월 13일  
**목적**: application-docker.yml 하드코딩 방식에서 Redis 기반 영구 저장으로 개선

---

## 1. 현재 방식의 한계

### 1.1 문제점

**현재**: `application-docker.yml`에 직접 하드코딩
```yaml
gateway:
  security:
    blacklist:
      ips: "172.104.24.172, 142.93.3.113"
```

**문제**:
1. ❌ IP 추가/제거 시마다 코드 수정 및 재배포 필요
2. ❌ 런타임에 API로 추가한 IP는 서버 재시작 시 사라짐
3. ❌ 설정 파일에 보안 정보 하드코딩
4. ❌ IP가 많아지면 관리 어려움

---

## 2. Redis 기반 개선 방안

### 2.1 개선 효과

✅ **장점**:
- **영구 저장**: 서버 재시작 후에도 유지
- **런타임 관리**: 코드 변경 없이 API로 즉시 추가/제거
- **확장성**: 많은 IP 관리 가능
- **성능**: Redis의 빠른 읽기/쓰기
- **분산 환경**: 여러 Gateway 인스턴스에서 공유 가능

### 2.2 Redis 저장 구조

```
Key: gateway:blacklist:ips
Type: Set
Value: ["172.104.24.172", "142.93.3.113", ...]

추가 정보 (선택):
Key: gateway:blacklist:ip:{ip}:info
Type: Hash
Value: {
  "ip": "142.93.3.113",
  "reason": "Decoding failed attacks",
  "created_at": "2025-11-12T03:07:29",
  "created_by": "admin"
}
```

### 2.3 구현 계획

#### Phase 1: 기본 Redis 연동

**1. 의존성 확인**
- Gateway에 이미 Redis 설정이 있음 (`application-docker.yml`)
- `ReactiveRedisTemplate` 추가 필요

**2. IpBlacklistFilter 수정**
```java
@Service
public class IpBlacklistFilter implements GlobalFilter, Ordered {
    private final Set<String> blacklistedIps = ConcurrentHashMap.newKeySet();
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private static final String REDIS_KEY = "gateway:blacklist:ips";
    
    @PostConstruct
    public void init() {
        // 1. application.yml에서 초기 IP 로드
        loadInitialIps();
        
        // 2. Redis에서 전체 IP 로드
        loadFromRedis().block();
        
        // 3. 초기 IP를 Redis에 저장 (없으면)
        saveInitialIpsToRedis();
    }
    
    private void loadFromRedis() {
        return redisTemplate.opsForSet()
            .members(REDIS_KEY)
            .doOnNext(ip -> {
                blacklistedIps.add(ip);
                log.debug("Redis에서 IP 로드: {}", ip);
            })
            .then();
    }
    
    public void addToBlacklist(String ip) {
        blacklistedIps.add(ip);
        // Redis에도 저장
        redisTemplate.opsForSet()
            .add(REDIS_KEY, ip)
            .subscribe(
                result -> log.info("Redis에 IP 추가됨: {}", ip),
                error -> log.error("Redis 저장 실패: {}", ip, error)
            );
    }
    
    public boolean removeFromBlacklist(String ip) {
        boolean removed = blacklistedIps.remove(ip);
        if (removed) {
            // Redis에서도 제거
            redisTemplate.opsForSet()
                .remove(REDIS_KEY, ip)
                .subscribe(
                    result -> log.info("Redis에서 IP 제거됨: {}", ip),
                    error -> log.error("Redis 제거 실패: {}", ip, error)
                );
        }
        return removed;
    }
}
```

#### Phase 2: 주기적 동기화 (선택)

```java
@Scheduled(fixedRate = 60000) // 1분마다
public void syncFromRedis() {
    loadFromRedis().block();
    log.debug("Redis에서 블랙리스트 동기화 완료. 총 {}개", blacklistedIps.size());
}
```

#### Phase 3: 이력 관리 (선택)

```java
// IP 추가 시 이력 저장
private void saveIpHistory(String ip, String reason, String createdBy) {
    String historyKey = "gateway:blacklist:ip:" + ip + ":info";
    Map<String, String> info = Map.of(
        "ip", ip,
        "reason", reason,
        "created_at", Instant.now().toString(),
        "created_by", createdBy
    );
    redisTemplate.opsForHash().putAll(historyKey, info).subscribe();
}
```

---

## 3. 구현 단계

### 3.1 즉시 구현 가능 (난이도: 중간)

**필요 작업**:
1. `ReactiveRedisTemplate` 빈 추가
2. `IpBlacklistFilter`에 Redis 연동 추가
3. 초기화 로직 수정
4. `addToBlacklist`, `removeFromBlacklist` 메서드에 Redis 저장 추가

**예상 시간**: 2-3시간

### 3.2 테스트 계획

1. **기본 기능 테스트**
   - Redis에 IP 저장 확인
   - 서버 재시작 후 IP 유지 확인
   - API로 IP 추가/제거 확인

2. **성능 테스트**
   - Redis 읽기/쓰기 성능 확인
   - 메모리 사용량 확인

3. **동시성 테스트**
   - 여러 요청 동시 처리 확인

---

## 4. 대안: 하이브리드 방식

### 4.1 하이브리드 방식

**초기 IP**: `application.yml`에서 로드 (기본 차단 IP)
**런타임 IP**: Redis에서 로드 (동적으로 추가된 IP)

**장점**:
- 기본 차단 IP는 설정 파일로 관리 (명확함)
- 동적 추가 IP는 Redis로 관리 (유연함)

**구현**:
```java
@PostConstruct
public void init() {
    // 1. application.yml에서 초기 IP 로드
    loadInitialIps();
    
    // 2. Redis에서 동적으로 추가된 IP 로드
    loadFromRedis().block();
    
    // 3. 초기 IP를 Redis에 병합 (없으면 추가)
    mergeInitialIpsToRedis();
}
```

---

## 5. 권장 사항

### 5.1 즉시 적용

**현재 상황**:
- IP `142.93.3.113`이 이미 `application-docker.yml`에 추가됨
- 하지만 향후 새로운 공격 IP가 계속 나타날 수 있음

**권장**:
1. ✅ **단기**: 현재 방식 유지 (이미 추가됨)
2. ✅ **중기**: Redis 기반 저장 구현 (1-2주 내)
3. ✅ **장기**: 자동 탐지 및 차단 시스템 구축

### 5.2 구현 우선순위

1. **높음**: Redis 기반 영구 저장
2. **중간**: 주기적 동기화
3. **낮음**: 이력 관리

---

## 6. 결론

### 6.1 개선 필요성

✅ **Redis 기반 저장 구현 권장**
- 현재 방식의 한계 명확
- Redis 이미 설정되어 있어 구현 난이도 중간
- 영구 저장 및 런타임 관리 가능

### 6.2 다음 단계

1. Redis 기반 저장 구현
2. 테스트 및 검증
3. 프로덕션 배포

---

**작성자**: AI Assistant  
**작성일**: 2025년 11월 13일

