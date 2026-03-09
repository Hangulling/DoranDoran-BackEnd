# Koach-Admin Vercel 배포 가이드

## 개요

**Koach-Admin**(어드민 전용 프론트엔드)을 Vercel에 배포하고, 실서버(Gateway/API)와 연동하는 방법입니다.

| 항목 | 내용 |
|------|------|
| **저장소** | https://github.com/Hangulling/Koach-Admin.git |
| **배포 URL 예시** | https://koach-admin-l5mc.vercel.app (로그인: `/login`) |
| **스택** | Vite + React (SPA), Tailwind, DaisyUI |
| **실서버 연동** | `VITE_API_BASE_URL`로 Gateway 주소 지정. CORS는 `https://*.vercel.app` 이미 허용됨 |

---

## 1. 사전 확인

### 1.1 실서버(Gateway) URL

어드민에서 API를 호출할 **Gateway 주소**를 정해 둡니다.

- 예: `https://api.doran-chat.com`
- 로컬: `http://localhost:8080`

### 1.2 CORS

Gateway에 이미 `https://*.vercel.app` 가 허용되어 있으므로, Vercel 기본 URL(`xxx.vercel.app`)로 배포하면 별도 CORS 수정 없이 연동됩니다.

---

## 2. Vercel 배포 절차

### 2.1 저장소 연결

