# 이메일 인증 딥링크 프론트엔드 처리 가이드

이메일 인증 완료 후 백엔드가 **앱 딥링크**(`dorandoran://email-verified?...`)로 리디렉트할 때, 프론트(앱)에서 이를 수신해 회원가입 폼으로 인증정보를 전달하는 로직을 상세히 안내합니다.

---

## 1. 개요

### 1.1 흐름

1. 사용자가 앱에서 **이메일 인증 요청** → 백엔드가 인증 링크가 담긴 메일 발송
2. 사용자가 **메일의 링크 클릭** → 브라우저/WebView에서 백엔드 검증 API 호출
3. 백엔드 **검증 성공/실패** → HTML로 **앱 딥링크**로 리디렉트 (`dorandoran://email-verified?...`)
4. OS가 **앱 실행** 후 딥링크 URL을 앱에 전달
5. 앱이 **`appUrlOpen`** 으로 URL 수신 → 파싱 → **회원가입 스토어 반영** → **`/signup` 이동**
6. **SignupPage**가 스토어의 `emailVerified` / `verifiedEmail` 등을 읽어 “이메일 인증 완료” UI 및 Sign up 버튼 활성화

### 1.2 프론트에서 필요한 것

- **URL 스킴 등록**: `dorandoran` 스킴을 앱이 처리하도록 설정 (Capacitor/네이티브 설정)
- **딥링크 수신**: `App.addListener('appUrlOpen', ...)` 로 URL 수신
- **URL 파싱**: `email`, `verified`, `firstName`, `lastName`, `error` 추출
- **상태 반영**: 회원가입용 스토어에 반영 후 `/signup`으로 이동
- **회원가입 폼**: 스토어를 구독해 인증 완료 상태 표시

---

## 2. 백엔드가 주는 URL 형식

이메일 인증 검증 API는 성공/실패 모두 **같은 스킴·경로**, 쿼리만 다르게 내려줍니다.

| 경우 | 예시 URL |
|------|----------|
| 성공 | `dorandoran://email-verified?email=user%40example.com&verified=true&firstName=Jane&lastName=Doe` |
| 실패 | `dorandoran://email-verified?email=user%40example.com&verified=false&error=인증%20링크가%20유효하지%20않습니다.&firstName=...&lastName=...` |

**쿼리 파라미터**

| 파라미터 | 필수 | 설명 |
|----------|------|------|
| `email` | ✅ | 인증 요청한 이메일 (URL 인코딩됨) |
| `verified` | ✅ | `true` / `false` |
| `firstName` | 선택 | 이름 |
| `lastName` | 선택 | 성 |
| `error` | 실패 시 | 에러 메시지 (URL 인코딩됨) |

---

## 3. 프론트 구현 구성

구현은 다음 세 부분으로 나뉩니다.

| 역할 | 파일 | 설명 |
|------|------|------|
| URL 파싱 | `src/utils/emailVerifiedDeepLink.ts` | 딥링크 URL 문자열 → `EmailVerifiedPayload` |
| 딥링크 수신·스토어 반영·라우팅 | `src/hooks/useEmailVerifiedDeepLink.ts` | `appUrlOpen` 리스너, 스토어 `setMany`, `navigate('/signup')` |
| 훅 등록 | `src/App.tsx` | `useEmailVerifiedDeepLink()` 호출로 앱 전역에서 한 번만 등록 |

회원가입 폼(`SignupPage`, `useSignupFormStore`)은 이미 스토어를 구독하므로, 스토어만 갱신하면 자동으로 “인증 완료” 상태가 반영됩니다.

---

## 4. URL 스킴 등록 (Capacitor)

앱이 `dorandoran://` URL을 받으려면 네이티브 쪽에서 스킴을 등록해야 합니다.

### 4.1 iOS (Info.plist)

Capacitor 프로젝트의 `ios/App/App/Info.plist` 또는 Xcode에서:

- **URL Types** 에 스킴 추가
- **Identifier**: 예) `com.dorandoran.app`
- **URL Schemes**: `dorandoran`
- **Role**: `Editor` (또는 `Viewer`)

또는 `Info.plist` XML:

```xml
<key>CFBundleURLTypes</key>
<array>
  <dict>
    <key>CFBundleURLSchemes</key>
    <array>
      <string>dorandoran</string>
    </array>
    <key>CFBundleURLName</key>
    <string>com.dorandoran.app</string>
    <key>CFBundleURLRole</key>
    <string>Editor</string>
  </dict>
</array>
```

### 4.2 Android (AndroidManifest.xml)

