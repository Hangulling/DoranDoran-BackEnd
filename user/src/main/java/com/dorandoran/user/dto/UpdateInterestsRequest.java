package com.dorandoran.user.dto;

import java.util.List;

/**
 * 관심 주제 변경 요청
 */
public record UpdateInterestsRequest(
    List<String> topicKeys
) {
}
