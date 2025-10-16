package com.dorandoran.store.client;

import com.dorandoran.store.client.dto.ChatRoomDto;
import java.util.Map;
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

  @Override
  public Map<String, Object> archiveMessage(UUID chatroomId, UUID messageId, UUID userId) {
    log.error("Chat Service 보관함 저장 API 호출 실패 - Fallback 실행: chatroomId={}, messageId={}, userId={}",
        chatroomId, messageId, userId);

    // 보관함 저장 실패 시 예외 발생 (사용자에게 알림 필요)
    throw new RuntimeException("메시지 보관함 저장에 실패했습니다. Chat Service에 연결할 수 없습니다.");
  }

  @Override
  public void deleteMessage(UUID messageId, UUID userId) {
    log.error("Chat Service 메시지 삭제 API 호출 실패 - Fallback 실행: messageId={}, userId={}",
        messageId, userId);

    // 삭제 실패는 로그만 남김 - Store는 이미 소프트 삭제 처리
    // TODO:Chat Service와 동기화는 나중에 재시도 메커니즘으로 처리
  }
}