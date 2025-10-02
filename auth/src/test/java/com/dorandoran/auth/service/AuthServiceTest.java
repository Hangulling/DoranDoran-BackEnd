package com.dorandoran.auth.service;

import com.dorandoran.auth.dto.LoginRequest;
import com.dorandoran.auth.dto.LoginResponse;
import com.dorandoran.common.exception.DoranDoranException;
import com.dorandoran.common.exception.ErrorCode;
import com.dorandoran.shared.dto.UserDto;
import com.dorandoran.auth.service.UserIntegrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 인증 서비스 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserIntegrationService userIntegrationService;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private UserDto testUser;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        testUser = new UserDto(
            java.util.UUID.randomUUID(),
            "test@example.com",
            "홍",
            "길동",
            "홍길동",
            "encodedPassword",
            "profile.jpg",
            "테스트 사용자",
            LocalDateTime.now(),
            UserDto.UserStatus.ACTIVE,
            UserDto.RoleName.ROLE_USER,
            false,
            LocalDateTime.now(),
            LocalDateTime.now()
        );

        loginRequest = new LoginRequest("test@example.com", "password123");
    }

    @Test
    void 로그인_성공() {
        // Given
        when(userIntegrationService.getUserByEmail(anyString())).thenReturn(testUser);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtService.generateAccessToken(anyString(), anyString(), anyString())).thenReturn("accessToken");
        when(jwtService.generateRefreshToken(anyString(), anyString(), anyString())).thenReturn("refreshToken");

        // When
        LoginResponse result = authService.login(loginRequest);

        // Then
        assertNotNull(result);
        assertEquals("accessToken", result.getAccessToken());
        assertEquals("refreshToken", result.getRefreshToken());
        assertEquals("Bearer", result.getTokenType());
        assertEquals(testUser.id().toString(), result.getUserId());
        assertEquals("test@example.com", result.getEmail());
        assertEquals("홍길동", result.getName());

        verify(userIntegrationService).getUserByEmail("test@example.com");
        verify(passwordEncoder).matches("password123", "encodedPassword");
        verify(jwtService).generateAccessToken(testUser.id().toString(), "test@example.com", "홍길동");
        verify(jwtService).generateRefreshToken(testUser.id().toString(), "test@example.com", "홍길동");
    }

    @Test
    void 로그인_실패_비밀번호_불일치() {
        // Given
        when(userIntegrationService.getUserByEmail(anyString())).thenReturn(testUser);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        // When & Then
        DoranDoranException exception = assertThrows(DoranDoranException.class, () -> {
            authService.login(loginRequest);
        });

        assertEquals(ErrorCode.INVALID_PASSWORD, exception.getErrorCode());
        verify(userIntegrationService).getUserByEmail("test@example.com");
        verify(passwordEncoder).matches("password123", "encodedPassword");
        verify(jwtService, never()).generateAccessToken(anyString(), anyString(), anyString());
    }

    @Test
    void 토큰_검증_성공() {
        // Given
        String token = "validToken";
        when(jwtService.isTokenValid(anyString())).thenReturn(true);
        when(jwtService.extractUserId(anyString())).thenReturn(testUser.id().toString());
        when(userIntegrationService.getUserById(anyString())).thenReturn(testUser);

        // When
        UserDto result = authService.validateToken(token);

        // Then
        assertNotNull(result);
        assertEquals(testUser.id(), result.id());
        verify(jwtService).isTokenValid(token);
        verify(jwtService).extractUserId(token);
        verify(userIntegrationService).getUserById(testUser.id().toString());
    }

    @Test
    void 토큰_검증_실패_유효하지_않은_토큰() {
        // Given
        String token = "invalidToken";
        when(jwtService.isTokenValid(anyString())).thenReturn(false);

        // When & Then
        DoranDoranException exception = assertThrows(DoranDoranException.class, () -> {
            authService.validateToken(token);
        });

        assertEquals(ErrorCode.AUTH_TOKEN_INVALID, exception.getErrorCode());
        verify(jwtService).isTokenValid(token);
        verify(jwtService, never()).extractUserId(anyString());
        verify(userIntegrationService, never()).getUserById(anyString());
    }

    @Test
    void 토큰_갱신_성공() {
        // Given
        String refreshToken = "validRefreshToken";
        when(jwtService.isTokenValid(anyString())).thenReturn(true);
        when(jwtService.extractUserId(anyString())).thenReturn(testUser.id().toString());
        when(userIntegrationService.getUserById(anyString())).thenReturn(testUser);
        when(jwtService.generateAccessToken(anyString(), anyString(), anyString())).thenReturn("newAccessToken");
        when(jwtService.generateRefreshToken(anyString(), anyString(), anyString())).thenReturn("newRefreshToken");

        // When
        LoginResponse result = authService.refreshToken(refreshToken);

        // Then
        assertNotNull(result);
        assertEquals("newAccessToken", result.getAccessToken());
        assertEquals("newRefreshToken", result.getRefreshToken());
        verify(jwtService).isTokenValid(refreshToken);
        verify(jwtService).extractUserId(refreshToken);
        verify(userIntegrationService).getUserById(testUser.id().toString());
    }

    @Test
    void 로그아웃_성공() {
        // Given
        String token = "validToken";
        when(jwtService.extractUserId(anyString())).thenReturn(testUser.id().toString());

        // When
        assertDoesNotThrow(() -> authService.logout(token));

        // Then
        verify(jwtService).extractUserId(token);
    }
}
