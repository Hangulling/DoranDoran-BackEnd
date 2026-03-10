# 이메일 인증 후 '인증 완료' 하얀 화면만 뜨고 앱으로 안 돌아가는 문제

## 증상

- 이메일 인증 링크 클릭 → **api.doran-chat.com**에서 "인증 완료" / "리디렉션 중..." 하얀 화면만 보임
- 앱 회원가입 폼으로 자동 복귀되지 않음
- 수동으로 앱으로 돌아가도 **인증 완료 정보가 전달되지 않음**

---

## 원인 요약

1. **링크가 앱 내 WebView에서 열림**  
   인증 링크를 **앱 내 브라우저(WebView/InAppBrowser)**에서 열면, 서버가 내려주는 HTML의 `window.location.href = 'dorandoran://email-verified?...'` 리디렉션이 **대부분 동작하지 않습니다**.  
   - WebView는 보안상 커스텀 스킴 이동을 막거나, 해도 **같은 WebView 안**에서만 처리하려고 해서 **네이티브 앱으로 인계되지 않음**
   - 그래서 사용자는 "인증 완료" HTML 페이지만 보게 되고, 그대로 멈춰 있는 것처럼 보임

2. **앱에서 딥링크를 처리하지 않거나, 회원가입 폼으로 전달하지 않음**  
   - `dorandoran://email-verified?...` 스킴을 앱이 받더라도,  
     - URL 스킴 미등록, 또는  
     - `App.addListener('appUrlOpen', ...)` 등으로 **수신은 하는데**  
     - **회원가입 화면으로 이동 + `email`, `verified=true`, `firstName`, `lastName` 전달**을 하지 않으면  
   - 앱으로 돌아가도 "인증 완료" 상태가 회원가입 폼에 반영되지 않음 → **인증정보 안 전해짐**

---

## 대응 방향

### 1) 인증 링크를 **앱 밖 브라우저**에서 열기 (가장 단순)

- 이메일의 인증 링크를 탭했을 때 **시스템 브라우저(Safari / Chrome)**로 열리게 하면,  
  같은 HTML에서 `dorandoran://email-verified?...`로 리디렉트할 때 OS가 **앱을 띄울 수 있음**.
- **앱 쪽**:
  - 인증 메일 보내기 전에  
    - “이메일 앱에서 링크를 열면 **기본 브라우저**에서 열리게” 하거나  
    - 인증 링크를 **외부 브라우저**에서 여는 버튼/안내만 해도 됨.
  - 또는 이메일에서 링크를 눌렀을 때 **외부 브라우저**로만 열리도록 OS/이메일 앱 설정에 의존 (앱에서 강제하기는 어렵지만, 사용자 안내는 가능).

이렇게 하면:
- 서버 응답은 그대로 두고,
- **앱만** `dorandoran://email-verified` 수신 시 회원가입 폼으로 이동 + 쿼리 파라미터 전달을 구현하면 됨.

---

### 2) 앱에서 **딥링크 수신 후 회원가입 폼으로 인증정보 전달** (필수)

**백엔드**는 이미 다음 형태로 리디렉트하고 있음:

- 성공: `dorandoran://email-verified?email=...&verified=true&firstName=...&lastName=...`
- 실패: `dorandoran://email-verified?email=...&verified=false&error=...&firstName=...&lastName=...`

**앱에서 해야 할 일**:

1. **URL 스킴 등록**  
   - `dorandoran` (또는 사용 중인 스킴)이 **앱에서 처리**하도록 설정되어 있는지 확인.

2. **딥링크 수신**  
   - Capacitor 예: `App.addListener('appUrlOpen', ev => { ... ev.url ... })`  
   - 수신 URL이 `dorandoran://email-verified?...` 인지 확인.

