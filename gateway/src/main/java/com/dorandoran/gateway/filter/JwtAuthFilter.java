package com.dorandoran.gateway.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * JWT 인증 필터 (완전 구현)
 * Auth 서비스와의 통신을 통한 토큰 검증 및 HMAC 서명 헤더 주입
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter implements WebFilter {

    private final WebClient.Builder webClientBuilder;

    @Value("${gateway.auth.base-url:http://localhost:8081}")
    private String authBaseUrl;

    @Value("${gateway.auth.validate-path:/api/auth/validate}")
    private String validatePath;

    @Value("${gateway.jwt.hmac-secret:change-me-hmac-secret}")
    private String hmacSecret;

    @Value("${gateway.jwt.skew-ms:60000}")
    private long skewMs;

    /**
     * true면 GET /api/deeplink/chatroom/** 에 대해 JWT 검증을 건너뜀.
     * Chat 서비스 {@code app.deeplink-public} 도 반드시 같이 켜야 하며, 쿼리 {@code userId} 스푸핑 위험이 있음.
     */
    @Value("${gateway.auth.deeplink-public:false}")
    private boolean deeplinkPublic;

    /**
     * {@link com.dorandoran.gateway.config.SecurityConfig#corsWebFilter()} 및
     * {@link CorsResponseFilter} 와 동일한 출처 허용 목록 (401 등 조기 응답 CORS용).
     */
    private static final List<String> GATEWAY_ALLOWED_ORIGINS = List.of(
            "http://localhost:3000",
            "http://localhost:3001",
            "http://127.0.0.1:3000",
            "http://127.0.0.1:3001",
            "https://localhost",
            "capacitor://localhost",
            "ionic://localhost",
            "https://doran-chat.com",
            "https://www.doran-chat.com",
            "https://doran-chat.vercel.app"
    );

    @Override
    public @NonNull Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod().name();
        
        log.debug("JWT 인증 체크: path={}, method={}", path, method);
        
        // CORS preflight 요청은 바로 통과
        if ("OPTIONS".equals(method)) {
            log.debug("OPTIONS 요청 통과: path={}", path);
            return chain.filter(exchange);
        }
        
        // 인증 제외 경로는 바로 통과
        if (isExcludedPath(exchange)) {
            log.debug("인증 제외 경로로 통과: path={}", path);
            return chain.filter(exchange);
        }

        String token = extractToken(exchange);
        if (token == null || token.isBlank()) {
            log.debug("JWT 토큰을 찾을 수 없음: path={}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            addCorsHeadersIfNeeded(exchange);
            return exchange.getResponse().setComplete();
        }

        // Auth 서비스를 통한 토큰 검증
        return validateTokenWithAuthService(token, exchange, chain);
    }

    private String extractToken(ServerWebExchange exchange) {
        // 1) 기본: Authorization 헤더(Bearer)
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // 2) ws/chat 전용: 쿼리 token 파라미터 허용 (iOS WebSocket handshake 대응)
        String path = exchange.getRequest().getURI().getPath();
        if (path != null && path.startsWith("/ws/chat/")) {
            String queryToken = exchange.getRequest().getQueryParams().getFirst("token");
            if (queryToken != null && !queryToken.isBlank()) {
                if (queryToken.startsWith("Bearer ")) {
                    return queryToken.substring(7);
                }
                return queryToken;
            }
            log.debug("ws/chat 쿼리 token 누락: path={}", path);
        } else {
            log.debug("Authorization 헤더가 없거나 형식이 잘못됨: path={}", path);
        }

        return null;
    }

    /**
     * 인증 제외 경로 확인
     * - Actuator: 모니터링 및 헬스체크
     * - 공개 API: 로그인, 토큰 갱신, 비밀번호 재설정, 헬스체크
     * - 회원가입: 사용자 등록 관련 API
     * - 이메일 인증: 이메일 인증 관련 API (인증 없이 접근 가능)
     */
    private boolean isExcludedPath(ServerWebExchange exchange) {
        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod() != null
            ? exchange.getRequest().getMethod().name()
            : "";
        log.debug("인증 제외 경로 체크 시작: path={}, method={}", path, method);

        // GET /api/support 만 공개: URL 직접 열람 시 User 서비스가 405 + 안내 (POST는 인증 필요)
        if (path.startsWith("/api/support") && "GET".equals(method)) {
            log.debug("Support GET은 인증 제외 (문의는 POST만)");
            return true;
        }

        if (deeplinkPublic && path.startsWith("/api/deeplink/chatroom")) {
            log.debug("딥링크 채팅방 생성은 gateway.auth.deeplink-public 로 JWT 제외");
            return true;
        }

        boolean excluded = path.startsWith("/actuator") || 
               path.equals("/") ||
               path.startsWith("/api/auth/login") ||
               path.startsWith("/api/auth/refresh") ||
               path.startsWith("/api/auth/password/reset") ||
               path.startsWith("/api/auth/health") ||
               path.startsWith("/api/auth/email/request-verification") ||
               path.startsWith("/api/auth/email/verify") ||
               path.startsWith("/api/auth/email/check") ||
               path.startsWith("/api/auth/oauth/login") ||  // OAuth 로그인 엔드포인트 제외
               path.equals("/api/users") ||  // POST /api/users (회원가입) 제외
               path.startsWith("/api/users/register") ||
               path.startsWith("/api/users/health") ||
               path.startsWith("/api/users/email/") ||
               path.startsWith("/api/users/auth/email/") ||
               path.startsWith("/api/users/check-email/") ||
               path.startsWith("/api/home") ||
               path.startsWith("/api/users/find-email") ||  // 이메일 찾기 제외
               path.startsWith("/api/batch/");  // Batch 서비스 인증 제외
        
        log.debug("인증 제외 경로 체크 결과: path={}, excluded={}", path, excluded);
        
        // 각 조건별 상세 로그
        if (path.startsWith("/api/users/find-email")) {
            log.debug("이메일 찾기 경로 매칭 확인: path={}, startsWith('/api/users/find-email')={}", 
                    path, path.startsWith("/api/users/find-email"));
        }
        
        return excluded;
    }

    /**
     * Auth 서비스를 통한 토큰 검증
     */
    private Mono<Void> validateTokenWithAuthService(String token, ServerWebExchange exchange, WebFilterChain chain) {
        WebClient client = webClientBuilder.build();
        final String validateUrl = authBaseUrl + validatePath;
        final String incomingPath = exchange.getRequest().getURI().getPath();
        log.debug("Auth 검증 요청: validateUrl={}, incomingPath={}", validateUrl, incomingPath);

        return client.get()
                .uri(validateUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .toBodilessEntity()
                .flatMap(response -> {
                    // 토큰 검증 성공 시 HMAC 헤더 주입
                    return addHmacHeadersAndContinue(token, exchange, chain);
                })
                .onErrorResume(err -> {
                    if (err instanceof WebClientResponseException wex) {
                        log.warn(
                            "JWT 검증 실패: incomingPath={}, validateUrl={}, status={}, body={}",
                            incomingPath,
                            validateUrl,
                            wex.getStatusCode(),
                            wex.getResponseBodyAsString()
                        );
                    } else {
                        log.warn(
                            "JWT 검증 실패: incomingPath={}, validateUrl={}, error={}",
                            incomingPath,
                            validateUrl,
                            err.toString()
                        );
                    }
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    addCorsHeadersIfNeeded(exchange);
                    return exchange.getResponse().setComplete();
                });
    }

    /**
     * HMAC 헤더 주입 및 요청 계속
     */
    private Mono<Void> addHmacHeadersAndContinue(String token, ServerWebExchange exchange, WebFilterChain chain) {
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
                            // add()는 기존 클라이언트 헤더와 병합되어 X-User-Id가 "잘못된값,uuid" 형태가 되어 User 서비스에서 UUID 파싱 실패할 수 있음
                            if (!userId.isEmpty()) http.set("X-User-Id", userId);
                            if (!email.isEmpty()) http.set("X-User-Email", email);
                            if (!name.isEmpty()) http.set("X-User-Name", name);
                            http.set("X-Auth-Ts", Long.toString(timestamp));
                            http.set("X-Auth-Sign", hmacSignature);
                        })
                ).build();
                
                return chain.filter(mutated);
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
     * CORS 헤더가 없을 때만 추가 (CorsWebFilter와의 중복 방지)
     * setComplete() 호출 전에 사용하여 인증 실패 응답에도 CORS 헤더 포함
     */
    private void addCorsHeadersIfNeeded(ServerWebExchange exchange) {
        HttpHeaders headers = exchange.getResponse().getHeaders();
        
        // 이미 CORS 헤더가 있으면 추가하지 않음 (CorsWebFilter가 이미 처리한 경우)
        if (headers.containsKey(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)) {
            return;
        }
        
        // Origin 헤더 확인
        String origin = exchange.getRequest().getHeaders().getFirst(HttpHeaders.ORIGIN);
        if (origin == null || !isAllowedOrigin(origin)) {
            return;
        }
        
        // CORS 헤더 추가
        headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
        headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "*");
        headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "*");
        headers.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "*");
    }

    /**
     * 허용된 Origin인지 확인 (SecurityConfig CorsWebFilter / CorsResponseFilter 와 동일 규칙).
     */
    private boolean isAllowedOrigin(String origin) {
        if (origin == null) {
            return false;
        }
        if (GATEWAY_ALLOWED_ORIGINS.contains(origin)) {
            return true;
        }
        return origin.matches("https://.*\\.doran-chat\\.com") ||
               origin.matches("https://.*\\.vercel\\.app") ||
               origin.matches("https://.*\\.netlify\\.app");
    }
}