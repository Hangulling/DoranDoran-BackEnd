package com.dorandoran.chat.service.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

/**
 * 챗봇 수정 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotUpdateRequest {
    
    @NotBlank(message = "챗봇 ID는 필수입니다")
    private String chatbotId;
    
    private String systemPrompt;
    
    private String userPrompt;
    
    private Map<String, Object> metadata;
    
    private String agentType; // conversation, intimacy, vocabulary, translation
}
