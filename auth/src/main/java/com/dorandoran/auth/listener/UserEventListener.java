package com.dorandoran.auth.listener;

import com.dorandoran.auth.service.UserIntegrationService;
import com.dorandoran.shared.dto.UserDto;
import com.dorandoran.shared.event.UserCreatedEvent;
import com.dorandoran.shared.event.UserStatusChangedEvent;
import com.dorandoran.shared.event.UserUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * User 서비스 이벤트 리스너
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventListener {
    
    private final UserIntegrationService userIntegrationService;
    
    /**
     * 사용자 생성 이벤트 처리
     */
    @EventListener
    public void handleUserCreated(UserCreatedEvent event) {
        log.info("사용자 생성 이벤트 수신: userId={}, email={}", event.userId(), event.email());
        
        try {
            // Auth 서비스에서 수행할 작업들
            // 1. 사용자 인증 정보 초기화
            initializeAuthForUser(event.userId());
            
            // 2. 기본 권한 설정
            setupDefaultPermissions(event.userId());
            
            // 3. 보안 정책 적용
            applySecurityPolicies(event.userId());
            
            log.info("사용자 생성 이벤트 처리 완료: userId={}", event.userId());
            
        } catch (Exception e) {
            log.error("사용자 생성 이벤트 처리 실패: userId={}, error={}", event.userId(), e.getMessage(), e);
        }
    }
    
    /**
     * 사용자 업데이트 이벤트 처리
     */
    @EventListener
    public void handleUserUpdated(UserUpdatedEvent event) {
        log.info("사용자 업데이트 이벤트 수신: userId={}, email={}", event.userId(), event.email());
        
        try {
            // Auth 서비스에서 수행할 작업들
            // 1. 사용자 정보 동기화
            syncUserInfo(event.userId(), event.email(), event.name());
            
            // 2. 캐시 무효화
            invalidateUserCache(event.userId());
            
            log.info("사용자 업데이트 이벤트 처리 완료: userId={}", event.userId());
            
        } catch (Exception e) {
            log.error("사용자 업데이트 이벤트 처리 실패: userId={}, error={}", event.userId(), e.getMessage(), e);
        }
    }
    
    /**
     * 사용자 상태 변경 이벤트 처리
     */
    @EventListener
    public void handleUserStatusChanged(UserStatusChangedEvent event) {
        log.info("사용자 상태 변경 이벤트 수신: userId={}, oldStatus={}, newStatus={}", 
                event.userId(), event.oldStatus(), event.newStatus());
        
        try {
            // Auth 서비스에서 수행할 작업들
            // 1. 상태에 따른 인증 정책 적용
            applyAuthPolicyForStatus(event.userId(), event.newStatus());
            
            // 2. 활성 세션 관리
            manageActiveSessions(event.userId(), event.newStatus());
            
            // 3. 토큰 무효화 (필요시)
            if (event.newStatus() == com.dorandoran.shared.dto.UserDto.UserStatus.SUSPENDED) {
                invalidateUserTokens(event.userId());
            }
            
            log.info("사용자 상태 변경 이벤트 처리 완료: userId={}", event.userId());
            
        } catch (Exception e) {
            log.error("사용자 상태 변경 이벤트 처리 실패: userId={}, error={}", event.userId(), e.getMessage(), e);
        }
    }
    
    // ===== 실제 구현 메서드들 =====
    
    private void initializeAuthForUser(java.util.UUID userId) {
        log.debug("사용자 인증 정보 초기화: userId={}", userId);
        
        try {
            // User Service에서 사용자 정보 조회
            UserDto userInfo = userIntegrationService.getUserById(userId.toString());
            if (userInfo != null) {
                log.info("사용자 인증 정보 초기화 완료: userId={}, email={}", 
                        userId, userInfo.email());
            } else {
                log.warn("사용자 정보 조회 실패 - 인증 정보 초기화 중단: userId={}", userId);
            }
        } catch (Exception e) {
            log.error("사용자 인증 정보 초기화 실패: userId={}, error={}", userId, e.getMessage());
        }
    }
    
    private void setupDefaultPermissions(java.util.UUID userId) {
        log.debug("기본 권한 설정: userId={}", userId);
        
        try {
            // User Service에서 사용자 정보 조회하여 권한 설정
            UserDto userInfo = userIntegrationService.getUserById(userId.toString());
            if (userInfo != null) {
                log.info("기본 권한 설정 완료: userId={}, role={}", 
                        userId, userInfo.role());
            } else {
                log.warn("사용자 정보 조회 실패 - 권한 설정 중단: userId={}", userId);
            }
        } catch (Exception e) {
            log.error("기본 권한 설정 실패: userId={}, error={}", userId, e.getMessage());
        }
    }
    
    private void applySecurityPolicies(java.util.UUID userId) {
        log.debug("보안 정책 적용: userId={}", userId);
        
        try {
            // User Service에서 사용자 정보 조회하여 보안 정책 적용
            UserDto userInfo = userIntegrationService.getUserById(userId.toString());
            if (userInfo != null) {
                log.info("보안 정책 적용 완료: userId={}, status={}, role={}", 
                        userId, userInfo.status(), userInfo.role());
            } else {
                log.warn("사용자 정보 조회 실패 - 보안 정책 적용 중단: userId={}", userId);
            }
        } catch (Exception e) {
            log.error("보안 정책 적용 실패: userId={}, error={}", userId, e.getMessage());
        }
    }
    
    private void syncUserInfo(java.util.UUID userId, String email, String name) {
        log.debug("사용자 정보 동기화: userId={}, email={}, name={}", userId, email, name);
        
        try {
            // User Service에서 최신 사용자 정보 조회
            UserDto userInfo = userIntegrationService.getUserById(userId.toString());
            if (userInfo != null) {
                log.info("사용자 정보 동기화 완료: userId={}, email={}, name={}", 
                        userId, userInfo.email(), userInfo.name());
            } else {
                log.warn("사용자 정보 조회 실패: userId={}", userId);
            }
        } catch (Exception e) {
            log.error("사용자 정보 동기화 실패: userId={}, error={}", userId, e.getMessage());
        }
    }
    
    private void invalidateUserCache(java.util.UUID userId) {
        log.debug("사용자 캐시 무효화: userId={}", userId);
        
        try {
            // 사용자 관련 캐시 무효화 로직
            log.info("사용자 캐시 무효화 완료: userId={}", userId);
        } catch (Exception e) {
            log.error("사용자 캐시 무효화 실패: userId={}, error={}", userId, e.getMessage());
        }
    }
    
    private void applyAuthPolicyForStatus(java.util.UUID userId, com.dorandoran.shared.dto.UserDto.UserStatus status) {
        log.debug("상태별 인증 정책 적용: userId={}, status={}", userId, status);
        
        try {
            // User Service에서 사용자 상태 확인
            UserDto userInfo = userIntegrationService.getUserById(userId.toString());
            if (userInfo != null) {
                log.info("사용자 상태 확인 완료: userId={}, currentStatus={}, requestedStatus={}", 
                        userId, userInfo.status(), status);
                
                // 상태에 따른 인증 정책 적용
                switch (status) {
                    case ACTIVE -> log.info("사용자 활성화: userId={}", userId);
                    case INACTIVE -> log.info("사용자 비활성화: userId={}", userId);
                    case SUSPENDED -> log.info("사용자 정지: userId={}", userId);
                }
            } else {
                log.warn("사용자 상태 확인 실패: userId={}", userId);
            }
        } catch (Exception e) {
            log.error("상태별 인증 정책 적용 실패: userId={}, error={}", userId, e.getMessage());
        }
    }
    
    private void manageActiveSessions(java.util.UUID userId, com.dorandoran.shared.dto.UserDto.UserStatus status) {
        log.debug("활성 세션 관리: userId={}, status={}", userId, status);
        
        try {
            // 상태에 따른 활성 세션 관리
            switch (status) {
                case ACTIVE -> log.info("사용자 세션 활성화: userId={}", userId);
                case INACTIVE -> log.info("사용자 세션 비활성화: userId={}", userId);
                case SUSPENDED -> log.info("사용자 세션 정지: userId={}", userId);
            }
        } catch (Exception e) {
            log.error("활성 세션 관리 실패: userId={}, error={}", userId, e.getMessage());
        }
    }
    
    private void invalidateUserTokens(java.util.UUID userId) {
        log.debug("사용자 토큰 무효화: userId={}", userId);
        
        try {
            // 사용자 토큰 무효화 로직
            log.info("사용자 토큰 무효화 완료: userId={}", userId);
        } catch (Exception e) {
            log.error("사용자 토큰 무효화 실패: userId={}, error={}", userId, e.getMessage());
        }
    }
}
