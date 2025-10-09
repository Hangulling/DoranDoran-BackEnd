package com.dorandoran.chat.service.agent;

/**
 * 대화 Agent 응답
 */
public record ConversationAgentResponse(
    String agentType,
    String response
) implements AgentResponse {
    public ConversationAgentResponse {
        if (agentType == null) agentType = "conversation";
    }
}
