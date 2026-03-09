# Auth 서비스 환경변수 예시

서버 배포 시 `dorandoran-auth.env` 또는 docker `--env-file` 로 사용할 수 있습니다.

## 기존과 달라진/추가된 환경변수 (Apple 웹 OAuth)

| 환경변수 | 설명 | 적용 값 |
|----------|------|---------|
| `APPLE_OAUTH_SERVICE_ID` | **신규** — Apple Services ID (웹 플로우) | `com.koach.apple.login` |
| `APPLE_OAUTH_REDIRECT_URI` | **신규** — 콜백 URL (Apple Return URLs와 동일) | `https://api.doran-chat.com/api/auth/oauth/callback` |
| `OAUTH_APP_REDIRECT_BASE` | **신규** — 로그인 성공 후 앱 딥링크 스킴 | `dorandoran://oauth-callback` |
| `APPLE_OAUTH_TEAM_ID` | **신규** — client_secret JWT 서명용 (선택) | (Apple Developer에서 확인) |
| `APPLE_OAUTH_KEY_ID` | **신규** — .p8 Key ID (선택) | (Keys에서 확인) |
| `APPLE_OAUTH_PRIVATE_KEY` | **신규** — .p8 PEM 내용 (선택) | (키 파일 내용) |

## Apple Sign In (기존, iOS identityToken)

- `APPLE_OAUTH_CLIENT_ID` — iOS 앱 Bundle ID (예: com.koach.app)
- `APPLE_OAUTH_CLIENT_IDS` — (선택) 여러 Bundle ID 쉼표 구분

## JWT / Gateway HMAC (서버: dorandoran-auth.env)

| 환경변수 | 설명 |
|----------|------|
| `APPLICATION_SECURITY_JWT_SECRET_KEY` | JWT 서명용 시크릿 키 |
| `GATEWAY_JWT_HMAC_SECRET` | Gateway ↔ 서비스 간 HMAC 검증 시크릿 |

## Google OAuth (서버: dorandoran-auth.env)

| 환경변수 | 설명 |
|----------|------|
| `GOOGLE_OAUTH_CLIENT_ID` | Web Client ID |
| `GOOGLE_OAUTH_ANDROID_CLIENT_IDS` | Android Client IDs (쉼표 구분) |
| `GOOGLE_OAUTH_CLIENT_SECRET` | OAuth Client Secret |

## 기타

- `BACKEND_URL` — 이메일 인증 링크 등에 사용하는 백엔드 공개 URL
- 메일/DB/Redis 등은 docker-compose 또는 서버별로 설정

## gateway, user, chat 서비스 (각 dorandoran-*.env)

- `GATEWAY_JWT_HMAC_SECRET` — auth와 동일한 값 (HMAC 검증용)
