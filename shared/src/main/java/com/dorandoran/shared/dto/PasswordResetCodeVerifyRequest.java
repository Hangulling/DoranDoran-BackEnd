package com.dorandoran.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 비밀번호 재설정 코드 검증 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetCodeVerifyRequest {
    private String email;
    private String code;
}

