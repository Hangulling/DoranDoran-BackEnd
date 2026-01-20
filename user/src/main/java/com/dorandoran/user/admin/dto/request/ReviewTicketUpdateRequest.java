package com.dorandoran.user.admin.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 관리 필요 내역 수정 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewTicketUpdateRequest {
    private String note;  // 메모
}
