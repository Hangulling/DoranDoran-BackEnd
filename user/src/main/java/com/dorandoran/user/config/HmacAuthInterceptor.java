package com.dorandoran.user.config;

import com.dorandoran.shared.security.HmacVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

@Component
@Slf4j
public class HmacAuthInterceptor implements HandlerInterceptor {

    @Value("${gateway.jwt.hmac-secret:}")
    private String hmacSecret;

    @Value("${gateway.jwt.skew-ms:60000}")
    private long skewMs;

    @Override
    public boolean preHandle(@NonNull jakarta.servlet.http.HttpServletRequest request, @NonNull jakarta.servlet.http.HttpServletResponse response, @NonNull Object handler) throws Exception {
        // 공개 엔드포인트는 통과
        String path = request.getRequestURI();
        if (isExcludedPath(path)) {
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
        try { 
            t = Long.parseLong(ts); 
        } catch (NumberFormatException e) { 
            response.setStatus(401); 
            return false; 
        }
        
        if (Math.abs(now - t) > skewMs) { 
            response.setStatus(401); 
            return false; 
        }

        String message = userId + "|" + ts;
        String expected = HmacVerifier.hmacSha256Hex(hmacSecret, message);
        if (!expected.equalsIgnoreCase(sign)) { 
            response.setStatus(401); 
            return false; 
        }

        return true;
    }
    
    /**
     * 인증 제외 경로 확인
     * - Swagger/Actuator: 개발 및 모니터링 도구
     * - 공개 API: 회원가입, 헬스체크, 이메일 관련 API
     */
    private boolean isExcludedPath(String path) {
        return path.startsWith("/actuator") || 
               path.equals("/") || 
               path.startsWith("/swagger-ui") || 
               path.startsWith("/v3/api-docs") || 
               path.startsWith("/api-docs") || 
               path.startsWith("/api/users/register") || 
               path.startsWith("/api/users/health") ||
               path.startsWith("/api/users/email/") ||
               path.startsWith("/api/users/auth/email/") ||
               path.startsWith("/api/users/check-email/");
    }
}
