package com.dorandoran.user.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 프롬프트 테스트 요청 DTO (User Service)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromptTestRequest {
    private String agentType;  // INTIMACY_ANALYSIS, INTIMACY_CORRECTION, VOCABULARY_EXTRACTION, VOCABULARY_EXPLANATION, CONVERSATION, GREETING
    private String concept;    // friend, coworker, boss, senior, honey
    private Integer intimacyLevel;  // 1 또는 3
    private String inputText;  // 테스트 입력 텍스트
    private Long promptVersionId; // 선택: 특정 버전 테스트
}
