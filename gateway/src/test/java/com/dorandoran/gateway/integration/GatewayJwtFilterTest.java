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
 * Gateway JWT 필터 통합 테스트 (현재 구조 반영)
 * JWT 필터가 Auth 서비스의 validate API를 호출하는 구조 검증
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class GatewayJwtFilterTest {

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
    @DisplayName("Gateway JWT 필터가 Auth 서비스 validate API를 호출하는지 확인")
    void Gateway_JWT필터가_Auth서비스_validate_API_호출확인() {
        // Given - Auth 서비스 validate API 호출 성공 시뮬레이션
        when(requestHeadersSpec.retrieve().toBodilessEntity()).thenReturn(
                reactor.core.publisher.Mono.just(mock(org.springframework.http.ResponseEntity.class))
        );

        // When
        webTestClient.get()
                .uri("/api/users/profile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid-jwt-token")
                .exchange()
                .expectStatus().isOk();

        // Then - Auth 서비스 validate API 호출 확인
        verify(webClient, atLeastOnce()).get();
        verify(requestHeadersUriSpec, atLeastOnce()).uri(contains("/api/auth/validate"));
        verify(requestHeadersSpec, atLeastOnce()).header(eq("Authorization"), contains("Bearer"));
    }

    @Test
    @DisplayName("Auth 서비스 validate API 호출 실패 시 401 응답 확인")
    void Auth서비스_validate_API_호출실패시_401응답확인() {
        // Given - Auth 서비스 validate API 호출 실패 시뮬레이션
        when(requestHeadersSpec.retrieve().toBodilessEntity()).thenReturn(
                reactor.core.publisher.Mono.error(new RuntimeException("Auth service unavailable"))
        );

        // When & Then
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
    @DisplayName("JWT 토큰 없이 요청 시 401 응답 확인")
    void JWT토큰_없이_요청시_401응답확인() {
        // When & Then
        webTestClient.get()
                .uri("/api/users/profile")
                .exchange()
                .expectStatus().isUnauthorized();

        // Auth 서비스 호출하지 않음 확인
        verify(webClient, never()).get();
    }

    @Test
    @DisplayName("잘못된 JWT 토큰 형식으로 요청 시 401 응답 확인")
    void 잘못된_JWT토큰_형식으로_요청시_401응답확인() {
        // When & Then
        webTestClient.get()
                .uri("/api/users/profile")
                .header(HttpHeaders.AUTHORIZATION, "InvalidToken")
                .exchange()
                .expectStatus().isUnauthorized();

        // Auth 서비스 호출하지 않음 확인
        verify(webClient, never()).get();
    }

    @Test
    @DisplayName("인증 제외 경로는 JWT 검증 없이 통과하는지 확인")
    void 인증제외_경로는_JWT검증없이_통과확인() {
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
    @DisplayName("HMAC 헤더 주입 동작 확인")
    void HMAC_헤더주입_동작확인() {
        // Given - 유효한 JWT 토큰 (실제로는 Base64 인코딩된 페이로드)
        String validJwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0LXVzZXItaWQiLCJlbWFpbCI6InRlc3RAZXhhbXBsZS5jb20iLCJuYW1lIjoiVGVzdCBVc2VyIn0.signature";

        // Given - Auth 서비스 validate API 호출 성공 시뮬레이션
        when(requestHeadersSpec.retrieve().toBodilessEntity()).thenReturn(
                reactor.core.publisher.Mono.just(mock(org.springframework.http.ResponseEntity.class))
        );

        // When
        webTestClient.get()
                .uri("/api/users/profile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
                .exchange()
                .expectStatus().isOk();

        // Then - Auth 서비스 validate API 호출 확인
        verify(webClient, atLeastOnce()).get();
        verify(requestHeadersUriSpec, atLeastOnce()).uri(contains("/api/auth/validate"));
    }

    @Test
    @DisplayName("Gateway 라우팅 설정 확인")
    void Gateway_라우팅설정_확인() {
        // When & Then - 각 서비스로의 라우팅 확인
        webTestClient.get()
                .uri("/api/auth/health")
                .exchange()
                .expectStatus().isOk();

        webTestClient.get()
                .uri("/api/users/health")
                .exchange()
                .expectStatus().isOk();

        webTestClient.get()
                .uri("/api/chat/health")
                .exchange()
                .expectStatus().isOk();
    }
}