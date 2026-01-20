package com.dorandoran.user.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 프롬프트 버전 목록 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptVersionListResponse {
    private List<PromptVersionResponse> content;
    private PageInfo page;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageInfo {
        private Integer number;      // 현재 페이지 (0부터 시작)
        private Integer size;         // 페이지 크기
        private Integer totalPages;   // 전체 페이지 수
        private Long totalElements;   // 전체 요소 수
    }
}
