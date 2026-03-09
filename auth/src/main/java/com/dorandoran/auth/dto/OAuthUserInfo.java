package com.dorandoran.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OAuth 토큰에서 추출한 사용자 정보 (회원가입 폼 pre-fill용)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuthUserInfo {
    private String email;
    private String firstName;
    private String lastName;
    private String name;
    private String picture;
    private String provider;  // GOOGLE, APPLE, FIREBASE
}
