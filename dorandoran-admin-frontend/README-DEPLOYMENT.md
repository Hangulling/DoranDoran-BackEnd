# 어드민 프론트엔드 배포 가이드

## 빠른 시작

### Vercel 배포 (권장)

1. [Vercel](https://vercel.com)에 로그인
2. "Add New Project" 클릭
3. Git 저장소 연결
4. 프로젝트 설정:
   - **Framework Preset**: `Vite`
   - **Root Directory**: `dorandoran-admin-frontend`
   - **Build Command**: `npm run build`
   - **Output Directory**: `dist`
5. 환경 변수 설정:
   - `VITE_API_BASE_URL=https://api.doran-chat.com`
6. 배포 완료!

### Netlify 배포

1. [Netlify](https://www.netlify.com)에 로그인
2. "Add new site" → "Import an existing project"
3. Git 저장소 연결
4. 빌드 설정:
   - **Base directory**: `dorandoran-admin-frontend`
   - **Build command**: `npm run build`
   - **Publish directory**: `dorandoran-admin-frontend/dist`
5. 환경 변수 설정:
   - `VITE_API_BASE_URL=https://api.doran-chat.com`
6. 배포 완료!

## 상세 가이드

자세한 배포 방법은 다음 문서를 참고하세요:
- [Vercel/Netlify 배포 가이드](../docs/deployment/admin-frontend-vercel-netlify.md)
- [서버 직접 배포 가이드](../docs/deployment/admin-frontend-deployment.md)

## 환경 변수

배포 플랫폼에서 다음 환경 변수를 설정하세요:

```
VITE_API_BASE_URL=https://api.doran-chat.com
```

## 로컬 개발

```bash
# 의존성 설치
npm install

# 개발 서버 실행 (포트 3001)
npm run dev

# 프로덕션 빌드
npm run build
```

## 문제 해결

### CORS 에러
- Gateway 서비스의 CORS 설정에 배포 URL이 포함되어 있는지 확인
- 현재 `https://*.vercel.app`와 `https://*.netlify.app` 패턴이 허용되어 있습니다

### 404 에러 (SPA 라우팅)
- `vercel.json` 또는 `netlify.toml` 파일이 올바르게 설정되어 있는지 확인

### 환경 변수 미적용
- 환경 변수 이름이 `VITE_`로 시작하는지 확인
- 빌드 후 재배포 필요
