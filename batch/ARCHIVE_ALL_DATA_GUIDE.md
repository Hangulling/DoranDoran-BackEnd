# Archive 모든 데이터 이관 가이드

> **작성일**: 2025-01-09  
> **목적**: ArchiveJob이 모든 데이터를 이관하도록 설정

---

## 현재 제한사항

### limit 파라미터

현재 `ArchiveJob`은 `limit` 파라미터로 한 번에 처리할 최대 채팅방 수를 제한합니다:

```yaml
archive:
  limit: 100  # 한 번에 처리할 최대 채팅방 수
```

**문제점**:
- 한 번 실행 시 최대 100개 채팅방만 처리
- 모든 데이터를 이관하려면 여러 번 실행해야 함

---

## 해결 방법

### 방법 1: limit을 매우 크게 설정 (간단) ✅

**설정**:
```bash
java -jar batch.jar \
  --archive.enabled=true \
  --archive.limit=999999 \
  --archive.days-old=90 \
  --archive.job-name="archive-all-job"
```

**장점**:
- 간단하고 빠름
- 기존 코드 변경 불필요

**단점**:
- 메모리 사용량이 클 수 있음
- 매우 큰 데이터셋에서는 문제 발생 가능

**권장 값**:
- `limit=999999` (거의 모든 경우 충분)
- 또는 `limit=2147483647` (Integer.MAX_VALUE)

### 방법 2: 반복 실행 스크립트 사용 (권장) ✅

모든 데이터를 처리할 때까지 반복 실행하는 스크립트:

```bash
#!/bin/bash
# 모든 데이터를 이관할 때까지 반복 실행

BATCH_JAR="/home/ec2-user/batch.jar"
LOG_FILE="/home/ec2-user/backups/archive-all.log"
BATCH_SIZE=1000  # 한 번에 처리할 채팅방 수
MAX_ITERATIONS=1000  # 최대 반복 횟수 (무한 루프 방지)

mkdir -p /home/ec2-user/backups

iteration=0
total_archived=0

while [ $iteration -lt $MAX_ITERATIONS ]; do
    iteration=$((iteration + 1))
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] === 반복 $iteration 시작 ==="
    
    java -Xmx512m -jar "$BATCH_JAR" \
      --archive.enabled=true \
      --archive.days-old=90 \
      --archive.limit=$BATCH_SIZE \
      --archive.job-name="archive-all-iteration-$iteration" \
      --chatroom-cleanup.enabled=false \
      --inactive-user.enabled=false \
      --batch.controller.enabled=false \
      >> "$LOG_FILE" 2>&1
    
    EXIT_CODE=$?
    
    if [ $EXIT_CODE -ne 0 ]; then
        echo "[$(date +'%Y-%m-%d %H:%M:%S')] ❌ 반복 $iteration 실패"
        break
    fi
    
    # 로그에서 처리된 채팅방 수 확인
    archived_count=$(grep -o "아카이빙 대상: [0-9]*개 채팅방" "$LOG_FILE" | tail -1 | grep -o "[0-9]*" || echo "0")
    
    if [ "$archived_count" = "0" ]; then
        echo "[$(date +'%Y-%m-%d %H:%M:%S')] ✅ 모든 데이터 이관 완료 (총 반복: $iteration)"
        break
    fi
    
    total_archived=$((total_archived + archived_count))
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] ✅ 반복 $iteration 완료 (이번 반복: $archived_count개, 누적: $total_archived개)"
    
    # 다음 반복 전 잠시 대기 (DB 부하 방지)
    sleep 5
done

echo "[$(date +'%Y-%m-%d %H:%M:%S')] === 전체 아카이빙 완료: 총 $total_archived개 채팅방 ==="
```

### 방법 3: ArchiveJob에 반복 로직 추가 (고급) ⚠️

`ArchiveJob`에 모든 데이터를 처리할 때까지 반복하는 로직을 추가할 수 있습니다.

**장점**:
- 자동으로 모든 데이터 처리
- 진행 상황 추적 가능

**단점**:
- 코드 변경 필요
- 테스트 필요

---

## 권장 설정

### 즉시 실행 (간단)

```bash
java -jar /home/ec2-user/batch.jar \
  --archive.enabled=true \
  --archive.limit=999999 \
  --archive.days-old=90 \
  --archive.job-name="archive-all-job" \
  --chatroom-cleanup.enabled=false \
  --inactive-user.enabled=false \
  --batch.controller.enabled=false
```

### 반복 실행 스크립트 (안전)

`scripts/backup/archive-all-chatrooms.sh` 스크립트를 생성하여 사용:

```bash
bash scripts/backup/archive-all-chatrooms.sh
```

---

## 설정 예시

### application.yml 수정 (선택사항)

```yaml
archive:
  enabled: false
  days-old: 90
  limit: 999999  # 모든 데이터 이관을 위해 매우 큰 값 설정
  job-name: "default-archive-job"
```

### 스크립트 수정

`archive-chatrooms.sh`에서 `LIMIT` 값을 크게 설정:

```bash
LIMIT=999999  # 기존: 100
```

---

## 주의사항

### 메모리 사용량

- `limit`을 크게 설정하면 메모리 사용량이 증가할 수 있습니다
- 대량 데이터 처리 시 `-Xmx` 옵션 조정 필요

### 실행 시간

- 모든 데이터를 한 번에 처리하면 시간이 오래 걸릴 수 있습니다
- 반복 실행 방식이 더 안전할 수 있습니다

### 트랜잭션

- 각 채팅방은 개별 트랜잭션으로 처리됩니다
- 실패해도 이미 처리된 데이터는 보존됩니다

---

## 검증 방법

### 아카이빙 완료 확인

```sql
-- 아카이빙 대상 채팅방 수 확인
SELECT COUNT(*) 
FROM chat_schema.chatrooms
WHERE (is_archived = true OR is_deleted = true)
  AND id NOT IN (
      SELECT source_chatroom_id FROM archive_schema.arch_chatrooms
  );

-- 결과가 0이면 모든 데이터 이관 완료
```

### 진행 상황 확인

```sql
-- 아카이빙된 채팅방 수
SELECT COUNT(*) FROM archive_schema.arch_chatrooms;

-- 최근 아카이빙 상태
SELECT * FROM archive_schema.arch_ingestion_state
ORDER BY updated_at DESC
LIMIT 1;
```

---

## 요약

### ✅ 권장 방법

1. **즉시 실행**: `limit=999999` 설정
2. **안전한 실행**: 반복 실행 스크립트 사용

### 설정 예시

```bash
# 간단한 방법
--archive.limit=999999

# 또는 반복 실행 스크립트
bash scripts/backup/archive-all-chatrooms.sh
```

---

**상태**: ✅ 모든 데이터 이관 가능


