package com.dorandoran.store.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 설정
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

  // HmacAuthInterceptor는 필요 시 주입
  private final HmacAuthInterceptor hmacAuthInterceptor;

  @Override
  public void addInterceptors(@NonNull InterceptorRegistry registry) {
    // HMAC 인증이 필요한 경우 활성화
     registry.addInterceptor(hmacAuthInterceptor)
             .addPathPatterns("/api/**")  // 모든 API 경로
             .excludePathPatterns(
                     "/actuator/**",      // Actuator 제외
                     "/swagger-ui/**",    // Swagger UI 제외
                     "/api-docs/**",       // API Docs 제외
                      "/v3/api-docs/**",
                      "/api/store/health"
             );
  }
}