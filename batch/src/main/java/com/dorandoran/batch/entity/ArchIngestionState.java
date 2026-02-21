package com.dorandoran.batch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Archive 적재 상태 엔티티
 */
@Entity
@Table(name = "arch_ingestion_state", schema = "archive_schema")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArchIngestionState {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @Column(name = "job_name", nullable = false, unique = true)
    private String jobName;
    
    @Column(name = "last_source_chatroom_id")
    private UUID lastSourceChatroomId;
    
    @Column(name = "last_source_message_id")
    private UUID lastSourceMessageId;
    
    @Column(name = "last_source_message_created_at")
    private LocalDateTime lastSourceMessageCreatedAt;
    
    @Column(name = "status", nullable = false)
    @Builder.Default
    private String status = "RUNNING";
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(name = "note", columnDefinition = "text")
    private String note;
}


