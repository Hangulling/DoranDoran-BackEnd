package com.dorandoran.store.client;

import com.dorandoran.store.client.dto.ChatRoomDto;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

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

  /**
   * 메시지 보관함 저장 (Redis → DB)
   */
  @PostMapping("/api/chat/messages/archive")
  Map<String, Object> archiveMessage(
      @RequestParam("chatroomId") UUID chatroomId,
      @RequestParam("messageId") UUID messageId,
      @RequestHeader("X-User-Id") UUID userId
  );

  /**
   * 메시지 소프트 삭제 (보관함 해제)
   */
  @DeleteMapping("/api/chat/messages/{messageId}")
  void deleteMessage(
      @PathVariable("messageId") UUID messageId,
      @RequestHeader("X-User-Id") UUID userId
  );
}