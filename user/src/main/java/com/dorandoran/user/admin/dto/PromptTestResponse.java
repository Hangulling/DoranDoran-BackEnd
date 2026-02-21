package com.dorandoran.user.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 프롬프트 테스트 응답 DTO (User Service)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptTestResponse {
    private String outputText;  // Agent 출력 텍스트
    private Long latencyMs;     // 실행 시간 (밀리초)
    private Integer tokens;      // 사용된 토큰 수
}
