# start-greeting 401 원인 좁히기

채팅 서비스 `POST /api/chat/chatrooms/{chatroomId}/start-greeting` 호출 시 게이트웨이는 통과했는데 채팅 서비스에서 401이 나는 경우, `SecurityContext`에 `userId`가 없어서 발생한다. 아래는 가능한 원인 세 가지를 코드/동작 기준으로 정리한 내용이다.

---

## 1. 헤더가 채팅 서비스까지 안 옴

**의미**: 게이트웨이 → 채팅 구간에서 `X-User-Id`가 빠지거나, 중간 프록시/로드밸런서가 제거하는 경우.

### 게이트웨이 쪽 동작

- **위치**: `gateway/.../JwtAuthFilter.java` 208~277라인
- JWT 검증 성공 시 `exchange.mutate().request(builder -> builder.headers(...))` 로 **요청을 새로 만들고** `X-User-Id` 등을 **설정**한 뒤 `chain.filter(mutated)` 로 넘긴다.
- 이후 `NettyRoutingFilter`가 **이 mutated exchange의 request**를 사용해 다운스트림(채팅)으로 HTTP 요청을 보낸다. 따라서 이 흐름이라면 `X-User-Id`는 다운스트림 요청에 포함된다.

### Spring Cloud Gateway 기본 동작

- **HttpHeadersFilters**: 다운스트림으로 보내기 전에 `RemoveHopByHopHeadersFilter`, `ForwardedHeadersFilter` 등이 적용된다.
- **RemoveHopByHop**: 제거 대상은 IETF 표준 hop-by-hop 헤더만 해당 (Connection, Keep-Alive, Proxy-Authenticate, Transfer-Encoding, Upgrade 등). **`X-User-Id`는 제거 대상이 아니다.**
- 커스텀 헤더를 제거하는 기본 필터는 없으므로, **게이트웨이 코드에서 한 번 설정해 주면 그대로 전달되는 것이 정상**이다.

### 확인 방법

- 채팅 서비스에 이미 추가한 로그:  
  `start-greeting 401: ... X-User-Id 헤더 present={}, value={}`  
  - **present=false** 이면 → 게이트웨이에서 채팅으로 가는 구간에서 헤더가 없어진 것이다.
- 그 경우: 게이트웨이와 채팅 사이에 nginx/ALB 등이 있다면 해당 구간에서 `X-User-Id`를 제거하지 않는지, 또는 게이트웨이 배포 버전이 헤더를 넣는 코드와 일치하는지 확인하면 된다.

---

## 2. 필터 적용/순서

**의미**: `UserIdHeaderAuthenticationFilter`가 해당 요청에 안 타거나, 다른 필터가 `SecurityContext`를 비우는 경우.

### 채팅 서비스 필터 구성

- **위치**: `chat/.../SecurityConfig.java`
  - `UserIdHeaderAuthenticationFilter`를 `UsernamePasswordAuthenticationFilter` **앞**에 추가:
    - `.addFilterBefore(userIdHeaderAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)`
- **UserIdHeaderAuthenticationFilter**: `OncePerRequestFilter` 상속, `shouldNotFilter` 오버라이드 없음 → **모든 요청에 한 번씩 실행**된다.
- **동작**: `X-User-Id`가 있으면 파싱 후 `SecurityContextHolder.getContext().setAuthentication(auth)` 로 설정하고, 없으면 아무 것도 하지 않는다.

### SecurityContext가 비워질 수 있는 경우

- **스레드**: 서블릿 컨테이너는 한 요청을 한 스레드에서 처리하므로, 필터에서 설정한 `SecurityContext`는 같은 스레드의 컨트롤러에서 그대로 보인다. (별도 스레드로 디스패치하지 않는 한 유지됨.)
- **UsernamePasswordAuthenticationFilter**: 폼 로그인 등으로 인증을 시도할 때만 동작하며, 기존에 설정된 `Authentication`을 **무조건 덮어쓰거나 비우는 동작은 하지 않는다.**  
  현재 설정은 `.formLogin(formLogin -> formLogin.disable())` 이므로, 이 필터가 `SecurityContext`를 비우는 가능성은 낮다.
