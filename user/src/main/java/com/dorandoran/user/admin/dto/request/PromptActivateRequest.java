package com.dorandoran.user.admin.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 프롬프트 활성화 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromptActivateRequest {
    private String env;        // prod, stage
    private String agentType;  // INTIMACY_ANALYSIS, INTIMACY_CORRECTION, VOCABULARY_EXTRACTION, VOCABULARY_EXPLANATION, CONVERSATION, GREETING
    private String concept;    // friend, coworker, boss, senior, honey
    private Integer intimacyLevel;  // 1 또는 3
    private Long versionId;    // 프롬프트 버전 ID
}
