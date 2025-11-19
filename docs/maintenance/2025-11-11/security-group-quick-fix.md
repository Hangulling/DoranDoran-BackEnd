# 보안 그룹 빠른 수정 가이드

**작성일**: 2025년 11월 11일  
**위협 IP**: 172.104.24.172  
**보안 그룹 ID**: sg-0000503a7e257077f

---

## 🚨 즉시 조치 필요

### 1. 위협 IP 차단 (5분 소요)

```powershell
# Network ACL을 사용하여 IP 차단
.\scripts\security\block-ip-network-acl.ps1 -IpAddress "172.104.24.172"
```

**주의**: 먼저 Dry Run으로 확인하려면:
```powershell
.\scripts\security\block-ip-network-acl.ps1 -IpAddress "172.104.24.172" -DryRun
```

---

### 2. SSH 포트 보안 강화 (신중하게 진행)

**⚠️ 경고**: 이 작업을 수행하면 전 세계 어디서나 SSH 접속이 불가능해집니다. 특정 IP에서만 접속 가능합니다.

```powershell
# 먼저 Dry Run으로 확인
.\scripts\security\secure-ssh-port.ps1 -DryRun

# 문제없으면 실제 실행
.\scripts\security\secure-ssh-port.ps1
```

**현재 허용된 SSH IP**:
- 117.111.17.85/32
- 14.33.20.25/32
- 192.168.1.184/32
- 175.121.171.130/32
- 1.225.233.113/32
- 117.111.17.244/32

이 IP들에서만 SSH 접속이 가능합니다.

---

## 📋 현재 보안 그룹 문제점 요약

### 🔴 심각한 문제
- **SSH 포트 22가 0.0.0.0/0으로 열려있음**
  - 규칙 ID: `sgr-05470539de4707a6b`
  - 전 세계 어디서나 SSH 접속 시도 가능
  - 무차별 대입 공격 위험

### 🟡 개선 필요
- 모든 애플리케이션 포트가 0.0.0.0/0으로 열려있음
  - 8080, 8081, 8082, 8083, 8085 포트 모두 공개
  - DDoS 공격 및 스캔 공격에 취약

---

## ✅ 권장 조치 순서

1. **위협 IP 차단** (즉시)
   ```powershell
   .\scripts\security\block-ip-network-acl.ps1 -IpAddress "172.104.24.172"
   ```

2. **SSH 포트 보안 강화** (신중하게)
   ```powershell
   .\scripts\security\secure-ssh-port.ps1 -DryRun  # 먼저 확인
   .\scripts\security\secure-ssh-port.ps1           # 실제 실행
   ```

3. **보안 그룹 규칙 정리** (이번 주)
   - AWS 콘솔에서 불필요한 규칙 제거
   - 규칙에 설명 추가
   - 태그 관리

---

## 📚 상세 가이드

더 자세한 내용은 다음 문서를 참고하세요:
- [보안 그룹 개선 가이드](./security-group-improvement-guide.md)
- [IP 보안 분석 보고서](./ip-172.104.24.172-security-analysis.md)

---

## 🔍 확인 방법

### IP 차단 확인
```powershell
# Network ACL 규칙 확인
aws ec2 describe-network-acls `
    --filters "Name=vpc-id,Values=vpc-0f1aac567bb6a1968" `
    --region ap-northeast-2 `
    --query 'NetworkAcls[0].Entries[?RuleAction==`deny`]' `
    --output table
```

### SSH 규칙 확인
```powershell
# 보안 그룹 SSH 규칙 확인
aws ec2 describe-security-groups `
    --group-ids sg-0000503a7e257077f `
    --region ap-northeast-2 `
    --query 'SecurityGroups[0].IpPermissions[?FromPort==`22`]' `
    --output table
```

---

**작성자**: AI Assistant  
**작성일**: 2025년 11월 11일






