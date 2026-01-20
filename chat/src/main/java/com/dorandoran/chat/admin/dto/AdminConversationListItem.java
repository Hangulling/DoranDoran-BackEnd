package com.dorandoran.chat.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 관리자 대화 목록 아이템 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminConversationListItem {
    private UUID conversationId;
    private UUID userId;
    private String roomKey;
    private Integer intimacyLevel;
    private LocalDateTime lastMessageAt;
    private Long lastSequenceNumber;
}
