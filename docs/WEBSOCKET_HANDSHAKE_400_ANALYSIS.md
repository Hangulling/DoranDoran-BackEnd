# WebSocket 핸드셰이크 400 에러 분석

> `wss://api.doran-chat.com/ws/chat/{chatroomId}?userId=...&token=...` 연결 시  
> **Error during WebSocket handshake: Unexpected response code: 400** 원인 정리.

---

## 1. 400이 나오는 위치

클라이언트가 받는 **HTTP 400 Bad Request**는 **Chat 서비스**에서 반환됩니다.

- **Gateway**: token 없음 → **401**, JWT 검증 실패 → **401**, JWT 파싱 실패 → **401**
- **Chat**: `WebSocketAuthHandshakeInterceptor.beforeHandshake()`에서 **X-User-Id**가 없으면 `false` 반환 → Spring WebSocket이 핸드셰이크 거부 → **400 Bad Request**

즉, **400 = Chat까지 요청은 도달했지만, Chat이 “X-User-Id 헤더 없음”으로 핸드셰이크를 거부한 경우**입니다.

---

## 2. 흐름 요약

```
[클라이언트]  GET /ws/chat/{chatroomId}?userId=xxx&token=JWT  (WebSocket Upgrade)
       ↓
[Gateway]  JwtAuthFilter
           - token 쿼리 추출
           - Auth 서비스로 JWT 검증 (또는 캐시 히트)
           - JWT payload에서 sub → X-User-Id
           - 쿼리에서 token 제거, 요청에 X-User-Id 헤더 추가
           - chain.filter(mutated)
       ↓
[Gateway]  WebsocketRoutingFilter → Chat(8083)으로 프록시
       ↓
[Chat]     WebSocketAuthHandshakeInterceptor.beforeHandshake()
           - request.getHeaders().getFirst("X-User-Id") 확인
           - 없으면 return false → 400
           - 있으면 attributes에 넣고 return true → 101 Switching Protocols
```

400이 나온다 = **Chat이 받은 요청에 X-User-Id가 없다**는 뜻입니다.

---

## 3. 가능한 원인

### 3.1 Gateway에서 WebSocket 다운스트림으로 헤더가 안 넘어감

- Spring Cloud Gateway의 WebSocket 프록시는 **일부 헤더만** 제거합니다  
  (`connection`, `keep-alive`, `transfer-encoding`, `te`, `trailer`, `proxy-authorization`, `proxy-authenticate`, `x-application-context`, `upgrade`).
- **X-User-Id**는 이 목록에 없어서, 이론상 그대로 전달되어야 합니다.
- 다만 **실제로는** WebSocket 업그레이드 요청을 백엔드로 보낼 때, 구현/버전에 따라 커스텀 헤더가 누락되는 이슈가 보고된 적이 있음.
- **확인 방법**: Chat 서비스 로그에서  
  `[WS] Handshake 실패: X-User-Id 헤더 없음`  
  여부 확인. 이 로그가 찍히면 “Chat까지 요청은 오는데 X-User-Id만 없다”는 뜻이라, Gateway → Chat 구간에서 헤더 누락 가능성이 큼.

### 3.2 Gateway에서 mutated 요청이 아닌 “원본” 요청으로 전달됨

`JwtAuthFilter.addHmacHeadersAndStripTokenForWebSocket()` 중:

- JWT가 **3 part가 아니거나**, payload 파싱에서 **예외**가 나면 `catch`에서 401을 보내고 끝남.
- 그런데 **`parts.length != 3`** 인 경우만 따지면, `if (parts.length == 3)` 블록을 타지 않고 맨 아래  
  `return chain.filter(exchange);`  
  로 빠질 수 있음. 이때는 **헤더를 넣지 않은 원본 `exchange`**가 그대로 다음 필터/라우팅으로 넘어감.
- 결과: Chat에는 **token만 제거된 상태로** 요청이 가고, **X-User-Id는 없음** → Chat이 400 반환.

즉, **토큰 형식이 3 part가 아니거나, 중간에 잘려서 3 part가 아닌 경우** 400이 날 수 있습니다.

- 예: URL 길이 제한/잘림, 쿼리 인코딩 문제로 `token` 값이 일부만 전달되는 경우.

### 3.3 JWT 만료

- 만료된 토큰이면 Auth `/api/auth/validate`에서 실패 → Gateway가 **401**을 반환해야 합니다.
- 따라서 **400**이 나왔다면, “만료로 인한 401”보다는 **Chat까지 도달했지만 X-User-Id가 없어서 400**인 경우에 해당합니다.

---

## 4. 확인 방법

1. **Chat 로그**
   - `[WS] Handshake 실패: X-User-Id 헤더 없음`  
     → Chat에는 요청이 오지만 X-User-Id가 없다는 뜻.
2. **Gateway 로그**
   - `WebSocket JWT 검증 캐시 히트` 또는 Auth 호출 성공 후  
     `addHmacHeadersAndStripTokenForWebSocket`까지 진행되는지 확인.
   - `WebSocket JWT 페이로드 파싱 실패` / `JWT에서 userId(sub) 추출 실패`  
     → 이때는 401이 나와야 정상. 400이 나왔다면 그 전에 다른 경로로 요청이 Chat에 갔을 수 있음.
3. **클라이언트**
   - `token` 쿼리 값이 **전체 JWT가 그대로** 한 번에 전달되는지(잘림/인코딩 오류 없는지) 확인.

---

## 5. 권장 대응

1. **Chat 로그로 400 원인 고정**
   - 위 `[WS] Handshake 실패` 로그가 있으면 → “X-User-Id 없음”으로 400이 나는 것이 맞음.
2. **Gateway에서 “원본 exchange” 전달 방지**
   - `addHmacHeadersAndStripTokenForWebSocket`에서  
     `parts.length != 3` 이거나 payload 파싱 실패 시 **절대** `chain.filter(exchange)` 하지 말고,  
     **401**로 응답하고 `exchange.getResponse().setComplete()` 하도록 처리.
3. **WebSocket 구간 헤더 전달 보장**
   - Gateway 버전/문서 확인 후, WebSocket 라우트에 **AddRequestHeader** 등으로  
     X-User-Id를 명시적으로 넣는 방식이 가능한지 검토.
   - 또는 쿼리에서 token 제거하지 않고, Chat 쪽에서도 쿼리 `token`을 읽어 검증하는 방식으로 구조 변경 검토(보안·일관성은 별도 설계 필요).

---

## 6. 참고

- Spring: `HandshakeInterceptor.beforeHandshake()`에서 `false` 반환 시 **400 Bad Request** 응답.
- API 명세: `docs/API_SPEC_WEBSOCKET_CHAT.md` — WebSocket URL 형식, 인증 흐름, 에러 코드(401 vs 400) 정리.