`android/app/src/main/AndroidManifest.xml` 의 `<activity>` 안에:

```xml
<intent-filter>
  <action android:name="android.intent.action.VIEW" />
  <category android:name="android.intent.category.DEFAULT" />
  <category android:name="android.intent.category.BROWSABLE" />
  <data android:scheme="dorandoran" android:host="email-verified" />
</intent-filter>
```

`android:host="email-verified"` 로 하면 `dorandoran://email-verified?...` 만 이 앱으로 들어옵니다. 모든 경로를 받으려면 `android:host` 를 빼거나 별도 `<data>` 로 추가합니다.

### 4.3 Capacitor 설정 확인

`capacitor.config.ts` (또는 `.json`) 에서 `server.url` 등만 필요에 맞게 두면 됩니다. 스킴 등록은 위 네이티브 설정에만 의존합니다.

---

## 5. 딥링크 수신 (appUrlOpen)

Capacitor 의 **App 플러그인** `appUrlOpen` 이벤트로 딥링크 URL을 받습니다.

- **동작**: 앱이 `dorandoran://...` 로 열릴 때(콜드 스타트 또는 이미 켜진 상태) 이벤트가 한 번 발생하고, `ev.url` 에 전체 URL 문자열이 들어옵니다.
- **등록 시점**: 앱 루트에서 한 번만 등록하면 됩니다. 이 프로젝트에서는 **`App.tsx`** 에서 `useEmailVerifiedDeepLink()` 를 호출해 등록합니다.

```ts
// App.tsx
import { useEmailVerifiedDeepLink } from './hooks/useEmailVerifiedDeepLink'

function App() {
  useEmailVerifiedDeepLink()
  // ...
}
```

훅 내부에서는 다음만 수행합니다.

1. `@capacitor/core` 로 **네이티브 플랫폼 여부** 확인 (`Capacitor.isNativePlatform()`).
2. 웹이면 리스너를 등록하지 않고 종료.
3. 네이티브면 `App.addListener('appUrlOpen', handler)` 로 리스너 등록.
4. `handler` 에서는 URL이 **이메일 인증용인지** 확인한 뒤, 해당할 때만 파싱·스토어·라우팅을 수행합니다 (아래 6·7절).

---

## 6. URL 파싱 (parseEmailVerifiedUrl)

들어온 URL이 **이메일 인증 완료**용인지 판별하고, 쿼리를 객체로 만드는 부분입니다.

**파일**: `src/utils/emailVerifiedDeepLink.ts`

### 6.1 판별 조건

- URL 문자열에 **`email-verified`** 가 포함되어 있는지로 이메일 인증 딥링크인지 판별합니다.
- `dorandoran://email-verified?...` 뿐 아니라 `dorandoran://something/email-verified?...` 같은 형태도 포함될 수 있으므로, `includes('email-verified')` 로 검사합니다.

### 6.2 반환 타입 (EmailVerifiedPayload)

```ts
interface EmailVerifiedPayload {
  email: string       // 디코딩된 이메일
  verified: boolean   // true: 성공, false: 실패
  firstName?: string
  lastName?: string
  error?: string      // 실패 시 에러 메시지 (디코딩됨)
}
```

- 파싱 실패 또는 `email` 없으면 **`null`** 반환.
- 쿼리 값은 **decodeURIComponent** 로 디코딩한 뒤 넘깁니다.

### 6.3 사용 예

```ts
const payload = parseEmailVerifiedUrl(ev.url)
if (!payload) return
// payload.email, payload.verified, payload.firstName, payload.lastName, payload.error 사용
```

---

## 7. 스토어 반영 및 라우팅

딥링크에서 파싱한 값을 **회원가입 폼용 전역 상태**에 넣고, **회원가입 화면**으로 보냅니다.

### 7.1 사용하는 스토어

**`useSignupFormStore`** (`src/stores/useSignupStore.ts`)

- `setMany(partial)` 로 한 번에 여러 필드 갱신.
- 이메일 인증 딥링크 처리 시 다음만 갱신하면 됩니다.

| 스토어 필드 | 딥링크에서 넣는 값 |
|-------------|--------------------|
| `email` | `payload.email` |
| `firstName` | `payload.firstName ?? ''` |
| `lastName` | `payload.lastName ?? ''` |
| `verifiedEmail` | `payload.verified ? payload.email : null` |
| `emailVerified` | `payload.verified` |

나머지(`password`, `passwordCheck`)는 건드리지 않습니다.

### 7.2 라우팅

