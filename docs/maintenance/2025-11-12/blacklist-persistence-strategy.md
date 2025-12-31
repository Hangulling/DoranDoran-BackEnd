# 블랙리스트 IP 영구 저장 전략

**작성일**: 2025년 11월 13일  
**목적**: Redis 데이터 손실 방지 및 영구 저장 보장

---

## 1. 문제 인식

### 1.1 Redis 데이터 손실 가능성

**Redis의 한계**:
- ❌ 메모리 기반 저장소 → 서버 재시작 시 데이터 손실 가능
- ❌ RDB/AOF 설정 없으면 데이터 영구 저장 안 됨
- ❌ Redis 장애 시 블랙리스트 IP 모두 사라짐

**현재 상황**:
- Redis는 캐시로 사용 중
- 영구 저장 설정 확인 필요
- 데이터 손실 시 복구 방법 없음

---

## 2. 해결 방안: 3계층 저장 구조

### 2.1 저장 계층 구조

```
┌─────────────────────────────────────┐
│  1. 데이터베이스 (Source of Truth)  │  ← 영구 저장, 복구 가능
│     - 모든 블랙리스트 IP 저장       │
│     - 이력 관리                     │
│     - 백업 가능                     │
└─────────────────────────────────────┘
              ↓ (동기화)
┌─────────────────────────────────────┐
│  2. Redis (캐시/성능)               │  ← 빠른 읽기/쓰기
│     - 메모리 캐시                   │
│     - 실시간 동기화                 │
│     - RDB/AOF 백업 설정              │
└─────────────────────────────────────┘
              ↓ (로드)
┌─────────────────────────────────────┐
│  3. 메모리 (HashSet)                 │  ← 최종 필터링
│     - 빠른 조회                     │
│     - 필터에서 사용                 │
└─────────────────────────────────────┘
```

### 2.2 데이터 흐름

**IP 추가 시**:
```
1. 데이터베이스에 저장 (영구 저장) ✅
   ↓
2. Redis에 저장 (캐시) ✅
   ↓
3. 메모리(HashSet)에 추가 (즉시 차단) ✅
```

**애플리케이션 시작 시**:
```
1. 데이터베이스에서 전체 IP 로드 (Source of Truth) ✅
   ↓
2. Redis에 동기화 (없으면 추가) ✅
   ↓
3. 메모리(HashSet)에 로드 ✅
```

**Redis 장애 시**:
```
1. 데이터베이스에서 전체 IP 로드 ✅
   ↓
2. 메모리(HashSet)에 직접 로드 ✅
   ↓
3. Redis 복구 후 자동 동기화 ✅
```

**데이터베이스 장애 시**:
```
1. Redis 캐시에서 로드 (임시) ⚠️
   ↓
2. 메모리(HashSet)에 로드 ⚠️
   ↓
3. DB 복구 후 자동 동기화 ✅
```

---

## 3. 데이터베이스 스키마 설계

### 3.1 Gateway 스키마 생성

```sql
-- Gateway 전용 스키마 생성
CREATE SCHEMA IF NOT EXISTS gateway_schema;

-- 블랙리스트 IP 테이블
CREATE TABLE IF NOT EXISTS gateway_schema.blacklist_ips (
    id BIGSERIAL PRIMARY KEY,
    ip_address VARCHAR(45) NOT NULL UNIQUE,
    reason TEXT,
    created_by VARCHAR(255),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    is_active BOOLEAN DEFAULT TRUE,
    
    CONSTRAINT uk_blacklist_ip UNIQUE (ip_address)
);

-- 인덱스 추가
CREATE INDEX IF NOT EXISTS idx_blacklist_ip_active 
    ON gateway_schema.blacklist_ips(ip_address) 
    WHERE is_active = TRUE;
    
CREATE INDEX IF NOT EXISTS idx_blacklist_created_at 
    ON gateway_schema.blacklist_ips(created_at);
```

### 3.2 초기 데이터 마이그레이션

```sql
-- application-docker.yml에 있는 IP를 DB에 마이그레이션
INSERT INTO gateway_schema.blacklist_ips (ip_address, reason, created_by, is_active)
VALUES 
    ('172.104.24.172', 'Protocol confusion attack (2025-11-11)', 'system', TRUE),
    ('142.93.3.113', 'Decoding failed attacks (2025-11-12)', 'system', TRUE)
ON CONFLICT (ip_address) DO NOTHING;
```

---

## 4. 구현 방안

### 4.1 Gateway에 데이터베이스 연결 추가

**현재 상태**: Gateway는 JPA 비활성화되어 있음

