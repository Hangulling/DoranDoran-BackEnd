# Nginx 리버스 프록시 설정 스크립트 (PowerShell)
# 사용법: .\setup-nginx.ps1 [domain_name] [ec2_ip]
# 예시: .\setup-nginx.ps1 dorandoran.com 3.21.177.186

param(
    [Parameter(Mandatory=$true)]
    [string]$DomainName,
    
    [Parameter(Mandatory=$true)]
    [string]$EC2IP
)

# 색상 정의
$Red = "Red"
$Green = "Green"
$Yellow = "Yellow"
$Blue = "Cyan"

# 로그 함수
function Write-Info {
    param([string]$Message)
    Write-Host "[INFO] $Message" -ForegroundColor $Blue
}

function Write-Success {
    param([string]$Message)
    Write-Host "[SUCCESS] $Message" -ForegroundColor $Green
}

function Write-Warning {
    param([string]$Message)
    Write-Host "[WARNING] $Message" -ForegroundColor $Yellow
}

function Write-Error {
    param([string]$Message)
    Write-Host "[ERROR] $Message" -ForegroundColor $Red
}

# SSH 연결 확인
function Test-SSHConnection {
    Write-Info "SSH 연결 확인 중..."
    try {
        $result = ssh -i "$env:USERPROFILE\Downloads\dorandoran-key.pem" -o ConnectTimeout=10 -o BatchMode=yes "ec2-user@$EC2IP" exit 2>$null
        if ($LASTEXITCODE -eq 0) {
            Write-Success "SSH 연결 성공"
        } else {
            throw "SSH 연결 실패"
        }
    } catch {
        Write-Error "SSH 연결 실패. 키 파일과 EC2 인스턴스를 확인하세요."
        exit 1
    }
}

# Nginx 설치 및 설정
function Install-Nginx {
    Write-Info "Nginx 설치 및 설정 중..."
    
    $nginxSetupCommand = @"
# Nginx 설치
sudo yum update -y
sudo yum install -y nginx

# Nginx 서비스 시작 및 자동 시작 설정
sudo systemctl start nginx
sudo systemctl enable nginx

# 방화벽 설정 (HTTP, HTTPS 포트 열기)
sudo firewall-cmd --permanent --add-service=http
sudo firewall-cmd --permanent --add-service=https
sudo firewall-cmd --reload

echo "Nginx 설치 완료"
"@
    
    ssh -i "$env:USERPROFILE\Downloads\dorandoran-key.pem" "ec2-user@$EC2IP" $nginxSetupCommand
    
    if ($LASTEXITCODE -eq 0) {
        Write-Success "Nginx 설치 완료"
    } else {
        Write-Error "Nginx 설치 실패"
        exit 1
    }
}

