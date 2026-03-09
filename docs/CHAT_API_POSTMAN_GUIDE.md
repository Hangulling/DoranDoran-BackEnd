# Postman으로 채팅(Chat) API 테스트하기

Gateway 경유 시 **JWT만 있으면** Postman에서 바로 호출할 수 있습니다.  
(서버 주소는 실제 도메인 또는 IP로 바꿔서 사용하세요.)

---

## 1. 환경 설정

### 1.1 Environment 변수 (권장)

Postman에서 **Environments** → 새 환경 생성 후 아래 변수 추가:

| 변수명 | 초기값 | 설명 |
|--------|--------|------|
| `base_url` | `https://api.도메인.com` 또는 `http://서버IP:8080` | Gateway 주소 (포트 8080) |
| `access_token` | (비움) | 로그인 후 수동으로 넣거나, 아래 2번으로 자동 저장 |
| `user_id` | (비움) | 테스트할 사용자 UUID (선택) |

**⚠️ `getaddrinfo ENOTFOUND api` 오류가 나는 경우**

- **원인**: URL 입력란에 **경로만** 넣었을 때 발생합니다. (예: `api/chat/chatrooms`만 입력 → Postman이 `api`를 호스트로 인식)
- **해결**:
  1. **URL 입력란**에 반드시 **전체 주소**를 적으세요.  
     - ✅ 올바른 예: `{{base_url}}/api/chat/chatrooms`  
     - ❌ 잘못된 예: `api/chat/chatrooms`
  2. **base_url** 값은 **프로토콜 + 호스트 + 포트**까지 넣고, 끝에 `/`는 빼세요.  
     - ✅ 예: `http://3.21.177.186:8080` 또는 `https://api.도메인.com`
  3. 우측 상단에서 **Environment를 선택**했는지 확인하세요. (선택하지 않으면 `{{base_url}}`이 치환되지 않습니다.)

---

## 2. JWT 발급 (로그인)

채팅 API는 **Gateway**를 통해 호출하며, **Authorization: Bearer {access_token}** 이 필요합니다.

### Request

- **Method**: `POST`
- **URL**: `{{base_url}}/api/auth/login`
- **Headers**: `Content-Type: application/json`
- **Body** (raw JSON):

```json
{
  "email": "테스트계정@이메일.com",
  "password": "비밀번호"
}
```

### 응답에서 토큰 저장

응답 예시:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "...",
  "userId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
}
```

**방법 A – 수동**  
- `accessToken` 값을 복사해서 Environment의 `access_token`에 붙여넣기.

**방법 B – 자동**  
- **Tests** 탭에 아래 스크립트 추가하면, 로그인 요청 한 번으로 `access_token`·`user_id`가 환경 변수에 저장됩니다.

```javascript
var json = pm.response.json();
if (json.accessToken) {
    pm.environment.set("access_token", json.accessToken);
}
if (json.userId) {
    pm.environment.set("user_id", json.userId);
}
```

---

## 3. 채팅 API 호출 (Gateway 경유)

모든 채팅 API는 **Gateway base_url + 경로**로 호출하고, **Authorization** 헤더만 넣으면 됩니다.

### 공통 설정

- **Headers**
  - `Authorization`: `Bearer {{access_token}}`
  - (필요 시) `Content-Type`: `application/json`

### 자주 쓰는 엔드포인트

| 메서드 | URL | 비고 |
|--------|-----|------|
| GET | `{{base_url}}/api/chat/chatrooms?page=0&size=20` | 채팅방 목록 |
| POST | `{{base_url}}/api/chat/chatrooms` | 채팅방 생성 (body 필요) |
| GET | `{{base_url}}/api/chat/chatrooms/{{chatroom_id}}?userId={{user_id}}` | 채팅방 상세 |
| GET | `{{base_url}}/api/chat/chatrooms/{{chatroom_id}}/messages?userId={{user_id}}&page=0&size=50` | 메시지 목록 |
| POST | `{{base_url}}/api/chat/chatrooms/{{chatroom_id}}/messages` | 메시지 전송 (body 필요) |
| GET | `{{base_url}}/api/chat/chatbots/{{chatbot_id}}` | 챗봇 정보 |

### 채팅방 생성 예시 (POST /api/chat/chatrooms)

**⚠️ 500 오류가 나는 경우**: Body의 `"userId": "{{user_id}}"`에서 **`{{user_id}}`가 실제 UUID로 치환되어야** 합니다. Environment에 `user_id`가 비어 있거나, Environment를 선택하지 않으면 서버가 문자열 `"{{user_id}}"`를 UUID로 파싱하다가 실패해 500을 반환합니다. → 로그인 응답에서 `userId`를 복사해 Environment의 `user_id`에 넣고, Environment를 선택한 뒤 다시 요청하세요.

- **Body** (raw JSON):

```json
{
  "userId": "{{user_id}}",
  "chatbotId": "3fa85f64-5717-4562-b3fc-2c963f66afa7",
  "name": "Postman 테스트방",
  "concept": "FRIEND"
}
```

- `chatbotId`는 **실제 DB에 있는 값**으로 넣어야 합니다. 프로젝트 시드/코드 기준 예시:
  - **한국어 튜터** (create_chatbot.sql): `3fa85f64-5717-4562-b3fc-2c963f66afa7`
  - **테스트봇** (test-data.sql): `3fa85f64-5717-4562-b3fc-2c963f66afa6`
  - **컨셉별 하드코딩** (ChatService): FRIEND `22222222-2222-2222-2222-222222222221`, HONEY `22222222-2222-2222-2222-222222222222`, COWORKER `22222222-2222-2222-2222-222222222223`, SENIOR `22222222-2222-2222-2222-222222222224`, BOSS `22222222-2222-2222-2222-222222222225`
- 서버 DB에 어떤 챗봇이 있는지 확인하려면: `SELECT id, name, display_name FROM chat_schema.chatbots WHERE is_active = true;`

### 메시지 전송 예시 (POST /api/chat/chatrooms/{chatroomId}/messages)

- **Body** (raw JSON):

```json
{
  "content": "안녕하세요",
  "userId": "{{user_id}}"
}
```

- `chatroomId`는 URL에 넣고, Environment에 `chatroom_id` 변수로 두면 `{{chatroom_id}}` 로 쓸 수 있음.

---

## 4. 정리

1. **Environment**에 `base_url`, `access_token`, (선택) `user_id` 설정.
2. **POST** `{{base_url}}/api/auth/login` 으로 로그인 → Tests에서 `access_token`·`user_id` 자동 저장.
3. 이후 모든 채팅 요청에 **Header**: `Authorization: Bearer {{access_token}}` 추가.
4. 채팅 API는 모두 `{{base_url}}/api/chat/...` 로 호출.

이렇게 하면 Postman만으로 로그인 → 채팅방 목록/생성 → 메시지 조회·전송까지 한 흐름으로 테스트할 수 있습니다.

---

## 5. 401 Unauthorized 대응 — 서버 로그 확인

401이 나오면 **어디서** 거절됐는지 로그로 확인하는 것이 좋습니다.

### 5.1 서버 로그 보는 방법 (SSH + Docker)

서버에 SSH 접속한 뒤, 컨테이너 로그를 봅니다.

```bash
# Gateway 로그 (최근 200줄, 실시간은 -f)
docker logs dorandoran-gateway --tail 200

