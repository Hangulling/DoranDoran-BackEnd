# 보안 그룹 개선 가이드

**작성일**: 2025년 11월 11일  
**보안 그룹 ID**: sg-0000503a7e257077f  
**보안 그룹 이름**: dorandoran-ec2-sg  
**VPC ID**: vpc-0f1aac567bb6a1968  
**위협 IP**: 172.104.24.172

---

## 1. 현재 보안 그룹 규칙 분석

### 1.1 인바운드 규칙 현황 (14개)

| 규칙 ID | 유형 | 프로토콜 | 포트 | 소스 | 위험도 |
|---------|------|----------|------|------|--------|
| sgr-05470539de4707a6b | SSH | TCP | 22 | 0.0.0.0/0 | 🔴 **매우 높음** |
| sgr-09f638c3ace2c9228 | SSH | TCP | 22 | 117.111.17.85/32 | 🟢 낮음 |
| sgr-0cf0f7d7d8bf8a3a7 | SSH | TCP | 22 | 14.33.20.25/32 | 🟢 낮음 |
| sgr-0440b271adc52efd4 | SSH | TCP | 22 | 192.168.1.184/32 | 🟢 낮음 |
| sgr-026cc226518d81e96 | SSH | TCP | 22 | 175.121.171.130/32 | 🟢 낮음 |
| sgr-0590165f2ec20151c | SSH | TCP | 22 | 1.225.233.113/32 | 🟢 낮음 |
| sgr-0e152936d2c2016cd | SSH | TCP | 22 | 117.111.17.244/32 | 🟢 낮음 |
| sgr-050b30f90460e260b | HTTP | TCP | 80 | 0.0.0.0/0 | 🟡 중간 |
| sgr-09ff0dc92881da3d9 | HTTPS | TCP | 443 | 0.0.0.0/0 | 🟡 중간 |
| sgr-05c437abc992d6333 | Custom TCP | TCP | 8080 | 0.0.0.0/0 | 🟡 중간 |
| sgr-0874582ae880b3ef4 | Custom TCP | TCP | 8081 | 0.0.0.0.0/0 | 🟡 중간 |
| sgr-0afe27a8893e385ea | Custom TCP | TCP | 8082 | 0.0.0.0/0 | 🟡 중간 |
| sgr-0920df8aebb9cf871 | Custom TCP | TCP | 8083 | 0.0.0.0/0 | 🟡 중간 |
| sgr-0fe6920110601d7fd | Custom TCP | TCP | 8085 | 0.0.0.0/0 | 🟡 중간 |

### 1.2 발견된 보안 문제점

#### 🔴 심각한 문제
1. **SSH 포트 22가 0.0.0.0/0으로 열려있음**
   - 전 세계 어디서나 SSH 접속 시도 가능
   - 무차별 대입 공격(Brute Force) 위험
   - 규칙 ID: `sgr-05470539de4707a6b`

#### 🟡 개선 필요 사항
2. **모든 애플리케이션 포트가 0.0.0.0/0으로 열려있음**
   - 8080, 8081, 8082, 8083, 8085 포트 모두 공개
   - DDoS 공격 및 스캔 공격에 취약
   - 불필요한 포트 노출

3. **특정 위협 IP 차단 불가**
   - AWS Security Group은 Deny 규칙을 지원하지 않음
   - 172.104.24.172 IP를 직접 차단할 수 없음

---

## 2. 즉시 조치 사항

### 2.1 위협 IP 차단 방법

AWS Security Group은 Deny 규칙을 지원하지 않으므로, 다음 방법 중 하나를 사용해야 합니다:

#### 방법 1: Network ACL을 사용한 차단 (권장)

Network ACL은 Deny 규칙을 지원하므로, 특정 IP를 차단할 수 있습니다.

```powershell
# VPC의 Network ACL ID 확인
aws ec2 describe-network-acls `
    --filters "Name=vpc-id,Values=vpc-0f1aac567bb6a1968" `
    --region ap-northeast-2 `
    --query 'NetworkAcls[0].NetworkAclId' `
    --output text

# Network ACL에 Deny 규칙 추가
aws ec2 create-network-acl-entry `
    --network-acl-id <NETWORK-ACL-ID> `
    --rule-number 100 `
    --protocol -1 `
    --cidr-block 172.104.24.172/32 `
    --egress false `
    --rule-action deny `
    --region ap-northeast-2
```

#### 방법 2: 기존 스크립트 개선

현재 `block-ip-aws.ps1` 스크립트를 개선하여 Network ACL 차단 기능을 추가합니다.

#### 방법 3: 애플리케이션 레벨 차단

Gateway 서비스에서 IP 블랙리스트를 관리하여 차단합니다.

---

### 2.2 SSH 포트 보안 강화 (최우선)

**즉시 조치 필요**: SSH 포트 22의 0.0.0.0/0 규칙을 제거하세요.

```powershell
# 위험한 SSH 규칙 제거
aws ec2 revoke-security-group-ingress `
    --group-id sg-0000503a7e257077f `
    --ip-permissions '[{
        "IpProtocol": "tcp",
        "FromPort": 22,
        "ToPort": 22,
        "IpRanges": [{"CidrIp": "0.0.0.0/0"}]
    }]' `
    --region ap-northeast-2
