package com.dorandoran.batch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Archive Agent 결과 엔티티
 */
@Entity
@Table(name = "arch_agent_results", schema = "archive_schema")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArchAgentResult {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "arch_message_id", nullable = false)
    private ArchMessage archMessage;
    
    @Column(name = "agent_type", nullable = false, length = 20)
    private String agentType; // intimacy, conver, voca
    
    @Column(name = "payload_json", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode payloadJson;
    
    @Column(name = "request_id")
    private String requestId;
    
    @Column(name = "provider")
    private String provider;
    
    @Column(name = "model")
    private String model;
    
    @Column(name = "latency_ms")
    private Integer latencyMs;
    
    @Column(name = "input_tokens")
    private Integer inputTokens;
    
    @Column(name = "output_tokens")
    private Integer outputTokens;
    
    @Column(name = "source_created_at")
    private LocalDateTime sourceCreatedAt;
    
    @Column(name = "archived_at", nullable = false)
    private LocalDateTime archivedAt;
}

