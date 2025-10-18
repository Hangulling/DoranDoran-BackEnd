package com.dorandoran.chat.entity;

import com.dorandoran.chat.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Chat 서비스 챗봇 엔티티 (단순화/현대화된 스키마 매핑)
 */
@Entity
@Table(name = "chatbots", schema = "chat_schema")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Chatbot {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "description")
    private String description;

    @Column(name = "bot_type", nullable = false, length = 50)
    private String botType;

    @Column(name = "model_name", length = 100)
    private String modelName;

    @Column(name = "personality", columnDefinition = "jsonb")
    private String personality;

    @Column(name = "system_prompt")
    private String systemPrompt;

    // 각 Agent별 프롬프트 필드
    @Column(name = "intimacy_system_prompt")
    private String intimacySystemPrompt;

    @Column(name = "intimacy_user_prompt")
    private String intimacyUserPrompt;

    @Column(name = "vocabulary_system_prompt")
    private String vocabularySystemPrompt;

    @Column(name = "vocabulary_user_prompt")
    private String vocabularyUserPrompt;

    @Column(name = "translation_system_prompt")
    private String translationSystemPrompt;

    @Column(name = "translation_user_prompt")
    private String translationUserPrompt;

    @Column(name = "capabilities", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String capabilities;

    @Column(name = "settings", columnDefinition = "jsonb")
    private String settings;

    @Column(name = "intimacy_level")
    private Integer intimacyLevel; // 1=격식체, 2=부드러운 존댓말, 3=반말

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "is_active")
    private Boolean isActive;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;
    
    @OneToMany(mappedBy = "chatbot", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<ChatRoom> chatRooms = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
