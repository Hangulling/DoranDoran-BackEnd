package com.dorandoran.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * JwtService 테스트
 */
@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @InjectMocks
    private JwtService jwtService;

    private String testUserId;
    private String testEmail;
    private String testName;

    @BeforeEach
    void setUp() {
        // 테스트용 시크릿 키 설정 (실제 운영에서는 더 강력한 키 사용)
        ReflectionTestUtils.setField(jwtService, "secretKey", 
                "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 3600000L); // 1시간
        ReflectionTestUtils.setField(jwtService, "refreshExpiration", 604800000L); // 7일

        testUserId = "test-user-id";
        testEmail = "test@example.com";
        testName = "Test User";
    }

    @Test
    @DisplayName("액세스 토큰 생성 성공 테스트")
    void generateAccessToken_Success() {
        // When
        String token = jwtService.generateAccessToken(testUserId, testEmail, testName);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        
        // 토큰에서 정보 추출하여 검증
        assertThat(jwtService.extractUserId(token)).isEqualTo(testUserId);
        assertThat(jwtService.extractEmail(token)).isEqualTo(testEmail);
        assertThat(jwtService.extractName(token)).isEqualTo(testName);
    }

    @Test
    @DisplayName("리프레시 토큰 생성 성공 테스트")
    void generateRefreshToken_Success() {
        // When
        String token = jwtService.generateRefreshToken(testUserId, testEmail, testName);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        
        // 토큰에서 정보 추출하여 검증
        assertThat(jwtService.extractUserId(token)).isEqualTo(testUserId);
        assertThat(jwtService.extractEmail(token)).isEqualTo(testEmail);
        assertThat(jwtService.extractName(token)).isEqualTo(testName);
    }

    @Test
    @DisplayName("토큰에서 사용자 ID 추출 테스트")
    void extractUserId_Success() {
        // Given
        String token = jwtService.generateAccessToken(testUserId, testEmail, testName);

        // When
        String extractedUserId = jwtService.extractUserId(token);

        // Then
        assertThat(extractedUserId).isEqualTo(testUserId);
    }

    @Test
    @DisplayName("토큰에서 이메일 추출 테스트")
    void extractEmail_Success() {
        // Given
        String token = jwtService.generateAccessToken(testUserId, testEmail, testName);

        // When
        String extractedEmail = jwtService.extractEmail(token);

        // Then
        assertThat(extractedEmail).isEqualTo(testEmail);
    }

    @Test
    @DisplayName("토큰에서 이름 추출 테스트")
    void extractName_Success() {
        // Given
        String token = jwtService.generateAccessToken(testUserId, testEmail, testName);

        // When
        String extractedName = jwtService.extractName(token);

        // Then
        assertThat(extractedName).isEqualTo(testName);
    }

    @Test
    @DisplayName("토큰에서 만료 시간 추출 테스트")
    void extractExpiration_Success() {
        // Given
        String token = jwtService.generateAccessToken(testUserId, testEmail, testName);

        // When
        Date expiration = jwtService.extractExpiration(token);

        // Then
        assertThat(expiration).isNotNull();
        assertThat(expiration).isAfter(new Date());
    }

    @Test
    @DisplayName("유효한 토큰 검증 성공 테스트")
    void isTokenValid_Success() {
        // Given
        String token = jwtService.generateAccessToken(testUserId, testEmail, testName);
        when(tokenBlacklistService.isBlacklisted(token)).thenReturn(false);

        // When
        boolean isValid = jwtService.isTokenValid(token);

        // Then
        assertThat(isValid).isTrue();
        verify(tokenBlacklistService, times(1)).isBlacklisted(token);
    }

    @Test
    @DisplayName("블랙리스트에 있는 토큰 검증 실패 테스트")
    void isTokenValid_Failure_Blacklisted() {
        // Given
        String token = jwtService.generateAccessToken(testUserId, testEmail, testName);
        when(tokenBlacklistService.isBlacklisted(token)).thenReturn(true);

        // When
        boolean isValid = jwtService.isTokenValid(token);

        // Then
        assertThat(isValid).isFalse();
        verify(tokenBlacklistService, times(1)).isBlacklisted(token);
    }

    @Test
    @DisplayName("유효하지 않은 토큰 검증 실패 테스트")
    void isTokenValid_Failure_InvalidToken() {
        // Given
        String invalidToken = "invalid.token.here";

        // When
        boolean isValid = jwtService.isTokenValid(invalidToken);

        // Then
        assertThat(isValid).isFalse();
        verify(tokenBlacklistService, never()).isBlacklisted(anyString());
    }

    @Test
    @DisplayName("null 토큰 검증 실패 테스트")
    void isTokenValid_Failure_NullToken() {
        // When
        boolean isValid = jwtService.isTokenValid(null);

        // Then
        assertThat(isValid).isFalse();
        verify(tokenBlacklistService, never()).isBlacklisted(anyString());
    }

    @Test
    @DisplayName("빈 토큰 검증 실패 테스트")
    void isTokenValid_Failure_EmptyToken() {
        // When
        boolean isValid = jwtService.isTokenValid("");

        // Then
        assertThat(isValid).isFalse();
        verify(tokenBlacklistService, never()).isBlacklisted(anyString());
    }

    @Test
    @DisplayName("토큰 검증 중 예외 발생 테스트")
    void isTokenValid_Failure_Exception() {
        // Given
        String token = jwtService.generateAccessToken(testUserId, testEmail, testName);
        when(tokenBlacklistService.isBlacklisted(token)).thenThrow(new RuntimeException("Redis connection failed"));

        // When
        boolean isValid = jwtService.isTokenValid(token);

        // Then
        assertThat(isValid).isFalse();
        verify(tokenBlacklistService, times(1)).isBlacklisted(token);
    }

    @Test
    @DisplayName("다른 시크릿 키로 생성된 토큰 검증 실패 테스트")
    void isTokenValid_Failure_DifferentSecretKey() {
        // Given - 다른 시크릿 키로 토큰 생성
        ReflectionTestUtils.setField(jwtService, "secretKey", 
                "DifferentSecretKey123456789012345678901234567890");
        String token = jwtService.generateAccessToken(testUserId, testEmail, testName);
        
        // 원래 시크릿 키로 복원
        ReflectionTestUtils.setField(jwtService, "secretKey", 
                "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");

        // When
        boolean isValid = jwtService.isTokenValid(token);

        // Then
        assertThat(isValid).isFalse();
        verify(tokenBlacklistService, never()).isBlacklisted(anyString());
    }

    @Test
    @DisplayName("액세스 토큰과 리프레시 토큰의 만료 시간 차이 테스트")
    void tokenExpiration_Difference() {
        // Given
        String accessToken = jwtService.generateAccessToken(testUserId, testEmail, testName);
        String refreshToken = jwtService.generateRefreshToken(testUserId, testEmail, testName);

        // When
        Date accessTokenExpiration = jwtService.extractExpiration(accessToken);
        Date refreshTokenExpiration = jwtService.extractExpiration(refreshToken);

        // Then
        assertThat(refreshTokenExpiration).isAfter(accessTokenExpiration);
        
        // 리프레시 토큰이 액세스 토큰보다 더 오래 유효해야 함
        long timeDifference = refreshTokenExpiration.getTime() - accessTokenExpiration.getTime();
        assertThat(timeDifference).isGreaterThan(0);
    }

    @Test
    @DisplayName("토큰 생성 시 클레임 검증 테스트")
    void tokenClaims_Validation() {
        // Given
        String token = jwtService.generateAccessToken(testUserId, testEmail, testName);

        // When & Then
        assertThat(jwtService.extractUserId(token)).isEqualTo(testUserId);
        assertThat(jwtService.extractEmail(token)).isEqualTo(testEmail);
        assertThat(jwtService.extractName(token)).isEqualTo(testName);
        
        // 만료 시간이 미래여야 함
        Date expiration = jwtService.extractExpiration(token);
        assertThat(expiration).isAfter(new Date());
    }

    @Test
    @DisplayName("동일한 정보로 생성된 토큰의 클레임 일치 테스트")
    void tokenClaims_Equal_ForSameInputs() {
        // Given
        String token1 = jwtService.generateAccessToken(testUserId, testEmail, testName);
        String token2 = jwtService.generateAccessToken(testUserId, testEmail, testName);

        // When & Then: 토큰 문자열의 동일성은 보장하지 않지만, 클레임은 동일해야 함
        assertThat(jwtService.extractUserId(token1)).isEqualTo(jwtService.extractUserId(token2));
        assertThat(jwtService.extractEmail(token1)).isEqualTo(jwtService.extractEmail(token2));
        assertThat(jwtService.extractName(token1)).isEqualTo(jwtService.extractName(token2));
    }
}
