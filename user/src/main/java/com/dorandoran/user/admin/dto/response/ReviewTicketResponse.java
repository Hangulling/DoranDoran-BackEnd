package com.dorandoran.user.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 관리 필요 내역 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewTicketResponse {
    private Long id;
    private UUID conversationId;
    private String status;  // OPEN, DONE
    private String agentType;
    private String note;
    private UUID createdBy;
    private String createdByName;  // 생성자 이름 (추가 조회 필요)
    private UUID assignee;
    private String assigneeName;  // 담당자 이름 (추가 조회 필요)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime doneAt;
}
