package com.dorandoran.user.dto;

import java.util.List;

/**
 * 안읽음 푸시 읽음 처리 요청.
 * ids: 읽음 처리할 id 배열 (null 또는 빈 리스트면 전체 읽음 처리)
 */
public record MarkReadRequest(
    List<Long> ids
) {
}
