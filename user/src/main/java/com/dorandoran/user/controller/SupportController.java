package com.dorandoran.user.controller;

import com.dorandoran.common.response.ApiResponse;
import com.dorandoran.user.dto.SupportCreateRequest;
import com.dorandoran.user.dto.SupportCreateResponse;
import com.dorandoran.user.entity.SupportRequest;
import com.dorandoran.user.service.SupportService;
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
@RequestMapping("/api/support")
@RequiredArgsConstructor
@Slf4j
public class SupportController {

    private final SupportService supportService;

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
            userId = UUID.fromString(userIdHeader);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error("사용자 ID 형식이 올바르지 않습니다."));
        }

        SupportRequest saved = supportService.createSupportRequest(userId, userEmailHeader, userNameHeader, request);
        SupportCreateResponse response = new SupportCreateResponse(saved.getId(), saved.getCreatedAt());
        return ResponseEntity.ok(ApiResponse.success(response, "문의가 접수되었습니다."));
    }
}
