# Route 53 도메인 설정 스크립트 (PowerShell)
# 사용법: .\setup-route53.ps1 [domain_name] [ec2_ip]
# 예시: .\setup-route53.ps1 dorandoran.com 3.21.177.186

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

# AWS CLI 설치 확인
function Test-AWSCLI {
    Write-Info "AWS CLI 설치 확인 중..."
    try {
        $awsVersion = aws --version 2>$null
        if ($LASTEXITCODE -eq 0) {
            Write-Success "AWS CLI 설치됨: $awsVersion"
            return $true
        } else {
            throw "AWS CLI가 설치되어 있지 않습니다."
        }
    } catch {
        Write-Error "AWS CLI가 설치되어 있지 않습니다."
        Write-Info "다음 명령어로 설치하세요:"
        Write-Info "winget install Amazon.AWSCLI"
        Write-Info "또는 https://aws.amazon.com/cli/ 에서 다운로드"
        return $false
    }
}

# AWS 자격 증명 확인
function Test-AWSCredentials {
    Write-Info "AWS 자격 증명 확인 중..."
    try {
        $identity = aws sts get-caller-identity 2>$null
        if ($LASTEXITCODE -eq 0) {
            $identityObj = $identity | ConvertFrom-Json
            Write-Success "AWS 자격 증명 확인됨"
            Write-Info "계정 ID: $($identityObj.Account)"
            Write-Info "사용자 ARN: $($identityObj.Arn)"
            return $true
        } else {
            throw "AWS 자격 증명이 설정되어 있지 않습니다."
        }
    } catch {
        Write-Error "AWS 자격 증명이 설정되어 있지 않습니다."
        Write-Info "다음 명령어로 설정하세요:"
        Write-Info "aws configure"
        return $false
    }
}

# Route 53 호스팅 영역 생성
function New-Route53HostedZone {
    param([string]$Domain)
    
    Write-Info "Route 53 호스팅 영역 생성 중: $Domain"
    
    # 기존 호스팅 영역 확인
    $existingZone = aws route53 list-hosted-zones --query "HostedZones[?Name=='$Domain.']" --output json 2>$null
    $existingZoneObj = $existingZone | ConvertFrom-Json
    
    if ($existingZoneObj.Count -gt 0) {
        Write-Warning "호스팅 영역이 이미 존재합니다: $Domain"
        $zoneId = $existingZoneObj[0].Id -replace "/hostedzone/", ""
        Write-Info "기존 호스팅 영역 ID: $zoneId"
        return $zoneId
    }
    
    # 새 호스팅 영역 생성
    $createZoneCommand = "aws route53 create-hosted-zone --name $Domain --caller-reference $(Get-Date -Format 'yyyyMMddHHmmss')"
    $zoneResult = Invoke-Expression $createZoneCommand 2>$null
    
    if ($LASTEXITCODE -eq 0) {
        $zoneObj = $zoneResult | ConvertFrom-Json
        $zoneId = $zoneObj.HostedZone.Id -replace "/hostedzone/", ""
        Write-Success "호스팅 영역 생성 완료: $zoneId"
        Write-Info "네임서버 정보:"
        $zoneObj.DelegationSet.NameServers | ForEach-Object { Write-Info "  - $_" }
        return $zoneId
    } else {
        Write-Error "호스팅 영역 생성 실패"
        return $null
    }
}

# A 레코드 생성
function New-ARecord {
    param([string]$ZoneId, [string]$Domain, [string]$IP)
    
    Write-Info "A 레코드 생성 중: $Domain -> $IP"
    
    $changeBatch = @{
        Changes = @(
            @{
                Action = "CREATE"
                ResourceRecordSet = @{
                    Name = $Domain
                    Type = "A"
                    TTL = 300
                    ResourceRecords = @(
                        @{
                            Value = $IP
                        }
                    )
                }
            }
        )
    } | ConvertTo-Json -Depth 10
    
    $changeResult = aws route53 change-resource-record-sets --hosted-zone-id $ZoneId --change-batch $changeBatch 2>$null
    
    if ($LASTEXITCODE -eq 0) {
        Write-Success "A 레코드 생성 완료"
        return $true
    } else {
        Write-Error "A 레코드 생성 실패"
        return $false
    }
}

# CNAME 레코드 생성 (www 서브도메인)
function New-CNAMERecord {
    param([string]$ZoneId, [string]$Domain)
    
    Write-Info "CNAME 레코드 생성 중: www.$Domain -> $Domain"
    
    $changeBatch = @{
        Changes = @(
            @{
                Action = "CREATE"
                ResourceRecordSet = @{
                    Name = "www.$Domain"
                    Type = "CNAME"
                    TTL = 300
                    ResourceRecords = @(
                        @{
                            Value = $Domain
                        }
                    )
                }
            }
        )
    } | ConvertTo-Json -Depth 10
    
    $changeResult = aws route53 change-resource-record-sets --hosted-zone-id $ZoneId --change-batch $changeBatch 2>$null
    
    if ($LASTEXITCODE -eq 0) {
        Write-Success "CNAME 레코드 생성 완료"
        return $true
    } else {
        Write-Error "CNAME 레코드 생성 실패"
        return $false
    }
}

