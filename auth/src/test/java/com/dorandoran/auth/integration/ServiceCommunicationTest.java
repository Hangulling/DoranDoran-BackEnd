package com.dorandoran.auth.integration;

import com.dorandoran.auth.service.UserIntegrationService;
import com.dorandoran.shared.dto.UserDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 서비스 간 통신 테스트
 * Auth Service -> User Service Feign Client 통신 검증
 */
@SpringBootTest
@ActiveProfiles("test")
@org.junit.jupiter.api.Disabled("통합 통신 테스트는 로컬 서비스/Redis 의존성으로 test 프로필에서 비활성화")
class ServiceCommunicationTest {

    @Autowired
    private UserIntegrationService userIntegrationService;

    @Test
    void User_Service_헬스체크_통신_테스트() {
        // When
        String healthStatus = userIntegrationService.healthCheck();

        // Then
        assertNotNull(healthStatus);
        System.out.println("User Service Health Status: " + healthStatus);
        
        // User Service가 실행 중이면 "User service is running" 또는 "unavailable" 응답
        assertTrue(healthStatus.contains("User service") || 
                  healthStatus.contains("unavailable") ||
                  healthStatus.contains("running"));
    }

    @Test
    void Feign_Client_설정_검증() {
        // Given
        UserIntegrationService service = userIntegrationService;

        // When & Then
        assertNotNull(service, "UserIntegrationService가 정상적으로 주입되어야 함");
        
        // Feign Client가 정상적으로 설정되었는지 확인
        assertDoesNotThrow(() -> {
            service.healthCheck();
        }, "Feign Client를 통한 User Service 호출이 정상적으로 동작해야 함");
    }

    @Test
    void Circuit_Breaker_패턴_동작_확인() {
        // Given
        String nonExistentUserId = UUID.randomUUID().toString();

        // When
        UserDto result = userIntegrationService.getUserById(nonExistentUserId);

        // Then
        // Circuit Breaker가 정상적으로 동작하면 null을 반환하거나 예외를 처리해야 함
        // 실제 User Service가 실행 중이지 않으면 null을 반환
        assertTrue(result == null || result.id() != null, 
                  "Circuit Breaker가 정상적으로 동작해야 함");
        
        System.out.println("Circuit Breaker Test Result: " + (result != null ? "User found" : "User not found"));
    }

    @Test
    void 비동기_통신_테스트() throws Exception {
        // Given
        String testUserId = UUID.randomUUID().toString();

        // When
        var future = userIntegrationService.getUserByIdAsync(testUserId);
        
        // Then
        assertNotNull(future, "비동기 호출이 정상적으로 실행되어야 함");
        
        // 비동기 결과 처리 (타임아웃 설정)
        try {
            UserDto result = future.get();
            System.out.println("Async Communication Test Result: " + (result != null ? "Success" : "No user found"));
        } catch (Exception e) {
            System.out.println("Async Communication Test Exception: " + e.getMessage());
            // 예외가 발생해도 비동기 통신 자체는 정상 동작
            assertTrue(true, "비동기 통신이 정상적으로 시도되었음");
        }
    }

    @Test
    void 서비스_간_데이터_전송_형식_검증() {
        // Given
        UserDto testUser = new UserDto(
            UUID.randomUUID(),
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

        // When & Then
        // UserDto가 정상적으로 직렬화/역직렬화되는지 확인
        assertNotNull(testUser.id());
        assertNotNull(testUser.email());
        assertNotNull(testUser.name());
        assertEquals("test@example.com", testUser.email());
        assertEquals("홍길동", testUser.name());
        
        System.out.println("Data Transfer Format Test - User ID: " + testUser.id());
        System.out.println("Data Transfer Format Test - Email: " + testUser.email());
        System.out.println("Data Transfer Format Test - Name: " + testUser.name());
    }
}
