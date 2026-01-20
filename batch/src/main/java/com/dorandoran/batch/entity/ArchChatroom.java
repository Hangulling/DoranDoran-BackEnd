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
 * Archive 채팅방 엔티티
 */
@Entity
@Table(name = "arch_chatrooms", schema = "archive_schema")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArchChatroom {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;
    
    @Column(name = "source_chatroom_id", nullable = false, unique = true)
    private UUID sourceChatroomId;
    
    // 스냅샷 컬럼
    @Column(name = "user_id")
    private UUID userId;
    
    @Column(name = "user_email_snapshot", length = 320)
    private String userEmailSnapshot;
    
    @Column(name = "chatbot_id")
    private UUID chatbotId;
    
    @Column(name = "chatbot_name_snapshot", length = 100)
    private String chatbotNameSnapshot;
    
    @Column(name = "chatbot_type_snapshot", length = 20)
    private String chatbotTypeSnapshot;
    
    @Column(name = "chatbot_intimacy_level_snapshot")
    private Integer chatbotIntimacyLevelSnapshot;
    
    // 평면 컬럼
    @Column(name = "name", nullable = false, length = 100)
    private String name;
    
    @Column(name = "description", columnDefinition = "text")
    private String description;
    
    @Column(name = "concept", length = 50)
    private String concept;
    
    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;
    
    @Column(name = "source_last_message_id")
    private UUID sourceLastMessageId;
    
    @Column(name = "is_archived", nullable = false)
    @Builder.Default
    private Boolean isArchived = false;
    
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;
    
    @Column(name = "source_created_at")
    private LocalDateTime sourceCreatedAt;
    
    @Column(name = "source_updated_at")
    private LocalDateTime sourceUpdatedAt;
    
    @Column(name = "archived_at", nullable = false)
    private LocalDateTime archivedAt;
    
    @Column(name = "source_deleted_at")
    private LocalDateTime sourceDeletedAt;
    
    // Archive 확장 메타
    @Column(name = "meta", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode meta;
}

