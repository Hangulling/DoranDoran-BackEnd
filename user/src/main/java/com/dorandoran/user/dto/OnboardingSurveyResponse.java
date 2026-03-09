package com.dorandoran.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 온보딩 설문 조회 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingSurveyResponse {

    private String referralSource;
    private String referralOther;
    private Integer koreanLevel;
    private String purposeKey;
    private String purposeOther;
}
