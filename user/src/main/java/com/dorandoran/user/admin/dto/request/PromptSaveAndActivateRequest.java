package com.dorandoran.user.admin.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 프롬프트 저장 및 적용 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptSaveAndActivateRequest {
    private String env;  // prod, dev, test
    private String agentType;  // INTIMACY_ANALYSIS, INTIMACY_CORRECTION, etc.
    private String concept;  // friend, coworker, etc.
    private Integer intimacyLevel;  // 1 or 3
    private String content;  // 프롬프트 내용
    private String memo;  // 메모 (선택)
}
