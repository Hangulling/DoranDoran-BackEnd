package com.dorandoran.chat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 설정 (Chat 서비스)
 * User/Auth 서비스와 동일한 구조로 모든 API 요청을 허용하고,
 * 실제 인증은 HmacAuthInterceptor에서 처리
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                // Swagger UI 접근 허용
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/api-docs/**").permitAll()
                // Actuator 엔드포인트 허용
                .requestMatchers("/actuator/**").permitAll()
                // API 엔드포인트는 MSA 내부 통신이므로 허용 (HmacAuthInterceptor가 인증 담당)
                .requestMatchers("/api/**").permitAll()
                // 기타 모든 요청은 허용
                .anyRequest().permitAll()
            )
            .csrf(csrf -> csrf.disable())
            .httpBasic(httpBasic -> httpBasic.disable())
            .formLogin(formLogin -> formLogin.disable());
        
        return http.build();
    }
}
