# 어드민 프론트엔드 Vercel/Netlify 배포 가이드

## 개요

`dorandoran-admin-frontend`를 Vercel, Netlify 같은 정적 호스팅 플랫폼에 배포하여 별도 URL로 서비스하는 방법입니다.

## 장점

- ✅ 무료 플랜 제공 (Vercel, Netlify)
- ✅ 자동 HTTPS 지원
- ✅ Git 연동 자동 배포
- ✅ CDN 자동 적용
- ✅ 서버 관리 불필요
- ✅ 빠른 전 세계 배포

---

## 옵션 1: Vercel 배포 (권장)

### 1단계: Vercel 계정 생성 및 프로젝트 연결

1. [Vercel](https://vercel.com)에 가입/로그인
2. "Add New Project" 클릭
3. Git 저장소 연결 (GitHub, GitLab, Bitbucket)
   - 또는 "Import Git Repository"로 직접 연결

### 2단계: 프로젝트 설정

**프로젝트 설정 값:**
- **Framework Preset**: `Vite`
- **Root Directory**: `dorandoran-admin-frontend`
- **Build Command**: `npm run build`
- **Output Directory**: `dist`
- **Install Command**: `npm install`

### 3단계: 환경 변수 설정

Vercel 대시보드에서 **Settings → Environment Variables**에 추가:

```
VITE_API_BASE_URL=https://api.doran-chat.com
```

또는 프로덕션/프리뷰/개발 환경별로 다르게 설정 가능:
- **Production**: `https://api.doran-chat.com`
- **Preview**: `https://api.doran-chat.com` (또는 테스트 서버)
- **Development**: `http://localhost:8080`

### 4단계: Vercel 설정 파일 생성 (선택사항)

프로젝트 루트에 `vercel.json` 파일 생성:

```json
{
  "rewrites": [
    {
      "source": "/(.*)",
      "destination": "/index.html"
    }
  ],
  "headers": [
    {
      "source": "/(.*)",
      "headers": [
        {
          "key": "X-Content-Type-Options",
          "value": "nosniff"
        },
        {
          "key": "X-Frame-Options",
          "value": "SAMEORIGIN"
        },
        {
          "key": "X-XSS-Protection",
          "value": "1; mode=block"
        }
      ]
    }
  ]
}
```

### 5단계: 배포

1. **자동 배포**: Git에 푸시하면 자동으로 배포됩니다
2. **수동 배포**: Vercel CLI 사용
   ```bash
   npm i -g vercel
   cd dorandoran-admin-frontend
   vercel
   ```

### 6단계: 커스텀 도메인 설정 (선택사항)

1. Vercel 대시보드 → **Settings → Domains**
2. 원하는 도메인 추가 (예: `admin.doran-chat.com`)
3. DNS 설정 안내에 따라 레코드 추가

---

## 옵션 2: Netlify 배포

### 1단계: Netlify 계정 생성 및 프로젝트 연결

1. [Netlify](https://www.netlify.com)에 가입/로그인
2. "Add new site" → "Import an existing project"
3. Git 저장소 연결

### 2단계: 빌드 설정

**Build settings:**
- **Base directory**: `dorandoran-admin-frontend`
- **Build command**: `npm run build`
- **Publish directory**: `dorandoran-admin-frontend/dist`

### 3단계: 환경 변수 설정

**Site settings → Environment variables**에 추가:

```
VITE_API_BASE_URL=https://api.doran-chat.com
```

### 4단계: Netlify 설정 파일 생성

프로젝트 루트에 `netlify.toml` 파일 생성:

```toml
[build]
  base = "dorandoran-admin-frontend"
  publish = "dist"
  command = "npm run build"

[[redirects]]
  from = "/*"
  to = "/index.html"
  status = 200

[[headers]]
  for = "/*"
  [headers.values]
    X-Content-Type-Options = "nosniff"
    X-Frame-Options = "SAMEORIGIN"
    X-XSS-Protection = "1; mode=block"
```

### 5단계: 배포

- **자동 배포**: Git에 푸시하면 자동 배포
- **수동 배포**: Netlify CLI 사용
  ```bash
  npm i -g netlify-cli
  cd dorandoran-admin-frontend
  netlify deploy --prod
  ```

---

## 옵션 3: GitHub Pages 배포

### 1단계: GitHub Actions 워크플로우 생성

`.github/workflows/deploy-admin-frontend.yml` 파일 생성:

```yaml
name: Deploy Admin Frontend

on:
  push:
    branches:
      - main
    paths:
      - 'dorandoran-admin-frontend/**'

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '18'
          
      - name: Install dependencies
        working-directory: dorandoran-admin-frontend
        run: npm ci
        
      - name: Build
        working-directory: dorandoran-admin-frontend
        env:
          VITE_API_BASE_URL: ${{ secrets.VITE_API_BASE_URL }}
        run: npm run build
        
      - name: Deploy to GitHub Pages
        uses: peaceiris/actions-gh-pages@v3
        with:
          github_token: ${{ secrets.GITHUB_TOKEN }}
          publish_dir: ./dorandoran-admin-frontend/dist
          cname: admin.doran-chat.com  # 커스텀 도메인 사용 시
```

### 2단계: GitHub Secrets 설정

Repository → **Settings → Secrets and variables → Actions**에서:

```
VITE_API_BASE_URL=https://api.doran-chat.com
```

### 3단계: GitHub Pages 활성화

Repository → **Settings → Pages**:
- **Source**: `GitHub Actions`

---

## 백엔드 CORS 설정 업데이트

어드민 프론트엔드의 배포 URL을 Gateway의 CORS 설정에 추가해야 합니다.

### Gateway CORS 설정 수정

`gateway/src/main/java/com/dorandoran/gateway/config/SecurityConfig.java`:

```java
@Bean
public CorsWebFilter corsWebFilter() {
    CorsConfiguration corsConfig = new CorsConfiguration();
    corsConfig.setAllowCredentials(true);

    // 로컬 개발 환경
    corsConfig.addAllowedOrigin("http://localhost:3000");
    corsConfig.addAllowedOrigin("http://localhost:3001");
    
    // 프로덕션 도메인
    corsConfig.addAllowedOrigin("https://doran-chat.com");
    corsConfig.addAllowedOrigin("https://www.doran-chat.com");
    
    // Vercel 배포 URL (와일드카드 패턴)
    corsConfig.addAllowedOriginPattern("https://*.vercel.app");
    
    // Netlify 배포 URL (와일드카드 패턴)
    corsConfig.addAllowedOriginPattern("https://*.netlify.app");
    
    // 커스텀 도메인 (어드민 프론트엔드)
    corsConfig.addAllowedOrigin("https://admin.doran-chat.com");
    corsConfig.addAllowedOriginPattern("https://*.doran-chat.com");
    
    corsConfig.addAllowedHeader("*");
    corsConfig.addAllowedMethod("*");
    corsConfig.addExposedHeader("*");

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", corsConfig);

    return new CorsWebFilter(source);
}
```

**참고**: 현재 코드에 이미 `https://*.vercel.app` 패턴이 포함되어 있어 Vercel 배포는 바로 작동합니다.

---

## 배포 후 확인 사항

### 1. 접속 확인
- 배포된 URL로 접속하여 로그인 페이지가 정상적으로 표시되는지 확인

### 2. API 연동 확인
- 브라우저 개발자 도구 → Network 탭에서 API 호출 확인
- CORS 에러가 없는지 확인

### 3. 환경 변수 확인
- 배포된 사이트에서 `console.log(import.meta.env.VITE_API_BASE_URL)`로 확인
- 또는 Network 탭에서 실제 API 호출 URL 확인

---

## 자동 배포 설정

### Vercel CLI 설치 및 배포

```bash
# Vercel CLI 설치
npm i -g vercel

# 프로젝트 디렉토리로 이동
cd dorandoran-admin-frontend

# 초기 배포 (첫 번째만)
vercel

# 프로덕션 배포
vercel --prod
```

### Netlify CLI 설치 및 배포

```bash
# Netlify CLI 설치
npm i -g netlify-cli

# 프로젝트 디렉토리로 이동
cd dorandoran-admin-frontend

# 초기 로그인 및 연결
netlify login
netlify init

# 프로덕션 배포
netlify deploy --prod
```

---

## 문제 해결

### 1. CORS 에러

**증상**: `Access to fetch at '...' from origin '...' has been blocked by CORS policy`

**해결**:
- Gateway의 CORS 설정에 배포 URL 추가
- 배포 URL이 와일드카드 패턴에 포함되는지 확인

### 2. 404 에러 (SPA 라우팅)

**증상**: 직접 URL 접속 시 404 에러

**해결**:
- Vercel: `vercel.json`에 `rewrites` 설정 확인
- Netlify: `netlify.toml`에 `redirects` 설정 확인
- GitHub Pages: `404.html` 파일 생성 또는 `_redirects` 파일 추가

### 3. 환경 변수 미적용

**증상**: API URL이 잘못된 값으로 설정됨

**해결**:
- 배포 플랫폼의 환경 변수 설정 확인
- 환경 변수 이름이 `VITE_`로 시작하는지 확인
- 빌드 후 재배포

### 4. 빌드 실패

**증상**: 배포 시 빌드 에러

**해결**:
- 로컬에서 `npm run build` 성공 여부 확인
- Node.js 버전 확인 (보통 18.x 권장)
- 의존성 설치 확인 (`npm ci` 사용 권장)

---

## 추천 배포 플랫폼 비교

| 플랫폼 | 무료 플랜 | 자동 배포 | 커스텀 도메인 | CDN | 추천도 |
|--------|----------|----------|--------------|-----|--------|
| **Vercel** | ✅ (제한적) | ✅ | ✅ | ✅ | ⭐⭐⭐⭐⭐ |
| **Netlify** | ✅ (제한적) | ✅ | ✅ | ✅ | ⭐⭐⭐⭐ |
| **GitHub Pages** | ✅ | ✅ (Actions) | ✅ | ✅ | ⭐⭐⭐ |
| **Cloudflare Pages** | ✅ | ✅ | ✅ | ✅ | ⭐⭐⭐⭐ |

**추천**: Vercel (가장 빠르고 사용하기 쉬움)

---

## 다음 단계

1. 배포 플랫폼 선택 및 계정 생성
2. Git 저장소 연결
3. 환경 변수 설정
4. 배포 및 확인
5. 백엔드 CORS 설정 업데이트 (필요시)
6. 커스텀 도메인 설정 (선택사항)
