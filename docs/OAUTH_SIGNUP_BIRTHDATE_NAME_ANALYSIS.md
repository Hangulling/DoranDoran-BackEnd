# OAuth 회원가입 birthdate·이름 처리 분석

## 1. 개요

OAuth 로그인 시 미가입 사용자에 대해 회원가입 폼으로 pre-fill 후 `confirmSignup: true`로 재호출하는 구조에서, **birthdate**와 **이름**이 어떻게 전달·저장되는지 분석한 문서입니다.

---

## 2. confirmSignup 요청 시 전달되는 데이터

`confirmSignup: true`로 호출할 때 **request body**에는 다음이 포함됩니다:

- `provider` (string)
- `idToken` (string)
- `confirmSignup` (boolean, true)
- `birthDate` (string, 선택) - yyyy-MM-dd 형식. **미전달 시 기본값(1900-01-01)으로 저장됨** (2026-02 적용)

firstName, lastName 등은 idToken에서 추출됩니다.

### 2.1 OAuthLoginRequest 스키마

```
POST /api/auth/oauth/login
Content-Type: application/json

{
  "provider": "google" | "apple" | "firebase",
  "idToken": "...",
  "confirmSignup": true,
  "birthDate": "1990-01-01"   // 선택. yyyy-MM-dd 형식
}
```

---

## 3. birthdate 저장 구조

이### 3.1 결론 (2026-02 업데이트)

| 항목 | 내용 |
|------|------|
| OAuthLoginRequest | `birthDate` 선택 필드 추가. yyyy-MM-dd 형식 |
| createOAuthUser API | `birthDate` 파라미터 추가 (선택). 전달 시 해당 값 저장 |
| User 엔티티 | `birthDate` 미전달 또는 형식 오류 시 기본값 `LocalDate.of(1900, 1, 1)` 사용 |
| OAuth 토큰 | Google/Apple/Firebase identity token 모두 **birthdate를 포함하지 않음** → 폼 입력 후 confirmSignup 시 전달 권장 |

**birthDate를 body에 포함해 confirmSignup 요청 시 해당 값이 DB에 저장됩니다.** 미전달 시 기존과 동일하게 1900-01-01로 저장됩니다.

### 3.2 관련 코드

**User 엔티티 (user module)**
```java
@Column(name = "birth_date", nullable = false)
@Builder.Default
private LocalDate birthDate = LocalDate.of(1900, 1, 1);
```

**UserService.createOAuthUser** - birthDate 설정 없음
```java
User user = User.builder()
    .id(UUID.randomUUID())
    .email(email)
    .firstName(firstName != null ? firstName : "")
    .lastName(lastName != null ? lastName : "")
    .name(name != null ? name : (firstName + " " + lastName).trim())
    .passwordHash(null)
    .picture(picture)
    .info("")
    // birthDate 지정 없음 → 엔티티 기본값 1900-01-01 사용
    .status(User.UserStatus.ACTIVE)
    // ...
    .build();
```

### 3.3 일반 회원가입과의 차이

일반 이메일 회원가입(`POST /api/users`)에서는 `CreateUserRequest`에 `birthDate`가 필수 필드이며, 파싱 후 정상 저장됩니다.

---

## 4. 이름(name) 표시 문제

### 4.1 결론: Apple Sign In은 토큰에 이름이 없음

| Provider | identity token에 이름 포함 | 비고 |
|----------|---------------------------|------|
| Google | O | `given_name`, `family_name`, `name` 등 포함 |
| Firebase | O | 프로필 정보에 따라 포함 |
| **Apple** | **X** | identity token에 이름 미포함 |

### 4.2 AppleOAuthService 동작

```java
// Apple은 identity token에 이름을 넣지 않음. 최초 로그인 시 클라이언트가 별도 전달 가능(추후 확장)
String firstName = "";
String lastName = "";
String name = "";

return new AppleUserInfo(email, firstName, lastName, name, sub);
```

Apple은 **최초 인증 시에만** authorization 응답(코드 교환 전)에서 `user` 객체로 `name.firstName`, `name.lastName`을 제공합니다.  
이후 identity token에는 포함하지 않으며, 현재 백엔드는 identity token만 사용하므로 `firstName`, `lastName`, `name`이 항상 빈 문자열로 저장됩니다.

### 4.3 마이페이지에서 이름이 비어 보이는 이유

- **Apple로 가입한 사용자**: DB에 `firstName`, `lastName`, `name`이 빈 문자열(`""`)로 저장됨
- **Google/Firebase로 가입한 사용자**: 토큰에서 추출한 이름이 정상 저장됨

---

## 5. 요약 표

| 질문 | 답변 |
|------|------|
| confirmSignup 시 birthdate를 별도로 전달하나요? | **아니요.** body에는 provider, idToken, confirmSignup만 전달됨 |
| birthdate가 저장되나요? | **별도 전달·저장되지 않음.** DB 기본값 1900-01-01로 저장됨 |
| 이름이 안 뜨는 이유 | **Apple**은 identity token에 이름 미포함 → firstName/lastName/name이 빈 문자열로 저장됨 |

---

## 6. 개선 방향 제안

### 6.1 birthdate

1. OAuth 회원가입 폼에서 사용자가 birthdate 입력 가능하도록 UI 추가
2. `confirmSignup` 요청 시 body에 `birthDate` 필드 추가
3. `OAuthLoginRequest` 및 `createOAuthUser` API에 `birthDate` 파라미터 추가
4. User 생성 시 해당 값을 사용하도록 수정

### 6.2 이름 (Apple)

1. **웹/앱 클라이언트**: Apple 최초 인증 시 `user.name.firstName`, `user.name.lastName`을 별도로 저장
2. **confirmSignup 요청 시**: body에 `firstName`, `lastName`(또는 `name`) 필드 추가
3. `OAuthLoginRequest` 및 `createOAuthUser` API에 선택적 `firstName`, `lastName` 파라미터 추가
4. Apple의 경우 토큰에서 이름이 비어 있으면 body로 전달된 값을 사용하도록 로직 수정

---

## 7. 관련 문서

- [OAUTH_NEED_SIGNUP_FRONTEND_GUIDE.md](./OAUTH_NEED_SIGNUP_FRONTEND_GUIDE.md) - OAuth 회원가입 필요(needSignup) 프론트엔드 구현 가이드
- [API_SPEC_APPLE_SIGNIN.md](./API_SPEC_APPLE_SIGNIN.md) - Apple Sign In API 명세

---

*작성일: 2026-02-22*
