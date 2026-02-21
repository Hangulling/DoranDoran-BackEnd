package com.dorandoran.user.admin.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 프롬프트 버전 생성 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromptVersionCreateRequest {
    private String agentType;  // INTIMACY_ANALYSIS, INTIMACY_CORRECTION, VOCABULARY_EXTRACTION, VOCABULARY_EXPLANATION, CONVERSATION, GREETING
    private String concept;    // friend, coworker, boss, senior, honey
    private Integer intimacyLevel;  // 1 또는 3
    private String content;    // 프롬프트 내용
    private String memo;       // 메모 (선택)
}
