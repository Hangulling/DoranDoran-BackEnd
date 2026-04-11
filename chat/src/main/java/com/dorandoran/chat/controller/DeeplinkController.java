package com.dorandoran.chat.controller;

import com.dorandoran.chat.entity.ChatRoom;
import com.dorandoran.chat.service.ChatService;
import com.dorandoran.chat.service.dto.ChatRoomResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 딥링크 기반 채팅방 생성 API (푸시 클릭 시 주제 기반 채팅방 생성)
 */
@RestController
@RequestMapping("/api/deeplink")
@RequiredArgsConstructor
@Slf4j
public class DeeplinkController {

    private final ChatService chatService;

    @GetMapping("/chatroom/create")
    @Operation(summary = "딥링크 채팅방 생성", description = "chatbotId, topic(선택), concept로 항상 새 채팅방을 생성합니다. intimacyLevel은 null로 설정되어 UI에서 선택 후 별도 API로 greeting을 시작합니다. X-User-Id 필수.")
    public ResponseEntity<ChatRoomResponse> createChatroomFromDeeplink(
        @Parameter(description = "챗봇 ID", required = true) @RequestParam UUID chatbotId,
        @Parameter(description = "대화 주제 (선택)") @RequestParam(required = false) String topic,
        @Parameter(description = "컨셉") @RequestParam(required = false, defaultValue = "FRIEND") String concept,
        @RequestParam(required = false) UUID userId
    ) {
        UUID uid = extractUserIdFromSecurityContext();
        if (uid == null && userId != null) {
            uid = userId;
        }
        if (uid == null) {
            log.warn("딥링크 채팅방 생성 요청 검증 실패: userId 없음, chatbotId={}, topic={}", chatbotId, topic);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String conceptStr = concept != null && !concept.isBlank() ? concept : "FRIEND";
        String topicVal = (topic != null && !topic.isBlank()) ? topic : null;
        try {
            // intimacyLevel을 null로 설정하여 UI에서 선택하도록 함
            ChatRoom room = chatService.recreateRoom(uid, chatbotId, "새 대화", conceptStr, null, topicVal, null);
            log.info("딥링크 채팅방 새로 생성 완료 (intimacyLevel 미설정): userId={}, chatbotId={}, topic={}, roomId={}", uid, chatbotId, topicVal, room.getId());
            ChatRoomResponse response = toChatRoomResponse(room);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("딥링크 채팅방 생성 파라미터 오류: userId={}, chatbotId={}, error={}", uid, chatbotId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("딥링크 채팅방 생성 실패: userId={}, chatbotId={}, topic={}", uid, chatbotId, topicVal, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
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

    private ChatRoomResponse toChatRoomResponse(ChatRoom room) {
        String concept = chatService.getConcept(room.getId());
        Integer intimacyLevel = chatService.getIntimacyLevel(room.getId());
        return ChatRoomResponse.from(room, concept, intimacyLevel);
    }
}
