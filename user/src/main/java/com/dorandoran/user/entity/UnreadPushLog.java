package com.dorandoran.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 안읽음 푸시 로그.
 * 푸시 발송 시 insert, 읽음 처리 시 read_at 업데이트.
 * read_at이 null이면 안읽음.
 */
@Entity
@Table(name = "unread_push_logs", schema = "user_schema")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnreadPushLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "push_type", nullable = false, length = 20)
    private String pushType;

    @Column(name = "chatbot_id")
    private UUID chatbotId;

    @Column(name = "chatroom_id")
    private UUID chatroomId;

    @Column(name = "message_id")
    private UUID messageId;

    @Column(name = "concept", length = 20)
    private String concept;

    @Column(name = "topic", length = 255)
    private String topic;

    @Column(name = "start_message", columnDefinition = "TEXT")
    private String startMessage;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    @Column(name = "sent_at", nullable = false)
    private OffsetDateTime sentAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "read_at")
    private OffsetDateTime readAt;

    public boolean isUnread() {
        return readAt == null;
    }
}
