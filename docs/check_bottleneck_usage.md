# check_bottleneck.sh 스크립트 사용 가이드

## 기본 사용법

### 1. 로컬에서 실행 (Windows → SSH로 서버 접속)

```powershell
# 서버에 접속하여 실행
ssh -i "$env:USERPROFILE\Downloads\dorandoran-key.pem" ec2-user@3.21.177.186 "~/check_bottleneck.sh dorandoran-auth 8081"
```

### 2. 서버에서 직접 실행

```bash
# 기본 사용 (Auth 서비스)
~/check_bottleneck.sh dorandoran-auth 8081

# 다른 서비스들
~/check_bottleneck.sh dorandoran-chat 8083
~/check_bottleneck.sh dorandoran-user 8082
~/check_bottleneck.sh dorandoran-store 8084
~/check_bottleneck.sh dorandoran-gateway 8080
```

## 인자 설명

```bash
./check_bottleneck.sh <서비스명> <포트>
```

- **서비스명**: Docker 컨테이너 이름 (예: `dorandoran-auth`)
- **포트**: 서비스가 리스닝하는 포트 (예: `8081`)

### 기본값
- 서비스명이 지정되지 않으면: `dorandoran-auth`
- 포트가 지정되지 않으면: `8081`

## 고급 사용법

### 1. 커스텀 엔드포인트 지정

환경변수 `ENDPOINTS`로 체크할 엔드포인트를 지정할 수 있습니다:

```bash
# 여러 엔드포인트 지정
ENDPOINTS="/api/auth/login /api/auth/me /api/auth/refresh" \
  ~/check_bottleneck.sh dorandoran-auth 8081

# 특정 엔드포인트만 체크
ENDPOINTS="/api/auth/login" ~/check_bottleneck.sh dorandoran-auth 8081
```

### 2. 주기적 모니터링 (watch 사용)

```bash
# 30초마다 자동으로 체크
watch -n 30 ~/check_bottleneck.sh dorandoran-auth 8081

# 10초마다 체크
watch -n 10 ~/check_bottleneck.sh dorandoran-chat 8083
```

### 3. 여러 서비스 일괄 체크

```bash
# 모든 서비스 체크 스크립트
for service in auth:8081 user:8082 chat:8083 store:8084 gateway:8080; do
  name=$(echo $service | cut -d: -f1)
  port=$(echo $service | cut -d: -f2)
  echo "=== dorandoran-$name ==="
  ~/check_bottleneck.sh dorandoran-$name $port
  echo ""
  sleep 2
done
```

### 4. 결과를 파일로 저장

```bash
# 결과를 파일로 저장
~/check_bottleneck.sh dorandoran-auth 8081 > auth_check_$(date +%Y%m%d_%H%M%S).log 2>&1

# 또는 append 모드로 저장
~/check_bottleneck.sh dorandoran-auth 8081 >> monitoring.log 2>&1
```

## 서비스별 기본 엔드포인트

스크립트는 서비스명을 기반으로 자동으로 엔드포인트를 선택합니다:

| 서비스 | 기본 엔드포인트 |
|--------|----------------|
| **auth** | `/api/auth/login`, `/api/auth/me` |
| **chat** | `/api/chat/chatrooms/all`, `/api/chat/messages` |
| **user** | `/api/users/me` |
| **store** | `/api/store/bookmarks`, `/api/store/bookmarks/cursor` |
| **gateway** | `/api/auth/login`, `/api/users/me`, `/api/chat/chatrooms/all` |

## 실행 예시

### 예시 1: Auth 서비스 체크

```bash
~/check_bottleneck.sh dorandoran-auth 8081
```

**출력 예시:**
```
=== 병목 현상 체크: dorandoran-auth ===
시간: Tue Nov  4 15:45:29 KST 2025
체크 대상 엔드포인트: /api/auth/login /api/auth/me

1. 서비스 상태
  ✅ 서비스가 정상 작동 중입니다

2. 응답 시간 체크
  /api/auth/login: 평균 .244초 (요청 수: 1.0)
  /api/auth/me: 평균 .034초 (요청 수: 9.0)

  전체 평균 응답 시간: .055초 (총 요청 수: 10.0, 엔드포인트: 2개)

3. 데이터베이스 연결 풀 체크
  활성 연결: 0.0 / 최대: 30.0 (사용률: 0%)

...
```

### 예시 2: Chat 서비스 체크

```bash
~/check_bottleneck.sh dorandoran-chat 8083
```

### 예시 3: 커스텀 엔드포인트로 체크

```bash
ENDPOINTS="/api/auth/login /api/auth/refresh /api/auth/validate" \
  ~/check_bottleneck.sh dorandoran-auth 8081
```

## 주의사항

1. **스크립트 위치**: 서버의 홈 디렉토리 (`~/check_bottleneck.sh`)
2. **실행 권한**: 스크립트는 실행 권한이 있어야 합니다 (`chmod +x ~/check_bottleneck.sh`)
3. **필수 도구**: `jq`, `bc`가 설치되어 있어야 정확한 분석이 가능합니다
4. **네트워크**: 서버에서 `localhost:포트`로 접근 가능해야 합니다

## 문제 해결

### "jq가 설치되어 있지 않습니다" 경고
```bash
# Amazon Linux 2
sudo yum install jq bc -y

# Ubuntu/Debian
sudo apt-get install jq bc -y
```

### "Health 체크 실패" 메시지
- 서비스가 실행 중인지 확인: `docker ps | grep dorandoran-auth`
- 포트가 올바른지 확인: `docker port dorandoran-auth`
- 서비스 로그 확인: `docker logs --tail 50 dorandoran-auth`

### "메트릭 데이터를 가져올 수 없습니다"
- Actuator 엔드포인트가 활성화되어 있는지 확인
- 서비스 설정에서 `management.endpoints.web.exposure.include=metrics,prometheus` 확인

## 자동화 예시

### Cron을 사용한 정기 체크

```bash
# crontab 편집
crontab -e

# 매 5분마다 체크하고 결과를 파일로 저장
*/5 * * * * ~/check_bottleneck.sh dorandoran-auth 8081 >> ~/monitoring/auth.log 2>&1
*/5 * * * * ~/check_bottleneck.sh dorandoran-chat 8083 >> ~/monitoring/chat.log 2>&1
```

### 스크립트로 모든 서비스 체크

```bash
#!/bin/bash
# check_all_services.sh

SERVICES=(
  "dorandoran-auth:8081"
  "dorandoran-user:8082"
  "dorandoran-chat:8083"
  "dorandoran-store:8084"
  "dorandoran-gateway:8080"
)

for service in "${SERVICES[@]}"; do
  name=$(echo $service | cut -d: -f1)
  port=$(echo $service | cut -d: -f2)
  
  echo "=========================================="
  echo "체크 중: $name"
  echo "=========================================="
  ~/check_bottleneck.sh $name $port
  echo ""
  sleep 1
done
```

## 요약

**가장 간단한 사용법:**
```bash
~/check_bottleneck.sh dorandoran-auth 8081
```

**주기적 모니터링:**
```bash
watch -n 30 ~/check_bottleneck.sh dorandoran-auth 8081
```

**커스텀 엔드포인트:**
```bash
ENDPOINTS="/api/custom1 /api/custom2" ~/check_bottleneck.sh dorandoran-auth 8081
```

