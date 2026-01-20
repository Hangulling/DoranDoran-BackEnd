package com.dorandoran.user.dto;

import java.util.UUID;

/**
 * 푸시 알림 발송 요청 (내부용)
 */
public record PushNotificationRequest(
    UUID userId,
    String title,
    String body,
    UUID chatroomId,
    UUID messageId
) {
}
