package com.dorandoran.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 문의/신고 목록 페이징 응답
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportPageResponse {
    private List<SupportSummaryResponse> content;
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
