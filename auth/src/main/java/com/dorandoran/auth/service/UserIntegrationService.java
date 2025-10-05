package com.dorandoran.auth.service;

import com.dorandoran.auth.client.UserServiceClient;
import com.dorandoran.shared.dto.UserDto;
import com.dorandoran.shared.dto.ResetPasswordRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * User Service 통합 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserIntegrationService {
    
    private final UserServiceClient userServiceClient;
    
    /**
     * 사용자 ID로 조회 (Circuit Breaker 적용)
     */
    @CircuitBreaker(name = "user-service", fallbackMethod = "getUserByIdFallback")
    @Retry(name = "user-service")
    @TimeLimiter(name = "user-service")
    public CompletableFuture<UserDto> getUserByIdAsync(String userId) {
        log.info("User Service 호출 - getUserById: userId={}", userId);
        return CompletableFuture.completedFuture(userServiceClient.getUserById(userId));
    }
    
    /**
     * 사용자 ID로 조회 (동기)
     */
    @CircuitBreaker(name = "user-service", fallbackMethod = "getUserByIdFallback")
    @Retry(name = "user-service")
    public UserDto getUserById(String userId) {
        log.info("User Service 호출 - getUserById: userId={}", userId);
        return userServiceClient.getUserById(userId);
    }
    
    /**
     * 이메일로 사용자 조회
     */
    @CircuitBreaker(name = "user-service", fallbackMethod = "getUserByEmailFallback")
    @Retry(name = "user-service")
    public UserDto getUserByEmail(String email) {
        log.info("User Service 호출 - getUserByEmail: email={}", email);
        return userServiceClient.getUserByEmail(email);
    }
    
    /**
     * 사용자 서비스 헬스체크
     */
    @CircuitBreaker(name = "user-service", fallbackMethod = "healthCheckFallback")
    @Retry(name = "user-service")
    public String healthCheck() {
        log.info("User Service 헬스체크 호출");
        return userServiceClient.healthCheck();
    }
    
    /**
     * 사용자 비밀번호 업데이트
     */
    @CircuitBreaker(name = "user-service", fallbackMethod = "updatePasswordFallback")
    @Retry(name = "user-service")
    public void updatePassword(UUID userId, String newPassword) {
        log.info("User Service 호출 - updatePassword: userId={}", userId);
        userServiceClient.updatePassword(userId.toString(), newPassword);
    }

    /**
     * 비밀번호 재설정
     */
    @CircuitBreaker(name = "user-service", fallbackMethod = "resetPasswordFallback")
    @Retry(name = "user-service")
    public void resetPassword(String email, String newPassword) {
        log.info("User Service 호출 - resetPassword: email={}", email);
        userServiceClient.resetPassword(new ResetPasswordRequest(email, newPassword));
    }

    public void resetPasswordFallback(String email, String newPassword, Exception ex) {
        log.error("User Service 호출 실패 - resetPassword: email={}, error={}", email, ex.getMessage());
        throw new RuntimeException("User Service unavailable");
    }
    
    // ===== Fallback 메서드들 =====
    
    public UserDto getUserByIdFallback(String userId, Exception ex) {
        log.error("User Service 호출 실패 - getUserById: userId={}, error={}", userId, ex.getMessage());
        throw new RuntimeException("User Service를 사용할 수 없습니다. 잠시 후 다시 시도해주세요.");
    }
    
    public UserDto getUserByEmailFallback(String email, Exception ex) {
        log.error("User Service 호출 실패 - getUserByEmail: email={}, error={}", email, ex.getMessage());
        throw new RuntimeException("User Service를 사용할 수 없습니다. 잠시 후 다시 시도해주세요.");
    }
    
    public String healthCheckFallback(Exception ex) {
        log.error("User Service 헬스체크 실패: error={}", ex.getMessage());
        return "User Service is unavailable";
    }
    
    public void updatePasswordFallback(UUID userId, String newPassword, Exception ex) {
        log.error("User Service 호출 실패 - updatePassword: userId={}, error={}", userId, ex.getMessage());
        throw new RuntimeException("User Service를 사용할 수 없습니다. 잠시 후 다시 시도해주세요.");
    }
}
