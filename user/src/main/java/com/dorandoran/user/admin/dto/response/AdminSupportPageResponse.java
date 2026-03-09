package com.dorandoran.user.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 관리자용 문의/신고 목록 페이징 응답
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminSupportPageResponse {
    private List<AdminSupportSummaryResponse> content;
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
