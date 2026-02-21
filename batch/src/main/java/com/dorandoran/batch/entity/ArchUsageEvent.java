package com.dorandoran.batch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Archive Usage Event 엔티티
 * billing.ai_usage_events의 모든 데이터를 아카이빙
 */
@Entity
@Table(name = "arch_usage_events", schema = "archive_schema")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArchUsageEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "arch_chatroom_id", nullable = false)
    private ArchChatroom archChatroom;
    
    @Column(name = "source_usage_event_id", nullable = false, unique = true)
    private UUID sourceUsageEventId;
    
    // 관계 (UUID만 저장, FK 없음)
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    // 이벤트 정보
    @Column(name = "event_time", nullable = false)
    private LocalDateTime eventTime;
    
    @Column(name = "provider", nullable = false)
    private String provider;
    
    @Column(name = "model", nullable = false)
    private String model;
    
    @Column(name = "request_id")
    private String requestId;
    
    // 토큰 및 비용
    @Column(name = "input_tokens", nullable = false)
    @Builder.Default
    private Integer inputTokens = 0;
    
    @Column(name = "output_tokens", nullable = false)
    @Builder.Default
    private Integer outputTokens = 0;
    
    @Column(name = "cost_in", nullable = false, precision = 18, scale = 6)
    @Builder.Default
    private BigDecimal costIn = BigDecimal.ZERO;
    
    @Column(name = "cost_out", nullable = false, precision = 18, scale = 6)
    @Builder.Default
    private BigDecimal costOut = BigDecimal.ZERO;
    
    // 메타데이터
    @Column(name = "meta", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode meta;
    
    // 타임스탬프
    @Column(name = "archived_at", nullable = false)
    private LocalDateTime archivedAt;
}


