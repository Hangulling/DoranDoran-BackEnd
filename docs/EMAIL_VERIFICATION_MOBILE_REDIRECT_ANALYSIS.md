# 이메일 인증 "모바일 접속 시 웹 리디렉션" 문제 분석 보고서

> **적용 완료**: 이메일 인증을 **앱 전용**으로 변경함. 웹 리디렉션 제거, 검증 후 항상 앱 딥링크(`dorandoran://email-verified?email=...&verified=...`)로 리디렉트. 설정: `email.verification.app-redirect-base`.

## 1. 요약

- **증상**: 모바일에서 이메일 인증 링크를 눌렀을 때도 **웹 signup 페이지**(`frontendUrl + "/signup?..."`)로 리디렉션됨.
- **원인**: (1) 클라이언트가 `platform`을 보내지 않아 인증 링크에 `format=json`이 붙지 않음. (2) 서버는 `format`/`Accept`이 없으면 무조건 웹용 HTML 리디렉트만 반환함.
- **적용한 개선**: 웹 리디렉션 제거, **항상 앱 딥링크로만 리디렉트**하도록 변경 (아래 5절 참고).

---

## 2. 현재 동작 정리

### 2.1 인증 요청 (`POST /api/auth/email/request-verification`)

| 항목 | 내용 |
|------|------|
| **위치** | `auth/src/main/java/.../AuthController.java` |
| **수신 파라미터** | `email`, `firstName`, `lastName`, **`platform`** (선택, 기본값 `"web"`) |
| **동작** | `platform`이 `"mobile"`일 때만 인증 링크에 **`&format=json`** 추가. 그 외에는 링크에 format 없음 → 서버 기본값 `html` 적용. |

```java
// 모바일 앱인 경우 format=json 파라미터 추가
if ("mobile".equalsIgnoreCase(platform)) {
    verifyLink += "&format=json";
}
```

### 2.2 인증 링크 검증 (`GET /api/auth/email/verify`)

| 항목 | 내용 |
|------|------|
| **위치** | 동일 `AuthController.java` |
| **모바일 판별** | `format=json` (쿼리) **또는** `Accept` 헤더에 `application/json` 포함 시 "모바일 요청"으로 간주. |
| **모바일 요청** | JSON 응답 반환 (리디렉트 없음). |
| **그 외** | HTML 페이지로 **항상** `frontendUrl + "/signup?email=...&verified=..."` 로 리디렉트. |

```java
boolean isMobileRequest = "json".equalsIgnoreCase(format) ||
        (acceptHeader != null && acceptHeader.contains("application/json"));
// ...
} else {
    // 웹 브라우저용 HTML 응답 → 무조건 웹 signup으로 리디렉트
    String redirectUrl = frontendUrl + "/signup?email=" + ...;
    writeRedirectHtml(response, redirectUrl, "인증 완료");
}
```

- 이메일 클라이언트에서 링크를 열면 **일반 모바일 브라우저**(Safari/Chrome 등)로 열림.
- 해당 브라우저는 `Accept: text/html,...` 를 보내고, 링크에 `format=json`이 없으면 서버는 **항상 웹용 HTML 리디렉트**만 하므로, 모바일에서도 웹 signup으로 넘어감.

---

## 3. 클라이언트 쪽 현황

### 3.1 웹 프론트엔드 (`src/`)

| 항목 | 내용 |
|------|------|
| **타입** | `VerificationRequest`에 `platform` 필드 **없음** (`src/types/auth.ts`: `email`, `firstName?`, `lastName?` 만 존재). |
| **호출** | `SignupPage.tsx`에서 `requestEmailVerification({ email, firstName, lastName })` 만 전달 → **`platform` 미전송**. |
| **플랫폼 감지** | `Capacitor` / `isNativePlatform` 등 모바일 앱 여부 감지 코드 **없음**. |

그래서 웹에서 요청한 인증 링크는 모두 `format=json` 없이 생성되고, 모바일에서 열어도 서버는 웹 리디렉트만 반환함.

### 3.2 모바일 앱

- 동일 레포/워크트리 내에서 이메일 인증을 호출하는 **모바일 전용 클라이언트**(Capacitor/네이티브) 코드는 확인하지 않았음.
- 만약 모바일 앱이 있다면, 현재 구조상 **`platform: "mobile"`을 보내지 않으면** 링크에 `format=json`이 붙지 않아, 모바일에서 열 때도 웹으로 리디렉션되는 동일 현상이 발생할 수 있음.

---

## 4. 원인 정리

1. **인증 링크에 `format=json`이 붙지 않음**  
   - 서버는 `platform=mobile`일 때만 `format=json`을 붙임.  
   - 웹/앱 모두 **`platform`을 보내지 않아** 링크는 항상 웹용(기본 `html`)으로 생성됨.

2. **모바일에서 링크를 여는 주체가 브라우저**  
   - 이메일 앱에서 링크 클릭 → 시스템/인앱 브라우저로 열림 → `Accept: text/html` 등으로 요청.  
   - `format` 쿼리도 없고 `Accept`도 JSON이 아니므로 서버는 **무조건 웹용 HTML 리디렉트**만 함.

3. **디바이스/앱 구분 없음**  
   - 서버는 User-Agent나 디바이스 정보로 “모바일”을 구분하지 않음.  
   - “모바일” 판별은 오직 `format=json` 또는 `Accept: application/json` 뿐.

그래서 **“모바일로 접속했을 경우에도 웹으로 리디렉션된다”** 는 동작이 그대로 나타남.

