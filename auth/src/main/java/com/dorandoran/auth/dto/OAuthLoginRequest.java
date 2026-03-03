package com.dorandoran.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * OAuth 로그인 요청 DTO
 */
public record OAuthLoginRequest(
    @NotBlank(message = "OAuth 제공자는 필수입니다")
    String provider,
    
    @NotBlank(message = "ID Token은 필수입니다")
    String idToken,
    
    /**
     * 신규 사용자(404)일 때 회원가입 확정 여부.
     * - false(기본): needSignup + oauthUserInfo 반환 (회원가입 폼 pre-fill용)
     * - true: 즉시 회원가입 후 토큰 반환
     */
    @JsonProperty(required = false)
    Boolean confirmSignup,
    
    /**
     * confirmSignup 시 회원 생성 시 저장할 생년월일. yyyy-MM-dd 형식.
     * 미전달 시 기본값(1900-01-01)으로 저장됨.
     */
    @JsonProperty(required = false)
    String birthDate
) {
    public boolean isConfirmSignup() {
        return Boolean.TRUE.equals(confirmSignup);
    }
}

