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
        
        // 향후 커스텀 도메인용
        // corsConfig.addAllowedOrigin("https://www.doran-chat.com");
    

		// 프로덕션 도메인
		corsConfig.addAllowedOrigin("https://doran-chat.com");
		corsConfig.addAllowedOrigin("https://www.doran-chat.com");
		// 와일드카드 도메인 허용 (Spring 5.3+)
		corsConfig.addAllowedOrigin("https://*.doran-chat.com");
		corsConfig.addAllowedOrigin("https://doran-chat.vercel.app");
		corsConfig.addAllowedOrigin("https://*.vercel.app");
        corsConfig.addAllowedOrigin("https://*.doran-chat.com");

        corsConfig.addAllowedHeader("*");
        corsConfig.addAllowedMethod("*");
        corsConfig.addExposedHeader("*");

        // 운영 환경
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}
