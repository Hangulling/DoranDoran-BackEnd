package com.dorandoran.gateway.filter;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import org.springframework.lang.NonNull;

/**
 * JWT 인증 필터 (완전 구현)
 * Auth 서비스와의 통신을 통한 토큰 검증 및 HMAC 서명 헤더 주입
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
    private final WebClient.Builder webClientBuilder;

    @Value("${gateway.auth.base-url:http://localhost:8081}")
    private String authBaseUrl;

    @Value("${gateway.auth.validate-path:/api/auth/validate}")
    private String validatePath;

    @Value("${gateway.jwt.hmac-secret:}")
    private String hmacSecret;

    @Value("${gateway.jwt.skew-ms:60000}")
    private long skewMs;

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        // @NonNull 어노테이션으로 인해 null 체크 불필요
        
        // 모든 요청에 CORS 헤더 추가
        ServerHttpResponse response = exchange.getResponse();
        HttpHeaders headers = response.getHeaders();
        
        // Origin 헤더 확인
        String origin = exchange.getRequest().getHeaders().getFirst("Origin");
        if (origin != null && isAllowedOrigin(origin)) {
            headers.add("Access-Control-Allow-Origin", origin);
            headers.add("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,PATCH,OPTIONS");
            headers.add("Access-Control-Allow-Headers", "*");
            headers.add("Access-Control-Allow-Credentials", "true");
            headers.add("Access-Control-Max-Age", "3600");
            log.debug("CORS 헤더 추가: origin={}, method={}", origin, exchange.getRequest().getMethod());
        }
        
        // CORS Preflight(OPTIONS) 요청은 인증을 우회
        if (exchange.getRequest().getMethod() == HttpMethod.OPTIONS) {
            log.debug("OPTIONS 요청 감지 - CORS Preflight 처리: path={}", exchange.getRequest().getURI().getPath());
            response.setStatusCode(HttpStatus.OK);
            return response.setComplete();
        }

        String path = exchange.getRequest().getURI().getPath();

        // 인증 제외 경로: 게이트웨이 헬스/액추에이터, auth 서비스의 로그인/리프레시/헬스/비번재설정
        if (isExcludedPath(path)) {
            log.debug("인증 제외 경로: path={}", path);
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("인증 헤더가 없거나 Bearer 토큰이 아닙니다: path={}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
        
        String token = authHeader.substring(7);

        // Auth 서비스의 validate API 호출로 토큰 검증
        return validateTokenWithAuthService(token, exchange, chain);
    }

    /**
     * 인증 제외 경로 확인
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
               path.startsWith("/api/auth/validate") ||  // Gateway가 validate API 호출할 때 무한 루프 방지
               path.startsWith("/api/users/health") ||
               path.startsWith("/api/users/email/") ||
               path.startsWith("/api/users/check-email/") ||  // 이메일 중복확인 API
               path.equals("/api/users");  // 사용자 생성 API (모든 메서드 허용)
    }

    /**
     * Auth 서비스를 통한 토큰 검증
     */
    private Mono<Void> validateTokenWithAuthService(String token, ServerWebExchange exchange, WebFilterChain chain) {
        WebClient client = webClientBuilder.build();
        
        return client.get()
                .uri(authBaseUrl + validatePath)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .toBodilessEntity()
                .flatMap(response -> {
                    // 토큰 검증 성공 시 HMAC 헤더 주입
                    return addHmacHeadersAndContinue(token, exchange, chain);
                })
                .onErrorResume(err -> {
                    log.debug("JWT 검증 실패: {}", err.getMessage());
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                });
    }

    /**
     * HMAC 헤더 주입 및 요청 계속
     */
    private Mono<Void> addHmacHeadersAndContinue(String token, ServerWebExchange exchange, WebFilterChain chain) {
        // HMAC secret 검증
        if (hmacSecret == null || hmacSecret.isEmpty()) {
            log.error("HMAC secret이 설정되지 않았습니다. gateway.jwt.hmac-secret 설정을 확인하세요.");
            exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return exchange.getResponse().setComplete();
        }
        
        try {
            // JWT 페이로드에서 클레임 추출
            String[] parts = token.split("\\.");
            if (parts.length == 3) {
                String payloadJson = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
                String compact = payloadJson.replaceAll("\\s+", "");
                
                // 클레임 추출
                String userId = extractJsonValue(compact, "sub");
                String email = extractJsonValue(compact, "email");
                String name = extractJsonValue(compact, "name");
                
                // HMAC 서명 생성
                long timestamp = System.currentTimeMillis();
                String message = (!userId.isEmpty() ? userId : "") + "|" + timestamp;
                String hmacSignature = generateHmacSignature(hmacSecret, message);

                // 헤더 주입하여 요청 계속
                var mutated = exchange.mutate().request(
                        builder -> builder.headers(http -> {
                            if (!userId.isEmpty()) http.add("X-User-Id", userId);
                            if (!email.isEmpty()) http.add("X-User-Email", email);
                            if (!name.isEmpty()) http.add("X-User-Name", name);
                            http.add("X-Auth-Ts", Long.toString(timestamp));
                            http.add("X-Auth-Sign", hmacSignature);
                        })
                ).build();
                
                // CORS 헤더 추가
                return addCorsHeadersAndFilter(mutated, chain);
            }
        } catch (Exception e) {
            log.warn("JWT 페이로드 파싱 실패", e);
        }
        
        // 파싱 실패해도 검증은 통과했으니 헤더 주입 없이 통과
        return chain.filter(exchange);
    }

    /**
     * JSON 값 추출 (간단한 파싱)
     */
    private String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\":\"";
        int idx = json.indexOf(pattern);
        if (idx < 0) return "";
        int start = idx + pattern.length();
        int end = json.indexOf('"', start);
        if (end < 0) return "";
        return json.substring(start, end);
    }

    /**
     * HMAC 서명 생성
     */
    private String generateHmacSignature(String secret, String message) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKeySpec = new javax.crypto.spec.SecretKeySpec(secret.getBytes(), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(message.getBytes());
            return bytesToHex(hmacBytes);
        } catch (Exception e) {
            log.error("HMAC 서명 생성 실패", e);
            return "invalid-signature";
        }
    }

    /**
     * 바이트 배열을 헥스 문자열로 변환
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    /**
     * 허용된 Origin 확인
     */
    private boolean isAllowedOrigin(String origin) {
        return origin.startsWith("http://localhost:3000") ||
               origin.startsWith("http://localhost:3001") ||
               origin.startsWith("http://3.21.177.186") ||
               origin.contains(".dorandoran.com");
    }

    /**
     * CORS 헤더 추가 및 필터 체인 실행
     */
    private Mono<Void> addCorsHeadersAndFilter(ServerWebExchange exchange, WebFilterChain chain) {
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            String origin = exchange.getRequest().getHeaders().getFirst("Origin");
            if (origin != null && isAllowedOrigin(origin)) {
                ServerHttpResponse response = exchange.getResponse();
                HttpHeaders headers = response.getHeaders();
                
                // CORS 헤더가 이미 설정되지 않은 경우에만 추가
                if (!headers.containsKey("Access-Control-Allow-Origin")) {
                    headers.add("Access-Control-Allow-Origin", origin);
                    headers.add("Access-Control-Allow-Credentials", "true");
                    log.debug("CORS 헤더 추가: origin={}, method={}", origin, exchange.getRequest().getMethod());
                }
            }
        }));
    }
}