package com.dorandoran.chat.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 친밀도 진척 추적 엔티티 (Multi-Agent AI)
 */
@Entity
@Table(name = "intimacy_progress", schema = "chat_schema")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntimacyProgress {
    
    @Id
    @Column(name = "id")
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chatroom_id", nullable = false)
    private ChatRoom chatRoom;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(name = "intimacy_level", nullable = false)
    private Integer intimacyLevel; // 1, 2, 3
    
    @Column(name = "total_corrections")
    private Integer totalCorrections;
    
    @Column(name = "last_feedback", columnDefinition = "text")
    private String lastFeedback;
    
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
    
    @Column(name = "progress_data", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String progressData;
}
