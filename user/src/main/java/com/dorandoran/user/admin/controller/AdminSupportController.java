package com.dorandoran.user.admin.controller;

import com.dorandoran.common.response.ApiResponse;
import com.dorandoran.user.admin.dto.response.AdminSupportDetailResponse;
import com.dorandoran.user.admin.dto.response.AdminSupportPageResponse;
import com.dorandoran.user.admin.service.AdminSupportQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 관리자용 문의/신고 조회 컨트롤러
 */
@RestController
@RequestMapping("/api/admin/support")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Support", description = "관리자 문의/신고 조회 API")
public class AdminSupportController {

    private final AdminSupportQueryService adminSupportQueryService;

    @GetMapping
    @Operation(summary = "문의/신고 목록 조회", description = "전체 문의/신고 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<AdminSupportPageResponse>> getAllSupportRequests(
            @RequestHeader("X-User-Id") String adminIdHeader,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean replyRequested,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        AdminSupportPageResponse response = adminSupportQueryService.getAllSupportRequests(
                type, userId, category, replyRequested, from, to, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "문의/신고 상세 조회", description = "문의/신고 상세를 조회합니다.")
    public ResponseEntity<ApiResponse<AdminSupportDetailResponse>> getSupportRequestDetail(
            @RequestHeader("X-User-Id") String adminIdHeader,
            @PathVariable Long id
    ) {
        AdminSupportDetailResponse response = adminSupportQueryService.getSupportRequestDetail(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
