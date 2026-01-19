package com.dorandoran.user.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Active 프롬프트 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptActiveResponse {
    private String env;        // prod, stage
    private String agentType;  // INTIMACY_ANALYSIS, INTIMACY_CORRECTION, VOCABULARY_EXTRACTION, VOCABULARY_EXPLANATION, CONVERSATION, GREETING
    private String concept;    // friend, coworker, boss, senior, honey
    private Integer intimacyLevel;  // 1 또는 3
    private Long versionId;    // 프롬프트 버전 ID
    private String version;     // 버전 번호 (v0.1, v0.2 등)
    private String content;     // 프롬프트 내용
    private LocalDateTime activatedAt;  // 활성화 시간
}
