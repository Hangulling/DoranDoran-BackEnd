# Archive 작업 실행 상태

> **실행일**: 2025-01-09  
> **방법**: 반복 실행 스크립트 (`archive-all-chatrooms.sh`)

---

## 실행 전 상태

### 데이터 현황
- **총 채팅방**: 470개
- **아카이빙 대상**: 369개 (`is_archived = true` 또는 `is_deleted = true`)
- **이미 아카이빙된 채팅방**: 0개

### 설정
- **BATCH_SIZE**: 1000 (한 번에 처리할 채팅방 수)
- **MAX_ITERATIONS**: 1000 (최대 반복 횟수)
- **예상 반복 횟수**: 약 1회 (369개 < 1000개)

---

## 실행 방법

### 백그라운드 실행

```bash
cd /home/ec2-user
nohup bash /home/ec2-user/backups/archive-all-chatrooms.sh > /home/ec2-user/backups/archive-all-execution.log 2>&1 &
```

### 진행 상황 확인

```bash
# 로그 확인
tail -f /home/ec2-user/backups/archive-all.log

# 실행 상태 확인
ps aux | grep java | grep batch.jar

# 아카이빙 진행 상황 확인
docker exec dorandoran-shared-db psql -U doran -d dorandoran -c "
SELECT 
  COUNT(*) as total_target,
  (SELECT COUNT(*) FROM archive_schema.arch_chatrooms) as archived_count
FROM chat_schema.chatrooms
WHERE (is_archived = true OR is_deleted = true)
  AND id NOT IN (SELECT source_chatroom_id FROM archive_schema.arch_chatrooms);
"
```

---

## 완료 확인

### SQL 쿼리

```sql
-- 아카이빙 완료 확인
SELECT COUNT(*) 
FROM chat_schema.chatrooms
WHERE (is_archived = true OR is_deleted = true)
  AND id NOT IN (
      SELECT source_chatroom_id FROM archive_schema.arch_chatrooms
  );

-- 결과가 0이면 모든 데이터 이관 완료
```

### 로그 확인

```bash
# 완료 메시지 확인
grep "모든 데이터 이관 완료" /home/ec2-user/backups/archive-all.log

# 총 이관된 채팅방 수 확인
grep "총 이관된 채팅방 수" /home/ec2-user/backups/archive-all.log
```

---

## 예상 소요 시간

- **채팅방당 약 1-5초** (메시지 수에 따라 다름)
- **369개 채팅방**: 약 6-30분
- **실제 시간**: 데이터 양에 따라 다를 수 있음

---

## 주의사항

- 작업이 백그라운드에서 실행 중입니다
- 로그 파일을 통해 진행 상황을 확인할 수 있습니다
- 작업이 완료될 때까지 기다려주세요

---

**상태**: ✅ 실행 중


