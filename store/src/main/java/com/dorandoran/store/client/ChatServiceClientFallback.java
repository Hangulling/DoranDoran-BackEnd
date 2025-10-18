package com.dorandoran.store.client;

import com.dorandoran.store.client.dto.ChatRoomDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Chat Service Feign Client Fallback
 * Circuit Breaker 패턴
 */
@Component
@Slf4j
public class ChatServiceClientFallback implements ChatServiceClient {

  @Override
  public ChatRoomDto getChatRoom(UUID chatroomId, UUID userId) {
    log.warn("Chat Service 호출 실패 - Fallback 실행: chatroomId={}, userId={}",
        chatroomId, userId);

    // Fallback 응답: name = "Unknown"
    return ChatRoomDto.builder()
        .id(chatroomId)
        .name("Unknown")
        .build();
  }
}