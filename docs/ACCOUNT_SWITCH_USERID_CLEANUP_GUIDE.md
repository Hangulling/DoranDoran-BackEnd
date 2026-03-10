# 계정 전환 시 userId/채팅 URL 정리 가이드

- **대상**: 웹 프론트엔드(dorandoran), Capacitor 네이티브 앱(com.koach.app)
- **목적**: 같은 기기에서 계정 A → 계정 B로 전환한 뒤에도 **이전 계정(A)의 userId가 URL·상태에 남아** 채팅/스트림 API 요청에 사용되어 403·SSE 거부 등이 나는 문제를 방지
- **작성일**: 2026-02-06

---

## 1. 현상 요약

서버 로그에서 다음이 확인됨:

- **JWT(Authorization)**: `koachchatapp@gmail.com` (userId: `3fa11b2f-...`)
- **요청 URL**: `GET /api/chat/stream/{chatroomId}?userId=52dee622-...` (koala0567의 userId)

즉, **인증은 B 계정**인데 **채팅/스트림 URL에는 A 계정의 userId가 쿼리로 들어간** 상태.  
이렇게 되면:

- 채팅 서버는 JWT·X-User-Id로 B 사용자로 요청을 처리하는데, URL의 `userId`는 A라서
- SSE 접근 거부(다른 사용자 소유 방으로 판단) 또는 일관성 없는 동작이 발생할 수 있음.

---

## 2. 원인

- **로그아웃/계정 전환 시** 이전 계정의 **userId**와 **채팅방 매핑(roomId 등)** 이 완전히 초기화되지 않음.
- **채팅/스트림 URL**을 만들 때 **한 번 읽은 userId**를 계속 쓰거나, **저장소/메모리에 남은 예전 userId**를 사용함.
- **Capacitor 앱**은 WebView/앱 생명주기 때문에 웹과 다른 저장소·탭 동작을 할 수 있어, 웹에서는 드물어도 앱에서 재현될 수 있음.

---

## 3. 해결 원칙

1. **로그아웃·계정 전환 시 반드시 “현재 사용자” 관련 상태 전부 제거**
2. **채팅/스트림 관련 URL은 항상 “지금 로그인한 사용자의 userId” 하나만 사용**
3. **URL/상태를 만들 때마다 최신 userId를 조회**하고, 예전에 만든 URL을 재사용하지 않기.

---

## 4. 웹 프론트엔드(dorandoran) 기준 체크리스트

### 4.1 로그아웃 시 초기화

다음은 **한 번에 같이** 수행하는 것을 권장합니다.

| 대상 | 방법 | 비고 |
|------|------|------|
| **토큰** | `sessionStorage`에서 `accessToken`, `refreshToken` 제거 | `api.ts`의 tokenService.clear() 또는 동일 로직 |
| **사용자 스토어** | `useUserStore.getState().reset()` | userId, name 비우기 |
| **채팅방 매핑** | `useRoomIdStore.getState().reset()` | roomsMap, chatbotMap 비우기 |
| **기타 앱 스토어** | 친밀도·코치·모달 등 사용자 단위 상태 리셋 | `useSidebarAction`의 `resetAllStores()` 참고 |

**참고 코드**: `src/hooks/useSidebarAction.ts`의 `resetAllStores()` + 로그아웃 후 `handleAppReset()` 호출.  
로그아웃 API 호출 **성공 후** 곧바로 위 초기화를 수행하고, 그 다음 로그인 화면으로 이동하는 흐름을 유지할 것.

### 4.2 채팅/스트림 URL에 쓰는 userId

- **단일 출처**:  
  - **페이지/훅 진입 시**: `useUserStore(state => state.id)` 또는 `useFetchUser()`에서 받은 `userId`.  
  - **채팅/스트림 훅**에서는 이 값만 사용하고, **다른 경로(예: URL 쿼리, 예전 클로저)의 userId는 사용하지 않기**.
