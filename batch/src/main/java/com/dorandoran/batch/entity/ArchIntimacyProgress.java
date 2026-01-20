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
 * Archive Intimacy Progress 엔티티
 * chat_schema.intimacy_progress의 모든 데이터를 아카이빙
 */
@Entity
@Table(name = "arch_intimacy_progress", schema = "archive_schema")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArchIntimacyProgress {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "arch_chatroom_id", nullable = false)
    private ArchChatroom archChatroom;
    
    @Column(name = "source_intimacy_progress_id", nullable = false, unique = true)
    private UUID sourceIntimacyProgressId;
    
    // 관계 (UUID만 저장, FK 없음)
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    // 친밀도 정보
    @Column(name = "intimacy_level", nullable = false)
    @Builder.Default
    private Integer intimacyLevel = 1;
    
    @Column(name = "total_corrections")
    @Builder.Default
    private Integer totalCorrections = 0;
    
    @Column(name = "last_feedback", columnDefinition = "text")
    private String lastFeedback;
    
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
    
    // 세부 학습 통계 (JSONB)
    @Column(name = "progress_data", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode progressData;
    
    // 타임스탬프
    @Column(name = "archived_at", nullable = false)
    private LocalDateTime archivedAt;
}


