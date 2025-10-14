package com.dorandoran.auth.controller;

import com.dorandoran.auth.dto.LoginRequest;
import com.dorandoran.auth.dto.LoginResponse;
import com.dorandoran.auth.dto.RefreshTokenRequest;
import com.dorandoran.auth.service.AuthService;
import com.dorandoran.common.exception.DoranDoranException;
import com.dorandoran.common.exception.ErrorCode;
import com.dorandoran.shared.dto.UserDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthController 테스트
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    private LoginRequest loginRequest;
    private LoginResponse loginResponse;
    private UserDto userDto;
    private RefreshTokenRequest refreshTokenRequest;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 설정
        userDto = new UserDto(
                UUID.randomUUID(),
                "test@example.com",
                "Test",
                "User",
                "Test User",
                "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi", // bcrypt hash
                "https://example.com/profile.jpg",
                "Hello World",
                "{}", // preferences
                LocalDateTime.now(), // lastConnTime
                UserDto.UserStatus.ACTIVE,
                UserDto.RoleName.ROLE_USER,
                false, // coachCheck
                LocalDateTime.now(), // createdAt
                LocalDateTime.now()  // updatedAt
        );

        loginRequest = LoginRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        loginResponse = LoginResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .user(userDto)
                .build();

        refreshTokenRequest = RefreshTokenRequest.builder()
                .refreshToken("refresh-token")
                .build();
    }

    @Test
    @DisplayName("로그인 성공 테스트")
    void login_Success() throws Exception {
        // Given
        when(authService.login(any(LoginRequest.class))).thenReturn(loginResponse);

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(3600))
                .andExpect(jsonPath("$.data.user.id").value(userDto.id()))
                .andExpect(jsonPath("$.data.user.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.user.name").value("Test User"))
                .andExpect(jsonPath("$.data.user.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.message").value("로그인에 성공했습니다."));

        verify(authService, times(1)).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("로그인 실패 - 잘못된 비밀번호")
    void login_Failure_InvalidPassword() throws Exception {
        // Given
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new DoranDoranException(ErrorCode.INVALID_PASSWORD));

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("비밀번호가 올바르지 않습니다"));

        verify(authService, times(1)).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("로그인 실패 - 사용자 없음")
    void login_Failure_UserNotFound() throws Exception {
        // Given
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new DoranDoranException(ErrorCode.USER_NOT_FOUND));

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("사용자를 찾을 수 없습니다"));

        verify(authService, times(1)).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("로그인 실패 - 서버 오류")
    void login_Failure_ServerError() throws Exception {
        // Given
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new RuntimeException("Database connection failed"));

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("로그인 중 오류가 발생했습니다."));

        verify(authService, times(1)).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("로그아웃 성공 테스트")
    void logout_Success() throws Exception {
        // Given
        doNothing().when(authService).logout(anyString());

        // When & Then
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.message").value("로그아웃에 성공했습니다."));

        verify(authService, times(1)).logout("access-token");
    }

    @Test
    @DisplayName("로그아웃 성공 - Bearer 없이 토큰 전달")
    void logout_Success_WithoutBearer() throws Exception {
        // Given
        doNothing().when(authService).logout(anyString());

        // When & Then
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.message").value("로그아웃에 성공했습니다."));

        verify(authService, times(1)).logout("access-token");
    }

    @Test
    @DisplayName("로그아웃 성공 - 예외 발생해도 성공 처리")
    void logout_Success_EvenWithException() throws Exception {
        // Given
        doThrow(new RuntimeException("Redis connection failed")).when(authService).logout(anyString());

        // When & Then
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.message").value("로그아웃에 성공했습니다."));

        verify(authService, times(1)).logout("access-token");
    }

    @Test
    @DisplayName("토큰 검증 성공 테스트")
    void validateToken_Success() throws Exception {
        // Given
        when(authService.validateToken(anyString())).thenReturn(userDto);

        // When & Then
        mockMvc.perform(get("/api/auth/validate")
                        .header("Authorization", "Bearer access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.message").value("토큰이 유효합니다."));

        verify(authService, times(1)).validateToken("access-token");
    }

    @Test
    @DisplayName("토큰 검증 실패 - 유효하지 않은 토큰")
    void validateToken_Failure_InvalidToken() throws Exception {
        // Given
        doThrow(new DoranDoranException(ErrorCode.AUTH_TOKEN_INVALID))
                .when(authService).validateToken(anyString());

        // When & Then
        mockMvc.perform(get("/api/auth/validate")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("유효하지 않은 인증 토큰입니다"));

        verify(authService, times(1)).validateToken("invalid-token");
    }

    @Test
    @DisplayName("토큰 검증 실패 - 서버 오류")
    void validateToken_Failure_ServerError() throws Exception {
        // Given
        doThrow(new RuntimeException("Database connection failed"))
                .when(authService).validateToken(anyString());

        // When & Then
        mockMvc.perform(get("/api/auth/validate")
                        .header("Authorization", "Bearer access-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("토큰 검증 중 오류가 발생했습니다."));

        verify(authService, times(1)).validateToken("access-token");
    }

    @Test
    @DisplayName("토큰 갱신 성공 테스트")
    void refreshToken_Success() throws Exception {
        // Given
        when(authService.refreshToken(anyString())).thenReturn(loginResponse);

        // When & Then
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshTokenRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.message").value("토큰 갱신에 성공했습니다."));

        verify(authService, times(1)).refreshToken("refresh-token");
    }

    @Test
    @DisplayName("토큰 갱신 실패 - 유효하지 않은 리프레시 토큰")
    void refreshToken_Failure_InvalidRefreshToken() throws Exception {
        // Given
        when(authService.refreshToken(anyString()))
                .thenThrow(new DoranDoranException(ErrorCode.AUTH_TOKEN_INVALID));

        // When & Then
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshTokenRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("유효하지 않은 인증 토큰입니다"));

        verify(authService, times(1)).refreshToken("refresh-token");
    }

    @Test
    @DisplayName("헬스체크 테스트")
    void health_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/auth/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Auth service is running"));
    }

    @Test
    @DisplayName("로그인 요청 유효성 검증 실패")
    void login_ValidationFailure() throws Exception {
        // Given - 잘못된 이메일 형식
        LoginRequest invalidRequest = LoginRequest.builder()
                .email("invalid-email")
                .password("password123")
                .build();

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).login(any(LoginRequest.class));
    }
}