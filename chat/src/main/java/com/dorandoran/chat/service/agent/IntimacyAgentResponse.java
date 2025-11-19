package com.dorandoran.chat.service.agent;

import java.util.List;

/**
 * 친밀도 분석 Agent 응답
 */
public record IntimacyAgentResponse(
    String agentType,
    int detectedLevel,
    String correctedSentence,
    FeedbackText feedback,
    String corrections,
    List<AlternativeExpression> alternativeExpressions
) implements AgentResponse {
    public IntimacyAgentResponse {
        if (agentType == null) agentType = "intimacy";
        if (feedback == null) feedback = new FeedbackText("", "");
        if (corrections == null) corrections = "";
        if (alternativeExpressions == null) alternativeExpressions = List.of();
    }
    
    /**
     * 하위 호환성을 위한 생성자 (alternativeExpressions 없이)
     */
    public IntimacyAgentResponse(
        String agentType,
        int detectedLevel,
        String correctedSentence,
        FeedbackText feedback,
        String corrections
    ) {
        this(agentType, detectedLevel, correctedSentence, feedback, corrections, List.of());
    }
}
