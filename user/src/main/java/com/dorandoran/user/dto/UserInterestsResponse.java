package com.dorandoran.user.dto;

import java.util.List;

/**
 * 사용자 관심 주제 응답
 */
public record UserInterestsResponse(
    List<InterestTopicDto> topics
) {
}
