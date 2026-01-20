package com.dorandoran.user.dto;

import java.time.LocalDateTime;

/**
 * 문의/신고 등록 응답
 */
public record SupportCreateResponse(
    Long id,
    LocalDateTime createdAt
) {
}
