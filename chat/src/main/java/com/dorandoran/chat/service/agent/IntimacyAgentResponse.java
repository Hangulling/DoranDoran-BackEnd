package com.dorandoran.chat.service.agent;

/**
 * 친밀도 분석 Agent 응답
 */
public record IntimacyAgentResponse(
    String agentType,
    int detectedLevel,
    String correctedSentence,
    FeedbackText feedback,
    String corrections
) implements AgentResponse {
    public IntimacyAgentResponse {
        if (agentType == null) agentType = "intimacy";
        if (feedback == null) feedback = new FeedbackText("", "");
        if (corrections == null) corrections = "";
    }
}
