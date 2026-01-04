package com.dorandoran.chat.service.agent;

/**
 * 교정 정보
 */
public record Correction(
    String from,  // 원래 문제 표현
    String to,    // 교정된 표현
    String reason // 왜 이렇게 고쳤는지 한국어로 한 문장 설명
) {
}

