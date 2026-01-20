package com.dorandoran.user.dto;

import java.time.LocalDate;

/**
 * 사용자 통계 응답
 */
public record UserStatsResponse(
    int streakCount,
    int perfectCount,
    LocalDate lastActiveDate
) {
}
