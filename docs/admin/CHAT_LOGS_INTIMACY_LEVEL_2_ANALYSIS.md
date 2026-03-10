# 사용자 채팅 내역 - 친밀도가 2로 표시되는 원인 분석

## 현상
- Koach-Admin `/chat-logs/user-history` (사용자 채팅 내역)에서
- 대화 목록의 **친밀도** 컬럼이 `2`로 표시됨

---

## 친밀도 데이터 소스

### 1. 운영(chat) 데이터 소스

**AdminConversationService.java** (라인 64-72)
```java
List<AdminConversationListItem> content = roomPage.getContent().stream()
    .map(room -> AdminConversationListItem.builder()
        .conversationId(room.getId())
        // ...
        .intimacyLevel(room.getChatbot() != null ? room.getChatbot().getIntimacyLevel() : null)
        // ...
        .build())
```

→ **`room.getChatbot().getIntimacyLevel()`**  
→ **chatbots.intimacy_level** 값 사용 (챗봇 기본 친밀도)

### 2. 보관(archive) 데이터 소스

**AdminConversationService.java** (라인 161, 195)
```java
// SQL: cr.chatbot_intimacy_level_snapshot AS intimacy_level
.intimacyLevel((Integer) rs.getObject("intimacy_level"))
```

→ **arch_chatrooms.chatbot_intimacy_level_snapshot** 값 사용

---

## 친밀도 2가 들어가는 경로

### 경로 A: chatbots.intimacy_level = 2

1. **테스트/시드 데이터**
   - `tests/sql/create_chatbot.sql` (라인 105): `intimacy_level = 2`로 INSERT

2. **운영 DB**
   - `chatbots` 테이블에 `intimacy_level = 2`로 등록된 챗봇이 있으면
   - 운영 데이터 소스에서 모두 2로 표시됨

### 경로 B: intimacy_progress.intimacy_level = 2 (아카이브용)

1. **UserWithdrawalArchiveService.java** (라인 112-116)
   ```java
   Integer chatbotIntimacy = chatbotData != null && chatbotData.get("intimacy_level") != null
       ? ((Number) chatbotData.get("intimacy_level")).intValue() : null;
   if (progressData != null && progressData.get("intimacy_level") != null) {
       chatbotIntimacy = ((Number) progressData.get("intimacy_level")).intValue();
   }
   ```
   - 아카이브 시 `intimacy_progress.intimacy_level`을 우선 사용
   - 없으면 `chatbots.intimacy_level` 사용

2. **ChatService.createRoom()** (라인 158)
   ```java
   int level = (intimacyLevel != null && intimacyLevel >= 1 && intimacyLevel <= 3) ? intimacyLevel : 2;
   ```
   - `intimacyLevel`이 null이면 **2**로 초기화

### 경로 C: 코드 기본값 2

| 위치 | 내용 |
|------|------|
| `ChatRoomCreateRequest.java` 라인 30 | `private Integer intimacyLevel = 2;` |
| `ChatService.getOrCreateRoom()` 라인 56 | `return getOrCreateRoom(userId, chatbotId, name, "FRIEND", 2);` |
| `ChatService.createRoom()` 라인 158 | `int level = ... ? intimacyLevel : 2;` |
| `ChatService.getIntimacyLevel()` 라인 702 | `.orElse(2);` (intimacy_progress 없을 때) |
| `ChatRoomConcept` enum | HONEY, COWORKER, SENIOR 기본값 **2** |
| `ChatController` 라인 373 | `int level = request.getIntimacyLevel() != null ? ... : 2;` |

---

## 설계 특이사항: 운영 vs 보관

- **운영 데이터**: `chatbot.intimacy_level` (챗봇 기본값)
- **보관 데이터**: `chatbot_intimacy_level_snapshot` (아카이브 시점 사용자 친밀도)

운영 데이터에서는 **채팅방별 사용자 친밀도(intimacy_progress)** 가 아니라  
**챗봇 기본 친밀도**만 보여주고 있어서,  
챗봇이 2로 설정되어 있으면 모든 대화가 2로 표시됩니다.

---

## 조치 제안

### 1. 운영 데이터에서 실제 사용자 친밀도 표시

- `AdminConversationService.getConversations()`에서
- `room.getChatbot().getIntimacyLevel()` 대신
- `intimacy_progress.intimacy_level`(채팅방별 사용자 현재 친밀도)를 조회하도록 변경

### 2. chatbots.intimacy_level 검증

- 운영 DB에서 `chat_schema.chatbots`의 `intimacy_level` 확인
- 의도치 않게 2로 설정된 레코드가 있는지 점검

### 3. 친밀도 필터 옵션

- 현재 `ChatLogsUserHistoryPage`에는 친밀도 필터가 1, 3만 있고 2가 없음 (라인 165-167)
- 2도 선택할 수 있도록 `option value="2">레벨 2</option>` 추가 가능
