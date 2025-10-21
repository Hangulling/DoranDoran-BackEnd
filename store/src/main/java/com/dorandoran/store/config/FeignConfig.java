package com.dorandoran.store.config;

import com.dorandoran.shared.security.HmacVerifier;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 보관함 조회 시 통신 401 - chatService 연결 401
 */
@Configuration
@Slf4j
public class FeignConfig {

  @Value("${gateway.jwt.hmac-secret}")
  private String hmacSecret;

  @Bean
  public RequestInterceptor hmacRequestInterceptor() {
    return new RequestInterceptor() {
      @Override
      public void apply(RequestTemplate template) {
        // URL에서 userId 추출 (쿼리 파라미터)
        String userId = extractUserIdFromUrl(template.url());

        if (userId == null) {
          userId = "system"; // 기본값
        }

        long timestamp = System.currentTimeMillis();
        String message = userId + "|" + timestamp;
        String signature = HmacVerifier.hmacSha256Hex(hmacSecret, message);

        template.header("X-User-Id", userId);
        template.header("X-Auth-Ts", String.valueOf(timestamp));
        template.header("X-Auth-Sign", signature);

        log.debug("Feign HMAC 헤더 추가: userId={}, ts={}", userId, timestamp);
      }

      private String extractUserIdFromUrl(String url) {
        // URL에서 userId 쿼리 파라미터 추출
        if (url.contains("userId=")) {
          int startIdx = url.indexOf("userId=") + 7;
          int endIdx = url.indexOf("&", startIdx);
          if (endIdx == -1) {
            endIdx = url.length();
          }
          return url.substring(startIdx, endIdx);
        }
        return null;
      }
    };
  }
}