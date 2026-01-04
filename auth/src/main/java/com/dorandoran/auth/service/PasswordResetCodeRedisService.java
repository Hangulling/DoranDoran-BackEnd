package com.dorandoran.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * 비밀번호 재설정 코드 관리 서비스 (Redis 기반)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PasswordResetCodeRedisService {

    private final RedisTemplate<String, String> redisTemplate;
    
    private static final String CODE_PREFIX = "password:reset:code:";
    private static final int CODE_EXPIRATION_MINUTES = 5;
    
    /**
     * 비밀번호 재설정 코드 저장 (5분 TTL)
     * 재발송 시 기존 코드는 자동으로 무효화됨 (같은 키로 덮어쓰기)
     */
    public void saveResetCode(String email, String code) {
        String key = CODE_PREFIX + email;
        Duration ttl = Duration.ofMinutes(CODE_EXPIRATION_MINUTES);
        
        redisTemplate.opsForValue().set(key, code, ttl);
        log.info("비밀번호 재설정 코드 저장: email={}, ttl={}분", email, CODE_EXPIRATION_MINUTES);
    }
    
    /**
     * 비밀번호 재설정 코드 검증
     */
    public boolean verifyCode(String email, String code) {
        String key = CODE_PREFIX + email;
        String storedCode = redisTemplate.opsForValue().get(key);
        
        if (storedCode == null) {
            log.warn("비밀번호 재설정 코드 없음 또는 만료: email={}", email);
            return false;
        }
        
        boolean isValid = storedCode.equals(code);
        if (!isValid) {
            log.warn("비밀번호 재설정 코드 불일치: email={}", email);
        } else {
            log.info("비밀번호 재설정 코드 검증 성공: email={}", email);
        }
        
        return isValid;
    }
    
    /**
     * 비밀번호 재설정 코드 무효화 (재발송 시 사용)
     */
    public void invalidateCode(String email) {
        String key = CODE_PREFIX + email;
        redisTemplate.delete(key);
        log.info("비밀번호 재설정 코드 무효화: email={}", email);
    }
    
    /**
     * 비밀번호 재설정 코드 유효성 확인 (존재 여부만 확인)
     */
    public boolean isCodeValid(String email) {
        String key = CODE_PREFIX + email;
        String storedCode = redisTemplate.opsForValue().get(key);
        return storedCode != null;
    }
    
    /**
     * 저장된 코드 조회 (내부 사용)
     */
    public Optional<String> getCode(String email) {
        String key = CODE_PREFIX + email;
        String code = redisTemplate.opsForValue().get(key);
        return Optional.ofNullable(code);
    }
}

