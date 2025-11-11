package com.dorandoran.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * IP 블랙리스트 필터
 * 차단된 IP 주소의 요청을 즉시 거부합니다.
 * 
 * 실행 순서: 가장 먼저 실행 (Order: -100)
 * 다른 필터보다 우선하여 차단된 IP의 요청을 처리하지 않습니다.
 */
@Slf4j
@Component
public class IpBlacklistFilter implements GlobalFilter, Ordered {

    private static final String X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String X_REAL_IP = "X-Real-IP";
    private static final String FORWARDED = "Forwarded";
    
    // 블랙리스트 체크 제외 경로
    private static final List<String> EXCLUDED_PATHS = List.of(
        "/actuator",           // Actuator 엔드포인트 (모니터링용)
        "/api/admin/blacklist" // 관리자 블랙리스트 관리 API (자기 자신이 차단되어도 관리 가능)
    );

    private final Set<String> blacklistedIps;

    /**
     * application.yml에서 블랙리스트 IP 목록을 읽어옵니다.
     * 
     * @param blacklistIps 설정 파일의 블랙리스트 IP 목록 (쉼표로 구분)
     */
    public IpBlacklistFilter(
            @Value("${gateway.security.blacklist.ips:}") String blacklistIps) {
        this.blacklistedIps = new HashSet<>();
        
        if (StringUtils.hasText(blacklistIps)) {
            Arrays.stream(blacklistIps.split(","))
                    .map(String::trim)
                    .filter(ip -> !ip.isEmpty())
                    .forEach(ip -> {
                        this.blacklistedIps.add(ip);
                        log.info("IP 블랙리스트에 추가됨: {}", ip);
                    });
        }
        
        log.info("IP 블랙리스트 필터 초기화 완료. 총 {}개의 IP가 차단됩니다.", this.blacklistedIps.size());
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Actuator 엔드포인트는 제외
        if (isExcludedPath(path)) {
            return chain.filter(exchange);
        }

        // 클라이언트 IP 주소 추출
        String clientIp = extractClientIp(request);
        
        if (clientIp == null) {
            log.warn("클라이언트 IP를 추출할 수 없습니다. 요청 경로: {}", path);
            // IP를 추출할 수 없으면 통과 (보안보다 가용성 우선)
            return chain.filter(exchange);
        }

        // 블랙리스트 체크
        if (isBlacklisted(clientIp)) {
            log.warn("차단된 IP에서 요청이 들어왔습니다. IP: {}, 경로: {}", clientIp, path);
            return handleBlockedRequest(exchange, clientIp);
        }

        // 블랙리스트에 없으면 정상 처리
        return chain.filter(exchange);
    }

    /**
     * 클라이언트 IP 주소를 추출합니다.
     * 
     * 우선순위:
     * 1. X-Forwarded-For 헤더 (프록시/로드밸런서 사용 시)
     * 2. X-Real-IP 헤더
     * 3. Forwarded 헤더
     * 4. RemoteAddress
     * 
     * @param request HTTP 요청
     * @return 클라이언트 IP 주소
     */
    private String extractClientIp(ServerHttpRequest request) {
        // X-Forwarded-For 헤더 확인 (가장 일반적)
        String xForwardedFor = request.getHeaders().getFirst(X_FORWARDED_FOR);
        if (xForwardedFor != null && StringUtils.hasText(xForwardedFor)) {
            // X-Forwarded-For는 여러 IP를 포함할 수 있음 (프록시 체인)
            // 첫 번째 IP가 원본 클라이언트 IP
            String[] ips = xForwardedFor.split(",");
            if (ips.length > 0) {
                String ip = ips[0].trim();
                // 포트 번호 제거 (예: "192.168.1.1:8080" -> "192.168.1.1")
                if (ip.contains(":")) {
                    ip = ip.substring(0, ip.indexOf(":"));
                }
                return ip;
            }
        }

        // X-Real-IP 헤더 확인
        String xRealIp = request.getHeaders().getFirst(X_REAL_IP);
        if (xRealIp != null && StringUtils.hasText(xRealIp)) {
            String ip = xRealIp.trim();
            if (ip.contains(":")) {
                ip = ip.substring(0, ip.indexOf(":"));
            }
            return ip;
        }

        // Forwarded 헤더 확인 (RFC 7239)
        String forwarded = request.getHeaders().getFirst(FORWARDED);
        if (forwarded != null && StringUtils.hasText(forwarded)) {
            // Forwarded: for=192.168.1.1;proto=http
            String[] parts = forwarded.split(";");
            for (String part : parts) {
                if (part != null && part.trim().startsWith("for=")) {
                    String ip = part.substring(4).trim();
                    // 따옴표 제거
                    ip = ip.replace("\"", "");
                    if (ip.contains(":")) {
                        ip = ip.substring(0, ip.indexOf(":"));
                    }
                    return ip;
                }
            }
        }

        // RemoteAddress 사용 (마지막 수단)
        var remoteAddress = request.getRemoteAddress();
        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            return remoteAddress.getAddress().getHostAddress();
        }

        return null;
    }

    /**
     * 경로가 제외 목록에 있는지 확인합니다.
     */
    private boolean isExcludedPath(String path) {
        return EXCLUDED_PATHS.stream().anyMatch(path::startsWith);
    }

    /**
     * IP가 블랙리스트에 있는지 확인합니다.
     */
    private boolean isBlacklisted(String ip) {
        return blacklistedIps.contains(ip);
    }

    /**
     * 차단된 요청에 대한 응답을 생성합니다.
     */
    private Mono<Void> handleBlockedRequest(ServerWebExchange exchange, String clientIp) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String message = String.format(
            "{\"error\":\"Forbidden\",\"message\":\"Access denied. Your IP address (%s) has been blocked.\",\"code\":403}",
            clientIp
        );

        DataBuffer buffer = response.bufferFactory().wrap(message.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // 가장 높은 우선순위로 설정하여 다른 필터보다 먼저 실행
        return -100;
    }

    /**
     * 런타임에 IP를 블랙리스트에 추가합니다.
     * (향후 관리 API에서 사용)
     */
    public void addToBlacklist(String ip) {
        if (StringUtils.hasText(ip)) {
            blacklistedIps.add(ip.trim());
            log.info("런타임에 IP 블랙리스트에 추가됨: {}", ip);
        }
    }

    /**
     * 런타임에 IP를 블랙리스트에서 제거합니다.
     * (향후 관리 API에서 사용)
     */
    public boolean removeFromBlacklist(String ip) {
        if (StringUtils.hasText(ip)) {
            boolean removed = blacklistedIps.remove(ip.trim());
            if (removed) {
                log.info("런타임에 IP 블랙리스트에서 제거됨: {}", ip);
            }
            return removed;
        }
        return false;
    }

    /**
     * 현재 블랙리스트에 있는 모든 IP를 반환합니다.
     * (향후 관리 API에서 사용)
     */
    public Set<String> getBlacklistedIps() {
        return new HashSet<>(blacklistedIps);
    }

    /**
     * IP가 블랙리스트에 있는지 확인합니다.
     * (향후 관리 API에서 사용)
     */
    public boolean isIpBlacklisted(String ip) {
        return isBlacklisted(ip);
    }
}

