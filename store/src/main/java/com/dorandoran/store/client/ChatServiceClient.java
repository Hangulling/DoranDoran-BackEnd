package com.dorandoran.store.client;

import com.dorandoran.store.client.dto.ChatRoomDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Chat Service Feign Client
 * 채팅방 정보 조회
 */
@FeignClient(
    name = "chat-service",
    url = "${feign.chat-service.url:http://chat-service:8083}",
    fallback = ChatServiceClientFallback.class
)
public interface ChatServiceClient {

  /**
   * 채팅방 단건 조회
   */
  @GetMapping("/api/chat/chatrooms/{chatroomId}")
  ChatRoomDto getChatRoom(@PathVariable("chatroomId") UUID chatroomId);
}