- **SSE/스트림**  
  - `getSseUrl(chatroomId, userId)` / `useChatStream(chatroomId, userId, accessToken)` 에 넘기는 `userId`는  
    **항상 현재 로그인 사용자**의 id여야 함.  
  - 로그아웃 후 재로그인했다면, **새로 마운트된 화면/훅**에서는 `useUserStore`나 `getCurrentUser()`로 **새 userId**를 받은 뒤 그 값만 사용.

### 4.3 계정 전환 시나리오

- **“로그아웃 → 다른 계정 로그인”**  
  - 로그아웃 시 위 4.1 초기화가 한 번에 실행되는지 확인.  
  - 로그인 화면으로 이동한 뒤, 새 계정 로그인 시 `getCurrentUser()`로 새 사용자 정보를 받고, 그 id가 `useUserStore` 등에만 설정되는지 확인.  
  - 채팅/스트림은 **이 새 userId가 설정된 이후**에만 열리도록 하면 됨.

---

## 5. Capacitor 앱(네이티브) 기준 체크리스트

### 5.1 저장소

- **웹와 동일한 스토어를 쓰는 경우**  
  - 로그아웃·계정 전환 시 웹과 **같은 순서**로 `resetAllStores()` 및 토큰 삭제를 수행.  
  - WebView/앱이 `sessionStorage`를 탭/프로세스별로 유지할 수 있으므로, **로그아웃 시 명시적으로**  
    `accessToken`, `refreshToken`, `user-storage`, `room-id-storage` 등 관련 키를 제거.
- **앱 전용 저장소(Preferences, SecureStorage 등)**  
  - userId, roomId 매핑, “마지막 사용자” 같은 값을 저장했다면, **로그아웃 시 반드시 삭제**하거나 초기화.

### 5.2 채팅/스트림 요청

- **모든 채팅·스트림 요청**  
  - `userId` 쿼리 파라미터, `X-User-Id`(앱에서 직접 붙이는 경우) 모두 **현재 로그인한 사용자 id**와 일치시키기.  
  - JWT는 서버가 검증 후 `X-User-Id`를 붙여 주므로, **클라이언트가 URL에 넣는 userId**만 JWT의 사용자와 맞추면 됨.
- **요청 직전에 userId 다시 읽기**  
  - 스트림 연결·채팅방 입장 시, **요청을 보내기 직전**에  
    “지금 로그인한 사용자 id”를 저장소/스토어에서 한 번 더 읽어서 그 값으로 URL을 구성하는 것을 권장.

### 5.3 계정 전환 테스트

- 다음 시나리오로 동작 확인 권장:  
  1. 계정 A로 로그인 → 채팅 목록/채팅방 진입 정상 동작 확인.  
  2. 로그아웃(앱 내 로그아웃 버튼).  
  3. 계정 B로 로그인.  
  4. 채팅 목록·채팅방·SSE 스트림이 **모두 B의 userId**로만 요청되는지 확인.  
- 서버 로그/프록시로 실제 요청 URL의 `userId` 쿼리와 JWT의 sub가 **같은 사용자**인지 확인하면 좋음.

---

## 6. 공통 요약

| 구분 | 내용 |
|------|------|
| **로그아웃/계정 전환** | 토큰 + useUserStore + useRoomIdStore(및 기타 사용자 단위 상태) 한 번에 초기화. |
| **채팅/스트림 URL** | 항상 “현재 로그인 사용자”의 userId 하나만 사용; 예전 URL·캐시된 userId 재사용 금지. |
| **단일 출처** | userId는 `useUserStore` 또는 `getCurrentUser()` 응답에서만 취하고, 채팅/스트림 훅은 이 값만 사용. |

이렇게 하면 “계정 A → B 전환 후에도 이전 A의 userId가 스트림 URL에 남는” 상황을 방지할 수 있습니다.
