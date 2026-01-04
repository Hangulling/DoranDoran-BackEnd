# Grafana 영구 저장 및 자동 재시작 확인

## 현재 설정 상태

### ✅ 재시작 정책
- **설정**: `--restart=unless-stopped`
- **의미**: 
  - 서버 재부팅 시 Docker가 시작되면 자동으로 컨테이너 재시작
  - 수동으로 중지한 경우에만 재시작하지 않음
- **상태**: ✅ 설정됨

### ✅ 데이터 영구 저장
- **볼륨 마운트**: `/opt/grafana/data` → `/var/lib/grafana`
- **의미**: 
  - Grafana의 모든 데이터(대시보드, 사용자, 설정 등)가 호스트 디렉토리에 저장
  - 컨테이너가 삭제되어도 데이터 유지
- **상태**: ✅ 설정됨

### ✅ 대시보드 영구 저장
- **볼륨 마운트**: `/opt/grafana/dashboards` → `/var/lib/grafana/dashboards`
- **의미**: 
  - 대시보드 파일이 호스트 디렉토리에 저장
  - Provisioning을 통해 자동 로드
- **상태**: ✅ 설정됨

### ✅ 설정 영구 저장
- **볼륨 마운트**: `/opt/grafana/provisioning` → `/etc/grafana/provisioning`
- **의미**: 
  - 데이터소스, 대시보드 Provisioning 설정이 호스트에 저장
- **상태**: ✅ 설정됨

## 서버 재부팅 시 동작

### 1. Docker 자동 시작
- **확인 필요**: `systemctl is-enabled docker`
- **상태**: 일반적으로 기본적으로 활성화됨

### 2. 컨테이너 자동 재시작
- **정책**: `--restart=unless-stopped`
- **동작**: Docker가 시작되면 자동으로 Grafana 컨테이너 재시작

### 3. 데이터 복구
- **데이터베이스**: `/opt/grafana/data/grafana.db`에서 자동 로드
- **대시보드**: `/opt/grafana/dashboards/`에서 자동 Provisioning
- **설정**: `/opt/grafana/provisioning/`에서 자동 로드

## 확인 방법

### 현재 설정 확인
```bash
# 재시작 정책 확인
docker inspect dorandoran-grafana | jq '.[0].HostConfig.RestartPolicy'

# 볼륨 마운트 확인
docker inspect dorandoran-grafana | jq '.[0].Mounts'

# 데이터베이스 존재 확인
ls -la /opt/grafana/data/grafana.db
```

### 테스트 방법
1. **서버 재부팅 테스트** (주의: 실제 재부팅)
   ```bash
   sudo reboot
   ```

2. **재부팅 후 확인**
   ```bash
   # 컨테이너 실행 확인
   docker ps | grep grafana
   
   # 데이터 확인
   ls -la /opt/grafana/data/
   
   # 대시보드 확인
   curl -u admin:admin123 http://localhost:3000/api/search
   ```

## 주의사항

### ⚠️ Docker 자동 시작 확인
Docker가 자동으로 시작되지 않으면 컨테이너도 자동으로 시작되지 않습니다.

**확인 및 설정**:
```bash
# Docker 자동 시작 확인
systemctl is-enabled docker

# 자동 시작 활성화 (필요 시)
sudo systemctl enable docker
```

### ⚠️ 볼륨 경로 확인
볼륨이 올바른 경로에 마운트되어 있는지 확인:
- `/opt/grafana/data`: Grafana 데이터
- `/opt/grafana/dashboards`: 대시보드 파일
- `/opt/grafana/provisioning`: Provisioning 설정

### ⚠️ 권한 문제
재부팅 후 권한 문제가 발생할 수 있습니다:
```bash
# 권한 확인 및 수정 (필요 시)
sudo chmod -R 777 /opt/grafana
```

## 백업 권장사항

서버 재부팅 후에도 데이터가 유지되지만, 백업은 권장됩니다:

```bash
# Grafana 데이터 백업
tar -czf grafana-backup-$(date +%Y%m%d).tar.gz /opt/grafana

# 대시보드만 백업
tar -czf dashboards-backup-$(date +%Y%m%d).tar.gz /opt/grafana/dashboards
```

## 결론

✅ **Grafana는 서버 재부팅 후에도 유지됩니다**

이유:
1. `--restart=unless-stopped` 정책으로 자동 재시작
2. 모든 데이터가 호스트 디렉토리에 영구 저장
3. Docker가 자동 시작되면 컨테이너도 자동 시작

**확인 사항**:
- Docker 자동 시작 활성화 여부
- 볼륨 마운트 경로 정확성
- 권한 설정







