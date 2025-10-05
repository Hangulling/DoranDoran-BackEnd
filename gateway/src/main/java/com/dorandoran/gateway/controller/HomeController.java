package com.dorandoran.gateway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
public class HomeController {

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
            "batch", "/api/batch/**"
        ));
        return Mono.just(response);
    }
}
