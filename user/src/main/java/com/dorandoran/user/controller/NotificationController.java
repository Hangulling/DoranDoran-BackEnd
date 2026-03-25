package com.dorandoran.user.controller;

import com.dorandoran.common.response.ApiResponse;
import com.dorandoran.user.dto.FcmTokenRequest;
import com.dorandoran.user.dto.PushNotificationRequest;
import com.dorandoran.user.entity.FcmToken;
import com.dorandoran.user.service.FcmTokenService;
import com.dorandoran.user.service.PushNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final FcmTokenService fcmTokenService;
    private final PushNotificationService pushNotificationService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> registerFcmToken(
        @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
        @RequestBody FcmTokenRequest request
    ) {
        if (userIdHeader == null || userIdHeader.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("사용자 인증 정보가 필요합니다."));
        }
        UUID userId = UUID.fromString(userIdHeader);
        if (request.token() == null || request.token().isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("FCM 토큰이 필요합니다."));
        }
        String platform = request.platform() != null ? request.platform() : "unknown";
        if (platform != null && "ios".equalsIgnoreCase(platform)) {
            String tokenValue = request.token() != null ? request.token() : "";
            String tokenPreview = tokenValue.length() > 12 ? tokenValue.substring(0, 12) + "..." : tokenValue;
            log.info("[iOS FCM debug] register: userId={}, platform=ios, tokenPreview={}, tokenLen={}",
                userId, tokenPreview, tokenValue.length());
        }
        FcmToken token = fcmTokenService.registerToken(userId, request.token(), platform);
        log.info("FCM 토큰 등록: userId={}, platform={}, tokenId={}", userId, platform, token.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "FCM 토큰이 등록되었습니다."));
    }

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<Void>> sendPushNotification(
        @RequestBody PushNotificationRequest request
    ) {
        if (request.userId() == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("userId가 필요합니다."));
        }
        pushNotificationService.sendToUser(
            request.userId(),
            request.title(),
            request.body(),
            request.chatroomId(),
            request.messageId()
        );
        return ResponseEntity.ok(ApiResponse.success(null, "푸시 알림 요청이 처리되었습니다."));
    }
}
