package com.dorandoran.chat.service.agent;

import java.util.List;

/**
 * 친밀도 분석 Agent 응답
 */
public record IntimacyAgentResponse(
    String agentType,
    int detectedLevel,
    String correctedSentence,
    String feedback,
    List<String> corrections
) implements AgentResponse {
    public IntimacyAgentResponse {
        if (agentType == null) agentType = "intimacy";
    }
}
