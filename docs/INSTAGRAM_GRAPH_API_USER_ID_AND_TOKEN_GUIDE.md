# Instagram Graph API — User ID·Access Token 발급 절차

캐러셀/비디오 확장 및 홈 피드 연동에 필요한 **Instagram User ID**와 **Access Token**을 발급받는 절차입니다.

---

## 전제 조건

- **인스타그램 계정**: **프로페셔널(크리에이터 또는 비즈니스)** 계정이어야 합니다. 개인 계정은 Graph API로 미디어를 읽을 수 없습니다.
- **Meta 개발자 계정**: [developers.facebook.com](https://developers.facebook.com) 에서 로그인할 수 있는 계정.

---

## 1. Meta 앱 생성 및 Instagram 연동 (최초 1회)

1. **Meta for Developers** 접속  
   - [https://developers.facebook.com/apps](https://developers.facebook.com/apps)  
   - 로그인 후 **앱 만들기** 또는 기존 앱 선택.

2. **앱 유형**  
   - 새 앱이면: 사용 사례에서 **기타** 선택 → **비즈니스** 앱으로 생성.  
   - (Graph API용 Instagram은 **비즈니스** 타입 앱에서만 사용 가능.)

3. **Instagram 제품 추가**  
   - 앱 대시보드 왼쪽에서 **Instagram** 찾기.  
   - **설정** 클릭.  
   - **"API setup with Instagram business login"** 이 추가됩니다.  
   - (Facebook 로그인 기반이 필요하면 **"API setup with Facebook login"** 선택 — 이 경우 Facebook 페이지와 인스타 비즈니스 계정 연결이 필요합니다.)

4. **인스타그램 계정 연결**  
   - 왼쪽 메뉴: **Instagram > API setup with Instagram business login**.  
   - **Add Instagram account** 또는 **Instagram 계정 추가**.  
   - 사용할 **인스타그램 프로 계정**으로 로그인하여 연결.  
   - (테스트용으로는 공개 계정이면 됩니다. 여러 계정 추가 가능.)  
   - **"개발자 역할 권한 부족"** 이 뜨면 아래 트러블슈팅을 먼저 진행한 뒤 다시 시도하세요.

---

## 트러블슈팅: "개발자 역할 권한 부족" (Insufficient Developer Role)

계정 연결 도중 **"개발자 역할 권한 부족: 개발자 역할 권한이 부족합니다"** 알림이 뜨는 경우, **연결하려는 인스타그램 계정**을 앱에서 **Instagram Tester**(또는 Instagram Evaluator)로 추가하고, **인스타그램 쪽에서 초대를 수락**해야 합니다.  
Meta 앱의 Administrator/Developer 권한만으로는 부족하고, **인스타그램 계정 단위 역할**이 필요합니다.

### 해결 절차 (Instagram Tester 추가)

1. **앱 대시보드** → 왼쪽 메뉴 **앱 역할(App roles)** → **역할(Roles)** 페이지로 이동.
2. **사람 추가(Add People)** 버튼 클릭.
3. 역할에서 **Instagram Tester**(인스타그램 테스터) 선택.
4. 검색창에 **연결하려는 인스타그램 계정의 사용자 이름(아이디)** 을 입력하고, 검색된 해당 계정 선택 후 추가.
   - 이메일이 아닌 **인스타그램 @사용자이름** 으로 검색합니다.
5. **인스타그램에서 초대 수락**  
   - 인스타그램 앱 또는 [https://www.instagram.com/accounts/manage_access](https://www.instagram.com/accounts/manage_access) 접속.  
   - **Tester Invites**(테스터 초대) 탭으로 이동.  
   - 해당 앱 초대에 **수락(Accept)** 클릭.
6. 수락 후 다시 앱 대시보드로 돌아가서 **Instagram > API setup with Instagram business login** → **Add Instagram account** / **Generate token** 을 시도.

### 대안: Instagram Evaluator 역할

일부 환경에서는 **Instagram Evaluator** 역할로 초대하는 방법도 안내됩니다.

- **앱 역할 > 역할** 에서 해당 인스타그램 계정을 **Instagram Evaluator** 로 추가.
- 인스타그램 계정으로 초대를 수락한 뒤, **Instagram Business Login API 설정** → **1. Generate Access Tokens** 에서 계정이 보이는지 확인 후 토큰 생성 시도.

여전히 **빈 화면(blank page)** 만 뜨거나 오류가 반복되면, 브라우저 캐시/시크릿 모드 재시도, 다른 브라우저 시도, 또는 [Meta 개발자 커뮤니티](https://developers.facebook.com/community/) 에서 동일 오류 사례를 검색해 보시는 것을 권장합니다.

---

## 2. Access Token 발급 (앱 대시보드에서)

1. 앱 대시보드에서 **Instagram > API setup with Instagram business login** 으로 이동.
2. 연결된 인스타그램 계정 옆 **Generate token** (토큰 생성) 클릭.
3. 인스타그램 로그인/권한 화면에서 **허용**.
4. 표시된 **Access Token** 을 복사해 안전한 곳에 보관.  
   - 대시보드에서 생성한 토큰은 **Long-lived** (약 60일 유효).

**주의**

- 토큰은 URL/로그에 노출하지 말 것.
- 운영 환경에서는 환경 변수(`INSTAGRAM_ACCESS_TOKEN`) 등으로만 주입.

---

## 3. Instagram User ID 확인

Access Token을 받은 뒤, 해당 토큰에 해당하는 **Instagram User ID**를 아래 API로 조회합니다.

**요청 예시 (curl):**

```bash
curl -G "https://graph.instagram.com/v24.0/me" \
  --data-urlencode "fields=user_id,username" \
  --data-urlencode "access_token=여기에_발급받은_Access_Token_입력"
```

**성공 시 응답 예시:**

```json
{
  "user_id": "17841405309211844",
  "username": "your_instagram_username"
}
```

- **user_id** 가 우리 서비스 설정에서 쓰는 **Instagram User ID** 입니다.  
  - igu 설정: `instagram.user-id` (환경변수 `INSTAGRAM_USER_ID`).
- `fields`에 `id`를 넣으면 앱 스코프 ID가 나올 수 있으며, 미디어 목록 조회 시에는 보통 **user_id** 값을 사용합니다 (또는 `me` 사용 가능).

---

## 4. igu/서버에 반영할 값

| 항목 | 환경 변수 (예) | 설명 |
|------|---------------------|------|
| Access Token | `INSTAGRAM_ACCESS_TOKEN` | 2단계에서 복사한 토큰 |
| User ID | `INSTAGRAM_USER_ID` | 3단계 응답의 `user_id`. 비워두면 API에서 `me` 사용 |

- **User ID**를 넣지 않으면 igu 코드에서는 `me`로 요청하므로, 토큰 소유 계정의 미디어를 가져옵니다.  
  명시적으로 넣어두면 같은 값이므로, 운영 시에는 `user_id`를 넣어 두는 것을 권장합니다.

---

## 5. Long-lived 토큰 갱신 (선택)

- 대시보드에서 생성한 토큰은 이미 60일 유효 Long-lived 토큰입니다.
- **Short-lived(1시간) 토큰**을 받은 경우(예: Business Login OAuth 후 코드 교환), Long-lived로 교환:

```http
GET https://graph.instagram.com/access_token
  ?grant_type=ig_exchange_token
  &client_secret=앱_시크릿
  &access_token=Short_lived_토큰
```

- **앱 시크릿**은 **앱 설정 > 기본** 등에서 확인. **서버에서만** 사용하고 클라이언트/공개 저장소에 두지 마세요.
- 60일 토큰은 만료 24시간 전부터 **refresh_access_token** 으로 갱신 가능합니다. (갱신 시 다시 60일.)

---

## 6. 권한 정리

- 미디어 읽기(피드/캐러셀/비디오)에는 **instagram_business_basic** 또는 **instagram_basic** (Facebook 로그인 시) 권한이 필요합니다.
- 앱 생성 시 Instagram 제품을 추가하면 기본 권한이 붙으며, **App Review** 를 통과해야 프로덕션에서 일반 사용자 계정 데이터에 접근할 수 있습니다.
- **개발/테스트** 단계에서는 앱에 연결한 인스타그램 계정(테스터)으로 토큰을 발급해 사용하면 됩니다.

---

## 참고 링크

- [Get started - Instagram API with Instagram Login](https://developers.facebook.com/docs/instagram-platform/instagram-api-with-instagram-login/get-started/)
- [Create a Meta App for Instagram](https://developers.facebook.com/docs/instagram-platform/create-an-instagram-app)
- [Access Token (exchange / long-lived)](https://developers.facebook.com/docs/instagram-platform/reference/access_token/)
- [IG User - /me](https://developers.facebook.com/docs/instagram-platform/reference/me/)
