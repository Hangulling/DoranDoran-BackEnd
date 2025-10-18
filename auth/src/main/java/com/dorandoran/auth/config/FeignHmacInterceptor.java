package com.dorandoran.auth.config;

import com.dorandoran.shared.security.HmacVerifier;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Feign 클라이언트용 HMAC 인증 인터셉터
 */
@Component
@Slf4j
public class FeignHmacInterceptor implements RequestInterceptor {

    @Value("${gateway.jwt.hmac-secret:}")
    private String hmacSecret;

    @Value("${gateway.jwt.skew-ms:60000}")
    private long skewMs;

    @Override
    public void apply(RequestTemplate template) {
        // Auth 서비스에서 User 서비스로 호출할 때만 HMAC 헤더 추가
        if (template.url().contains("/api/users/")) {
            String userId = "auth-service"; // Auth 서비스 식별자
            long timestamp = System.currentTimeMillis();
            String message = userId + "|" + timestamp;
            String signature = HmacVerifier.hmacSha256Hex(hmacSecret, message);

            template.header("X-User-Id", userId);
            template.header("X-Auth-Ts", String.valueOf(timestamp));
            template.header("X-Auth-Sign", signature);

            log.debug("HMAC 헤더 추가: userId={}, timestamp={}", userId, timestamp);
        }
    }
}
