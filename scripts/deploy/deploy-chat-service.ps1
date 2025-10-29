# Chat 서비스 배포 스크립트
# 사용법: .\deploy-chat-service.ps1

param(
    [string]$EC2_IP = "3.21.177.186",
    [string]$KEY_PATH = "$env:USERPROFILE\Downloads\dorandoran-key.pem"
)

Write-Host "🚀 Chat 서비스 배포 시작..." -ForegroundColor Green

# 1. Chat 서비스 빌드
Write-Host "📦 Chat 서비스 빌드 중..." -ForegroundColor Yellow
.\gradlew.bat :chat:clean :chat:build -x test
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ 빌드 실패!" -ForegroundColor Red
    exit 1
}

# 2. Docker 이미지 생성
Write-Host "🐳 Docker 이미지 생성 중..." -ForegroundColor Yellow
$timestamp = Get-Date -Format "yyyyMMdd"
docker build -f docker/Dockerfile.chat -t "dorandoran-chat:prod-$timestamp" .
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Docker 이미지 생성 실패!" -ForegroundColor Red
    exit 1
}

docker tag "dorandoran-chat:prod-$timestamp" "dorandoran-chat:latest"

# 3. 이미지 압축
Write-Host "📦 이미지 압축 중..." -ForegroundColor Yellow
if (!(Test-Path "dist")) { New-Item -ItemType Directory -Path "dist" }
docker save dorandoran-chat:latest -o "dist/dorandoran-chat-prod.tar"
Compress-Archive -Path "dist/dorandoran-chat-prod.tar" -DestinationPath "dist/chat-prod.zip" -Force

# 4. AWS 서버로 전송
Write-Host "📤 AWS 서버로 전송 중..." -ForegroundColor Yellow
scp -i $KEY_PATH "dist/chat-prod.zip" "ec2-user@${EC2_IP}:/tmp/"
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ 파일 전송 실패!" -ForegroundColor Red
    exit 1
}

# 5. AWS 서버에서 배포
Write-Host "🚀 AWS 서버에서 배포 중..." -ForegroundColor Yellow
$deployCommands = @"
cd /tmp && unzip -o chat-prod.zip && docker load < dorandoran-chat-prod.tar
docker stop dorandoran-chat || true
docker rm dorandoran-chat || true
docker run -d --name dorandoran-chat --network dorandoran-network -p 8083:8083 --restart=unless-stopped \
  -e SPRING_PROFILES_ACTIVE=docker \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://dorandoran-shared-db:5432/dorandoran \
  -e SPRING_DATASOURCE_USERNAME=doran \
  -e SPRING_DATASOURCE_PASSWORD=doran \
  -e SPRING_JPA_HIBERNATE_DEFAULT_SCHEMA=chat_schema \
  -e SPRING_REDIS_HOST=dorandoran-redis \
  -e SPRING_REDIS_PORT=6379 \
  dorandoran-chat:latest
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
    # Chat 서비스 헬스체크
    $healthResponse = Invoke-WebRequest -Uri "http://${EC2_IP}:8080/api/chat/health" -Headers @{"Origin"="http://localhost:3000"} -TimeoutSec 10
    if ($healthResponse.StatusCode -eq 200) {
        Write-Host "✅ Chat 서비스 배포 성공!" -ForegroundColor Green
        Write-Host "📊 헬스체크 응답: $($healthResponse.Content.Substring(0, [Math]::Min(100, $healthResponse.Content.Length)))..." -ForegroundColor Cyan
    } else {
        Write-Host "⚠️ 배포는 완료되었지만 헬스체크 실패 (Status: $($healthResponse.StatusCode))" -ForegroundColor Yellow
    }
} catch {
    Write-Host "⚠️ 배포는 완료되었지만 검증 실패: $($_.Exception.Message)" -ForegroundColor Yellow
    Write-Host "💡 Chat 서비스는 정상적으로 배포되었을 수 있습니다. 수동으로 확인해보세요." -ForegroundColor Cyan
}

Write-Host "🎉 Chat 서비스 배포 완료!" -ForegroundColor Green
Write-Host "📋 사용 가능한 Chat API:" -ForegroundColor Cyan
Write-Host "  - GET  http://${EC2_IP}:8080/api/chat/health (헬스체크)" -ForegroundColor White
Write-Host "  - GET  http://${EC2_IP}:8080/api/chat/rooms (채팅방 목록)" -ForegroundColor White
Write-Host "  - POST http://${EC2_IP}:8080/api/chat/rooms (채팅방 생성)" -ForegroundColor White
Write-Host "  - GET  http://${EC2_IP}:8080/api/chat/rooms/{roomId}/messages (메시지 조회)" -ForegroundColor White
Write-Host "  - POST http://${EC2_IP}:8080/api/chat/rooms/{roomId}/messages (메시지 전송)" -ForegroundColor White
Write-Host "  - GET  http://${EC2_IP}:8080/api/chat/events/{roomId} (SSE 이벤트 스트림)" -ForegroundColor White
