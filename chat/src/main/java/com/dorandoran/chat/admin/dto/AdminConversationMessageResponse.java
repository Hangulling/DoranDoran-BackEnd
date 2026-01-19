package com.dorandoran.chat.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 관리자 대화 메시지 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminConversationMessageResponse {
    private UUID messageId;
    private String senderType;
    private String content;
    private String contentType;
    private String metadata;
    private Long sequenceNumber;
    private Long turnNumber;
    private LocalDateTime createdAt;
}
