package com.dorandoran.chat.service;

import com.dorandoran.chat.entity.Message;
import com.dorandoran.chat.entity.ChatRoom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Redis 캐싱 서비스
 * 메시지와 채팅방 정보를 캐싱하여 DB 부하 감소
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisCacheService {

  private final RedisTemplate<String, Object> redisTemplate;

  // 캐시 설정
  private static final Duration MESSAGE_CACHE_TTL = Duration.ofHours(1);
  private static final Duration ROOM_CACHE_TTL = Duration.ofMinutes(10);
  private static final Duration CONTEXT_CACHE_TTL = Duration.ofMinutes(30);
  private static final int MAX_CACHED_MESSAGES = 50;

  /**
   * 메시지 목록 캐시 조회
   */
  public List<Message> getCachedMessages(UUID chatroomId) {
    String key = "messages:" + chatroomId;
    try {
      List<Object> cached = redisTemplate.opsForList().range(key, 0, MAX_CACHED_MESSAGES - 1);
      if (cached != null && !cached.isEmpty()) {
        log.debug("메시지 캐시 히트: chatroomId={}, count={}", chatroomId, cached.size());
        return cached.stream()
            .map(obj -> (Message) obj)
            .collect(Collectors.toList());
      }
    } catch (Exception e) {
      log.error("메시지 캐시 조회 실패: chatroomId={}", chatroomId, e);
    }
    log.debug("메시지 캐시 미스: chatroomId={}", chatroomId);
    return new ArrayList<>();
  }

  /**
   * 메시지 목록 캐시 저장
   */
  public void cacheMessages(UUID chatroomId, List<Message> messages) {
    String key = "messages:" + chatroomId;
    try {
      // 기존 캐시 삭제
      redisTemplate.delete(key);

      // 최근 메시지만 캐싱 (최대 50개)
      List<Message> messagesToCache = messages.size() > MAX_CACHED_MESSAGES
          ? messages.subList(Math.max(0, messages.size() - MAX_CACHED_MESSAGES), messages.size())
          : messages;

      // Redis List에 저장
      messagesToCache.forEach(msg -> redisTemplate.opsForList().rightPush(key, msg));

      // TTL 설정
      redisTemplate.expire(key, MESSAGE_CACHE_TTL);

      log.debug("메시지 캐시 저장: chatroomId={}, count={}", chatroomId, messagesToCache.size());
    } catch (Exception e) {
      log.error("메시지 캐시 저장 실패: chatroomId={}", chatroomId, e);
    }
  }

  /**
   * 새 메시지를 캐시에 추가 (append)
   */
  public void appendMessageToCache(UUID chatroomId, Message message) {
    String key = "messages:" + chatroomId;
    try {
      // 캐시가 존재하는 경우에만 추가
      if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
        redisTemplate.opsForList().rightPush(key, message);

        // 최대 개수 유지 (오래된 것 삭제)
        Long size = redisTemplate.opsForList().size(key);
        if (size != null && size > MAX_CACHED_MESSAGES) {
          redisTemplate.opsForList().trim(key, -MAX_CACHED_MESSAGES, -1);
        }

        log.debug("메시지 캐시 추가: chatroomId={}, messageId={}", chatroomId, message.getId());
      }
    } catch (Exception e) {
      log.error("메시지 캐시 추가 실패: chatroomId={}", chatroomId, e);
    }
  }

  /**
   * 메시지 캐시 무효화
   */
  public void invalidateMessageCache(UUID chatroomId) {
    String key = "messages:" + chatroomId;
    try {
      redisTemplate.delete(key);
      log.debug("메시지 캐시 무효화: chatroomId={}", chatroomId);
    } catch (Exception e) {
      log.error("메시지 캐시 무효화 실패: chatroomId={}", chatroomId, e);
    }
  }

  /**
   * 채팅방 정보 캐시 조회
   */
  public ChatRoom getCachedChatRoom(UUID chatroomId) {
    String key = "chatroom:" + chatroomId;
    try {
      Object cached = redisTemplate.opsForValue().get(key);
      if (cached != null) {
        log.debug("채팅방 캐시 히트: chatroomId={}", chatroomId);
        return (ChatRoom) cached;
      }
    } catch (Exception e) {
      log.error("채팅방 캐시 조회 실패: chatroomId={}", chatroomId, e);
    }
    log.debug("채팅방 캐시 미스: chatroomId={}", chatroomId);
    return null;
  }

  /**
   * 채팅방 정보 캐시 저장
   */
  public void cacheChatRoom(ChatRoom chatRoom) {
    String key = "chatroom:" + chatRoom.getId();
    try {
      redisTemplate.opsForValue().set(key, chatRoom, ROOM_CACHE_TTL);
      log.debug("채팅방 캐시 저장: chatroomId={}", chatRoom.getId());
    } catch (Exception e) {
      log.error("채팅방 캐시 저장 실패: chatroomId={}", chatRoom.getId(), e);
    }
  }

  /**
   * 채팅방 캐시 무효화
   */
  public void invalidateChatRoomCache(UUID chatroomId) {
    String key = "chatroom:" + chatroomId;
    try {
      redisTemplate.delete(key);
      log.debug("채팅방 캐시 무효화: chatroomId={}", chatroomId);
    } catch (Exception e) {
      log.error("채팅방 캐시 무효화 실패: chatroomId={}", chatroomId, e);
    }
  }

  /**
   * 컨텍스트 데이터 캐시 (선택적)
   */
  public void cacheContext(UUID chatroomId, String contextKey, Object contextData) {
    String key = "context:" + chatroomId + ":" + contextKey;
    try {
      redisTemplate.opsForValue().set(key, contextData, CONTEXT_CACHE_TTL);
      log.debug("컨텍스트 캐시 저장: chatroomId={}, key={}", chatroomId, contextKey);
    } catch (Exception e) {
      log.error("컨텍스트 캐시 저장 실패: chatroomId={}", chatroomId, e);
    }
  }

  /**
   * 컨텍스트 데이터 조회
   */
  public Object getCachedContext(UUID chatroomId, String contextKey) {
    String key = "context:" + chatroomId + ":" + contextKey;
    try {
      return redisTemplate.opsForValue().get(key);
    } catch (Exception e) {
      log.error("컨텍스트 캐시 조회 실패: chatroomId={}", chatroomId, e);
      return null;
    }
  }
}