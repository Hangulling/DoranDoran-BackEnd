package com.dorandoran.chat.service.dto;

import com.dorandoran.chat.entity.Chatbot;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.util.UUID;

/**
 * Chatbot 엔티티 캐싱용 DTO
 * 순환 참조 방지를 위해 필요한 필드만 포함
 */
@Getter
@AllArgsConstructor
@Builder
public class ChatbotCacheDto implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private UUID id;
    private String name;
    private String displayName;
    private String botType;
    
    public static ChatbotCacheDto from(Chatbot chatbot) {
        return ChatbotCacheDto.builder()
            .id(chatbot.getId())
            .name(chatbot.getName())
            .displayName(chatbot.getDisplayName())
            .botType(chatbot.getBotType())
            .build();
    }
}