**옵션 1: R2DBC 사용 (Reactive, 권장)**
- Gateway가 Reactive이므로 R2DBC가 적합
- 비동기/논블로킹 처리

**옵션 2: JDBC 직접 사용 (Blocking)**
- 간단하지만 블로킹 호출 필요
- `@Async` 또는 별도 스레드 풀 사용

**옵션 3: 별도 Admin 서비스**
- Gateway는 필터링만 담당
- Admin 서비스에서 블랙리스트 관리

### 4.2 R2DBC 기반 구현 (권장)

#### 4.2.1 의존성 추가

```gradle
// build.gradle
dependencies {
    // R2DBC PostgreSQL
    implementation 'org.springframework.boot:spring-boot-starter-data-r2dbc'
    implementation 'io.r2dbc:r2dbc-postgresql'
    
    // R2DBC Pool
    implementation 'io.r2dbc:r2dbc-pool'
}
```

#### 4.2.2 설정 추가

```yaml
# application-docker.yml
spring:
  r2dbc:
    url: r2dbc:postgresql://dorandoran-shared-db:5432/dorandoran
    username: doran
    password: ${DB_PASSWORD:DoranDoran123!}
    pool:
      initial-size: 5
      max-size: 20
```

#### 4.2.3 Entity (R2DBC)

```java
@Table(name = "blacklist_ips", schema = "gateway_schema")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlacklistIp {
    @Id
    @Column("id")
    private Long id;
    
    @Column("ip_address")
    private String ipAddress;
    
    @Column("reason")
    private String reason;
    
    @Column("created_by")
    private String createdBy;
    
    @Column("created_at")
    private LocalDateTime createdAt;
    
    @Column("updated_at")
    private LocalDateTime updatedAt;
    
    @Column("is_active")
    private Boolean isActive;
}
```

#### 4.2.4 Repository (R2DBC)

```java
@Repository
public interface BlacklistIpRepository extends ReactiveCrudRepository<BlacklistIp, Long> {
    Flux<BlacklistIp> findByIsActiveTrue();
    Mono<BlacklistIp> findByIpAddressAndIsActiveTrue(String ipAddress);
    Mono<Boolean> existsByIpAddressAndIsActiveTrue(String ipAddress);
}
```

