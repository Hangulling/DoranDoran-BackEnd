# DoranDoran AWS 배포 가이드

## 📋 개요

이 가이드는 DoranDoran 프로젝트를 AWS EC2에 배포하는 방법을 설명합니다.

## 🚀 빠른 시작

### 1. 전체 서비스 배포 (권장)

**Linux/Mac:**
```bash
chmod +x deploy-aws.sh
./deploy-aws.sh all prod
```

**Windows:**
```powershell
.\deploy-aws.ps1 all prod
```

### 2. Chat 서비스만 배포

**Linux/Mac:**
```bash
chmod +x deploy-chat-only.sh
./deploy-chat-only.sh
```

**Windows:**
```powershell
.\deploy-chat-only.ps1
```

## 🔧 사전 준비사항

### 1. AWS EC2 인스턴스 설정
- **인스턴스 타입**: t3.medium (2 vCPU, 4GB RAM)
- **운영체제**: Amazon Linux 2 또는 Ubuntu
- **보안 그룹**: 다음 포트들 열어두기
  - 22 (SSH)
  - 8080 (API Gateway)
  - 8081 (Auth Service)
  - 8082 (User Service)
  - 8083 (Chat Service)
  - 8085 (Batch Service)

### 2. AWS RDS 설정
- **엔진**: PostgreSQL
- **인스턴스 클래스**: db.t3.micro (개발용) 또는 db.t3.small (운영용)
- **스토리지**: 20GB (범용 SSD)
- **백업**: 7일 보관

### 3. 로컬 환경 설정
- Docker 설치
- SSH 클라이언트 설치
- EC2 키 페어 파일 (`dorandoran-key.pem`) 다운로드

## 📊 예상 비용 (월간)

### 기본 구성 (t3.medium + db.t3.micro)
| 서비스 | 인스턴스 타입 | 월간 비용 |
|--------|---------------|-----------|
| EC2 | t3.medium | $29.95 |
| RDS | db.t3.micro | $12.41 |
| 스토리지 (100GB) | gp2 | $10.00 |
| **총합** | | **$52.36** |

### 운영 구성 (t3.large + db.t3.small)
| 서비스 | 인스턴스 타입 | 월간 비용 |
|--------|---------------|-----------|
| EC2 | t3.large | $59.90 |
| RDS | db.t3.small | $24.82 |
| 스토리지 (200GB) | gp2 | $20.00 |
| **총합** | | **$104.72** |

## 🛠️ 서비스별 배포

### 1. API Gateway (포트 8080)
```bash
./deploy-aws.sh gateway prod
```

### 2. Auth Service (포트 8081)
```bash
./deploy-aws.sh auth prod
```

### 3. User Service (포트 8082)
```bash
./deploy-aws.sh user prod
```

### 4. Chat Service (포트 8083)
```bash
./deploy-aws.sh chat prod
```

### 5. Batch Service (포트 8085)
```bash
./deploy-aws.sh batch prod
```

## 🔍 배포 후 확인사항

### 1. 서비스 상태 확인
```bash
# 모든 서비스 헬스 체크
curl http://3.21.177.186:8080/actuator/health  # API Gateway
curl http://3.21.177.186:8081/actuator/health  # Auth Service
curl http://3.21.177.186:8082/actuator/health  # User Service
curl http://3.21.177.186:8083/actuator/health  # Chat Service
curl http://3.21.177.186:8085/actuator/health  # Batch Service
```

### 2. Docker 컨테이너 상태 확인
```bash
ssh -i "$HOME/Downloads/dorandoran-key.pem" ec2-user@3.21.177.186 "docker ps"
```

### 3. 로그 확인
```bash
# 특정 서비스 로그 확인
ssh -i "$HOME/Downloads/dorandoran-key.pem" ec2-user@3.21.177.186 "docker logs dorandoran-chat"
```

## 🚨 문제 해결

### 1. SSH 연결 실패
- 키 파일 권한 확인: `chmod 400 dorandoran-key.pem`
- EC2 인스턴스 상태 확인
- 보안 그룹에서 SSH 포트(22) 열려있는지 확인

### 2. Docker 이미지 빌드 실패
- Docker 데몬이 실행 중인지 확인
- 충분한 디스크 공간이 있는지 확인
- 네트워크 연결 상태 확인

### 3. 서비스 시작 실패
- 환경 변수 설정 확인
- 데이터베이스 연결 상태 확인
- 포트 충돌 확인

### 4. 헬스 체크 실패
- 서비스 로그 확인
- 데이터베이스 연결 확인
- 외부 API 키 설정 확인 (OpenAI API 등)

## 📝 환경 변수 설정

### 필수 환경 변수
```bash
# OpenAI API 설정
export OPENAI_API_KEY='your-openai-api-key'

# 데이터베이스 설정
export RDS_HOST='your-rds-endpoint'
export RDS_USER='your-db-username'
export RDS_PASSWORD='your-db-password'
```

### 선택적 환경 변수
```bash
# 서비스 포트 설정
export SERVER_PORT=8083
export SERVER_ADDRESS=0.0.0.0

# Spring 프로파일 설정
export SPRING_PROFILES_ACTIVE=prod
```

## 🔄 업데이트 및 재배포

### 1. 코드 변경 후 재배포
```bash
# 전체 서비스 재배포
./deploy-aws.sh all prod

# 특정 서비스만 재배포
./deploy-aws.sh chat prod
```

### 2. 롤백
```bash
# 이전 버전으로 롤백 (Docker 태그 사용)
ssh -i "$HOME/Downloads/dorandoran-key.pem" ec2-user@3.134.243.98 "
    docker stop dorandoran-chat
    docker rm dorandoran-chat
    docker run -d --name dorandoran-chat -p 8083:8083 dorandoran-chat:previous
"
```

## 📈 모니터링

### 1. 기본 모니터링
- AWS CloudWatch를 통한 리소스 모니터링
- Docker 컨테이너 상태 모니터링
- 애플리케이션 로그 모니터링

### 2. 알림 설정
- CloudWatch 알람 설정
- 이메일/SMS 알림 설정
- Slack/Discord 웹훅 설정

## 💡 최적화 팁

### 1. 비용 최적화
- 개발 환경에서는 t3.micro 사용
- 불필요한 서비스는 중지
- 스토리지 최적화

### 2. 성능 최적화
- 적절한 인스턴스 타입 선택
- 로드 밸런서 사용
- CDN 사용 고려

### 3. 보안 최적화
- 보안 그룹 최소 권한 원칙
- SSL/TLS 인증서 적용
- 정기적인 보안 업데이트

## 📞 지원

문제가 발생하면 다음을 확인하세요:
1. 로그 파일 확인
2. AWS CloudWatch 메트릭 확인
3. 네트워크 연결 상태 확인
4. 리소스 사용량 확인

추가 도움이 필요하면 개발팀에 문의하세요.
