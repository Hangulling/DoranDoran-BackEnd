# DoranDoran 도메인 설정 가이드

## 📋 개요

이 가이드는 DoranDoran 프로젝트에 도메인을 설정하는 방법을 설명합니다. Route 53과 Nginx를 사용하여 전문적인 도메인 설정을 구성합니다.

## 🚀 빠른 시작

### 1단계: 도메인 구매 및 Route 53 설정

```powershell
# Route 53 호스팅 영역 생성 및 DNS 레코드 설정
.\setup-route53.ps1 your-domain.com 3.21.177.186
```

### 2단계: Nginx 리버스 프록시 설정

```powershell
# Nginx 설치 및 도메인별 라우팅 설정
.\setup-nginx.ps1 your-domain.com 3.21.177.186
```

### 3단계: 배포 스크립트 업데이트

```powershell
# deploy-aws.ps1에서 도메인 사용 활성화
# $USE_DOMAIN = $true로 변경
# $DOMAIN_NAME = "your-domain.com"으로 변경
```

## 🔧 상세 설정 과정

### 1. 도메인 구매

1. **도메인 등록업체 선택**
   - AWS Route 53 (권장)
   - GoDaddy, Namecheap 등

2. **도메인 선택**
   - 예: `dorandoran.com`, `mydorandoran.com`
   - .com, .net, .org 등 권장

### 2. Route 53 설정

#### 2.1 호스팅 영역 생성
```powershell
.\setup-route53.ps1 dorandoran.com 3.21.177.186
```

이 스크립트가 자동으로 생성하는 DNS 레코드:
- `dorandoran.com` → EC2 IP (A 레코드)
- `www.dorandoran.com` → `dorandoran.com` (CNAME 레코드)
- `api.dorandoran.com` → EC2 IP (A 레코드)
- `auth.dorandoran.com` → EC2 IP (A 레코드)
- `user.dorandoran.com` → EC2 IP (A 레코드)
- `chat.dorandoran.com` → EC2 IP (A 레코드)
- `batch.dorandoran.com` → EC2 IP (A 레코드)

#### 2.2 네임서버 변경
Route 53에서 제공하는 네임서버를 도메인 등록업체에서 설정:
```
ns-1234.awsdns-12.org
ns-567.awsdns-34.net
ns-890.awsdns-56.co.uk
ns-1234.awsdns-78.com
```

### 3. Nginx 리버스 프록시 설정

#### 3.1 Nginx 설치 및 설정
```powershell
.\setup-nginx.ps1 dorandoran.com 3.21.177.186
```

이 스크립트가 자동으로 설정하는 내용:
- Nginx 설치 및 서비스 시작
- 도메인별 서버 블록 생성
- 포트 80/443 방화벽 설정
- WebSocket 지원 (Chat 서비스용)

#### 3.2 SSL 인증서 설정 (Let's Encrypt)
```bash
# EC2에서 직접 실행
sudo certbot --nginx -d dorandoran.com -d www.dorandoran.com -d api.dorandoran.com -d auth.dorandoran.com -d user.dorandoran.com -d chat.dorandoran.com -d batch.dorandoran.com
```

### 4. 배포 스크립트 업데이트

`deploy-aws.ps1` 파일 수정:
```powershell
# 도메인 설정 활성화
$DOMAIN_NAME = "dorandoran.com"
$USE_DOMAIN = $true
```

## 🌐 최종 도메인 구조

### 서비스별 도메인
- **메인 사이트**: `https://dorandoran.com`
- **www 사이트**: `https://www.dorandoran.com`
- **API Gateway**: `https://api.dorandoran.com`
- **Auth Service**: `https://auth.dorandoran.com`
- **User Service**: `https://user.dorandoran.com`
- **Chat Service**: `https://chat.dorandoran.com`
- **Batch Service**: `https://batch.dorandoran.com`

### 포트 매핑
| 서비스 | 내부 포트 | 외부 도메인 |
|--------|-----------|-------------|
| Gateway | 8080 | api.dorandoran.com |
| Auth | 8081 | auth.dorandoran.com |
| User | 8082 | user.dorandoran.com |
| Chat | 8083 | chat.dorandoran.com |
| Batch | 8085 | batch.dorandoran.com |

