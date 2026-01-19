package com.dorandoran.user.admin.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Archive Agent Result Entity (읽기 전용)
 *
 * archive_schema.arch_agent_results 테이블
 * Agent 실행 결과 (intimacy, conver, voca)
 */
@Entity
@Table(name = "arch_agent_results", schema = "archive_schema")
@Getter
@NoArgsConstructor
@Immutable  // 읽기 전용
public class ArchAgentResult {

  @Id
  @Column(columnDefinition = "uuid")
  private UUID id;

  @Column(name = "arch_message_id", nullable = false, columnDefinition = "uuid")
  private UUID archMessageId;

  @Column(name = "agent_type", nullable = false, length = 20)
  private String agentType;  // intimacy, conver, voca

  // JSONB 결과 데이터
  @Column(name = "payload_json", nullable = false, columnDefinition = "jsonb")
  private String payloadJson;

  // 추적 정보
  @Column(name = "request_id", columnDefinition = "text")
  private String requestId;

  @Column(columnDefinition = "text")
  private String provider;

  @Column(columnDefinition = "text")
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