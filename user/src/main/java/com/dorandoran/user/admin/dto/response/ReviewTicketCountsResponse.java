package com.dorandoran.user.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 관리 필요 내역 카운트 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewTicketCountsResponse {
    private Long total;  // 전체 카운트
    private Map<String, Long> byAgentType;  // 에이전트 타입별 카운트
}
