# Loki 데이터 영구 저장 안내

## ✅ 안전합니다: 재시작만으로는 로그가 삭제되지 않습니다

### 데이터 저장 방식

Loki는 **Docker 볼륨**을 사용하여 데이터를 영구 저장합니다:

```yaml
volumes:
  - loki_data:/loki  # Docker 볼륨으로 데이터 영구 저장
```

### 안전한 작업

다음 작업들은 **기존 로그를 삭제하지 않습니다**:

1. ✅ **컨테이너 재시작** (`docker restart dd-loki`)
2. ✅ **설정 파일 변경 후 재시작**
3. ✅ **컨테이너 중지 후 시작** (`docker stop/start`)
4. ✅ **docker-compose restart loki**

### 위험한 작업

다음 작업들은 **기존 로그를 삭제할 수 있습니다**:

1. ❌ **볼륨 삭제** (`docker volume rm loki_data`)
2. ❌ **컨테이너와 볼륨 함께 제거** (`docker-compose down -v`)
3. ❌ **볼륨 마운트 경로 변경** (새 볼륨 사용 시)

## 현재 설정 확인

### 볼륨 확인

```bash
# 볼륨 목록 확인
docker volume ls | grep loki

# 볼륨 상세 정보 확인
docker volume inspect loki_data

# 볼륨 데이터 크기 확인
docker exec dd-loki du -sh /loki
```

### 데이터 위치

- **볼륨 이름**: `loki_data`
- **컨테이너 내부 경로**: `/loki`
- **실제 저장 위치**: Docker가 관리하는 볼륨 디렉토리

## 설정 변경 시 주의사항

### retention_period 변경

`retention_period`를 변경하면:
- ✅ **기존 로그는 유지됩니다**
- ✅ **새로운 보존 정책이 적용됩니다**
- ⚠️ **기존 로그가 새로운 보존 기간을 초과하면 자동 삭제됩니다**

예시:
- 기존: 30일 보존 → 90일로 변경
  - 30일 이전 로그도 90일까지 보존됨 ✅
  
- 기존: 90일 보존 → 30일로 변경
  - 30일을 초과한 로그는 자동 삭제됨 ⚠️

## 데이터 백업 (권장)

중요한 로그 데이터는 정기적으로 백업하는 것을 권장합니다:

```bash
# 볼륨 백업
docker run --rm -v loki_data:/data -v $(pwd):/backup alpine tar czf /backup/loki_backup_$(date +%Y%m%d).tar.gz /data

# 백업 복원
docker run --rm -v loki_data:/data -v $(pwd):/backup alpine tar xzf /backup/loki_backup_YYYYMMDD.tar.gz -C /
```

## 현재 상태 확인

서버에서 다음 명령어로 확인:

```bash
# 1. 볼륨 존재 확인
docker volume ls | grep loki_data

# 2. 볼륨 데이터 확인
docker exec dd-loki ls -lh /loki

# 3. 로그 데이터 크기 확인
docker exec dd-loki du -sh /loki/*

# 4. 최근 로그 확인 (Loki API)
curl -s "http://localhost:3100/loki/api/v1/query?query={container=~\".*dorandoran.*\"}&limit=5"
```

## 결론

**현재 설정으로는 재시작만으로는 로그가 삭제되지 않습니다.** 
Docker 볼륨(`loki_data`)이 데이터를 영구 저장하므로 안전하게 재시작할 수 있습니다.











