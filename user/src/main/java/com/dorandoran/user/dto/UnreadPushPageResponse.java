package com.dorandoran.user.dto;

import com.dorandoran.user.entity.UnreadPushLog;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * GET /api/notifications/unread 페이지 응답 (명세: API_SPEC_PUSH_AND_DEEPLINK 2.3).
 */
public record UnreadPushPageResponse(
    List<UnreadPushResponse> content,
    long totalElements,
    int totalPages,
    int number,
    int size,
    long unreadCount
) {
    public static UnreadPushPageResponse fromPage(Page<UnreadPushLog> page) {
        List<UnreadPushResponse> items = page.getContent().stream()
            .map(UnreadPushResponse::from)
            .toList();
        long total = page.getTotalElements();
        return new UnreadPushPageResponse(
            items,
            total,
            page.getTotalPages(),
            page.getNumber(),
            page.getSize(),
            total
        );
    }
}
