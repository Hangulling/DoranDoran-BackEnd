package com.dorandoran.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 이메일 찾기 응답 DTO (마스킹된 이메일 포함)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FindEmailResponse {
    private String email; // 마스킹된 이메일
}

