package com.dorandoran.chat.service.dto;

import com.dorandoran.chat.entity.Message;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class MessageResponse {
    private UUID id;
    private UUID chatroomId;
    private String senderType;
    private UUID senderId;
    private String content;
    private String contentType;
    private Long sequenceNumber;
    private Boolean isEdited;
    private Boolean isDeleted;
    private LocalDateTime createdAt;

    public static MessageResponse from(Message m) {
        return new MessageResponse(
            m.getId(),
            m.getChatRoom().getId(),
            m.getSenderType(),
            m.getSenderId(),
            m.getContent(),
            m.getContentType(),
            m.getSequenceNumber(),
            m.getIsEdited(),
            m.getIsDeleted(),
            m.getCreatedAt()
        );
    }
}


