package com.dorandoran.chat.service.agent;

/**
 * 어휘 추출 Agent의 결과
 */
public record VocabularyExtractionResult(
    String originalExpression,  // 원본 표현 (문장에서 추출한 그대로)
    String rootForm,            // 동사원형 (또는 기본형)
    VocabularyCategory category, // 어휘 카테고리
    int difficulty,            // 난이도 (1-3)
    String reason               // 추출 이유
) {
    public VocabularyExtractionResult {
        if (originalExpression == null) {
            throw new IllegalArgumentException("originalExpression cannot be null");
        }
        if (rootForm == null) {
            throw new IllegalArgumentException("rootForm cannot be null");
        }
        if (category == null) {
            throw new IllegalArgumentException("category cannot be null");
        }
        if (difficulty < 1 || difficulty > 3) {
            throw new IllegalArgumentException("difficulty must be between 1 and 3");
        }
        if (reason == null) {
            reason = "";
        }
    }
}

