# Grafana 접속 문제 해결 가이드

## 문제 상황
- Grafana URL: http://3.21.177.186:3000
- 외부에서 접속 불가

## 진단 결과

### ✅ 정상 동작 확인
1. **컨테이너 상태**: 정상 실행 중
   ```bash
   docker ps | grep grafana
   # dorandoran-grafana 컨테이너 실행 중
   ```

2. **포트 리스닝**: 정상
   ```bash
   ss -tlnp | grep 3000
   # 0.0.0.0:3000에서 리스닝 중
   ```

3. **서버 내부 접속**: 정상
   ```bash
   curl http://localhost:3000
   # HTTP 302 응답 (로그인 페이지로 리다이렉트)
   ```

### ❌ 문제 원인
**AWS 보안 그룹에서 포트 3000이 열려있지 않음**

## 해결 방법

### 방법 1: AWS 콘솔에서 보안 그룹 설정 (권장)

1. **AWS 콘솔 접속**
   - EC2 서비스 → 인스턴스 선택
   - 인스턴스 ID: 확인 필요

2. **보안 그룹 편집**
   - 인스턴스 → 보안 탭 → 보안 그룹 클릭
   - 인바운드 규칙 편집

3. **규칙 추가**
   ```
   유형: 사용자 지정 TCP
   포트 범위: 3000
   소스: 0.0.0.0/0 (또는 특정 IP)
   설명: Grafana
   ```

4. **규칙 저장**

### 방법 2: AWS CLI 사용

```bash
# 보안 그룹 ID 확인
aws ec2 describe-instances --instance-ids <INSTANCE_ID> --query 'Reservations[0].Instances[0].SecurityGroups[*].GroupId'

# 인바운드 규칙 추가
aws ec2 authorize-security-group-ingress \
  --group-id <SECURITY_GROUP_ID> \
  --protocol tcp \
  --port 3000 \
  --cidr 0.0.0.0/0
```

### 방법 3: 특정 IP만 허용 (보안 강화)

```bash
# 특정 IP만 허용
aws ec2 authorize-security-group-ingress \
  --group-id <SECURITY_GROUP_ID> \
  --protocol tcp \
  --port 3000 \
  --cidr <YOUR_IP>/32
```

## 접속 확인

보안 그룹 설정 후:
1. 브라우저에서 http://3.21.177.186:3000 접속
2. 로그인 화면이 나타나면 성공
3. 계정: `admin` / 비밀번호: `admin123`

## 추가 보안 권장사항

### 1. SSH 터널링 사용 (권장)
외부에 직접 노출하지 않고 SSH 터널링 사용:

```bash
# 로컬에서 실행
ssh -i "$env:USERPROFILE\Downloads\dorandoran-key.pem" -L 3000:localhost:3000 ec2-user@3.21.177.186

# 브라우저에서 http://localhost:3000 접속
```

### 2. Nginx 리버스 프록시 설정
- HTTPS 적용
- 인증 추가
- 포트 443만 외부에 노출

### 3. VPN 사용
- 사내 VPN을 통해서만 접속 가능하도록 설정

## 현재 보안 그룹 확인 방법

```bash
# 인스턴스의 보안 그룹 확인
aws ec2 describe-instances \
  --filters "Name=ip-address,Values=3.21.177.186" \
  --query 'Reservations[0].Instances[0].SecurityGroups[*].[GroupId,GroupName]' \
  --output table

# 보안 그룹의 인바운드 규칙 확인
aws ec2 describe-security-groups \
  --group-ids <SECURITY_GROUP_ID> \
  --query 'SecurityGroups[0].IpPermissions[*].[IpProtocol,FromPort,ToPort,IpRanges[*].CidrIp]' \
  --output table
```

## 참고: 다른 서비스 포트

| 서비스 | 포트 | 보안 그룹 설정 필요 |
|--------|------|-------------------|
| Prometheus | 9090 | 예 |
| AlertManager | 9093 | 예 |
| Grafana | 3000 | 예 (현재 문제) |

모든 모니터링 서비스 포트를 외부에 노출하려면 각각 보안 그룹 규칙을 추가해야 합니다.

