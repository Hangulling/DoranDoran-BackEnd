package com.dorandoran.chat.messaging;

import com.dorandoran.chat.event.MessageEvent;
import com.dorandoran.chat.event.SSEEvent;
import com.dorandoran.chat.sse.SSEManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Redis 메시지 구독자
 * Redis Pub/Sub 메시지를 수신하여 SSE로 전달
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisMessageSubscriber implements MessageListener {

  private final SSEManager sseManager;
  private final ObjectMapper objectMapper;

  /**
   * Redis 메시지 수신 핸들러
   */
  @Override
  public void onMessage(Message message, byte[] pattern) {
    try {
      String channel = new String(message.getChannel());
      String body = new String(message.getBody());

      log.debug("Redis 메시지 수신: channel={}", channel);

      if (channel.startsWith("chat:sse:")) {
        handleSSEEvent(channel, body);
      } else if (channel.startsWith("chat:room:")) {
        handleMessageEvent(channel, body);
      } else {
        log.warn("알 수 없는 채널 형식: {}", channel);
      }
    } catch (Exception e) {
      log.error("Redis 메시지 처리 오류", e);
    }
  }

  /**
   * SSE 이벤트 처리
   */
  private void handleSSEEvent(String channel, String body) {
    try {
      SSEEvent event = objectMapper.readValue(body, SSEEvent.class);
      UUID chatroomId = extractChatroomIdFromChannel(channel);

      // SSEManager를 통해 연결된 클라이언트들에게 전송
      sseManager.send(chatroomId, event.getEventType(), event.getData());

      log.debug("SSE 이벤트 처리 완료: chatroomId={}, eventType={}",
          chatroomId, event.getEventType());
    } catch (Exception e) {
      log.error("SSE 이벤트 처리 실패: channel={}, body={}", channel, body, e);
    }
  }

  /**
   * 메시지 이벤트 처리
   */
  private void handleMessageEvent(String channel, String body) {
    try {
      MessageEvent event = objectMapper.readValue(body, MessageEvent.class);

      // 메시지 이벤트를 SSE로 전송
      sseManager.send(event.getChatroomId(), "new_message", event);

      log.debug("메시지 이벤트 처리 완료: messageId={}, chatroomId={}",
          event.getMessageId(), event.getChatroomId());
    } catch (Exception e) {
      log.error("메시지 이벤트 처리 실패: channel={}, body={}", channel, body, e);
    }
  }

  /**
   * 채널명에서 채팅방 ID 추출
   *
   * @param channel 채널명 (chat:sse:{chatroomId} 또는 chat:room:{chatroomId})
   * @return 채팅방 ID
   */
  private UUID extractChatroomIdFromChannel(String channel) {
    String[] parts = channel.split(":");
    if (parts.length >= 3) {
      try {
        return UUID.fromString(parts[2]);
      } catch (IllegalArgumentException e) {
        log.error("채널에서 UUID 파싱 실패: {}", channel, e);
        throw new IllegalArgumentException("Invalid channel format: " + channel);
      }
    }
    throw new IllegalArgumentException("Invalid channel format: " + channel);
  }
}