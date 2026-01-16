package com.dorandoran.chat.service.agent;

import java.util.List;

/**
 * IntimacyCorrectionAgent 교정 결과
 */
public record IntimacyCorrectionResult(
    String correctedSentence,
    FeedbackText feedback,
    List<Correction> corrections,  // 배열로 변경
    List<AlternativeExpression> alternativeExpressions
) {
    public IntimacyCorrectionResult {
        if (feedback == null) {
            feedback = new FeedbackText("", "");
        }
        if (corrections == null) {
            corrections = List.of();
        }
        if (alternativeExpressions == null) {
            alternativeExpressions = List.of();
        }
    }
    
    // 하위 호환성을 위한 생성자
    public IntimacyCorrectionResult(String correctedSentence, FeedbackText feedback, String corrections, List<AlternativeExpression> alternativeExpressions) {
        this(correctedSentence, feedback, List.of(), alternativeExpressions);
    }
}


