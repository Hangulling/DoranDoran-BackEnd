package com.dorandoran.batch.config;

import com.dorandoran.shared.security.HmacVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class HmacAuthInterceptor implements HandlerInterceptor {

    @Value("${gateway.jwt.hmac-secret:}")
    private String hmacSecret;

    @Value("${gateway.jwt.skew-ms:60000}")
    private long skewMs;

    @Override
    public boolean preHandle(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        if (path.startsWith("/actuator") || path.equals("/")) {
            return true;
        }

        String userId = request.getHeader("X-User-Id");
        String ts = request.getHeader("X-Auth-Ts");
        String sign = request.getHeader("X-Auth-Sign");

        if (userId == null || ts == null || sign == null || hmacSecret == null || hmacSecret.isEmpty()) {
            response.setStatus(401);
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


