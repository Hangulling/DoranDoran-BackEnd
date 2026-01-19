package com.dorandoran.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 관심 주제 마스터
 */
@Entity
@Table(name = "interest_topics", schema = "user_schema")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterestTopic {

    @Id
    @Column(name = "topic_key", length = 50)
    private String topicKey;

    @Column(name = "label", nullable = false, length = 100)
    private String label;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
