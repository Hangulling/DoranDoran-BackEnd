package com.dorandoran.store.dto.response;

import com.dorandoran.store.dto.AiResponse;
import com.dorandoran.store.entity.Store;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 표현 보관 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookmarkResponse {

  private UUID id;
  private UUID messageId;
  private UUID chatroomId;
  private String content;
  private AiResponse aiResponse;
  private String intimacyTag;
  private LocalDateTime createdAt;
  private String message;  // 성공 메시지

  // Entity -> DTO 변환
  public static BookmarkResponse from(Store store, String message) {
    return BookmarkResponse.builder()
        .id(store.getId())
        .messageId(store.getMessageId())
        .chatroomId(store.getChatroomId())
        .content(store.getContent())
        .aiResponse(store.getAiResponse())
        .intimacyTag(store.getIntimacyTag())
        .createdAt(store.getCreatedAt())
        .message(message)
        .build();
  }
}