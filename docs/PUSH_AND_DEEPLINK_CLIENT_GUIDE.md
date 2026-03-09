# 모바일 클라이언트 푸시·딥링크 구현 가이드

> Capacitor 기반 모바일 앱에서 푸시 알림 등록, 푸시 클릭 시 채팅방 진입·채팅방 생성 처리 방법

## 1. 푸시 알림 등록 (Capacitor)

### 1.1 패키지 설치

```bash
npm install @capacitor/push-notifications
npx cap sync
```

### 1.2 초기화 및 FCM 토큰 등록

앱 기동 시 권한 요청 후 FCM 토큰을 백엔드에 등록한다.

```typescript
import { PushNotifications } from '@capacitor/push-notifications';
import { Capacitor } from '@capacitor/core';

async function initPush() {
  if (!Capacitor.isNativePlatform()) return;

  const perm = await PushNotifications.checkPermissions();
  if (perm.receive === 'prompt') {
    await PushNotifications.requestPermissions();
  }
  if ((await PushNotifications.checkPermissions()).receive !== 'granted') {
    return; // 권한 거부 시 스킵
  }

  await PushNotifications.register();

  PushNotifications.addListener('registration', async (ev) => {
    const token = ev.value;
    await registerTokenToBackend(token);
  });

  PushNotifications.addListener('pushNotificationActionPerformed', (ev) => {
    handlePushClick(ev.notification);
  });
}

async function registerTokenToBackend(token: string) {
  await fetch(`${API_BASE}/api/notifications/register`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${accessToken}`,
    },
    body: JSON.stringify({
      token,
      platform: Capacitor.getPlatform(), // 'ios' | 'android'
    }),
  });
}
```

- **API**: `POST /api/notifications/register`  
- **헤더**: `Authorization: Bearer {accessToken}`, `X-User-Id`는 Gateway에서 JWT로부터 설정되는 경우 생략 가능  
- **Body**: `{ "token": "fcm_token", "platform": "android" }`

---

## 2. 푸시 클릭 시 처리

FCM **data** 페이로드에 따라 분기한다.

### 2.1 채팅방 진입 (기존 채팅방)

**data에 `chatroomId`가 있는 경우** (예: 새 메시지 알림)

1. `chatroomId`, `messageId`, `startMessage`, `deeplink`/`universalLink` 등 추출
2. 해당 채팅방 화면으로 이동
3. **채팅방 정보**: `GET /api/chat/chatrooms/{chatroomId}`
4. **메시지 목록**: `GET /api/chat/chatrooms/{chatroomId}/messages?page=0&size=50`
5. **startMessage**가 비어 있지 않으면, 진입 후  
   `POST /api/chat/chatrooms/{chatroomId}/messages` 로 `content`에 `startMessage`를 넣어 한 번 전송해 대화 시작

자세한 흐름은 [PUSH_TO_CHAT_FLOW.md](./PUSH_TO_CHAT_FLOW.md)(동일 디렉터리) 참고.

### 2.2 채팅방 생성 (딥링크)

**data에 `deeplink`가 있고, 스킴이 `dorandoran://chatroom/create` 인 경우**

1. data에서 `deeplink` 또는 `chatbotId`, `topic`, `concept`, `intimacyLevel` 추출
2. 백엔드 **딥링크 채팅방 생성 API** 호출 후, 반환된 채팅방으로 이동

**호출 API**: `GET /api/deeplink/chatroom/create`

- **쿼리**: `chatbotId`(필수), `topic`, `concept`(기본 FRIEND), `intimacyLevel`(기본 2)
- **헤더**: `Authorization: Bearer {accessToken}` (Gateway가 JWT 검증 후 `X-User-Id` 설정)

예시:

```typescript
function handlePushClick(notification: any) {
  const data = notification.data || {};
  const chatroomId = data.chatroomId;
  const deeplink = data.deeplink || data.url;

  if (chatroomId) {
    navigateToChat(chatroomId, data.messageId, data.startMessage);
    return;
  }

  if (deeplink && deeplink.includes('chatroom/create')) {
    openChatroomCreateFromPush(data);
    return;
  }
}

async function openChatroomCreateFromPush(data: any) {
  const params = new URLSearchParams();
  if (data.chatbotId) params.set('chatbotId', data.chatbotId);
  if (data.topic) params.set('topic', data.topic);
  if (data.concept) params.set('concept', data.concept || 'FRIEND');
  if (data.intimacyLevel) params.set('intimacyLevel', data.intimacyLevel);

  const res = await fetch(
    `${API_BASE}/api/deeplink/chatroom/create?${params}`,
    { headers: { Authorization: `Bearer ${accessToken}` } }
  );
  const chatroom = await res.json();
  navigateToChat(chatroom.id);
}
```

---

## 3. 앱 딥링크 (appUrlOpen)

외부에서 `dorandoran://...` 로 앱이 열린 경우 `App.addListener('appUrlOpen', ...)` 로 처리한다.

```typescript
import { App } from '@capacitor/app';

App.addListener('appUrlOpen', (ev) => {
  const url = ev.url; // e.g. dorandoran://chatroom/create?chatbotId=...&topic=...
  handleDeepLinkUrl(url);
});

function handleDeepLinkUrl(url: string) {
  const u = new URL(url);
  if (u.pathname === '//chatroom/create' || url.includes('chatroom/create')) {
    const params = u.searchParams;
    openChatroomCreateFromPush({
      chatbotId: params.get('chatbotId'),
      topic: params.get('topic'),
      concept: params.get('concept') || 'FRIEND',
      intimacyLevel: params.get('intimacyLevel') || '2',
    });
  }
}
```

- **chatroom/create** → 위와 동일하게 `GET /api/deeplink/chatroom/create` 호출 후 채팅 화면으로 이동
- 다른 경로(예: `/archive/{storeId}`)는 `GET /api/deeplink/route?path=...` 로 화면/파라미터만 조회해 라우팅할 수 있음

---

## 4. 백엔드 API 요약

| 용도 | 메서드/경로 | 비고 |
|------|-------------|------|
| FCM 토큰 등록 | `POST /api/notifications/register` | Body: `token`, `platform` |
| 푸시 클릭 → 채팅방 진입 | 기존 Chat API 사용 | [PUSH_TO_CHAT_FLOW.md](./PUSH_TO_CHAT_FLOW.md) |
| 푸시/딥링크 → 채팅방 생성 | `GET /api/deeplink/chatroom/create?chatbotId=&topic=&concept=&intimacyLevel=` | X-User-Id(JWT 기반) |
| 경로 → 화면/파라미터 | `GET /api/deeplink/route?path=...` | 응답: `{ screen, params }` |

---

## 5. 참고 문서

- [PUSH_NOTIFICATION_DEVELOPMENT_PLAN.md](./PUSH_NOTIFICATION_DEVELOPMENT_PLAN.md) — 푸시·딥링크 전체 설계
- [API_SPECIFICATION_V3.md](./API_SPECIFICATION_V3.md) §5.4 — 알림(푸시) API 명세
- [PUSH_TO_CHAT_FLOW.md](./PUSH_TO_CHAT_FLOW.md) — 푸시 클릭 시 채팅방 진입 상세 흐름 (문서 존재 시)
