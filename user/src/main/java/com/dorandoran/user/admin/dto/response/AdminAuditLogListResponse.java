package com.dorandoran.user.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 감사 로그 목록 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAuditLogListResponse {
    private List<AdminAuditLogResponse> content;
    private PageInfo page;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageInfo {
        private Integer number;
        private Integer size;
        private Integer totalPages;
        private Long totalElements;
    }
}
