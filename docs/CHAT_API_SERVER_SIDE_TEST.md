# 채팅 서비스 API — 서버 내부 테스트 방법

서버(EC2) 안에서 Chat 서비스 API를 호출해 보는 방법입니다.

- **방법 A**: Gateway 경유 + JWT (실제 클라이언트와 동일)
- **방법 B**: Chat 직접 호출 + HMAC 헤더 (서버 내부 전용, JWT 불필요)

---

## 1. Chat 직접 호출 (HMAC 필수)

Chat 서비스는 **직접 호출 시** Gateway가 넣어 주는 **HMAC 인증**을 요구합니다.  
`X-User-Id`, `X-Auth-Ts`, `X-Auth-Sign` 세 헤더가 모두 있어야 합니다 (인터셉터: `HmacAuthInterceptor`).

### 1.1 테스트용 사용자 UUID 준비

- DB에서 실제 사용자 UUID 하나를 조회하거나,
- 테스트용 UUID 사용: `11111111-1111-1111-1111-111111111111` (DB에 해당 사용자가 있어야 일부 API가 동작함)

### 1.2 HMAC 시그니처 생성 후 호출

Chat과 Gateway에 설정된 **동일한 `gateway.jwt.hmac-secret`** 값(hex 문자열)을 사용해 `X-Auth-Ts`, `X-Auth-Sign`을 만듭니다.

- **메시지**: `{X-User-Id}|{X-Auth-Ts}` (타임스탬프는 밀리초)
- **서명**: `HMAC-SHA256(hmac-secret, 메시지)` 결과를 hex 문자열로

서버에 SSH 접속한 뒤 (Chat 포트 8083이 호스트에 열려 있음):

```bash
# 환경 변수 — 서버의 application.yml / docker 환경과 동일한 시크릿 사용
USER_ID="11111111-1111-1111-1111-111111111111"
HMAC_SECRET="404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970"

# 타임스탬프(밀리초)와 서명 생성
TS=$(($(date +%s) * 1000))
MSG="${USER_ID}|${TS}"
SIGN=$(echo -n "$MSG" | openssl dgst -sha256 -mac HMAC -macopt hexkey:"$HMAC_SECRET" | awk '{print $2}')

# 헬스는 HMAC 제외 경로 — 인증 없이 호출 가능
curl -s -w "\n%{http_code}\n" http://127.0.0.1:8083/actuator/health

# 채팅방 목록 (HMAC 헤더 필수)
curl -s -H "X-User-Id: $USER_ID" -H "X-Auth-Ts: $TS" -H "X-Auth-Sign: $SIGN" \
  "http://127.0.0.1:8083/api/chat/chatrooms?page=0&size=5"
```

### 1.3 한 번에 복사해 쓸 수 있는 예시

```bash
USER_ID="11111111-1111-1111-1111-111111111111"
HMAC_SECRET="404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970"
TS=$(($(date +%s) * 1000))
SIGN=$(echo -n "${USER_ID}|${TS}" | openssl dgst -sha256 -mac HMAC -macopt hexkey:"$HMAC_SECRET" | awk '{print $2}')
curl -s -H "X-User-Id: $USER_ID" -H "X-Auth-Ts: $TS" -H "X-Auth-Sign: $SIGN" "http://127.0.0.1:8083/api/chat/chatrooms?page=0&size=2"
```

### 1.4 스크립트로 실행

프로젝트에 `docs/scripts/chat-api-test.sh` 스크립트가 있습니다. 서버에 복사한 뒤 실행해도 됩니다.

```bash
# 서버에 업로드 후
chmod +x chat-api-test.sh
./chat-api-test.sh
# 또는 특정 사용자 UUID로
./chat-api-test.sh "실제-사용자-UUID"
```

### 1.5 HMAC 제외 경로 (인증 없이 호출 가능)

다음 경로는 `HmacAuthInterceptor`에서 제외되므로 **HMAC 없이** 호출해도 됩니다.

- `/actuator/**` — 헬스 등
- `/swagger-ui/**`, `/v3/api-docs/**`
- `/api/chat/health`
- `/api/admin/prompts/sync`

---

## 2. Gateway 경유 (실제와 동일한 경로, JWT 필요)

클라이언트와 동일하게 **Gateway(8080) → Chat** 경로로 테스트하려면 **유효한 JWT**가 필요합니다.  
Gateway가 JWT를 검증한 뒤 Chat으로 넘길 때 HMAC 헤더를 붙여 주므로, **서버 호스트에서 curl만으로** 테스트할 수 있습니다.

### 2.1 JWT 발급

- 앱/웹에서 로그인 후 받은 `accessToken`을 복사하거나,
- Auth 서비스 로그인 API를 서버에서 호출해 토큰을 받습니다.

```bash
# 예: 로그인 API로 토큰 받기 (실제 이메일/비밀번호로 교체)
curl -s -X POST -H "Content-Type: application/json" \
  -d '{"email":"your@email.com","password":"yourpassword"}' \
  http://127.0.0.1:8081/api/auth/login
```

응답에서 `accessToken` 값을 복사한 뒤:

```bash
export TOKEN="여기에_accessToken_붙여넣기"

# Gateway 경유 — 채팅방 목록 (호스트에서 8080으로 호출)
curl -s -H "Authorization: Bearer $TOKEN" "http://127.0.0.1:8080/api/chat/chatrooms?page=0&size=5"
```

> **참고**: Auth 포트(8081), Gateway 포트(8080)가 호스트에 열려 있어야 합니다.  
> 열려 있지 않다면 `docker run --rm --network host curlimages/curl:latest ...` 로 같은 호스트에서 호출할 수 있습니다.

---

## 3. 자주 쓰는 Chat API 엔드포인트 요약

| 메서드 | 경로 | 비고 |
|--------|------|------|
| GET | `/api/chat/chatrooms` | 채팅방 목록, `userId` 또는 `X-User-Id` |
| POST | `/api/chat/chatrooms` | 채팅방 생성, body: `userId`, `chatbotId`, `name`, `concept` 등 |
| GET | `/api/chat/chatrooms/{chatroomId}/messages` | 메시지 목록 |
| POST | `/api/chat/chatrooms/{chatroomId}/messages` | 메시지 전송 |
| GET | `/api/chat/chatrooms/{chatroomId}` | 채팅방 상세 |
| GET | `/api/chat/chatbots/{chatbotId}` | 챗봇 정보 |
| GET | `/actuator/health` | 헬스체크 (인증 없음) |

---

## 4. 정리

| 목적 | 방법 |
|------|------|
| **JWT 없이 서버 안에서만** Chat API 테스트 | **1번**: Chat 직접 호출 시 `X-User-Id` + `X-Auth-Ts` + `X-Auth-Sign`(HMAC) 필수. 위 1.3 블록 복사 후 실행. |
| **실제와 동일한 경로** (Gateway 경유) | **2번**: 유효한 JWT로 `http://127.0.0.1:8080/api/chat/...` 호출. |
| **인증 없이** 상태만 확인 | `curl -s http://127.0.0.1:8083/actuator/health` (HMAC 제외 경로). |

---

## 5. 요약

- **JWT 없이 서버 안에서만 빠르게**:  
  `docker exec dorandoran-gateway curl -s -H "X-User-Id: <UUID>" http://dorandoran-chat:8083/api/chat/...`
- **실제와 동일한 경로(Gateway 경유)**:  
  유효한 JWT로 `http://localhost:8080/api/chat/...` 호출 (호스트 또는 gateway 컨테이너 내부에서).
