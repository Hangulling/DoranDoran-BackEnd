package com.dorandoran.user.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

/**
 * 관리 필요 내역 항목 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewTicketItemResponse {
    private UUID messageId;
    private String agentType;
    private Map<String, Object> snapshotJson;
}
