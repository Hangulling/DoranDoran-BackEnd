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
# 4. 서버에서 배포
#    - 단일 소스: /home/ec2-user/dorandoran-firebase.json
#    - 배포 전 검증: JSON/private_key 형식 체크
#    - auth env 동기화: FIREBASE_* 및 FIREBASE_ADMIN_JSON_BASE64 갱신
# -----------------------------------------------------------------------------
Write-Host "[4/4] 서버에서 컨테이너 배포 및 Firebase env 검증/동기화 중..." -ForegroundColor Yellow
$remoteScript = @'
set -euo pipefail

ENV_FILE="/home/ec2-user/dorandoran-auth.env"
JSON_FILE="/home/ec2-user/dorandoran-firebase.json"
TAR_FILE="/home/ec2-user/dorandoran-auth-latest.tar"

if [ ! -f "$JSON_FILE" ]; then
  echo "[ERROR] Firebase JSON 파일이 없습니다: $JSON_FILE" >&2
  exit 1
fi
if [ ! -f "$ENV_FILE" ]; then
  echo "[ERROR] Auth env 파일이 없습니다: $ENV_FILE" >&2
  exit 1
fi

python3 - <<'PY'
import base64
import json
from pathlib import Path

env_path = Path("/home/ec2-user/dorandoran-auth.env")
json_path = Path("/home/ec2-user/dorandoran-firebase.json")

raw = json_path.read_text(encoding="utf-8")
data = json.loads(raw)

project_id = (data.get("project_id") or "").strip()
client_email = (data.get("client_email") or "").strip()
private_key = (data.get("private_key") or "").strip()

if not project_id or not client_email or not private_key:
    raise SystemExit("[ERROR] Firebase JSON에 project_id/client_email/private_key가 모두 필요합니다.")

if "-----BEGIN PRIVATE KEY-----" not in private_key or "-----END PRIVATE KEY-----" not in private_key:
    raise SystemExit("[ERROR] Firebase private_key PEM 형식이 올바르지 않습니다.")

private_key_env = private_key.replace("\r\n", "\n").replace("\r", "\n").replace("\n", "\\n")
admin_json_base64 = base64.b64encode(raw.encode("utf-8")).decode("ascii")

lines = env_path.read_text(encoding="utf-8").splitlines()
drop_prefixes = (
    "FIREBASE_PROJECT_ID=",
    "FIREBASE_CLIENT_EMAIL=",
    "FIREBASE_PRIVATE_KEY=",
    "FIREBASE_ADMIN_JSON_BASE64=",
)
kept = [ln for ln in lines if not ln.startswith(drop_prefixes)]
kept.extend([
    f"FIREBASE_PROJECT_ID={project_id}",
    f"FIREBASE_CLIENT_EMAIL={client_email}",
    f"FIREBASE_PRIVATE_KEY={private_key_env}",
    f"FIREBASE_ADMIN_JSON_BASE64={admin_json_base64}",
])

tmp_path = env_path.with_suffix(".env.new")
tmp_path.write_text("\n".join(kept) + "\n", encoding="utf-8")
tmp_path.replace(env_path)

print("[OK] Firebase env 동기화 완료")
print(f"[OK] project_id={project_id}")
print(f"[OK] client_email={client_email}")
print(f"[OK] private_key_chars={len(private_key)}")
print(f"[OK] admin_json_base64_chars={len(admin_json_base64)}")
PY

chmod 600 "$ENV_FILE"

docker load -i "$TAR_FILE"
docker stop dorandoran-auth 2>/dev/null || true
docker rm dorandoran-auth 2>/dev/null || true
docker run -d --name dorandoran-auth --network dorandoran-network -p 8081:8081 --restart=unless-stopped --env-file "$ENV_FILE" dorandoran-auth:latest
'@

($remoteScript -replace "`r","") | ssh -i $PemPath $ServerHost "tr -d '\r' | bash -s"
if ($LASTEXITCODE -ne 0) {
    Write-Host "서버 배포 실패." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "=== Auth 서비스 배포 완료 ===" -ForegroundColor Green
Write-Host "  포트 8081, 컨테이너 이름: dorandoran-auth" -ForegroundColor Gray
Write-Host ""
