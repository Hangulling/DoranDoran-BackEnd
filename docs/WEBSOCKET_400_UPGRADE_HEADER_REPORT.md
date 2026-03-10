# WebSocket 400 오류 원인 분석 보고서

**분석 일시**: 2026-02-26  
**서버**: ec2-user@3.21.177.186  
**도구**: SSH 접속, Docker 로그 분석

---

## 1. 요약

| 항목 | 내용 |
|------|------|
| **현상** | WebSocket 연결 시 `Error during WebSocket handshake: Unexpected response code: 400` (400 Bad Request) |
| **근본 원인** | **Upgrade 헤더가 null**로 Chat 서비스에 전달됨 → `DefaultHandshakeHandler`가 핸드셰이크 거부 |
| **오해 가능성** | 기존 분석(docs/WEBSOCKET_HANDSHAKE_400_ANALYSIS.md)에서는 X-User-Id 부재를 의심했으나, **실제로는 X-User-Id는 정상 도달**함 |

---

## 2. 서버 로그 근거

### 2.1 Chat 서비스 로그 (2026-02-26 04:47:20)

```
[WS] Handshake: X-User-Id 추출 완료: userId=3fa11b2f-0e4d-4332-aa76-308407ec68ca
"Handshake failed due to invalid Upgrade header: null"
```

- `WebSocketAuthHandshakeInterceptor`: **X-User-Id 헤더 존재** → `beforeHandshake` 통과
- `DefaultHandshakeHandler`: **Upgrade 헤더가 null** → 핸드셰이크 실패 → **400 Bad Request 반환**

### 2.2 해석

- Gateway → Chat 구간에서 **X-User-Id는 정상 전달**됨
- 동시에 **Upgrade 헤더는 Chat에 도달하지 않음**
- 따라서 문제는 X-User-Id 누락이 아니라, **Upgrade 헤더의 제거/미전달**임

---

## 3. 원인 분석

### 3.1 Spring Cloud Gateway의 헤더 처리

Spring Cloud Gateway는 HTTP 프록시 시 **hop-by-hop 헤더**를 제거합니다.

- `RemoveHopByHopRequestHeaders` 등에서 `connection`, `keep-alive`, `transfer-encoding`, `te`, `trailer`, `proxy-authorization`, `proxy-authenticate`, **`upgrade`** 등을 제거
- WebSocket 업그레이드에 필수인 `Upgrade: websocket` 헤더가 이 목록에 포함되어 제거될 수 있음

### 3.2 가능한 시나리오

1. **일반 HTTP 프록시 사용**
   - WebSocket 업그레이드 요청이 `WebSocketRoutingFilter`가 아닌 일반 라우팅 필터(예: `NettyRoutingFilter`)로 처리
   - 요청 전달 시 `upgrade` 헤더가 제거되어 Chat에 전달되지 않음

2. **WebSocket 프록시 동작 이상**
   - `WebSocketRoutingFilter`가 활성화되어 있어도, 백엔드로 보내는 요청 생성 시 Upgrade 관련 헤더가 빠지는 경우

3. **중간 리버스 프록시(Nginx 등)**
   - Gateway 앞단 리버스 프록시가 WebSocket용 헤더를 제거/변형하는 경우

---

## 4. WebSocket API 구조 (참고)

### 4.1 연결 흐름

```
[클라이언트]  GET /ws/chat/{chatroomId}?userId=xxx&token=JWT  (Upgrade: websocket)
       ↓
[Gateway]  1. token 파라미터로 JWT 검증
           2. X-User-Id 헤더 주입
           3. Chat 서비스로 프록시
       ↓
[Chat]     1. WebSocketAuthHandshakeInterceptor: X-User-Id 확인 ✅
           2. DefaultHandshakeHandler: Upgrade 헤더 확인 ❌ → 400
```

### 4.2 관련 설정

| 구성요소 | 파일 | 역할 |
|----------|------|------|
| JwtAuthFilter | gateway/.../JwtAuthFilter.java | /ws/chat/ 요청 시 token 검증, X-User-Id 주입 |
| chat-websocket 라우트 | application-docker.yml | Path=/ws/chat/** → dorandoran-chat:8083 |
| WebSocketAuthHandshakeInterceptor | chat/.../WebSocketAuthHandshakeInterceptor.java | X-User-Id 확인, attributes 저장 |
| DefaultHandshakeHandler | Spring WebSocket | Upgrade 헤더 확인 → 없으면 400 |

---

## 5. 권장 대응

### 5.1 즉시 시도 가능

1. **Gateway 라우트에 AddRequestHeader 추가**
   - WebSocket 라우트에서 `AddRequestHeader=Upgrade, websocket` 등으로 Upgrade 관련 헤더를 명시적으로 추가
   - Spring Cloud Gateway 버전/필터 동작에 따라 효과 여부 확인 필요

2. **uri를 ws:// 로 변경**
   - `uri: ws://dorandoran-chat:8083` 으로 설정해 WebSocket 전용 프록시 경로를 사용
   - WebSocketRoutingFilter가 확실히 적용되는지 확인

### 5.2 구조적 대안

3. **WebSocket 경로의 Nginx/로드밸런서 설정 확인**
   - Nginx가 WebSocket을 프록시할 때 `Upgrade`, `Connection` 헤더를 그대로 전달하도록 설정되어 있는지 검토
   - `proxy_http_version 1.1`, `proxy_set_header Upgrade $http_upgrade`, `proxy_set_header Connection "upgrade"` 등 확인

4. **클라이언트 → Chat 직연결 (개발/테스트용)**
   - Gateway를 우회하고 Chat에 직접 WebSocket 연결해 400이 사라지는지 확인
   - 사라지면 Gateway 또는 그 앞단 프록시 문제로 좁혀짐

---

## 6. 참고 문서

- [docs/WEBSOCKET_HANDSHAKE_400_ANALYSIS.md](WEBSOCKET_HANDSHAKE_400_ANALYSIS.md) – X-User-Id 중심 분석 (실제 원인과는 다름)
- [docs/API_SPEC_WEBSOCKET_CHAT.md](API_SPEC_WEBSOCKET_CHAT.md) – WebSocket API 명세
- [Spring Cloud Gateway #2289](https://github.com/spring-cloud/spring-cloud-gateway/issues/2289) – WebSocket 관련 헤더 제거 이슈
