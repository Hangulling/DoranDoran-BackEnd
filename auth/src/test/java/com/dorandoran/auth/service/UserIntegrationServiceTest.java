package com.dorandoran.auth.service;

import com.dorandoran.auth.client.UserServiceClient;
import com.dorandoran.shared.dto.UserDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * User Integration Service 테스트
 */
@ExtendWith(MockitoExtension.class)
class UserIntegrationServiceTest {

    @Mock
    private UserServiceClient userServiceClient;

    @InjectMocks
    private UserIntegrationService userIntegrationService;

    private UserDto testUser;
    private String testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID().toString();
        testUser = new UserDto(
            UUID.fromString(testUserId),
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
    }

    @Test
    void 사용자_ID로_조회_성공() {
        // Given
        when(userServiceClient.getUserById(testUserId)).thenReturn(testUser);

        // When
        UserDto result = userIntegrationService.getUserById(testUserId);

        // Then
        assertNotNull(result);
        assertEquals(testUserId, result.id().toString());
        assertEquals("test@example.com", result.email());
        assertEquals("홍길동", result.name());
        verify(userServiceClient).getUserById(testUserId);
    }

    @Test
    void 사용자_ID로_조회_실패() {
        // Given
        when(userServiceClient.getUserById(testUserId)).thenReturn(null);

        // When
        UserDto result = userIntegrationService.getUserById(testUserId);

        // Then
        assertNull(result);
        verify(userServiceClient).getUserById(testUserId);
    }

    @Test
    void 이메일로_사용자_조회_성공() {
        // Given
        when(userServiceClient.getUserByEmail("test@example.com")).thenReturn(testUser);

        // When
        UserDto result = userIntegrationService.getUserByEmail("test@example.com");

        // Then
        assertNotNull(result);
        assertEquals("test@example.com", result.email());
        assertEquals("홍길동", result.name());
        verify(userServiceClient).getUserByEmail("test@example.com");
    }

    @Test
    void 이메일로_사용자_조회_실패() {
        // Given
        when(userServiceClient.getUserByEmail("test@example.com")).thenReturn(null);

        // When
        UserDto result = userIntegrationService.getUserByEmail("test@example.com");

        // Then
        assertNull(result);
        verify(userServiceClient).getUserByEmail("test@example.com");
    }

    @Test
    void 헬스체크_성공() {
        // Given
        when(userServiceClient.healthCheck()).thenReturn("User service is running");

        // When
        String result = userIntegrationService.healthCheck();

        // Then
        assertEquals("User service is running", result);
        verify(userServiceClient).healthCheck();
    }

    @Test
    void 헬스체크_실패() {
        // Given
        when(userServiceClient.healthCheck()).thenReturn("User Service is unavailable");

        // When
        String result = userIntegrationService.healthCheck();

        // Then
        assertEquals("User Service is unavailable", result);
        verify(userServiceClient).healthCheck();
    }

    @Test
    void 사용자_ID로_비동기_조회_성공() throws Exception {
        // Given
        when(userServiceClient.getUserById(testUserId)).thenReturn(testUser);

        // When
        CompletableFuture<UserDto> future = userIntegrationService.getUserByIdAsync(testUserId);
        UserDto result = future.get();

        // Then
        assertNotNull(result);
        assertEquals(testUserId, result.id().toString());
        assertEquals("test@example.com", result.email());
        verify(userServiceClient).getUserById(testUserId);
    }
}
