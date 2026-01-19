# 어드민 프론트엔드 배포 가이드

## 개요

`dorandoran-admin-frontend`를 별도 URL로 배포하는 방법입니다. Nginx를 사용하여 정적 파일을 서빙합니다.

## 배포 방법

### 옵션 1: 서브도메인 사용 (권장)
예: `admin.doran-chat.com` 또는 `admin.dorandoran.com`

### 옵션 2: 경로 사용
예: `www.doran-chat.com/admin` 또는 `api.doran-chat.com/admin`

---

## 배포 단계

### 1단계: 로컬 빌드

```powershell
# 어드민 프론트엔드 디렉토리로 이동
cd dorandoran-admin-frontend

# 의존성 설치 (이미 완료됨)
npm install

# 프로덕션 빌드
npm run build
```

빌드 결과물은 `dorandoran-admin-frontend/dist/` 디렉토리에 생성됩니다.

### 2단계: 서버에 빌드 파일 전송

```powershell
# dist 폴더를 압축하여 전송
cd dorandoran-admin-frontend
Compress-Archive -Path dist -DestinationPath dist.zip -Force
scp -i "$env:USERPROFILE\Downloads\dorandoran-key.pem" dist.zip ec2-user@3.21.177.186:/home/ec2-user/

# 또는 dist 폴더 전체를 직접 전송 (rsync 사용, 더 효율적)
# rsync가 설치되어 있다면:
# rsync -avz -e "ssh -i $env:USERPROFILE\Downloads\dorandoran-key.pem" dist/ ec2-user@3.21.177.186:/var/www/admin/
```

### 3단계: 서버에서 파일 배치

SSH로 서버에 접속하여 파일을 배치합니다:

```bash
# SSH 접속
ssh -i "$env:USERPROFILE\Downloads\dorandoran-key.pem" ec2-user@3.21.177.186

# 서버에서 실행할 명령어들:
# 1. 어드민 프론트엔드 디렉토리 생성
sudo mkdir -p /var/www/admin

# 2. 압축 해제 (zip으로 전송한 경우)
cd /home/ec2-user
unzip -q dist.zip -d /var/www/admin/
sudo chown -R nginx:nginx /var/www/admin

# 또는 직접 전송한 경우
# sudo cp -r /home/ec2-user/dist/* /var/www/admin/
# sudo chown -R nginx:nginx /var/www/admin

# 3. 권한 설정
sudo chmod -R 755 /var/www/admin
```

### 4단계: Nginx 설정 추가

#### 옵션 A: 서브도메인 사용 (권장)

서버에서 Nginx 설정 파일을 생성/수정합니다:

```bash
# Nginx 설정 파일 생성
sudo nano /etc/nginx/conf.d/admin-frontend.conf
```

다음 내용을 추가:

```nginx
# 어드민 프론트엔드 (서브도메인)
server {
    listen 80;
    server_name admin.doran-chat.com;  # 또는 원하는 서브도메인

    root /var/www/admin;
    index index.html;

    # SPA 라우팅 지원 (모든 경로를 index.html로)
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 정적 파일 캐싱
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    # 보안 헤더
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
}
```

#### 옵션 B: 경로 사용

기존 Nginx 설정에 location 블록 추가:

```nginx
# 기존 server 블록 내에 추가
location /admin {
    alias /var/www/admin;
    index index.html;
    
    try_files $uri $uri/ /admin/index.html;
    
    # 정적 파일 캐싱
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
```

### 5단계: Nginx 설정 적용

```bash
# Nginx 설정 테스트
sudo nginx -t

# 설정이 올바르면 Nginx 재시작
sudo systemctl reload nginx
```

### 6단계: 방화벽 확인 (필요시)

```bash
# HTTP 포트가 열려있는지 확인
sudo firewall-cmd --list-all

# 필요시 포트 열기
sudo firewall-cmd --permanent --add-service=http
sudo firewall-cmd --reload
```

### 7단계: DNS 설정 (서브도메인 사용 시)

도메인 관리 패널에서 A 레코드 추가:
- **호스트**: `admin`
- **타입**: `A`
- **값**: `3.21.177.186`
- **TTL**: `300` (또는 기본값)

---

## 자동화 스크립트

배포를 자동화하는 PowerShell 스크립트를 생성할 수 있습니다:

### `scripts/deploy/deploy-admin-frontend.ps1`

