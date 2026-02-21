package com.dorandoran.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 사용자 통계 (연속 접속/퍼펙트)
 */
@Entity
@Table(name = "user_stats", schema = "user_schema")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStats {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "streak_count", nullable = false)
    private int streakCount;

    @Column(name = "last_active_date")
    private LocalDate lastActiveDate;

    @Column(name = "perfect_count", nullable = false)
    private int perfectCount;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
