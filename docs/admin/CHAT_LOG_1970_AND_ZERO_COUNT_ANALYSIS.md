# 채팅 로그 조회 - 날짜 1970.1.1, 메시지 수 0 원인 분석

## 현상
- Koach-Admin의 '채팅 로그 조회' 기능에서:
  - **날짜**가 `1970. 1. 1.`으로 표시됨
  - **메시지 수**가 `0`으로 표시됨

---

## 1. 날짜 1970.1.1 원인

### 흐름

```
[DB] arch_chatrooms.last_message_at = NULL
  → [Backend] ChatLogListResponse.lastMessageAt = null (Jackson 직렬화)
  → [API 응답] "lastMessageAt": null
  → [Frontend] new Date(null).toLocaleString('ko-KR')
  → [결과] "1970. 1. 1. 오전 9:00:00" (Unix epoch 0)
```

### 관련 코드

**백엔드 - ArchChatroom 엔티티** (`user/admin/entity/ArchChatroom.java`)
```java
@Column(name = "last_message_at")
private LocalDateTime lastMessageAt;  // nullable - DB에서 NULL 가능
```

**백엔드 - ArchChatroomRepository** (`user/admin/repository/ArchChatroomRepository.java`)
- JPQL에서 `c.lastMessageAt`을 그대로 ChatLogListResponse로 전달
- NULL이면 Jackson이 JSON `null`로 직렬화

**프론트엔드 - MyChatLogListPage.tsx** (라인 256-258)
```tsx
<td className="py-3 px-4 text-sm">
  {new Date(log.lastMessageAt).toLocaleString('ko-KR')}
</td>
```
- `log.lastMessageAt`이 `null`/`undefined`일 때
- JavaScript에서 `new Date(null)` = **1970-01-01 00:00:00 UTC** (Unix epoch)
- 한국 시간대(KST)로 변환 시 `1970. 1. 1. 오전 9:00:00` 등으로 표시

### last_message_at이 NULL이 되는 경우
- 채팅방이 생성되었지만 아직 **사용자 메시지가 한 건도 없는 경우**
- **삭제된 채팅방** (`chatrooms.is_deleted = true`, `last_message_at = NULL`)
- 아카이브 시점에 원본 `chatrooms.last_message_at`이 NULL이었던 경우
- 운영 DB의 `chatrooms` 테이블에서 `last_message_at`이 NULL인 레코드를 아카이브한 경우

---

## 2. 메시지 수 0 원인

### 흐름

```
[ArchChatroomRepository.searchChatLogs JPQL]
  → (SELECT COUNT(m.id) FROM ArchMessage m WHERE m.archChatroomId = c.id)
  → arch_messages 테이블에 해당 arch_chatroom_id의 메시지가 없으면 0 반환
```

### 관련 코드

**백엔드 - ArchChatroomRepository** (`user/admin/repository/ArchChatroomRepository.java` 라인 32-35)
```java
"c.lastMessageAt, " +
"(SELECT COUNT(m.id) FROM ArchMessage m WHERE m.archChatroomId = c.id), " +
"c.userEmailSnapshot) " +
```

- `arch_chatroom_id`로 `arch_messages`를 조회
- 해당 채팅방에 메시지가 없으면 COUNT = 0

### 메시지가 0인 경우
1. **채팅방만 있고 메시지가 없는 경우**
   - 인사말만 있는 채팅방, 사용자가 한 번도 메시지를 보내지 않은 경우
2. **아카이브 타이밍 이슈**
   - `arch_chatrooms`는 삽입되었으나 `arch_messages` 아카이브가 실패/미실행된 경우
   - (UserWithdrawalArchiveService는 room → messages 순으로 처리하므로 정상 흐름에서는 둘 다 있어야 함)
3. **원본 데이터 특성**
   - 운영 `chat_schema.messages`에 해당 채팅방 메시지가 없었던 경우

---

## 3. 1970.1.1과 메시지 수 0이 동시에 나타나는 이유

- `last_message_at`이 NULL인 채팅방은 **마지막 메시지가 없다**는 의미
- 그런 채팅방은 보통 **메시지가 없거나**, 삭제·초기화된 채팅방
- 따라서 `last_message_at = NULL`이면 `messageCount = 0`인 경우가 많음

---

## 4. 수정 제안

### 4.1 날짜 1970.1.1 수정 (프론트엔드)

**MyChatLogListPage.tsx** - null/undefined 처리 추가:
```tsx
<td className="py-3 px-4 text-sm">
  {log.lastMessageAt
    ? new Date(log.lastMessageAt).toLocaleString('ko-KR')
    : '-'}
</td>
```

### 4.2 (선택) 백엔드에서 fallback

- `lastMessageAt`이 null일 때 `sourceCreatedAt` 등을 대체 값으로 사용
- 또는 API 스펙에서 null 허용하고 프론트에서 `-` 표시하는 방식으로 처리

### 4.3 메시지 수 0

- 데이터 특성상 0인 경우가 있을 수 있음
- 로직 변경보다는, `last_message_at`이 null인 채팅방은 메시지가 없는 것이 맞는지 데이터/비즈니스 관점에서 확인하는 것이 좋음

---

## 5. 참고 - 데이터 흐름 요약

| 단계 | 위치 | 설명 |
|------|------|------|
| 1 | `archive_schema.arch_chatrooms` | last_message_at, 메시지 수 서브쿼리로 조회 |
| 2 | `ArchChatroomRepository.searchChatLogs` | JPQL → ChatLogListResponse 생성 |
| 3 | `ChatLogController.searchChatLogs` | `GET /api/admin/chat-logs/search` |
| 4 | `Koach-Admin` → `searchChatLogs` API | startDate, endDate 등으로 검색 |
| 5 | `MyChatLogListPage` | `log.lastMessageAt`, `log.messageCount` 표시 |
