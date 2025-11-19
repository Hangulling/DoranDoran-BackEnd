package com.dorandoran.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * OAuth 로그인 요청 DTO
 */
public record OAuthLoginRequest(
    @NotBlank(message = "OAuth 제공자는 필수입니다")
    String provider,
    
    @NotBlank(message = "ID Token은 필수입니다")
    String idToken
) {
}

