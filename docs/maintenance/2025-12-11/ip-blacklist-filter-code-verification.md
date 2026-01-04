# IP 블랙리스트 필터 코드 검증 보고서

**작성일**: 2025년 12월 11일  
**대상**: `IpBlacklistFilter.java`  
**목적**: 필터 동작 검증 및 "Decoding failed" 에러와의 관계 분석

---

## 1. 필터 코드 구조 분석

### 1.1 필터 기본 정보

```java
@Component
public class IpBlacklistFilter implements GlobalFilter, Ordered {
    // Order: -100 (가장 높은 우선순위)
    // 실행 위치: GatewayFilterChain
}
```

**검증 결과**: ✅ 정상
- `GlobalFilter`와 `Ordered` 인터페이스를 올바르게 구현
- Order: -100으로 설정되어 다른 필터보다 먼저 실행됨

### 1.2 필터 초기화

```java
public IpBlacklistFilter(@Value("${gateway.security.blacklist.ips:}") String blacklistIps) {
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

**검증 결과**: ✅ 정상
- 설정 파일에서 IP 목록을 올바르게 읽어옴
- 쉼표로 구분된 IP 목록을 파싱하여 HashSet에 저장
- 초기화 시 로그 출력으로 확인 가능

### 1.3 필터 실행 로직

```java
@Override
public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    ServerHttpRequest request = exchange.getRequest();
    String path = request.getURI().getPath();

    // Actuator 엔드포인트는 제외
    if (isExcludedPath(path)) {
        return chain.filter(exchange);
    }

    // 클라이언트 IP 주소 추출
    String clientIp = extractClientIp(request);
    
    if (clientIp == null) {
        log.warn("클라이언트 IP를 추출할 수 없습니다. 요청 경로: {}", path);
        // IP를 추출할 수 없으면 통과 (보안보다 가용성 우선)
        return chain.filter(exchange);
    }

    // 블랙리스트 체크
    if (isBlacklisted(clientIp)) {
        log.warn("차단된 IP에서 요청이 들어왔습니다. IP: {}, 경로: {}", clientIp, path);
        return handleBlockedRequest(exchange, clientIp);
    }

    // 블랙리스트에 없으면 정상 처리
    return chain.filter(exchange);
}
```

**검증 결과**: ✅ 정상
- 필터 체인에서 올바르게 실행됨
- 제외 경로 처리 로직 정상
- IP 추출 실패 시 경고 로그 출력 후 통과 (가용성 우선)

---

## 2. IP 추출 로직 분석

### 2.1 IP 추출 우선순위

```java
private String extractClientIp(ServerHttpRequest request) {
    // 1. X-Forwarded-For 헤더 확인
    // 2. X-Real-IP 헤더 확인
    // 3. Forwarded 헤더 확인
    // 4. RemoteAddress 사용 (마지막 수단)
}
```

**검증 결과**: ✅ 정상
- 표준 프록시 헤더를 우선순위대로 확인
- Docker 환경에서도 RemoteAddress를 사용하여 IP 추출 가능

### 2.2 Docker 환경에서의 IP 추출

**잠재적 문제**: ⚠️ 주의 필요
- Docker 네트워크를 통해 들어오는 요청의 경우, `request.getRemoteAddress()`가 Docker 내부 IP(예: `172.18.0.x`)를 반환할 수 있음
- 실제 클라이언트 IP는 `X-Forwarded-For` 또는 `X-Real-IP` 헤더에 있을 가능성이 높음

**권장사항**:
- Docker 컨테이너 앞에 리버스 프록시(Nginx, Traefik 등)를 두고 실제 클라이언트 IP를 헤더에 주입하는 것이 좋음
- 현재는 `X-Forwarded-For` 헤더를 우선 확인하므로 문제 없을 가능성이 높음

---

## 3. "Decoding failed" 에러와 필터의 관계

### 3.1 문제 분석

**발견된 문제**: ⚠️ **중요**

"Decoding failed" 에러는 **Netty 레벨에서 발생**하며, 이는 **필터가 실행되기 전**에 발생합니다.

```
요청 흐름:
1. Netty HTTP 파서 (요청 파싱)
   └─ "Decoding failed" 에러 발생 ← 여기서 발생!
   
2. GatewayFilterChain (필터 실행)
   └─ IpBlacklistFilter (-100) ← 여기까지 도달하지 않음!
