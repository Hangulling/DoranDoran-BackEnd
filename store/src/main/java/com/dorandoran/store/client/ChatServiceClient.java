package com.dorandoran.store.client;

import com.dorandoran.store.client.dto.ChatRoomDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Chat Service Feign Client
 * 채팅과 보관함 연결
 */
@FeignClient(
    name = "chat-service",
    url = "${feign.chat-service.url:http://localhost:8083}",
    fallback = ChatServiceClientFallback.class
)
public interface ChatServiceClient {

  /**
   * 채팅방 정보 조회
   * @param chatroomId 채팅방 ID
   * @param userId 사용자 ID (권한 체크용)
   * @return 채팅방 정보
   */
  @GetMapping("/api/chat/chatrooms/{chatroomId}")
  ChatRoomDto getChatRoom(
      @PathVariable("chatroomId") UUID chatroomId,
      @RequestParam("userId") UUID userId
  );
}
