package com.dorandoran.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 비밀번호 재설정 요청 DTO
 * 기존 호환성을 위해 유지하되, 코드 기반 재설정을 위해 확장
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordRequest {
    private String email;
    private String newPassword;
    private String code; // 비밀번호 재설정 코드 (선택적)
}


