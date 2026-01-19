package com.dorandoran.user.dto;

/**
 * 알림 설정 변경 요청
 */
public record UpdateNotificationSettingsRequest(
    Boolean pushEnabled
) {
}
