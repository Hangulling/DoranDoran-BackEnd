package com.dorandoran.user.dto;

/**
 * 알림 설정 응답
 */
public record NotificationSettingsResponse(
    boolean pushEnabled
) {
}
