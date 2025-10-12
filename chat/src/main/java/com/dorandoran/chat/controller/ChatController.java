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
import com.dorandoran.chat.service.GreetingService;
import com.dorandoran.chat.service.MultiAgentOrchestrator;
import com.dorandoran.chat.service.ChatbotService;
import com.dorandoran.chat.service.dto.ChatbotUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Chat Service REST API Controller (단순화 스키마 기반)
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Chat Management", description = "채팅 서비스 API")
public class ChatController {

    private final ChatService chatService;
    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository;
    private final AIService aiService;
    private final GreetingService greetingService;
    private final MultiAgentOrchestrator multiAgentOrchestrator;
    private final ChatbotService chatbotService;

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
        
        // 기존 채팅방이 있는지 확인
        boolean isNewRoom = !chatRoomRepository.findByUserIdAndChatbotIdAndIsDeletedFalse(userId, request.getChatbotId()).isPresent();
        
        ChatRoom room = chatService.getOrCreateRoom(userId, request.getChatbotId(), request.getName());
        
        // 새로 생성된 채팅방에만 AI 인사말 발송
        if (isNewRoom) {
            greetingService.sendGreeting(room.getId(), userId);
        }
        
        return ResponseEntity.ok(ChatRoomResponse.from(room));
    }

    @GetMapping("/chatrooms")
    public ResponseEntity<Page<ChatRoomResponse>> listRooms(
            @RequestParam(required = false) UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID uid = extractUserIdFromSecurityContext();
        if (uid == null && userId != null) {
            uid = userId;
        }
        if (uid == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<ChatRoom> rooms = chatService.listRooms(uid, pageable);
        Page<ChatRoomResponse> response = rooms.map(ChatRoomResponse::from);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/chatrooms/{chatroomId}/messages")
    public ResponseEntity<Page<MessageResponse>> listMessages(
            @PathVariable UUID chatroomId,
            @RequestParam(required = false) UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        UUID uid = extractUserIdFromSecurityContext();
        if (uid == null && userId != null) {
            uid = userId;
        }
        if (uid == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
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
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestParam(required = false) UUID userId) {
        // SecurityContext에서 우선 추출, 없으면 요청 파라미터, 마지막으로 헤더
        UUID senderId = extractUserIdFromSecurityContext();
        if (senderId == null && userId != null) {
            senderId = userId;
        }
        if (senderId == null && userIdHeader != null && !userIdHeader.isBlank()) {
            try { senderId = UUID.fromString(userIdHeader); } catch (IllegalArgumentException ignored) {}
        }
        if (senderId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
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
        log.info("=== ChatController: 메시지 저장 완료 - messageId={}, senderType={} ===", saved.getId(), senderType);
        
        if ("user".equalsIgnoreCase(senderType)) {
            log.info("=== ChatController: Multi-Agent 처리 시작 - chatroomId={}, senderId={} ===", chatroomId, senderId);
            // Multi-Agent 처리로 변경
            multiAgentOrchestrator.processUserMessage(chatroomId, senderId, saved);
            log.info("=== ChatController: Multi-Agent 처리 호출 완료 ===");
        } else {
            log.info("=== ChatController: Multi-Agent 처리 건너뜀 - senderType={} ===", senderType);
        }
        return ResponseEntity.ok(MessageResponse.from(saved));
    }

    @Operation(summary = "챗봇 프롬프트 수정", description = "챗봇의 프롬프트를 수정합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "프롬프트 수정 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "챗봇을 찾을 수 없음")
    })
    @PostMapping("/chatbots/prompt")
    public ResponseEntity<?> updateChatbotPrompt(@Valid @RequestBody ChatbotUpdateRequest request) {
        log.info("=== 챗봇 프롬프트 수정 요청: {} ===", request);
        
        boolean success = chatbotService.updateChatbotPrompt(request);
        
        if (success) {
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "챗봇 프롬프트가 성공적으로 업데이트되었습니다.",
                "chatbotId", request.getChatbotId(),
                "agentType", request.getAgentType()
            ));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "챗봇 프롬프트 업데이트에 실패했습니다.",
                "chatbotId", request.getChatbotId()
            ));
        }
    }

    @Operation(summary = "챗봇 프롬프트 리셋", description = "챗봇의 프롬프트를 기본값으로 리셋합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "프롬프트 리셋 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "챗봇을 찾을 수 없음")
    })
    @PostMapping("/chatbots/reset")
    public ResponseEntity<?> resetChatbotPrompt(
            @RequestParam String chatbotId,
            @RequestParam String agentType) {
        log.info("=== 챗봇 프롬프트 리셋 요청: chatbotId={}, agentType={} ===", chatbotId, agentType);
        
        boolean success = chatbotService.resetChatbotPrompt(chatbotId, agentType);
        
        if (success) {
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "챗봇 프롬프트가 기본값으로 리셋되었습니다.",
                "chatbotId", chatbotId,
                "agentType", agentType
            ));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "챗봇 프롬프트 리셋에 실패했습니다.",
                "chatbotId", chatbotId
            ));
        }
    }

    @Operation(summary = "챗봇 조회", description = "챗봇 정보를 조회합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "챗봇 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "챗봇을 찾을 수 없음")
    })
    @GetMapping("/chatbots/{chatbotId}")
    public ResponseEntity<?> getChatbot(@PathVariable String chatbotId) {
        log.info("=== 챗봇 조회 요청: {} ===", chatbotId);
        
        return chatbotService.getChatbot(chatbotId)
            .map(chatbot -> {
                Map<String, Object> chatbotData = new HashMap<>();
                chatbotData.put("id", chatbot.getId());
                chatbotData.put("name", chatbot.getName());
                chatbotData.put("displayName", chatbot.getDisplayName());
                chatbotData.put("description", chatbot.getDescription());
                chatbotData.put("systemPrompt", chatbot.getSystemPrompt());
                chatbotData.put("intimacySystemPrompt", chatbot.getIntimacySystemPrompt());
                chatbotData.put("intimacyUserPrompt", chatbot.getIntimacyUserPrompt());
                chatbotData.put("vocabularySystemPrompt", chatbot.getVocabularySystemPrompt());
                chatbotData.put("vocabularyUserPrompt", chatbot.getVocabularyUserPrompt());
                chatbotData.put("translationSystemPrompt", chatbot.getTranslationSystemPrompt());
                chatbotData.put("translationUserPrompt", chatbot.getTranslationUserPrompt());
                chatbotData.put("intimacyLevel", chatbot.getIntimacyLevel());
                chatbotData.put("isActive", chatbot.getIsActive());
                
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "chatbot", chatbotData
                ));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Agent 프롬프트 조회", description = "특정 Agent의 프롬프트를 조회합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Agent 프롬프트 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "챗봇을 찾을 수 없음")
    })
    @GetMapping("/chatbots/{chatbotId}/agents/{agentType}")
    public ResponseEntity<?> getAgentPrompts(
            @PathVariable String chatbotId,
            @PathVariable String agentType) {
        log.info("=== Agent 프롬프트 조회 요청: chatbotId={}, agentType={} ===", chatbotId, agentType);
        
        Map<String, String> prompts = chatbotService.getAgentPrompts(chatbotId, agentType);
        
        if (prompts.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "chatbotId", chatbotId,
            "agentType", agentType,
            "prompts", prompts
        ));
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

    /**
     * 채팅방 단 건 조회
     * (보관함 사용)
     */
    @GetMapping("/chatrooms/{chatroomId}")
    @Operation(summary = "채팅방 조회", description = "채팅방 ID로 채팅방 정보 조회")
    public ResponseEntity<ChatRoomResponse> getChatRoom(
        @Parameter(description = "채팅방 ID", required = true)
        @PathVariable UUID chatroomId,

        @Parameter(description = "사용자 ID (선택)")
        @RequestParam(required = false) UUID userId) {

        // SecurityContext에서 우선 추출
        UUID uid = extractUserIdFromSecurityContext();
        if (uid == null && userId != null) {
            uid = userId;
        }

        // 권한 확인 (요청한 사용자의 채팅방인지)
        if (uid != null && !chatRoomRepository.existsByUserIdAndIdAndIsDeletedFalse(uid, chatroomId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        ChatRoom room = chatService.getChatRoomById(chatroomId);
        return ResponseEntity.ok(ChatRoomResponse.from(room));
    }
}
