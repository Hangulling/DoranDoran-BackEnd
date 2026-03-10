# WebSocket Chat API 명세 (iOS 대응)

> iOS WKWebView에서 SSE가 동작하지 않는 문제를 해결하기 위해 WebSocket 기반 실시간 이벤트 수신을 지원합니다.
> 기존 SSE와 동일한 이벤트를 WebSocket으로도 전송하며, 데이터 형식만 다릅니다.

---

## 1. 개요

| 항목 | 내용 |
|------|------|
| **엔드포인트** | `wss://{host}/ws/chat/{chatroomId}` |
| **프로토콜** | WebSocket |
| **인증** | JWT (쿼리 파라미터 `token`으로 전달) |
| **용도** | AI 채팅 실시간 이벤트 수신 (intimacy_analysis, conversation_complete 등) |
| **대상** | iOS 네이티브 앱 (Capacitor), Android/Web은 기존 SSE 사용 |

---

## 2. 연결 (Handshake)

### 2.1 URL 형식

```
wss://{API_BASE_URL}/ws/chat/{chatroomId}?userId={userId}&token={jwt}
```

| 파라미터 | 필수 | 설명 |
|----------|------|------|
| `chatroomId` | O | 채팅방 UUID (경로) |
| `userId` | O | 사용자 UUID (쿼리) |
| `token` | O | JWT 액세스 토큰 (쿼리) |

> **참고**: 브라우저 WebSocket API는 커스텀 헤더(Authorization)를 지원하지 않으므로 JWT를 쿼리 파라미터로 전달합니다. Gateway에서 검증 후 token은 다운스트림(Chat)에 전달되지 않습니다.

### 2.2 인증 흐름

```
[클라이언트]  GET /ws/chat/{chatroomId}?userId=xxx&token=JWT (WebSocket Upgrade)
       ↓
[Gateway]  1. token 파라미터 추출
           2. Auth 서비스로 JWT 검증 (/api/auth/validate)
           3. 성공 시 JWT sub에서 userId 추출
           4. X-User-Id 헤더 주입
           5. 쿼리에서 token 제거 후 Chat 서비스로 전달
       ↓
[Chat]     1. HandshakeInterceptor: X-User-Id 헤더 확인
           2. 채팅방 접근 권한 검증 (hasAccessToChatroom)
           3. WebSocket 연결 수립
```

### 2.3 에러 응답

| 상황 | HTTP 상태 | 설명 |
|------|-----------|------|
| token 없음 | 401 Unauthorized | Gateway에서 반환 |
| token 유효하지 않음 | 401 Unauthorized | Auth 검증 실패 |
| X-User-Id 없음 | 핸드셰이크 거부 | Chat HandshakeInterceptor |
| 채팅방 접근 권한 없음 | 1008 Policy Violation | ChatWebSocketHandler |

---

## 3. 서버 → 클라이언트 메시지 (수신)

### 3.1 메시지 형식

모든 서버 푸시 메시지는 아래 JSON 형식입니다.

```json
{
  "event": "{이벤트명}",
  "data": { ... }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `event` | string | 이벤트 종류 |
| `data` | object \| string | 이벤트 페이로드 (SSE와 동일 구조) |

### 3.2 이벤트 목록

| 이벤트명 | 출처 | data 형식 | 설명 |
|----------|------|-----------|------|
| `intimacy_analysis` | MultiAgentOrchestrator | object | 친밀도 분석 결과 |
| `vocabulary_extracted` | MultiAgentOrchestrator | object | 어휘 추출 결과 |
| `vocabulary_translated` | MultiAgentOrchestrator | object | 번역 결과 |
| `conversation_complete` | MultiAgentOrchestrator | object | 대화 완료 (messageId, content) |
| `aggregated_complete` | MultiAgentOrchestrator | object | 전체 집계 완료 |
| `conversation_cancelled` | MultiAgentOrchestrator | object | 대화 취소 (messageId) |
| `conversation_error` | MultiAgentOrchestrator | string | 대화 오류 |
| `agent_error` | MultiAgentOrchestrator | string | Agent 오류 |
| `ai_info` | AIService | string | AI 모델 정보 |
| `ai_usage` | AIService | string | 토큰 사용량 (토큰 단위) |
| `ai_response` | AIService | string | AI 응답 스트리밍 청크 |
| `ai_response_done` | AIService | object | AI 응답 완료 (messageId) |
| `ai_usage_total` | AIService | string | 총 토큰 사용량 |
| `ai_error` | AIService | string | AI 오류 |
| `greeting_bot_message` | GreetingService | object | 인사 봇 메시지 |
| `greeting_guide_message` | GreetingService | object | 인사 가이드 메시지 |
| `greeting_message` | ConnectionGreetingService | object | 인사 시작 알림 |

### 3.3 data 구조 예시

**intimacy_analysis**
```json
{
  "detectedLevel": 2,
  "correctedSentence": "...",
  "feedback": { "ko": "...", "en": "..." },
  "corrections": [...],
  "alternativeExpressions": [...]
}
```

**conversation_complete**
```json
{
  "messageId": "uuid",
  "content": "{...}"
}
```

---

## 4. 클라이언트 → 서버 메시지 (전송)

사용자 메시지 전송 시 아래 형식을 사용합니다.

### 4.1 메시지 포맷 (텍스트)

```
{senderId}|{senderType}|{content}
```

| 필드 | 값 | 설명 |
|------|-----|------|
| senderId | UUID | 사용자 ID |
| senderType | `"user"` | 발신자 타입 (사용자만 허용) |
| content | string | 메시지 본문 |

### 4.2 예시

```
550e8400-e29b-41d4-a716-446655440000|user|안녕하세요
```

> **참고**: 현재 프론트엔드(iOS)는 메시지 전송 시 REST API(`POST /api/chat/chatrooms/{id}/messages`)를 사용합니다. WebSocket 수신 전용으로 활용할 수 있으며, 전송도 WebSocket으로 처리하려면 위 포맷을 따르면 됩니다.

---

## 5. 연결 시 동작

- **첫 연결 시**: `ConnectionGreetingService`가 AI 인사 발송 여부를 판단하고, 필요 시 `greeting_message` → `greeting_bot_message`, `greeting_guide_message` 순으로 이벤트 전송
- **SSE와 동등**: 동일한 비즈니스 로직이 SSE와 WebSocket 양쪽으로 이벤트를 전송

---

## 6. 프론트엔드 구현

### 6.1 사용 방식

iOS에서만 WebSocket을 사용하고, Android/Web은 기존 SSE를 유지합니다.

```tsx
// useChatStream.tsx - 플랫폼 분기
const isIos = Capacitor.isNativePlatform() && Capacitor.getPlatform() === 'ios'

