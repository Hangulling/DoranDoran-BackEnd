package com.dorandoran.chat.service.agent;

import java.util.List;

/**
 * IntimacyAnalysisAgent 분석 결과
 */
public record IntimacyAnalysisResult(
    int detectedLevel,
    List<ProblematicExpression> problematicExpressions,
    String originalMessage  // 원본 사용자 메시지 (correction에서 사용)
) {
    public IntimacyAnalysisResult {
        if (problematicExpressions == null) {
            problematicExpressions = List.of();
        }
        if (originalMessage == null) {
            originalMessage = "";
        }
    }
    
    // 하위 호환성을 위한 생성자
    public IntimacyAnalysisResult(int detectedLevel, List<ProblematicExpression> problematicExpressions) {
        this(detectedLevel, problematicExpressions, "");
    }
}


