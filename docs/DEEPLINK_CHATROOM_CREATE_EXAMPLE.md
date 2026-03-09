# 딥링크 채팅방 생성 + 토큰 Refresh 예시 코드

백엔드 Auth API 스펙에 맞춘 TypeScript 예시입니다.  
푸시 클릭 시 accessToken이 만료되어 있어도 **401 → refresh → 1회 재시도**로 처리합니다.

---

## 백엔드 스펙 요약

| API | Method | 요청 | 응답 |
|-----|--------|------|------|
| 토큰 갱신 | `POST /api/auth/refresh` | Body: `{ "refreshToken": "..." }` | 200: `{ success: true, data: { accessToken, refreshToken, tokenType, expiresIn, user } }` |
| 채팅방 생성 | `GET /api/deeplink/chatroom/create?chatbotId=...&topic=...&concept=...` | Header: `Authorization: Bearer <accessToken>` | 200: 채팅방 정보 JSON |

- Refresh 실패 시: 400 등, body에 `{ success: false, message, errorCode }`.
- 백엔드에서 refresh 시 **refreshToken을 로테이션**할 수 있으므로, 응답의 `data.refreshToken`을 반드시 다시 저장해야 합니다.

---

## 타입 정의

```ts
const API_BASE = 'https://api.doran-chat.com';

interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user?: { id: string; email: string; name?: string; [k: string]: unknown };
}

interface ApiResponse<T> {
  success: boolean;
  data?: T;
  message?: string;
  errorCode?: string;
}

interface ChatRoomResponse {
  id: string;
  name?: string;
  concept?: string;
  intimacyLevel?: number | null;
  [k: string]: unknown;
}
```

---

## 토큰 저장/조회

앱에서 사용하는 스토리지(SecureStore, AsyncStorage, Zustand 등)에 맞게 구현합니다.

```ts
async function getStoredTokens(): Promise<{ accessToken: string; refreshToken: string } | null> {
  const accessToken = await yourStorage.get('accessToken');
  const refreshToken = await yourStorage.get('refreshToken');
  if (!accessToken || !refreshToken) return null;
  return { accessToken, refreshToken };
}

async function saveTokens(data: LoginResponse): Promise<void> {
  await yourStorage.set('accessToken', data.accessToken);
  await yourStorage.set('refreshToken', data.refreshToken);
  if (data.expiresIn) {
    await yourStorage.set('accessTokenExpiresAt', String(Date.now() + data.expiresIn * 1000));
  }
}
```

---

## Refresh 호출 (백엔드 스펙 그대로)

```ts
async function refreshAccessToken(): Promise<LoginResponse | null> {
  const tokens = await getStoredTokens();
  if (!tokens?.refreshToken) return null;

  const res = await fetch(`${API_BASE}/api/auth/refresh`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken: tokens.refreshToken }),
  });

  const json: ApiResponse<LoginResponse> = await res.json();
  if (!res.ok || !json.success || !json.data) return null;

  await saveTokens(json.data);
  return json.data;
}
```

---

## 딥링크 채팅방 생성 (401 시 refresh 후 1회 재시도)

```ts
export interface CreateChatroomFromDeeplinkParams {
  chatbotId: string;
  topic?: string;
  concept?: string;
}

export async function createChatroomFromDeeplink(
  params: CreateChatroomFromDeeplinkParams,
  onSessionExpired?: () => void
): Promise<ChatRoomResponse | null> {
  const tokens = await getStoredTokens();
  if (!tokens) {
    onSessionExpired?.();
    return null;
  }

  const query = new URLSearchParams({
    chatbotId: params.chatbotId,
    ...(params.topic && { topic: params.topic }),
    ...(params.concept && { concept: params.concept }),
  });
  const url = `${API_BASE}/api/deeplink/chatroom/create?${query}`;

  let accessToken = tokens.accessToken;

  let res = await fetch(url, {
    method: 'GET',
    headers: { Authorization: `Bearer ${accessToken}` },
  });

  // 401이면 refresh 후 한 번만 재시도
  if (res.status === 401) {
    const refreshed = await refreshAccessToken();
    if (!refreshed) {
      onSessionExpired?.();
      return null;
    }
    accessToken = refreshed.accessToken;
    res = await fetch(url, {
      method: 'GET',
      headers: { Authorization: `Bearer ${accessToken}` },
    });
  }

  if (!res.ok) {
    if (res.status === 401) onSessionExpired?.();
    return null;
  }

  return res.json() as Promise<ChatRoomResponse>;
}
```

---

## 딥링크 핸들러에서 사용 예시

```ts
// 딥링크 URL 예: dorandoran://chatroom/create?chatbotId=xxx&topic=음식&concept=honey
function handleDeeplink(deeplinkUrl: string, pushData?: { startMessage?: string }) {
  const url = new URL(deeplinkUrl.replace('dorandoran://', 'https://placeholder/'));
  const chatbotId = url.searchParams.get('chatbotId');
  const topic = url.searchParams.get('topic') ?? '';
  const concept = url.searchParams.get('concept') ?? 'friend';

  if (!chatbotId) return;

  createChatroomFromDeeplink(
    { chatbotId, topic, concept },
    () => navigateToLogin()
  ).then((chatroom) => {
    if (!chatroom) return;
    navigateToIntimacySelect({
      chatroomId: chatroom.id,
      startMessage: pushData?.startMessage,
    });
  });
}
```

---

## 흐름 정리

1. 푸시 탭 → `dorandoran://chatroom/create?...` 수신.
2. `createChatroomFromDeeplink({ chatbotId, topic, concept }, onSessionExpired)` 호출.
3. 저장된 accessToken으로 `GET /api/deeplink/chatroom/create` 요청.
4. **200** → 채팅방 정보 반환 → intimacy 선택 화면으로 이동.
5. **401** → `POST /api/auth/refresh`로 새 토큰 발급 → 저장 후 3번 요청 **한 번 더**.
6. 재시도 후에도 401이거나 refresh 실패 → `onSessionExpired()` 호출 (로그인 화면 등).
7. 사용자가 intimacyLevel 선택 후 `POST /api/chat/chatrooms/{chatroomId}/start-greeting` 호출 (여기서도 동일하게 401 시 refresh + 재시도 적용 권장).

이렇게 하면 푸시를 오래 두었다가 눌러도 accessToken 만료 시 자동으로 갱신 후 채팅방 생성이 이루어지고, refreshToken까지 만료된 경우에만 로그인 유도가 됩니다.
