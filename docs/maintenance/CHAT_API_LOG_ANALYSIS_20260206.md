# 채팅 API 서버 로그 분석 요약 (2026-02-06)

- **분석 구간**: 최근 48~72시간 (chat + gateway)
- **대상**: `api.doran-chat.com` (dorandoran-chat, dorandoran-gateway)

---

## 1. Gateway 로그 요약

### 1.1 정상 동작

- **koala0567@gmail.com** (52dee622-0dd1-4a3c-8b2e-a6c7f700f515): POST/GET chat, last-interactions, stream 요청 시 JWT 검증 통과, `X-User-Id` 정상 설정.
- **koachchatapp@gmail.com** (3fa11b2f-0e4d-4332-aa76-308407ec68ca): 동일하게 인증 통과, `X-User-Id` 설정됨.
- 401 "Authorization 헤더가 없거나 형식이 잘못됨" 로그는 이번 구간에서 **없음** → 채팅 경로로의 인증 실패는 적음.

### 1.2 발견 이슈

| 시각 (KST) | 내용 |
|------------|------|
| **09:01:41** | **userId 불일치**: `GET /api/chat/stream/4a4b49a3-...?userId=52dee622-...` (쿼리에는 koala0567) 인데, 요청 헤더의 JWT·X-User-Id는 **koachchatapp** (3fa11b2f). 동일 기기(Android, com.koach.app). → 계정 전환 후에도 **URL에 이전 계정(koala0567) userId가 남아** 스트림 요청에 사용된 사례. |
| **10:11:39** | `JWT 검증 실패: 401 Unauthorized from GET .../api/auth/validate` → 토큰 만료/무효로 1건 401 발생. |

---

## 2. Chat 서비스 로그 요약

### 2.1 duplicate key (23505, idx_chatrooms_user_chatbot)

- **발생 사용자**: `3fa11b2f-0e4d-4332-aa76-308407ec68ca` (koachchatapp) 만 해당.
- **발생 시각 예**: 00:52, 08:44, 08:55, 08:56, 08:59, 09:01, 09:02, 09:03, 09:04, 09:19, 09:20, 10:12, 10:17 등.
- **내용**: `(user_id, chatbot_id)` 조합이 이미 존재하는데 `INSERT` 시도 → **DataIntegrityViolationException** 발생 후 **GlobalExceptionHandler**까지 전달되어 500 응답으로 이어짐.
- **코드**: `ChatService.createRoom` 에는 23505 + `idx_chatrooms_user_chatbot` 시 활성 방 재조회 후 반환하는 방어 로직이 **구현되어 있음**. 다만 이번 수집 로그에는 **"createRoom duplicate detected, returning existing active room"** WARN 로그가 **한 건도 없음** → 실제 운영 중에는 해당 catch 블록이 타지 않고 예외가 그대로 전파되는 것으로 보임. (배포 버전 미반영, 또는 예외 cause chain/제약 이름 차이 가능성.)

### 2.2 SSE 접근 거부

- **08:56:45**: `SSE 접근 거부: userId=52dee622-0dd1-4a3c-8b2e-a6c7f700f515, chatroomId=dd394b80-2a3a-4228-b1a8-58b77f776cec`
- **의미**: 쿼리/헤더의 userId(52dee622, koala0567)로 해당 채팅방에 SSE 접속 시도했으나, 서버가 해당 사용자의 방 소유/접근 권한이 없다고 판단해 거부. 위 1.2의 “스트림 URL에 이전 계정 userId 사용”과 맞물려 발생한 것으로 추정.

### 2.3 기타

- **NoResourceFoundException (No static resource ".")**: 잘못된 경로(예: `GET /.`) 요청으로 추정. 채팅 비즈니스 로직과는 무관.
- **HttpMessageNotWritableException**: SSE 스트림 요청 처리 중 예외가 났을 때, `GlobalExceptionHandler`가 `Content-Type: text/event-stream` 인 응답에 `ErrorResponse`(JSON)를 쓰려다 변환 실패. → SSE 중 에러 시 클라이언트가 깨진 응답을 보게 될 수 있음.
- **PromptGenerationService / GreetingService WARN**: User Service 프롬프트 404, fallback 사용. Greeting은 동작 중.
- **PageImpl 직렬화 WARN**: Spring Data Page 직렬화 안정성 경고. 동작에는 큰 영향 없음.

---

## 3. 정리 및 권장 사항

| 구분 | 요약 | 권장 |
|------|------|------|
| **계정 전환 후 URL/상태** | 스트림 URL에 이전 계정(koala0567) userId가 남아 있고, JWT는 koachchatapp. 그 결과 SSE 접근 거부 발생. | `docs/ACCOUNT_SWITCH_USERID_CLEANUP_GUIDE.md` 적용: 로그아웃/계정 전환 시 userId·채팅 매핑 초기화, 스트림/채팅 URL은 항상 현재 로그인 사용자만 사용. |
| **createRoom 500** | koachchatapp 계정에서 동일 (user, chatbot) 에 대한 중복 생성 시 23505 → 500. 방어 로직은 코드에 있으나 로그상 동작 흔적 없음. | 1) 현재 운영 배포에 해당 방어 로직이 포함된 빌드가 올라가 있는지 확인. 2) 예외 원인 chain에서 `ConstraintViolationException` 및 constraint 이름 `idx_chatrooms_user_chatbot` 이 실제로 나오는지 로그로 확인. 필요 시 catch 조건 완화 또는 로깅 보강. |
| **JWT 401** | 1건: validate 401. | 토큰 갱신/재로그인 플로우 유지. |
| **SSE 에러 응답** | SSE 중 예외 시 JSON 에러를 event-stream으로 보내다 실패. | SSE 전용 예외 처리에서 `text/event-stream` 에 맞는 에러 포맷 사용 또는 별도 처리 검토. |

이 문서는 위 로그 수집·분석 결과를 요약한 것이며, 추후 재수집 시 이 파일을 갱신해 두면 추적에 유리합니다.
