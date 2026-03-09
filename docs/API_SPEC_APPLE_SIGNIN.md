# Apple 로그인 API 명세

## 1. 개요

- **서비스**: Auth Service
- **Gateway Base URL**: `http://localhost:8080` (또는 배포 도메인)
- **요청 Content-Type**: `application/json`
- **인증**: 불필요 (로그인 전이므로 `Authorization` 헤더 없이 호출)

Apple Sign In으로 앱에서 받은 **identity token**을 서버에 보내 로그인(또는 자동 회원가입)하고, JWT 액세스/리프레시 토큰을 발급받는 API입니다.  
기존 OAuth 로그인 엔드포인트(`POST /api/auth/oauth/login`)에 `provider: "apple"`로 호출합니다.

---

## 2. 엔드포인트

### 2.1 OAuth 로그인 (Apple)

`POST /api/auth/oauth/login`

Google / Firebase / Apple 공통 엔드포인트이며, **Apple 로그인 시** `provider`를 `"apple"`로 보냅니다.

#### 요청 헤더

| 헤더 | 필수 | 설명 |
|------|------|------|
| `Content-Type` | O | `application/json` |

- **Authorization**: 없음 (로그인 전 단계이므로 제외)

#### 요청 바디 (OAuthLoginRequest)

```json
{
  "provider": "apple",
  "idToken": "eyJraWQiOiJlWGF1...",
  "confirmSignup": false
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `provider` | string | O | `"apple"` (대소문자 무관, 서버에서 `APPLE`로 정규화) |
| `idToken` | string | O | Apple Sign In으로 앱에서 받은 **identity token** (JWT 문자열) |
| `confirmSignup` | boolean | X | **신규 사용자(404)일 때** 회원가입 확정 여부. 생략/`false`: `needSignup`+`oauthUserInfo` 반환(폼 pre-fill). `true`: 즉시 회원가입 후 토큰 반환 |

#### 서버 처리 요약

1. **토큰 검증**: Apple JWKS(`https://appleid.apple.com/auth/keys`)로 identity token 서명·iss·aud·exp 검증
2. **aud 검증**: token의 `aud`가 서버 설정값 `APPLE_OAUTH_CLIENT_ID`(예: `com.koach.app`)와 일치해야 함
3. **사용자 처리**: `sub`로 기존 사용자 조회 → 없으면 이메일로 조회 → **없을 때** `confirmSignup` 여부에 따라:
   - `confirmSignup` false/생략: `needSignup: true` + `oauthUserInfo` 반환 (회원가입 폼 pre-fill용)
   - `confirmSignup` true: 자동 회원가입 후 토큰 반환
4. **탈퇴 사용자**: `INACTIVE` 사용자가 Apple 로그인 시 재활성화 후 진행
5. **JWT 발급**: 액세스 토큰·리프레시 토큰 생성 후 응답

#### 성공 응답

`200 OK`

```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "id": "uuid",
      "email": "user@privaterelay.appleid.com",
      "firstName": "",
      "lastName": "",
      "name": "",
      "picture": null,
      "info": null,
      "birthDate": null,
      "signupQuestion": null,
      "signupAnswer": null,
      "preferences": null,
      "lastConnTime": null,
      "status": "ACTIVE",
      "role": "ROLE_USER",
      "coachCheck": false,
      "exitModalDoNotShowAgain": false,
      "isOnboard": false,
      "createdAt": "2026-02-04T12:00:00",
      "updatedAt": "2026-02-04T12:00:00"
    }
  },
  "message": "OAuth 로그인에 성공했습니다.",
  "errorCode": null,
  "timestamp": "2026-02-04T12:00:00"
}
```

- **user.email**: Apple이 제공한 이메일 또는 이메일 숨기기 시 `{sub}@apple.privaterelay` 형태의 플레이스홀더
- **user.picture**: Apple은 프로필 사진을 제공하지 않아 `null`
- **user.firstName / lastName / name**: identity token에는 이름이 없어 빈 문자열 또는 서버 기본값

#### 신규 사용자 - 회원가입 필요 응답 (needSignup)

`confirmSignup` 없이/`false`로 호출 시, 사용자가 없으면 `needSignup: true`와 `oauthUserInfo`를 반환합니다.  
클라이언트는 이 값을 이용해 회원가입 폼을 pre-fill하고, 사용자가 동의 후 **같은 idToken + `confirmSignup: true`**로 다시 호출해 회원가입을 완료합니다.

`200 OK`

```json
{
  "success": true,
  "data": {
    "needSignup": true,
    "oauthUserInfo": {
      "email": "user@privaterelay.appleid.com",
      "firstName": "",
      "lastName": "",
      "name": "",
      "picture": null,
      "provider": "APPLE"
    }
  },
  "message": "회원가입이 필요합니다.",
  "errorCode": null,
  "timestamp": "2026-02-04T12:00:00"
}
```

- `accessToken`, `refreshToken`, `user`는 null
- `oauthUserInfo`로 폼 pre-fill 후, 동의 시 `confirmSignup: true`로 다시 `POST /api/auth/oauth/login` 호출

#### 실패 응답

**400 Bad Request**

| 상황 | errorCode | message 예시 |
|------|-----------|--------------|
| identity token 검증 실패 (서명/만료/iss/aud 등) | `A002` (AUTH_TOKEN_INVALID) | "Apple identity token 검증에 실패했습니다." |
| 지원하지 않는 provider | `E002` (INVALID_REQUEST) | "지원하지 않는 OAuth 제공자입니다. (google, firebase, apple 지원)" |
| idToken 누락/형식 오류 | (validation) | "ID Token은 필수입니다." 등 |

**500 Internal Server Error**

- 토큰 검증 중 예외, User 서비스 연동 실패 등  
- `errorCode`: `INTERNAL_SERVER_ERROR`  
- `message`: "OAuth 로그인 중 오류가 발생했습니다."

#### 에러 응답 본문 예시 (400)

```json
{
  "success": false,
  "data": null,
  "message": "Apple identity token 검증에 실패했습니다.",
  "errorCode": "A002",
  "timestamp": "2026-02-04T12:00:00"
}
```

---

## 3. 클라이언트 동작 요약

1. 앱에서 **Sign in with Apple** 완료 후 `ASAuthorizationAppleIDCredential.identityToken` 획득
2. `identityToken`(Data)을 UTF-8 문자열로 변환
3. `POST /api/auth/oauth/login` 호출  
   - Body: `{ "provider": "apple", "idToken": "<위에서 변환한 문자열>" }`
4. 성공 시 `data.accessToken`, `data.refreshToken` 저장 후 이후 API 호출에 `Authorization: Bearer {accessToken}` 사용
5. `data.user.isOnboard` 등으로 온보딩 여부에 따라 화면 분기

---

## 4. 서버 측 필요 설정

- **Auth 서비스** 환경변수: `APPLE_OAUTH_CLIENT_ID` = iOS 앱 Bundle ID (예: `com.koach.app`)
- identity token의 `aud` 클레임이 이 값과 일치해야 검증 통과

자세한 설정·앱 연동 예시는 [APPLE_SIGNIN_APP_GUIDE.md](./APPLE_SIGNIN_APP_GUIDE.md) 참고.

---

## 5. 참고

- **동일 엔드포인트**: Google(`provider: "google"`), Firebase(`provider: "firebase"`)와 같은 `POST /api/auth/oauth/login` 사용
- **Gateway**: 해당 경로는 인증 제외(로그인 전이므로 JWT/HMAC 불필요)
