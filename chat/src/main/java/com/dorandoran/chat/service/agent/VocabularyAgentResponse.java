package com.dorandoran.chat.service.agent;

import java.util.List;

/**
 * 어휘 추출 Agent 응답
 */
public record VocabularyAgentResponse(
    String agentType,
    List<VocabularyWord> words
) implements AgentResponse {
    public VocabularyAgentResponse {
        if (agentType == null) agentType = "vocabulary";
    }
    
    public record VocabularyWord(String word, int difficulty, String context) {}
}
