# Archive 수정 사항 요약

> **수정일**: 2025-01-04  
> **목적**: 모든 데이터 이관 및 SQL 쿼리 오류 수정

---

## 수정 완료 사항

### 1. `billing.ai_usage_events` 쿼리 오류 수정 ✅

**문제**: `billing.ai_usage_events` 테이블에 `message_id` 컬럼이 존재하지 않아 쿼리 실행 시 오류 발생

**수정 내용**:
- `queryUsageRequestIdMap()` 메서드를 `@Deprecated`로 표시하고 빈 맵 반환
- `buildMetadataJson()` 메서드에서 메시지의 `metadata.usage.requestId`를 직접 추출하도록 수정
- `billing.ai_usage_events` 테이블과의 직접 연결 제거

**파일**: `batch/src/main/java/com/dorandoran/batch/service/ArchiveService.java`

**변경 사항**:
```java
// 수정 전: billing.ai_usage_events에서 message_id로 조회 (오류 발생)
SELECT message_id, request_id FROM billing.ai_usage_events WHERE chatroom_id = ?

// 수정 후: 메시지의 metadata.usage.requestId에서 직접 추출
if (originalMetadata.has("usage")) {
    JsonNode usage = originalMetadata.get("usage");
    if (usage.has("requestId")) {
        usageRequestId = usage.get("requestId").asText();
    }
}
```

---

### 2. 아카이빙 대상 조건 수정 ✅

**문제**: 
- 날짜 필터(`last_message_at < NOW() - INTERVAL '90 days'`)로 인해 모든 채팅방이 제외됨
- 활성 채팅방(`is_archived = false AND is_deleted = false`)이 아카이빙되지 않음

**수정 내용**:
- 날짜 필터 제거
- `is_archived = true OR is_deleted = true`인 모든 채팅방을 아카이빙 대상으로 설정
- 정렬 순서 개선: `last_message_at ASC NULLS LAST, created_at ASC`

**파일**: `batch/src/main/java/com/dorandoran/batch/service/ArchiveService.java`

**변경 사항**:
```java
// 수정 전: 날짜 필터로 인해 아무것도 아카이빙되지 않음
WHERE (is_archived = true OR is_deleted = true)
  AND last_message_at < NOW() - INTERVAL '%d days'

// 수정 후: 날짜 필터 제거, 모든 대상 채팅방 아카이빙
WHERE (is_archived = true OR is_deleted = true)
  AND id NOT IN (SELECT source_chatroom_id FROM archive_schema.arch_chatrooms)
ORDER BY last_message_at ASC NULLS LAST, created_at ASC
```

---

### 3. 메시지 페이징 제한 개선 ✅

**문제**: `offset > 10000`이면 중단되어 메시지가 많은 채팅방은 일부만 아카이빙됨

**수정 내용**:
- 배치 크기를 100 → 1000으로 증가
- 최대 offset을 10,000 → 1,000,000으로 증가
- 로그 메시지 개선

**파일**: `batch/src/main/java/com/dorandoran/batch/service/ArchiveService.java`

**변경 사항**:
```java
// 수정 전
int batchSize = 100;
if (offset > 10000) {
    log.warn("메시지가 너무 많아 중단: chatroomId={}, offset={}", chatroomId, offset);
    break;
}

// 수정 후
int batchSize = 1000;
int maxOffset = 1000000; // 최대 100만 개 메시지까지 처리
if (offset >= maxOffset) {
    log.warn("메시지가 너무 많아 중단: chatroomId={}, offset={}", chatroomId, offset);
    break;
}
```

---

## 영향 분석

### 긍정적 영향

1. **모든 데이터 이관 가능**: 날짜 필터 제거로 모든 대상 채팅방이 아카이빙됨
2. **쿼리 오류 해결**: `billing.ai_usage_events.message_id` 오류 해결
3. **Usage Request ID 추출**: 메시지 metadata에서 직접 추출하여 더 정확한 데이터 보존
4. **대용량 메시지 처리**: 페이징 제한 증가로 더 많은 메시지 처리 가능

### 주의사항

1. **활성 채팅방**: 여전히 `is_archived = false AND is_deleted = false`인 활성 채팅방은 아카이빙되지 않음
   - 필요 시 아카이빙 조건을 추가로 수정해야 함
2. **메시지 페이징**: 100만 개를 초과하는 메시지가 있는 채팅방은 여전히 일부만 아카이빙됨
   - 필요 시 cursor-based pagination으로 변경 고려

---

## 다음 단계 (선택 사항)

1. **활성 채팅방 아카이빙**: 필요 시 `is_archived = false AND is_deleted = false` 조건 추가
2. **Cursor-based Pagination**: 대용량 메시지 처리를 위한 개선
3. **관련 데이터 아카이빙**: Store, Usage, Intimacy Progress 전체 데이터 아카이빙

---

**수정 완료일**: 2025-01-04  
**검증 상태**: 코드 수정 완료, 실제 실행 검증 필요


