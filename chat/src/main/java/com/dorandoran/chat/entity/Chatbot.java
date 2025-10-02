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
 * Chat 서비스 챗봇 엔티티
 */
@Entity
@Table(name = "chatbot", schema = "chat")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Chatbot {
    
    @Id
    @Column(name = "bot_id")
    private UUID botId;
    
    @Column(name = "bot_type", nullable = false, length = 50)
    private String botType;
    
    @Column(name = "intimacy", nullable = false)
    private Integer intimacy;
    
    @Column(name = "bot_img_url", nullable = false)
    private String botImgUrl;
    
    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;
    
    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
