package com.dorandoran.chat.service.dto;

import com.dorandoran.chat.entity.ChatRoom;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class ChatRoomResponse {
    private UUID id;
    private UUID userId;
    private UUID chatbotId;
    private String name;
    private String description;
    private LocalDateTime lastMessageAt;
    private UUID lastMessageId;
    private Boolean isArchived;
    private Boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String concept;
    private Integer intimacyLevel;

    public static ChatRoomResponse from(ChatRoom r) {
        String concept = extractConceptFromSettings(r.getSettings());
        // intimacyLevel은 IntimacyProgress에서 조회해야 하므로 null로 설정
        // 실제 사용 시에는 ChatService.getIntimacyLevel()을 호출해야 함
        return from(r, concept, null);
    }
    
    public static ChatRoomResponse from(ChatRoom r, String concept, Integer intimacyLevel) {
        return new ChatRoomResponse(
            r.getId(),
            r.getUser().getId(),
            r.getChatbot().getId(),
            r.getName(),
            r.getDescription(),
            r.getLastMessageAt(),
            r.getLastMessage() != null ? r.getLastMessage().getId() : null,
            r.getIsArchived(),
            r.getIsDeleted(),
            r.getCreatedAt(),
            r.getUpdatedAt(),
            concept,
            intimacyLevel
        );
    }
    
    private static String extractConceptFromSettings(JsonNode settings) {
        if (settings != null && settings.has("concept")) {
            return settings.get("concept").asText();
        }
        return "FRIEND"; // 기본값
    }
}


