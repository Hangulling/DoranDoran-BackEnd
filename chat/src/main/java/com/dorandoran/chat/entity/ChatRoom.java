package com.dorandoran.chat.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Chat 서비스 채팅방 엔티티
 */
@Entity
@Table(name = "chatroom", schema = "chat")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoom {
    
    @Id
    @Column(name = "room_id")
    private UUID roomId;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(name = "bot_id", nullable = false)
    private UUID botId;
    
    @Column(name = "room_name", nullable = false, length = 50)
    private String roomName;
    
    @Column(name = "settings", columnDefinition = "json", nullable = false)
    private String settings;
    
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = true;
    
    @CreationTimestamp
    @Column(name = "create_at", nullable = false, updatable = false)
    private LocalDateTime createAt;
    
    @UpdateTimestamp
    @Column(name = "update_at")
    private LocalDateTime updateAt;
}
