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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AuthService 통합 테스트
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
            "Integration",
            "Test",
            "Integration Test",
            "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi", // bcrypt hash for "password123"
            "https://example.com/profile.jpg",
            "Integration Test User",
            null,
            UserDto.UserStatus.ACTIVE,
            UserDto.RoleName.ROLE_USER,
            false,
            null,
            null
        );

        loginRequest = LoginRequest.builder()
            .email("integration@example.com")
            .password("password123")
            .build();
    }

    @Test
    void 로그인_통합_테스트_성공() {
        // Given - UserIntegrationService를 모킹하여 실제 사용자 데이터 반환
        // (실제 환경에서는 User 서비스와 통신)
        
        // When
        LoginResponse response = authService.login(loginRequest);

        // Then
        assertNotNull(response);
        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(3600L, response.getExpiresIn());
        assertEquals("integration@example.com", response.getEmail());
        assertEquals("Integration Test", response.getName());
    }

    @Test
    void 로그인_통합_테스트_실패_잘못된_비밀번호() {
        // Given
        LoginRequest wrongPasswordRequest = LoginRequest.builder()
            .email("integration@example.com")
            .password("wrongpassword")
            .build();

        // When & Then
        DoranDoranException exception = assertThrows(DoranDoranException.class, () -> {
            authService.login(wrongPasswordRequest);
        });

        assertEquals(ErrorCode.INVALID_PASSWORD, exception.getErrorCode());
    }

    @Test
    void 토큰_검증_통합_테스트_성공() {
        // Given
        LoginResponse loginResponse = authService.login(loginRequest);
        String accessToken = loginResponse.getAccessToken();

        // When
        UserDto validatedUser = authService.validateToken(accessToken);

        // Then
        assertNotNull(validatedUser);
        assertEquals("integration@example.com", validatedUser.email());
        assertEquals("Integration Test", validatedUser.name());
    }

    @Test
    void 토큰_검증_통합_테스트_실패_유효하지_않은_토큰() {
        // Given
        String invalidToken = "invalid.token.here";

        // When & Then
        DoranDoranException exception = assertThrows(DoranDoranException.class, () -> {
            authService.validateToken(invalidToken);
        });

        assertEquals(ErrorCode.AUTH_TOKEN_INVALID, exception.getErrorCode());
    }

    @Test
    void 토큰_갱신_통합_테스트_성공() {
        // Given
        LoginResponse loginResponse = authService.login(loginRequest);
        String refreshToken = loginResponse.getRefreshToken();

        // When
        LoginResponse newTokens = authService.refreshToken(refreshToken);

        // Then
        assertNotNull(newTokens);
        assertNotNull(newTokens.getAccessToken());
        assertNotNull(newTokens.getRefreshToken());
        assertEquals("Bearer", newTokens.getTokenType());
        assertEquals(3600L, newTokens.getExpiresIn());
        assertEquals("integration@example.com", newTokens.getEmail());
        assertEquals("Integration Test", newTokens.getName());
        
        // 새 토큰이 이전 토큰과 다른지 확인
        assertNotEquals(loginResponse.getAccessToken(), newTokens.getAccessToken());
        assertNotEquals(loginResponse.getRefreshToken(), newTokens.getRefreshToken());
    }

    @Test
    void 토큰_갱신_통합_테스트_실패_유효하지_않은_리프레시_토큰() {
        // Given
        String invalidRefreshToken = "invalid.refresh.token";

        // When & Then
        DoranDoranException exception = assertThrows(DoranDoranException.class, () -> {
            authService.refreshToken(invalidRefreshToken);
        });

        assertEquals(ErrorCode.AUTH_TOKEN_EXPIRED, exception.getErrorCode());
    }

    @Test
    void 로그아웃_통합_테스트_성공() {
        // Given
        LoginResponse loginResponse = authService.login(loginRequest);
        String accessToken = loginResponse.getAccessToken();

        // When - 로그아웃은 예외가 발생하지 않아야 함
        assertDoesNotThrow(() -> {
            authService.logout(accessToken);
        });

        // Then - 로그아웃 후 토큰이 무효화되었는지 확인
        DoranDoranException exception = assertThrows(DoranDoranException.class, () -> {
            authService.validateToken(accessToken);
        });

        assertEquals(ErrorCode.AUTH_TOKEN_INVALID, exception.getErrorCode());
    }

    @Test
    void JWT_토큰_생성_및_검증_통합_테스트() {
        // Given
        String userId = "test-user-id";
        String email = "jwt@example.com";
        String name = "JWT Test User";

        // When
        String accessToken = jwtService.generateAccessToken(userId, email, name);
        String refreshToken = jwtService.generateRefreshToken(userId, email, name);

        // Then
        assertNotNull(accessToken);
        assertNotNull(refreshToken);
        
        // 토큰에서 정보 추출 검증
        assertEquals(userId, jwtService.extractUserId(accessToken));
        assertEquals(email, jwtService.extractEmail(accessToken));
        assertEquals(name, jwtService.extractName(accessToken));
        
        assertEquals(userId, jwtService.extractUserId(refreshToken));
        assertEquals(email, jwtService.extractEmail(refreshToken));
        assertEquals(name, jwtService.extractName(refreshToken));
        
        // 토큰 유효성 검증
        assertTrue(jwtService.isTokenValid(accessToken));
        assertTrue(jwtService.isTokenValid(refreshToken));
    }

    @Test
    void 사용자_조회_통합_테스트() {
        // Given
        String email = "integration@example.com";

        // When
        UserDto foundUser = authService.findUserByEmail(email);

        // Then
        assertNotNull(foundUser);
        assertEquals(email, foundUser.email());
    }

    @Test
    void 사용자_조회_통합_테스트_실패_사용자없음() {
        // Given
        String nonExistentEmail = "nonexistent@example.com";

        // When & Then
        DoranDoranException exception = assertThrows(DoranDoranException.class, () -> {
            authService.findUserByEmail(nonExistentEmail);
        });

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void 토큰_무효화_통합_테스트() {
        // Given
        String userId = "test-user-id";

        // When - 토큰 무효화는 예외가 발생하지 않아야 함
        assertDoesNotThrow(() -> {
            authService.invalidateAllTokensForUser(userId);
        });

        // Then - 메서드가 정상적으로 실행되었음을 확인
        // (실제 구현에서는 토큰이 무효화되었는지 검증할 수 있음)
    }
}
