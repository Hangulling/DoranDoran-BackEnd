package com.dorandoran.user.admin.enums;

import lombok.Getter;

/**
 * 프롬프트 에이전트 타입
 */
@Getter
public enum AgentType {
    INTIMACY_ANALYSIS("intimacy/analysis"),
    INTIMACY_CORRECTION("intimacy/correction"),
    VOCABULARY_EXTRACTION("vocabulary/extraction"),
    VOCABULARY_EXPLANATION("vocabulary/explanation"),
    CONVERSATION("conversation"),
    GREETING("greeting");

    private final String filePathPrefix;

    AgentType(String filePathPrefix) {
        this.filePathPrefix = filePathPrefix;
    }

    /**
     * 파일 경로 생성
     * @param concept 컨셉 (소문자)
     * @param intimacyLevel 친밀도 레벨 (1 또는 3)
     * @return 파일 경로 (예: prompts/intimacy/analysis/friend_1.txt)
     */
    public String buildFilePath(String concept, int intimacyLevel) {
        return String.format("prompts/%s/%s_%d.txt", filePathPrefix, concept.toLowerCase(), intimacyLevel);
    }
}
