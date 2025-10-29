package com.dorandoran.chat.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis 캐시 설정
 * 
 * 주요 기능:
 * - 캐시별 TTL 정책 설정
 * - JSON 직렬화 설정
 * - 캐시 통계 활성화
 */
@Configuration
@EnableCaching
public class RedisCacheConfig {

    /**
     * Redis Cache Manager 설정
     * 각 캐시별로 다른 TTL과 설정을 적용
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory, MeterRegistry meterRegistry) {
        // 기본 직렬화 설정
        Jackson2JsonRedisSerializer<Object> serializer = createJacksonSerializer();
        
        // 기본 캐시 설정
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .entryTtl(Duration.ofMinutes(30)) // 기본 TTL: 30분
                .disableCachingNullValues();

        // 캐시별 설정 맵
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // ChatRoom 캐싱 (30분 TTL)
        cacheConfigurations.put("chatrooms", defaultConfig.entryTtl(Duration.ofMinutes(30)));
        
        // IntimacyProgress 캐싱 (15분 TTL)
        cacheConfigurations.put("intimacy", defaultConfig.entryTtl(Duration.ofMinutes(15)));
        
        // User/Chatbot 캐싱 (2시간 TTL - 마스터 데이터)
        cacheConfigurations.put("users", defaultConfig.entryTtl(Duration.ofHours(2)));
        cacheConfigurations.put("chatbots", defaultConfig.entryTtl(Duration.ofHours(2)));
        
        // SystemPrompt 캐싱 (10분 TTL)
        cacheConfigurations.put("prompts", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        
        // 메시지 히스토리 캐싱 (3분 TTL - 짧은 TTL로 최신성 보장)
        cacheConfigurations.put("messageHistory", defaultConfig.entryTtl(Duration.ofMinutes(3)));
        
        // 채팅방 목록 캐싱 (5분 TTL)
        cacheConfigurations.put("roomList", defaultConfig.entryTtl(Duration.ofMinutes(5)));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

/**
 * Jackson JSON 직렬화 설정
 * DTO 캐싱으로 전환 완료 - Entity 직렬화 문제 해결됨
 * Hibernate 프록시 처리 설정은 유지 (호환성)
 */
private Jackson2JsonRedisSerializer<Object> createJacksonSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();
        
        // Java 8 시간 타입 지원 모듈 등록
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        
        // 순환 참조 방지 설정 (강화)
        objectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_SELF_REFERENCES, false);
        
        // JPA 프록시 객체 처리
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_INVALID_SUBTYPE, false);
        
        // Hibernate 프록시 객체 처리
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false);
        
        // 순환 참조 방지를 위한 추가 설정
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);
        
        return new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);
    }
}
