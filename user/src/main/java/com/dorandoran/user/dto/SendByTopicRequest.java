package com.dorandoran.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 주제 기반 푸시 발송 요청 (POST /api/notifications/send-by-topic)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendByTopicRequest {
    private UUID userId;
    private UUID chatbotId;
    private String topic;
    @Builder.Default
    private String concept = "FRIEND";
    @Builder.Default
    private Integer intimacyLevel = 1;
}
