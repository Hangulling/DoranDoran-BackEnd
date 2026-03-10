# 온보딩·마이페이지 API 비교: root vs igu worktree

`D:\new_dev\dorandoran-project`(root)와 `igu` worktree에서 구현된 동일 API를 비교한 결과입니다.

---

## 1. 동일한 부분

| 항목 | 비고 |
|------|------|
| **OnboardingSurvey** 엔티티 | 코드 동일 |
| **OnboardingSurveyRepository** | `findByUserId` 동일 |
| **OnboardingSubmitRequest** | 필드·검증 동일 |
| **OnboardingSurveyResponse** | 필드 동일 |
| **DB 마이그레이션 SQL** | 테이블·컬럼·제약 동일 (root: V5, igu: V7) |
| **UserController** | `GET /{userId}/profile`, `PATCH /{userId}/onboard`(optional body), 예외 처리 흐름 동일 |
| **completeOnboarding** 로직 | is_onboard 갱신 → 관심주제 → 알림설정 → 설문 저장 순서·조건 동일 |
| **getMypageProfile** 로직 | user + interests + notificationSetting + onboardingSurvey 조립 방식 동일 |

---

## 2. 다른 부분 (igu 쪽이 기존 모듈 구조에 맞춤)

### 2.1 DTO·서비스 타입 이름

| 구분 | root | igu |
|------|------|-----|
| 관심 주제 응답 타입 | `InterestTopicResponse` | `UserInterestsResponse` |
| 알림 설정 응답 타입 | `NotificationSettingResponse` | `NotificationSettingsResponse` |
| 관심 주제 저장 | `interestService.saveUserInterests(userId, InterestTopicRequest)` | `interestService.updateUserInterests(userId, List<String>)` |
| 알림 설정 조회 | `notificationSettingService.getUserNotificationSetting(userId)` | `notificationSettingService.getOrCreate(userId)` → DTO 수동 생성 |
| 알림 설정 저장 | `notificationSettingService.updateUserNotificationSetting(userId, NotificationSettingRequest)` | `notificationSettingService.updateSetting(userId, boolean)` |

→ 동작은 동일하고, igu는 이미 있던 Interest/Notification 서비스·DTO 이름과 시그니처에 맞춰 연결한 차이입니다.

### 2.2 GET /api/users/{userId}/profile 응답 JSON

| 필드 | root | igu |
|------|------|-----|
| 관심 주제 항목 키 | `interests.topics[].key` | `interests.topics[].key` (InterestTopicDto에 @JsonProperty("key") 적용) |
| 관심 주제 항목 라벨 | `interests.topics[].label` | `interests.topics[].label` |
| 알림 설정 | `notificationSetting.pushEnabled` (Boolean) | `notificationSetting.pushEnabled` (boolean) |

- **수정 완료**: igu의 `InterestTopicDto`에 `@JsonProperty("key")`를 적용하여 JSON 필드명을 `key`로 내려주도록 함. 명세·root와 동일.

### 2.3 마이그레이션 버전

- root: `V5__add_onboarding_survey_table.sql`
- igu: `V7__add_onboarding_survey_table.sql` (이미 V5, V6가 있어서 V7로 추가)

SQL 내용은 동일합니다.

---

## 3. 권장 사항

1. **클라이언트/명세와의 통일**  
   - igu에서도 명세대로 `key`를 쓰고 싶다면, `MypageProfileResponse`에 넣기 전에 `InterestTopicDto`를 `key`/`label` 구조로 매핑하는 DTO를 두거나, `UserInterestsResponse`/`InterestTopicDto`에 `key`라는 이름의 getter(또는 `topicKey`를 `key`로 직렬화하는 `@JsonProperty("key")`)를 두어 **응답 JSON에 `key`가 나오도록** 맞추는 것이 좋습니다.
2. **동작·엔드포인트**  
   - 온보딩 통합 제출·마이페이지 통합 조회의 **동작과 엔드포인트**는 두 트리 모두 동일하게 맞춰져 있습니다.

---

## 4. 요약

- **동일**: 엔티티, 리포지토리, 온보딩 요청/설문 응답 DTO, SQL, 컨트롤러·서비스 흐름.
- **차이**: igu는 기존 DTO/서비스 이름·시그니처 사용; **GET profile 응답에서 관심 주제 키가 `topicKey`로 나감** → 명세/root와 맞추려면 `key`로 노출하는 쪽으로 igu만 수정하면 됨.
