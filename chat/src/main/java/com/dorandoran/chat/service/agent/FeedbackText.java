package com.dorandoran.chat.service.agent;

/**
 * 피드백 텍스트 (한국어/영어)
 */
public record FeedbackText(
    String ko,
    String en
) {
    public FeedbackText {
        if (ko == null) ko = "";
        if (en == null) en = "";
    }
}
