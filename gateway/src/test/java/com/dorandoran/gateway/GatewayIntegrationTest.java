package com.dorandoran.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Gateway 통합 테스트
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class GatewayIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    @DisplayName("Gateway 헬스체크 테스트")
    void healthCheck_Success() {
        // When & Then
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
    }

    @Test
    @DisplayName("Gateway 정보 엔드포인트 테스트")
    void infoEndpoint_Success() {
        // When & Then
        webTestClient.get()
                .uri("/actuator/info")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("Gateway 메트릭스 엔드포인트 테스트")
    void metricsEndpoint_Success() {
        // When & Then
        webTestClient.get()
                .uri("/actuator/metrics")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("Gateway 라우트 정보 엔드포인트 테스트")
    void gatewayRoutesEndpoint_Success() {
        // When & Then
        webTestClient.get()
                .uri("/actuator/gateway/routes")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("홈 엔드포인트 테스트")
    void homeEndpoint_Success() {
        // When & Then
        webTestClient.get()
                .uri("/")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.message").isEqualTo("DoranDoran MSA API Gateway")
                .jsonPath("$.status").isEqualTo("running")
                .jsonPath("$.version").isEqualTo("1.0.0");
    }

    @Test
    @DisplayName("CORS 설정 테스트")
    void corsConfiguration_Success() {
        // When & Then
        webTestClient.options()
                .uri("/api/users")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "Content-Type")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists("Access-Control-Allow-Origin")
                .expectHeader().exists("Access-Control-Allow-Methods")
                .expectHeader().exists("Access-Control-Allow-Headers");
    }
}
