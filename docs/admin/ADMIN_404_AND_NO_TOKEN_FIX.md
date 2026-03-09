# Admin 404 및 "No token found" 오류 원인 및 수정

## 1. 로그 요약

- `No token found, redirecting to login.` → AdminPrivateRoute에서 `accessToken`이 없을 때 출력 후 `/login`으로 리다이렉트
- `api.doran-chat.com/api/admin/prompts/options/concepts` 등 **404** → 해당 요청이 백엔드까지 도달하지 못함

---

## 2. 원인

### 2.1 404가 나는 이유: Gateway에 `/api/admin/**` 라우트 없음

Gateway(`application.yml` / `application-docker.yml`)에는 다음만 정의되어 있었음:

- `/api/auth/**` → Auth
- `/api/users/**` → User
- `/api/home/**`, `/api/support/**`, `/api/notifications/**` → User
- `/api/chat/**` → Chat
- `/api/deeplink/...` → User / Chat
- `/api/store/**`, `/api/batch/**` → Store / Batch

**`/api/admin/**`에 대한 라우트가 없어서**,  
`/api/admin/prompts/...`, `/api/admin/review-tickets` 등 Admin API 요청이 **어느 서비스로도 전달되지 않고 Gateway 단에서 404**가 발생함.

### 2.2 "No token found"가 나는 이유

- Admin 페이지(`/prompts/test-and-apply` 등)는 `AdminPrivateRoute`로 보호됨.
- `sessionStorage`에 `accessToken`이 없으면 (비로그인, 새 탭, 만료 후 정리 등)  
  `"No token found, redirecting to login."` 로그 후 `/login`으로 보냄.
- 이때 로그인 전이거나, 로그인 직후가 아니면 토큰이 없으므로 위 메시지가 정상 동작임.

---

## 3. 수정 내용 (Gateway)

**파일**: `gateway/src/main/resources/application.yml`, `application-docker.yml`

**추가한 라우트**:

```yaml
# Admin API 라우팅 (User 서비스: prompts, review-tickets, management-queue, chat-logs, audit-logs, support 등)
- id: user-service-admin
  uri: http://dorandoran-user:8082
  predicates:
    - Path=/api/admin/**
  filters: []
```

- `/api/admin/**` 요청을 **User 서비스(dorandoran-user:8082)** 로 보내도록 함.
- User 서비스에 이미  
  `/api/admin/prompts/**`, `/api/admin/review-tickets/**`, `/api/admin/management-queue/**`,  
  `/api/admin/chat-logs/**`, `/api/admin/audit-logs/**`, `/api/admin/support/**` 등이 있으므로,  
  Gateway만 배포해 주면 해당 Admin API들은 404가 아니라 **정상 라우팅**됨.

참고: `/api/admin/blacklist`는 Gateway 자체 컨트롤러에서 처리하는 경우,  
Spring Cloud Gateway에서 라우트와 컨트롤러 매핑 순서에 따라 위 라우트에 걸릴 수 있음.  
blacklist만 404가 나오면 blacklist 전용 라우트/필터로 분리하는 식으로 추가 조정 가능.

---

## 4. 배포 후 확인 순서

1. **Gateway 배포**  
   위 설정이 반영된 Gateway를 배포해야 Admin API 404가 사라짐.

2. **로그인 후 Admin 사용**  
   - 먼저 `/login`에서 관리자 계정으로 로그인.
   - 로그인 성공 후 `accessToken`/`adminUser`가 들어간 상태에서  
     `/prompts/test-and-apply` 등 Admin 페이지 접속.
   - 이렇게 하면 "No token found" 없이, 같은 탭/같은 도메인에서 API 호출이 200으로 나와야 함.

3. **그래도 401이 나는 경우**  
   - JWT에 `role`이 포함된 Auth 배포 여부 확인.
   - Gateway에서 `/api/admin/*` 요청 시 `ROLE_ADMIN` 검증을 하므로,  
     해당 계정이 DB에서 `ROLE_ADMIN`인지, 그리고 **한 번 더 로그인해서 role이 들어간 새 토큰**을 받았는지 확인.

---

## 5. 요약

| 현상 | 원인 | 조치 |
|------|------|------|
| `/api/admin/*` 404 | Gateway에 `/api/admin/**` 라우트 없음 | Gateway에 `user-service-admin` 라우트 추가 (완료) |
| No token found, redirecting to login | 해당 탭에 `accessToken` 없음 (비로그인/만료/다른 탭) | 로그인 후 같은 탭에서 Admin 페이지 접속 |
