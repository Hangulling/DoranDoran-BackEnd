# Archive 작업 실행 결과

> **실행일**: 2025-01-09  
> **방법**: 한 번에 모든 데이터 이관 (`archive-chatrooms.sh`, limit=999999)

---

## 실행 전 상태

- **아카이빙 대상 채팅방**: 369개
- **이미 아카이빙된 채팅방**: 0개

---

## 실행 결과

### 현재 상태 확인 필요

로그 파일을 확인하여 실행 결과를 확인해야 합니다:
- 로그 파일: `/home/ec2-user/backups/archive.log`
- 실행 스크립트: `/home/ec2-user/backups/archive-chatrooms.sh`

### 확인 명령어

```bash
# 로그 확인
tail -100 /home/ec2-user/backups/archive.log

# 아카이빙 완료 확인
docker exec dorandoran-shared-db psql -U doran -d dorandoran -c "
SELECT 
  COUNT(*) as archived_chatrooms,
  (SELECT COUNT(*) FROM archive_schema.arch_messages) as archived_messages,
  (SELECT COUNT(*) FROM archive_schema.arch_stores) as archived_stores,
  (SELECT COUNT(*) FROM archive_schema.arch_usage_events) as archived_usage_events,
  (SELECT COUNT(*) FROM archive_schema.arch_intimacy_progress) as archived_intimacy_progress
FROM archive_schema.arch_chatrooms;
"

# 남은 대상 확인
docker exec dorandoran-shared-db psql -U doran -d dorandoran -c "
SELECT COUNT(*) as remaining
FROM chat_schema.chatrooms
WHERE (is_archived = true OR is_deleted = true)
  AND id NOT IN (SELECT source_chatroom_id FROM archive_schema.arch_chatrooms);
"
```

---

## 예상 결과

성공 시:
- **아카이빙된 채팅방**: 369개
- **아카이빙된 메시지**: 채팅방당 평균 메시지 수에 따라 다름
- **아카이빙된 Store**: Store가 있는 메시지 수
- **아카이빙된 Usage 이벤트**: Usage 이벤트 수
- **아카이빙된 Intimacy Progress**: Intimacy Progress 수

---

**상태**: 실행 중 또는 완료 (로그 확인 필요)


