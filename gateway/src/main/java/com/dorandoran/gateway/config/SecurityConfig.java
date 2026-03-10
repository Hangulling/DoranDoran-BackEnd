package com.dorandoran.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

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
                // WebSocket 채팅: JwtAuthFilter에서 쿼리 token으로 검증 (브라우저는 헤더 미지원)
                .pathMatchers("/ws/chat/**").permitAll()
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

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowCredentials(true);

        // 로컬 개발 환경
        corsConfig.addAllowedOrigin("http://localhost:3000");
        corsConfig.addAllowedOrigin("http://localhost:3001");
        corsConfig.addAllowedOrigin("http://127.0.0.1:3000");
        corsConfig.addAllowedOrigin("http://127.0.0.1:3001");
        corsConfig.addAllowedOrigin("https://localhost");
        corsConfig.addAllowedOrigin("capacitor://localhost");
        corsConfig.addAllowedOrigin("ionic://localhost");
        
        // 프로덕션 도메인
        corsConfig.addAllowedOrigin("https://doran-chat.com");
        corsConfig.addAllowedOrigin("https://www.doran-chat.com");
        corsConfig.addAllowedOrigin("https://doran-chat.vercel.app");
        
        // 와일드카드 도메인 허용 (Spring 5.3+)
        // setAllowedOriginPatterns()를 사용하여 패턴 기반 허용
        corsConfig.addAllowedOriginPattern("https://*.doran-chat.com");
        corsConfig.addAllowedOriginPattern("https://*.vercel.app");
        corsConfig.addAllowedOriginPattern("https://*.netlify.app");

        corsConfig.addAllowedHeader("*");
        corsConfig.addAllowedMethod("*");
        corsConfig.addExposedHeader("*");

        // 운영 환경
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
    
    /**
     * Google OAuth 팝업과의 postMessage 통신을 위해 COOP 헤더를 설정하는 필터
     */
    @Bean
    public org.springframework.web.server.WebFilter coopHeaderFilter() {
        return (exchange, chain) -> {
            org.springframework.http.server.reactive.ServerHttpResponse response = exchange.getResponse();
            org.springframework.http.HttpHeaders headers = response.getHeaders();
            
            // COOP 헤더가 이미 설정되어 있지 않으면 unsafe-none으로 설정
            if (!headers.containsKey("Cross-Origin-Opener-Policy")) {
                headers.add("Cross-Origin-Opener-Policy", "unsafe-none");
            }
            
            return chain.filter(exchange);
        };
    }
}
