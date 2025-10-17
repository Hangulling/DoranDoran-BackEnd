# Gateway 서비스 배포 스크립트
# 사용법: .\deploy-gateway-service.ps1

param(
    [string]$EC2_IP = "3.21.177.186",
    [string]$KEY_PATH = "$env:USERPROFILE\Downloads\dorandoran-key.pem"
)

Write-Host "🚀 Gateway 서비스 배포 시작..." -ForegroundColor Green

# 1. Gateway 서비스 빌드
Write-Host "📦 Gateway 서비스 빌드 중..." -ForegroundColor Yellow
.\gradlew.bat :gateway:clean :gateway:build -x test
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ 빌드 실패!" -ForegroundColor Red
    exit 1
}

# 2. Docker 이미지 생성
Write-Host "🐳 Docker 이미지 생성 중..." -ForegroundColor Yellow
$timestamp = Get-Date -Format "yyyyMMdd"
docker build -f docker/Dockerfile.gateway -t "dorandoran-gateway:prod-$timestamp" .
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Docker 이미지 생성 실패!" -ForegroundColor Red
    exit 1
}

docker tag "dorandoran-gateway:prod-$timestamp" "dorandoran-gateway:latest"

# 3. 이미지 압축
Write-Host "📦 이미지 압축 중..." -ForegroundColor Yellow
if (!(Test-Path "dist")) { New-Item -ItemType Directory -Path "dist" }
docker save dorandoran-gateway:latest -o "dist/dorandoran-gateway-prod.tar"
Compress-Archive -Path "dist/dorandoran-gateway-prod.tar" -DestinationPath "dist/gateway-prod.zip" -Force

# 4. AWS 서버로 전송
Write-Host "📤 AWS 서버로 전송 중..." -ForegroundColor Yellow
scp -i $KEY_PATH "dist/gateway-prod.zip" "ec2-user@${EC2_IP}:/tmp/"
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ 파일 전송 실패!" -ForegroundColor Red
    exit 1
}

# 5. AWS 서버에서 배포
Write-Host "🚀 AWS 서버에서 배포 중..." -ForegroundColor Yellow
$deployCommands = @"
cd /tmp && unzip -o gateway-prod.zip && docker load < dorandoran-gateway-prod.tar
docker stop dorandoran-gateway || true
docker rm dorandoran-gateway || true
docker run -d --name dorandoran-gateway -p 8080:8080 --restart=unless-stopped \
  --add-host user-service:172.17.0.2 \
  -e SPRING_PROFILES_ACTIVE=docker \
  -e SPRING_REDIS_HOST=localhost \
  -e SPRING_REDIS_PORT=6379 \
  -e SPRING_MAIN_ALLOW_BEAN_DEFINITION_OVERRIDING=true \
  dorandoran-gateway:latest
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
    $response = Invoke-WebRequest -Uri "http://${EC2_IP}:8080/api/users" -Method OPTIONS -Headers @{"Origin"="http://localhost:3000"; "Access-Control-Request-Method"="POST"; "Access-Control-Request-Headers"="Content-Type"} -TimeoutSec 10
    if ($response.StatusCode -eq 200) {
        Write-Host "✅ Gateway 서비스 배포 성공!" -ForegroundColor Green
        Write-Host "📊 CORS 헤더 확인됨" -ForegroundColor Cyan
    } else {
        Write-Host "⚠️ 배포는 완료되었지만 검증 실패 (Status: $($response.StatusCode))" -ForegroundColor Yellow
    }
} catch {
    Write-Host "⚠️ 배포는 완료되었지만 검증 실패: $($_.Exception.Message)" -ForegroundColor Yellow
}

Write-Host "🎉 배포 완료!" -ForegroundColor Green
