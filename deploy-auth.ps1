# =============================================================================
# Auth 서비스 배포 스크립트 (igu 트리)
# 빌드 -> tar 저장 -> 서버 전송 -> 서버 배포
# 서버 전송 구간은 이미지 크기에 따라 5~10분 정도 걸릴 수 있습니다.
# =============================================================================

$ErrorActionPreference = "Stop"
$PemPath = "$env:USERPROFILE\Downloads\dorandoran-key.pem"
$ServerHost = "ec2-user@3.21.177.186"
$TarName = "dorandoran-auth-latest.tar"
$DistDir = "dist"
$TarPath = "$DistDir\$TarName"

# 현재 디렉터리를 기준으로 auth/Dockerfile 존재 여부만 확인
if (-not (Test-Path "auth\Dockerfile")) {
    Write-Host "오류: auth/Dockerfile 을 찾을 수 없습니다. igu 프로젝트 루트(Deploy 스크립트가 있는 위치)에서 실행하세요." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "=== Auth 서비스 배포 (빌드 -> 전송 -> 배포) ===" -ForegroundColor Green
Write-Host ""

# -----------------------------------------------------------------------------
# 1. Docker 이미지 빌드
# -----------------------------------------------------------------------------
Write-Host "[1/4] Docker 이미지 빌드 중..." -ForegroundColor Yellow
$buildStart = Get-Date
docker build -f auth/Dockerfile -t dorandoran-auth:latest .
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
docker save dorandoran-auth:latest -o $TarPath
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
# 4. 서버에서 배포 (환경변수는 서버의 dorandoran-auth.env 에서 로드)
# -----------------------------------------------------------------------------
Write-Host "[4/4] 서버에서 컨테이너 배포 중..." -ForegroundColor Yellow
$deployCmd = "docker load -i /home/ec2-user/dorandoran-auth-latest.tar && docker stop dorandoran-auth 2>/dev/null || true && docker rm dorandoran-auth 2>/dev/null || true && docker run -d --name dorandoran-auth --network dorandoran-network -p 8081:8081 --restart=unless-stopped --env-file /home/ec2-user/dorandoran-auth.env dorandoran-auth:latest"
ssh -i $PemPath $ServerHost $deployCmd
if ($LASTEXITCODE -ne 0) {
    Write-Host "서버 배포 실패." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "=== Auth 서비스 배포 완료 ===" -ForegroundColor Green
Write-Host "  포트 8081, 컨테이너 이름: dorandoran-auth" -ForegroundColor Gray
Write-Host ""
