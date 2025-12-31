# 사용자 분석 및 블랙리스트 개선 방안

**작성일**: 2025년 11월 13일  
**분석 일자**: 2025년 11월 12일

---

## 1. 새로 가입한 사용자 동향

### 1.1 분석 결과

**오늘(2025-11-12) 가입한 사용자 수**: **0명**

**최근 가입한 사용자**:
- 마지막 가입: 2025-11-11 13:14:11 (hj9832621@naver.com)
- 그 이전: 2025-11-10 09:44:49 (gjdmstngjdms@gmail.com)

### 1.2 결론

✅ **새로 가입한 사용자가 없습니다.**
- 오늘 하루 동안 신규 가입이 없었습니다.
- 마지막 가입은 전일(11월 11일) 오후 1시경입니다.

---

## 2. 3시~8시 트래픽 분석

### 2.1 분석 결과

**3시~8시 요청을 보낸 사용자**:

| 사용자 ID | 요청 수 | 이메일 | 가입일 |
|-----------|--------|--------|--------|
| ef341a6c-62f3-4e42-8ad0-e59587ec3417 | 136건 | zipizigy121@gmail.com | 2025-11-09 |
| 4f50e3b3-886e-4f55-a4af-7c2b333af318 | 62건 | hj9832621@naver.com | 2025-11-11 |

**총 사용자 수**: 2명  
**총 요청 수**: 198건

### 2.2 개발 중 조작 가능성 분석

**높은 확률로 개발 중 조작입니다.**

**근거**:
1. **시간대**: 오전 3시~8시는 일반 사용자 활동이 적은 시간대
2. **사용자 수**: 단 2명의 사용자만 활동
3. **요청 패턴**: 짧은 시간에 집중된 요청 (136건, 62건)
4. **가입 시기**: 두 사용자 모두 최근 가입 (11월 9일, 11일)

**추가 확인 사항**:
- 이 사용자들의 요청 패턴이 테스트/개발 패턴과 일치하는지
- 실제 사용자 활동인지, 자동화된 테스트인지

### 2.3 권장 사항

1. **개발 환경 분리**: 개발/테스트는 별도 환경에서 수행
2. **테스트 사용자 구분**: 테스트 계정에 명확한 식별자 추가
3. **트래픽 필터링**: 개발 중 생성된 트래픽은 분석에서 제외 가능

---

## 3. 블랙리스트 IP 관리 개선 방안

### 3.1 현재 방식의 문제점

**현재 방식**: `application-docker.yml`에 직접 하드코딩
```yaml
gateway:
  security:
    blacklist:
      ips: "172.104.24.172, 142.93.3.113"
```

**문제점**:
1. ❌ **코드 변경 필요**: IP 추가/제거 시마다 코드 수정 및 재배포 필요
2. ❌ **영구 저장 없음**: 런타임에 API로 추가한 IP는 서버 재시작 시 사라짐
3. ❌ **버전 관리 혼란**: 설정 파일에 보안 관련 정보가 하드코딩됨
4. ❌ **확장성 부족**: IP가 많아지면 관리가 어려움

### 3.2 개선 방안: Redis 기반 영구 저장

#### 3.2.1 개선 효과

✅ **장점**:
1. **영구 저장**: 서버 재시작 후에도 유지
2. **런타임 관리**: 코드 변경 없이 API로 즉시 추가/제거
3. **확장성**: 많은 IP 관리 가능
4. **성능**: Redis의 빠른 읽기/쓰기
5. **분산 환경**: 여러 Gateway 인스턴스에서 공유 가능

#### 3.2.2 구현 방안

**1. Redis 저장 구조**
```
Key: gateway:blacklist:ips
Type: Set
Value: ["172.104.24.172", "142.93.3.113", ...]
```

**2. 초기화 로직**
```java
// 애플리케이션 시작 시
1. application.yml에서 초기 IP 로드
2. Redis에 저장 (없으면 생성, 있으면 병합)
3. Redis에서 전체 IP 목록 로드하여 메모리에 캐시
```

