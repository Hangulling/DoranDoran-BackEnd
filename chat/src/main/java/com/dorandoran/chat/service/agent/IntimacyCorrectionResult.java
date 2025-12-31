package com.dorandoran.chat.service.agent;

import java.util.List;

/**
 * IntimacyCorrectionAgent 교정 결과
 */
public record IntimacyCorrectionResult(
    String correctedSentence,
    FeedbackText feedback,
    String corrections,
    List<AlternativeExpression> alternativeExpressions
) {
    public IntimacyCorrectionResult {
        if (feedback == null) {
            feedback = new FeedbackText("", "");
        }
        if (corrections == null) {
            corrections = "";
        }
        if (alternativeExpressions == null) {
            alternativeExpressions = List.of();
        }
    }
}


