package com.dorandoran.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 푸시 발송 로그 페이지 응답 (명세 5.4.3)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushDeliveryLogPageResponse {
    private List<PushDeliveryLogResponse> content;
    private PageInfo page;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageInfo {
        private int number;
        private int size;
        private int totalPages;
        private long totalElements;
    }
}