```

**주의**: 이 규칙을 제거하기 전에 현재 접속 중인 SSH 세션이 있는지 확인하세요. 규칙 제거 후에는 특정 IP에서만 SSH 접속이 가능합니다.

---

## 3. 권장 보안 그룹 규칙 구성

### 3.1 최소 권한 원칙 적용

다음과 같이 보안 그룹 규칙을 재구성하는 것을 권장합니다:

#### SSH (포트 22)
- ✅ **허용**: 신뢰할 수 있는 특정 IP만 허용
- ❌ **차단**: 0.0.0.0/0 규칙 제거
- 📝 **권장 IP 목록**:
  - 117.111.17.85/32 (개발자 1)
  - 14.33.20.25/32 (개발자 2)
  - 192.168.1.184/32 (사무실)
  - 175.121.171.130/32 (개발자 3)
  - 1.225.233.113/32 (개발자 4)
  - 117.111.17.244/32 (개발자 5)

#### HTTP/HTTPS (포트 80, 443)
- ✅ **허용**: 0.0.0.0/0 (공개 웹 서비스이므로 필요)
- ⚠️ **보완**: WAF를 통한 추가 보안 강화 권장

#### 애플리케이션 포트 (8080-8085)
- 🟡 **옵션 1**: 0.0.0.0/0 유지 (현재 상태)
  - 공개 API 서비스인 경우
  - Gateway를 통한 라우팅만 허용하는 경우
- 🟢 **옵션 2**: 특정 IP 대역만 허용 (더 안전)
  - 내부 네트워크나 특정 클라이언트만 허용
  - 예: 회사 IP 대역, VPN IP 대역

---

## 4. 보안 그룹 규칙 수정 스크립트

### 4.1 SSH 포트 보안 강화 스크립트

```powershell
# scripts/security/secure-ssh-port.ps1
param(
    [Parameter(Mandatory=$false)]
    [string]$SecurityGroupId = "sg-0000503a7e257077f",
    
    [Parameter(Mandatory=$false)]
    [string]$Region = "ap-northeast-2",
    
    [Parameter(Mandatory=$false)]
    [switch]$DryRun
)

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "SSH 포트 보안 강화 스크립트" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

if ($DryRun) {
    Write-Host "[DRY RUN 모드] 실제 변경은 수행하지 않습니다." -ForegroundColor Magenta
    Write-Host ""
}

# 위험한 SSH 규칙 제거
Write-Host "위험한 SSH 규칙 (0.0.0.0/0) 제거 중..." -ForegroundColor Yellow

if ($DryRun) {
    Write-Host "[DRY RUN] 다음 규칙을 제거할 예정:" -ForegroundColor Magenta
    Write-Host "  - 규칙 ID: sgr-05470539de4707a6b" -ForegroundColor Magenta
    Write-Host "  - 포트: 22" -ForegroundColor Magenta
    Write-Host "  - 소스: 0.0.0.0/0" -ForegroundColor Magenta
} else {
    try {
        aws ec2 revoke-security-group-ingress `
            --group-id $SecurityGroupId `
            --ip-permissions '[{
                "IpProtocol": "tcp",
                "FromPort": 22,
                "ToPort": 22,
                "IpRanges": [{"CidrIp": "0.0.0.0/0"}]
            }]' `
            --region $Region
        
        Write-Host "✅ 위험한 SSH 규칙이 제거되었습니다." -ForegroundColor Green
    } catch {
        Write-Error "규칙 제거 실패: $_"
        exit 1
    }
}

Write-Host ""
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "작업 완료" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "⚠️  주의: 이제 특정 IP에서만 SSH 접속이 가능합니다." -ForegroundColor Yellow
```

### 4.2 Network ACL을 사용한 IP 차단 스크립트

```powershell
# scripts/security/block-ip-network-acl.ps1
param(
    [Parameter(Mandatory=$true)]
    [string]$IpAddress,
    
    [Parameter(Mandatory=$false)]
    [string]$VpcId = "vpc-0f1aac567bb6a1968",
    
    [Parameter(Mandatory=$false)]
    [string]$Region = "ap-northeast-2",
    
    [Parameter(Mandatory=$false)]
    [switch]$DryRun
)

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "Network ACL을 사용한 IP 차단 스크립트" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

# IP 주소 유효성 검사
$ipPattern = '^([0-9]{1,3}\.){3}[0-9]{1,3}$'
if ($IpAddress -notmatch $ipPattern) {
    Write-Error "유효하지 않은 IP 주소 형식입니다: $IpAddress"
    exit 1
}

Write-Host "대상 IP: $IpAddress" -ForegroundColor Yellow
Write-Host "VPC ID: $VpcId" -ForegroundColor Yellow
Write-Host "리전: $Region" -ForegroundColor Yellow
Write-Host ""

if ($DryRun) {
    Write-Host "[DRY RUN 모드] 실제 변경은 수행하지 않습니다." -ForegroundColor Magenta
    Write-Host ""
}

