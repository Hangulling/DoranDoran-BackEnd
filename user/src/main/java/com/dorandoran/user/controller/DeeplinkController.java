package com.dorandoran.user.controller;

import com.dorandoran.user.dto.DeeplinkRouteResponse;
import com.dorandoran.user.service.DeeplinkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 딥링크 라우팅 API.
 * GET /api/deeplink/route?path=... — path를 파싱하여 화면(screen)과 파라미터(params) 반환.
 */
@RestController
@RequestMapping("/api/deeplink")
@RequiredArgsConstructor
@Slf4j
public class DeeplinkController {

    private final DeeplinkService deeplinkService;

    @GetMapping("/route")
    public ResponseEntity<DeeplinkRouteResponse> route(
            @RequestParam String path,
            @RequestHeader("X-User-Id") UUID userId) {
        if (userId == null) {
            log.warn("Deeplink route: X-User-Id missing, path={}", path);
            return ResponseEntity.badRequest().build();
        }
        if (path == null || path.isBlank()) {
            log.warn("Deeplink route: path missing, userId={}", userId);
            return ResponseEntity.badRequest().build();
        }
        DeeplinkRouteResponse response = deeplinkService.parsePath(path, userId);
        if ("unknown".equals(response.getScreen())) {
            log.debug("Deeplink route: unknown path, path={}, userId={}", path, userId);
        }
        return ResponseEntity.ok(response);
    }
}
