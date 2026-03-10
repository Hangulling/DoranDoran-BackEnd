package com.dorandoran.user.dto;

import java.util.UUID;

/**
 * 테스트용 '푸시 클릭 → 채팅방 생성' 연동 API 요청.
 * 채팅방 생성용 payload를 담은 푸시 1회 발송.
 */
public record TestChatroomPushRequest(
    UUID userId,
    UUID chatbotId,
    String topic,
    String concept,
    Integer intimacyLevel,
    String title,
    String body
) {
}
