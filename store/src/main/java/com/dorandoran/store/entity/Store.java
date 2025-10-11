package com.dorandoran.store.entity;

import com.dorandoran.store.config.JsonbConverter;
import com.dorandoran.store.dto.AiResponse;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Store Entity - 보관함
 * 사용자가 저장한 표현과 AI 응답을 관리
 */
@Entity
@Table(
    name = "stores",
    schema = "store_schema",
    indexes = {
        @Index(name = "idx_store_user_message", columnList = "user_id, message_id", unique = true),
        @Index(name = "idx_store_user_created", columnList = "user_id, created_at"),
        @Index(name = "idx_store_chatroom", columnList = "chatroom_id, created_at")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Store {

  // PK
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  // 관계 (MSA 원칙: UUID만 저장, FK 없음)
  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "message_id", nullable = false)
  private UUID messageId;

  @Column(name = "chatroom_id", nullable = false)
  private UUID chatroomId;

  // 표현 원본
  @Column(name = "content", columnDefinition = "text", nullable = false)
  private String content;

  // Multi-Agent AI 응답 (JSONB)
  @Column(name = "ai_response", columnDefinition = "jsonb", nullable = false)
  @Convert(converter = JsonbConverter.class)
  private AiResponse aiResponse;

  // 챗봇 역할 (Honey, Coworker, Senior, Client)
  @Column(name = "bot_type", length = 20)
  private String botType;

  // 소프트 삭제
  @Column(name = "is_deleted", nullable = false)
  @Builder.Default
  private Boolean isDeleted = false;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  // 타임스탬프
  @CreatedDate
  @Column(name = "created_at", updatable = false, nullable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;
}