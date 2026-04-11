package com.dorandoran.user.admin.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 관리자용 문의/신고 목록 항목 응답
 */
public record AdminSupportSummaryResponse(
    Long id,
    UUID userId,
    String requesterName,
    String requesterEmail,
    String type,
    String category,
    String contentPreview,
    LocalDateTime createdAt,
    Boolean replyRequested,
    UUID chatroomId,
    UUID messageId,
    String status,
    String answeredBy,
    LocalDateTime answeredAt
) {
}
