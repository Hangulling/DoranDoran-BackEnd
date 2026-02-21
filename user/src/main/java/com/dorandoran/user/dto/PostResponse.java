package com.dorandoran.user.dto;

import java.time.LocalDateTime;

/**
 * 메인홈 게시글 응답
 */
public record PostResponse(
    String externalId,
    String title,
    String imageUrl,
    String description,
    String permalink,
    LocalDateTime publishedAt
) {
}
