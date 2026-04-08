package com.dorandoran.user.controller;

import com.dorandoran.common.response.ApiResponse;
import com.dorandoran.user.dto.FcmTokenRequest;
import com.dorandoran.user.dto.MarkReadBulkRequest;
import com.dorandoran.user.dto.PushNotificationRequest;
import com.dorandoran.user.dto.UnreadPushPageResponse;
import com.dorandoran.user.entity.FcmToken;
import com.dorandoran.user.entity.UnreadPushLog;
import com.dorandoran.user.service.FcmTokenService;
import com.dorandoran.user.service.PushNotificationService;
import com.dorandoran.user.service.UnreadPushLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private static final int UNREAD_PAGE_MAX_SIZE = 100;

    private final FcmTokenService fcmTokenService;
    private final PushNotificationService pushNotificationService;
    private final UnreadPushLogService unreadPushLogService;

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

    @GetMapping("/unread")
    public ResponseEntity<ApiResponse<UnreadPushPageResponse>> getUnreadPushList(
        @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        if (userIdHeader == null || userIdHeader.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("사용자 인증 정보가 필요합니다."));
        }
        UUID userId;
        try {
            userId = UUID.fromString(userIdHeader);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error("사용자 ID 형식이 올바르지 않습니다."));
        }
        int safeSize = Math.min(Math.max(size, 1), UNREAD_PAGE_MAX_SIZE);
        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize);
        Page<UnreadPushLog> result = unreadPushLogService.findUnreadByUserId(userId, pageable);
        UnreadPushPageResponse body = UnreadPushPageResponse.fromPage(result);
        return ResponseEntity.ok(ApiResponse.success(body));
    }

    @PostMapping("/unread/mark-read")
    public ResponseEntity<ApiResponse<Void>> markUnreadPushReadBulk(
        @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
        @RequestBody(required = false) MarkReadBulkRequest request
    ) {
        if (userIdHeader == null || userIdHeader.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("사용자 인증 정보가 필요합니다."));
        }
        UUID userId;
        try {
            userId = UUID.fromString(userIdHeader);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error("사용자 ID 형식이 올바르지 않습니다."));
        }
        List<Long> ids = request != null ? request.ids() : null;
        unreadPushLogService.markRead(userId, ids);
        return ResponseEntity.ok(ApiResponse.success(null, "읽음 처리되었습니다."));
    }

    @PostMapping("/unread/{id}/mark-read")
    public ResponseEntity<ApiResponse<Void>> markUnreadPushReadSingle(
        @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
        @PathVariable Long id
    ) {
        if (userIdHeader == null || userIdHeader.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("사용자 인증 정보가 필요합니다."));
        }
        UUID userId;
        try {
            userId = UUID.fromString(userIdHeader);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error("사용자 ID 형식이 올바르지 않습니다."));
        }
        if (id == null || id < 1) {
            return ResponseEntity.badRequest().body(ApiResponse.error("알림 ID가 올바르지 않습니다."));
        }
        if (!unreadPushLogService.markReadById(userId, id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("해당 알림을 찾을 수 없습니다."));
        }
        return ResponseEntity.ok(ApiResponse.success(null, "읽음 처리되었습니다."));
    }
}
