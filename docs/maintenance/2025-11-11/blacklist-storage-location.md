# 블랙리스트 IP 저장 위치 분석

**작성일**: 2025년 11월 11일  
**목적**: 현재 블랙리스트 IP 저장 위치 및 영구 저장 여부 확인

---

## 1. 현재 저장 위치

### 1.1 초기 로드 위치

**설정 파일**:
- `gateway/src/main/resources/application.yml`
- `gateway/src/main/resources/application-docker.yml`

**설정 경로**:
```yaml
gateway:
  security:
    blacklist:
      ips: "172.104.24.172"
```

**로드 시점**: 애플리케이션 시작 시 (IpBlacklistFilter 생성자)

### 1.2 런타임 저장 위치

**저장소**: 메모리 (Java HashSet)
```java
private final Set<String> blacklistedIps;  // 메모리에만 저장
```

**위치**: `IpBlacklistFilter` 클래스의 인스턴스 변수

---

## 2. 저장 방식 상세

### 2.1 초기 로드 과정

```
1. 애플리케이션 시작
   ↓
2. IpBlacklistFilter 빈 생성
   ↓
3. @Value("${gateway.security.blacklist.ips:}")로 설정 읽기
   ↓
4. HashSet<String> blacklistedIps 초기화
   ↓
5. 설정 파일의 IP 목록을 HashSet에 추가
```

**코드 위치**:
```52:67:gateway/src/main/java/com/dorandoran/gateway/filter/IpBlacklistFilter.java
public IpBlacklistFilter(
        @Value("${gateway.security.blacklist.ips:}") String blacklistIps) {
    this.blacklistedIps = new HashSet<>();
    
    if (StringUtils.hasText(blacklistIps)) {
        Arrays.stream(blacklistIps.split(","))
                .map(String::trim)
                .filter(ip -> !ip.isEmpty())
                .forEach(ip -> {
                    this.blacklistedIps.add(ip);
                    log.info("IP 블랙리스트에 추가됨: {}", ip);
                });
    }
    
    log.info("IP 블랙리스트 필터 초기화 완료. 총 {}개의 IP가 차단됩니다.", this.blacklistedIps.size());
}
```

### 2.2 런타임 추가/제거

**API를 통한 추가**:
```java
public void addToBlacklist(String ip) {
    if (StringUtils.hasText(ip)) {
        blacklistedIps.add(ip.trim());  // 메모리에만 추가
        log.info("런타임에 IP 블랙리스트에 추가됨: {}", ip);
    }
}
```

**API를 통한 제거**:
```java
public boolean removeFromBlacklist(String ip) {
    if (StringUtils.hasText(ip)) {
        boolean removed = blacklistedIps.remove(ip.trim());  // 메모리에서만 제거
        if (removed) {
            log.info("런타임에 IP 블랙리스트에서 제거됨: {}", ip);
        }
        return removed;
    }
    return false;
}
```

---

## 3. 영구 저장 여부

### 3.1 현재 상태

**영구 저장**: ❌ 없음

**사용하지 않는 저장소**:
- ❌ Redis (설정되어 있지만 사용 안 함)
- ❌ 데이터베이스
- ❌ 파일 시스템
- ❌ 외부 설정 서비스

### 3.2 문제점

**서버 재시작 시 데이터 손실**:
```
1. 런타임에 API로 IP 추가 (예: 192.168.1.100)
   ↓
2. 메모리에 저장됨 (HashSet)
   ↓
3. 서버 재시작
   ↓
4. application.yml에서만 로드 (172.104.24.172만 유지)
   ↓
5. 런타임에 추가한 IP (192.168.1.100) 사라짐 ❌
```

---

## 4. 저장 위치 요약

| 저장 위치 | 초기 로드 | 런타임 저장 | 영구 저장 | 재시작 후 유지 |
|-----------|----------|------------|----------|--------------|
| application.yml | ✅ | ❌ | ✅ | ✅ |
| 메모리 (HashSet) | ✅ | ✅ | ❌ | ❌ |
| Redis | ❌ | ❌ | ❌ | ❌ |
| 데이터베이스 | ❌ | ❌ | ❌ | ❌ |

---

## 5. 현재 등록된 IP

### 5.1 설정 파일에 등록된 IP

**application.yml**:
```yaml
gateway:
  security:
    blacklist:
      ips: "172.104.24.172"
```

**application-docker.yml**:
```yaml
gateway:
  security:
    blacklist:
      ips: "172.104.24.172"
```

### 5.2 런타임에 추가된 IP

**확인 방법**: API 호출
```bash
GET /api/admin/blacklist
```

**현재 상태**: 런타임에 추가된 IP는 서버 재시작 전까지만 유지

---

## 6. 권장 개선 사항

### 6.1 즉시 개선 필요 🟡

#### 영구 저장 기능 추가

**옵션 1: Redis 사용** (권장)
- 이미 Redis가 설정되어 있음
- 빠른 읽기/쓰기 성능
- 분산 환경에서 공유 가능

**옵션 2: 데이터베이스 사용**
- 영구 저장 보장
- 이력 관리 가능
- 하지만 성능 오버헤드

**옵션 3: 파일 시스템 사용**
- 간단한 구현
- 하지만 동시성 문제

### 6.2 단기 개선 사항 🟢

#### 설정 파일 자동 업데이트

- 런타임에 추가한 IP를 application.yml에 자동 저장
- 서버 재시작 시에도 유지

---

## 7. 결론

### 7.1 현재 저장 위치

1. **초기 로드**: `application.yml` / `application-docker.yml`
2. **런타임 저장**: 메모리 (HashSet)
3. **영구 저장**: 없음

### 7.2 문제점

- 런타임에 추가한 IP는 서버 재시작 시 사라짐
- 영구 저장 기능이 없음

### 7.3 권장 조치

- Redis를 활용한 영구 저장 기능 추가 권장

---

**작성자**: AI Assistant  
**작성일**: 2025년 11월 11일

