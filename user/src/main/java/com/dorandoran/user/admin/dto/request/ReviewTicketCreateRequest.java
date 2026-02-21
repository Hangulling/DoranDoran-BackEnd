package com.dorandoran.user.admin.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 관리 필요 내역 생성 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewTicketCreateRequest {
    private UUID conversationId;
    private String agentType;
    private String note;
    private List<ReviewTicketItemRequest> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewTicketItemRequest {
        private UUID messageId;
        private String agentType;
        private Map<String, Object> snapshotJson;
    }
}
