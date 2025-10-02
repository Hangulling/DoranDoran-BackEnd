package com.dorandoran.auth.client;

import com.dorandoran.shared.dto.UserDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * User Service Feign 클라이언트 Fallback
 */
@Component
@Slf4j
public class UserServiceClientFallback implements UserServiceClient {
    
    @Override
    public UserDto getUserById(String userId) {
        log.warn("User Service 호출 실패 - getUserById: userId={}", userId);
        // Fallback 로직: 기본 사용자 정보 반환 또는 예외 처리
        return null;
    }
    
    @Override
    public UserDto getUserByEmail(String email) {
        log.warn("User Service 호출 실패 - getUserByEmail: email={}", email);
        // Fallback 로직: 기본 사용자 정보 반환 또는 예외 처리
        return null;
    }
    
    @Override
    public String healthCheck() {
        log.warn("User Service 헬스체크 실패");
        return "User Service is unavailable";
    }
}