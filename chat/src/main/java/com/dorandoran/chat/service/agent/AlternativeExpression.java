package com.dorandoran.chat.service.agent;

/**
 * 대안 표현 정보
 */
public record AlternativeExpression(
    String expression,
    String tone,  // "반말", "존댓말"
    String example
) {
}


