package com.dorandoran.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Locale;
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
        String norm = normalizeEmail(email);
        String key = CODE_PREFIX + norm;
        Duration ttl = Duration.ofMinutes(CODE_EXPIRATION_MINUTES);
        
        redisTemplate.opsForValue().set(key, code, ttl);
        log.info("비밀번호 재설정 코드 저장: email={}, ttl={}분", norm, CODE_EXPIRATION_MINUTES);
    }
    
    /**
     * 비밀번호 재설정 코드 검증
     */
    public boolean verifyCode(String email, String code) {
        String norm = normalizeEmail(email);
        String key = CODE_PREFIX + norm;
        String storedCode = redisTemplate.opsForValue().get(key);
        
        if (storedCode == null) {
            log.warn("비밀번호 재설정 코드 없음 또는 만료: email={}", norm);
            return false;
        }
        
        boolean isValid = storedCode.equals(code);
        if (!isValid) {
            log.warn("비밀번호 재설정 코드 불일치: email={}", norm);
        } else {
            log.info("비밀번호 재설정 코드 검증 성공: email={}", norm);
        }
        
        return isValid;
    }
    
    /**
     * 비밀번호 재설정 코드 무효화 (재발송 시 사용)
     */
    public void invalidateCode(String email) {
        String norm = normalizeEmail(email);
        String key = CODE_PREFIX + norm;
        redisTemplate.delete(key);
        log.info("비밀번호 재설정 코드 무효화: email={}", norm);
    }
    
    /**
     * 비밀번호 재설정 코드 유효성 확인 (존재 여부만 확인)
     */
    public boolean isCodeValid(String email) {
        String norm = normalizeEmail(email);
        String key = CODE_PREFIX + norm;
        String storedCode = redisTemplate.opsForValue().get(key);
        return storedCode != null;
    }
    
    /**
     * 저장된 코드 조회 (내부 사용)
     */
    public Optional<String> getCode(String email) {
        String norm = normalizeEmail(email);
        String key = CODE_PREFIX + norm;
        String code = redisTemplate.opsForValue().get(key);
        return Optional.ofNullable(code);
    }

    private static String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}

