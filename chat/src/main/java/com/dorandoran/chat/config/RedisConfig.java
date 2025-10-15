package com.dorandoran.chat.config;

import com.dorandoran.chat.messaging.RedisMessageSubscriber;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis Pub/Sub 설정
 * 실시간 메시징 및 캐싱을 위한 Redis 구성
 */
@Configuration
@RequiredArgsConstructor
public class RedisConfig {

  private final RedisMessageSubscriber redisMessageSubscriber;

  /**
   * Redis 연결 팩토리
   * localhost:6379 기본 연결
   */
  @Bean
  public RedisConnectionFactory redisConnectionFactory() {
    return new LettuceConnectionFactory("localhost", 6379);
  }

  /**
   * RedisTemplate 설정
   * 메시지 발행/구독 및 캐싱에 사용
   */
  @Bean
  public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);

    // Key는 String으로 직렬화
    template.setKeySerializer(new StringRedisSerializer());
    // Value는 JSON으로 직렬화 (Object 저장 가능)
    template.setValueSerializer(new GenericJackson2JsonRedisSerializer());

    // Hash 자료구조용 직렬화
    template.setHashKeySerializer(new StringRedisSerializer());
    template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

    return template;
  }

  /**
   * Redis 메시지 리스너 컨테이너
   * Pub/Sub 구독을 관리
   */
  @Bean
  public RedisMessageListenerContainer redisMessageListenerContainer(
      RedisConnectionFactory connectionFactory) {
    RedisMessageListenerContainer container = new RedisMessageListenerContainer();
    container.setConnectionFactory(connectionFactory);

    // chat:room:* 패턴으로 모든 채팅방 메시지 구독
    container.addMessageListener(redisMessageSubscriber, new PatternTopic("chat:room:*"));

    // chat:sse:* 패턴으로 모든 SSE 이벤트 구독
    container.addMessageListener(redisMessageSubscriber, new PatternTopic("chat:sse:*"));

    return container;
  }
}