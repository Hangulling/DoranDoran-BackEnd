package com.dorandoran.user.dto;

/**
 * FCM 토큰 등록 요청
 */
public record FcmTokenRequest(
    String token,
    String platform
) {
}
