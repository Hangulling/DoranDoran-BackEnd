package com.dorandoran.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 온보딩 설문 엔티티 (유입 경로, 한국어 수준, 학습 목적)
 * 통합 온보딩 API에서 사용자당 1행으로 저장
 */
@Entity
@Table(name = "onboarding_survey", schema = "user_schema")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingSurvey {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "referral_source", length = 50)
    private String referralSource;

    @Column(name = "referral_other", length = 80)
    private String referralOther;

    @Column(name = "korean_level")
    private Integer koreanLevel;

    @Column(name = "purpose_key", length = 50)
    private String purposeKey;

    @Column(name = "purpose_other", length = 80)
    private String purposeOther;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
