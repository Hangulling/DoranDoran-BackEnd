package com.dorandoran.chat.service.agent;

/**
 * Multi-Agent AI 응답 인터페이스
 */
public sealed interface AgentResponse permits 
    IntimacyAgentResponse, 
    VocabularyAgentResponse, 
    ConversationAgentResponse,
    TranslationAgentResponse {
    String agentType();
}
