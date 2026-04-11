package com.dorandoran.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

/**
 * Redis를 사용한 이메일 인증 상태 관리 서비스
 */
@Service
@Slf4j
public class EmailVerificationRedisService {

    private final RedisTemplate<String, String> redisTemplate;
    
    // LocalDateTime 직렬화를 지원하는 ObjectMapper
    private final ObjectMapper objectMapper;
    
    @Value("${email.verification.expiration-minutes:5}")
    private int expirationMinutes;
    
    private static final String VERIFICATION_PREFIX = "email:verification:";
    
    // ObjectMapper 초기화
    public EmailVerificationRedisService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
    
    /**
     * 이메일 인증 요청 정보 저장
     */
    public void saveVerificationRequest(String email, String token) {
        try {
            String norm = normalizeEmail(email);
            String key = VERIFICATION_PREFIX + norm;
            VerificationData data = VerificationData.builder()
                    .email(norm)
                    .token(token)
                    .verified(false)
                    .createdAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusMinutes(expirationMinutes))
                    .build();
            
            String jsonValue = objectMapper.writeValueAsString(data);
            Duration ttl = Duration.ofMinutes(expirationMinutes);
            
            redisTemplate.opsForValue().set(key, jsonValue, ttl);
            log.info("이메일 인증 요청 저장: email={}, ttl={}분", norm, expirationMinutes);
        } catch (JsonProcessingException e) {
            log.error("이메일 인증 요청 저장 실패: email={}, error={}", email, e.getMessage());
            throw new RuntimeException("이메일 인증 요청 저장 실패", e);
        }
    }
    
    /**
     * 이메일 인증 완료 처리
     */
    public void markEmailVerified(String email) {
        try {
            String norm = normalizeEmail(email);
            String key = VERIFICATION_PREFIX + norm;
            Optional<VerificationData> dataOpt = getVerificationData(norm);
            
            if (dataOpt.isPresent()) {
                VerificationData data = dataOpt.get();
                data.setVerified(true);
                String jsonValue = objectMapper.writeValueAsString(data);
                
                // 남은 TTL 계산
                long remainingSeconds = Duration.between(LocalDateTime.now(), data.getExpiresAt()).getSeconds();
                if (remainingSeconds > 0) {
                    redisTemplate.opsForValue().set(key, jsonValue, Duration.ofSeconds(remainingSeconds));
                    log.info("이메일 인증 완료 처리: email={}", norm);
                } else {
                    log.warn("이메일 인증 만료됨: email={}", norm);
                }
            } else {
                log.warn("인증 요청 정보를 찾을 수 없음: email={}", norm);
            }
        } catch (JsonProcessingException e) {
            log.error("이메일 인증 완료 처리 실패: email={}, error={}", email, e.getMessage());
            throw new RuntimeException("이메일 인증 완료 처리 실패", e);
        }
    }
    
    /**
     * 이메일 인증 완료 여부 확인
     */
    public boolean isEmailVerified(String email) {
        Optional<VerificationData> dataOpt = getVerificationData(email);
        return dataOpt.map(VerificationData::isVerified).orElse(false);
    }
    
    /**
     * 토큰 검증
     */
    public boolean verifyToken(String email, String token) {
        String norm = normalizeEmail(email);
        Optional<VerificationData> dataOpt = getVerificationData(norm);
        if (dataOpt.isEmpty()) {
            return false;
        }
        
        VerificationData data = dataOpt.get();
        
        // 토큰 일치 확인
        if (!data.getToken().equals(token)) {
            log.warn("토큰 불일치: email={}", norm);
            return false;
        }
        
        // 만료 시간 확인
        if (data.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("토큰 만료: email={}, expiresAt={}", norm, data.getExpiresAt());
            return false;
        }
        
        return true;
    }
    
    /**
     * 인증 데이터 조회
     */
    private Optional<VerificationData> getVerificationData(String email) {
        try {
            String norm = normalizeEmail(email);
            String key = VERIFICATION_PREFIX + norm;
            String jsonValue = redisTemplate.opsForValue().get(key);
            
            if (jsonValue == null) {
                return Optional.empty();
            }
            
            VerificationData data = objectMapper.readValue(jsonValue, VerificationData.class);
            return Optional.of(data);
        } catch (JsonProcessingException e) {
            log.error("인증 데이터 조회 실패: email={}, error={}", email, e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * 인증 데이터 삭제
     */
    public void deleteVerificationData(String email) {
        String norm = normalizeEmail(email);
        String key = VERIFICATION_PREFIX + norm;
        redisTemplate.delete(key);
        log.info("이메일 인증 데이터 삭제: email={}", norm);
    }

    private static String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
    
    /**
     * 인증 요청 정보 DTO
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class VerificationData {
        private String email;
        private String token;
        private boolean verified;
        private LocalDateTime createdAt;
        private LocalDateTime expiresAt;
    }
}


