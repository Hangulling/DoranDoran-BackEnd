# DAYS_OLD 파라미터 설명

> **작성일**: 2025-01-09  
> **상태**: ⚠️ **현재 사용하지 않음**

---

## DAYS_OLD 파라미터

### 현재 상태

**`DAYS_OLD` (또는 `days-old`) 파라미터는 현재 실제로 사용되지 않습니다.**

### 이유

이전에는 "90일 이상 된 채팅방만 아카이빙"하는 로직이 있었지만, **모든 데이터를 이관하기 위해 날짜 필터를 제거**했습니다.

### 코드 확인

```java
/**
 * 아카이빙 대상 채팅방 조회
 * 
 * 모든 데이터를 이관하기 위해 날짜 필터를 제거하고,
 * is_archived=true 또는 is_deleted=true인 채팅방을 대상으로 합니다.
 * 
 * @param daysOld 사용하지 않음 (하위 호환성을 위해 유지)
 * @param limit 최대 조회 개수
 * @return 아카이빙 대상 채팅방 ID 목록
 */
public List<UUID> findChatroomsToArchive(int daysOld, int limit) {
    // 모든 데이터 이관을 위해 날짜 필터 제거
    // is_archived=true 또는 is_deleted=true인 모든 채팅방을 대상으로 함
    String sql = """
        SELECT id
        FROM chat_schema.chatrooms
        WHERE (is_archived = true OR is_deleted = true)
          AND id NOT IN (
              SELECT source_chatroom_id FROM archive_schema.arch_chatrooms
          )
        ORDER BY last_message_at ASC NULLS LAST, created_at ASC
        LIMIT ?
        """;
    
    return jdbcTemplate.queryForList(sql, UUID.class, limit);
    // daysOld 파라미터는 사용되지 않음
}
```

### 실제 동작

현재 아카이빙 대상은:
- ✅ `is_archived = true` 또는 `is_deleted = true`인 **모든** 채팅방
- ❌ 날짜 필터 없음 (90일 제한 없음)

---

## 왜 유지하는가?

### 하위 호환성

- 기존 스크립트나 설정에서 `days-old` 파라미터를 사용하고 있을 수 있음
- 파라미터를 제거하면 기존 코드가 깨질 수 있음
- 따라서 파라미터는 유지하되, 실제로는 사용하지 않음

---

## 스크립트에서의 사용

### archive-chatrooms.sh

```bash
DAYS_OLD=90  # ⚠️ 사용하지 않음 (하위 호환성을 위해 유지)
```

### archive-all-chatrooms.sh

```bash
DAYS_OLD=90  # ⚠️ 사용하지 않음 (하위 호환성을 위해 유지)
```

**실제로는**:
- `--archive.days-old=$DAYS_OLD`로 전달되지만
- ArchiveService에서 무시됨
- 모든 데이터가 이관됨

---

## 요약

| 항목 | 내용 |
|------|------|
| **파라미터 이름** | `DAYS_OLD` / `days-old` |
| **기본값** | 90 |
| **실제 사용 여부** | ❌ 사용하지 않음 |
| **유지 이유** | 하위 호환성 |
| **실제 동작** | 모든 데이터 이관 (날짜 제한 없음) |

---

## 결론

**`DAYS_OLD`는 현재 사용되지 않는 파라미터입니다.**

- 값을 변경해도 아무 영향이 없습니다
- 스크립트에서 제거해도 동작에는 문제가 없습니다
- 다만 하위 호환성을 위해 유지하는 것을 권장합니다

---

**상태**: ⚠️ 사용하지 않음 (하위 호환성 유지)


