package com.dorandoran.user.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 문의/신고 상세 응답
 */
public record SupportDetailResponse(
    Long id,
    String type,
    String category,
    String content,
    LocalDateTime createdAt,
    Boolean replyRequested,
    String replyEmail,
    UUID chatroomId,
    UUID messageId,
    String messageContent,
    JsonNode aiResponseSnapshot,
    String requesterEmail,
    String requesterName
) {
}
