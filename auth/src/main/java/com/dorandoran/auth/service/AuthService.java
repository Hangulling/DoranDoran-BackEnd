package com.dorandoran.auth.service;

import com.dorandoran.auth.service.UserIntegrationService;
import com.dorandoran.auth.dto.LoginRequest;
import com.dorandoran.auth.dto.LoginResponse;
import com.dorandoran.common.exception.DoranDoranException;
import com.dorandoran.common.exception.ErrorCode;
import com.dorandoran.shared.dto.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 인증 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UserIntegrationService userIntegrationService;
    
    /**
     * 로그인 (REST API 호출 방식)
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        log.info("사용자 로그인 요청: email={}", request.getEmail());
        
        try {
            // User 서비스에서 사용자 정보 조회
            UserDto user = userIntegrationService.getUserByEmail(request.getEmail());
            
            // 비밀번호 검증 (실제로는 User 서비스에서 검증해야 하지만, 
            // 현재는 Auth 서비스에서 검증)
            if (!passwordEncoder.matches(request.getPassword(), user.passwordHash())) {
                throw new DoranDoranException(ErrorCode.INVALID_PASSWORD);
            }
            
            // JWT 토큰 생성
            String accessToken = jwtService.generateAccessToken(user.id().toString(), user.email(), user.name());
            String refreshToken = jwtService.generateRefreshToken(user.id().toString(), user.email(), user.name());
            
            log.info("로그인 성공: userId={}, email={}", user.id(), user.email());
            
            return LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(3600L) // 1시간 (초 단위)
                    .userId(user.id().toString())
                    .email(user.email())
                    .name(user.name())
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
     * 토큰 검증 (REST API 호출 방식)
     */
    public UserDto validateToken(String token) {
        log.debug("토큰 검증: token={}", token);
        
        try {
            if (!jwtService.isTokenValid(token)) {
                throw new DoranDoranException(ErrorCode.AUTH_TOKEN_INVALID);
            }
            
            String userId = jwtService.extractUserId(token);
            
            // User 서비스에서 사용자 정보 조회
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
     * 토큰 갱신 (REST API 호출 방식)
     */
    public LoginResponse refreshToken(String refreshToken) {
        log.info("토큰 갱신 요청");
        
        try {
            if (!jwtService.isTokenValid(refreshToken)) {
                throw new DoranDoranException(ErrorCode.AUTH_TOKEN_EXPIRED);
            }
            
            String userId = jwtService.extractUserId(refreshToken);
            
            // User 서비스에서 사용자 정보 조회
            UserDto user = userIntegrationService.getUserById(userId);
            
            // 새 토큰 생성
            String newAccessToken = jwtService.generateAccessToken(user.id().toString(), user.email(), user.name());
            String newRefreshToken = jwtService.generateRefreshToken(user.id().toString(), user.email(), user.name());
            
            log.info("토큰 갱신 성공: userId={}", user.id());
            
            return LoginResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .tokenType("Bearer")
                    .expiresIn(3600L) // 1시간 (초 단위)
                    .userId(user.id().toString())
                    .email(user.email())
                    .name(user.name())
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
        log.info("사용자 로그아웃");
        
        try {
            // 현재는 단순히 로그만 남김
            // 실제 운영에서는 토큰을 블랙리스트에 추가하거나 Redis에서 제거
            String userId = jwtService.extractUserId(token);
            log.info("로그아웃 완료: userId={}", userId);
            
        } catch (Exception e) {
            log.error("로그아웃 중 오류 발생", e);
            // 로그아웃은 실패해도 사용자에게는 성공으로 처리
        }
    }
}
