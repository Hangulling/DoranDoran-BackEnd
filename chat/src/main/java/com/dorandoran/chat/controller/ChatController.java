package com.dorandoran.chat.controller;

import com.dorandoran.chat.entity.ChatRoom;
import com.dorandoran.chat.entity.Message;
import com.dorandoran.chat.service.dto.ChatRoomCreateRequest;
import com.dorandoran.chat.service.dto.ChatRoomResponse;
import com.dorandoran.chat.service.dto.MessageResponse;
import com.dorandoran.chat.service.dto.MessageSendRequest;
import com.dorandoran.chat.repository.ChatRoomRepository;
import com.dorandoran.chat.repository.MessageRepository;
import com.dorandoran.chat.service.ChatService;
import com.dorandoran.chat.service.AIService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

/**
 * Chat Service REST API Controller (단순화 스키마 기반)
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "Chat Management", description = "채팅 서비스 API")
public class ChatController {

    private final ChatService chatService;
    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository;
    private final AIService aiService;

    @Operation(summary = "채팅방 생성/조회", description = "새로운 채팅방을 생성하거나 기존 채팅방을 조회합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "채팅방 생성/조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 데이터")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/chatrooms")
    public ResponseEntity<ChatRoomResponse> getOrCreateRoom(@Valid @RequestBody ChatRoomCreateRequest request) {
        // SecurityContext에서 userId 우선 사용 (없으면 요청 바디)
        UUID userId = extractUserIdFromSecurityContext();
        if (userId == null) userId = request.getUserId();
        ChatRoom room = chatService.getOrCreateRoom(userId, request.getChatbotId(), request.getName());
        return ResponseEntity.ok(ChatRoomResponse.from(room));
    }

    @GetMapping("/chatrooms")
    public ResponseEntity<Page<ChatRoomResponse>> listRooms(
            @RequestParam(required = false) UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID uid = extractUserIdFromSecurityContext();
        if (uid == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<ChatRoom> rooms = chatService.listRooms(uid, pageable);
        Page<ChatRoomResponse> response = rooms.map(ChatRoomResponse::from);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/chatrooms/{chatroomId}/messages")
    public ResponseEntity<Page<MessageResponse>> listMessages(
            @PathVariable UUID chatroomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        UUID uid = extractUserIdFromSecurityContext();
        if (uid == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!chatRoomRepository.existsByUserIdAndIdAndIsDeletedFalse(uid, chatroomId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Pageable pageable = PageRequest.of(page, size);
        Page<Message> messages = chatService.listMessages(chatroomId, pageable);
        Page<MessageResponse> response = messages.map(MessageResponse::from);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "메시지 전송", description = "채팅방에 메시지를 전송합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "메시지 전송 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "채팅방 접근 권한 없음")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/chatrooms/{chatroomId}/messages")
    public ResponseEntity<MessageResponse> send(
            @Parameter(description = "채팅방 UUID", required = true)
            @PathVariable UUID chatroomId,
            @Valid @RequestBody MessageSendRequest request,
            @Parameter(description = "사용자 ID (선택적 헤더)")
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        // SecurityContext에서 우선 추출, 없으면 헤더
        UUID senderId = extractUserIdFromSecurityContext();
        if (senderId == null && userIdHeader != null && !userIdHeader.isBlank()) {
            try { senderId = UUID.fromString(userIdHeader); } catch (IllegalArgumentException ignored) {}
        }
        if (senderId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!chatRoomRepository.existsByUserIdAndIdAndIsDeletedFalse(senderId, chatroomId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        // senderType 검증: API 호출은 사용자 발신만 허용
        String senderType = request.getSenderType();
        if (senderType == null || !senderType.equalsIgnoreCase("user")) {
            senderType = "user";
        }
        Message saved = chatService.sendMessage(chatroomId, senderId, senderType, request.getContent(), request.getContentType());
        if ("user".equalsIgnoreCase(senderType)) {
            aiService.streamAIResponse(saved);
        }
        return ResponseEntity.ok(MessageResponse.from(saved));
    }

    private UUID extractUserIdFromSecurityContext() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UUID u) {
                return u;
            }
        } catch (Exception ignored) {}
        return null;
    }
}