# Network ACL ID 조회
Write-Host "Network ACL ID 조회 중..." -ForegroundColor Green
try {
    $networkAclId = aws ec2 describe-network-acls `
        --filters "Name=vpc-id,Values=$VpcId" `
        --region $Region `
        --query 'NetworkAcls[0].NetworkAclId' `
        --output text
    
    if (-not $networkAclId -or $networkAclId -eq "None") {
        Write-Error "Network ACL을 찾을 수 없습니다."
        exit 1
    }
    
    Write-Host "Network ACL ID: $networkAclId" -ForegroundColor Green
    Write-Host ""
} catch {
    Write-Error "Network ACL 조회 실패: $_"
    exit 1
}

# Deny 규칙 추가
Write-Host "IP 차단 규칙 추가 중..." -ForegroundColor Green

if ($DryRun) {
    Write-Host "[DRY RUN] 다음 규칙을 추가할 예정:" -ForegroundColor Magenta
    Write-Host "  - Network ACL ID: $networkAclId" -ForegroundColor Magenta
    Write-Host "  - 규칙 번호: 100" -ForegroundColor Magenta
    Write-Host "  - 프로토콜: 모든 프로토콜 (-1)" -ForegroundColor Magenta
    Write-Host "  - CIDR: $IpAddress/32" -ForegroundColor Magenta
    Write-Host "  - 액션: deny" -ForegroundColor Magenta
} else {
    try {
        # 기존 규칙 확인 (규칙 번호 충돌 방지)
        $existingRules = aws ec2 describe-network-acls `
            --network-acl-ids $networkAclId `
            --region $Region `
            --query 'NetworkAcls[0].Entries[?RuleNumber==`100`]' `
            --output json | ConvertFrom-Json
        
        if ($existingRules) {
            Write-Warning "규칙 번호 100이 이미 존재합니다. 다른 번호를 사용합니다."
            $ruleNumber = 101
        } else {
            $ruleNumber = 100
        }
        
        aws ec2 create-network-acl-entry `
            --network-acl-id $networkAclId `
            --rule-number $ruleNumber `
            --protocol -1 `
            --cidr-block "$IpAddress/32" `
            --egress false `
            --rule-action deny `
            --region $Region
        
        Write-Host "✅ IP 차단 규칙이 추가되었습니다." -ForegroundColor Green
        Write-Host "   규칙 번호: $ruleNumber" -ForegroundColor Green
    } catch {
        Write-Error "규칙 추가 실패: $_"
        exit 1
    }
}

Write-Host ""
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "작업 완료" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
```

---

## 5. 단계별 실행 계획

### 단계 1: 즉시 조치 (오늘)

1. **위협 IP 차단**
   ```powershell
   .\scripts\security\block-ip-network-acl.ps1 -IpAddress "172.104.24.172"
   ```

2. **SSH 포트 보안 강화 (신중하게)**
   ```powershell
   # 먼저 Dry Run으로 확인
   .\scripts\security\secure-ssh-port.ps1 -DryRun
   
   # 문제없으면 실제 실행
   .\scripts\security\secure-ssh-port.ps1
   ```

### 단계 2: 단기 조치 (이번 주)

1. **보안 그룹 규칙 정리**
   - 불필요한 규칙 제거
   - 규칙 설명 추가
   - 태그 관리

2. **모니터링 강화**
   - CloudWatch 알람 설정
   - 보안 이벤트 로깅

### 단계 3: 장기 조치 (이번 달)

1. **WAF 도입 검토**
   - AWS WAF 또는 Cloudflare
   - 비정상 요청 자동 차단

2. **자동화된 보안 모니터링**
   - 의심스러운 IP 자동 탐지
   - 자동 차단 시스템 구축

---

## 6. 보안 체크리스트

### 현재 상태 점검

- [ ] SSH 포트 22가 0.0.0.0/0으로 열려있는지 확인
- [ ] 불필요한 포트가 열려있는지 확인
- [ ] 위협 IP 172.104.24.172 차단 여부 확인
- [ ] 보안 그룹 규칙에 설명이 있는지 확인
- [ ] 태그가 제대로 설정되어 있는지 확인

### 개선 사항

- [ ] SSH 포트를 특정 IP만 허용하도록 변경
- [ ] Network ACL에 위협 IP 차단 규칙 추가
- [ ] 보안 그룹 규칙에 설명 추가
- [ ] 정기적인 보안 그룹 규칙 검토 일정 수립
- [ ] 자동화된 보안 모니터링 시스템 구축

---

## 7. 참고 자료

- [AWS Security Group 문서](https://docs.aws.amazon.com/vpc/latest/userguide/security-groups.html)
- [Network ACL 문서](https://docs.aws.amazon.com/vpc/latest/userguide/vpc-network-acls.html)
- [보안 분석 보고서](./ip-172.104.24.172-security-analysis.md)
- [보안 대응 계획](./security-response-plan.md)

---

**작성자**: AI Assistant  
**작성일**: 2025년 11월 11일  
**다음 검토일**: 2025년 11월 18일

