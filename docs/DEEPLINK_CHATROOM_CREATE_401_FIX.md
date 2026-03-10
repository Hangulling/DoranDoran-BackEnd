# 딥링크 채팅방 생성 401 해결 방안

`GET /api/deeplink/chatroom/create?chatbotId=...&topic=...&concept=...` 호출 시 401이 나는 이유는 **게이트웨이에서 JWT를 요구하는데, URL을 그대로 연 요청에는 `Authorization: Bearer` 헤더가 없기 때문**입니다.

---

## 방안 1: 앱에서 딥링크 수신 후 API 호출 (권장)

**백엔드 변경 없음.** 앱만 수정하면 됩니다.

### 흐름

1. 사용자가 푸시를 탭 → OS가 앱을 띄우며 **커스텀 스킴 딥링크** 전달  
   예: `dorandoran://chatroom/create?chatbotId=xxx&topic=음식&concept=friend`
2. **앱의 딥링크 핸들러**에서 URL 수신
3. 쿼리 파라미터(chatbotId, topic, concept) 파싱
4. **저장해 둔 accessToken**으로 아래 API 호출  
   - Method: `GET`  
   - URL: `https://api.doran-chat.com/api/deeplink/chatroom/create?chatbotId=xxx&topic=음식&concept=friend`  
   - Header: `Authorization: Bearer <accessToken>`
5. 응답으로 받은 채팅방 정보로 화면 이동 (예: 해당 채팅방 화면으로)

### 앱 쪽 구현 포인트

- **딥링크 등록**: 앱 스킴 `dorandoran`이 이미 등록되어 있어야 함 (푸시의 deeplink와 동일).
- **딥링크 수신 처리**:  
  - **React Native / Expo**: `Linking.getInitialURL()` + `Linking.addEventListener('url', handler)`  
  - 푸시에서 오는 경우: FCM 데이터 메시지의 `deeplink` 필드로 들어온 URL을 그대로 사용해도 됨 (이미 `dorandoran://chatroom/create?...` 형태).
- **API 호출**:  
  - axios/fetch로 `GET` + 동일 쿼리 + `Authorization: Bearer ${accessToken}`  
  - 토큰은 보통 SecureStore/AsyncStorage 등에 저장된 값 사용.
- **주의**:  
  - 딥링크를 **웹뷰나 외부 브라우저로 열지 말 것**.  
  - 반드시 **앱 내부**에서 URL을 파싱한 뒤, **앱이 직접** 위 API를 호출해야 합니다.

이렇게 하면 서버는 기존처럼 JWT로만 허용해도 되고, 401이 사라집니다.

---

## 방안 2: 해당 API만 인증 통과 (게이트웨이 제외)

URL만 열어도 동작하게 하려면 **게이트웨이에서 이 경로만 JWT 검사에서 제외**해야 합니다.

### 보안 이슈

- 인증을 건너뛰면 **누가** 채팅방을 생성할지 알 수 없음.
- 그래서 **userId를 쿼리로만 받으면**, 악의적 사용자가 다른 사람의 userId를 넣어 채팅방을 만들 수 있음.
- 따라서 **인증 제외 + userId 쿼리**만으로는 부족하고, 아래 중 하나가 필요합니다.
  - **서명된 링크**: 푸시 생성 시 `userId`, `expiry`, `signature=HMAC(secret, userId|chatbotId|topic|concept|expiry)` 를 URL에 붙이고, 서버에서 서명 검증 후 userId 사용.
  - **일회용 토큰**: 푸시 발송 전에 Redis 등에 `token → userId` 저장 후, 링크에 `token=xxx`만 붙이고, API에서 토큰 조회 후 삭제하고 userId 사용.

### 구현 규모

- **방안 2**는 다음이 모두 필요합니다.
  - 게이트웨이: `/api/deeplink/chatroom/**` 인증 제외
  - user 서비스: 푸시에 넣는 URL에 서명(또는 일회용 토큰) 추가
  - chat 서비스: 서명 검증(또는 토큰 조회) 후 userId 설정하고 기존 DeeplinkController 로직 사용
  - user–chat 간 공유 시크릿 또는 Redis 등 공유 저장소

---

## 정리

| 구분 | 방안 1 (앱에서 API 호출) | 방안 2 (API 인증 제외) |
|------|---------------------------|-------------------------|
| 백엔드 변경 | 없음 | 게이트웨이 + user + chat 수정 |
| 보안 | JWT로 사용자 식별 유지 | 서명/일회용 토큰 필수 |
| 앱 변경 | 딥링크 핸들러에서 API 호출만 추가 | 없음 (URL만 열면 됨) |
| 추천 | ✅ 권장 | URL만 열어야 하는 요구가 있을 때만 |

**토큰 만료 시**: 푸시를 나중에 눌렀을 때 accessToken이 만료되면 401이 날 수 있으므로, **401 시 refresh 한 번 시도 후 같은 요청 재시도** 로직이 필요합니다. 백엔드 `/api/auth/refresh` 스펙에 맞춘 TypeScript 예시 코드는 [DEEPLINK_CHATROOM_CREATE_EXAMPLE.md](./DEEPLINK_CHATROOM_CREATE_EXAMPLE.md)를 참고하세요.

**권장**: 먼저 **방안 1**으로 앱에서 딥링크 수신 후 `Authorization: Bearer <accessToken>` 붙여 API 호출하도록 구현하는 것이 단순하고 안전합니다.  
정말로 “해당 API만 인증 통과”가 필요하면, 그때 방안 2를 **서명 또는 일회용 토큰**과 함께 설계하는 것이 좋습니다.
