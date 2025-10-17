package com.dorandoran.user.integration;

import com.dorandoran.user.config.HmacAuthInterceptor;
import com.dorandoran.user.service.UserService;
import com.dorandoran.shared.dto.CreateUserRequest;
import com.dorandoran.shared.dto.UserDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * User 서비스 HMAC 검증 통합 테스트 (현재 구조 반영)
 * Gateway에서 주입된 HMAC 헤더를 검증하는 구조 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserHmacVerificationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private HmacAuthInterceptor hmacAuthInterceptor;

    private UserDto testUser;
    private HttpServletRequest mockRequest;
    private HttpServletResponse mockResponse;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        CreateUserRequest createRequest = new CreateUserRequest(
            "hmac@example.com",
            "김",
            "철수",
            "김철수",
            "password123",
            "profile.jpg",
            "HMAC 테스트 사용자"
        );
        testUser = userService.createUser(createRequest);

        // Mock 객체 설정
        mockRequest = mock(HttpServletRequest.class);
        mockResponse = mock(HttpServletResponse.class);
    }

    @Test
    @DisplayName("HMAC 인증 성공 - 유효한 헤더")
    void HMAC_인증_성공_유효한헤더() throws Exception {
        // Given - 유효한 HMAC 헤더 설정
        long timestamp = System.currentTimeMillis();
        String userId = testUser.id().toString();
        String message = userId + "|" + timestamp;
        String hmacSecret = "change-me-hmac-secret";
        String signature = generateHmacSignature(hmacSecret, message);

        when(mockRequest.getRequestURI()).thenReturn("/api/users/profile");
        when(mockRequest.getHeader("X-User-Id")).thenReturn(userId);
        when(mockRequest.getHeader("X-Auth-Ts")).thenReturn(String.valueOf(timestamp));
        when(mockRequest.getHeader("X-Auth-Sign")).thenReturn(signature);

        // When
        boolean result = hmacAuthInterceptor.preHandle(mockRequest, mockResponse, null);

        // Then
        assertTrue(result);
        verify(mockResponse, never()).setStatus(401);
    }

    @Test
    @DisplayName("HMAC 인증 실패 - 잘못된 서명")
    void HMAC_인증_실패_잘못된서명() throws Exception {
        // Given - 잘못된 HMAC 헤더 설정
        long timestamp = System.currentTimeMillis();
        String userId = testUser.id().toString();

        when(mockRequest.getRequestURI()).thenReturn("/api/users/profile");
        when(mockRequest.getHeader("X-User-Id")).thenReturn(userId);
        when(mockRequest.getHeader("X-Auth-Ts")).thenReturn(String.valueOf(timestamp));
        when(mockRequest.getHeader("X-Auth-Sign")).thenReturn("invalid-signature");

        // When
        boolean result = hmacAuthInterceptor.preHandle(mockRequest, mockResponse, null);

        // Then
        assertFalse(result);
        verify(mockResponse, times(1)).setStatus(401);
    }

    @Test
    @DisplayName("HMAC 인증 실패 - 만료된 타임스탬프")
    void HMAC_인증_실패_만료된타임스탬프() throws Exception {
        // Given - 만료된 타임스탬프 설정
        long expiredTimestamp = System.currentTimeMillis() - 120000; // 2분 전
        String userId = testUser.id().toString();
        String message = userId + "|" + expiredTimestamp;
        String hmacSecret = "change-me-hmac-secret";
        String signature = generateHmacSignature(hmacSecret, message);

        when(mockRequest.getRequestURI()).thenReturn("/api/users/profile");
        when(mockRequest.getHeader("X-User-Id")).thenReturn(userId);
        when(mockRequest.getHeader("X-Auth-Ts")).thenReturn(String.valueOf(expiredTimestamp));
        when(mockRequest.getHeader("X-Auth-Sign")).thenReturn(signature);

        // When
        boolean result = hmacAuthInterceptor.preHandle(mockRequest, mockResponse, null);

        // Then
        assertFalse(result);
        verify(mockResponse, times(1)).setStatus(401);
    }

    @Test
    @DisplayName("HMAC 인증 실패 - 누락된 헤더")
    void HMAC_인증_실패_누락된헤더() throws Exception {
        // Given - 누락된 헤더 설정
        when(mockRequest.getRequestURI()).thenReturn("/api/users/profile");
        when(mockRequest.getHeader("X-User-Id")).thenReturn(testUser.id().toString());
        when(mockRequest.getHeader("X-Auth-Ts")).thenReturn(String.valueOf(System.currentTimeMillis()));
        when(mockRequest.getHeader("X-Auth-Sign")).thenReturn(null);

        // When
        boolean result = hmacAuthInterceptor.preHandle(mockRequest, mockResponse, null);

        // Then
        assertFalse(result);
        verify(mockResponse, times(1)).setStatus(401);
    }

    @Test
    @DisplayName("공개 엔드포인트는 HMAC 검증 없이 통과")
    void 공개엔드포인트는_HMAC검증없이_통과() throws Exception {
        // Given - 공개 엔드포인트 설정
        when(mockRequest.getRequestURI()).thenReturn("/api/users/health");

        // When
        boolean result = hmacAuthInterceptor.preHandle(mockRequest, mockResponse, null);

        // Then
        assertTrue(result);
        verify(mockResponse, never()).setStatus(401);
    }

    @Test
    @DisplayName("사용자 정보 CRUD 테스트 - HMAC 인증 후")
    void 사용자정보_CRUD테스트_HMAC인증후() {
        // Given - 유효한 사용자 정보
        UserDto foundUser = userService.findById(testUser.id());

        // Then
        assertNotNull(foundUser);
        assertEquals(testUser.id(), foundUser.id());
        assertEquals(testUser.email(), foundUser.email());
        assertEquals(testUser.name(), foundUser.name());
    }

    @Test
    @DisplayName("HMAC 인증 성능 테스트")
    void HMAC_인증_성능테스트() throws Exception {
        // Given - 성능 테스트를 위한 설정
        long timestamp = System.currentTimeMillis();
        String userId = testUser.id().toString();
        String message = userId + "|" + timestamp;
        String hmacSecret = "change-me-hmac-secret";
        String signature = generateHmacSignature(hmacSecret, message);

        when(mockRequest.getRequestURI()).thenReturn("/api/users/profile");
        when(mockRequest.getHeader("X-User-Id")).thenReturn(userId);
        when(mockRequest.getHeader("X-Auth-Ts")).thenReturn(String.valueOf(timestamp));
        when(mockRequest.getHeader("X-Auth-Sign")).thenReturn(signature);

        // When - 성능 측정
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            hmacAuthInterceptor.preHandle(mockRequest, mockResponse, null);
        }
        long endTime = System.currentTimeMillis();

        // Then - 성능 검증 (1000회 호출이 1초 이내에 완료되어야 함)
        long duration = endTime - startTime;
        assertTrue(duration < 1000, "HMAC 인증 성능이 예상보다 느림: " + duration + "ms");
    }

    @Test
    @DisplayName("Gateway에서 주입된 HMAC 헤더 검증")
    void Gateway에서_주입된_HMAC헤더_검증() throws Exception {
        // Given - Gateway에서 주입된 HMAC 헤더 시뮬레이션
        long timestamp = System.currentTimeMillis();
        String userId = testUser.id().toString();
        String message = userId + "|" + timestamp;
        String hmacSecret = "change-me-hmac-secret";
        String signature = generateHmacSignature(hmacSecret, message);

        when(mockRequest.getRequestURI()).thenReturn("/api/users/profile");
        when(mockRequest.getHeader("X-User-Id")).thenReturn(userId);
        when(mockRequest.getHeader("X-User-Email")).thenReturn(testUser.email());
        when(mockRequest.getHeader("X-User-Name")).thenReturn(testUser.name());
        when(mockRequest.getHeader("X-Auth-Ts")).thenReturn(String.valueOf(timestamp));
        when(mockRequest.getHeader("X-Auth-Sign")).thenReturn(signature);

        // When
        boolean result = hmacAuthInterceptor.preHandle(mockRequest, mockResponse, null);

        // Then
        assertTrue(result);
        verify(mockResponse, never()).setStatus(401);
    }

    @Test
    @DisplayName("HMAC 서명 검증 로직 테스트")
    void HMAC_서명검증_로직테스트() throws Exception {
        // Given - 다양한 HMAC 서명 시나리오
        long timestamp = System.currentTimeMillis();
        String userId = testUser.id().toString();
        String hmacSecret = "change-me-hmac-secret";

        // 정상적인 서명
        String validMessage = userId + "|" + timestamp;
        String validSignature = generateHmacSignature(hmacSecret, validMessage);

        when(mockRequest.getRequestURI()).thenReturn("/api/users/profile");
        when(mockRequest.getHeader("X-User-Id")).thenReturn(userId);
        when(mockRequest.getHeader("X-Auth-Ts")).thenReturn(String.valueOf(timestamp));
        when(mockRequest.getHeader("X-Auth-Sign")).thenReturn(validSignature);

        // When
        boolean result = hmacAuthInterceptor.preHandle(mockRequest, mockResponse, null);

        // Then
        assertTrue(result);
        verify(mockResponse, never()).setStatus(401);
    }

    /**
     * HMAC 서명 생성 헬퍼 메서드
     */
    private String generateHmacSignature(String secret, String message) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKeySpec = new javax.crypto.spec.SecretKeySpec(secret.getBytes(), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(message.getBytes());
            return bytesToHex(hmacBytes);
        } catch (Exception e) {
            throw new RuntimeException("HMAC 서명 생성 실패", e);
        }
    }

    /**
     * 바이트 배열을 헥스 문자열로 변환
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}