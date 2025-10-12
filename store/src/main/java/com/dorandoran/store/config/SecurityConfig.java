package com.dorandoran.store.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 설정
 * Store Service는 내부 MSA 서비스로, API Gateway에서 이미 인증을 처리함
 * 전달된 내용을 바탕으로 X-User-ID 헤더만 검증하고 Security Context는 사용하지 않음 : Controller에서 X-User-ID 검증
 *
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(authz -> authz
            // Swagger UI 접근 허용
            .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**",
                "/api-docs/**").permitAll()
            // Actuator 엔드포인트 허용
            .requestMatchers("/actuator/**").permitAll()
            // API 엔드포인트 허용 (API Gateway가 이미 인증 처리)
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