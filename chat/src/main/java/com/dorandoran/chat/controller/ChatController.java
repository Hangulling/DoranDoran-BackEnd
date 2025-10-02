package com.dorandoran.chat.controller;

import com.dorandoran.chat.entity.ChatRoom;
import com.dorandoran.chat.entity.Message;
import com.dorandoran.chat.service.ChatService;
import com.dorandoran.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Chat 서비스 REST API 컨트롤러
 * Gateway를 통해 접근: http://localhost:8080/api/chat/**
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;

    /**
     * 헬스체크 - 서비스 상태 확인
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        log.info("Chat 서비스 헬스체크 요청");
        return ResponseEntity.ok("Chat service is running");
    }

    /**
     * 모든 채팅방 조회
     */
    @GetMapping("/rooms")
    public ResponseEntity<ApiResponse<List<ChatRoom>>> getAllChatRooms() {
        log.info("모든 채팅방 조회 요청");
        List<ChatRoom> rooms = chatService.getAllChatRooms();
        return ResponseEntity.ok(ApiResponse.success(rooms));
    }

    /**
     * 사용자의 채팅방 조회
     */
    @GetMapping("/users/{userId}/rooms")
    public ResponseEntity<ApiResponse<List<ChatRoom>>> getUserChatRooms(@PathVariable String userId) {
        log.info("사용자 {}의 채팅방 조회 요청", userId);
        UUID userUuid = UUID.fromString(userId);
        List<ChatRoom> rooms = chatService.getChatRoomsByUser(userUuid);
        return ResponseEntity.ok(ApiResponse.success(rooms));
    }

    /**
     * 채팅방 생성
     */
    @PostMapping("/rooms")
    public ResponseEntity<ApiResponse<ChatRoom>> createChatRoom(@RequestBody CreateRoomRequest request) {
        log.info("채팅방 생성 요청 - 사용자: {}, 봇: {}, 방명: {}", 
                request.getUserId(), request.getBotId(), request.getName());
        
        try {
            ChatRoom room = chatService.createChatRoom(
                    request.getUserId(), 
                    request.getBotId(), 
                    request.getName()
            );
            return ResponseEntity.ok(ApiResponse.success(room));
        } catch (Exception e) {
            log.error("채팅방 생성 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("채팅방 생성에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 채팅방 조회
     */
    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<ApiResponse<ChatRoom>> getChatRoom(@PathVariable String roomId) {
        log.info("채팅방 조회 요청 - ID: {}", roomId);
        UUID roomUuid = UUID.fromString(roomId);
        Optional<ChatRoom> room = chatService.getChatRoom(roomUuid);
        
        if (room.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(ApiResponse.success(room.get()));
    }

    /**
     * 채팅방 삭제
     */
    @DeleteMapping("/rooms/{roomId}")
    public ResponseEntity<ApiResponse<String>> deleteChatRoom(@PathVariable String roomId) {
        log.info("채팅방 삭제 요청 - ID: {}", roomId);
        UUID roomUuid = UUID.fromString(roomId);
        
        boolean deleted = chatService.deleteChatRoom(roomUuid);
        if (deleted) {
            return ResponseEntity.ok(ApiResponse.success("채팅방이 삭제되었습니다"));
        } else {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("채팅방을 찾을 수 없습니다"));
        }
    }

    /**
     * 채팅방의 메시지 조회
     */
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<ApiResponse<List<Message>>> getMessages(@PathVariable String roomId) {
        log.info("채팅방 {}의 메시지 조회 요청", roomId);
        UUID roomUuid = UUID.fromString(roomId);
        List<Message> messages = chatService.getMessages(roomUuid);
        return ResponseEntity.ok(ApiResponse.success(messages));
    }

    /**
     * 메시지 전송
     */
    @PostMapping("/rooms/{roomId}/messages")
    public ResponseEntity<ApiResponse<Message>> sendMessage(
            @PathVariable String roomId, 
            @RequestBody SendMessageRequest request) {
        
        log.info("메시지 전송 요청 - 방: {}, 사용자: {}, 내용: {}", 
                roomId, request.getUserId(), request.getContent());
        
        try {
            UUID roomUuid = UUID.fromString(roomId);
            Message message = chatService.sendMessage(
                    roomUuid,
                    request.getUserId(),
                    request.getBotId(),
                    request.getContent(),
                    request.getSenderType()
            );
            return ResponseEntity.ok(ApiResponse.success(message));
        } catch (IllegalArgumentException e) {
            log.warn("메시지 전송 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("메시지 전송 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("메시지 전송에 실패했습니다"));
        }
    }

    /**
     * 메시지 검색
     */
    @GetMapping("/rooms/{roomId}/messages/search")
    public ResponseEntity<ApiResponse<List<Message>>> searchMessages(
            @PathVariable String roomId,
            @RequestParam String keyword) {
        log.info("메시지 검색 요청 - 방: {}, 키워드: {}", roomId, keyword);
        UUID roomUuid = UUID.fromString(roomId);
        List<Message> messages = chatService.searchMessages(roomUuid, keyword);
        return ResponseEntity.ok(ApiResponse.success(messages));
    }

    /**
     * 채팅방 통계 조회
     */
    @GetMapping("/rooms/{roomId}/stats")
    public ResponseEntity<ApiResponse<ChatService.ChatRoomStats>> getChatRoomStats(@PathVariable String roomId) {
        log.info("채팅방 통계 조회 요청 - 방: {}", roomId);
        UUID roomUuid = UUID.fromString(roomId);
        ChatService.ChatRoomStats stats = chatService.getChatRoomStats(roomUuid);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * 채팅방 생성 요청 DTO
     */
    public static class CreateRoomRequest {
        private UUID userId;
        private UUID botId;
        private String name;

        // Getters and Setters
        public UUID getUserId() { return userId; }
        public void setUserId(UUID userId) { this.userId = userId; }
        public UUID getBotId() { return botId; }
        public void setBotId(UUID botId) { this.botId = botId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    /**
     * 메시지 전송 요청 DTO
     */
    public static class SendMessageRequest {
        private UUID userId;
        private UUID botId;
        private String content;
        private String senderType;
        private Integer chatNum;

        // Getters and Setters
        public UUID getUserId() { return userId; }
        public void setUserId(UUID userId) { this.userId = userId; }
        public UUID getBotId() { return botId; }
        public void setBotId(UUID botId) { this.botId = botId; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getSenderType() { return senderType; }
        public void setSenderType(String senderType) { this.senderType = senderType; }
        public Integer getChatNum() { return chatNum; }
        public void setChatNum(Integer chatNum) { this.chatNum = chatNum; }
    }
}
