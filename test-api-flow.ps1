# DoranDoran API 테스트 플로우
# 사용자 생성 -> 로그인 -> JWT 토큰 받기 -> 채팅방 생성 -> 메시지 전송

Write-Host "=== DoranDoran API 테스트 플로우 ===" -ForegroundColor Green

# 서비스 URL 설정
$USER_SERVICE_URL = "http://localhost:8082"
$AUTH_SERVICE_URL = "http://localhost:8081"
$CHAT_SERVICE_URL = "http://localhost:8080"

# 테스트용 사용자 정보
$TEST_EMAIL = "test@example.com"
$TEST_PASSWORD = "password123"
$TEST_NAME = "Test User"

Write-Host "`n1. 사용자 생성 중..." -ForegroundColor Yellow
$createUserBody = @{
    email = $TEST_EMAIL
    firstName = "Test"
    lastName = "User"
    name = $TEST_NAME
    password = $TEST_PASSWORD
    profileImageUrl = "https://example.com/profile.jpg"
    bio = "Test user for API testing"
} | ConvertTo-Json

try {
    $createResponse = Invoke-RestMethod -Uri "$USER_SERVICE_URL/api/users" -Method POST -Body $createUserBody -ContentType "application/json"
    Write-Host "사용자 생성 성공: $($createResponse.email)" -ForegroundColor Green
} catch {
    if ($_.Exception.Response.StatusCode -eq 400) {
        Write-Host "사용자가 이미 존재합니다. 계속 진행합니다." -ForegroundColor Yellow
    } else {
        Write-Host "사용자 생성 실패: $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    }
}

Write-Host "`n2. 로그인 중..." -ForegroundColor Yellow
$loginBody = @{
    email = $TEST_EMAIL
    password = $TEST_PASSWORD
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "$AUTH_SERVICE_URL/api/auth/login" -Method POST -Body $loginBody -ContentType "application/json"
    $accessToken = $loginResponse.data.accessToken
    $userId = $loginResponse.data.userId
    Write-Host "로그인 성공!" -ForegroundColor Green
    Write-Host "Access Token: $($accessToken.Substring(0, 20))..." -ForegroundColor Cyan
    Write-Host "User ID: $userId" -ForegroundColor Cyan
} catch {
    Write-Host "로그인 실패: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host "`n3. 토큰 검증 중..." -ForegroundColor Yellow
$headers = @{
    "Authorization" = "Bearer $accessToken"
}

try {
    $validateResponse = Invoke-RestMethod -Uri "$AUTH_SERVICE_URL/api/auth/validate" -Method GET -Headers $headers
    Write-Host "토큰 검증 성공: $($validateResponse.data.email)" -ForegroundColor Green
} catch {
    Write-Host "토큰 검증 실패: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host "`n4. 채팅방 생성 중..." -ForegroundColor Yellow
# 먼저 챗봇 ID를 생성 (테스트용)
$chatbotId = [System.Guid]::NewGuid().ToString()

$createRoomBody = @{
    userId = $userId
    chatbotId = $chatbotId
    name = "API 테스트 채팅방"
} | ConvertTo-Json

try {
    $roomResponse = Invoke-RestMethod -Uri "$CHAT_SERVICE_URL/api/chat/chatrooms" -Method POST -Body $createRoomBody -ContentType "application/json" -Headers $headers
    $chatroomId = $roomResponse.id
    Write-Host "채팅방 생성 성공: $chatroomId" -ForegroundColor Green
} catch {
    Write-Host "채팅방 생성 실패: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "응답: $($_.Exception.Response)" -ForegroundColor Red
    exit 1
}

Write-Host "`n5. 메시지 전송 중..." -ForegroundColor Yellow
$messageBody = @{
    content = "안녕하세요! API 테스트 메시지입니다."
    senderType = "user"
    contentType = "text/plain"
} | ConvertTo-Json

try {
    $messageResponse = Invoke-RestMethod -Uri "$CHAT_SERVICE_URL/api/chat/chatrooms/$chatroomId/messages" -Method POST -Body $messageBody -ContentType "application/json" -Headers $headers
    Write-Host "메시지 전송 성공: $($messageResponse.content)" -ForegroundColor Green
} catch {
    Write-Host "메시지 전송 실패: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "응답: $($_.Exception.Response)" -ForegroundColor Red
    exit 1
}

Write-Host "`n6. 채팅방 목록 조회 중..." -ForegroundColor Yellow
try {
    $roomsResponse = Invoke-RestMethod -Uri "$CHAT_SERVICE_URL/api/chat/chatrooms" -Method GET -Headers $headers
    Write-Host "채팅방 목록 조회 성공: $($roomsResponse.content.Count)개 채팅방" -ForegroundColor Green
    foreach ($room in $roomsResponse.content) {
        Write-Host "  - $($room.name) (ID: $($room.id))" -ForegroundColor Cyan
    }
} catch {
    Write-Host "채팅방 목록 조회 실패: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n7. 메시지 목록 조회 중..." -ForegroundColor Yellow
try {
    $messagesResponse = Invoke-RestMethod -Uri "$CHAT_SERVICE_URL/api/chat/chatrooms/$chatroomId/messages" -Method GET -Headers $headers
    Write-Host "메시지 목록 조회 성공: $($messagesResponse.content.Count)개 메시지" -ForegroundColor Green
    foreach ($message in $messagesResponse.content) {
        Write-Host "  - $($message.senderType): $($message.content)" -ForegroundColor Cyan
    }
} catch {
    Write-Host "메시지 목록 조회 실패: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n=== API 테스트 플로우 완료 ===" -ForegroundColor Green
Write-Host "모든 API 호출이 성공적으로 완료되었습니다!" -ForegroundColor Green
