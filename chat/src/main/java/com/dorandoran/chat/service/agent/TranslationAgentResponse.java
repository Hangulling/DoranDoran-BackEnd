package com.dorandoran.chat.service.agent;

import java.util.List;

/**
 * 번역 Agent 응답
 */
public record TranslationAgentResponse(
    String agentType,
    List<TranslatedWord> translations
) implements AgentResponse {
    public TranslationAgentResponse {
        if (agentType == null) agentType = "translation";
    }
    
    public record TranslatedWord(String original, String english, String pronunciation) {}
}
