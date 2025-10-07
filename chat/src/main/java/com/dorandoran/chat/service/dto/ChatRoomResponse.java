package com.dorandoran.chat.service.dto;

import com.dorandoran.chat.entity.ChatRoom;
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

    public static ChatRoomResponse from(ChatRoom r) {
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
            r.getUpdatedAt()
        );
    }
}


