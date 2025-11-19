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
    
    @Override
    public UserDto updateStatus(String userId, String status) {
        log.warn("User Service 호출 실패 - updateStatus: userId={}, status={}", userId, status);
        // Fallback 로직: 상태 업데이트 실패 처리
        return null;
    }
    
    @Override
    public boolean isEmailDuplicate(String email) {
        log.warn("User Service 호출 실패 - isEmailDuplicate: email={}", email);
        // Fallback 로직: 이메일 중복 확인 실패 시 false 반환 (중복이 아닌 것으로 간주)
        return false;
    }
    
    @Override
    public UserDto getUserByOAuth(String provider, String oauthId) {
        log.warn("User Service 호출 실패 - getUserByOAuth: provider={}, oauthId={}", provider, oauthId);
        // Fallback 로직: OAuth 사용자 조회 실패 처리
        return null;
    }
    
    @Override
    public UserDto createOAuthUser(String email, String firstName, String lastName, String name, String picture, String provider, String oauthId) {
        log.warn("User Service 호출 실패 - createOAuthUser: email={}, provider={}, oauthId={}", email, provider, oauthId);
        // Fallback 로직: OAuth 사용자 생성 실패 처리
        return null;
    }
}