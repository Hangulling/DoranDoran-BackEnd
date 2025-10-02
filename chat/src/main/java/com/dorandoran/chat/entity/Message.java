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
 * Chat 서비스 메시지 엔티티
 */
@Entity
@Table(name = "message", schema = "chat")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    
    @Id
    @Column(name = "messege_id")
    private UUID messegeId;
    
    @Column(name = "room_id", nullable = false)
    private UUID roomId;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(name = "bot_id", nullable = false)
    private UUID botId;
    
    @Column(name = "content", nullable = false, length = 100)
    private String content;
    
    @Column(name = "sender_type", nullable = false)
    private String senderType;
    
    @Column(name = "chat_num", nullable = false)
    private Integer chatNum;
    
    @Column(name = "message_send_time", nullable = false)
    private LocalDateTime messageSendTime;
    
    @Column(name = "message_type", nullable = false, length = 10)
    private String messageType;
    
    @Column(name = "message_meta", columnDefinition = "json")
    private String messageMeta;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
