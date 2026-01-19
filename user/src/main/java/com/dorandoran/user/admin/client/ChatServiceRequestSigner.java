package com.dorandoran.user.admin.client;

import com.dorandoran.shared.security.HmacVerifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/**
 * User Service -> Chat Service 호출용 HMAC 헤더 생성기
 */
@Component
@Slf4j
public class ChatServiceRequestSigner {

    @Value("${gateway.jwt.hmac-secret:}")
    private String hmacSecret;

    public HttpHeaders createHeaders(String userId) {
        if (hmacSecret == null || hmacSecret.isEmpty()) {
            throw new IllegalStateException("HMAC secret이 설정되지 않았습니다. gateway.jwt.hmac-secret을 확인하세요.");
        }

        long timestamp = System.currentTimeMillis();
        String message = userId + "|" + timestamp;
        String signature = HmacVerifier.hmacSha256Hex(hmacSecret, message);

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-User-Id", userId);
        headers.add("X-Auth-Ts", String.valueOf(timestamp));
        headers.add("X-Auth-Sign", signature);
        return headers;
    }
}
