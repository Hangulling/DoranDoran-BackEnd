# =============================================================================
# User 서비스 배포 스크립트 (igu 트리)
# 빌드 -> tar 저장 -> 서버 전송 -> 서버 배포
# 서버 전송 구간은 이미지 크기에 따라 5~10분 정도 걸릴 수 있습니다.
# =============================================================================

$ErrorActionPreference = "Stop"
$PemPath = "$env:USERPROFILE\Downloads\dorandoran-key.pem"
$ServerHost = "ec2-user@3.21.177.186"
$TarName = "dorandoran-user-latest.tar"
$DistDir = "dist"
$TarPath = "$DistDir\$TarName"

# 일일 관심 주제 배치 스케줄 Cron (임시: 10분마다 실행)
# Spring Cron 형식(6자리): 초 분 시 일 월 요일
# 기본: 0 0 9 * * *    (매일 09:00)
# 임시: 0 */10 * * * * (10분마다)
$NotificationCron = "0 */10 * * * *"

# 현재 디렉터리를 기준으로 user/Dockerfile 존재 여부만 확인
if (-not (Test-Path "user\Dockerfile")) {
    Write-Host "오류: user/Dockerfile 을 찾을 수 없습니다. igu 프로젝트 루트(Deploy 스크립트가 있는 위치)에서 실행하세요." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "=== User 서비스 배포 (빌드 -> 전송 -> 배포) ===" -ForegroundColor Green
Write-Host ""

# -----------------------------------------------------------------------------
# 1. Docker 이미지 빌드
# -----------------------------------------------------------------------------
Write-Host "[1/4] Docker 이미지 빌드 중..." -ForegroundColor Yellow
$buildStart = Get-Date
docker build -f user/Dockerfile -t dorandoran-user:latest .
if ($LASTEXITCODE -ne 0) {
    Write-Host "Docker 빌드 실패." -ForegroundColor Red
    exit 1
}
$buildElapsed = (Get-Date) - $buildStart
Write-Host "  빌드 완료 (소요: $([math]::Round($buildElapsed.TotalSeconds))초)" -ForegroundColor Gray
Write-Host ""

# -----------------------------------------------------------------------------
# 2. tar 파일로 저장
# -----------------------------------------------------------------------------
Write-Host "[2/4] 이미지를 tar 파일로 저장 중..." -ForegroundColor Yellow
if (!(Test-Path $DistDir)) {
    New-Item -ItemType Directory -Path $DistDir | Out-Null
}
docker save dorandoran-user:latest -o $TarPath
if ($LASTEXITCODE -ne 0) {
    Write-Host "tar 저장 실패." -ForegroundColor Red
    exit 1
}
$tarSizeMB = [math]::Round((Get-Item $TarPath).Length / 1MB, 1)
Write-Host "  저장 완료: $TarPath ($tarSizeMB MB)" -ForegroundColor Gray
Write-Host ""

# -----------------------------------------------------------------------------
# 3. 서버에 전송 (5~10분 소요 가능)
# -----------------------------------------------------------------------------
Write-Host "[3/4] 서버에 이미지 전송 중..." -ForegroundColor Yellow
Write-Host "  (이미지 크기에 따라 약 5~10분 걸릴 수 있습니다. 완료될 때까지 기다려 주세요.)" -ForegroundColor Cyan
$transferStart = Get-Date
scp -i $PemPath $TarPath "${ServerHost}:/home/ec2-user/"
if ($LASTEXITCODE -ne 0) {
    Write-Host "서버 전송 실패." -ForegroundColor Red
    exit 1
}
$transferElapsed = (Get-Date) - $transferStart
Write-Host "  전송 완료 (소요: $([math]::Round($transferElapsed.TotalSeconds))초)" -ForegroundColor Gray
Write-Host ""

# -----------------------------------------------------------------------------
# 4. 서버에서 배포
# -----------------------------------------------------------------------------
# Firebase: FIREBASE_ADMIN_JSON_BASE64 등은 서버의 dorandoran-user.env에만 두고,
# 여기서 cat/jq로 export 하지 않음 (값 깨짐 방지). env 변경 전 백업은 서버에서 수행.
#
# docker run 시 --env-file 로 SMTP·INSTAGRAM 토큰·Firebase 등을 읽고,
# 아래 -e 는 인프라/프로필 기본값을 명시적으로 고정(스니펫과 동일한 운영 값).
# -----------------------------------------------------------------------------
Write-Host "[4/4] 서버에서 컨테이너 배포 중..." -ForegroundColor Yellow
$deployCmd =
    "docker load -i /home/ec2-user/dorandoran-user-latest.tar && " +
    "docker stop dorandoran-user 2>/dev/null || true && " +
    "docker rm dorandoran-user 2>/dev/null || true && " +
    "docker run -d --name dorandoran-user --network dorandoran-network -p 8082:8082 --restart=unless-stopped " +
    "--env-file /home/ec2-user/dorandoran-user.env " +
    "-e SPRING_PROFILES_ACTIVE=docker " +
    "-e SPRING_DATASOURCE_URL=jdbc:postgresql://dorandoran-shared-db:5432/dorandoran " +
    "-e SPRING_DATASOURCE_USERNAME=doran " +
    "-e SPRING_DATASOURCE_PASSWORD=doran " +
    "-e SPRING_JPA_HIBERNATE_DEFAULT_SCHEMA=user_schema " +
    "-e SPRING_REDIS_HOST=dorandoran-redis " +
    "-e SPRING_REDIS_PORT=6379 " +
    "-e AUTH_SERVICE_URL=http://dorandoran-auth:8081 " +
    "-e CHAT_SERVICE_URL=http://dorandoran-chat:8083 " +
    "-e SPRING_FLYWAY_ENABLED=false " +
    "-e INSTAGRAM_ENABLED=true " +
    "-e APP_DEEPLINK_SCHEME=dorandoran://chat " +
    "-e APP_DEEPLINK_UNIVERSAL_BASE=https://www.doran-chat.com/chat " +
    "-e NOTIFICATION_DAILY_CRON='$NotificationCron' " +
    "dorandoran-user:latest"
ssh -i $PemPath $ServerHost $deployCmd
if ($LASTEXITCODE -ne 0) {
    Write-Host "서버 배포 실패." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "=== User 서비스 배포 완료 ===" -ForegroundColor Green
Write-Host "  포트 8082, 컨테이너 이름: dorandoran-user" -ForegroundColor Gray
Write-Host ""
