package com.dorandoran.chat.service.agent;

import java.util.List;

/**
 * IntimacyAnalysisAgent 분석 결과
 */
public record IntimacyAnalysisResult(
    int detectedLevel,
    List<ProblematicExpression> problematicExpressions
) {
    public IntimacyAnalysisResult {
        if (problematicExpressions == null) {
            problematicExpressions = List.of();
        }
    }
}


