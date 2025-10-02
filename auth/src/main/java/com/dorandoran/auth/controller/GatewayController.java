package com.dorandoran.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gateway용 컨트롤러
 */
@RestController
@RequestMapping("/api/auth")
public class GatewayController {
    
    /**
     * 헬스체크 (Gateway용)
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Auth service is running");
    }
}