1. [Vercel](https://vercel.com) 로그인
2. **Add New** → **Project**
3. **Import Git Repository** 에서 `Hangulling/Koach-Admin` 선택 (또는 해당 저장소 연결)
4. **Import** 후 프로젝트 설정 화면으로 이동

### 2.2 프로젝트 설정

**Koach-Admin을 단일 저장소로 배포하는 경우** (저장소가 Koach-Admin 하나일 때)

| 항목 | 값 |
|------|-----|
| **Framework Preset** | Vite |
| **Root Directory** | *(비워 둠)* |
| **Build Command** | `npm run build` |
| **Output Directory** | `dist` |
| **Install Command** | `npm install` |

**igu 모노레포에서 Koach-Admin 서브모듈을 배포하는 경우** (저장소가 igu이고 Koach-Admin이 서브모듈일 때)

| 항목 | 값 |
|------|-----|
| **Framework Preset** | Vite |
| **Root Directory** | `Koach-Admin` |
| **Build Command** | `npm run build` |
| **Output Directory** | `dist` |
| **Install Command** | `npm install` |

- **Node.js**: Koach-Admin의 `package.json`에 `"engines": { "node": ">=22.0.0" }` 이 있으면, Vercel **Settings → General → Node.js Version** 에서 `22.x` 선택 권장.

### 2.3 환경 변수

Vercel 대시보드: **Settings → Environment Variables** 에서 추가합니다.

| 이름 | 설명 | 예시 |
|------|------|------|
| `VITE_API_BASE_URL` | API Gateway 주소 (끝 `/` 제거) | `https://api.doran-chat.com` |

- 실서버 연동에 **필수**인 것은 `VITE_API_BASE_URL` 하나입니다.
- Production / Preview / Development 환경별로 다른 값을 줄 수 있습니다 (예: Preview에 스테이징 URL).

### 2.4 vercel.json (저장소에 이미 있음)

Koach-Admin 저장소 루트의 `vercel.json` 에 SPA 라우팅과 보안 헤더가 이미 설정되어 있습니다.

- `rewrites`: `/(.*)` → `/index.html` (SPA 라우팅)
- `headers`: X-Content-Type-Options, X-Frame-Options, X-XSS-Protection

Root Directory를 비우거나 `Koach-Admin`으로 두면 이 파일이 자동으로 사용됩니다.

### 2.5 배포 실행

- **Vercel Git 연동**: 설정 저장 후 **Deploy** 한 번 실행. 이후 연결한 브랜치에 푸시할 때마다 **자동으로 빌드·배포**됩니다.
- **수동 (CLI)**:
  ```bash
  npm i -g vercel
  cd Koach-Admin   # 또는 Koach-Admin 저장소 루트
  vercel
  # 프로덕션: vercel --prod
  ```

배포가 끝나면 `https://<프로젝트명>.vercel.app` 형태의 URL이 부여됩니다. (예: `https://koach-admin-l5mc.vercel.app`)

---

## 3. 배포 후 확인

1. **접속**: 배포 URL로 접속 (예: `https://koach-admin-l5mc.vercel.app/login`)
2. **로그인**: 어드민 계정으로 로그인 동작 확인
3. **API 호출**: 브라우저 **Network** 탭에서 요청이 `VITE_API_BASE_URL` 로 가는지 확인
4. **CORS**: 콘솔에 CORS 에러가 없어야 함 (`*.vercel.app` 이미 허용됨)

---

## 4. 커스텀 도메인 (선택)

- Vercel **Settings → Domains** 에서 도메인 추가 (예: `admin.doran-chat.com`)
- DNS에 CNAME 또는 A 레코드 설정
- 커스텀 도메인을 쓰는 경우, 해당 도메인이 Gateway CORS에 포함돼 있어야 합니다.

---

## 5. 문제 해결

### 5.1 API 요청이 실서버로 안 감

- Vercel **Environment Variables** 에 `VITE_API_BASE_URL` 이 **Production(및 사용 중인 환경)** 에 설정돼 있는지 확인.
- `VITE_` 변수는 빌드 시점에만 주입되므로, 값을 바꾼 뒤에는 **재배포(Redeploy)** 가 필요합니다.

### 5.2 CORS 에러

- Gateway에 `https://*.vercel.app` 가 있으므로 기본 Vercel URL에서는 발생하지 않아야 합니다.
- 커스텀 도메인을 쓰면 해당 도메인이 Gateway `SecurityConfig` 에 포함돼 있는지 확인하세요.

### 5.3 404 (새로고침 시)

- `vercel.json` 의 `rewrites` 가 `/(.*)` → `/index.html` 로 되어 있는지 확인 (Koach-Admin 기본 설정에 포함됨).

### 5.4 빌드 실패

- 로컬에서 `cd Koach-Admin && npm ci && npm run build` 가 성공하는지 확인.
- Node 22 사용 시 Vercel **Node.js Version** 을 22.x 로 맞춥니다.

---

## 6. 자동화 (GitHub Actions)

푸시 시 GitHub Actions로 Vercel 배포를 실행하려면 아래 시크릿을 설정합니다. (igu 모노레포에 `.github/workflows/vercel-deploy.yml` 이 있는 경우)

### 6.1 필요한 GitHub 시크릿

| 시크릿 이름 | 설명 | 얻는 방법 |
|-------------|------|-----------|
| `VERCEL_TOKEN` | Vercel API 토큰 | [Vercel → Account Settings → Tokens](https://vercel.com/account/tokens) 에서 생성 |
| `VERCEL_ORG_ID` | 팀/개인 조직 ID | 로컬에서 `vercel link` 후 `.vercel/project.json` 의 `orgId` |
| `VERCEL_PROJECT_ID` | 프로젝트 ID | 위와 동일, `.vercel/project.json` 의 `projectId` |

**orgId / projectId 확인**

1. Koach-Admin 디렉터리로 이동 (단일 레포 루트 또는 igu 내 `Koach-Admin`)
2. `npx vercel link` 실행 후 배포 중인 Vercel 프로젝트 선택
3. 생성된 `.vercel/project.json` 예시:
   ```json
   { "orgId": "team_xxxx", "projectId": "prj_xxxx" }
   ```
4. GitHub 저장소 **Settings → Secrets and variables → Actions** 에서 위 세 시크릿 추가

### 6.2 워크플로 동작 (igu 모노레포 기준)

- **브랜치**: `main`, `develop` 푸시 시 실행
- **main** 푸시 → 프로덕션 배포 (`--prod`)
- **develop** 등 → Preview 배포
- **배포 경로**: `env.VERCEL_WORKING_DIR` 를 `Koach-Admin` 으로 두면 Koach-Admin만 배포됨

### 6.3 Git 연동과 중복 배포

- Vercel에서 Git 연동을 켜 두면 푸시할 때마다 Vercel이 직접 배포합니다. (가장 흔한 방식)
- GitHub Actions로만 배포하려면 Vercel **Settings → Git** 에서 연동을 끄거나, `github.enabled: false` 설정을 사용하세요.

---

## 7. 요약 체크리스트

- [ ] Vercel 프로젝트 생성, Git 저장소 연결 (Hangulling/Koach-Admin 또는 igu)
- [ ] Root Directory: 단일 레포면 비움, igu 모노레포면 `Koach-Admin`
- [ ] Build: `npm run build`, Output: `dist`
- [ ] 환경 변수: `VITE_API_BASE_URL` = 실서버 URL (필수)
- [ ] Deploy 후 `https://<프로젝트>.vercel.app/login` 접속·로그인·API 확인
- [ ] (선택) GitHub Actions: `VERCEL_TOKEN`, `VERCEL_ORG_ID`, `VERCEL_PROJECT_ID` 시크릿 설정
- [ ] (선택) 커스텀 도메인 연결

이 순서대로 진행하면 Koach-Admin을 Vercel에 띄우고 실서버와 연동할 수 있습니다.
