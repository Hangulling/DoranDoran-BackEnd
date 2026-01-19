package com.dorandoran.user.admin.entity;

import com.dorandoran.user.admin.enums.ReviewStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 관리 필요 내역 Entity
 */
@Entity
@Table(name = "review_tickets", schema = "user_schema")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "conversation_id", columnDefinition = "uuid")
    private UUID conversationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ReviewStatus status = ReviewStatus.OPEN;

    @Column(name = "agent_type", length = 50)
    private String agentType;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_by", columnDefinition = "uuid")
    private UUID createdBy;

    @Column(name = "assignee", columnDefinition = "uuid")
    private UUID assignee;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "done_at")
    private LocalDateTime doneAt;
}
