package com.dorandoran.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * 사용자 관심 주제 복합 키
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInterestTopicId implements Serializable {

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "topic_key", length = 50)
    private String topicKey;
}
