package com.dorandoran.user.admin.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Archive Chatroom Entity (읽기 전용)
 *
 * archive_schema.arch_chatrooms 테이블
 * 채팅방 아카이브 데이터
 */
@Entity
@Table(name = "arch_chatrooms", schema = "archive_schema")
@Getter
@NoArgsConstructor
@Immutable  // 읽기 전용
public class ArchChatroom {

  @Id
  @Column(columnDefinition = "uuid")
  private UUID id;

  @Column(name = "source_chatroom_id", nullable = false, unique = true, columnDefinition = "uuid")
  private UUID sourceChatroomId;

  // 스냅샷 컬럼
  @Column(name = "user_id", columnDefinition = "uuid")
  private UUID userId;

  @Column(name = "user_email_snapshot", length = 320)
  private String userEmailSnapshot;

  @Column(name = "chatbot_id", columnDefinition = "uuid")
  private UUID chatbotId;

  @Column(name = "chatbot_name_snapshot", length = 100)
  private String chatbotNameSnapshot;

  @Column(name = "chatbot_type_snapshot", length = 20)
  private String chatbotTypeSnapshot;

  @Column(name = "chatbot_intimacy_level_snapshot")
  private Integer chatbotIntimacyLevelSnapshot;

  // 운영 컬럼
  @Column(nullable = false, length = 100)
  private String name;

  @Column(columnDefinition = "text")
  private String description;

  @Column(length = 50)
  private String concept;

  @Column(name = "last_message_at")
  private LocalDateTime lastMessageAt;

  @Column(name = "source_last_message_id", columnDefinition = "uuid")
  private UUID sourceLastMessageId;

  @Column(name = "is_archived", nullable = false)
  private Boolean isArchived = false;

  @Column(name = "is_deleted", nullable = false)
  private Boolean isDeleted = false;

  @Column(name = "source_created_at")
  private LocalDateTime sourceCreatedAt;

  @Column(name = "source_updated_at")
  private LocalDateTime sourceUpdatedAt;

  @Column(name = "archived_at", nullable = false)
  private LocalDateTime archivedAt;

  @Column(name = "source_deleted_at")
  private LocalDateTime sourceDeletedAt;

  // JSONB 메타데이터
  @Column(nullable = false, columnDefinition = "jsonb")
  private String meta = "{}";
}