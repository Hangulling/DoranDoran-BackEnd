package com.dorandoran.chat.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 설정 (Chat 서비스)
 * CORS 설정 및 HMAC 인증 인터셉터 등록
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final HmacAuthInterceptor hmacAuthInterceptor;

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(hmacAuthInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/actuator/**",      // Actuator 제외
                        "/swagger-ui/**",    // Swagger UI 제외
                        "/v3/api-docs/**",   // API Docs 제외
                        "/api-docs/**",      // API Docs 제외
                        "/api/chat/health"   // 헬스체크 제외
                );
    }

    // Gateway에서 CORS를 처리하므로 Chat 서비스의 CORS 설정은 비활성화
    // @Override
    // public void addCorsMappings(@NonNull CorsRegistry registry) {
    //     registry.addMapping("/api/**")
    //             .allowedOriginPatterns("*")
    //             .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
    //             .allowedHeaders("*")
    //             .allowCredentials(true)
    //             .maxAge(3600);
    // }
}