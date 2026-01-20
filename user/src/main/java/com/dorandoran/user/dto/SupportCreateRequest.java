package com.dorandoran.user.dto;

import java.util.UUID;

/**
 * 문의/신고 등록 요청
 */
public record SupportCreateRequest(
    String type,
    String category,
    String content,
    Boolean replyRequested,
    String replyEmail,
    UUID chatroomId,
    UUID messageId,
    String messageContent,
    String aiResponseSnapshot
) {
}
