package com.dorandoran.user.controller;

import com.dorandoran.common.exception.ErrorCode;
import com.dorandoran.common.response.ApiResponse;
import com.dorandoran.user.dto.SupportCreateRequest;
import com.dorandoran.user.dto.SupportCreateResponse;
import com.dorandoran.user.entity.SupportRequest;
import com.dorandoran.user.service.SupportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
@Slf4j
public class SupportController {

    private final SupportService supportService;

    /**
     * GET으로 URL만 열면 정적 리소스 폴백 → NoResourceFoundException → 기존에는 500으로 처리되던 케이스를 방지합니다.
     * 실제 문의 접수는 POST만 지원합니다.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Void>> supportGetNotAllowed() {
        return ResponseEntity
            .status(HttpStatus.METHOD_NOT_ALLOWED)
            .header(HttpHeaders.ALLOW, "POST, OPTIONS")
            .body(ApiResponse.error("문의 등록은 POST /api/support 만 지원합니다.", ErrorCode.INVALID_REQUEST.getCode()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SupportCreateResponse>> createSupport(
        @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
        @RequestHeader(value = "X-User-Email", required = false) String userEmailHeader,
        @RequestHeader(value = "X-User-Name", required = false) String userNameHeader,
        @RequestBody SupportCreateRequest request
    ) {
        if (userIdHeader == null || userIdHeader.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("사용자 인증 정보가 필요합니다."));
        }
        UUID userId;
        try {
            userId = UUID.fromString(userIdHeader.trim());
        } catch (IllegalArgumentException ex) {
            String h = userIdHeader.trim();
            log.warn(
                "문의 API X-User-Id UUID 파싱 실패: len={}, commaSeparated={}, preview={}",
                h.length(),
                h.contains(","),
                h.length() > 16 ? h.substring(0, 16) + "…" : h
            );
            return ResponseEntity.badRequest().body(ApiResponse.error("사용자 ID 형식이 올바르지 않습니다."));
        }

        SupportRequest saved = supportService.createSupportRequest(userId, userEmailHeader, userNameHeader, request);
        SupportCreateResponse response = new SupportCreateResponse(saved.getId(), saved.getCreatedAt());
        return ResponseEntity.ok(ApiResponse.success(response, "문의가 접수되었습니다."));
    }
}
