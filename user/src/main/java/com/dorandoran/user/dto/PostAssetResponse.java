package com.dorandoran.user.dto;

/**
 * 캐러셀/비디오 확장용 에셋 한 건 (이미지 또는 비디오)
 */
public record PostAssetResponse(
    String type,   // "IMAGE" | "VIDEO"
    String url,
    String thumbnailUrl  // VIDEO일 때 권장, optional
) {
}
