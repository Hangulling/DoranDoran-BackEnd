# Firebase 서비스 계정 키(base64) 적용 후 User 서비스 배포
# 사용법: .\apply-firebase-and-deploy-user.ps1 -JsonPath "C:\Users\KDH\Downloads\dorandoran-firebase-firebase-adminsdk-fbsvc-f706102d91.json"

param(
    [Parameter(Mandatory=$true)]
    [string]$JsonPath,
    [string]$EC2_IP = "3.21.177.186",
    [string]$KEY_PATH = "$env:USERPROFILE\Downloads\dorandoran-key.pem"
)

$ErrorActionPreference = "Stop"

# 1. JSON -> Base64
Write-Host "Encoding Firebase key to Base64..." -ForegroundColor Cyan
$bytes = [System.IO.File]::ReadAllBytes($JsonPath)
$base64 = [Convert]::ToBase64String($bytes)

# 2. 임시 파일에 저장
$b64File = [System.IO.Path]::GetTempFileName()
[System.IO.File]::WriteAllText($b64File, $base64)

try {
    # 3. 서버에 base64 파일 업로드
    Write-Host "Uploading base64 to server..." -ForegroundColor Cyan
    scp -i $KEY_PATH $b64File "ec2-user@${EC2_IP}:/home/ec2-user/firebase-b64-tmp.txt"

    # 4. 서버에서 env 파일 업데이트 (스크립트 파일로 전달)
    Write-Host "Updating dorandoran-user.env on server..." -ForegroundColor Cyan
    $shScript = @"
ENV_FILE="/home/ec2-user/dorandoran-user.env"
B64_FILE="/home/ec2-user/firebase-b64-tmp.txt"
[ -f "`$ENV_FILE" ] && grep -v '^FIREBASE_ADMIN_JSON' "`$ENV_FILE" > "`${ENV_FILE}.new" || touch "`${ENV_FILE}.new"
echo "FIREBASE_ADMIN_JSON_BASE64=`$(cat `$B64_FILE)" >> "`${ENV_FILE}.new"
mv "`${ENV_FILE}.new" "`$ENV_FILE"
rm -f "`$B64_FILE"
echo "Env updated."
"@
    $shPath = [System.IO.Path]::GetTempFileName() + ".sh"
    $unixScript = $shScript -replace "`r`n", "`n"
    [System.IO.File]::WriteAllText($shPath, $unixScript, [System.Text.UTF8Encoding]::new($false))
    try {
        scp -i $KEY_PATH $shPath "ec2-user@${EC2_IP}:/home/ec2-user/update-firebase-env.sh"
        ssh -i $KEY_PATH "ec2-user@$EC2_IP" "chmod +x /home/ec2-user/update-firebase-env.sh && /home/ec2-user/update-firebase-env.sh && rm -f /home/ec2-user/update-firebase-env.sh"
    } finally {
        Remove-Item -Force $shPath -ErrorAction SilentlyContinue
    }

    # 5. User 서비스 배포
    Write-Host "Deploying User service..." -ForegroundColor Cyan
    $scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
    & "$scriptDir\deploy-user-service.ps1" -EC2_IP $EC2_IP -KEY_PATH $KEY_PATH
} finally {
    Remove-Item -Force $b64File -ErrorAction SilentlyContinue
}
