package com.dorandoran.chat.service.agent;

/**
 * 어휘 설명 Agent의 결과
 */
public record VocabularyExplanationResult(
    String roma,  // 로마자 표기
    String ko,    // 한국어 설명 (컨셉/레벨에 맞는 말투)
    String en     // 영어 설명
) {
    public VocabularyExplanationResult {
        if (roma == null) {
            roma = "";
        }
        if (ko == null) {
            ko = "";
        }
        if (en == null) {
            en = "";
        }
    }
}

