package com.dorandoran.user.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 관리 필요 내역 상세 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewTicketDetailResponse {
    private Long id;
    private UUID conversationId;
    private String status;
    private String agentType;
    private String note;
    private UUID createdBy;
    private String createdByName;
    private UUID assignee;
    private String assigneeName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime doneAt;
    private List<ReviewTicketItemResponse> items;
}
