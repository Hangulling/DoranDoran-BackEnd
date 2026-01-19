package com.dorandoran.user.admin.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 관리 필요 내역 처리 완료 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewTicketCompleteRequest {
    private List<Long> ticketIds;  // 처리 완료할 티켓 ID 목록
}
