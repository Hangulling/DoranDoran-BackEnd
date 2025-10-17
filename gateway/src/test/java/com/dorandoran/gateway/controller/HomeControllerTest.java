package com.dorandoran.gateway.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.springframework.test.web.reactive.server.WebTestClient.bindToController;

/**
 * HomeController 테스트
 */
@WebFluxTest(HomeController.class)
@Import(com.dorandoran.gateway.config.SecurityConfig.class)
class HomeControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    @DisplayName("홈 엔드포인트 테스트")
    void home_Success() {
        // When & Then
        webTestClient.get()
                .uri("/")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.message").isEqualTo("DoranDoran MSA API Gateway")
                .jsonPath("$.status").isEqualTo("running")
                .jsonPath("$.version").isEqualTo("1.0.0")
                .jsonPath("$.endpoints.actuator").isEqualTo("/actuator")
                .jsonPath("$.endpoints.auth").isEqualTo("/api/auth/**")
                .jsonPath("$.endpoints.user").isEqualTo("/api/users/**")
                .jsonPath("$.endpoints.chat").isEqualTo("/api/chat/**")
                .jsonPath("$.endpoints.store").isEqualTo("/api/store/**");
    }
}
