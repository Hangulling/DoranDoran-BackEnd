package com.dorandoran.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 온보딩 통합 제출 요청 DTO
 * 모든 필드 선택. Body 없으면 기존처럼 is_onboard만 true로 변경.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingSubmitRequest {

    /** 관심 주제 키 목록 (interest_topics 마스터에 존재하는 키) */
    private List<String> topicKeys;

    /** 푸시 알림 허용 여부 */
    private Boolean pushEnabled;

    /** 유입 경로: ads, instagram_contents, instagram_reels, facebook_contents, friend, other */
    @Size(max = 50)
    private String referralSource;

    /** 유입 경로 기타 (최대 80자) */
    @Size(max = 80)
    private String referralOther;

    /** 한국어 수준 1~5 */
    @Min(1)
    @Max(5)
    private Integer koreanLevel;

    /** 학습 목적: casual_chats, dating, workplace, school, other */
    @Size(max = 50)
    private String purposeKey;

    /** 학습 목적 기타 (최대 80자) */
    @Size(max = 80)
    private String purposeOther;
}
