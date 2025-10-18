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
  private String correctedContent;
  private AiResponse aiResponse;
  private String botType;
  private LocalDateTime createdAt;
  private String message;

  // Entity -> DTO 변환
  public static BookmarkResponse from(Store store, String message) {
    return BookmarkResponse.builder()
        .id(store.getId())
        .messageId(store.getMessageId())
        .chatroomId(store.getChatroomId())
        .content(store.getContent())
        .correctedContent(store.getCorrectedContent())
        .aiResponse(store.getAiResponse())
        .botType(store.getBotType())
        .createdAt(store.getCreatedAt())
        .message(message)
        .build();
  }
}