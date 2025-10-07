package com.dorandoran.auth.service;

import com.dorandoran.auth.dto.LoginRequest;
import com.dorandoran.auth.dto.LoginResponse;
import com.dorandoran.auth.repository.LoginAttemptRepository;
import com.dorandoran.auth.repository.AuthEventRepository;
import com.dorandoran.common.exception.DoranDoranException;
import com.dorandoran.common.exception.ErrorCode;
import com.dorandoran.shared.dto.UserDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AuthService 테스트
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserIntegrationService userIntegrationService;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private LoginAttemptRepository loginAttemptRepository;

    @Mock
    private AuthEventRepository authEventRepository;

    @InjectMocks
    private AuthService authService;

    private UserDto userDto;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        userDto = new UserDto(
                UUID.randomUUID(),
                "test@example.com",
                "Test",
                "User",
                "Test User",
                "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi", // bcrypt hash
                "https://example.com/profile.jpg",
                "Hello World",
                null, // lastConnTime
                UserDto.UserStatus.ACTIVE,
                UserDto.RoleName.ROLE_USER,
                false, // coachCheck
                null, // createdAt
                null  // updatedAt
        );

        loginRequest = LoginRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();
    }

    @Test
    @DisplayName("로그인 성공 테스트")
    void login_Success() {
        // Given
        when(userIntegrationService.getUserByEmail("test@example.com")).thenReturn(userDto);
        when(passwordEncoder.matches("password123", userDto.passwordHash())).thenReturn(true);
        when(jwtService.generateAccessToken(anyString(), anyString(), anyString())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(anyString(), anyString(), anyString())).thenReturn("refresh-token");

        // When
        LoginResponse response = authService.login(loginRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(3600L);
        assertThat(response.getUserId()).isEqualTo(userDto.id().toString());
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getName()).isEqualTo("Test User");

        verify(userIntegrationService, times(1)).getUserByEmail("test@example.com");
        verify(passwordEncoder, times(1)).matches("password123", userDto.passwordHash());
        verify(jwtService, times(1)).generateAccessToken(userDto.id().toString(), userDto.email(), userDto.name());
        verify(jwtService, times(1)).generateRefreshToken(userDto.id().toString(), userDto.email(), userDto.name());
    }

    @Test
    @DisplayName("로그인 실패 - 사용자 없음 (코드 검증)")
    void login_Failure_UserNotFound() {
        // Given
        when(userIntegrationService.getUserByEmail("test@example.com"))
                .thenThrow(new DoranDoranException(ErrorCode.USER_NOT_FOUND));

        // When & Then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(DoranDoranException.class)
                .satisfies(throwable -> {
                    DoranDoranException ex = (DoranDoranException) throwable;
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
                });

        verify(userIntegrationService, times(1)).getUserByEmail("test@example.com");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtService, never()).generateAccessToken(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("로그인 실패 - 잘못된 비밀번호 (코드 검증)")
    void login_Failure_InvalidPassword() {
        // Given
        when(userIntegrationService.getUserByEmail("test@example.com")).thenReturn(userDto);
        when(passwordEncoder.matches("password123", userDto.passwordHash())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(DoranDoranException.class)
                .satisfies(throwable -> {
                    DoranDoranException ex = (DoranDoranException) throwable;
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_PASSWORD);
                });

        verify(userIntegrationService, times(1)).getUserByEmail("test@example.com");
        verify(passwordEncoder, times(1)).matches("password123", userDto.passwordHash());
        verify(jwtService, never()).generateAccessToken(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("로그인 실패 - 서버 오류 (코드 검증)")
    void login_Failure_ServerError() {
        // Given
        when(userIntegrationService.getUserByEmail("test@example.com"))
                .thenThrow(new RuntimeException("Database connection failed"));

        // When & Then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(DoranDoranException.class)
                .satisfies(throwable -> {
                    DoranDoranException ex = (DoranDoranException) throwable;
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR);
                });

        verify(userIntegrationService, times(1)).getUserByEmail("test@example.com");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("토큰 검증 성공 테스트")
    void validateToken_Success() {
        // Given
        String token = "valid-token";
        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractUserId(token)).thenReturn(userDto.id().toString());
        when(userIntegrationService.getUserById(userDto.id().toString())).thenReturn(userDto);

        // When
        UserDto result = authService.validateToken(token);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(userDto.id());
        assertThat(result.email()).isEqualTo(userDto.email());
        assertThat(result.name()).isEqualTo(userDto.name());

        verify(jwtService, times(1)).isTokenValid(token);
        verify(jwtService, times(1)).extractUserId(token);
        verify(userIntegrationService, times(1)).getUserById(userDto.id().toString());
    }

    @Test
    @DisplayName("토큰 검증 실패 - 유효하지 않은 토큰 (코드 검증)")
    void validateToken_Failure_InvalidToken() {
        // Given
        String token = "invalid-token";
        when(jwtService.isTokenValid(token)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.validateToken(token))
                .isInstanceOf(DoranDoranException.class)
                .satisfies(throwable -> {
                    DoranDoranException ex = (DoranDoranException) throwable;
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.AUTH_TOKEN_INVALID);
                });

        verify(jwtService, times(1)).isTokenValid(token);
        verify(jwtService, never()).extractUserId(anyString());
        verify(userIntegrationService, never()).getUserById(anyString());
    }

    @Test
    @DisplayName("토큰 검증 실패 - 사용자 없음 (코드 검증)")
    void validateToken_Failure_UserNotFound() {
        // Given
        String token = "valid-token";
        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractUserId(token)).thenReturn("non-existent-user-id");
        when(userIntegrationService.getUserById("non-existent-user-id"))
                .thenThrow(new DoranDoranException(ErrorCode.USER_NOT_FOUND));

        // When & Then
        assertThatThrownBy(() -> authService.validateToken(token))
                .isInstanceOf(DoranDoranException.class)
                .satisfies(throwable -> {
                    DoranDoranException ex = (DoranDoranException) throwable;
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
                });

        verify(jwtService, times(1)).isTokenValid(token);
        verify(jwtService, times(1)).extractUserId(token);
        verify(userIntegrationService, times(1)).getUserById("non-existent-user-id");
    }

    @Test
    @DisplayName("토큰 검증 실패 - 서버 오류 (코드 검증)")
    void validateToken_Failure_ServerError() {
        // Given
        String token = "valid-token";
        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractUserId(token)).thenReturn(userDto.id().toString());
        when(userIntegrationService.getUserById(userDto.id().toString()))
                .thenThrow(new RuntimeException("Database connection failed"));

        // When & Then
        assertThatThrownBy(() -> authService.validateToken(token))
                .isInstanceOf(DoranDoranException.class)
                .satisfies(throwable -> {
                    DoranDoranException ex = (DoranDoranException) throwable;
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.AUTH_TOKEN_INVALID);
                });

        verify(jwtService, times(1)).isTokenValid(token);
        verify(jwtService, times(1)).extractUserId(token);
        verify(userIntegrationService, times(1)).getUserById(userDto.id().toString());
    }

    @Test
    @DisplayName("토큰 갱신 성공 테스트")
    void refreshToken_Success() {
        // Given
        String refreshToken = "valid-refresh-token";
        when(jwtService.isTokenValid(refreshToken)).thenReturn(true);
        when(jwtService.extractUserId(refreshToken)).thenReturn(userDto.id().toString());
        when(userIntegrationService.getUserById(userDto.id().toString())).thenReturn(userDto);
        when(jwtService.generateAccessToken(anyString(), anyString(), anyString())).thenReturn("new-access-token");
        when(jwtService.generateRefreshToken(anyString(), anyString(), anyString())).thenReturn("new-refresh-token");
        when(jwtService.extractExpiration(anyString())).thenReturn(new Date(System.currentTimeMillis() + 3600000));
        when(tokenBlacklistService.hashToken(anyString())).thenReturn("hashed-token");
        when(refreshTokenService.findByHash(anyString())).thenReturn(java.util.Optional.empty());
        when(refreshTokenService.issue(any(), anyString(), any(), any(), any(), any()))
                .thenReturn(new com.dorandoran.auth.entity.RefreshToken());

        // When
        LoginResponse response = authService.refreshToken(refreshToken);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(3600L);
        assertThat(response.getUserId()).isEqualTo(userDto.id().toString());
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getName()).isEqualTo("Test User");

        verify(jwtService, times(1)).isTokenValid(refreshToken);
        verify(jwtService, times(1)).extractUserId(refreshToken);
        verify(userIntegrationService, times(1)).getUserById(userDto.id().toString());
        verify(jwtService, times(1)).generateAccessToken(userDto.id().toString(), userDto.email(), userDto.name());
        verify(jwtService, times(1)).generateRefreshToken(userDto.id().toString(), userDto.email(), userDto.name());
    }

    @Test
    @DisplayName("토큰 갱신 실패 - 유효하지 않은 리프레시 토큰 (코드 검증)")
    void refreshToken_Failure_InvalidToken() {
        // Given
        String refreshToken = "invalid-refresh-token";
        when(jwtService.isTokenValid(refreshToken)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.refreshToken(refreshToken))
                .isInstanceOf(DoranDoranException.class)
                .satisfies(throwable -> {
                    DoranDoranException ex = (DoranDoranException) throwable;
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.AUTH_TOKEN_EXPIRED);
                });

        verify(jwtService, times(1)).isTokenValid(refreshToken);
        verify(jwtService, never()).extractUserId(anyString());
        verify(userIntegrationService, never()).getUserById(anyString());
    }

    @Test
    @DisplayName("토큰 갱신 실패 - 서버 오류 (코드 검증)")
    void refreshToken_Failure_ServerError() {
        // Given
        String refreshToken = "valid-refresh-token";
        when(jwtService.isTokenValid(refreshToken)).thenReturn(true);
        when(jwtService.extractUserId(refreshToken)).thenReturn(userDto.id().toString());
        when(userIntegrationService.getUserById(userDto.id().toString()))
                .thenThrow(new RuntimeException("Database connection failed"));

        // When & Then
        assertThatThrownBy(() -> authService.refreshToken(refreshToken))
                .isInstanceOf(DoranDoranException.class)
                .satisfies(throwable -> {
                    DoranDoranException ex = (DoranDoranException) throwable;
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR);
                });

        verify(jwtService, times(1)).isTokenValid(refreshToken);
        verify(jwtService, times(1)).extractUserId(refreshToken);
        verify(userIntegrationService, times(1)).getUserById(userDto.id().toString());
    }

    @Test
    @DisplayName("로그아웃 성공 테스트")
    void logout_Success() {
        // Given
        String token = "valid-token";
        Date expirationTime = new Date(System.currentTimeMillis() + 3600000); // 1시간 후
        when(jwtService.extractUserId(token)).thenReturn(userDto.id().toString());
        when(jwtService.extractExpiration(token)).thenReturn(expirationTime);
        doNothing().when(tokenBlacklistService).addToBlacklist(anyString(), anyString(), any(Duration.class));

        // When
        authService.logout(token);

        // Then
        verify(jwtService, times(1)).extractUserId(token);
        verify(jwtService, times(1)).extractExpiration(token);
        verify(tokenBlacklistService, times(1)).addToBlacklist(anyString(), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("로그아웃 성공 - 만료된 토큰")
    void logout_Success_ExpiredToken() {
        // Given
        String token = "expired-token";
        Date expirationTime = new Date(System.currentTimeMillis() - 3600000); // 1시간 전
        when(jwtService.extractUserId(token)).thenReturn(userDto.id().toString());
        when(jwtService.extractExpiration(token)).thenReturn(expirationTime);

        // When
        authService.logout(token);

        // Then
        verify(jwtService, times(1)).extractUserId(token);
        verify(jwtService, times(1)).extractExpiration(token);
        verify(tokenBlacklistService, never()).addToBlacklist(anyString(), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("로그아웃 성공 - 예외 발생해도 성공 처리")
    void logout_Success_EvenWithException() {
        // Given
        String token = "valid-token";
        when(jwtService.extractUserId(token)).thenThrow(new RuntimeException("JWT parsing failed"));

        // When
        authService.logout(token);

        // Then - 예외가 발생해도 메서드가 정상 종료되어야 함
        verify(jwtService, times(1)).extractUserId(token);
        verify(jwtService, never()).extractExpiration(anyString());
        verify(tokenBlacklistService, never()).addToBlacklist(anyString(), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("로그아웃 성공 - null 만료 시간")
    void logout_Success_NullExpiration() {
        // Given
        String token = "valid-token";
        when(jwtService.extractUserId(token)).thenReturn(userDto.id().toString());
        when(jwtService.extractExpiration(token)).thenReturn(null);

        // When
        authService.logout(token);

        // Then
        verify(jwtService, times(1)).extractUserId(token);
        verify(jwtService, times(1)).extractExpiration(token);
        verify(tokenBlacklistService, never()).addToBlacklist(anyString(), anyString(), any(Duration.class));
    }
}