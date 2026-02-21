package com.dorandoran.user.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 사용자 관심 주제 매핑
 */
@Entity
@Table(name = "user_interest_topics", schema = "user_schema")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInterestTopic {

    @EmbeddedId
    private UserInterestTopicId id;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
