package com.dorandoran.gateway.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
@Tag(name = "Gateway", description = "API 게이트웨이 정보")
public class HomeController {

    @Operation(summary = "API 게이트웨이 정보", description = "DoranDoran API 게이트웨이의 상태와 엔드포인트 정보를 반환합니다.")
    @ApiResponse(responseCode = "200", description = "게이트웨이 정보 조회 성공")
    @GetMapping("/")
    public Mono<Map<String, Object>> home() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "DoranDoran MSA API Gateway");
        response.put("status", "running");
        response.put("version", "1.0.0");
        response.put("endpoints", Map.of(
            "actuator", "/actuator",
            "auth", "/api/auth/**",
            "user", "/api/users/**",
            "chat", "/api/chat/**",
            "batch", "/api/batch/**",
            "store", "/api/store/**"
        ));
        return Mono.just(response);
    }
}
