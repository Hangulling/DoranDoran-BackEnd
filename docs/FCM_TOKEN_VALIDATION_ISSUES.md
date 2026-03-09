# FCM 토큰 검증 구조에서 발견된 문제 요소

> 일일 푸시 실패 원인(`FCM token not found`, `The registration token is not a valid FCM registration token`)과 연관된 토큰 검증/처리 이슈 정리.

---

## 1. 발송 실패 시 “유효하지 않은 토큰”을 DB에서 제거하지 않음 (핵심)

**위치**: `PushNotificationService.isUnregisteredToken()`, `sendByTopic()` catch 블록

**현상**  
- **UNREGISTERED**만 “무효 토큰”으로 간주하고 `deleteToken()` 후 다음 토큰으로 진행  
- **INVALID_ARGUMENT** (`The registration token is not a valid FCM registration token`)는 처리하지 않아:
  - `sendToUser` / `sendChatroomCreatePush`: 로그만 남기고 다음 토큰 시도 (동작은 나쁘지 않음)
  - **`sendByTopic`**: `RuntimeException("FCM send failed")`를 던져 **해당 사용자 전체 실패**로 처리하고, **해당 토큰은 DB에 그대로 유지**

**결과**  
- 같은 잘못된/만료된 토큰이 DB에 남아 매일 스케줄에서 동일 에러 반복  
- Firebase 권장: “유효하지 않은 토큰은 제거하고, 새 토큰만 유지”

**권장**  
- `FirebaseMessagingException`에서 **INVALID_ARGUMENT**도 “삭제 대상 토큰”으로 간주  
- `sendByTopic`에서도 UNREGISTERED와 동일하게: **해당 토큰 삭제 후 다음 토큰으로 진행**, 전체 실패로 던지지 않기

---

## 2. sendByTopic: 한 토큰 실패 시 나머지 토큰까지 모두 실패 처리

**위치**: `PushNotificationService.sendByTopic()` for 루프

**현상**  
- 한 사용자에게 토큰이 여러 개(예: 구기기 + 신기기)일 수 있음  
- 첫 번째 토큰에서 UNREGISTERED가 아니고 INVALID_ARGUMENT 등이 나면 **즉시 `throw new RuntimeException("FCM send failed")`**  
- 그 사용자의 **다른 유효한 토큰에는 전혀 시도하지 않음**

**결과**  
- 한 기기의 잘못된 토큰 때문에, 같은 사용자의 정상 기기까지 푸시를 못 받음

**권장**  
- UNREGISTERED/INVALID_ARGUMENT 등 “토큰 무효”로 판단되면: **해당 토큰만 삭제하고 `continue`**  
- 모든 토큰을 시도한 뒤, **한 번도 성공이 없었을 때만** 예외 처리 또는 실패 로깅

---

## 3. 등록 시 토큰 정규화/검증 부재

**위치**: `NotificationController.registerFcmToken()`, `FcmTokenService.registerToken()`

**현상**  
- `request.token()`을 **trim 하지 않고** 그대로 저장  
- 앞뒤 공백이 있으면:
  - 동일한 논리적 토큰이 “다른 문자열”로 중복 저장될 수 있음 (`findByUserIdAndToken` 미일치)
  - FCM이 “invalid token”으로 거부할 수 있음  
- **형식 검증 없음**: 길이, FCM 토큰 형식 등 검사하지 않음 (최소 길이조차 없음)

**권장**  
- 등록 전 `token = token != null ? token.trim() : ""`  
- `isBlank()` 체크 후, **최소 길이** (예: FCM 토큰 일반 길이 100자 이상) 등 간단한 형식 검사 검토

---

## 4. (참고) “FCM token not found for user”의 원인

**위치**: `PushNotificationService.sendByTopic()` — `tokens.isEmpty()`일 때

**현상**  
- 해당 `userId`에 대해 **DB에 FCM 토큰이 하나도 없음**  
- 앱에서 토큰을 아직 등록하지 않았거나, 과거에 모두 삭제(UNREGISTERED/INVALID 등)된 경우

**대응**  
- 클라이언트: 앱 기동/로그인 시 `POST /api/notifications/register` 호출 보장  
- 서버: 위 1~3 보완으로 “잘못된 토큰”을 DB에서 정리해, 다음 등록이 정상 반영되도록 유지

---

## 5. 요약 표

| 문제 | 원인 | 권장 조치 |
|------|------|-----------|
| 매일 같은 사용자에서 INVALID_ARGUMENT 반복 | INVALID_ARGUMENT 시 토큰 미삭제 | INVALID_ARGUMENT도 삭제 대상으로 처리 |
| 한 기기 토큰 오류로 전체 실패 | sendByTopic이 첫 에러에서 throw | 토큰별 삭제+continue, 전송 실패는 “모든 토큰 실패 시”만 |
| 동일 토큰 중복/공백 저장 | 등록 시 trim 없음 | 등록 전 token trim + 필요 시 최소 길이 검사 |

---

## 6. 수정 대상 파일

- `user/.../PushNotificationService.java`: `isUnregisteredToken` 확장(또는 `isDeletableInvalidToken` 추가), `sendByTopic`에서 삭제 후 continue, “전체 실패” 조건 정리  
- `user/.../NotificationController.java` 또는 `FcmTokenService.java`: 등록 시 `token.trim()`, 선택적으로 최소 길이 검증
