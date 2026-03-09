# 온보딩·마이페이지 API 명세서 (변경/추가분)

이 문서는 **온보딩 통합 API** 및 **마이페이지 통합 조회 API** 적용으로 추가·변경된 API를 정리한 명세입니다.  
기본 인증·공통 응답 형식은 기존 API 명세서를 참고합니다.

---

## 1. 개요

| 구분 | 내용 |
|------|------|
| 대상 서비스 | User 서비스 (`/api/users`), Auth 서비스 (`/api/auth/me`) |
| 인증 | JWT 필요. Gateway 경유 시 `X-User-Id`, `X-User-Email` 주입 |
| 공통 응답 | `{ "success", "data", "message", "errorCode", "timestamp" }` |

---

## 2. 온보딩 관련 API

### 2.1 PATCH /api/users/{userId}/onboard (변경)

**설명**:  
- **Body 없음**: 기존과 동일. 사용자 `is_onboard`만 `true`로 변경.  
- **Body 있음**: 온보딩 완료 + 관심 주제 + 알림 설정 + 설문을 한 트랜잭션으로 저장.

**경로 파라미터**
- `userId` (string, required): 사용자 UUID

**요청 본문** (application/json, **모두 선택**)

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| topicKeys | string[] | N | 관심 주제 키 목록 |
| pushEnabled | boolean | N | 푸시 알림 허용 여부 |
| referralSource | string | N | 유입 경로: ads, instagram_contents, instagram_reels, facebook_contents, friend, other |
| referralOther | string | N | 유입 경로 기타 (최대 80자) |
| koreanLevel | number | N | 한국어 수준 1~5 |
| purposeKey | string | N | 학습 목적: casual_chats, dating, workplace, school, other |
| purposeOther | string | N | 학습 목적 기타 (최대 80자) |

**요청 예시 (Body 있을 때)**
```json
{
  "topicKeys": ["travel", "food"],
  "pushEnabled": true,
  "referralSource": "instagram_reels",
  "referralOther": null,
  "koreanLevel": 2,
  "purposeKey": "casual_chats",
  "purposeOther": null
}
```

**응답** (200 OK): `ApiResponse<UserDto>`  
- `data`: 업데이트된 사용자 정보 (예: `isOnboard: true`)

**에러**: 400 (잘못된 userId, 유효하지 않은 topicKey, koreanLevel 1~5 이탈 등), 404 (사용자 없음)

---

### 2.2 기존 개별 API (유지)

- `PUT /api/users/{userId}/interests` — 관심 주제 저장 (Body: `{ "topicKeys": ["travel", "food"] }`)
- `PUT /api/users/{userId}/notifications` — 알림 설정 변경 (Body: `{ "pushEnabled": true }`)

마이페이지에서 항목별 수정 시 사용. 온보딩 완료 시에는 **2.1 통합 Body**로 한 번에 보내면 됨.

---

## 3. 마이페이지 관련 API

### 3.1 GET /api/auth/me (기존, 변경 없음)

**설명**: 인증된 현재 사용자의 **기본 정보만** 조회.

**응답**: `ApiResponse<UserDto>`  
- id, email, firstName, lastName, name, isOnboard 등 기본 필드만 포함  
- **미포함**: 관심 주제, 알림 설정, 온보딩 설문

**용도**: 로그인 후 `userId` 확보, 상단/네비에 사용자 이름 등 표시.

---

### 3.2 GET /api/users/{userId}/profile (신규)

**설명**: 유저 기본 정보 + 관심 주제 + 알림 설정 + 온보딩 설문 결과를 **한 번에** 조회.

**경로 파라미터**
- `userId` (string, required): 사용자 UUID

**응답** (200 OK): `ApiResponse<MypageProfileResponse>`

**MypageProfileResponse**

| 필드 | 타입 | 설명 |
|------|------|------|
| user | UserDto | 사용자 기본 정보 |
| interests | UserInterestsResponse | `{ "topics": [ { "key", "label" }, ... ] }` |
| notificationSetting | NotificationSettingsResponse | `{ "pushEnabled": true }` |
| onboardingSurvey | OnboardingSurveyResponse \| null | 설문 미제출 시 null |

**OnboardingSurveyResponse**

| 필드 | 타입 | 설명 |
|------|------|------|
| referralSource | string \| null | 유입 경로 코드 |
| referralOther | string \| null | 유입 경로 기타 |
| koreanLevel | number \| null | 1~5 |
| purposeKey | string \| null | 학습 목적 코드 |
| purposeOther | string \| null | 학습 목적 기타 |

**응답 예시**
```json
{
  "success": true,
  "data": {
    "user": { "id": "uuid", "email": "...", "firstName": "...", "isOnboard": true, ... },
    "interests": { "topics": [ { "key": "travel", "label": "여행" } ] },
    "notificationSetting": { "pushEnabled": true },
    "onboardingSurvey": {
      "referralSource": "instagram_reels",
      "referralOther": null,
      "koreanLevel": 2,
      "purposeKey": "casual_chats",
      "purposeOther": null
    }
  },
  "message": "마이페이지 정보를 조회했습니다.",
  "errorCode": null,
  "timestamp": "2026-01-19T12:00:00"
}
```

**에러**: 400 (잘못된 userId), 404 (사용자 없음), 500 (서버 오류)

**권장 클라이언트 흐름**: `GET /api/auth/me` → userId 획득 → 마이페이지 진입 시 `GET /api/users/{userId}/profile` 1회 호출.

---

## 4. DB 요약

| 저장 대상 | 스키마.테이블 | 비고 |
|-----------|----------------|------|
| 온보딩 완료 | user_schema.app_user | `is_onboard` |
| 관심 주제 | user_schema.user_interest_topics | 기존 |
| 알림 설정 | user_schema.user_notification_settings | 기존 |
| 온보딩 설문 | user_schema.onboarding_survey | **신규** (V7 마이그레이션) |

---

## 5. 엔드포인트 요약

| 메서드 | 경로 | 설명 |
|--------|------|------|
| PATCH | /api/users/{userId}/onboard | 온보딩 완료 (Body 선택: 통합 저장) |
| PUT | /api/users/{userId}/interests | 관심 주제 저장 (기존) |
| PUT | /api/users/{userId}/notifications | 알림 설정 변경 (기존) |
| GET | /api/auth/me | 현재 사용자 기본 정보 (기존) |
| GET | /api/users/{userId}/profile | 마이페이지 통합 조회 (신규) |
