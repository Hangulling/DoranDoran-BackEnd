package com.dorandoran.store.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Chat Service의 ChatRoom 응답 DTO
 * 필요한 필드만 매핑
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomDto {
  private UUID id;
  private String name;
}