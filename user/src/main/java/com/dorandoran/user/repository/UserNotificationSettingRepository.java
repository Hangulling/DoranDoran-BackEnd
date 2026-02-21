package com.dorandoran.user.repository;

import com.dorandoran.user.entity.UserNotificationSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserNotificationSettingRepository extends JpaRepository<UserNotificationSetting, UUID> {
    List<UserNotificationSetting> findByPushEnabledTrue();
}
