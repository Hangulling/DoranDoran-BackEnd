package com.dorandoran.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * 토큰 블랙리스트 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    private static final String BLACKLIST_PREFIX = "blacklist:token:";
    private static final String USER_TOKENS_PREFIX = "user:tokens:";
    
    /**
     * 토큰을 블랙리스트에 추가
     */
    public void addToBlacklist(String token, String reason, Duration expiration) {
        try {
            String tokenHash = hashToken(token);
            String blacklistKey = BLACKLIST_PREFIX + tokenHash;
            
            // 블랙리스트에 토큰 추가
            redisTemplate.opsForValue().set(blacklistKey, reason, expiration);
            
            log.info("토큰 블랙리스트 추가: tokenHash={}, reason={}, expiration={}", 
                    tokenHash, reason, expiration);
        } catch (Exception e) {
            log.error("토큰 블랙리스트 추가 실패: error={}", e.getMessage(), e);
        }
    }
    
    /**
     * 토큰이 블랙리스트에 있는지 확인
     */
    public boolean isBlacklisted(String token) {
        try {
            String tokenHash = hashToken(token);
            String blacklistKey = BLACKLIST_PREFIX + tokenHash;
            
            return Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey));
        } catch (Exception e) {
            log.error("토큰 블랙리스트 확인 실패: error={}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 사용자의 모든 토큰을 블랙리스트에 추가
     */
    public void blacklistUserTokens(UUID userId, String reason) {
        try {
            String userTokensKey = USER_TOKENS_PREFIX + userId;
            
            // 사용자의 모든 토큰 조회
            var tokens = redisTemplate.opsForSet().members(userTokensKey);
            if (tokens != null) {
                for (String token : tokens) {
                    addToBlacklist(token, reason, Duration.ofDays(7)); // 7일간 블랙리스트 유지
                }
                
                // 사용자 토큰 세트 삭제
                redisTemplate.delete(userTokensKey);
            }
            
            log.info("사용자 모든 토큰 블랙리스트 추가: userId={}, reason={}", userId, reason);
        } catch (Exception e) {
            log.error("사용자 토큰 블랙리스트 추가 실패: userId={}, error={}", userId, e.getMessage(), e);
        }
    }
    
    /**
     * 사용자 토큰 등록
     */
    public void registerUserToken(UUID userId, String token) {
        try {
            String userTokensKey = USER_TOKENS_PREFIX + userId;
            redisTemplate.opsForSet().add(userTokensKey, token);
            
            // 토큰 만료 시간 설정 (7일)
            redisTemplate.expire(userTokensKey, Duration.ofDays(7));
            
            log.debug("사용자 토큰 등록: userId={}", userId);
        } catch (Exception e) {
            log.error("사용자 토큰 등록 실패: userId={}, error={}", userId, e.getMessage(), e);
        }
    }
    
    /**
     * 토큰 해시 생성
     */
    private String hashToken(String token) {
        // 실제 구현에서는 SHA-256 등 안전한 해시 함수 사용
        return String.valueOf(token.hashCode());
    }
}