- **비동기/다른 스레드**: `start-greeting`은 동기 `ResponseEntity` 반환이므로, 요청 스레드가 바뀌는 디스패치가 없다면 필터에서 넣은 값이 컨트롤러까지 유지되는 것이 맞다.

### 확인 방법

- `UserIdHeaderAuthenticationFilter`에 추가한 로그:
  - **start-greeting 경로**로 요청이 들어올 때 `X-User-Id` **있음 + 유효 UUID** → SecurityContext 설정 여부
  - **있음 + 잘못된 값** → `IllegalArgumentException` 로그 (UUID 파싱 실패)
  - **없음** → `X-User-Id`가 아예 도착하지 않음 (위 1번과 연결)
- 위 로그로 “헤더는 오는데 SecurityContext는 비어 있다”가 나오면, 필터 순서나 다른 필터가 context를 지우는지 추가로 추적할 수 있다.

---

## 3. 헤더 이름/값 (대소문자, 공백, 인코딩)

**의미**: 헤더는 오지만 이름/값 형태 때문에 채팅 서비스에서 인식하지 못하는 경우. (가능성은 상대적으로 낮음.)

### 게이트웨이

- `http.set("X-User-Id", userId);` 로 **한 가지 이름**만 사용한다. 대소문자는 Spring이 정규화한다.

### 채팅 서비스

- `request.getHeader("X-User-Id")`: 서블릿 스펙상 **헤더 이름은 대소문자 무관**으로 매칭된다. `x-user-id` 로 와도 `X-User-Id`로 조회 가능하다.
- **값**: `UUID.fromString(userId)` 사용. **앞뒤 공백**이 있으면 파싱 실패할 수 있다.  
  - **현재**: `UserIdHeaderAuthenticationFilter`에서 `userId = (userIdRaw != null) ? userIdRaw.trim() : null` 로 **trim 적용됨**.  
  - trim 후에도 UUID 형식이 아니면 `IllegalArgumentException` → catch에서 무시되고 SecurityContext는 설정되지 않는다.

### 권장 (적용 완료)

- 필터에서 헤더 사용 시 **trim()** 적용 완료. 디버그 로그에서 **value=** 로 실제 들어온 값을 보면 공백/인코딩 이슈 여부를 확인할 수 있다.
- 디버그 로그에서 **value=** 로 실제 들어온 값을 보면, 공백/인코딩 이슈 여부를 바로 확인할 수 있다.

---

## 요약 표

| 원인 | 확인 방법 | 비고 |
|------|-----------|------|
| 1. 헤더가 채팅까지 안 옴 | 채팅 로그 `X-User-Id 헤더 present=false` | 게이트웨이~채팅 구간, 프록시/배포 확인 |
| 2. 필터 적용/순서 | 필터 로그에서 start-greeting 요청 시 헤더 유무·설정 여부 | OncePerRequestFilter, addFilterBefore 유지 |
| 3. 헤더 이름/값 | `value=` 로 들어온 값 확인, UUID 파싱 실패 로그 | trim 적용·파싱 실패 시 로그 추가 권장 |

배포 후 같은 401이 나면,  
1) 채팅 서비스의 `start-greeting 401: ... X-User-Id 헤더 present=..., value=...`  
2) 채팅 서비스의 `UserIdHeaderAuthenticationFilter` 로그  
를 함께 보면 위 세 가지 중 어디에 가까운지 바로 좁힐 수 있다.

---

## 4시 이후 로그 수집 (서버에서)

EC2 접속 후 채팅 컨테이너에서 **start-greeting / 401 / X-User-Id** 관련 라인만 뽑을 때:

```bash
# 4시간 구간 (오후 4시 로그 띄운 뒤 ~ 현재)
docker logs --since 4h dorandoran-chat 2>&1 | grep -E "start-greeting 401|UserIdHeaderAuthenticationFilter|X-User-Id"

# 시간 포함 전체 로그에서 위 패턴만 (맥락 보려면)
docker logs --since 4h dorandoran-chat 2>&1 | grep -E "start-greeting 401|UserIdHeaderAuthenticationFilter|X-User-Id"
```

Windows에서 SSH로 한 번에 가져오기:

