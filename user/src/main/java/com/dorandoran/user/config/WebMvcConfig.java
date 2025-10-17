package com.dorandoran.user.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final HmacAuthInterceptor hmacAuthInterceptor;

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(hmacAuthInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/actuator/**",                    // 헬스체크 및 모니터링
                        "/",                              // 루트 경로
                        "/swagger-ui/**",                  // API 문서
                        "/v3/api-docs/**",                 // API 문서
                        "/api-docs/**",                    // API 문서
                        "/api/auth/login",                 // 로그인
                        "/api/auth/refresh",               // 토큰 갱신
                        "/api/auth/password/reset/**",     // 비밀번호 재설정
                        "/api/auth/health",                // Auth 서비스 헬스체크
                        "/api/users/health",               // User 서비스 헬스체크
                        "/api/users/email/**",             // 이메일로 사용자 조회
                        "/api/users"                       // 사용자 생성 (POST)
                );
    }

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}