if (isIos) {
  return useChatStreamOverWebSocket(...)  // WebSocket
}
// 그 외: EventSourcePolyfill (SSE)
```

### 6.2 API 함수

**getWebSocketUrl** (`dorandoran-frontend/src/api/chats.ts`)

```ts
getWebSocketUrl(chatroomId: string, userId?: string, token?: string): string
```

- `token`: JWT. `tokenService.access` 또는 `accessToken` 파라미터 사용
- Base URL의 `https://` → `wss://`, `http://` → `ws://` 변환

### 6.3 훅: useChatStreamOverWebSocket

**경로**: `dorandoran-frontend/src/hooks/chat/useChatStreamOverWebSocket.ts`

**시그니처**
```ts
function useChatStreamOverWebSocket<T>(
  chatroomId: string,
  userId?: string,
  accessToken?: string,
  onEventReceived?: (eventType: string, data: T) => void,
  onError?: (event: Event | unknown) => void,
  onOpen?: () => void,
  retryKey?: number,
  enabled?: boolean
): { isLoading: boolean; error: Error | null }
```

**동작**
- `enabled`가 true이고 `chatroomId`가 있을 때만 연결
- `accessToken` 또는 `tokenService.access`를 URL 쿼리에 포함
- 수신 메시지: `JSON.parse` → `{ event, data }` → `onEventReceived(event, data)` 호출
- 재연결: 최대 5회, 지수 백오프 (3s × 2^N, 최대 30s)
- 앱 포그라운드 복귀 시 재연결, 백그라운드 전환 시 연결 해제

### 6.4 이벤트 핸들링

SSE와 동일한 `onEventReceived(eventType, data)` 콜백 사용. `useChatInteraction`의 `handleSseEvent`와 호환됩니다.

```ts
onEventReceived={(eventType, data) => {
  switch (eventType) {
    case 'intimacy_analysis': ...
    case 'conversation_complete': ...
    // SSE와 동일한 처리
  }
}}
```

---

## 7. 엔드포인트 정의

**경로**: `dorandoran-frontend/src/api/endpoints.ts`

```ts
WEBSOCKET_CHAT: (chatroomId: string, userId?: string, token?: string) =>
  `/ws/chat/${chatroomId}${params ? `?${params}` : ''}`
```

---

## 8. Gateway 라우팅

```yaml
# application.yml / application-docker.yml
- id: chat-websocket
  uri: http://dorandoran-chat:8083
  predicates:
    - Path=/ws/chat/**
```

- Spring Cloud Gateway가 WebSocket 업그레이드 요청을 Chat 서비스로 프록시
- `/ws/chat/` 경로는 JWT 검증 제외 목록에 없음 → token 쿼리 파라미터로 검증

---

## 9. 참고

- [iOS_SSE_ROOT_CAUSE_ANALYSIS_V2.md](iOS_SSE_ROOT_CAUSE_ANALYSIS_V2.md): iOS SSE 실패 원인 분석
- [CHAT_SERVICE_DESIGN.md](../chat/CHAT_SERVICE_DESIGN.md): 채팅 서비스 설계
