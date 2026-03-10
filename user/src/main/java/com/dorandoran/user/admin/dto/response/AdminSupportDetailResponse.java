package com.dorandoran.user.admin.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 관리자용 문의/신고 상세 응답
 */
public record AdminSupportDetailResponse(
    Long id,
    UUID userId,
    String requesterName,
    String requesterEmail,
    String type,
    String category,
    String content,
    LocalDateTime createdAt,
    Boolean replyRequested,
    String replyEmail,
    UUID chatroomId,
    UUID messageId,
    String messageContent,
    JsonNode aiResponseSnapshot
) {
}