# 서브도메인 A 레코드 생성
function New-SubdomainARecords {
    param([string]$ZoneId, [string]$Domain, [string]$IP)
    
    $subdomains = @("api", "auth", "user", "chat", "batch")
    
    foreach ($subdomain in $subdomains) {
        Write-Info "서브도메인 A 레코드 생성 중: $subdomain.$Domain -> $IP"
        
        $changeBatch = @{
            Changes = @(
                @{
                    Action = "CREATE"
                    ResourceRecordSet = @{
                        Name = "$subdomain.$Domain"
                        Type = "A"
                        TTL = 300
                        ResourceRecords = @(
                            @{
                                Value = $IP
                            }
                        )
                    }
                }
            )
        } | ConvertTo-Json -Depth 10
        
        $changeResult = aws route53 change-resource-record-sets --hosted-zone-id $ZoneId --change-batch $changeBatch 2>$null
        
        if ($LASTEXITCODE -eq 0) {
            Write-Success "$subdomain.$Domain A 레코드 생성 완료"
        } else {
            Write-Warning "$subdomain.$Domain A 레코드 생성 실패"
        }
    }
}

# SSL 인증서 요청 (ACM)
function Request-SSLCertificate {
    param([string]$Domain)
    
    Write-Info "SSL 인증서 요청 중: $Domain"
    
    # 기존 인증서 확인
    $existingCerts = aws acm list-certificates --query "CertificateSummaryList[?DomainName=='$Domain']" --output json 2>$null
    $existingCertsObj = $existingCerts | ConvertFrom-Json
    
    if ($existingCertsObj.Count -gt 0) {
        Write-Warning "SSL 인증서가 이미 존재합니다: $Domain"
        return $existingCertsObj[0].CertificateArn
    }
    
    # 새 인증서 요청
    $certResult = aws acm request-certificate --domain-name $Domain --subject-alternative-names "www.$Domain" --validation-method DNS 2>$null
    
    if ($LASTEXITCODE -eq 0) {
        $certObj = $certResult | ConvertFrom-Json
        Write-Success "SSL 인증서 요청 완료: $($certObj.CertificateArn)"
        Write-Info "DNS 검증을 위해 도메인 소유권을 확인해야 합니다."
        return $certObj.CertificateArn
    } else {
        Write-Error "SSL 인증서 요청 실패"
        return $null
    }
}

# 메인 실행 함수
function Main {
    Write-Info "Route 53 도메인 설정 시작"
    Write-Info "도메인: $DomainName"
    Write-Info "EC2 IP: $EC2IP"
    
    # AWS CLI 확인
    if (-not (Test-AWSCLI)) {
        exit 1
    }
    
    # AWS 자격 증명 확인
    if (-not (Test-AWSCredentials)) {
        exit 1
    }
    
    # Route 53 호스팅 영역 생성
    $zoneId = New-Route53HostedZone $DomainName
    if (-not $zoneId) {
        Write-Error "호스팅 영역 생성 실패"
        exit 1
    }
    
    # A 레코드 생성 (루트 도메인)
    if (-not (New-ARecord $zoneId $DomainName $EC2IP)) {
        Write-Error "A 레코드 생성 실패"
        exit 1
    }
    
    # CNAME 레코드 생성 (www 서브도메인)
    New-CNAMERecord $zoneId $DomainName
    
    # 서브도메인 A 레코드 생성
    New-SubdomainARecords $zoneId $DomainName $EC2IP
    
    # SSL 인증서 요청
    $certArn = Request-SSLCertificate $DomainName
    
    Write-Success "Route 53 설정 완료!"
    Write-Info ""
    Write-Info "=== 도메인 설정 정보 ==="
    Write-Info "호스팅 영역 ID: $zoneId"
    Write-Info "루트 도메인: $DomainName -> $EC2IP"
    Write-Info "www 도메인: www.$DomainName -> $DomainName"
    Write-Info "API 도메인: api.$DomainName -> $EC2IP:8080"
    Write-Info "Auth 도메인: auth.$DomainName -> $EC2IP:8081"
    Write-Info "User 도메인: user.$DomainName -> $EC2IP:8082"
    Write-Info "Chat 도메인: chat.$DomainName -> $EC2IP:8083"
    Write-Info "Batch 도메인: batch.$DomainName -> $EC2IP:8085"
    
    if ($certArn) {
        Write-Info "SSL 인증서: $certArn"
    }
    
    Write-Info ""
    Write-Warning "중요: 도메인 등록업체에서 네임서버를 AWS Route 53으로 변경해야 합니다."
    Write-Info "DNS 전파에는 최대 48시간이 소요될 수 있습니다."
}

# 스크립트 실행
Main