# Nginx 설정 파일 생성
function New-NginxConfig {
    param([string]$Domain, [string]$IP)
    
    Write-Info "Nginx 설정 파일 생성 중..."
    
    $nginxConfig = @"
# API Gateway (루트 도메인)
server {
    listen 80;
    server_name $Domain www.$Domain;
    
    location / {
        proxy_pass http://$IP`:8080;
        proxy_set_header Host `$host;
        proxy_set_header X-Real-IP `$remote_addr;
        proxy_set_header X-Forwarded-For `$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto `$scheme;
    }
}

# Auth Service
server {
    listen 80;
    server_name auth.$Domain;
    
    location / {
        proxy_pass http://$IP`:8081;
        proxy_set_header Host `$host;
        proxy_set_header X-Real-IP `$remote_addr;
        proxy_set_header X-Forwarded-For `$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto `$scheme;
    }
}

# User Service
server {
    listen 80;
    server_name user.$Domain;
    
    location / {
        proxy_pass http://$IP`:8082;
        proxy_set_header Host `$host;
        proxy_set_header X-Real-IP `$remote_addr;
        proxy_set_header X-Forwarded-For `$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto `$scheme;
    }
}

# Chat Service
server {
    listen 80;
    server_name chat.$Domain;
    
    location / {
        proxy_pass http://$IP`:8083;
        proxy_set_header Host `$host;
        proxy_set_header X-Real-IP `$remote_addr;
        proxy_set_header X-Forwarded-For `$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto `$scheme;
    }
    
    # WebSocket 지원
    location /ws {
        proxy_pass http://$IP`:8083;
        proxy_http_version 1.1;
        proxy_set_header Upgrade `$http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host `$host;
        proxy_set_header X-Real-IP `$remote_addr;
        proxy_set_header X-Forwarded-For `$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto `$scheme;
    }
}

# Batch Service
server {
    listen 80;
    server_name batch.$Domain;
    
    location / {
        proxy_pass http://$IP`:8085;
        proxy_set_header Host `$host;
        proxy_set_header X-Real-IP `$remote_addr;
        proxy_set_header X-Forwarded-For `$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto `$scheme;
    }
}
"@
    
    # 임시 파일로 설정 저장
    $tempConfigFile = "$env:TEMP\nginx-dorandoran.conf"
    $nginxConfig | Out-File -FilePath $tempConfigFile -Encoding UTF8
    
    # EC2로 설정 파일 전송
    scp -i "$env:USERPROFILE\Downloads\dorandoran-key.pem" $tempConfigFile "ec2-user@$EC2IP`:/tmp/nginx-dorandoran.conf"
    
    # EC2에서 설정 파일 적용
    $applyConfigCommand = @"
# 기존 설정 백업
sudo cp /etc/nginx/nginx.conf /etc/nginx/nginx.conf.backup

# 새 설정 파일 적용
sudo cp /tmp/nginx-dorandoran.conf /etc/nginx/conf.d/dorandoran.conf

# Nginx 설정 테스트
sudo nginx -t

if [ `$? -eq 0 ]; then
    # 설정이 올바르면 Nginx 재시작
    sudo systemctl reload nginx
    echo "Nginx 설정 적용 완료"
else
    echo "Nginx 설정 오류"
    exit 1
fi

# 임시 파일 삭제
rm /tmp/nginx-dorandoran.conf
"@
    
    ssh -i "$env:USERPROFILE\Downloads\dorandoran-key.pem" "ec2-user@$EC2IP" $applyConfigCommand
    
    # 로컬 임시 파일 삭제
    Remove-Item $tempConfigFile -Force -ErrorAction SilentlyContinue
    
    if ($LASTEXITCODE -eq 0) {
        Write-Success "Nginx 설정 적용 완료"
    } else {
        Write-Error "Nginx 설정 적용 실패"
        exit 1
    }
}

# SSL 인증서 설정 (Let's Encrypt)
function Setup-SSLCertificate {
    param([string]$Domain)
    
    Write-Info "SSL 인증서 설정 중 (Let's Encrypt)..."
    
    $sslSetupCommand = @"
# Certbot 설치
sudo yum install -y certbot python3-certbot-nginx

# SSL 인증서 발급
sudo certbot --nginx -d $Domain -d www.$Domain -d api.$Domain -d auth.$Domain -d user.$Domain -d chat.$Domain -d batch.$Domain --non-interactive --agree-tos --email admin@$Domain

# 자동 갱신 설정
echo "0 12 * * * /usr/bin/certbot renew --quiet" | sudo crontab -

echo "SSL 인증서 설정 완료"
"@
    
    ssh -i "$env:USERPROFILE\Downloads\dorandoran-key.pem" "ec2-user@$EC2IP" $sslSetupCommand
    
    if ($LASTEXITCODE -eq 0) {
        Write-Success "SSL 인증서 설정 완료"
    } else {
        Write-Warning "SSL 인증서 설정 실패 (수동 설정 필요)"
    }
}

# 메인 실행 함수
function Main {
    Write-Info "Nginx 리버스 프록시 설정 시작"
    Write-Info "도메인: $DomainName"
    Write-Info "EC2 IP: $EC2IP"
    
    # SSH 연결 확인
    Test-SSHConnection
    
    # Nginx 설치
    Install-Nginx
    
    # Nginx 설정
    New-NginxConfig $DomainName $EC2IP
    
    # SSL 인증서 설정 (선택사항)
    Write-Info "SSL 인증서를 설정하시겠습니까? (y/n)"
    $setupSSL = Read-Host
    if ($setupSSL -eq "y" -or $setupSSL -eq "Y") {
        Setup-SSLCertificate $DomainName
    }
    
    Write-Success "Nginx 설정 완료!"
    Write-Info ""
    Write-Info "=== 서비스 접속 정보 ==="
    Write-Info "- 메인 사이트: http://$DomainName"
    Write-Info "- www 사이트: http://www.$DomainName"
    Write-Info "- API Gateway: http://api.$DomainName"
    Write-Info "- Auth Service: http://auth.$DomainName"
    Write-Info "- User Service: http://user.$DomainName"
    Write-Info "- Chat Service: http://chat.$DomainName"
    Write-Info "- Batch Service: http://batch.$DomainName"
    Write-Info ""
    Write-Info "SSL 인증서가 설정되었다면 https:// 도메인으로 접속 가능합니다."
}

# 스크립트 실행
Main
