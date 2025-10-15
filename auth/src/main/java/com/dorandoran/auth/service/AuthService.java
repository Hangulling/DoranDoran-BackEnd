package com.dorandoran.auth.service;

// same package class, no import needed
import com.dorandoran.auth.entity.LoginAttempt;
import com.dorandoran.auth.entity.AuthEvent;
import com.dorandoran.auth.entity.RefreshToken;
import com.dorandoran.auth.entity.PasswordResetToken;
import com.dorandoran.auth.repository.LoginAttemptRepository;
import com.dorandoran.auth.repository.AuthEventRepository;
import com.dorandoran.auth.dto.LoginRequest;
import com.dorandoran.auth.dto.LoginResponse;
import com.dorandoran.common.exception.DoranDoranException;
import com.dorandoran.common.exception.ErrorCode;
import com.dorandoran.shared.dto.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Date;

/**
 * 인증 서비스 (User 서비스 중심 구조)
 * User 서비스와 완전 통합된 구조
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UserIntegrationService userIntegrationService;
    private final TokenBlacklistService tokenBlacklistService;
    private final RefreshTokenService refreshTokenService;
    private final LoginAttemptRepository loginAttemptRepository;
    private final AuthEventRepository authEventRepository;
    private final PasswordResetService passwordResetService;
    
    /**
     * 로그인 (User 서비스 중심 구조)
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        log.info("사용자 로그인 요청: email={}", request.getEmail());
        
        try {
            // User 서비스에서 사용자 정보 조회 (단일 소스)
            UserDto user = userIntegrationService.getUserByEmail(request.getEmail());
            
            // 비밀번호 검증 (User 서비스의 데이터 사용)
            if (!passwordEncoder.matches(request.getPassword(), user.passwordHash())) {
                // 실패 시도 기록
                recordLoginAttempt(null, request.getEmail(), false);
                throw new DoranDoranException(ErrorCode.INVALID_PASSWORD);
            }
            
            // JWT 토큰 생성 (User 서비스 데이터 기반)
            String accessToken = jwtService.generateAccessToken(user.id().toString(), user.email(), user.name());
            String refreshToken = jwtService.generateRefreshToken(user.id().toString(), user.email(), user.name());

            // 성공 시도 기록
            recordLoginAttempt(UUID.fromString(user.id()), user.email(), true);

            // 리프레시 토큰 저장(해시)
            saveRefreshToken(UUID.fromString(user.id()), refreshToken);

            // 이벤트 로깅
            recordAuthEvent(UUID.fromString(user.id()), "LOGIN");
            
            log.info("로그인 성공: userId={}, email={}", user.id(), user.email());
            
            return LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(3600L) // 1시간 (초 단위)
                    .user(user)
                    .build();
                    
        } catch (DoranDoranException e) {
            log.error("로그인 실패: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("로그인 중 예상치 못한 오류 발생", e);
            throw new DoranDoranException(ErrorCode.INTERNAL_SERVER_ERROR, "로그인 중 오류가 발생했습니다.");
        }
    }

    /**
     * 이메일로 사용자 조회(비밀번호 재설정 등 내부 사용)
     */
    public UserDto findUserByEmail(String email) {
        return userIntegrationService.getUserByEmail(email);
    }

    /**
     * 사용자 모든 토큰 무효화(비밀번호 재설정 후 호출)
     */
    public void invalidateAllTokensForUser(String userId) {
        try {
            tokenBlacklistService.blacklistAllUserTokens(userId);
            log.info("사용자 토큰 무효화 완료: userId={}", userId);
        } catch (Exception e) {
            log.warn("사용자 토큰 무효화 실패: userId={}", userId, e);
        }
    }
    
    /**
     * 토큰 검증 (User 서비스 중심 구조)
     */
    public UserDto validateToken(String token) {
        log.debug("토큰 검증: token={}", token);
        
        try {
            if (!jwtService.isTokenValid(token)) {
                throw new DoranDoranException(ErrorCode.AUTH_TOKEN_INVALID);
            }
            
            String userId = jwtService.extractUserId(token);
            
            // User 서비스에서 사용자 정보 조회 (단일 소스)
            UserDto user = userIntegrationService.getUserById(userId);
            
            return user;
            
        } catch (DoranDoranException e) {
            log.error("토큰 검증 실패: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("토큰 검증 중 예상치 못한 오류 발생", e);
            throw new DoranDoranException(ErrorCode.AUTH_TOKEN_INVALID);
        }
    }
    
    /**
     * 토큰 갱신 (User 서비스 중심 구조)
     */
    public LoginResponse refreshToken(String refreshToken) {
        log.info("토큰 갱신 요청");
        
        try {
            if (!jwtService.isTokenValid(refreshToken)) {
                throw new DoranDoranException(ErrorCode.AUTH_TOKEN_EXPIRED);
            }
            
            String userId = jwtService.extractUserId(refreshToken);
            
            // User 서비스에서 사용자 정보 조회 (단일 소스)
            UserDto user = userIntegrationService.getUserById(userId);
            
            // 리프레시 토큰 로테이션: 기존 토큰 해시 확인 후 revoke/신규 발급
            String oldHash = tokenBlacklistService.hashToken(refreshToken);
            java.util.Optional<RefreshToken> existing = refreshTokenService.findByHash(oldHash);

            // 새 토큰 생성
            String newAccessToken = jwtService.generateAccessToken(user.id().toString(), user.email(), user.name());
            String newRefreshToken = jwtService.generateRefreshToken(user.id().toString(), user.email(), user.name());

            java.util.Date newRefreshExp = jwtService.extractExpiration(newRefreshToken);
            String newHash = tokenBlacklistService.hashToken(newRefreshToken);

            if (existing.isPresent()) {
                refreshTokenService.rotate(existing.get(), newHash,
                        java.time.LocalDateTime.ofInstant(newRefreshExp.toInstant(), java.time.ZoneId.systemDefault()));
            } else {
                // UserDto를 User 엔티티로 변환
                com.dorandoran.auth.entity.User userEntity = com.dorandoran.auth.entity.User.builder()
                        .id(UUID.fromString(user.id()))
                        .email(user.email())
                        .firstName(user.firstName())
                        .lastName(user.lastName())
                        .name(user.name())
                        .passwordHash(user.passwordHash())
                        .picture(user.picture())
                        .info(user.info())
                        .lastConnTime(user.lastConnTime())
                        .status(com.dorandoran.auth.entity.User.UserStatus.valueOf(user.status().name()))
                        .role(com.dorandoran.auth.entity.User.RoleName.valueOf(user.role().name()))
                        .coachCheck(user.coachCheck())
                        .createdAt(user.createdAt())
                        .updatedAt(user.updatedAt())
                        .build();
                
                // 과거 미추적 토큰인 경우, 현재 토큰을 기록하고 새 토큰 발급 기록 추가
                java.util.Date currentExp = jwtService.extractExpiration(refreshToken);
                if (currentExp != null) {
                    refreshTokenService.issue(userEntity, oldHash,
                            java.time.LocalDateTime.ofInstant(currentExp.toInstant(), java.time.ZoneId.systemDefault()),
                            null, null, null);
                }
                refreshTokenService.issue(userEntity, newHash,
                        java.time.LocalDateTime.ofInstant(newRefreshExp.toInstant(), java.time.ZoneId.systemDefault()),
                        null, null, null);
            }

            // 이벤트 로깅
            recordAuthEvent(UUID.fromString(user.id()), "TOKEN_ROTATE");
            
            log.info("토큰 갱신 성공: userId={}", user.id());
            
            return LoginResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .tokenType("Bearer")
                    .expiresIn(3600L) // 1시간 (초 단위)
                    .user(user)
                    .build();
                    
        } catch (DoranDoranException e) {
            log.error("토큰 갱신 실패: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("토큰 갱신 중 예상치 못한 오류 발생", e);
            throw new DoranDoranException(ErrorCode.INTERNAL_SERVER_ERROR, "토큰 갱신 중 오류가 발생했습니다.");
        }
    }
    
    /**
     * 로그아웃
     */
    public void logout(String token) {
        log.info("사용자 로그아웃 요청");
        
        try {
            // 1. 토큰에서 사용자 ID 추출
            String userId = jwtService.extractUserId(token);
            
            // 2. 토큰 만료 시간 추출
            Date expirationTime = jwtService.extractExpiration(token);
            
            // 3. 토큰을 블랙리스트에 추가
            if (expirationTime != null && expirationTime.after(new Date())) {
                long remainingTime = expirationTime.getTime() - System.currentTimeMillis();
                Duration duration = Duration.ofMillis(remainingTime);
                tokenBlacklistService.addToBlacklist(token, "User logout", duration);
                log.info("토큰이 블랙리스트에 추가되었습니다: userId={}", userId);
            } else {
                log.warn("토큰이 이미 만료되어 블랙리스트에 추가하지 않습니다: userId={}", userId);
            }
            
            // 이벤트 로깅
            recordAuthEvent(java.util.UUID.fromString(userId), "LOGOUT");

            log.info("로그아웃 완료: userId={}", userId);
            
        } catch (Exception e) {
            log.error("로그아웃 중 오류 발생", e);
            // 로그아웃은 실패해도 사용자에게는 성공으로 처리
        }
    }
    
    // ===== 헬퍼 메서드들 =====
    
    /**
     * 로그인 시도 기록
     */
    private void recordLoginAttempt(java.util.UUID userId, String email, boolean succeeded) {
        try {
            // User 객체를 조회하여 연관관계 설정
            com.dorandoran.auth.entity.User user = null;
            if (userId != null) {
                try {
                    // UserIntegrationService에서 UserDto를 받아서 User 엔티티로 변환
                    com.dorandoran.shared.dto.UserDto userDto = userIntegrationService.getUserById(userId.toString());
                    user = com.dorandoran.auth.entity.User.builder()
                            .id(UUID.fromString(userDto.id()))
                            .email(userDto.email())
                            .firstName(userDto.firstName())
                            .lastName(userDto.lastName())
                            .name(userDto.name())
                            .passwordHash(userDto.passwordHash())
                            .picture(userDto.picture())
                            .info(userDto.info())
                            .lastConnTime(userDto.lastConnTime())
                            .status(com.dorandoran.auth.entity.User.UserStatus.valueOf(userDto.status().name()))
                            .role(com.dorandoran.auth.entity.User.RoleName.valueOf(userDto.role().name()))
                            .coachCheck(userDto.coachCheck())
                            .createdAt(userDto.createdAt())
                            .updatedAt(userDto.updatedAt())
                            .build();
                } catch (Exception e) {
                    log.warn("사용자 조회 실패: {}", e.getMessage());
                }
            }
            
            loginAttemptRepository.save(LoginAttempt.builder()
                    .user(user)
                    .email(email)
                    .succeeded(succeeded)
                    .createdAt(java.time.LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.warn("로그인 시도 기록 실패: {}", e.getMessage());
        }
    }
    
    /**
     * 리프레시 토큰 저장
     */
    private void saveRefreshToken(java.util.UUID userId, String refreshToken) {
        try {
            // UserIntegrationService에서 UserDto를 받아서 User 엔티티로 변환
            com.dorandoran.shared.dto.UserDto userDto = userIntegrationService.getUserById(userId.toString());
            com.dorandoran.auth.entity.User user = com.dorandoran.auth.entity.User.builder()
                    .id(UUID.fromString(userDto.id()))
                    .email(userDto.email())
                    .firstName(userDto.firstName())
                    .lastName(userDto.lastName())
                    .name(userDto.name())
                    .passwordHash(userDto.passwordHash())
                    .picture(userDto.picture())
                    .info(userDto.info())
                    .lastConnTime(userDto.lastConnTime())
                    .status(com.dorandoran.auth.entity.User.UserStatus.valueOf(userDto.status().name()))
                    .role(com.dorandoran.auth.entity.User.RoleName.valueOf(userDto.role().name()))
                    .coachCheck(userDto.coachCheck())
                    .createdAt(userDto.createdAt())
                    .updatedAt(userDto.updatedAt())
                    .build();
            
            String refreshHash = tokenBlacklistService.hashToken(refreshToken);
            java.util.Date refreshExp = jwtService.extractExpiration(refreshToken);
            if (refreshExp != null) {
                refreshTokenService.issue(user, refreshHash,
                        java.time.LocalDateTime.ofInstant(refreshExp.toInstant(), java.time.ZoneId.systemDefault()),
                        null, null, null);
            }
        } catch (Exception e) {
            log.warn("리프레시 토큰 저장 실패: {}", e.getMessage());
        }
    }
    
    /**
     * 인증 이벤트 기록
     */
    private void recordAuthEvent(java.util.UUID userId, String eventType) {
        try {
            // User 객체를 조회하여 연관관계 설정
            com.dorandoran.auth.entity.User user = null;
            if (userId != null) {
                try {
                    // UserIntegrationService에서 UserDto를 받아서 User 엔티티로 변환
                    com.dorandoran.shared.dto.UserDto userDto = userIntegrationService.getUserById(userId.toString());
                    user = com.dorandoran.auth.entity.User.builder()
                            .id(UUID.fromString(userDto.id()))
                            .email(userDto.email())
                            .firstName(userDto.firstName())
                            .lastName(userDto.lastName())
                            .name(userDto.name())
                            .passwordHash(userDto.passwordHash())
                            .picture(userDto.picture())
                            .info(userDto.info())
                            .lastConnTime(userDto.lastConnTime())
                            .status(com.dorandoran.auth.entity.User.UserStatus.valueOf(userDto.status().name()))
                            .role(com.dorandoran.auth.entity.User.RoleName.valueOf(userDto.role().name()))
                            .coachCheck(userDto.coachCheck())
                            .createdAt(userDto.createdAt())
                            .updatedAt(userDto.updatedAt())
                            .build();
                } catch (Exception e) {
                    log.warn("사용자 조회 실패: {}", e.getMessage());
                }
            }
            
            authEventRepository.save(AuthEvent.builder()
                    .user(user)
                    .eventType(eventType)
                    .metadata(null)
                    .createdAt(java.time.LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.warn("인증 이벤트 기록 실패: {}", e.getMessage());
        }
    }
    
    /**
     * 비밀번호 재설정 요청 처리
     */
    @Transactional
    public String requestPasswordReset(String email) {
        log.info("비밀번호 재설정 요청: email={}", email);
        
        try {
            // User 서비스에서 사용자 조회
            UserDto user = userIntegrationService.getUserByEmail(email);
            
            // UserDto를 User 엔티티로 변환
            com.dorandoran.auth.entity.User userEntity = com.dorandoran.auth.entity.User.builder()
                    .id(UUID.fromString(user.id()))
                    .email(user.email())
                    .firstName(user.firstName())
                    .lastName(user.lastName())
                    .name(user.name())
                    .passwordHash(user.passwordHash())
                    .picture(user.picture())
                    .info(user.info())
                    .lastConnTime(user.lastConnTime())
                    .status(com.dorandoran.auth.entity.User.UserStatus.valueOf(user.status().name()))
                    .role(com.dorandoran.auth.entity.User.RoleName.valueOf(user.role().name()))
                    .coachCheck(user.coachCheck())
                    .createdAt(user.createdAt())
                    .updatedAt(user.updatedAt())
                    .build();
            
            // 기존 비밀번호 재설정 토큰이 있다면 무효화
            passwordResetService.invalidateUserTokens(userEntity);
            
            // 새로운 비밀번호 재설정 토큰 생성
            String resetToken = generatePasswordResetToken();
            java.time.LocalDateTime expiresAt = java.time.LocalDateTime.now().plusHours(24); // 24시간 후 만료
            
            passwordResetService.issue(userEntity, resetToken, expiresAt);
            
            // 이벤트 로깅
            recordAuthEvent(UUID.fromString(user.id()), "PASSWORD_RESET_REQUESTED");
            
            log.info("비밀번호 재설정 토큰 생성 완료: userId={}, email={}", user.id(), email);
            
            // 토큰을 직접 반환 (실제 환경에서는 이메일로 발송)
            return resetToken;
            
        } catch (Exception e) {
            log.error("비밀번호 재설정 요청 실패: email={}, error={}", email, e.getMessage());
            throw new DoranDoranException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * 비밀번호 재설정 실행
     */
    @Transactional
    public void executePasswordReset(String token, String newPassword) {
        log.info("비밀번호 재설정 실행: token={}", token);
        
        try {
            // 토큰 검증
            PasswordResetToken resetToken = passwordResetService.findByRawToken(token)
                .orElseThrow(() -> new DoranDoranException(ErrorCode.AUTH_TOKEN_INVALID));
            
            // 토큰 만료 확인
            if (resetToken.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
                throw new DoranDoranException(ErrorCode.AUTH_TOKEN_EXPIRED);
            }
            
            // 토큰 사용 여부 확인
            if (resetToken.isUsed()) {
                throw new DoranDoranException(ErrorCode.AUTH_TOKEN_INVALID);
            }
            
            // 새 비밀번호 검증
            validatePasswordPolicy(newPassword);
            
            // User 서비스에서 비밀번호 업데이트
            userIntegrationService.updatePassword(resetToken.getUser().getId(), newPassword);
            
            // 토큰 사용 처리
            passwordResetService.markUsed(resetToken);
            
            // 이벤트 로깅
            recordAuthEvent(resetToken.getUser().getId(), "PASSWORD_RESET_COMPLETED");
            
            log.info("비밀번호 재설정 완료: userId={}", resetToken.getUser().getId());
            
        } catch (DoranDoranException e) {
            log.error("비밀번호 재설정 실행 실패: token={}, error={}", token, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("비밀번호 재설정 실행 중 예상치 못한 오류: token={}, error={}", token, e.getMessage());
            throw new DoranDoranException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * 비밀번호 재설정 토큰 생성
     */
    private String generatePasswordResetToken() {
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * 비밀번호 정책 검증
     */
    private void validatePasswordPolicy(String password) {
        if (password == null || password.length() < 8) {
            throw new DoranDoranException(ErrorCode.INVALID_PASSWORD_FORMAT);
        }
        
        // 영문, 숫자 포함 검증
        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        
        if (!hasLetter || !hasDigit) {
            throw new DoranDoranException(ErrorCode.INVALID_PASSWORD_FORMAT);
        }
    }
}