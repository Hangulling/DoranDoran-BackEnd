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
 * Archive 메시지 엔티티
 */
@Entity
@Table(name = "arch_messages", schema = "archive_schema")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArchMessage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "arch_chatroom_id", nullable = false)
    private ArchChatroom archChatroom;
    
    @Column(name = "source_message_id", nullable = false, unique = true)
    private UUID sourceMessageId;
    
    @Column(name = "source_parent_message_id")
    private UUID sourceParentMessageId;
    
    @Column(name = "sender_type", nullable = false, length = 20)
    private String senderType;
    
    @Column(name = "sender_id")
    private UUID senderId;
    
    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;
    
    @Column(name = "content_type", nullable = false, length = 20)
    @Builder.Default
    private String contentType = "text";
    
    @Column(name = "sequence_number", nullable = false)
    private Long sequenceNumber;
    
    @Column(name = "turn_number", nullable = false)
    @Builder.Default
    private Long turnNumber = 0L;
    
    @Column(name = "token_count")
    private Integer tokenCount;
    
    @Column(name = "processing_time_ms")
    private Integer processingTimeMs;
    
    @Column(name = "is_edited", nullable = false)
    @Builder.Default
    private Boolean isEdited = false;
    
    @Column(name = "edited_at")
    private LocalDateTime editedAt;
    
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;
    
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    
    @Column(name = "source_created_at")
    private LocalDateTime sourceCreatedAt;
    
    @Column(name = "source_updated_at")
    private LocalDateTime sourceUpdatedAt;
    
    @Column(name = "archived_at", nullable = false)
    private LocalDateTime archivedAt;
    
    // Archive 확장 메타
    @Column(name = "metadata_json", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode metadataJson;
}

