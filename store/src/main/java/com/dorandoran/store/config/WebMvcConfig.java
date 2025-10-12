package com.dorandoran.store.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 설정
 *
 * <p>Store Service의 웹 계층 설정 (CORS, Interceptor 등)</p>
 *
 * <h3>주요 설정:</h3>
 * <ul>
 *   <li>CORS: 프론트엔드와의 통신 허용</li>
 *   <li>Interceptor: HMAC 인증 (선택사항)</li>
 * </ul>
 *
 * @author DoranDoran Team
 * @version 1.0
 * @since 2025-10-11
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

  // HmacAuthInterceptor는 필요 시 주입
  // private final HmacAuthInterceptor hmacAuthInterceptor;

  /**
   * CORS 설정
   *
   * <p>프론트엔드(React 등)와의 크로스 도메인 통신을 허용</p>
   *
   * <h3>허용 항목:</h3>
   * <ul>
   *   <li>Origin: localhost:3000 (개발), 운영 도메인</li>
   *   <li>Method: GET, POST, PUT, DELETE, OPTIONS</li>
   *   <li>Header: 모든 헤더 허용</li>
   *   <li>Credentials: 쿠키/인증 정보 허용</li>
   * </ul>
   *
   * @param registry CORS 레지스트리
   */
  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/api/**")  // /api/** 경로에 대해 CORS 허용
        .allowedOrigins(
            "http://localhost:3000",      // React 개발 서버
            "http://localhost:8080",      // API Gateway
            "https://dorandoran.com"      // 운영 도메인
        )
        .allowedMethods(
            "GET",     // 조회
            "POST",    // 생성
            "PUT",     // 수정
            "DELETE",  // 삭제
            "OPTIONS"  // Preflight
        )
        .allowedHeaders("*")        // 모든 헤더 허용
        .allowCredentials(true)     // 쿠키/인증 정보 허용
        .maxAge(3600);              // Preflight 캐시 시간 (1시간)
  }
}