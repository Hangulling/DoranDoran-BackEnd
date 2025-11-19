package com.dorandoran.chat.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Conversation Agent Excel 예시 데이터 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationExampleDto {
    private String concept; // Friend, Honey, Senior, Coworker, Boss
    private String situation; // 상황
    private Integer intimacyLevel; // Intimacy Level (1, 2, 3)
    private String userMessage; // User message
    private String botMessage; // Bot message
}


