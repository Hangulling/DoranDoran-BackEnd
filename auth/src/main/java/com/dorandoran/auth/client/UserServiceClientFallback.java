package com.dorandoran.auth.client;

import com.dorandoran.shared.dto.UserDto;
import com.dorandoran.shared.dto.UserWithPasswordDto;
import com.dorandoran.shared.dto.ResetPasswordRequest;
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
    public UserWithPasswordDto getUserByEmailForAuth(String email) {
        log.warn("User Service 호출 실패 - getUserByEmailForAuth: email={}", email);
        // Fallback 로직: 기본 사용자 정보 반환 또는 예외 처리
        return null;
    }
    
    @Override
    public String healthCheck() {
        log.warn("User Service 헬스체크 실패");
        return "User Service is unavailable";
    }
    
    @Override
    public void resetPassword(ResetPasswordRequest request) {
        log.warn("User Service 호출 실패 - resetPassword: email={}", request.getEmail());
        // Fallback 로직: 비밀번호 재설정 실패 처리
        // void 메서드이므로 로그만 남기고 종료
    }
    
    @Override
    public void updatePassword(String userId, String newPassword) {
        log.warn("User Service 호출 실패 - updatePassword: userId={}", userId);
        // Fallback 로직: 비밀번호 업데이트 실패 처리
        // void 메서드이므로 로그만 남기고 종료
    }
}