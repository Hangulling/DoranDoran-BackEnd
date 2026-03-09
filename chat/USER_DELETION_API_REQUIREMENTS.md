# 사용자 완전 삭제를 위한 Chat 서비스 API 요구사항

> **작성일**: 2026-01-28  
> **상태**: 설계 완료, 구현 대기

---

## 개요

사용자 완전 삭제 플로우에서 **해당 유저의 모든 채팅방 ID 목록을 조회**하기 위한 Chat 서비스 API가 필요합니다.

---

## 요구사항

### 엔드포인트

**GET** `/api/chatrooms/user/{userId}/ids`

**목적**: 해당 유저의 모든 채팅방 ID 목록을 조회

**인증**: HMAC 인증 필요 (서비스 간 통신)

**응답 형식**:
```json
[
  "550e8400-e29b-41d4-a716-446655440000",
  "550e8400-e29b-41d4-a716-446655440001",
  "550e8400-e29b-41d4-a716-446655440002"
]
```

**필터링**:
- `is_deleted = false`인 채팅방만 조회
- `is_archived = false`인 채팅방만 조회 (선택)

---

## 구현 예시

### ChatController에 추가

**파일**: `chat/src/main/java/com/dorandoran/chat/controller/ChatController.java`

```java
/**
 * 해당 유저의 모든 채팅방 ID 목록 조회
 * 사용자 완전 삭제 시 아카이브를 위해 사용
 */
@GetMapping("/chatrooms/user/{userId}/ids")
public ResponseEntity<List<UUID>> getUserChatroomIds(
        @PathVariable UUID userId) {
    log.info("사용자 채팅방 ID 목록 조회 요청: userId={}", userId);
    
    try {
        List<UUID> chatroomIds = chatService.getUserChatroomIds(userId);
        return ResponseEntity.ok(chatroomIds);
    } catch (Exception e) {
        log.error("사용자 채팅방 ID 목록 조회 실패: userId={}, error={}", userId, e.getMessage());
        return ResponseEntity.internalServerError().build();
    }
}
```

### ChatService에 메서드 추가

**파일**: `chat/src/main/java/com/dorandoran/chat/service/ChatService.java`

```java
/**
 * 해당 유저의 모든 채팅방 ID 목록 조회
 */
public List<UUID> getUserChatroomIds(UUID userId) {
    log.info("사용자 채팅방 ID 목록 조회: userId={}", userId);
    
    return chatRoomRepository.findByUser_IdAndIsDeletedFalse(userId)
        .stream()
        .map(ChatRoom::getId)
        .collect(Collectors.toList());
}
```

---

## 참고

- 이 API는 **서비스 간 통신용**이므로 HMAC 인증이 필요합니다
- User 서비스의 `ChatServiceClient`에서 이 API를 호출합니다
- 배치 작업의 `UserDeletionService`에서도 이 API를 호출합니다
