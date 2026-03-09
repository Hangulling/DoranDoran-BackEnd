package com.dorandoran.user.repository;

import com.dorandoran.user.entity.OnboardingSurvey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * 온보딩 설문 Repository
 */
@Repository
public interface OnboardingSurveyRepository extends JpaRepository<OnboardingSurvey, UUID> {

    Optional<OnboardingSurvey> findByUserId(UUID userId);
}
