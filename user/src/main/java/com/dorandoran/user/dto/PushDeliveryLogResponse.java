package com.dorandoran.user.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 푸시 발송 로그 응답
 */
public record PushDeliveryLogResponse(
    Long id,
    UUID userId,
    UUID chatroomId,
    LocalDate sentDate,
    LocalDateTime createdAt
) {
}