---

## 5. 개선 방향 (가능한 옵션)

### 옵션 A: 클라이언트에서 `platform` 전달 (권장 기반)

- **웹**:  
  - 그대로 두거나, “모바일 브라우저” 감지 시에만 `platform: "mobile"` 전달하는 방식은 **권장하지 않음**.  
  - 같은 링크를 데스크톱에서 열 수도 있어, 링크 자체는 웹용(`format` 없음)으로 두는 편이 단순함.
- **모바일 앱**(Capacitor/네이티브):  
  - 이메일 인증 요청 시 **반드시 `platform: "mobile"`** 전달.  
  - 그러면 인증 링크에 `format=json`이 붙고, 앱에서 해당 링크를 **앱 내 WebView나 Custom Tab에서 `Accept: application/json`으로 열거나**, **App Link/Universal Link로 앱이 열어서** 같은 URL을 `format=json`으로 호출하면 JSON을 받을 수 있음.  
  - 이 경우 “모바일에서 링크 클릭 → 브라우저로 열림” 시나리오는 아래 B/C와 함께 보완하는 것이 좋음.

### 옵션 B: 서버에서 User-Agent로 모바일 보완

- **동작**: `GET /email/verify`에서 `format`도 없고 `Accept`도 JSON이 아닐 때, **User-Agent**로 모바일(또는 앱 WebView) 여부를 보조 판별.
- **예**:  
  - 모바일로 판단되면 → `format=json`과 동일하게 JSON 응답 **또는**  
  - 앱 딥링크/유니버설 링크로 리디렉트하는 전용 HTML 페이지 반환.
- **주의**: User-Agent는 위조 가능하고, “모바일 브라우저”와 “앱 내 WebView”를 구분하기 어려울 수 있어, **보조 수단**으로만 쓰는 것이 안전함.

### 옵션 C: 모바일 전용 랜딩/딥링크 페이지

- **동작**:  
  - 모바일(UA 또는 `format=mobile` 등)로 들어온 경우, **웹 signup으로 바로 리디렉트하지 않고**,  
  - “앱이 설치되어 있으면 앱으로 열기”, “웹에서 계속하기” 버튼이 있는 **작은 HTML 랜딩**을 보여주고,  
  - “앱으로 열기”는 `igu://email-verified?email=...&verified=true` 같은 **앱 딥링크**로 이동.
- **효과**:  
  - 모바일에서 링크를 눌렀을 때 무조건 웹 signup으로만 가지 않고, 앱이 있으면 앱으로 돌아갈 수 있음.  
- **필요 조건**: 앱에서 해당 URL 스킴 또는 Universal Link 처리 구현.

### 옵션 D: 항상 `format`을 링크에 포함 (비권장)

- 모든 인증 링크에 `format=json`을 붙이면, **데스크톱 브라우저**에서 링크를 열었을 때 JSON만 보이게 되어 UX가 나쁨.  
- 따라서 **전역 기본을 JSON으로 두는 방식은 비권장**.

---

## 6. 권장 조합

1. **모바일 앱이 있는 경우**  
   - 앱에서 이메일 인증 요청 시 **`platform: "mobile"`** 필수 전달.  
   - 인증 링크는 `format=json` 포함으로 생성되게 유지.  
   - 앱에서 인증 링크를 **App Link / Custom Scheme**로 받아서, 앱 내에서 `GET /email/verify?...&format=json` 호출 후 JSON으로 인증 완료 처리.

2. **“모바일 브라우저에서 링크를 눌렀을 때”까지 개선하려면**  
   - **옵션 B**: 서버에서 User-Agent로 모바일일 때 JSON 또는 앱 유도 응답.  
   - **옵션 C**: 모바일일 때만 “앱으로 열기 / 웹에서 계속” 랜딩을 주고, 웹 리디렉트는 “웹에서 계속” 선택 시에만 수행.

3. **타입/API 정리**  
   - `VerificationRequest`에 `platform?: 'web' | 'mobile'` 추가.  
   - 웹은 생략 또는 `platform: 'web'`, 모바일 앱은 `platform: 'mobile'` 전달하도록 문서화 및 적용.

---

## 7. 참고 코드 위치

| 구분 | 파일 |
|------|------|
| 인증 요청 | `auth/.../AuthController.java` — `requestEmailVerification` (platform/format=json 제거됨) |
| 인증 검증·앱 리디렉트 | `auth/.../AuthController.java` — `verifyEmail`, `buildAppRedirectUrl`, `writeRedirectHtml` |
| 설정 | `auth/.../application*.yml` — `email.verification.app-redirect-base` (기본: `dorandoran://email-verified`) |

---

## 8. 적용된 변경 (앱 전용)

- **웹 리디렉션 제거**: `frontendUrl`/웹 signup으로 보내던 동작 전부 제거.
- **항상 앱 딥링크로 리디렉트**: 검증 성공/실패 모두 `email.verification.app-redirect-base`(기본 `dorandoran://email-verified`)로 이동.
- **리디렉트 URL 형식**: `dorandoran://email-verified?email=...&verified=true|false&firstName=...&lastName=...&error=...(실패 시)`
- **앱 측**: `App.addListener('appUrlOpen', ...)` 등으로 `dorandoran://email-verified?...` 수신 후 쿼리 파라미터로 인증 결과 처리하면 됨.

---

**결론**: 이메일 인증을 **앱 전용**으로 바꿔, 웹 리디렉션 없이 검증 후 항상 앱 딥링크로만 보내도록 적용했다.
