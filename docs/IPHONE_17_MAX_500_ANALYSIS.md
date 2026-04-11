# iPhone 17 Max 전용 500 에러 가능 원인 및 대응

> 특정 기기(iPhone 17 Max)에서만 500이 나고 다른 iPhone 기종은 정상인 경우를 위한 원인 후보와 조치입니다.

---

## 1. 가능 원인 요약

| 원인 후보 | 설명 | 대응 |
|-----------|------|------|
| **JWT sub 파싱/전달 오류** | Gateway가 JWT payload에서 `sub`를 단순 문자열 검색으로 추출. 새 기기/새 OS에서 JWT 구조나 클레임 순서가 달라지면 잘못된 값이 `X-User-Id`로 전달되고, 하위 서비스에서 `UUID.fromString(...)` 예외로 500 발생 가능. | ✅ Gateway에서 `sub`를 UUID 형식으로 검증 후에만 `X-User-Id` 주입하도록 수정함. |
| **Origin 미허용** | iOS 19 / 새 WebView가 다른 Origin(예: `http://localhost`, 새 스킴)을 보낼 수 있음. CORS는 보통 403/차단이지 500을 주지는 않으나, 일부 미들웨어/프록시 조합에서는 500이 나올 수 있음. | 허용 Origin 목록 확인 및 필요 시 추가. |
| **User-Agent/헤더 길이** | iPhone 17 Pro Max UA 예: `Mozilla/5.0 (iPhone 17 Pro Max; CPU iPhone OS 19_3 like Mac OS X) ...` — 일부 서버/로그 버퍼가 긴 헤더에서 오버플로우할 가능성(드묾). | nginx/서버 버퍼 설정 및 로그 확인. |
| **앱 첫 호출 API** | 앱 실행 직후 호출하는 API가 해당 기기에서만 실패할 수 있음(예: `/api/auth/me`, `/api/notifications/unread`, `/api/home/...`). | 서버 로그에서 **정확한 요청 경로 + 스택 트레이스** 확인. |

---

## 2. 반드시 확인할 것

### 2.1 서버에서 실제 500 원인 잡기

iPhone 17 Max에서 **재현한 직후**에 다음을 확인하는 것이 가장 중요합니다.

1. **어느 서비스에서 500이 났는지**
   - Gateway: `docker logs dorandoran-gateway 2>&1 | tail -200`
   - Auth: `docker logs dorandoran-auth 2>&1 | tail -200`
   - User: `docker logs dorandoran-user 2>&1 | tail -200`
2. **해당 로그에 찍힌 예외 메시지 + stack trace**
   - `"예상치 못한 오류"`, `NoResourceFoundException`, `NumberFormatException`, `IllegalArgumentException` 등이 있으면 원인 추적에 결정적입니다.
3. **실패한 요청의 경로**
   - 예: `GET /api/auth/me`, `GET /api/notifications/unread` 등.

같은 시각대의 nginx access 로그도 보면 좋습니다.

```bash
# 서버에서 (예시)
sudo grep "500" /var/log/nginx/access.log | tail -20
docker logs --since 5m dorandoran-user 2>&1 | grep -E "ERROR|Exception|500"
```

---

## 3. 적용한 코드 변경 (Gateway)

- **파일**: `gateway/.../JwtAuthFilter.java`
- **내용**:
  - JWT payload에서 추출한 `sub` 값을 **UUID 형식인지 검사**하는 `isValidUuid()` 추가.
  - `sub`가 UUID가 아니면 `X-User-Id` 등을 넣지 않고 요청만 그대로 전달.
- **효과**: 특정 기기에서 JWT 파싱/클레임 구조가 달라져 잘못된 `sub`가 나와도, 하위 서비스에서 `UUID.fromString(...)` 예외로 500이 나는 상황을 막을 수 있습니다. (대신 해당 요청은 401/400에 가깝게 처리될 수 있음.)

---

## 4. 추가로 점검하면 좋은 것

1. **Capacitor / iOS 빌드**
   - iPhone 17 Max에서 보내는 **Origin**, **User-Agent**를 앱/프록시 로그로 한 번 확인.
   - 필요하면 Gateway/백엔드 CORS 허용 목록에 해당 Origin 추가.
2. **Auth 서비스**
   - `RefreshToken`의 `user_agent`(500자), `device_id`(200자) 제한.  
     실제 로그인/리프레시 시 전달되는 값이 이 길이를 넘지 않는지 확인(넘으면 DB 예외 → 500 가능).
3. **Apple 로그인**
   - 해당 기기에서 Apple 로그인으로만 500이 나는지 여부.  
     Apple identity token 형식/클레임이 기기/OS별로 다를 수 있음.

---

## 5. 요약

- **즉시 할 일**: iPhone 17 Max에서 500을 **한 번 재현**한 뒤, 위 2.1처럼 **어느 서비스 로그에 어떤 예외(스택 트레이스)가 찍렸는지** 확인.
- **이미 적용한 것**: Gateway에서 JWT `sub`를 UUID로 검증해, 잘못된 `X-User-Id`로 인한 하위 서비스 500 가능성을 줄임.
- **추가 조치**: 로그로 확인한 예외 종류에 따라, CORS/Origin, Auth 엔티티 길이, Apple 로그인 등 위 4번을 순서대로 점검하면 됩니다.
