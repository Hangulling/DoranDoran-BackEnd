package com.dorandoran.user.dto;

import com.dorandoran.shared.dto.UserDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 마이페이지 통합 조회 응답 (유저 기본 정보 + 관심주제 + 알림설정 + 온보딩 설문)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MypageProfileResponse {

    /** 사용자 기본 정보 */
    private UserDto user;

    /** 관심 주제 목록 */
    private UserInterestsResponse interests;

    /** 알림 설정 (pushEnabled 등) */
    private NotificationSettingsResponse notificationSetting;

    /** 온보딩 설문 결과 (미제출 시 null) */
    private OnboardingSurveyResponse onboardingSurvey;
}