- **`navigate('/signup', { replace: true })`** 한 번 호출해 회원가입 페이지로 이동합니다.
- 쿼리 스트링은 붙이지 않아도 됩니다. SignupPage는 **스토어**를 구독하므로, 위처럼 스토어만 갱신해 두면 “이메일 인증 완료”로 표시되고 Sign up 버튼 활성화 조건에 반영됩니다.

### 7.3 훅 내부 코드 요약

`useEmailVerifiedDeepLink` 안에서는 대략 다음 순서로 동작합니다.

1. `appUrlOpen` 의 `ev.url` 로 `parseEmailVerifiedUrl(ev.url)` 호출.
2. `payload === null` 이면 아무것도 하지 않음.
3. `useSignupFormStore.getState().setMany({ ... })` 로 위 표대로 반영.
4. `navigate('/signup', { replace: true })` 호출.
5. cleanup 시 `listener.remove()` 로 리스너 해제.

---

## 8. 회원가입 폼과의 연동

SignupPage와 useSignupFormStore는 **이미** 다음을 하고 있습니다.

- **스토어 구독**: `email`, `emailVerified`, `verifiedEmail`, `firstName`, `lastName` 등을 읽어 폼과 버튼 상태에 반영.
- **Sign up 버튼**: `emailVerified === true` 등 조건으로 활성화.
- **persist**: `partialize` 로 firstName, lastName, email, emailVerified, verifiedEmail 만 저장하므로, 딥링크로 넣은 값도 새로고침 후에도 유지됩니다.

따라서 **딥링크 쪽에서는 스토어만 갱신하고 `/signup`으로 보내면 되고**, SignupPage 쪽 코드를 바꿀 필요는 없습니다.

(실패 시 `error` 메시지를 토스트 등으로 보여주고 싶다면, 스토어에 `verificationError: string | null` 같은 필드를 추가하고 SignupPage에서 구독해 표시하는 식으로 확장할 수 있습니다.)

---

## 9. 코드 위치 요약

| 목적 | 파일 | 위치 |
|------|------|------|
| URL 파싱 | `src/utils/emailVerifiedDeepLink.ts` | `parseEmailVerifiedUrl()`, `EmailVerifiedPayload` |
| 딥링크 리스너·스토어·라우팅 | `src/hooks/useEmailVerifiedDeepLink.ts` | `useEmailVerifiedDeepLink()` |
| 리스너 등록 | `src/App.tsx` | `useEmailVerifiedDeepLink()` 호출 |
| 회원가입 상태 | `src/stores/useSignupStore.ts` | `useSignupFormStore` |
| 회원가입 화면 | `src/pages/SignupPage.tsx` | 스토어 구독, 버튼 활성화 |

---

## 10. 테스트 방법

### 10.1 실제 디바이스/에뮬레이터

1. 앱을 **디바이스 또는 에뮬레이터**에 설치하고, 위 **4. URL 스킴 등록** 이 적용되어 있는지 확인.
2. 터미널/브라우저에서 **딥링크 직접 호출**:
   - **iOS 시뮬레이터**: `xcrun simctl openurl booted "dorandoran://email-verified?email=test%40example.com&verified=true&firstName=Jane&lastName=Doe"`
   - **Android**: `adb shell am start -a android.intent.action.VIEW -d "dorandoran://email-verified?email=test%40example.com&verified=true&firstName=Jane&lastName=Doe"`
3. 앱이 뜨고 `/signup` 으로 이동했는지, 폼에 이메일·이름·인증 완료 상태가 채워졌는지 확인.

### 10.2 웹 빌드

- `Capacitor.isNativePlatform()` 이 `false` 이므로 `appUrlOpen` 리스너는 **등록되지 않습니다**. 웹에서는 이메일 인증 후 웹 자체의 리디렉트/쿼리 플로우를 쓰면 됩니다.

---

## 11. 문제 해결

- **딥링크로 앱이 안 열림**: 4절의 URL 스킴 등록(Info.plist, AndroidManifest) 확인.
- **앱은 열리는데 회원가입으로 안 감**: `appUrlOpen` 리스너가 등록되는지, `parseEmailVerifiedUrl(ev.url)` 이 `null` 이 아닌지 로그로 확인.
- **회원가입 화면에 인증 상태가 안 보임**: 스토어 `setMany` 에 `emailVerified`, `verifiedEmail`, `email` 등이 들어가는지 확인.

자세한 증상별 대응은 ** [EMAIL_VERIFICATION_APP_REDIRECT_TROUBLESHOOTING.md](./EMAIL_VERIFICATION_APP_REDIRECT_TROUBLESHOOTING.md)** 를 참고하면 됩니다.