# Auth 서비스 로그 (토큰 검증 요청이 여기로 옴)
docker logs dorandoran-auth --tail 200

# 채팅 요청이 Gateway → Chat으로 가므로, 필요 시 Chat 로그도
docker logs dorandoran-chat --tail 200
```

- **실시간**으로 보려면: `docker logs dorandoran-gateway -f` (Ctrl+C로 종료)
- **특정 시간 이후**만 보려면: `docker logs dorandoran-gateway --since 5m`

### 5.2 로그에서 볼 것

| 로그 메시지 (대략) | 의미 | 조치 |
|-------------------|------|------|
| `Authorization 헤더가 없거나 형식이 잘못됨` | Gateway: Bearer 토큰이 없거나 `Bearer ` 로 시작하지 않음 | Postman Headers에 `Authorization` / `Bearer {{access_token}}` 확인, 공백 하나만 |
| `JWT 검증 실패` | Gateway가 Auth에 검증 요청했는데 실패 | 토큰 만료·변조 가능성 → **다시 로그인**해서 새 `access_token` 사용 |
| Auth 쪽 4xx/5xx 또는 에러 로그 | Auth 서비스에서 토큰 거절 | 동일: 재로그인, 또는 Auth 설정/시크릿 확인 |

### 5.3 자주 하는 확인

1. **Header 이름/값**
   - Key: `Authorization` (오타 없이)
   - Value: `Bearer {{access_token}}` (Bearer 뒤에 **공백 한 칸**, 토큰은 변수로)

2. **토큰 만료**
   - 로그인한 지 오래됐으면 **다시 로그인** 후 응답의 `accessToken`으로 Environment `access_token` 갱신.

3. **Environment**
   - `access_token`에 **실제 토큰 문자열**이 들어가 있는지 확인 (다른 요청에서 저장한 값이 있는지).

4. **인증 제외 경로**
   - `/api/auth/login`, `/actuator/**` 등은 401 없이 통과합니다. **채팅 API**(`/api/chat/**`)는 인증 필요하므로 위 항목들을 만족해야 합니다.

### 5.4 서버 로그에서 확인한 401 원인 예시 (2026-02-06)

실제 서버 Auth 로그에서 다음이 기록되었습니다:

```
토큰 검증 실패: JWT expired 1226304 milliseconds ago at 2026-02-06T05:32:11.000Z.
Current time: 2026-02-06T05:52:37.304Z. Allowed clock skew: 0 milliseconds.
```

- **원인**: 사용 중이던 JWT가 **만료**된 상태에서 `/api/auth/validate` 호출 → Auth가 거절 → Gateway가 401 반환.
- **조치**: **다시 로그인**해서 새 `accessToken`을 받고, Postman Environment의 `access_token`을 그 값으로 갱신한 뒤 채팅 API를 다시 호출하세요.