```

**결과**:
- 비정상 요청(프로토콜 혼동, 제어 문자 주입 등)은 필터까지 도달하지 않음
- 따라서 블랙리스트 필터로는 "Decoding failed" 에러를 완전히 차단할 수 없음

### 3.2 블랙리스트 필터의 효과

**효과 있는 경우**:
- ✅ 정상적인 HTTP 요청을 보내는 공격자
- ✅ 블랙리스트에 등록된 IP에서 정상 요청을 시도하는 경우

**효과 없는 경우**:
- ❌ 비정상 요청(프로토콜 혼동, 제어 문자 주입)을 보내는 공격자
- ❌ "Decoding failed" 에러를 발생시키는 요청

### 3.3 3.131.215.38 사례 분석

**상황**:
- 3.131.215.38이 블랙리스트에 등록되어 있음 (11월 23일 추가)
- 하지만 12월 8일에 6건의 "Decoding failed" 에러 발생

**원인 분석**:
1. **비정상 요청 사용**: 공격자가 HTTP/0.9 프로토콜 혼동 공격을 사용
2. **필터 미실행**: 비정상 요청이 Netty 레벨에서 실패하여 필터까지 도달하지 않음
3. **결과**: 블랙리스트에 있음에도 "Decoding failed" 에러 발생

**결론**:
- 블랙리스트 필터는 **정상 요청을 차단**하는 데 효과적
- 하지만 **비정상 요청은 Netty 레벨에서 차단**되므로 필터가 실행되지 않음
- 이는 **정상적인 동작**이며, 비정상 요청은 어차피 처리되지 않으므로 보안상 문제 없음

---

## 4. 필터 동작 검증 결과

### 4.1 정상 동작 확인

✅ **필터 초기화**: 설정 파일에서 IP 목록을 올바르게 읽어옴  
✅ **필터 실행 순서**: Order: -100으로 가장 먼저 실행됨  
✅ **IP 추출 로직**: 표준 프록시 헤더를 우선순위대로 확인  
✅ **블랙리스트 체크**: HashSet을 사용하여 O(1) 시간 복잡도로 체크  
✅ **차단 응답**: HTTP 403 Forbidden 응답 반환

### 4.2 잠재적 개선 사항

1. **IP 추출 로직 강화**
   - Docker 환경에서의 IP 추출 정확도 향상
   - 로드밸런서/프록시 환경에서의 IP 추출 개선

2. **로깅 강화**
   - IP 추출 실패 시 더 자세한 로그 출력
   - 차단된 요청에 대한 상세 로그 (User-Agent, 요청 경로 등)

3. **모니터링**
   - 차단된 요청 수 모니터링
   - 블랙리스트 효과 측정

---

## 5. 결론 및 권장사항

### 5.1 필터 코드 검증 결과

**전체 평가**: ✅ **정상 동작**

필터 코드는 올바르게 구현되어 있으며, 정상적인 HTTP 요청에 대해서는 블랙리스트가 제대로 작동합니다.

### 5.2 "Decoding failed" 에러에 대한 이해

**중요**: "Decoding failed" 에러는 필터가 실행되기 전에 발생하므로, 블랙리스트 필터로는 완전히 차단할 수 없습니다. 하지만 이는 **보안상 문제가 되지 않습니다**:

1. 비정상 요청은 어차피 처리되지 않음
2. Netty 레벨에서 차단되어 서비스에 영향을 주지 않음
3. 블랙리스트는 정상 요청을 차단하는 데 효과적

### 5.3 권장 조치사항

1. **현재 상태 유지**: 필터 코드는 정상이므로 수정 불필요
2. **모니터링 강화**: "Decoding failed" 에러와 블랙리스트 차단 로그를 함께 모니터링
3. **문서화**: "Decoding failed" 에러가 필터 실행 전에 발생한다는 점을 문서화

### 5.4 향후 개선 방향

1. **Netty 레벨 차단**: 커스텀 Netty Handler를 구현하여 비정상 요청을 더 일찍 차단
2. **자동 블랙리스트**: "Decoding failed" 에러를 발생시키는 IP를 자동으로 블랙리스트에 추가
3. **Rate Limiting**: IP별 요청 수 제한으로 공격 완화

---

## 부록: 필터 실행 순서

```
요청 처리 흐름:

1. Netty HTTP 파서
   └─ 요청 파싱
      ├─ 정상 요청 → 다음 단계
      └─ 비정상 요청 → "Decoding failed" 에러 (여기서 종료)

2. GatewayFilterChain
   └─ IpBlacklistFilter (Order: -100) ← 정상 요청만 여기까지 도달
      ├─ IP 추출
      ├─ 블랙리스트 체크
      │  ├─ 차단됨 → HTTP 403 반환 (종료)
      │  └─ 통과 → 다음 필터
      └─ JwtAuthFilter (Order: 0)
         └─ 인증 처리
```

**결론**: 필터는 정상적으로 작동하며, "Decoding failed" 에러는 필터 실행 전에 발생하므로 필터의 문제가 아닙니다.












