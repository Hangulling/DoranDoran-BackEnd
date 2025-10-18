package com.dorandoran.auth.config;

import com.dorandoran.shared.security.HmacVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.lang.NonNull;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class HmacAuthInterceptor implements HandlerInterceptor {

    @Value("${gateway.jwt.hmac-secret:}")
    private String hmacSecret;

    @Value("${gateway.jwt.skew-ms:60000}")
    private long skewMs;

    @Override
    public boolean preHandle(@NonNull jakarta.servlet.http.HttpServletRequest request, @NonNull jakarta.servlet.http.HttpServletResponse response, @NonNull Object handler) throws Exception {
        String path = request.getRequestURI();
        
        // Gateway와 동일한 제외 경로 적용
        if (path.startsWith("/actuator") || 
            path.equals("/") || 
            path.startsWith("/swagger-ui") || 
            path.startsWith("/v3/api-docs") || 
            path.startsWith("/api-docs") || 
            path.startsWith("/api/auth/login") || 
            path.startsWith("/api/auth/refresh") || 
            path.startsWith("/api/auth/password/reset") || 
            path.startsWith("/api/auth/health") ||
            path.startsWith("/api/auth/validate") ||  // Gateway가 validate API 호출할 때 무한 루프 방지
            path.startsWith("/api/users/health") ||
            path.startsWith("/api/users/email/")) {
            return true;
        }

        String userId = request.getHeader("X-User-Id");
        String ts = request.getHeader("X-Auth-Ts");
        String sign = request.getHeader("X-Auth-Sign");

        if (userId == null || ts == null || sign == null) {
            response.setStatus(401);
            return false;
        }
        
        if (hmacSecret == null || hmacSecret.isEmpty()) {
            log.error("HMAC secret이 설정되지 않았습니다. gateway.jwt.hmac-secret 설정을 확인하세요.");
            response.setStatus(500);
            return false;
        }

        long now = System.currentTimeMillis();
        long t;
        try { t = Long.parseLong(ts); } catch (NumberFormatException e) { response.setStatus(401); return false; }
        if (Math.abs(now - t) > skewMs) { response.setStatus(401); return false; }

        String message = userId + "|" + ts;
        String expected = HmacVerifier.hmacSha256Hex(hmacSecret, message);
        if (!expected.equalsIgnoreCase(sign)) { response.setStatus(401); return false; }

        return true;
    }
}


