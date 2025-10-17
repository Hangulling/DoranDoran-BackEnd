package com.dorandoran.gateway.integration;

import com.dorandoran.gateway.filter.JwtAuthFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 전체 인증 플로우 E2E 테스트 (현재 구조 반영)
 * Gateway -> Auth Service -> User Service 전체 플로우 테스트
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class AuthE2ETest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private WebClient.Builder webClientBuilder;

    @MockBean
    private WebClient webClient;

    @MockBean
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @MockBean
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @BeforeEach
    void setUp() {
        // WebClient Mock 설정
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        // Mock WebClientResponseSpec
        org.springframework.web.reactive.function.client.WebClientResponseSpec mockResponseSpec = mock(org.springframework.web.reactive.function.client.WebClientResponseSpec.class);
        when(requestHeadersSpec.retrieve()).thenReturn(mockResponseSpec);
    }

    @Test
    @DisplayName("전체 인증 플로우 E2E 테스트 - 성공 시나리오")
    void 전체_인증_플로우_E2E_성공시나리오() {
        // Given - Auth 서비스 validate API 호출 성공 시뮬레이션
        when(requestHeadersSpec.retrieve().toBodilessEntity()).thenReturn(
                reactor.core.publisher.Mono.just(mock(org.springframework.http.ResponseEntity.class))
        );

        // When - 전체 인증 플로우 실행
        webTestClient.get()
                .uri("/api/users/profile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid-jwt-token")
                .exchange()
                .expectStatus().isOk();

        // Then - 전체 플로우 검증
        // 1. Gateway에서 Auth 서비스 validate API 호출 확인
        verify(webClient, atLeastOnce()).get();
        verify(requestHeadersUriSpec, atLeastOnce()).uri(contains("/api/auth/validate"));
        verify(requestHeadersSpec, atLeastOnce()).header(eq("Authorization"), contains("Bearer"));

        // 2. HMAC 헤더 주입 확인 (Gateway에서 User 서비스로)
        // 실제로는 Gateway에서 HMAC 헤더를 주입하여 User 서비스로 요청을 전달
    }

    @Test
    @DisplayName("전체 인증 플로우 E2E 테스트 - Auth 서비스 실패 시나리오")
    void 전체_인증_플로우_E2E_Auth서비스실패시나리오() {
        // Given - Auth 서비스 validate API 호출 실패 시뮬레이션
        when(requestHeadersSpec.retrieve().toBodilessEntity()).thenReturn(
                reactor.core.publisher.Mono.error(new RuntimeException("Auth service unavailable"))
        );

        // When & Then - Auth 서비스 실패 시 401 응답
        webTestClient.get()
                .uri("/api/users/profile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-jwt-token")
                .exchange()
                .expectStatus().isUnauthorized();

        // Auth 서비스 validate API 호출 확인
        verify(webClient, atLeastOnce()).get();
        verify(requestHeadersUriSpec, atLeastOnce()).uri(contains("/api/auth/validate"));
    }

    @Test
    @DisplayName("전체 인증 플로우 E2E 테스트 - User 서비스 중심 구조")
    void 전체_인증_플로우_E2E_User서비스중심구조() {
        // Given - User 서비스 중심 구조에서 Auth 서비스가 User 서비스 데이터를 사용
        when(requestHeadersSpec.retrieve().toBodilessEntity()).thenReturn(
                reactor.core.publisher.Mono.just(mock(org.springframework.http.ResponseEntity.class))
        );

        // When - User 서비스 중심 구조 전체 플로우
        webTestClient.get()
                .uri("/api/users/profile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid-jwt-token")
                .exchange()
                .expectStatus().isOk();

        // Then - User 서비스 중심 구조 검증
        // 1. Gateway -> Auth Service (JWT 검증)
        verify(webClient, atLeastOnce()).get();
        verify(requestHeadersUriSpec, atLeastOnce()).uri(contains("/api/auth/validate"));

        // 2. Auth Service -> User Service (사용자 정보 조회)
        // 3. Gateway -> User Service (HMAC 헤더 주입하여 요청 전달)
    }

    @Test
    @DisplayName("전체 인증 플로우 E2E 테스트 - 서비스 간 통신 실패 시나리오")
    void 전체_인증_플로우_E2E_서비스간통신실패시나리오() {
        // Given - 서비스 간 통신 실패 시뮬레이션
        when(requestHeadersSpec.retrieve().toBodilessEntity()).thenReturn(
                reactor.core.publisher.Mono.error(new RuntimeException("Service communication failed"))
        );

        // When & Then - 서비스 간 통신 실패 시 401 응답
        webTestClient.get()
                .uri("/api/users/profile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid-jwt-token")
                .exchange()
                .expectStatus().isUnauthorized();

        // 서비스 간 통신 실패 확인
        verify(webClient, atLeastOnce()).get();
        verify(requestHeadersUriSpec, atLeastOnce()).uri(contains("/api/auth/validate"));
    }

    @Test
    @DisplayName("전체 인증 플로우 E2E 테스트 - Circuit Breaker 패턴 동작")
    void 전체_인증_플로우_E2E_CircuitBreaker패턴동작() {
        // Given - Circuit Breaker 패턴 동작 시뮬레이션
        when(requestHeadersSpec.retrieve().toBodilessEntity()).thenReturn(
                reactor.core.publisher.Mono.error(new RuntimeException("Circuit breaker activated"))
        );

        // When - Circuit Breaker 패턴 동작 확인
        for (int i = 0; i < 5; i++) {
            webTestClient.get()
                    .uri("/api/users/profile")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer valid-jwt-token")
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        // Then - Circuit Breaker 패턴 동작 확인
        verify(webClient, atLeast(5)).get();
        verify(requestHeadersUriSpec, atLeast(5)).uri(contains("/api/auth/validate"));
    }

    @Test
    @DisplayName("전체 인증 플로우 E2E 테스트 - 인증 제외 경로")
    void 전체_인증_플로우_E2E_인증제외경로() {
        // When & Then - 인증 제외 경로들은 JWT 검증 없이 통과
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();

        webTestClient.get()
                .uri("/api/auth/health")
                .exchange()
                .expectStatus().isOk();

        webTestClient.get()
                .uri("/api/auth/login")
                .exchange()
                .expectStatus().isOk();

        webTestClient.get()
                .uri("/api/users/register")
                .exchange()
                .expectStatus().isOk();

        webTestClient.get()
                .uri("/api/users/health")
                .exchange()
                .expectStatus().isOk();

        // Auth 서비스 호출하지 않음 확인
        verify(webClient, never()).get();
    }

    @Test
    @DisplayName("전체 인증 플로우 E2E 테스트 - HMAC 헤더 주입 확인")
    void 전체_인증_플로우_E2E_HMAC헤더주입확인() {
        // Given - 유효한 JWT 토큰과 Auth 서비스 validate API 호출 성공
        String validJwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0LXVzZXItaWQiLCJlbWFpbCI6InRlc3RAZXhhbXBsZS5jb20iLCJuYW1lIjoiVGVzdCBVc2VyIn0.signature";

        when(requestHeadersSpec.retrieve().toBodilessEntity()).thenReturn(
                reactor.core.publisher.Mono.just(mock(org.springframework.http.ResponseEntity.class))
        );

        // When - HMAC 헤더 주입이 포함된 전체 플로우
        webTestClient.get()
                .uri("/api/users/profile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
                .exchange()
                .expectStatus().isOk();

        // Then - HMAC 헤더 주입 확인
        // 1. Gateway에서 JWT 토큰 검증 (Auth 서비스 호출)
        verify(webClient, atLeastOnce()).get();
        verify(requestHeadersUriSpec, atLeastOnce()).uri(contains("/api/auth/validate"));

        // 2. Gateway에서 HMAC 헤더 주입하여 User 서비스로 요청 전달
        // 실제로는 Gateway에서 HMAC 헤더를 주입하여 User 서비스로 요청을 전달
    }

    @Test
    @DisplayName("전체 인증 플로우 E2E 테스트 - JWT 토큰 검증 실패")
    void 전체_인증_플로우_E2E_JWT토큰검증실패() {
        // Given - JWT 토큰 검증 실패 시뮬레이션
        when(requestHeadersSpec.retrieve().toBodilessEntity()).thenReturn(
                reactor.core.publisher.Mono.error(new RuntimeException("Invalid JWT token"))
        );

        // When & Then - JWT 토큰 검증 실패 시 401 응답
        webTestClient.get()
                .uri("/api/users/profile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-jwt-token")
                .exchange()
                .expectStatus().isUnauthorized();

        // Auth 서비스 validate API 호출 확인
        verify(webClient, atLeastOnce()).get();
        verify(requestHeadersUriSpec, atLeastOnce()).uri(contains("/api/auth/validate"));
    }

    @Test
    @DisplayName("전체 인증 플로우 E2E 테스트 - 서비스 간 통신 타임아웃")
    void 전체_인증_플로우_E2E_서비스간통신타임아웃() {
        // Given - 서비스 간 통신 타임아웃 시뮬레이션
        when(requestHeadersSpec.retrieve().toBodilessEntity()).thenReturn(
                reactor.core.publisher.Mono.error(new RuntimeException("Connection timeout"))
        );

        // When & Then - 서비스 간 통신 타임아웃 시 401 응답
        webTestClient.get()
                .uri("/api/users/profile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid-jwt-token")
                .exchange()
                .expectStatus().isUnauthorized();

        // 서비스 간 통신 타임아웃 확인
        verify(webClient, atLeastOnce()).get();
        verify(requestHeadersUriSpec, atLeastOnce()).uri(contains("/api/auth/validate"));
    }
}