package com.dorandoran.user.dto;

import java.time.LocalDateTime;

/**
 * 마케팅 수신 동의 응답 DTO
 */
public record MarketingConsentResponse(
    boolean marketingConsent,
    LocalDateTime marketingConsentAt
) {
}

