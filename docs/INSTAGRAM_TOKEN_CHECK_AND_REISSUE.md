# Instagram Access Token — 확인 및 재발급 가이드

Meta 개발자 센터와 Graph API를 이용해 **현재 토큰 상태를 확인**하고, **새 토큰을 발급·갱신**하는 방법을 정리한 문서입니다.

---

## 1. 토큰 확인 (유효 여부·만료일)

### 1-1. Graph API Explorer로 확인 (가장 간단)

1. **Graph API Explorer** 접속  
   - [https://developers.facebook.com/tools/explorer/](https://developers.facebook.com/tools/explorer/)
2. 오른쪽 상단에서 **Meta 앱** 선택 (Instagram 토큰을 발급한 앱).
3. **User or Page** 드롭다운에서 **Get User Access Token** 등으로 **앱의 Access Token**을 하나 받아 둡니다.  
   - (이미 앱 토큰이 있으면 4단계에서 그걸 써도 됩니다.)
4. 상단 **API 버전**을 v18.0 이상으로 두고, **엔드포인트**를 아래처럼 입력합니다.

   ```
   debug_token?input_token=여기에_확인할_Instagram_토큰_붙여넣기
   ```

   - 예: `debug_token?input_token=IGAARluUwuEi...` (실제 토큰 전체를 넣습니다.)
5. **Submit** 실행 후 응답에서 다음을 봅니다.
   - **`data.is_valid`**  
     - `true` → 아직 유효  
     - `false` → 만료되었거나 무효(재발급 필요)
   - **`data.expires_at`**  
     - Unix 시간(초). 이 시각 이후면 토큰 만료.
   - **`data.error`**  
     - 있으면 오류 코드·메시지(예: 190, "Failed to decrypt" 등).

**주의:** 확인용 토큰을 URL/로그에 남기지 않도록, 테스트 후 브라우저 기록·복사본을 정리하는 것이 좋습니다.

---

### 1-2. Debug Token API로 확인 (curl / 스크립트)

**같은 Meta 앱**의 **App Access Token**이 필요합니다.

- **App Access Token** 만들기:  
  - [앱 대시보드](https://developers.facebook.com/apps) → 해당 앱 → **설정 > 기본**  
  - **앱 ID**와 **앱 시크릿** 확인 후  
  - `앱ID|앱시크릿` 형태로 붙인 문자열이 App Access Token입니다.  
  - 예: `1234567890123456|a1b2c3d4e5f6...`

**요청 예시 (curl):**

```bash
# 확인할 토큰을 INPUT_TOKEN, 앱 토큰을 APP_TOKEN 에 넣고 실행
curl -s -G "https://graph.facebook.com/v21.0/debug_token" \
  --data-urlencode "input_token=INPUT_TOKEN" \
  --data-urlencode "access_token=APP_TOKEN"
```

**성공 시 응답 예시:**

```json
{
  "data": {
    "app_id": "1234567890123456",
    "type": "USER",
    "application": "Your App Name",
    "is_valid": true,
    "expires_at": 1735689600,
    "user_id": "17841405309211844",
    "scopes": ["instagram_basic", "instagram_manage_insights"]
  }
}
```

- **`is_valid: false`** 이거나 **`expires_at`** 이 이미 지났으면 → 토큰 만료/무효. 재발급 또는 갱신 필요.
- **`error`** 필드가 있으면 → 코드·메시지 확인 (190, "Failed to decrypt" 등은 만료/손상/잘못된 토큰일 때 자주 나옵니다).

---

### 1-3. 실제 API 호출로 확인 (실무 검증)

토큰이 우리 서비스에서 어떻게 쓰이는지와 동일하게 확인하려면, **Instagram Graph API**를 직접 호출해 봅니다.

```bash
# 미디어 목록 조회 (홈 피드와 동일한 용도)
curl -s -G "https://graph.instagram.com/me/media" \
  --data-urlencode "fields=id,caption,media_type,media_url,permalink,timestamp" \
  --data-urlencode "access_token=여기에_Instagram_Access_Token"
```

- **200 + JSON `data` 배열** → 토큰 유효, 정상 동작.
- **400 + OAuthException code 190** ("Failed to decrypt" 등) → 토큰 만료/무효/손상 → 재발급 또는 갱신 필요.

---

## 2. 토큰 재발급·갱신 방법

### 2-1. 메타 개발자 센터에서 새 토큰 발급 (완전 재발급)

기존 토큰이 만료되었거나, 확인 결과 무효일 때 **새 Long-lived 토큰**을 받는 방법입니다.

1. **Meta for Developers** 접속  
   - [https://developers.facebook.com/apps](https://developers.facebook.com/apps)  
   - 해당 **앱** 선택.
2. **Instagram** 제품으로 이동  
   - 왼쪽 메뉴 **Instagram** → **API setup with Instagram business login** (또는 사용 중인 Instagram 설정 메뉴).
3. **토큰 생성**  
   - 연결된 **인스타그램 계정** 옆 **Generate token** (토큰 생성) 클릭.  
   - 인스타그램 로그인/권한 화면에서 **허용**.
4. 화면에 표시된 **Access Token** 을 복사해 안전하게 보관.  
   - 이 토큰은 **Long-lived** (약 60일 유효).
5. **서버/앱에 반영**  
   - 서버 env 파일(`dorandoran-user.env`)의 `INSTAGRAM_ACCESS_TOKEN` 값을 새 토큰으로 교체.  
   - 필요 시 `docker restart dorandoran-user` 등으로 서비스 재시작.

자세한 앱 생성·인스타 계정 연결 절차는 [INSTAGRAM_GRAPH_API_USER_ID_AND_TOKEN_GUIDE.md](./INSTAGRAM_GRAPH_API_USER_ID_AND_TOKEN_GUIDE.md)를 참고하세요.

---

### 2-2. 갱신 API로 연장 (만료 전 24시간 이후만 가능)

이미 **유효한 Long-lived 토큰**이 있고, **만료되기 전에** 연장하고 싶을 때 사용합니다.  
(만료된 토큰은 이 API로 갱신할 수 없고, 2-1처럼 대시보드에서 새로 발급해야 합니다.)

- **조건:**  
  - Long-lived Instagram User Access Token  
  - 발급 후 **최소 24시간** 지난 토큰  
  - **아직 만료되지 않은** 토큰

**요청:**

```http
GET https://graph.instagram.com/refresh_access_token
  ?grant_type=ig_refresh_token
  &access_token=현재_Long_lived_토큰
```

**curl 예시:**

```bash
curl -s -G "https://graph.instagram.com/refresh_access_token" \
  --data-urlencode "grant_type=ig_refresh_token" \
  --data-urlencode "access_token=현재_유효한_Long_lived_토큰"
```

**성공 시 응답 예시:**

```json
{
  "access_token": "새로_발급된_토큰_문자열",
  "token_type": "bearer",
  "expires_in": 5183944
}
```

- **`access_token`** → 새 Long-lived 토큰 (갱신 시점부터 약 60일 유효).  
  이 값을 `INSTAGRAM_ACCESS_TOKEN` 으로 교체하면 됩니다.
- **`expires_in`** → 초 단위 유효 기간.

갱신 후에는 서버 env를 업데이트하고, user 서비스를 재시작해 반영합니다.

---

## 3. 정리

| 목적           | 방법 |
|----------------|------|
| 토큰 유효 여부 확인 | Graph API Explorer에서 `debug_token?input_token=토큰` 호출 후 `data.is_valid`, `data.expires_at` 확인 |
| 토큰 완전 재발급   | Meta 개발자 센터 → 앱 → Instagram → **Generate token** → 새 토큰 복사 후 env 반영 |
| 토큰 연장(갱신)   | 만료 전 Long-lived 토큰으로 `refresh_access_token` 호출 → 응답의 `access_token`으로 env 갱신 |

- **190 "Failed to decrypt"** 등이 나오면 만료/무효/손상 가능성이 크므로, 위에서 **토큰 확인** 후 **재발급(2-1)** 또는 **갱신(2-2)** 절차를 진행하면 됩니다.

---

## 참고 링크

- [Debug Token (Graph API)](https://developers.facebook.com/docs/graph-api/reference/debug_token/)
- [Refresh Access Token (Instagram)](https://developers.facebook.com/docs/instagram-platform/reference/refresh_access_token/)
- [Access Token (Instagram)](https://developers.facebook.com/docs/instagram-platform/reference/access_token/)
- 프로젝트 내 발급 절차: [INSTAGRAM_GRAPH_API_USER_ID_AND_TOKEN_GUIDE.md](./INSTAGRAM_GRAPH_API_USER_ID_AND_TOKEN_GUIDE.md)
