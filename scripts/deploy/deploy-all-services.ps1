# 전체 서비스 배포 스크립트
# 사용법: .\deploy-all-services.ps1

param(
    [string]$EC2_IP = "3.21.177.186",
    [string]$KEY_PATH = "$env:USERPROFILE\Downloads\dorandoran-key.pem",
    [switch]$SkipUser = $false,
    [switch]$SkipGateway = $false,
    [switch]$SkipChat = $false
)

Write-Host "🚀 전체 서비스 배포 시작..." -ForegroundColor Green

# User 서비스 배포
if (-not $SkipUser) {
    Write-Host "`n📦 User 서비스 배포 중..." -ForegroundColor Cyan
    .\deploy-user-service.ps1 -EC2_IP $EC2_IP -KEY_PATH $KEY_PATH
    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ User 서비스 배포 실패!" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "⏭️ User 서비스 배포 건너뜀" -ForegroundColor Yellow
}

# Gateway 서비스 배포
if (-not $SkipGateway) {
    Write-Host "`n📦 Gateway 서비스 배포 중..." -ForegroundColor Cyan
    .\deploy-gateway-service.ps1 -EC2_IP $EC2_IP -KEY_PATH $KEY_PATH
    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ Gateway 서비스 배포 실패!" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "⏭️ Gateway 서비스 배포 건너뜀" -ForegroundColor Yellow
}

# Chat 서비스 배포
if (-not $SkipChat) {
    Write-Host "`n📦 Chat 서비스 배포 중..." -ForegroundColor Cyan
    .\deploy-chat-service.ps1 -EC2_IP $EC2_IP -KEY_PATH $KEY_PATH
    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ Chat 서비스 배포 실패!" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "⏭️ Chat 서비스 배포 건너뜀" -ForegroundColor Yellow
}

# 최종 통합 테스트
Write-Host "`n🔍 최종 통합 테스트 중..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

try {
    # CORS Preflight 테스트
    $corsResponse = Invoke-WebRequest -Uri "http://${EC2_IP}:8080/api/users" -Method OPTIONS -Headers @{"Origin"="http://localhost:3000"; "Access-Control-Request-Method"="POST"; "Access-Control-Request-Headers"="Content-Type"} -TimeoutSec 10
    
    # 사용자 조회 테스트
    $userResponse = Invoke-WebRequest -Uri "http://${EC2_IP}:8080/api/users/email/test300@example.com" -Headers @{"Origin"="http://localhost:3000"} -TimeoutSec 10
    
    # Chat 서비스 헬스체크 테스트
    $chatResponse = Invoke-WebRequest -Uri "http://${EC2_IP}:8080/api/chat/health" -Headers @{"Origin"="http://localhost:3000"} -TimeoutSec 10
    
    if ($corsResponse.StatusCode -eq 200 -and $userResponse.StatusCode -eq 200 -and $chatResponse.StatusCode -eq 200) {
        Write-Host "✅ 모든 서비스 정상 작동!" -ForegroundColor Green
        Write-Host "📊 CORS: OK, User API: OK, Chat API: OK" -ForegroundColor Cyan
    } else {
        Write-Host "⚠️ 일부 서비스에 문제가 있을 수 있습니다" -ForegroundColor Yellow
        Write-Host "📊 CORS: $($corsResponse.StatusCode), User: $($userResponse.StatusCode), Chat: $($chatResponse.StatusCode)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "⚠️ 통합 테스트 실패: $($_.Exception.Message)" -ForegroundColor Yellow
}

Write-Host "`n🎉 전체 배포 완료!" -ForegroundColor Green
Write-Host "📋 사용 가능한 API:" -ForegroundColor Cyan
Write-Host "  - POST http://${EC2_IP}:8080/api/users (사용자 생성)" -ForegroundColor White
Write-Host "  - GET  http://${EC2_IP}:8080/api/users/email/{email} (이메일로 사용자 조회)" -ForegroundColor White
Write-Host "  - GET  http://${EC2_IP}:8080/api/users/health (User 헬스체크)" -ForegroundColor White
Write-Host "  - GET  http://${EC2_IP}:8080/api/chat/health (Chat 헬스체크)" -ForegroundColor White
Write-Host "  - GET  http://${EC2_IP}:8080/api/chat/rooms (채팅방 목록)" -ForegroundColor White
Write-Host "  - POST http://${EC2_IP}:8080/api/chat/rooms (채팅방 생성)" -ForegroundColor White
Write-Host "  - GET  http://${EC2_IP}:8080/api/chat/events/{roomId} (SSE 이벤트 스트림)" -ForegroundColor White
