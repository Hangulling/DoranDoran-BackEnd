package com.dorandoran.user.controller;

import com.dorandoran.common.response.ApiResponse;
import com.dorandoran.user.dto.NotificationSettingsResponse;
import com.dorandoran.user.dto.UpdateInterestsRequest;
import com.dorandoran.user.dto.UpdateNotificationSettingsRequest;
import com.dorandoran.user.dto.UserInterestsResponse;
import com.dorandoran.user.dto.UserStatsResponse;
import com.dorandoran.user.entity.UserNotificationSetting;
import com.dorandoran.user.entity.UserStats;
import com.dorandoran.user.service.InterestService;
import com.dorandoran.user.service.NotificationSettingService;
import com.dorandoran.user.service.UserStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserPreferenceController {

    private final InterestService interestService;
    private final NotificationSettingService notificationSettingService;
    private final UserStatsService userStatsService;

    @GetMapping("/{userId}/interests")
    public ResponseEntity<UserInterestsResponse> getUserInterests(@PathVariable String userId) {
        UUID uid = UUID.fromString(userId);
        return ResponseEntity.ok(new UserInterestsResponse(interestService.getUserInterests(uid)));
    }

    @PutMapping("/{userId}/interests")
    public ResponseEntity<UserInterestsResponse> updateUserInterests(
        @PathVariable String userId,
        @RequestBody UpdateInterestsRequest request
    ) {
        UUID uid = UUID.fromString(userId);
        return ResponseEntity.ok(new UserInterestsResponse(
            interestService.updateUserInterests(uid, request.topicKeys())
        ));
    }

    @GetMapping("/{userId}/notifications")
    public ResponseEntity<NotificationSettingsResponse> getNotificationSettings(@PathVariable String userId) {
        UUID uid = UUID.fromString(userId);
        UserNotificationSetting setting = notificationSettingService.getOrCreate(uid);
        return ResponseEntity.ok(new NotificationSettingsResponse(setting.isPushEnabled()));
    }

    @PutMapping("/{userId}/notifications")
    public ResponseEntity<NotificationSettingsResponse> updateNotificationSettings(
        @PathVariable String userId,
        @RequestBody UpdateNotificationSettingsRequest request
    ) {
        UUID uid = UUID.fromString(userId);
        boolean enabled = request.pushEnabled() != null && request.pushEnabled();
        UserNotificationSetting setting = notificationSettingService.updateSetting(uid, enabled);
        return ResponseEntity.ok(new NotificationSettingsResponse(setting.isPushEnabled()));
    }

    @GetMapping("/{userId}/stats")
    public ResponseEntity<UserStatsResponse> getUserStats(@PathVariable String userId) {
        UUID uid = UUID.fromString(userId);
        UserStats stats = userStatsService.touchStreak(uid, LocalDate.now());
        return ResponseEntity.ok(new UserStatsResponse(
            stats.getStreakCount(),
            stats.getPerfectCount(),
            stats.getLastActiveDate()
        ));
    }

    @PostMapping("/{userId}/stats/perfect")
    public ResponseEntity<ApiResponse<UserStatsResponse>> incrementPerfect(@PathVariable String userId) {
        UUID uid = UUID.fromString(userId);
        UserStats stats = userStatsService.incrementPerfect(uid);
        UserStatsResponse response = new UserStatsResponse(
            stats.getStreakCount(),
            stats.getPerfectCount(),
            stats.getLastActiveDate()
        );
        return ResponseEntity.ok(ApiResponse.success(response, "퍼펙트 횟수가 증가했습니다."));
    }
}
