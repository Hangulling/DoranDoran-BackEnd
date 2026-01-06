package com.dorandoran.user.admin.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Archive Message Entity (읽기 전용)
 *
 * archive_schema.arch_messages 테이블
 * 채팅 메시지 아카이브 데이터
 */
@Entity
@Table(name = "arch_messages", schema = "archive_schema")
@Getter
@NoArgsConstructor
@Immutable  // 읽기 전용
public class ArchMessage {

  @Id
  @Column(columnDefinition = "uuid")
  private UUID id;

  @Column(name = "arch_chatroom_id", nullable = false, columnDefinition = "uuid")
  private UUID archChatroomId;

  @Column(name = "source_message_id", nullable = false, unique = true, columnDefinition = "uuid")
  private UUID sourceMessageId;

  @Column(name = "source_parent_message_id", columnDefinition = "uuid")
  private UUID sourceParentMessageId;

  @Column(name = "sender_type", nullable = false, length = 20)
  private String senderType;  // user, bot, system

  @Column(name = "sender_id", columnDefinition = "uuid")
  private UUID senderId;

  @Column(nullable = false, columnDefinition = "text")
  private String content;

  @Column(name = "content_type", nullable = false, length = 20)
  private String contentType = "text";

  @Column(name = "sequence_number", nullable = false)
  private Long sequenceNumber;

  @Column(name = "turn_number", nullable = false)
  private Long turnNumber = 0L;

  @Column(name = "token_count")
  private Integer tokenCount;

  @Column(name = "processing_time_ms")
  private Integer processingTimeMs;

  @Column(name = "is_edited", nullable = false)
  private Boolean isEdited = false;

  @Column(name = "edited_at")
  private LocalDateTime editedAt;

  @Column(name = "is_deleted", nullable = false)
  private Boolean isDeleted = false;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @Column(name = "source_created_at")
  private LocalDateTime sourceCreatedAt;

  @Column(name = "source_updated_at")
  private LocalDateTime sourceUpdatedAt;

  @Column(name = "archived_at", nullable = false)
  private LocalDateTime archivedAt;

  // JSONB 메타데이터
  @Column(name = "metadata_json", nullable = false, columnDefinition = "jsonb")
  private String metadataJson = "{}";
}