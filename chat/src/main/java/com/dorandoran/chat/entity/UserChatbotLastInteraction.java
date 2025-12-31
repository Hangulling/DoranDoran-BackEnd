package com.dorandoran.chat.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 사용자-챗봇 마지막 상호작용 엔티티
 */
@Entity
@Table(name = "user_chatbot_last_interaction", schema = "chat_schema")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(UserChatbotLastInteractionId.class)
public class UserChatbotLastInteraction {
    
    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Id
    @Column(name = "chatbot_id", nullable = false)
    private UUID chatbotId;
    
    @Column(name = "last_interaction_at")
    private OffsetDateTime lastInteractionAt;
    
    @Column(name = "last_room_id")
    private UUID lastRoomId;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}