#### 4.2.5 Service 계층

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class BlacklistIpService {
    private final BlacklistIpRepository repository;
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private static final String REDIS_KEY = "gateway:blacklist:ips";
    
    /**
     * 데이터베이스에서 활성화된 모든 IP 조회
     */
    public Flux<String> getAllActiveIps() {
        return repository.findByIsActiveTrue()
            .map(BlacklistIp::getIpAddress)
            .doOnNext(ip -> log.debug("DB에서 IP 로드: {}", ip));
    }
    
    /**
     * IP 추가 (DB → Redis → 반환)
     */
    @Transactional
    public Mono<BlacklistIp> addIp(String ipAddress, String reason, String createdBy) {
        // 1. DB에 저장
        BlacklistIp blacklistIp = BlacklistIp.builder()
            .ipAddress(ipAddress)
            .reason(reason)
            .createdBy(createdBy)
            .isActive(true)
            .build();
        
        return repository.save(blacklistIp)
            .doOnNext(saved -> {
                log.info("블랙리스트 IP 추가됨 (DB): {}", ipAddress);
                // 2. Redis에 저장 (비동기)
                saveToRedis(ipAddress).subscribe();
            });
    }
    
    /**
     * IP 제거 (소프트 삭제)
     */
    @Transactional
    public Mono<Boolean> removeIp(String ipAddress) {
        return repository.findByIpAddressAndIsActiveTrue(ipAddress)
            .flatMap(ip -> {
                ip.setIsActive(false);
                return repository.save(ip)
                    .doOnNext(saved -> {
                        log.info("블랙리스트 IP 제거됨 (DB): {}", ipAddress);
                        // Redis에서 제거
                        removeFromRedis(ipAddress).subscribe();
                    })
                    .thenReturn(true);
            })
            .defaultIfEmpty(false);
    }
    
    /**
     * Redis에 저장
     */
    private Mono<Long> saveToRedis(String ip) {
        return redisTemplate.opsForSet()
            .add(REDIS_KEY, ip)
            .doOnSuccess(result -> log.debug("Redis에 IP 추가: {}", ip))
            .doOnError(error -> log.error("Redis 저장 실패: {}", ip, error));
    }
    
    /**
     * Redis에서 제거
     */
    private Mono<Long> removeFromRedis(String ip) {
        return redisTemplate.opsForSet()
            .remove(REDIS_KEY, ip)
            .doOnSuccess(result -> log.debug("Redis에서 IP 제거: {}", ip))
            .doOnError(error -> log.error("Redis 제거 실패: {}", ip, error));
    }
    
    /**
     * 데이터베이스에서 Redis로 동기화
     */
    public Mono<Void> syncToRedis() {
        return getAllActiveIps()
            .flatMap(this::saveToRedis)
            .then()
            .doOnSuccess(v -> log.info("Redis 동기화 완료"))
            .doOnError(error -> log.error("Redis 동기화 실패", error));
    }
}
```

#### 4.2.6 IpBlacklistFilter 수정

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class IpBlacklistFilter implements GlobalFilter, Ordered {
    
    private final Set<String> blacklistedIps = ConcurrentHashMap.newKeySet();
    private final BlacklistIpService blacklistIpService;
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    
    private static final String REDIS_KEY = "gateway:blacklist:ips";
    
    @Value("${gateway.security.blacklist.ips:}")
    private String initialIps;
    
    @PostConstruct
    public void init() {
        log.info("IP 블랙리스트 필터 초기화 시작...");
        
        // 1. application.yml에서 초기 IP 로드 (기본 차단 IP)
        loadInitialIps();
        
        // 2. 데이터베이스에서 전체 IP 로드 (Source of Truth)
        loadFromDatabase().block();
        
        // 3. Redis 동기화 (없으면 추가)
        blacklistIpService.syncToRedis().block();
        
        // 4. Redis에서도 로드 (캐시 확인)
        loadFromRedis().block();
        
        log.info("IP 블랙리스트 필터 초기화 완료. 총 {}개의 IP가 차단됩니다.", blacklistedIps.size());
    }
    
    /**
     * application.yml에서 초기 IP 로드
     */
    private void loadInitialIps() {
        if (StringUtils.hasText(initialIps)) {
            Arrays.stream(initialIps.split(","))
                .map(String::trim)
                .filter(ip -> !ip.isEmpty())
                .forEach(ip -> {
                    blacklistedIps.add(ip);
                    log.debug("설정 파일에서 IP 로드: {}", ip);
                });
        }
    }
    
    /**
     * 데이터베이스에서 전체 IP 로드 (Source of Truth)
     */
    private Mono<Void> loadFromDatabase() {
        return blacklistIpService.getAllActiveIps()
            .doOnNext(ip -> {
                blacklistedIps.add(ip);
                log.debug("DB에서 IP 로드: {}", ip);
            })
            .then()
            .doOnSuccess(v -> log.info("데이터베이스에서 IP 로드 완료"))
            .doOnError(error -> {
                log.warn("데이터베이스 로드 실패, Redis 캐시 사용: {}", error.getMessage());
                // Redis에서 로드 (임시)
                loadFromRedis().block();
            });
    }
    
    /**
     * Redis에서 IP 로드 (캐시)
     */
    private Mono<Void> loadFromRedis() {
        return redisTemplate.opsForSet()
            .members(REDIS_KEY)
            .doOnNext(ip -> {
                if (!blacklistedIps.contains(ip)) {
                    blacklistedIps.add(ip);
                    log.debug("Redis에서 IP 로드: {}", ip);
                }
            })
            .then()
            .doOnSuccess(v -> log.debug("Redis에서 IP 로드 완료"))
            .doOnError(error -> log.warn("Redis 로드 실패 (DB에서 로드된 IP 사용): {}", error.getMessage()));
    }
    
    /**
     * IP 추가 (DB → Redis → 메모리)
     */
    public void addToBlacklist(String ip, String reason, String createdBy) {
        // 1. DB에 저장 (Source of Truth)
        blacklistIpService.addIp(ip, reason, createdBy)
            .subscribe(
                saved -> {
                    // 2. 메모리에 추가 (즉시 차단)
                    blacklistedIps.add(ip);
                    log.info("IP 블랙리스트에 추가됨: {}", ip);
                },
                error -> log.error("IP 추가 실패: {}", ip, error)
            );
    }
    
    /**
     * IP 제거 (DB → Redis → 메모리)
     */
    public Mono<Boolean> removeFromBlacklist(String ip) {
        return blacklistIpService.removeIp(ip)
            .doOnNext(removed -> {
                if (removed) {
                    // 메모리에서 제거
                    blacklistedIps.remove(ip);
                    log.info("IP 블랙리스트에서 제거됨: {}", ip);
                }
            });
    }
    
    // ... 기존 필터 로직 유지 ...
}
```

---

## 5. Redis 영구 저장 설정 (추가 보안)

### 5.1 Redis RDB 설정

