# 앱 전용 Apple 로그인 가이드

앱(iOS/Android)에서만 Apple 로그인을 사용할 때, **필요한 환경변수 발급 방법**과 **앱에서 identityToken 받아 API 호출하는 예시**를 정리했습니다.

---

## 1. 필요한 환경변수

백엔드(Auth 서비스)에서 Apple identity token의 `aud`(대상)를 검증할 때 사용합니다.

| 환경변수 | 설명 | 예시 |
|----------|------|------|
| `APPLE_OAUTH_CLIENT_ID` | iOS 앱의 **Bundle ID** (1개만 쓸 때) | `com.koach.app` |
| `APPLE_OAUTH_CLIENT_IDS` | 여러 앱을 쓸 때 Bundle ID를 쉼표로 구분 | `com.koach.app,com.koach.app.widget` |

**앱 전용이면** iOS Bundle ID 하나만 있으면 되므로 `APPLE_OAUTH_CLIENT_ID`만 설정하면 됩니다.

---

## 2. 환경변수 발급 방법 (Apple Developer)

### 2.1 전제 조건

- [Apple Developer Program](https://developer.apple.com/programs/) 가입 (유료)
- 앱의 **Bundle ID**가 이미 정해져 있어야 함 (예: `com.dorandoran.app`)

### 2.2 Sign in with Apple 설정

1. **Apple Developer 콘솔** 접속  
   - [Certificates, Identifiers & Profiles](https://developer.apple.com/account/resources/identifiers/list) → **Identifiers**

2. **App ID 선택 또는 생성**
   - 기존 앱: 해당 **App ID** 클릭
   - 새 앱: **+** → **App IDs** → **App** 선택 후  
     - Description: 앱 이름  
     - Bundle ID: **Explicit** → `com.yourapp.bundleid` 입력  
     - Capabilities에서 **Sign in with Apple** 체크  
     - **Continue** → **Register**

3. **Sign in with Apple이 켜진 App ID 확인**
   - 해당 App ID 상세 화면에서 **Sign in with Apple**이 **Enabled**인지 확인  
   - **Bundle ID** 값이 곧 백엔드에 넣을 `APPLE_OAUTH_CLIENT_ID` 값입니다.

4. **Xcode에서 동일하게 설정**
   - 타겟 → **Signing & Capabilities** → **+ Capability** → **Sign in with Apple** 추가  
   - Bundle ID가 Apple Developer에 등록한 것과 동일해야 합니다.

### 2.3 정리: 어떤 값을 쓰나요?

- **발급받는 것**: 별도 “키”나 “시크릿”은 없습니다.  
- **사용하는 값**: 앱의 **Bundle ID** 하나 (예: `com.dorandoran.app`).  
- **환경변수**: 이 Bundle ID를 그대로 `APPLE_OAUTH_CLIENT_ID`에 넣으면 됩니다.

```bash
# Koach 앱 Bundle ID
APPLE_OAUTH_CLIENT_ID=com.koach.app
```

---

## 3. 백엔드 설정 예시

### 3.1 로컬 / 서버

- `auth` 서비스가 읽을 수 있게 환경변수 설정.

```bash
export APPLE_OAUTH_CLIENT_ID=com.koach.app
```

### 3.2 Docker / docker-compose

```yaml
# docker-compose.yml 또는 실행 시
environment:
  APPLE_OAUTH_CLIENT_ID: "com.koach.app"
```

### 3.3 application.yml (선택)

로컬 개발용으로만 쓰고, 프로덕션은 환경변수 권장.

```yaml
apple:
  oauth:
    client-id: ${APPLE_OAUTH_CLIENT_ID:}
```

---

## 4. API 호출 규격 (참고)

- **URL**: `POST /api/auth/oauth/login`
- **Body (JSON)**:
  - `provider`: `"apple"`
  - `idToken`: Apple에서 받은 **identity token** 문자열

응답은 기존 로그인과 동일: `accessToken`, `refreshToken`, `user` 등.

---

## 5. 앱 예시 코드: iOS (Swift / SwiftUI)

앱에서 **Apple 로그인 버튼** → **identityToken** 획득 → **백엔드 API 호출**까지의 흐름입니다.

### 5.1 Info.plist

- 별도 설정 없이 사용 가능.  
- (이미 다른 OAuth를 위해 URL scheme이 있다면 그대로 두면 됨.)

### 5.2 Sign in with Apple 버튼 + API 호출 (SwiftUI)

```swift
import AuthenticationServices
import SwiftUI

struct AppleLoginView: View {
    @Environment(\.dismiss) private var dismiss
    
    var body: some View {
        SignInWithAppleButton(.signIn) { request in
            request.requestedScopes = [.fullName, .email]
        } onCompletion: { result in
            switch result {
            case .success(let authorization):
                handleAppleSignIn(authorization: authorization)
            case .failure(let error):
                print("Apple Sign In 실패: \(error.localizedDescription)")
                // 사용자에게 실패 메시지 표시
            }
        }
        .signInWithAppleButtonStyle(.black)
        .frame(height: 50)
    }
    
    private func handleAppleSignIn(authorization: ASAuthorization) {
        guard let appleIDCredential = authorization.credential as? ASAuthorizationAppleIDCredential else {
            return
        }
        
        guard let identityTokenData = appleIDCredential.identityToken,
              let identityToken = String(data: identityTokenData, encoding: .utf8) else {
            print("identityToken을 받지 못함")
            return
        }
        
        // 백엔드 API 호출
        callOAuthLogin(identityToken: identityToken)
    }
    
    private func callOAuthLogin(identityToken: String) {
        let url = URL(string: "https://your-api.com/api/auth/oauth/login")!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        
        let body: [String: Any] = [
            "provider": "apple",
            "idToken": identityToken
        ]
        request.httpBody = try? JSONSerialization.data(withJSONObject: body)
        
        URLSession.shared.dataTask(with: request) { data, response, error in
            if let error = error {
                print("API 오류: \(error)")
                return
            }
            guard let data = data,
                  let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
                  let success = json["success"] as? Bool, success,
                  let resData = json["data"] as? [String: Any],
                  let accessToken = resData["accessToken"] as? String,
                  let refreshToken = resData["refreshToken"] as? String else {
                print("로그인 응답 파싱 실패 또는 실패 응답")
                return
            }
            
            // 토큰 저장 후 화면 전환 (UserDefaults / Keychain 등)
            saveTokens(accessToken: accessToken, refreshToken: refreshToken)
            DispatchQueue.main.async {
                dismiss()
                // 홈 또는 온보딩으로 이동
            }
        }.resume()
    }
    
    private func saveTokens(accessToken: String, refreshToken: String) {
        UserDefaults.standard.set(accessToken, forKey: "accessToken")
        UserDefaults.standard.set(refreshToken, forKey: "refreshToken")
        // 보안이 중요하면 Keychain 사용 권장
    }
}
```

### 5.3 UIKit에서 버튼만 쓰는 경우

```swift
import AuthenticationServices

class LoginViewController: UIViewController {
    override func viewDidLoad() {
        super.viewDidLoad()
        let appleButton = ASAuthorizationAppleIDButton(type: .signIn, style: .black)
        appleButton.addTarget(self, action: #selector(handleAppleSignIn), for: .touchUpInside)
        // 레이아웃 설정 후
    }
    
    @objc private func handleAppleSignIn() {
        let request = ASAuthorizationAppleIDProvider().createRequest()
        request.requestedScopes = [.fullName, .email]
        
        let controller = ASAuthorizationController(authorizationRequests: [request])
        controller.delegate = self
        controller.presentationContextProvider = self
        controller.performRequests()
    }
}

extension LoginViewController: ASAuthorizationControllerDelegate, ASAuthorizationControllerPresentationContextProviding {
    func authorizationController(controller: ASAuthorizationController, didCompleteWithAuthorization authorization: ASAuthorization) {
        guard let credential = authorization.credential as? ASAuthorizationAppleIDCredential,
              let tokenData = credential.identityToken,
              let identityToken = String(data: tokenData, encoding: .utf8) else { return }
        
        callOAuthLogin(identityToken: identityToken)
    }
    
    func authorizationController(controller: ASAuthorizationController, didCompleteWithError error: Error) {
        print("Apple Sign In 오류: \(error.localizedDescription)")
    }
    
    func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        view.window!
    }
    
    private func callOAuthLogin(identityToken: String) {
        // 위 SwiftUI 예시와 동일한 URLSession 코드 사용
    }
}
```

### 5.4 최초 로그인 시 이름 전달 (선택)

Apple은 **최초 1회만** `ASAuthorizationAppleIDCredential.fullName`으로 이름을 줍니다.  
백엔드가 이름을 받는 API를 지원하면, 그때만 body에 넣어서 보낼 수 있습니다.

```swift
// 예: credential.fullName?.givenName, credential.fullName?.familyName
// 백엔드에 firstName, lastName 필드가 있다면 첫 로그인 시에만 전달
```

현재 백엔드 OAuth 로그인 API는 `provider` + `idToken`만 받으므로, 이름을 쓰려면 API 스펙 확장이 필요합니다.

---

## 6. Android에서 Apple 로그인 (참고)

Android 앱만 사용하는 경우에도 Apple 정책상 “다른 소셜 로그인”이 있으면 Apple 로그인을 함께 제공해야 할 수 있습니다.

- **방법 1**: [Firebase Authentication - Sign in with Apple](https://firebase.google.com/docs/auth/android/apple) 설정 후, 앱에서는 Firebase ID 토큰을 받아 기존처럼 `provider: "firebase"`, `idToken: firebaseIdToken`으로 호출.
- **방법 2**: Apple의 Android용 Sign in with Apple REST API/라이브러리를 사용해 identity token을 직접 받고, `provider: "apple"`, `idToken: identityToken`으로 동일 API 호출.

앱 전용이 **iOS만**이라면 위 iOS 예시만 적용하면 됩니다.

---

## 7. 체크리스트

- [ ] Apple Developer에서 App ID에 **Sign in with Apple** 활성화
- [ ] Xcode에서 해당 타겟에 **Sign in with Apple** capability 추가
- [ ] 백엔드에 `APPLE_OAUTH_CLIENT_ID` = iOS **Bundle ID** 설정
- [ ] 앱에서 Apple 로그인 성공 시 `identityToken` → `POST /api/auth/oauth/login` (`provider: "apple"`, `idToken`)
- [ ] 응답의 `accessToken` / `refreshToken` 저장 후 이후 API 호출에 Bearer 토큰 사용

이 가이드만 따라가면 앱 전용 Apple 로그인과 환경변수 발급·API 호출까지 한 번에 맞출 수 있습니다.

---

## 8. AOS 웹 OAuth (Authorization Code) + 콜백

Android 앱에서 **웹뷰/브라우저**로 Apple 로그인 페이지를 열고, **authorization code**를 서버 콜백으로 받아 로그인을 완료하는 방식입니다. 서버가 code → id_token 교환을 수행한 뒤 기존 `oauthLogin` 로직을 재사용합니다.

### 8.1 흐름 요약

1. 앱: `GET /api/auth/oauth/authorize-url?provider=apple` 호출 → `authorizationUrl`, `state` 수신
2. 앱: 브라우저/웹뷰로 `authorizationUrl` 오픈
3. 사용자가 Apple 로그인 완료 → Apple이 `GET /api/auth/oauth/callback?code=...&state=...` 로 리다이렉트
4. 서버: state 검증(Redis) → code를 id_token으로 교환 → `oauthLogin(apple, id_token)` → 앱 딥링크로 리다이렉트 (쿼리에 `accessToken`, `refreshToken` 등 포함)
5. 앱: 딥링크에서 토큰 추출 후 저장

### 8.2 Apple Developer 설정 (웹 플로우)

- **Identifiers** → **Services IDs** → 새 Services ID 생성 (Bundle ID와 **다른** 식별자 사용)
- **Sign in with Apple** 활성화
- **Return URLs**에 백엔드 콜백 URL 등록 (예: `https://api.yourdomain.com/api/auth/oauth/callback`)
- **Keys**에서 Sign in with Apple용 키 생성 → **.p8** 파일 다운로드, **Key ID** 확인. **Team ID**, **Services ID**는 콘솔에서 확인

### 8.3 백엔드 환경변수 (Auth 서비스)

| 환경변수 | 설명 |
|----------|------|
| `APPLE_OAUTH_SERVICE_ID` | Services ID (웹용 client_id) |
| `APPLE_OAUTH_TEAM_ID` | Apple Developer Team ID |
| `APPLE_OAUTH_KEY_ID` | .p8 키의 Key ID |
| `APPLE_OAUTH_PRIVATE_KEY` | .p8 PEM 내용 (줄바꿈 포함 문자열로 전달 가능) |
| `APPLE_OAUTH_REDIRECT_URI` | 콜백 절대 URL (Return URLs에 등록한 값과 동일) |
| `OAUTH_APP_REDIRECT_BASE` | 로그인 성공 후 이동할 앱 스킴 (예: `dorandoran://oauth-callback`) |

기존 `APPLE_OAUTH_CLIENT_ID`(iOS Bundle ID)는 identity token 검증용으로 그대로 두면 됩니다.

### 8.4 API

- **Authorization URL 발급**  
  `GET /api/auth/oauth/authorize-url?provider=apple`  
  선택 쿼리: `redirect_uri_after` (로그인 후 리다이렉트할 URI, state에 저장)  
  응답 예: `{ "data": { "authorizationUrl": "https://appleid.apple.com/auth/authorize?...", "state": "uuid" } }`

- **콜백** (서버·브라우저용, 앱에서 직접 호출하지 않음)  
  Apple이 리다이렉트: `GET /api/auth/oauth/callback?code=...&state=...`  
  서버가 처리 후 앱 스킴으로 리다이렉트:  
  성공 시 `{OAUTH_APP_REDIRECT_BASE}?accessToken=...&refreshToken=...&tokenType=Bearer&expiresIn=3600`  
  실패 시 `{OAUTH_APP_REDIRECT_BASE}?error=...&errorCode=...`

### 8.5 앱 연동 요약

- 앱은 **authorize-url** 응답의 `authorizationUrl`을 웹뷰/브라우저로 연다.
- 앱 딥링크 스킴을 **OAUTH_APP_REDIRECT_BASE**와 동일하게 맞추고, 콜백 후 해당 스킴으로 돌아올 때 URL 쿼리에서 `accessToken`, `refreshToken`을 파싱해 저장한다.
