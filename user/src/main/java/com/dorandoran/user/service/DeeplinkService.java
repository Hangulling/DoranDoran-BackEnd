package com.dorandoran.user.service;

import com.dorandoran.user.dto.DeeplinkRouteResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 딥링크 path 파싱: path → { screen, params }.
 * path 예: /chatroom/{chatroomId}, /archive/{storeId}, /chatroom/create?chatbotId=...
 */
@Service
@Slf4j
public class DeeplinkService {

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    /**
     * path와 userId로 이동할 화면(screen)과 파라미터(params) 반환.
     * 지원 path 형식:
     * - /chatroom/{chatroomId} → screen=chatroom, params.chatroomId
     * - /archive/{storeId} → screen=archive, params.storeId
     * - /chatroom/create → screen=chatroomCreate (쿼리 파라미터는 호출측에서 전달)
     */
    public DeeplinkRouteResponse parsePath(String path, UUID userId) {
        if (path == null || path.isBlank()) {
            log.warn("Deeplink parsePath: path null or blank, userId={}", userId);
            return DeeplinkRouteResponse.builder()
                    .screen("unknown")
                    .params(Map.of())
                    .build();
        }
        String normalized = path.startsWith("/") ? path : "/" + path;
        String pathOnly = normalized.contains("?") ? normalized.substring(0, normalized.indexOf("?")) : normalized;
        String[] segments = pathOnly.split("/");
        // segments[0] is "" when path is "/chatroom/xxx"
        int start = segments.length > 0 && segments[0].isBlank() ? 1 : 0;
        if (start >= segments.length) {
            log.warn("Deeplink parsePath: no segment, path={}, userId={}", path, userId);
            return DeeplinkRouteResponse.builder()
                    .screen("unknown")
                    .params(Map.of())
                    .build();
        }
        String screenType = segments[start].toLowerCase();
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId != null ? userId.toString() : null);

        switch (screenType) {
            case "chatroom":
                if (start + 1 < segments.length) {
                    String second = segments[start + 1];
                    if ("create".equalsIgnoreCase(second)) {
                        params.put("action", "create");
                        return DeeplinkRouteResponse.builder()
                                .screen("chatroomCreate")
                                .params(params)
                                .build();
                    }
                    if (isUuid(second)) {
                        params.put("chatroomId", second);
                        return DeeplinkRouteResponse.builder()
                                .screen("chatroom")
                                .params(params)
                                .build();
                    }
                }
                return DeeplinkRouteResponse.builder()
                        .screen("chatroom")
                        .params(params)
                        .build();
            case "archive":
                if (start + 1 < segments.length && isUuid(segments[start + 1])) {
                    params.put("storeId", segments[start + 1]);
                }
                return DeeplinkRouteResponse.builder()
                        .screen("archive")
                        .params(params)
                        .build();
            default:
                log.warn("Deeplink parsePath: unsupported path, path={}, userId={}", path, userId);
                params.put("path", path);
                return DeeplinkRouteResponse.builder()
                        .screen("unknown")
                        .params(params)
                        .build();
        }
    }

    private static boolean isUuid(String s) {
        return s != null && UUID_PATTERN.matcher(s).matches();
    }
}