```conf
# redis.conf
save 900 1      # 900초(15분) 동안 1개 이상 변경 시 저장
save 300 10     # 300초(5분) 동안 10개 이상 변경 시 저장
save 60 10000   # 60초 동안 10000개 이상 변경 시 저장
```

### 5.2 Redis AOF 설정

```conf
# redis.conf
appendonly yes
appendfsync everysec  # 매초마다 디스크에 동기화
```

### 5.3 Docker Compose 설정

```yaml
redis:
  image: redis:7-alpine
  command: redis-server --appendonly yes --save 60 1
  volumes:
    - redis-data:/data
```

---

## 6. 복구 전략

### 6.1 Redis 장애 시

**자동 복구**:
```java
@PostConstruct
public void init() {
    // 1. DB에서 로드 (항상 가능)
    loadFromDatabase().block();
    
    // 2. Redis 동기화 시도
    try {
        blacklistIpService.syncToRedis().block(Duration.ofSeconds(5));
    } catch (Exception e) {
        log.warn("Redis 동기화 실패 (DB에서 로드된 IP 사용): {}", e.getMessage());
    }
}
```

### 6.2 데이터베이스 장애 시

**Redis 캐시 사용**:
```java
private Mono<Void> loadFromDatabase() {
    return blacklistIpService.getAllActiveIps()
        .doOnNext(ip -> blacklistedIps.add(ip))
        .then()
        .onErrorResume(error -> {
            log.warn("데이터베이스 로드 실패, Redis 캐시 사용: {}", error.getMessage());
            // Redis에서만 로드 (임시)
            return loadFromRedis();
        });
}
```

### 6.3 양쪽 모두 장애 시

**application.yml 기본 IP 사용**:
```java
// 최소한의 기본 차단 IP는 application.yml에서 로드
// 완전한 장애 상황에서도 기본 보안 유지
```

---

## 7. 구현 단계

### 7.1 Phase 1: 데이터베이스 스키마 생성

**작업**:
1. `gateway_schema` 생성
2. `blacklist_ips` 테이블 생성
3. 초기 데이터 마이그레이션

**예상 시간**: 30분

### 7.2 Phase 2: R2DBC 연동

**작업**:
1. R2DBC 의존성 추가
2. 설정 추가
3. Entity, Repository 구현

**예상 시간**: 1-2시간

### 7.3 Phase 3: Service 및 Filter 수정

**작업**:
1. `BlacklistIpService` 구현
2. `IpBlacklistFilter` 수정
3. 3계층 저장 로직 구현

**예상 시간**: 2-3시간

### 7.4 Phase 4: 테스트 및 검증

**작업**:
1. 기본 기능 테스트
2. 장애 시나리오 테스트
3. 성능 테스트

**예상 시간**: 1-2시간

---

## 8. 대안: 간단한 JDBC 방식

### 8.1 Gateway에 JDBC 직접 사용

**장점**:
- R2DBC 설정 불필요
- 기존 JDBC 코드 재사용 가능
- 구현 간단

**단점**:
- 블로킹 호출 (별도 스레드 풀 필요)
- Reactive 패턴과 맞지 않음

**구현 예시**:
```java
@Service
@RequiredArgsConstructor
public class BlacklistIpService {
    private final JdbcTemplate jdbcTemplate;
    
    public List<String> getAllActiveIps() {
        return jdbcTemplate.queryForList(
            "SELECT ip_address FROM gateway_schema.blacklist_ips WHERE is_active = TRUE",
            String.class
        );
    }
}
```

---

## 9. 결론

### 9.1 최종 구조

**3계층 저장**:
1. **데이터베이스** (Source of Truth) - 영구 저장, 복구 가능 ✅
2. **Redis** (캐시) - 빠른 읽기/쓰기, 성능 최적화 ✅
3. **메모리** (HashSet) - 최종 필터링, 즉시 차단 ✅

### 9.2 데이터 손실 방지

✅ **다중 백업**:
- 데이터베이스: 영구 저장 (PostgreSQL)
- Redis: RDB/AOF 설정으로 백업
- application.yml: 기본 IP 보관

✅ **자동 복구**:
- Redis 장애 → DB에서 로드
- DB 장애 → Redis 캐시 사용
- 양쪽 장애 → application.yml 기본 IP

### 9.3 권장 사항

1. ✅ **데이터베이스를 Source of Truth로 사용**
2. ✅ **Redis는 캐시로만 사용**
3. ✅ **주기적 동기화로 일관성 유지**
4. ✅ **Redis RDB/AOF 설정으로 추가 보안**

---

**작성자**: AI Assistant  
**작성일**: 2025년 11월 13일
