package com.dorandoran.chat.service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LastInteractionResponse {
    private UUID chatbotId;
    private UUID lastRoomId;
    private OffsetDateTime lastInteractionAt;
    private String chatbotName; // 옵션
}


