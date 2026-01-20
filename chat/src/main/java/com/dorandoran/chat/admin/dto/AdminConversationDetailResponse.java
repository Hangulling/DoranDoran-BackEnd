package com.dorandoran.chat.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * 관리자 대화 상세 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminConversationDetailResponse {
    private UUID conversationId;
    private List<AdminConversationMessageResponse> timeline;
}
