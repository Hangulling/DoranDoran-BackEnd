# 리소스 모니터링 가이드

## 간단한 명령어로 확인

### CPU 사용률
```bash
# 실시간 CPU 사용률
top

# 1회 확인
top -bn1 | grep 'Cpu(s)'

# CPU 코어 정보
nproc
lscpu
```

### 메모리 사용률
```bash
# 메모리 사용량 확인
free -h

# 상세 메모리 정보
cat /proc/meminfo

# Swap 사용량
swapon --show
```

### 디스크 사용률
```bash
# 디스크 사용량
df -h

# 특정 디렉토리 크기
du -sh /path/to/directory

# 상위 디렉토리 크기
du -h --max-depth=1 /
```

### Load Average
```bash
# Load Average 확인 (1분, 5분, 15분)
uptime

# w 명령으로도 확인
w
```

## Docker 리소스 모니터링

### 컨테이너별 리소스
```bash
# 실시간 리소스 사용량
docker stats

# 1회 확인
docker stats --no-stream

# 특정 컨테이너만
docker stats container_name

# 포맷 지정
docker stats --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}"
```

### 컨테이너 리소스 제한 확인
```bash
# 컨테이너 설정 확인
docker inspect container_name | grep -i memory

# 네트워크 설정
docker network inspect network_name
```

### 디스크 사용량
```bash
# Docker 디스크 사용량
docker system df

# 상세 정보
docker system df -v

# 불필요한 리소스 정리
docker system prune -a
```

## 고급 모니터링 도구

### htop (대화형)
```bash
# 설치
sudo yum install htop -y

# 실행
htop
```

### iotop (디스크 I/O)
```bash
# 설치
sudo yum install iotop -y

# 실행
sudo iotop
```

### nethogs (네트워크)
```bash
# 설치
sudo yum install nethogs -y

# 실행
sudo nethogs
```

### Glances (종합 모니터링)
```bash
# 설치
pip3 install glances

# 실행
glances
```

## 지속적인 모니터링

### watch 명령어
```bash
# 1초마다 업데이트
watch -n 1 'free -h'

# Docker stats
watch -n 1 'docker stats --no-stream'

# CPU 사용률
watch -n 1 'top -bn1 | grep "Cpu(s)"'
```

### 백그라운드 모니터링
```bash
# 모니터링 로그 저장
while true; do
  echo "$(date): $(free -h | grep Mem)" >> mem.log
  sleep 60
done

# 위 명령을 백그라운드로
nohup ./monitor.sh &
```

## 스크립트를 이용한 모니터링

### 간단한 모니터링 스크립트
```bash
#!/bin/bash
# save as monitor.sh

while true; do
  echo "=== $(date) ===" >> monitor.log
  echo "CPU: $(top -bn1 | grep 'Cpu(s)')" >> monitor.log
  echo "Memory: $(free -h | grep Mem)" >> monitor.log
  echo "Load: $(uptime)" >> monitor.log
  echo "" >> monitor.log
  sleep 300  # 5분마다
done
```

### 실행
```bash
chmod +x monitor.sh
nohup ./monitor.sh &

# 로그 확인
tail -f monitor.log
```

## 클라우드 모니터링 (AWS)

### CloudWatch 모니터링
```bash
# AWS CLI로 지표 확인
aws cloudwatch get-metric-statistics \
  --namespace AWS/EC2 \
  --metric-name CPUUtilization \
  --dimensions Name=InstanceId,Value=i-1234567890abcdef0 \
  --start-time 2025-01-01T00:00:00Z \
  --end-time 2025-01-02T00:00:00Z \
  --period 3600 \
  --statistics Average
```

### CloudWatch 대시보드
- AWS Console → CloudWatch → Dashboards
- EC2 인스턴스 선택
- CPU, Memory, Disk, Network 모니터링

## Prometheus & Grafana (현재 프로젝트에 포함)

### Prometheus 설정 확인
```bash
# Prometheus 컨테이너 확인
docker ps | grep prometheus

# Prometheus 메트릭 확인
curl http://localhost:9090/api/v1/targets

# 서비스 상태
curl http://localhost:9090/api/v1/query?query=up
```

