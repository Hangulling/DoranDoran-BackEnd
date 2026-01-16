package com.dorandoran.chat.service.agent;

/**
 * 문제가 되는 표현 정보
 */
public record ProblematicExpression(
    String original,
    String type,  // INFORMAL_TO_FORMAL, FORMAL_TO_INFORMAL, STYLE_MISMATCH
    String reason,
    String suggestionHint  // 교정에 참고할 짧은 예시 표현 (선택적)
) {
    public ProblematicExpression {
        if (suggestionHint == null) {
            suggestionHint = "";
        }
    }
}


