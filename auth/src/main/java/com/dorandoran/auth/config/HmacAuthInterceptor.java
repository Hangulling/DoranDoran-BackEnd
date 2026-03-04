package com.dorandoran.auth.config;

import com.dorandoran.shared.security.HmacVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.lang.NonNull;
import lombok.extern.slf4j.Slf4j;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * HMAC 인증 인터셉터 (Auth 서비스)
 * Gateway에서 주입한 HMAC 헤더를 검증하여 서비스 간 통신 보안을 보장
 */
@Component
@Slf4j
public class HmacAuthInterceptor implements HandlerInterceptor {

    @Value("${gateway.jwt.hmac-secret:}")
    private String hmacSecret;

    @Value("${gateway.jwt.skew-ms:60000}")
    private long skewMs;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        // 공개 엔드포인트는 통과
        String path = request.getRequestURI();
        boolean excluded = isExcludedPath(path);
        log.debug("HmacAuthInterceptor - path={}, excluded={}", path, excluded);
        if (excluded) {
            return true;
        }

        String userId = request.getHeader("X-User-Id");
        String ts = request.getHeader("X-Auth-Ts");
        String sign = request.getHeader("X-Auth-Sign");

        if (userId == null || ts == null || sign == null) {
            log.warn("HMAC 헤더 누락: path={}, excluded={}", path, isExcludedPath(path));
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
            log.debug("잘못된 타임스탬프 형식: {}", ts);
            response.setStatus(401); 
            return false; 
        }
        
        if (Math.abs(now - t) > skewMs) { 
            log.debug("타임스탬프 만료: now={}, ts={}, diff={}", now, t, Math.abs(now - t));
            response.setStatus(401); 
            return false; 
        }

        String message = userId + "|" + ts;
        String expected = HmacVerifier.hmacSha256Hex(hmacSecret, message);
        if (!expected.equalsIgnoreCase(sign)) { 
            log.debug("HMAC 서명 불일치: expected={}, actual={}", expected, sign);
            response.setStatus(401); 
            return false; 
        }

        log.debug("HMAC 인증 성공: userId={}, path={}", userId, path);
        return true;
    }

    /**
     * 인증 제외 경로 확인
     * - Swagger/Actuator: 개발 및 모니터링 도구
     * - 공개 API: 로그인, 토큰 갱신, 비밀번호 재설정, 헬스체크, 토큰 검증
     * - 이메일 인증: 회원가입 전 이메일 인증 관련 엔드포인트
     */
    private boolean isExcludedPath(String path) {
        return path.startsWith("/actuator") || 
               path.equals("/") || 
               path.startsWith("/swagger-ui") || 
               path.startsWith("/v3/api-docs") || 
               path.startsWith("/api-docs") || 
               path.startsWith("/api/auth/login") || 
               path.startsWith("/api/auth/refresh") || 
               path.startsWith("/api/auth/password/reset") || 
               path.startsWith("/api/auth/health") ||
               path.startsWith("/api/auth/validate") ||
               path.startsWith("/api/auth/email/request-verification") ||
               path.startsWith("/api/auth/email/verify") ||
               path.startsWith("/api/auth/email/check") ||
               path.startsWith("/api/auth/oauth/login") ||  // OAuth 로그인 엔드포인트 제외
               path.startsWith("/api/auth/oauth/callback") ||  // OAuth 콜백 (IdP 리다이렉트)
               path.startsWith("/api/auth/oauth/authorize-url") ||  // OAuth Authorization URL 발급
               path.startsWith("/error");  // Spring 에러 핸들링 경로
    }
}