## 🔒 보안 설정

### 1. SSL/TLS 인증서
- **Let's Encrypt**: 무료 SSL 인증서
- **자동 갱신**: Cron 작업으로 자동 갱신
- **HTTPS 강제**: HTTP → HTTPS 리다이렉트

### 2. 방화벽 설정
```bash
# 필요한 포트만 열기
sudo firewall-cmd --permanent --add-service=http
sudo firewall-cmd --permanent --add-service=https
sudo firewall-cmd --permanent --add-service=ssh
sudo firewall-cmd --reload
```

### 3. 보안 그룹 (AWS)
- **HTTP (80)**: 0.0.0.0/0
- **HTTPS (443)**: 0.0.0.0/0
- **SSH (22)**: 내 IP만
- **내부 포트 (8080-8085)**: EC2 내부만

## 📊 비용 분석

### Route 53 비용 (월간)
- **호스팅 영역**: $0.50
- **DNS 쿼리**: 1,000,000건당 $0.40
- **예상 총 비용**: $1-2/월

### Let's Encrypt SSL
- **비용**: 무료
- **갱신**: 자동 (90일마다)

### 총 추가 비용
- **월간**: $1-2
- **연간**: $12-24

## 🔍 문제 해결

### 1. DNS 전파 지연
- **증상**: 도메인 접속 불가
- **해결**: 최대 48시간 대기, `nslookup` 명령어로 확인

### 2. SSL 인증서 오류
- **증상**: HTTPS 접속 실패
- **해결**: 
  ```bash
  sudo certbot renew --dry-run
  sudo systemctl reload nginx
  ```

### 3. Nginx 설정 오류
- **증상**: 502 Bad Gateway
- **해결**:
  ```bash
  sudo nginx -t
  sudo systemctl status nginx
  sudo journalctl -u nginx
  ```

### 4. 서비스 연결 실패
- **증상**: 도메인은 접속되지만 서비스 응답 없음
- **해결**:
  ```bash
  # 서비스 상태 확인
  docker ps
  # 포트 확인
  netstat -tlnp | grep :8080
  ```

## 📝 체크리스트

### 도메인 설정 전
- [ ] 도메인 구매 완료
- [ ] AWS 계정 준비
- [ ] EC2 인스턴스 실행 중
- [ ] 서비스 배포 완료

### Route 53 설정
- [ ] 호스팅 영역 생성
- [ ] DNS 레코드 생성
- [ ] 네임서버 변경
- [ ] DNS 전파 확인

### Nginx 설정
- [ ] Nginx 설치
- [ ] 설정 파일 생성
- [ ] 서비스 재시작
- [ ] 도메인 접속 테스트

### SSL 설정
- [ ] Let's Encrypt 인증서 발급
- [ ] HTTPS 리다이렉트 설정
- [ ] 자동 갱신 설정
- [ ] SSL 테스트

### 최종 확인
- [ ] 모든 서브도메인 접속 가능
- [ ] HTTPS 정상 작동
- [ ] 서비스 기능 정상
- [ ] 모바일 접속 확인

## 🚀 고급 설정

### 1. CDN 설정 (CloudFront)
```bash
# CloudFront 배포 생성
aws cloudfront create-distribution --distribution-config file://cloudfront-config.json
```

### 2. 로드 밸런서 설정
```bash
# Application Load Balancer 생성
aws elbv2 create-load-balancer --name dorandoran-alb --subnets subnet-12345 subnet-67890
```

### 3. 모니터링 설정
```bash
# CloudWatch 알람 설정
aws cloudwatch put-metric-alarm --alarm-name "HighCPU" --alarm-description "High CPU usage"
```

## 📞 지원

문제가 발생하면 다음을 확인하세요:
1. DNS 전파 상태: `nslookup your-domain.com`
2. 서비스 상태: `docker ps`
3. Nginx 로그: `sudo journalctl -u nginx`
4. SSL 상태: `sudo certbot certificates`

추가 도움이 필요하면 개발팀에 문의하세요.
