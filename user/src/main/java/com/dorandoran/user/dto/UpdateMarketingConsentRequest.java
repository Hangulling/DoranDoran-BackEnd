package com.dorandoran.user.dto;

/**
 * 마케팅 수신 동의 업데이트 요청 DTO
 */
public record UpdateMarketingConsentRequest(
    Boolean marketingConsent
) {
}

