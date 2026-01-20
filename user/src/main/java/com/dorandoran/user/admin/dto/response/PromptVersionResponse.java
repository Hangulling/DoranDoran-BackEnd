package com.dorandoran.user.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 프롬프트 버전 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptVersionResponse {
    private Long id;
    private String agentType;
    private String concept;
    private Integer intimacyLevel;
    private String version;
    private String content;
    private String filePath;
    private String memo;
    private UUID createdBy;
    private String createdByName;  // 생성자 이름 (추가 조회 필요)
    private LocalDateTime createdAt;
    private Boolean isActive;  // 현재 활성화되어 있는지 여부
}
