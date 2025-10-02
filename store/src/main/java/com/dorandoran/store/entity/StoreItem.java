package com.dorandoran.store.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Store 서비스 저장소 아이템 엔티티
 */
@Entity
@Table(name = "store", schema = "store")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreItem {
    
    @Id
    @Column(name = "store_id")
    private UUID storeId;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(name = "messege_id", nullable = false)
    private UUID messegeId;
    
    @Column(name = "store_meta", columnDefinition = "json", nullable = false)
    private String storeMeta;
    
    @Column(name = "store_content", nullable = false)
    private String storeContent;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
