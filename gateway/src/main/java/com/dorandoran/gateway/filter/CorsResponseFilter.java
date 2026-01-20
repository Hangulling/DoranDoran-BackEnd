package com.dorandoran.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * SSE 스트림 응답에 CORS 헤더를 추가하는 필터
 * CorsWebFilter가 스트리밍 응답에 제대로 작동하지 않는 경우를 대비
 */
@Component
public class CorsResponseFilter implements GlobalFilter, Ordered {

    private static final List<String> ALLOWED_ORIGINS = List.of(
            "http://localhost:3000",
            "http://localhost:3001",
            "http://127.0.0.1:3000",
            "http://127.0.0.1:3001",
            "https://localhost",
            "capacitor://localhost",
            "https://doran-chat.com",
            "https://www.doran-chat.com",
            "https://doran-chat.vercel.app"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Accept 헤더 확인 (SSE 요청인지)
        String accept = exchange.getRequest().getHeaders().getFirst(HttpHeaders.ACCEPT);
        boolean isSSERequest = accept != null && accept.contains(MediaType.TEXT_EVENT_STREAM_VALUE);
        
        // SSE 요청에만 CORS 헤더 추가 (일반 요청은 CorsWebFilter에서 처리)
        if (isSSERequest) {
            ServerHttpResponse response = exchange.getResponse();
            HttpHeaders headers = response.getHeaders();
            
            // 이미 CORS 헤더가 있으면 추가하지 않음 (CorsWebFilter가 이미 설정한 경우)
            if (headers.containsKey(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)) {
                return chain.filter(exchange);
            }
            
            // Origin 헤더 확인
            String origin = exchange.getRequest().getHeaders().getFirst(HttpHeaders.ORIGIN);
            if (origin != null && isAllowedOrigin(origin)) {
                headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
                headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
                headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT, DELETE, OPTIONS, PATCH");
                headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "*");
                headers.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "*");
                
                // SSE를 위한 추가 헤더
                headers.add(HttpHeaders.CACHE_CONTROL, "no-cache");
                headers.add(HttpHeaders.CONNECTION, "keep-alive");
                
                // Google OAuth 팝업과의 postMessage 통신을 위해 COOP 헤더 설정
                headers.add("Cross-Origin-Opener-Policy", "unsafe-none");
            }
        }
        
        return chain.filter(exchange);
    }

    private boolean isAllowedOrigin(String origin) {
        if (origin == null) {
            return false;
        }
        
        // 정확한 매칭
        if (ALLOWED_ORIGINS.contains(origin)) {
            return true;
        }
        
        // 와일드카드 패턴 매칭
        return origin.matches("https://.*\\.doran-chat\\.com") ||
               origin.matches("https://.*\\.vercel\\.app") ||
               origin.matches("https://.*\\.netlify\\.app");
    }

    @Override
    public int getOrder() {
        // 응답 헤더를 설정하기 위해 높은 우선순위 설정
        // NettyRoutingFilter보다 먼저 실행되어야 함
        return -1;
    }
}

