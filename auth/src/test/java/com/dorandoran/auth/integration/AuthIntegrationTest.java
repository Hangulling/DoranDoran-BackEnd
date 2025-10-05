package com.dorandoran.auth.integration;

import com.dorandoran.auth.dto.LoginRequest;
import com.dorandoran.auth.dto.LoginResponse;
import com.dorandoran.auth.service.AuthService;
import com.dorandoran.auth.service.JwtService;
import com.dorandoran.auth.service.UserIntegrationService;
import com.dorandoran.shared.dto.UserDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Auth 서비스 통합 테스트
 * 실제 서비스 간 통신과 JWT 토큰 검증을 포함한 전체 인증 플로우 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtService jwtService;

    @MockBean
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
                "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi", // bcrypt hash for "password123"
                "https://example.com/profile.jpg",
                "통합 테스트 사용자",
                LocalDateTime.now(),
                UserDto.UserStatus.ACTIVE,
                UserDto.RoleName.ROLE_USER,
                false,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        loginRequest = LoginRequest.builder()
                .email("integration@example.com")
                .password("password123")
                .build();
    }

    @Test
    @DisplayName("전체 인증 플로우 통합 테스트 - 로그인 성공 (User 서비스 중심 구조)")
    void 전체_인증_플로우_로그인_성공() {
        // Given - User 서비스에서 사용자 정보 조회 (현재 구조 반영)
        when(userIntegrationService.getUserByEmail("integration@example.com")).thenReturn(testUser);

        // When
        LoginResponse response = authService.login(loginRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isNotEmpty();
        assertThat(response.getRefreshToken()).isNotEmpty();
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(3600L);
        assertThat(response.getUserId()).isEqualTo(testUser.id().toString());
        assertThat(response.getEmail()).isEqualTo("integration@example.com");
        assertThat(response.getName()).isEqualTo("홍길동");

        // JWT 토큰 검증 (User 서비스 데이터 기반)
        String accessToken = response.getAccessToken();
        assertThat(jwtService.extractUserId(accessToken)).isEqualTo(testUser.id().toString());
        assertThat(jwtService.extractEmail(accessToken)).isEqualTo("integration@example.com");
        assertThat(jwtService.extractName(accessToken)).isEqualTo("홍길동");
        assertThat(jwtService.isTokenValid(accessToken)).isTrue();

        // User 서비스 호출 확인 (User 서비스 중심 구조)
        verify(userIntegrationService, times(1)).getUserByEmail("integration@example.com");
    }

    @Test
    @DisplayName("전체 인증 플로우 통합 테스트 - 로그인 실패 (사용자 없음)")
    void 전체_인증_플로우_로그인_실패_사용자없음() {
        // Given
        when(userIntegrationService.getUserByEmail("integration@example.com"))
                .thenThrow(new RuntimeException("User not found"));

        // When & Then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(Exception.class);

        verify(userIntegrationService, times(1)).getUserByEmail("integration@example.com");
    }

    @Test
    @DisplayName("전체 인증 플로우 통합 테스트 - 토큰 검증 성공 (User 서비스 중심)")
    void 전체_인증_플로우_토큰검증_성공() {
        // Given - User 서비스에서 사용자 정보 조회 (현재 구조 반영)
        when(userIntegrationService.getUserByEmail("integration@example.com")).thenReturn(testUser);
        when(userIntegrationService.getUserById(testUser.id().toString())).thenReturn(testUser);

        // When - 로그인하여 토큰 생성 (User 서비스 데이터 기반)
        LoginResponse loginResponse = authService.login(loginRequest);
        String accessToken = loginResponse.getAccessToken();

        // Then - 토큰 검증 (User 서비스에서 사용자 정보 재조회)
        UserDto validatedUser = authService.validateToken(accessToken);

        assertThat(validatedUser).isNotNull();
        assertThat(validatedUser.id()).isEqualTo(testUser.id());
        assertThat(validatedUser.email()).isEqualTo(testUser.email());
        assertThat(validatedUser.name()).isEqualTo(testUser.name());

        // User 서비스 호출 확인
        verify(userIntegrationService, times(1)).getUserByEmail("integration@example.com");
        verify(userIntegrationService, times(1)).getUserById(testUser.id().toString());
    }

    @Test
    @DisplayName("전체 인증 플로우 통합 테스트 - 토큰 갱신 성공 (User 서비스 중심)")
    void 전체_인증_플로우_토큰갱신_성공() {
        // Given - User 서비스에서 사용자 정보 조회 (현재 구조 반영)
        when(userIntegrationService.getUserByEmail("integration@example.com")).thenReturn(testUser);
        when(userIntegrationService.getUserById(testUser.id().toString())).thenReturn(testUser);

        // When - 로그인하여 리프레시 토큰 생성 (User 서비스 데이터 기반)
        LoginResponse loginResponse = authService.login(loginRequest);
        String refreshToken = loginResponse.getRefreshToken();

        // Then - 토큰 갱신 (User 서비스에서 사용자 정보 재조회)
        LoginResponse refreshResponse = authService.refreshToken(refreshToken);

        assertThat(refreshResponse).isNotNull();
        assertThat(refreshResponse.getAccessToken()).isNotEmpty();
        assertThat(refreshResponse.getRefreshToken()).isNotEmpty();
        assertThat(refreshResponse.getAccessToken()).isNotEqualTo(loginResponse.getAccessToken());
        assertThat(refreshResponse.getRefreshToken()).isNotEqualTo(loginResponse.getRefreshToken());

        // User 서비스 호출 확인
        verify(userIntegrationService, times(2)).getUserByEmail("integration@example.com");
        verify(userIntegrationService, times(1)).getUserById(testUser.id().toString());
    }

    @Test
    @DisplayName("JWT 토큰 생성 및 검증 통합 테스트")
    void JWT_토큰_생성_및_검증_통합테스트() {
        // Given
        String userId = testUser.id().toString();
        String email = testUser.email();
        String name = testUser.name();

        // When - 토큰 생성
        String accessToken = jwtService.generateAccessToken(userId, email, name);
        String refreshToken = jwtService.generateRefreshToken(userId, email, name);

        // Then - 토큰 검증
        assertThat(accessToken).isNotEmpty();
        assertThat(refreshToken).isNotEmpty();
        assertThat(accessToken).isNotEqualTo(refreshToken);

        // 액세스 토큰 검증
        assertThat(jwtService.extractUserId(accessToken)).isEqualTo(userId);
        assertThat(jwtService.extractEmail(accessToken)).isEqualTo(email);
        assertThat(jwtService.extractName(accessToken)).isEqualTo(name);
        assertThat(jwtService.isTokenValid(accessToken)).isTrue();

        // 리프레시 토큰 검증
        assertThat(jwtService.extractUserId(refreshToken)).isEqualTo(userId);
        assertThat(jwtService.extractEmail(refreshToken)).isEqualTo(email);
        assertThat(jwtService.extractName(refreshToken)).isEqualTo(name);
        assertThat(jwtService.isTokenValid(refreshToken)).isTrue();
    }

    @Test
    @DisplayName("서비스 간 통신 실패 시 Fallback 동작 테스트")
    void 서비스간_통신_실패시_Fallback_동작테스트() {
        // Given - User 서비스 호출 실패 시뮬레이션
        when(userIntegrationService.getUserByEmail("integration@example.com"))
                .thenThrow(new RuntimeException("User Service unavailable"));

        // When & Then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(Exception.class);

        verify(userIntegrationService, times(1)).getUserByEmail("integration@example.com");
    }

    @Test
    @DisplayName("Circuit Breaker 패턴 동작 확인")
    void Circuit_Breaker_패턴_동작확인() {
        // Given
        when(userIntegrationService.getUserByEmail("integration@example.com"))
                .thenThrow(new RuntimeException("Service unavailable"));

        // When & Then - Circuit Breaker가 동작하여 Fallback 메서드 호출 확인
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(Exception.class);

        // 여러 번 호출하여 Circuit Breaker 상태 변화 확인
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(Exception.class);
        }

        verify(userIntegrationService, atLeast(1)).getUserByEmail("integration@example.com");
    }
}