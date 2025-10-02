package com.dorandoran.user.repository;

import com.dorandoran.user.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    // 사용자 ID로 프로필 찾기
    UserProfile findByUserId(UUID userId);
}
