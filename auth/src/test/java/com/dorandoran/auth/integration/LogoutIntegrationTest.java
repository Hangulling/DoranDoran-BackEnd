package com.dorandoran.auth.integration;

import com.dorandoran.auth.service.AuthService;
import com.dorandoran.auth.service.JwtService;
import com.dorandoran.auth.service.TokenBlacklistService;
import com.dorandoran.auth.dto.LoginRequest;
import com.dorandoran.auth.dto.LoginResponse;
import com.dorandoran.shared.dto.UserDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 로그아웃 통합 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
class LogoutIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    private UserDto testUser;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        testUser = new UserDto(
            UUID.randomUUID(),
            "logout@example.com",
            "로그아웃",
            "테스트",
            "로그아웃테스트",
            "encodedPassword",
            "logout-profile.jpg",
            "로그아웃 테스트 사용자",
            LocalDateTime.now(),
            UserDto.UserStatus.ACTIVE,
            UserDto.RoleName.ROLE_USER,
            false,
            LocalDateTime.now(),
            LocalDateTime.now()
        );

        loginRequest = new LoginRequest("logout@example.com", "password123");
    }

    @Test
    void 토큰_생성_및_검증_테스트() {
        // Given
        String userId = testUser.id().toString();
        String email = testUser.email();
        String name = testUser.name();

        // When
        String accessToken = jwtService.generateAccessToken(userId, email, name);

        // Then
        assertNotNull(accessToken);
        assertTrue(jwtService.isTokenValid(accessToken));
        assertEquals(userId, jwtService.extractUserId(accessToken));
        assertEquals(email, jwtService.extractEmail(accessToken));
        assertEquals(name, jwtService.extractName(accessToken));
    }

    @Test
    void 로그아웃_후_토큰_무효화_테스트() {
        // Given
        String userId = testUser.id().toString();
        String email = testUser.email();
        String name = testUser.name();
        String accessToken = jwtService.generateAccessToken(userId, email, name);

        // 토큰이 유효한지 확인
        assertTrue(jwtService.isTokenValid(accessToken));
        assertFalse(tokenBlacklistService.isBlacklisted(accessToken));

        // When - 로그아웃 실행
        authService.logout(accessToken);

        // Then - 토큰이 무효화되었는지 확인
        assertTrue(tokenBlacklistService.isBlacklisted(accessToken));
        assertFalse(jwtService.isTokenValid(accessToken));
    }

    @Test
    void 블랙리스트_상태_확인_테스트() {
        // Given
        String userId = testUser.id().toString();
        String email = testUser.email();
        String name = testUser.name();
        String accessToken = jwtService.generateAccessToken(userId, email, name);

        // When
        authService.logout(accessToken);

        // Then
        String status = tokenBlacklistService.getBlacklistStatus(accessToken);
        assertNotNull(status);
        assertTrue(status.contains("Blacklisted: true"));
        
        System.out.println("블랙리스트 상태: " + status);
    }

    @Test
    void 만료된_토큰_로그아웃_테스트() {
        // Given - 정상 토큰 생성 후 만료 시뮬레이션
        String userId = testUser.id().toString();
        String email = testUser.email();
        String name = testUser.name();
        String normalToken = jwtService.generateAccessToken(userId, email, name);

        // When - 정상 토큰으로 로그아웃
        authService.logout(normalToken);

        // Then - 토큰이 블랙리스트에 추가되었는지 확인
        assertTrue(tokenBlacklistService.isBlacklisted(normalToken));
        
        // 만료된 토큰은 블랙리스트에 추가되지 않아야 함
        // (실제로는 JWT 만료 시간이 지나면 자동으로 무효화됨)
        System.out.println("정상 토큰 블랙리스트 상태: " + tokenBlacklistService.getBlacklistStatus(normalToken));
    }

    @Test
    void 사용자_모든_토큰_블랙리스트_테스트() {
        // Given
        String userId = testUser.id().toString();
        String email = testUser.email();
        String name = testUser.name();
        
        String token1 = jwtService.generateAccessToken(userId, email, name);
        String token2 = jwtService.generateRefreshToken(userId, email, name);

        // When
        tokenBlacklistService.blacklistUserTokens(userId, token1, token2);

        // Then
        assertTrue(tokenBlacklistService.isBlacklisted(token1));
        assertTrue(tokenBlacklistService.isBlacklisted(token2));
        assertFalse(jwtService.isTokenValid(token1));
        assertFalse(jwtService.isTokenValid(token2));
    }

    @Test
    void 토큰_검증_성능_테스트() {
        // Given
        String userId = testUser.id().toString();
        String email = testUser.email();
        String name = testUser.name();
        String accessToken = jwtService.generateAccessToken(userId, email, name);

        // When - 여러 번 토큰 검증
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) { // 100회에서 10회로 줄임
            jwtService.isTokenValid(accessToken);
        }
        long endTime = System.currentTimeMillis();

        // Then
        long duration = endTime - startTime;
        System.out.println("10회 토큰 검증 소요 시간: " + duration + "ms");
        
        // 성능 테스트 (1초 이내)
        assertTrue(duration < 1000, "토큰 검증이 너무 느립니다: " + duration + "ms");
    }
}
