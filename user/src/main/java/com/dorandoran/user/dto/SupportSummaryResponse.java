package com.dorandoran.user.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 문의/신고 목록 항목 응답
 */
public record SupportSummaryResponse(
    Long id,
    String type,
    String category,
    String contentPreview,
    LocalDateTime createdAt,
    Boolean replyRequested,
    UUID chatroomId,
    UUID messageId
) {
}
