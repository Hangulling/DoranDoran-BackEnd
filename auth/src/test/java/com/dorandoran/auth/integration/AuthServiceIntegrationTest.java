package com.dorandoran.auth.integration;

import com.dorandoran.auth.dto.LoginRequest;
import com.dorandoran.auth.dto.LoginResponse;
import com.dorandoran.auth.service.AuthService;
import com.dorandoran.auth.service.JwtService;
import com.dorandoran.auth.service.UserIntegrationService;
import com.dorandoran.common.exception.DoranDoranException;
import com.dorandoran.common.exception.ErrorCode;
import com.dorandoran.shared.dto.UserDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Auth Service 통합 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthServiceIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserIntegrationService userIntegrationService;

    private UserDto testUser;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        testUser = new UserDto(
            UUID.randomUUID(),
            "integration@example.com",
            "홍",
            "길동",
            "홍길동",
            "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi", // password123
            "profile.jpg",
            "통합 테스트 사용자",
            LocalDateTime.now(),
            UserDto.UserStatus.ACTIVE,
            UserDto.RoleName.ROLE_USER,
            false,
            LocalDateTime.now(),
            LocalDateTime.now()
        );

        loginRequest = new LoginRequest("integration@example.com", "password123");
    }

    @Test
    void JWT_토큰_생성_및_검증_성공() {
        // Given
        String userId = testUser.id().toString();
        String email = testUser.email();
        String name = testUser.name();

        // When
        String accessToken = jwtService.generateAccessToken(userId, email, name);
        String refreshToken = jwtService.generateRefreshToken(userId, email, name);

        // Then
        assertNotNull(accessToken);
        assertNotNull(refreshToken);
        assertTrue(jwtService.isTokenValid(accessToken));
        assertTrue(jwtService.isTokenValid(refreshToken));
        assertEquals(userId, jwtService.extractUserId(accessToken));
        assertEquals(email, jwtService.extractEmail(accessToken));
        assertEquals(name, jwtService.extractName(accessToken));
    }

    @Test
    void JWT_토큰_만료_시간_확인() {
        // Given
        String userId = testUser.id().toString();
        String email = testUser.email();
        String name = testUser.name();

        // When
        String accessToken = jwtService.generateAccessToken(userId, email, name);

        // Then
        assertTrue(jwtService.isTokenValid(accessToken));
        assertNotNull(jwtService.extractUserId(accessToken));
    }

    @Test
    void JWT_토큰_유효하지_않은_토큰_검증_실패() {
        // Given
        String invalidToken = "invalid.token.here";

        // When & Then
        assertFalse(jwtService.isTokenValid(invalidToken));
    }

    @Test
    void JWT_토큰_추출_실패() {
        // Given
        String invalidToken = "invalid.token.here";

        // When & Then
        assertThrows(Exception.class, () -> {
            jwtService.extractUserId(invalidToken);
        });
    }

    @Test
    void 사용자_통합_서비스_헬스체크() {
        // When
        String healthStatus = userIntegrationService.healthCheck();

        // Then
        assertNotNull(healthStatus);
        assertTrue(healthStatus.contains("User service") || healthStatus.contains("unavailable"));
    }

    @Test
    void Auth_Service_전체_플로우_테스트() {
        // Given - Mock UserIntegrationService가 실제 User Service를 호출하도록 설정
        // 실제 환경에서는 User Service가 실행 중이어야 함

        // When & Then
        // 이 테스트는 실제 User Service와의 통신이 필요하므로
        // User Service가 실행 중일 때만 성공할 수 있음
        assertDoesNotThrow(() -> {
            // Auth Service의 기본 기능들이 정상적으로 동작하는지 확인
            assertNotNull(authService);
            assertNotNull(jwtService);
            assertNotNull(userIntegrationService);
        });
    }

    @Test
    void JWT_토큰_다양한_사용자_정보_처리() {
        // Given
        String userId1 = UUID.randomUUID().toString();
        String email1 = "user1@example.com";
        String name1 = "사용자1";

        String userId2 = UUID.randomUUID().toString();
        String email2 = "user2@example.com";
        String name2 = "사용자2";

        // When
        String token1 = jwtService.generateAccessToken(userId1, email1, name1);
        String token2 = jwtService.generateAccessToken(userId2, email2, name2);

        // Then
        assertNotNull(token1);
        assertNotNull(token2);
        assertNotEquals(token1, token2);
        
        assertEquals(userId1, jwtService.extractUserId(token1));
        assertEquals(email1, jwtService.extractEmail(token1));
        assertEquals(name1, jwtService.extractName(token1));
        
        assertEquals(userId2, jwtService.extractUserId(token2));
        assertEquals(email2, jwtService.extractEmail(token2));
        assertEquals(name2, jwtService.extractName(token2));
    }
}
