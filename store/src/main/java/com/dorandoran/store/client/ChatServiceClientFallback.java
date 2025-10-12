package com.dorandoran.store.client;

import com.dorandoran.store.client.dto.ChatRoomDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Chat Service Feign Client Fallback
 * Chat Service 호출 실패 시 대체 응답
 */
@Component
@Slf4j
public class ChatServiceClientFallback implements ChatServiceClient {

  @Override
  public ChatRoomDto getChatRoom(UUID chatroomId) {
    log.warn("Chat Service 호출 실패 - Fallback 실행: chatroomId={}", chatroomId);

    // Fallback: 채팅방 이름을 "Unknown"으로 반환
    return ChatRoomDto.builder()
        .id(chatroomId)
        .name("Unknown")
        .build();
  }
}