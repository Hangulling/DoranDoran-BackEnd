package com.dorandoran.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeExchange(exchanges -> exchanges
                        // Actuator 엔드포인트는 모든 접근 허용
                        .pathMatchers("/actuator/**").permitAll()
                        // API 경로는 모든 접근 허용 (MSA 내부 통신용)
                        .pathMatchers("/api/**").permitAll()
                        // 루트 경로는 모든 접근 허용
                        .pathMatchers("/").permitAll()
                        // 기타 모든 요청은 인증 필요
                        .anyExchange().authenticated()
                )
                .build();
    }

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
