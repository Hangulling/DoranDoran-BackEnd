package com.dorandoran.user.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 메인홈 게시글 응답 (v2) - mediaType, coverImageUrl, assets 확장
 */
public record PostResponseV2(
    String externalId,
    String title,
    String imageUrl,
    String description,
    String permalink,
    LocalDateTime publishedAt,
    String mediaType,
    String coverImageUrl,
    List<PostAssetResponse> assets
) {
}