```powershell
# 어드민 프론트엔드 배포 스크립트
Write-Host "=== DoranDoran Admin Frontend Deploy ===" -ForegroundColor Green

# 1. 빌드
Write-Host "`n[1/4] Building admin frontend..." -ForegroundColor Yellow
Set-Location dorandoran-admin-frontend
npm run build

if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed!" -ForegroundColor Red
    exit 1
}

# 2. 압축
Write-Host "`n[2/4] Compressing build files..." -ForegroundColor Yellow
Compress-Archive -Path dist -DestinationPath ../dist-admin.zip -Force

# 3. 서버 전송
Write-Host "`n[3/4] Uploading to server..." -ForegroundColor Yellow
$pemPath = "$env:USERPROFILE\Downloads\dorandoran-key.pem"
$serverHost = "ec2-user@3.21.177.186"

scp -i $pemPath ../dist-admin.zip ${serverHost}:/home/ec2-user/

if ($LASTEXITCODE -ne 0) {
    Write-Host "Upload failed!" -ForegroundColor Red
    exit 1
}

# 4. 서버에서 배포 실행
Write-Host "`n[4/4] Deploying on server..." -ForegroundColor Yellow

$deployCommand = @"
sudo mkdir -p /var/www/admin && \
cd /home/ec2-user && \
unzip -q -o dist-admin.zip -d /tmp/admin-dist && \
sudo rm -rf /var/www/admin/* && \
sudo cp -r /tmp/admin-dist/dist/* /var/www/admin/ && \
sudo chown -R nginx:nginx /var/www/admin && \
sudo chmod -R 755 /var/www/admin && \
sudo rm -rf /tmp/admin-dist && \
sudo nginx -t && sudo systemctl reload nginx && \
echo "Admin frontend deployed successfully"
"@

ssh -i $pemPath $serverHost $deployCommand

if ($LASTEXITCODE -eq 0) {
    Write-Host "`nDeployment completed successfully!" -ForegroundColor Green
} else {
    Write-Host "`nDeployment failed!" -ForegroundColor Red
    exit 1
}

# 정리
Remove-Item ../dist-admin.zip -Force -ErrorAction SilentlyContinue
Set-Location ..
```

---

## 환경 변수 설정

서버에 배포된 어드민 프론트엔드가 올바른 API URL을 사용하도록 확인:

### 빌드 시 환경 변수 주입

`.env.production` 파일 생성:

```bash
# dorandoran-admin-frontend/.env.production
VITE_API_BASE_URL=https://api.doran-chat.com
```

또는 빌드 시 환경 변수 지정:

```powershell
$env:VITE_API_BASE_URL="https://api.doran-chat.com"; npm run build
```

---

## SSL 인증서 설정 (HTTPS)

Let's Encrypt를 사용하여 SSL 인증서를 발급받습니다:

```bash
# 서버에서 실행
sudo certbot --nginx -d admin.doran-chat.com --non-interactive --agree-tos --email your-email@example.com

# 자동 갱신 설정
echo "0 12 * * * /usr/bin/certbot renew --quiet" | sudo crontab -
```

---

## 접속 확인

배포 완료 후 다음 URL로 접속 확인:

- **HTTP**: `http://admin.doran-chat.com` (또는 설정한 서브도메인)
- **HTTPS**: `https://admin.doran-chat.com` (SSL 설정 후)

---

## 문제 해결

### 1. 404 에러 (SPA 라우팅 문제)
- Nginx 설정에서 `try_files $uri $uri/ /index.html;` 확인
- 경로 사용 시 `/admin/index.html`로 수정

### 2. API 호출 실패
- 브라우저 개발자 도구에서 Network 탭 확인
- `VITE_API_BASE_URL` 환경 변수 확인
- CORS 설정 확인 (Gateway 서비스)

### 3. 정적 파일 로드 실패
- 파일 권한 확인: `sudo chown -R nginx:nginx /var/www/admin`
- Nginx 에러 로그 확인: `sudo tail -f /var/log/nginx/error.log`

### 4. Nginx 재시작 실패
- 설정 파일 문법 확인: `sudo nginx -t`
- 에러 메시지 확인 후 수정

---

## 참고사항

1. **빌드 파일 크기**: `dist` 폴더는 보통 1-5MB 정도입니다.
2. **캐싱**: 정적 파일은 브라우저에서 캐시되므로, 업데이트 후 강력 새로고침(Ctrl+F5)이 필요할 수 있습니다.
3. **자동 배포**: CI/CD 파이프라인을 구축하면 자동 배포가 가능합니다 (GitHub Actions, GitLab CI 등).
