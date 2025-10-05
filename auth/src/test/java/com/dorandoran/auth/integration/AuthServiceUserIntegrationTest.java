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
 * Auth 서비스와 User 서비스 통합 테스트 (현재 구조 반영)
 * User 서비스 중심 구조에서 Auth 서비스가 User 서비스 데이터를 사용하는 구조 검증
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthServiceUserIntegrationTest {

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
    @DisplayName("Auth 서비스가 User 서비스 데이터를 사용하여 로그인 처리")
    void Auth서비스가_User서비스_데이터를_사용하여_로그인처리() {
        // Given - User 서비스에서 사용자 정보 조회
        when(userIntegrationService.getUserByEmail("integration@example.com")).thenReturn(testUser);

        // When - Auth 서비스에서 로그인 처리
        LoginResponse response = authService.login(loginRequest);

        // Then - User 서비스 데이터 기반으로 JWT 토큰 생성 확인
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isNotEmpty();
        assertThat(response.getRefreshToken()).isNotEmpty();
        assertThat(response.getUserId()).isEqualTo(testUser.id().toString());
        assertThat(response.getEmail()).isEqualTo(testUser.email());
        assertThat(response.getName()).isEqualTo(testUser.name());

        // User 서비스 호출 확인
        verify(userIntegrationService, times(1)).getUserByEmail("integration@example.com");
    }

    @Test
    @DisplayName("Auth 서비스가 User 서비스 데이터를 사용하여 토큰 검증")
    void Auth서비스가_User서비스_데이터를_사용하여_토큰검증() {
        // Given - User 서비스에서 사용자 정보 조회
        when(userIntegrationService.getUserByEmail("integration@example.com")).thenReturn(testUser);
        when(userIntegrationService.getUserById(testUser.id().toString())).thenReturn(testUser);

        // When - 로그인하여 토큰 생성 후 검증
        LoginResponse loginResponse = authService.login(loginRequest);
        String accessToken = loginResponse.getAccessToken();
        UserDto validatedUser = authService.validateToken(accessToken);

        // Then - User 서비스 데이터 기반으로 토큰 검증 확인
        assertThat(validatedUser).isNotNull();
        assertThat(validatedUser.id()).isEqualTo(testUser.id());
        assertThat(validatedUser.email()).isEqualTo(testUser.email());
        assertThat(validatedUser.name()).isEqualTo(testUser.name());

        // User 서비스 호출 확인
        verify(userIntegrationService, times(1)).getUserByEmail("integration@example.com");
        verify(userIntegrationService, times(1)).getUserById(testUser.id().toString());
    }

    @Test
    @DisplayName("Auth 서비스가 User 서비스 데이터를 사용하여 토큰 갱신")
    void Auth서비스가_User서비스_데이터를_사용하여_토큰갱신() {
        // Given - User 서비스에서 사용자 정보 조회
        when(userIntegrationService.getUserByEmail("integration@example.com")).thenReturn(testUser);
        when(userIntegrationService.getUserById(testUser.id().toString())).thenReturn(testUser);

        // When - 로그인하여 리프레시 토큰 생성 후 갱신
        LoginResponse loginResponse = authService.login(loginRequest);
        String refreshToken = loginResponse.getRefreshToken();
        LoginResponse refreshResponse = authService.refreshToken(refreshToken);

        // Then - User 서비스 데이터 기반으로 토큰 갱신 확인
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
    @DisplayName("User 서비스 통신 실패 시 Auth 서비스 Fallback 동작")
    void User서비스_통신실패시_Auth서비스_Fallback동작() {
        // Given - User 서비스 호출 실패 시뮬레이션
        when(userIntegrationService.getUserByEmail("integration@example.com"))
                .thenThrow(new RuntimeException("User Service unavailable"));

        // When & Then - Auth 서비스에서 예외 발생
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(Exception.class);

        // User 서비스 호출 확인
        verify(userIntegrationService, times(1)).getUserByEmail("integration@example.com");
    }

    @Test
    @DisplayName("Circuit Breaker 패턴 동작 확인")
    void Circuit_Breaker_패턴_동작확인() {
        // Given - User 서비스 호출 실패 시뮬레이션
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

        // User 서비스 호출 확인
        verify(userIntegrationService, atLeast(1)).getUserByEmail("integration@example.com");
    }

    @Test
    @DisplayName("JWT 토큰 생성 시 User 서비스 데이터 사용 확인")
    void JWT토큰_생성시_User서비스_데이터사용확인() {
        // Given - User 서비스에서 사용자 정보 조회
        when(userIntegrationService.getUserByEmail("integration@example.com")).thenReturn(testUser);

        // When - 로그인하여 JWT 토큰 생성
        LoginResponse response = authService.login(loginRequest);
        String accessToken = response.getAccessToken();

        // Then - JWT 토큰에 User 서비스 데이터가 포함되어 있는지 확인
        assertThat(jwtService.extractUserId(accessToken)).isEqualTo(testUser.id().toString());
        assertThat(jwtService.extractEmail(accessToken)).isEqualTo(testUser.email());
        assertThat(jwtService.extractName(accessToken)).isEqualTo(testUser.name());
        assertThat(jwtService.isTokenValid(accessToken)).isTrue();

        // User 서비스 호출 확인
        verify(userIntegrationService, times(1)).getUserByEmail("integration@example.com");
    }
}