3. **회원가입 화면으로 이동 + 인증정보 전달**  
   - URL 쿼리에서 `email`, `verified`, `firstName`, `lastName`, `error` 파싱.
   - 회원가입 폼(또는 해당하는 라우트)으로 이동하면서 **상태로 전달**:
     - `email` → 입력된 이메일과 동일한지 확인용
     - `verified === 'true'` → 이메일 인증 완료로 표시 (예: `emailVerified = true`, `verifiedEmail = email`)
     - `firstName`, `lastName` → 필요하면 폼 필드에 반영
     - `error` → 실패 시 메시지 표시

4. **회원가입 폼 쪽**  
   - 딥링크에서 넘어온 경우,  
     - 위에서 설정한 `emailVerified` / `verifiedEmail` 등을 읽어서  
     - “이메일 인증 완료” UI 표시 및 Sign up 버튼 활성화.

지금처럼 “앱으로 돌아가도 인증정보 안 전해짐”이면, 2~4번 중 한 곳이 비어 있을 가능성이 큼.

---

### 3) (선택) WebView에서도 되게 하려면 – 브릿지 또는 Universal Link

- **앱 내 WebView**에서 계속 열 거라면:
  - **방안 A**: 서버에서 내려주는 HTML에서 **앱에만 노출되는 JavaScript 인터페이스** 호출  
    (예: `window.AppBridge.onEmailVerified({ email, verified, firstName, lastName })`)  
    → WebView를 닫고, 앱이 그 인자로 회원가입 폼으로 이동 + 상태 반영.  
    이 경우 서버 HTML을 “앱용”과 “브라우저용” 두 종류로 나누거나, 같은 HTML에 브릿지 호출 + 기존 `dorandoran://` 링크를 같이 넣어도 됨.
  - **방안 B**: 인증 완료 URL을 **Universal Link(iOS) / App Link(Android)**로 등록  
    → 링크 자체를 `https://api.doran-chat.com/...` 또는 `https://www.doran-chat.com/email-verified?...` 같은 **https**로 두고, 그 URL을 앱이 소유한 도메인으로 연결해 두면, 링크 탭 시 **WebView가 아니라 앱이 바로 열리게** 할 수 있음.  
    그러면 “하얀 화면” 없이 앱만 뜨고, 앱이 URL 쿼리로 인증정보를 받을 수 있음. (이 경우 서버는 `redirect`를 https URL로 주도록 바꿔야 함.)

---

## 체크리스트

| 항목 | 확인 |
|------|------|
| 인증 링크를 **외부 브라우저**에서 열면 앱이 뜨는지 | |
| 앱에 `dorandoran` (또는 사용 스킴) URL 스킴 등록 여부 | |
| `appUrlOpen`(또는 동일 역할) 리스너에서 `email-verified` 경로 처리 여부 | |
| 수신한 `email`, `verified`, `firstName`, `lastName`을 **회원가입 화면 상태**에 넣는지 | |
| 회원가입 폼이 그 상태를 읽어 “이메일 인증 완료”로 표시하는지 | |

위가 모두 되어 있으면,  
- 링크를 **외부 브라우저**에서 열고,  
- “여기를 클릭하세요”까지 가면 앱이 뜨고,  
- 앱이 인증정보를 받아 회원가입 폼에 반영하는 흐름이 됨.

---

## 요약

- **하얀 화면만 보이는 이유**: 인증 링크가 **앱 내 WebView**에서 열려서, `dorandoran://...` 리디렉션이 앱으로 넘어가지 않기 때문.
- **인증정보가 안 전해지는 이유**: 앱이 `dorandoran://email-verified?...` 를 받았을 때 **회원가입 화면으로 이동 + 쿼리 파라미터를 상태로 전달**하는 처리가 없기 때문.
- **권장**: (1) 인증 링크는 **시스템 브라우저**에서 열리게 안내/설정. (2) 앱에서 **딥링크 수신 → 회원가입 폼으로 이동 + `email`/`verified`/`firstName`/`lastName` 전달**까지 구현.
