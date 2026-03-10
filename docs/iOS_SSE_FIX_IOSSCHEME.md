# iOS SSE 연결 실패 해결 - 방안 1: iosScheme 변경 적용 가이드

## 개요

iOS Capacitor 앱의 origin을 `capacitor://localhost` → `https://localhost`로 변경하여 CORS 이슈를 해결합니다.

---

## 수정 대상

| 항목 | 내용 |
|------|------|
| **파일 경로** | `dorandoran-frontend/capacitor.config.ts` |
| **수정 라인** | 8~11줄 |
| **변경 내용** | `server` 옵션에 `iosScheme: 'https'` 추가 |

---

## 수정 상세

### 수정 전 (현재 코드)

```typescript
  webDir: 'dist',
  // server: {
  //   url: 'http://localhost:3000',
  //   cleartext: true,
  // },
  plugins: {
```

### 수정 후

```typescript
  webDir: 'dist',
  server: {
    iosScheme: 'https',
  },
  // server: {
  //   url: 'http://localhost:3000',
  //   cleartext: true,
  // },
  plugins: {
```

---

## diff 형식 (참고용)

```diff
  webDir: 'dist',
+ server: {
+   iosScheme: 'https',
+ },
  // server: {
  //   url: 'http://localhost:3000',
  //   cleartext: true,
  // },
  plugins: {
```

---

## 적용 방법

1. `dorandoran-frontend/capacitor.config.ts` 파일을 엽니다.
2. `webDir: 'dist',` 다음에 아래 블록을 추가합니다.
3. iOS 앱을 다시 빌드합니다. (`npm run build` 후 `npx cap sync ios`)

---

## 서버 CORS 확인

Gateway의 CORS 설정에 `https://localhost`가 이미 허용되어 있으므로 **추가 수정 없음**입니다.

- `gateway/.../SecurityConfig.java` – `addAllowedOrigin("https://localhost")` 포함
- `gateway/.../CorsResponseFilter.java` – `ALLOWED_ORIGINS`에 `"https://localhost"` 포함

---

## 적용 후 검증

1. `npm run build` 후 `npx cap sync ios` 실행
2. Xcode에서 iOS 시뮬레이터/실기기로 실행
3. 채팅방 진입 시 SSE 연결 정상 여부 확인
