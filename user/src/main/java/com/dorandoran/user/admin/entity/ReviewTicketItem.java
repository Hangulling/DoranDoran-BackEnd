package com.dorandoran.user.admin.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 관리 필요 내역 항목 Entity
 */
@Entity
@Table(name = "review_ticket_items", schema = "user_schema")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewTicketItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private ReviewTicket ticket;

    @Column(name = "message_id", columnDefinition = "uuid")
    private UUID messageId;

    @Column(name = "agent_type", nullable = false, length = 50)
    private String agentType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "snapshot_json", columnDefinition = "jsonb")
    private Map<String, Object> snapshotJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
