package com.dorandoran.user.dto;

import com.dorandoran.user.entity.UnreadPushLog;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UnreadPushResponse(
    Long id,
    String pushType,
    UUID chatbotId,
    UUID chatroomId,
    UUID messageId,
    String concept,
    String topic,
    String startMessage,
    String title,
    String body,
    OffsetDateTime sentAt
) {
    public static UnreadPushResponse from(UnreadPushLog log) {
        return new UnreadPushResponse(
            log.getId(),
            log.getPushType(),
            log.getChatbotId(),
            log.getChatroomId(),
            log.getMessageId(),
            log.getConcept(),
            log.getTopic(),
            log.getStartMessage(),
            log.getTitle(),
            log.getBody(),
            log.getSentAt()
        );
    }
}
