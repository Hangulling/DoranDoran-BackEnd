# User 서비스 배포 스크립트
# 사용법: .\deploy-user-service.ps1

param(
    [string]$EC2_IP = "3.21.177.186",
    [string]$KEY_PATH = "$env:USERPROFILE\Downloads\dorandoran-key.pem"
)

Write-Host "🚀 User 서비스 배포 시작..." -ForegroundColor Green

# 1. User 서비스 빌드
Write-Host "📦 User 서비스 빌드 중..." -ForegroundColor Yellow
.\gradlew.bat :user:clean :user:build -x test
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ 빌드 실패!" -ForegroundColor Red
    exit 1
}

# 2. Docker 이미지 생성
Write-Host "🐳 Docker 이미지 생성 중..." -ForegroundColor Yellow
$timestamp = Get-Date -Format "yyyyMMdd"
docker build -f docker/Dockerfile.user -t "dorandoran-user:prod-$timestamp" .
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Docker 이미지 생성 실패!" -ForegroundColor Red
    exit 1
}

docker tag "dorandoran-user:prod-$timestamp" "dorandoran-user:latest"

# 3. 이미지 압축
Write-Host "📦 이미지 압축 중..." -ForegroundColor Yellow
if (!(Test-Path "dist")) { New-Item -ItemType Directory -Path "dist" }
docker save dorandoran-user:latest -o "dist/dorandoran-user-prod.tar"
Compress-Archive -Path "dist/dorandoran-user-prod.tar" -DestinationPath "dist/user-prod.zip" -Force

# 4. AWS 서버로 전송
Write-Host "📤 AWS 서버로 전송 중..." -ForegroundColor Yellow
scp -i $KEY_PATH "dist/user-prod.zip" "ec2-user@${EC2_IP}:/tmp/"
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ 파일 전송 실패!" -ForegroundColor Red
    exit 1
}

# 5. AWS 서버에서 배포
Write-Host "🚀 AWS 서버에서 배포 중..." -ForegroundColor Yellow
$deployCommands = @"
cd /tmp && unzip -o user-prod.zip && docker load < dorandoran-user-prod.tar
docker stop dorandoran-user || true
docker rm dorandoran-user || true
docker run -d --name dorandoran-user --network dorandoran-network -p 8082:8082 --restart=unless-stopped \
  -e SPRING_PROFILES_ACTIVE=docker \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://dorandoran-shared-db:5432/dorandoran \
  -e SPRING_DATASOURCE_USERNAME=doran \
  -e SPRING_DATASOURCE_PASSWORD=doran \
  -e SPRING_JPA_HIBERNATE_DEFAULT_SCHEMA=user_schema \
  -e SPRING_REDIS_HOST=dorandoran-redis \
  -e SPRING_REDIS_PORT=6379 \
  dorandoran-user:latest
"@

ssh -i $KEY_PATH "ec2-user@${EC2_IP}" $deployCommands
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ 배포 실패!" -ForegroundColor Red
    exit 1
}

# 6. 배포 검증
Write-Host "⏳ 서비스 시작 대기 중..." -ForegroundColor Yellow
Start-Sleep -Seconds 30

Write-Host "🔍 배포 검증 중..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://${EC2_IP}:8080/api/users/email/test300@example.com" -Headers @{"Origin"="http://localhost:3000"} -TimeoutSec 10
    if ($response.StatusCode -eq 200) {
        Write-Host "✅ User 서비스 배포 성공!" -ForegroundColor Green
        Write-Host "📊 응답: $($response.Content.Substring(0, [Math]::Min(100, $response.Content.Length)))..." -ForegroundColor Cyan
    } else {
        Write-Host "⚠️ 배포는 완료되었지만 검증 실패 (Status: $($response.StatusCode))" -ForegroundColor Yellow
    }
} catch {
    Write-Host "⚠️ 배포는 완료되었지만 검증 실패: $($_.Exception.Message)" -ForegroundColor Yellow
}

Write-Host "🎉 배포 완료!" -ForegroundColor Green