```powershell
ssh -i $env:USERPROFILE\Downloads\dorandoran-key.pem ec2-user@3.21.177.186 "docker logs --since 4h dorandoran-chat 2>&1" | Select-String -Pattern "start-greeting 401|UserIdHeaderAuthenticationFilter|X-User-Id"
```

수집한 로그는 `docs/logs/chat-start-greeting-logs-YYYYMMDD.md` 등에 붙여넣어 두면 분석 시 참고할 수 있다.

---

## 서버 로그 분석 결과 (2026-02-18)

서버에서 `docker logs --since 4h dorandoran-chat 2>&1 | grep -E 'start-greeting 401|UserIdHeaderAuthenticationFilter|X-User-Id'` 로 수집한 결과:

- **동일 스레드·동일 요청**에서 다음이 동시에 관찰됨:
  - `UserIdHeaderAuthenticationFilter: X-User-Id 설정됨 path=.../start-greeting, userId=3fa11b2f-...` (DEBUG)
  - 직후(약 18ms 뒤) `start-greeting 401: SecurityContext userId=null, ... X-User-Id 헤더 present=true, value=3fa11b2f-...` (WARN)

**해석**

- **원인 1(헤더 미전달) 아님**: `present=true`, `value=유효 UUID` → 헤더는 채팅까지 정상 전달됨.
- **원인 3(헤더 값 형식) 아님**: 값은 유효한 UUID이며, 필터에서도 "X-User-Id 설정됨"으로 인식함.
- **원인 2(필터/순서)에 해당**: 같은 스레드(`http-nio-0.0.0.0-8083-exec-7`)에서 필터가 SecurityContext에 설정했는데, 컨트롤러 진입 시점에는 `userId=null`로 비어 있음.  
  → 필터와 컨트롤러 **사이**에서 SecurityContext가 비워지거나 덮어쓰이는 동작이 있는 것으로 추정 (Spring Security 체인 내 다른 필터 또는 SecurityContextHolder 정책 추가 확인 필요).

**대응**

- **당장 401 방지**: `startGreeting`에서 SecurityContext에서 userId를 못 읽을 때, **X-User-Id 헤더가 있으면 유효 UUID로 파싱해 사용**하는 fallback 적용 (다른 API와 동일한 패턴).  
- **근본 원인 조사 결과**: 
  - Spring Security 6의 `SecurityContextHolderFilter`는 필터 체인 시작 시 `SecurityContextRepository`에서 SecurityContext를 로드합니다.
  - 기본적으로 `HttpSessionSecurityContextRepository`를 사용하는데, 세션이 없으면 빈 SecurityContext를 생성합니다.
  - 우리 필터(`UserIdHeaderAuthenticationFilter`)가 `UsernamePasswordAuthenticationFilter` 앞에 추가되어 있었는데, 이는 필터 체인 순서상 늦은 위치입니다.
  - `SecurityContextHolderFilter` (ORDER=100) → ... → `AnonymousAuthenticationFilter` (ORDER=2200) → `UsernamePasswordAuthenticationFilter` (ORDER=2100) 순서에서, 우리 필터가 2100 근처에 위치해 `AnonymousAuthenticationFilter` 이후에 실행되거나, 또는 `SecurityContextHolderFilter`가 세션에서 빈 SecurityContext를 로드한 후 덮어쓸 가능성이 있습니다.

**수정 사항**

1. **필터 순서 변경**: `UserIdHeaderAuthenticationFilter`를 `SecurityContextHolderFilter` **바로 다음**에 실행되도록 변경 (`addFilterAfter(userIdHeaderAuthenticationFilter(), SecurityContextHolderFilter.class)`).
2. **Stateless 세션 정책**: `.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))` 추가하여 세션 기반 SecurityContextRepository가 작동하지 않도록 설정.
3. **추적 로그 추가**: 필터 진입/종료 시점과 컨트롤러 진입 시점에 SecurityContext 상태를 로깅하여 언제 비워지는지 추적 가능하도록 함.

이제 `SecurityContextHolderFilter`가 빈 SecurityContext를 생성한 직후, 우리 필터가 X-User-Id 헤더로 인증을 설정하므로, 이후 필터들이 이를 덮어쓰지 않아야 합니다.
