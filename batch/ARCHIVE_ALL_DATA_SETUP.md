# Archive 모든 데이터 이관 설정 완료

> **작성일**: 2025-01-09  
> **목적**: ArchiveJob이 모든 데이터를 이관하도록 설정

---

## ✅ 설정 완료 사항

### 1. application.yml 기본값 변경

```yaml
archive:
  limit: 999999  # 기존: 100 → 모든 데이터 이관을 위해 매우 큰 값 설정
```

### 2. archive-chatrooms.sh 스크립트 업데이트

```bash
LIMIT=999999  # 기존: 100
```

### 3. archive-all-chatrooms.sh 스크립트 생성

모든 데이터를 이관할 때까지 반복 실행하는 스크립트 추가

---

## 사용 방법

### 방법 1: 한 번에 모든 데이터 이관 (간단)

```bash
bash scripts/backup/archive-chatrooms.sh
```

**설정**:
- `LIMIT=999999`로 설정되어 모든 데이터를 한 번에 처리

**장점**:
- 간단하고 빠름
- 한 번 실행으로 완료

**단점**:
- 메모리 사용량이 클 수 있음
- 매우 큰 데이터셋에서는 문제 발생 가능

### 방법 2: 반복 실행으로 안전하게 이관 (권장)

```bash
bash scripts/backup/archive-all-chatrooms.sh
```

**설정**:
- `BATCH_SIZE=1000`: 한 번에 1000개씩 처리
- `MAX_ITERATIONS=1000`: 최대 1000번 반복 (무한 루프 방지)
- 모든 데이터를 처리할 때까지 자동 반복

**장점**:
- 메모리 사용량 안정적
- 진행 상황 추적 가능
- 실패 시 재시작 가능

**단점**:
- 실행 시간이 더 걸릴 수 있음

---

## 설정 비교

| 방법 | limit 값 | 실행 횟수 | 메모리 사용 | 권장 상황 |
|------|----------|----------|------------|----------|
| **방법 1** | 999999 | 1회 | 높음 | 데이터가 적을 때 |
| **방법 2** | 1000 | 여러 회 | 낮음 | 데이터가 많을 때 |

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

## 주의사항

### 메모리 사용량

- `limit=999999` 설정 시 메모리 사용량이 증가할 수 있습니다
- 대량 데이터 처리 시 `-Xmx` 옵션 조정 필요

### 실행 시간

- 모든 데이터를 한 번에 처리하면 시간이 오래 걸릴 수 있습니다
- 반복 실행 방식이 더 안전할 수 있습니다

### 트랜잭션

- 각 채팅방은 개별 트랜잭션으로 처리됩니다
- 실패해도 이미 처리된 데이터는 보존됩니다

---

## 요약

### ✅ 설정 완료

1. **application.yml**: `limit=999999` 설정
2. **archive-chatrooms.sh**: `LIMIT=999999` 설정
3. **archive-all-chatrooms.sh**: 반복 실행 스크립트 생성

### ✅ 사용 방법

- **간단한 방법**: `archive-chatrooms.sh` 사용 (한 번에 모든 데이터)
- **안전한 방법**: `archive-all-chatrooms.sh` 사용 (반복 실행)

---

**상태**: ✅ 모든 데이터 이관 설정 완료


