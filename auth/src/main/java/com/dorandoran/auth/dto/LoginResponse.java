package com.dorandoran.auth.dto;

import com.dorandoran.shared.dto.UserDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 로그인 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    private UserDto user;        // 개별 필드 대신 UserDto 객체 사용

    /** 회원가입 필요 시 true. 이때 accessToken/user는 null, oauthUserInfo로 폼 pre-fill */
    private Boolean needSignup;
    /** needSignup=true일 때 토큰에서 추출한 사용자 정보 (회원가입 폼용) */
    private OAuthUserInfo oauthUserInfo;
}
