package com.dorandoran.user.service;

import com.dorandoran.user.entity.UserNotificationSetting;
import com.dorandoran.user.repository.UserNotificationSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationSettingService {

    private final UserNotificationSettingRepository userNotificationSettingRepository;

    @Transactional(readOnly = true)
    public UserNotificationSetting getOrCreate(UUID userId) {
        return userNotificationSettingRepository.findById(userId)
            .orElseGet(() -> UserNotificationSetting.builder()
                .userId(userId)
                .pushEnabled(false)
                .build());
    }

    @Transactional
    public UserNotificationSetting updateSetting(UUID userId, boolean pushEnabled) {
        UserNotificationSetting setting = userNotificationSettingRepository.findById(userId)
            .orElseGet(() -> UserNotificationSetting.builder()
                .userId(userId)
                .pushEnabled(false)
                .build());
        setting.setPushEnabled(pushEnabled);
        return userNotificationSettingRepository.save(setting);
    }
}
