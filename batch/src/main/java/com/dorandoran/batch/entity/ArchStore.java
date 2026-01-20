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
 * Archive Store 엔티티
 * store_schema.stores의 모든 데이터를 아카이빙
 */
@Entity
@Table(name = "arch_stores", schema = "archive_schema")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArchStore {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "arch_chatroom_id", nullable = false)
    private ArchChatroom archChatroom;
    
    @Column(name = "source_store_id", nullable = false, unique = true)
    private UUID sourceStoreId;
    
    @Column(name = "source_message_id", nullable = false)
    private UUID sourceMessageId;
    
    // 관계 (UUID만 저장, FK 없음)
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    // 표현 원본
    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;
    
    @Column(name = "corrected_content", columnDefinition = "text")
    private String correctedContent;
    
    // Multi-Agent AI 응답 (JSONB)
    @Column(name = "ai_response", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode aiResponse;
    
    // 챗봇 역할
    @Column(name = "bot_type", nullable = false, length = 20)
    private String botType;
    
    // 소프트 삭제
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;
    
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    
    // 타임스탬프
    @Column(name = "source_created_at")
    private LocalDateTime sourceCreatedAt;
    
    @Column(name = "source_updated_at")
    private LocalDateTime sourceUpdatedAt;
    
    @Column(name = "archived_at", nullable = false)
    private LocalDateTime archivedAt;
}


