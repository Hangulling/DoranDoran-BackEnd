package com.dorandoran.user.repository;

import com.dorandoran.user.entity.UserSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserSettingRepository extends JpaRepository<UserSetting, Long> {
    // 사용자 ID로 설정 목록 찾기
    List<UserSetting> findByUserId(UUID userId);
    
    // 사용자 ID와 설정 키로 설정 찾기
    UserSetting findByUserIdAndSettingKey(UUID userId, String settingKey);
}