**3. 런타임 관리**
```java
// API로 IP 추가 시
1. 메모리(HashSet)에 추가
2. Redis에도 추가 (영구 저장)
3. 다른 Gateway 인스턴스에도 알림 (선택사항)
```

**4. 주기적 동기화**
```java
// 주기적으로 Redis에서 최신 목록 가져오기
// 또는 Redis Pub/Sub으로 실시간 동기화
```

#### 3.2.3 구현 예시

**IpBlacklistFilter 수정**:
```java
@Service
public class IpBlacklistFilter implements GlobalFilter, Ordered {
    private final Set<String> blacklistedIps = new ConcurrentHashMap<>().keySet();
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    
    @PostConstruct
    public void init() {
        // 1. application.yml에서 초기 IP 로드
        loadInitialIps();
        
        // 2. Redis에서 전체 IP 로드
        loadFromRedis();
        
        // 3. Redis에 초기 IP 저장 (없으면)
        saveToRedis();
    }
    
    private void loadFromRedis() {
        redisTemplate.opsForSet()
            .members("gateway:blacklist:ips")
            .subscribe(ip -> blacklistedIps.add(ip));
    }
    
    public void addToBlacklist(String ip) {
        blacklistedIps.add(ip);
        // Redis에도 저장
        redisTemplate.opsForSet()
            .add("gateway:blacklist:ips", ip)
            .subscribe();
    }
}
```

### 3.3 대안: 데이터베이스 기반 저장

**장점**:
- 영구 저장 보장
- 이력 관리 가능 (언제, 누가, 왜 추가했는지)
- 복잡한 쿼리 가능

**단점**:
- 성능 오버헤드 (Redis보다 느림)
- 데이터베이스 부하 증가

**구조 예시**:
```sql
CREATE TABLE gateway_blacklist_ips (
    id BIGSERIAL PRIMARY KEY,
    ip_address VARCHAR(45) NOT NULL UNIQUE,
    reason TEXT,
    created_by VARCHAR(255),
    created_at TIMESTAMP DEFAULT NOW(),
    is_active BOOLEAN DEFAULT TRUE
);
```

### 3.4 권장 구현 순서

**Phase 1: Redis 기반 저장 (권장)**
1. Redis 의존성 추가 (이미 있음)
2. `IpBlacklistFilter`에 Redis 연동 추가
3. 초기화 시 Redis에서 로드
4. API 호출 시 Redis에도 저장

**Phase 2: 자동 동기화 (선택)**
1. 주기적 동기화 (예: 1분마다)
2. 또는 Redis Pub/Sub으로 실시간 동기화

**Phase 3: 이력 관리 (선택)**
1. 데이터베이스에 이력 저장
2. 관리자 API에 이력 조회 기능 추가

---

## 4. 즉시 적용 가능한 개선 사항

### 4.1 현재 상황

- IP `142.93.3.113`이 이미 `application-docker.yml`에 추가됨
- 하지만 런타임에 추가한 IP는 서버 재시작 시 사라짐

### 4.2 단기 해결책

**옵션 1: 설정 파일에 추가 (현재 방식)**
- ✅ 즉시 적용 가능
- ❌ 재배포 필요

**옵션 2: API로 추가 후 설정 파일에도 반영**
- ✅ 즉시 차단 가능
- ⚠️ 설정 파일 자동 업데이트 로직 필요

### 4.3 중장기 해결책

**Redis 기반 저장 구현** (권장)
- 영구 저장 보장
- 코드 변경 없이 관리 가능
- 확장성 확보

---

## 5. 결론

### 5.1 사용자 동향
- ✅ 오늘 신규 가입: 0명
- ✅ 3시~8시 트래픽: 개발 중 조작 가능성 높음 (2명, 198건)

### 5.2 블랙리스트 관리 개선
- ✅ **권장**: Redis 기반 영구 저장 구현
- ✅ **장점**: 영구 저장, 런타임 관리, 확장성
- ✅ **구현 난이도**: 중간 (Redis 이미 설정되어 있음)

---

**작성자**: AI Assistant  
**작성일**: 2025년 11월 13일

