package com.dorandoran.gateway.controller;

import com.dorandoran.gateway.filter.IpBlacklistFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * IP 블랙리스트 관리 API
 * 
 * 관리자가 런타임에 IP 블랙리스트를 조회, 추가, 제거할 수 있는 API를 제공합니다.
 * 
 * 엔드포인트:
 * - GET  /api/admin/blacklist - 블랙리스트 조회
 * - POST /api/admin/blacklist - IP 추가
 * - DELETE /api/admin/blacklist/{ip} - IP 제거
 * - GET  /api/admin/blacklist/{ip} - 특정 IP 차단 여부 확인
 * 
 * 인증: JWT 토큰 필요 (Authorization: Bearer <token>)
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/blacklist")
@RequiredArgsConstructor
public class BlacklistController {

    private final IpBlacklistFilter ipBlacklistFilter;

    /**
     * 블랙리스트에 있는 모든 IP 조회
     */
    @GetMapping
    public Mono<ResponseEntity<Map<String, Object>>> getBlacklist() {
        Set<String> blacklistedIps = ipBlacklistFilter.getBlacklistedIps();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("count", blacklistedIps.size());
        response.put("ips", blacklistedIps);
        
        log.info("블랙리스트 조회 요청. 총 {}개의 IP가 차단 중입니다.", blacklistedIps.size());
        
        return Mono.just(ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response));
    }

    /**
     * 특정 IP의 차단 여부 확인
     */
    @GetMapping("/{ip}")
    public Mono<ResponseEntity<Map<String, Object>>> checkIp(@PathVariable String ip) {
        boolean isBlacklisted = ipBlacklistFilter.isIpBlacklisted(ip);
        
        Map<String, Object> response = new HashMap<>();
        response.put("ip", ip);
        response.put("blacklisted", isBlacklisted);
        
        log.info("IP 차단 여부 확인 요청. IP: {}, 차단 여부: {}", ip, isBlacklisted);
        
        return Mono.just(ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response));
    }

    /**
     * IP를 블랙리스트에 추가
     */
    @PostMapping
    public Mono<ResponseEntity<Map<String, Object>>> addToBlacklist(
            @RequestBody Map<String, String> request) {
        
        String ip = request.get("ip");
        String reason = request.getOrDefault("reason", "No reason provided");
        
        // IP 유효성 검사
        if (ip == null || ip.trim().isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "IP address is required");
            errorResponse.put("message", "IP 주소를 제공해주세요.");
            
            return Mono.just(ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(errorResponse));
        }
        
        ip = ip.trim();
        
        // IP 형식 검증 (간단한 검증)
        if (!isValidIpAddress(ip)) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Invalid IP address format");
            errorResponse.put("message", "유효하지 않은 IP 주소 형식입니다.");
            
            return Mono.just(ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(errorResponse));
        }
        
        // 이미 차단되어 있는지 확인
        if (ipBlacklistFilter.isIpBlacklisted(ip)) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "IP already blacklisted");
            response.put("message", "이미 차단된 IP 주소입니다.");
            response.put("ip", ip);
            
            log.warn("이미 차단된 IP 추가 시도. IP: {}", ip);
            
            return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response));
        }
        
        // 블랙리스트에 추가
        ipBlacklistFilter.addToBlacklist(ip);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "IP가 블랙리스트에 추가되었습니다.");
        response.put("ip", ip);
        response.put("reason", reason);
        
        log.info("IP 블랙리스트에 추가됨. IP: {}, 사유: {}", ip, reason);
        
        return Mono.just(ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response));
    }

    /**
     * IP를 블랙리스트에서 제거
     */
    @DeleteMapping("/{ip}")
    public Mono<ResponseEntity<Map<String, Object>>> removeFromBlacklist(@PathVariable String ip) {
        // IP 유효성 검사
        if (ip == null || ip.trim().isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "IP address is required");
            errorResponse.put("message", "IP 주소를 제공해주세요.");
            
            return Mono.just(ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(errorResponse));
        }
        
        ip = ip.trim();
        
        // 블랙리스트에서 제거 시도
        boolean removed = ipBlacklistFilter.removeFromBlacklist(ip);
        
        if (!removed) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "IP not found in blacklist");
            response.put("message", "블랙리스트에 없는 IP 주소입니다.");
            response.put("ip", ip);
            
            log.warn("블랙리스트에 없는 IP 제거 시도. IP: {}", ip);
            
            return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response));
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "IP가 블랙리스트에서 제거되었습니다.");
        response.put("ip", ip);
        
        log.info("IP 블랙리스트에서 제거됨. IP: {}", ip);
        
        return Mono.just(ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response));
    }

    /**
     * IP 주소 형식 유효성 검사
     * 간단한 IPv4 형식 검증 (더 엄격한 검증은 필요시 추가)
     */
    private boolean isValidIpAddress(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            return false;
        }
        
        // IPv4 형식: xxx.xxx.xxx.xxx (각 xxx는 0-255)
        String ipPattern = "^([0-9]{1,3}\\.){3}[0-9]{1,3}$";
        if (!ip.matches(ipPattern)) {
            return false;
        }
        
        // 각 옥텟이 0-255 범위인지 확인
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return false;
        }
        
        try {
            for (String part : parts) {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) {
                    return false;
                }
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}

