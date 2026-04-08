package com.dorandoran.user.dto;

import java.util.List;

/**
 * POST /api/notifications/unread/mark-read 요청 본문.
 * ids가 null이거나 빈 배열이면 전체 읽음 처리.
 */
public record MarkReadBulkRequest(List<Long> ids) {
}