### Grafana 접속
```bash
# Grafana 컨테이너 확인
docker ps | grep grafana

# 웹 브라우저에서 접속
http://EC2-IP:3000

# 기본 로그인
# User: admin
# Password: admin123
```

### Grafana 대시보드
- CPU 사용률
- 메모리 사용률
- 네트워크 I/O
- 디스크 I/O
- Docker 컨테이너 메트릭

## 액세스 확인

### Prometheus 접속
```bash
# 포트 확인
docker ps | grep prometheus

# 접속 테스트
curl http://localhost:9090
```

### Grafana 접속
```bash
# 포트 확인
docker ps | grep grafana

# 접속 테스트
curl http://localhost:3000
```

## 알림 설정

### CloudWatch 알람
```bash
# CPU 사용률 알람 생성
aws cloudwatch put-metric-alarm \
  --alarm-name high-cpu-alarm \
  --alarm-description "CPU usage is high" \
  --metric-name CPUUtilization \
  --namespace AWS/EC2 \
  --statistic Average \
  --period 300 \
  --threshold 80 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 2
```

### SNS 알림
```bash
# SNS 토픽 생성
aws sns create-topic --name server-alerts

# 이메일 구독
aws sns subscribe \
  --topic-arn arn:aws:sns:region:account:server-alerts \
  --protocol email \
  --notification-endpoint your-email@example.com
```

## 실용적인 명령어 모음

### 한 줄로 전체 상태 확인
```bash
echo "=== CPU ===" && top -bn1 | grep 'Cpu(s)';
echo "=== Memory ===" && free -h | grep Mem;
echo "=== Disk ===" && df -h | grep '/$';
echo "=== Load ===" && uptime;
echo "=== Docker ===" && docker stats --no-stream | grep dorandoran;
```

### 리소스 사용량 로그 저장
```bash
# CSV 형태로 저장
echo "timestamp,cpu,memory,disk" >> resource-log.csv
echo "$(date +%Y-%m-%d\ %H:%M:%S),$(top -bn1 | grep 'Cpu(s)' | awk '{print $2}'),$(free | grep Mem | awk '{print $3}'),$(df -h | grep '/$' | awk '{print $5}')" >> resource-log.csv
```

### 메모리 부족 알림
```bash
#!/bin/bash
# alarm.sh

MEMORY=$(free | grep Mem | awk '{printf "%.0f", $3/$2 * 100}')

if [ "$MEMORY" -gt 90 ]; then
  echo "경고: 메모리 사용률 ${MEMORY}%"
  # 알림 발송 (예: 이메일, Slack 등)
fi
```

## 일일 보고서 생성

### 자동 리포트 스크립트
```bash
#!/bin/bash
# daily-report.sh

DATE=$(date +%Y%m%d)
REPORT_DIR="/home/ec2-user/reports"

mkdir -p "$REPORT_DIR"

{
  echo "=== 리소스 사용량 리포트 - $(date) ==="
  echo ""
  echo "=== CPU ==="
  top -bn1 | grep 'Cpu(s)'
  echo ""
  echo "=== Memory ==="
  free -h
  echo ""
  echo "=== Disk ==="
  df -h
  echo ""
  echo "=== Docker Containers ==="
  docker stats --no-stream
  echo ""
  echo "=== Top Processes ==="
  ps aux --sort=-%mem | head -10
} > "$REPORT_DIR/report_${DATE}.txt"

echo "리포트 생성: $REPORT_DIR/report_${DATE}.txt"
```

## 권장 모니터링 빈도

| 항목 | 빈도 | 명령어 |
|------|------|--------|
| CPU | 실시간 | `top`, `htop` |
| Memory | 1분마다 | `free -h`, `watch -n 60 free -h` |
| Disk | 1시간마다 | `df -h`, `watch -n 3600 df -h` |
| Docker | 필요시 | `docker stats` |
| Load Average | 5분마다 | `uptime`, `watch -n 300 uptime` |

## 정리

- **기본 모니터링**: `top`, `free`, `df`
- **Docker**: `docker stats`
- **고급 도구**: `htop`, `iotop`, `glances`
- **지속 모니터링**: `watch`, `nohup`
- **클라우드**: CloudWatch, Grafana
- **자동화**: Cron + 스크립트

