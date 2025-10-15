package com.dorandoran.chat.messaging;

import com.dorandoran.chat.event.MessageEvent;
import com.dorandoran.chat.event.SSEEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Redis 메시지 발행자
 * 채팅 메시지와 SSE 이벤트를 Redis Pub/Sub으로 발행
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisMessagePublisher {

  private final RedisTemplate<String, Object> redisTemplate;

  /**
   * 채팅 메시지 발행
   *
   * @param event 메시지 이벤트
   */
  public void publishMessage(MessageEvent event) {
    String channel = "chat:room:" + event.getChatroomId();

    try {
      redisTemplate.convertAndSend(channel, event);
      log.info("메시지 발행 성공: channel={}, messageId={}, senderType={}",
          channel, event.getMessageId(), event.getSenderType());
    } catch (Exception e) {
      log.error("메시지 발행 실패: channel={}, messageId={}",
          channel, event.getMessageId(), e);
    }
  }

  /**
   * SSE 이벤트 발행
   *
   * @param chatroomId 채팅방 ID
   * @param eventType 이벤트 타입
   * @param data 이벤트 데이터
   */
  public void publishSSEEvent(UUID chatroomId, String eventType, Object data) {
    String channel = "chat:sse:" + chatroomId;
    SSEEvent event = new SSEEvent(chatroomId, eventType, data, System.currentTimeMillis());

    try {
      redisTemplate.convertAndSend(channel, event);
      log.debug("SSE 이벤트 발행 성공: channel={}, eventType={}", channel, eventType);
    } catch (Exception e) {
      log.error("SSE 이벤트 발행 실패: channel={}, eventType={}", channel, eventType, e);
    }
  }

  /**
   * 여러 채팅방에 브로드캐스트
   *
   * @param chatroomIds 채팅방 ID 목록
   * @param eventType 이벤트 타입
   * @param data 이벤트 데이터
   */
  public void broadcastSSEEvent(java.util.List<UUID> chatroomIds, String eventType, Object data) {
    for (UUID chatroomId : chatroomIds) {
      publishSSEEvent(chatroomId, eventType, data);
    }
    log.info("SSE 브로드캐스트 완료: rooms={}, eventType={}", chatroomIds.size(), eventType);
  }
}