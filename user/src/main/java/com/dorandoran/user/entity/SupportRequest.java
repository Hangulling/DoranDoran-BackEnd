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
 * 문의/신고 통합 엔티티
 */
@Entity
@Table(name = "support_requests", schema = "user_schema")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "requester_name", length = 100)
    private String requesterName;

    @Column(name = "requester_email", nullable = false, length = 320)
    private String requesterEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private SupportType type;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "reply_requested", nullable = false)
    private boolean replyRequested;

    @Column(name = "reply_email", length = 320)
    private String replyEmail;

    @Column(name = "chatroom_id")
    private UUID chatroomId;

    @Column(name = "message_id")
    private UUID messageId;

    @Column(name = "message_content", columnDefinition = "TEXT")
    private String messageContent;

    @Column(name = "ai_response_snapshot", columnDefinition = "jsonb")
    private String aiResponseSnapshot;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum SupportType {
        INQUIRY,
        REPORT
    }
}
