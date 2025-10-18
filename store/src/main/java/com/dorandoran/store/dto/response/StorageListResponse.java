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
 * 보관함 목록 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageListResponse {

  private UUID id;
  private UUID messageId;
  private UUID chatroomId;
  private String chatroomName;  // Chat Service에서 조회 (Feign)
  private String content;
  private String correctedContent;
  private AiResponse aiResponse;
  private String botType;
  private LocalDateTime createdAt;

  // Entity -> DTO 변환
  public static StorageListResponse from(Store store) {
    return StorageListResponse.builder()
        .id(store.getId())
        .messageId(store.getMessageId())
        .chatroomId(store.getChatroomId())
        .content(store.getContent())
        .correctedContent(store.getCorrectedContent())
        .aiResponse(store.getAiResponse())
        .botType(store.getBotType())
        .createdAt(store.getCreatedAt())
        .build();
  }

  // Chatroom 이름 설정 (Feign Client 조회 후)
  public void setChatroomNameFromClient(String name) {
    this.chatroomName = name;
  }
}