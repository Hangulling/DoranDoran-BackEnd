package com.dorandoran.user.admin.controller;

import com.dorandoran.user.admin.dto.response.AdminAuditLogListResponse;
import com.dorandoran.user.admin.dto.response.AdminAuditLogResponse;
import com.dorandoran.user.admin.entity.AdminAuditLog;
import com.dorandoran.user.entity.User;
import com.dorandoran.user.admin.enums.ActionType;
import com.dorandoran.user.admin.service.AdminAuditLogService;
import com.dorandoran.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 관리 감사 로그 Controller
 */
@RestController
@RequestMapping("/api/admin/audit-logs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Audit Log", description = "감사 로그 API")
public class AdminAuditLogController {

    private final AdminAuditLogService adminAuditLogService;
    private final UserRepository userRepository;

    private String resolveAdminEmail(UUID adminUserId) {
        if (adminUserId == null) return null;
        return userRepository.findById(adminUserId).map(User::getEmail).orElse(null);
    }

    @GetMapping
    @Operation(summary = "감사 로그 조회", description = "관리자 감사 로그를 조회합니다.")
    public ResponseEntity<AdminAuditLogListResponse> getAuditLogs(
            @RequestParam(required = false) String adminUserId,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        try {
            UUID adminId = adminUserId != null ? UUID.fromString(adminUserId) : null;
            ActionType action = ActionType.fromString(actionType);
            LocalDateTime fromTime = parseDateTime(from);
            LocalDateTime toTime = parseDateTime(to);

            Pageable pageable = PageRequest.of(page, size);
            Page<AdminAuditLog> logPage = adminAuditLogService.getAuditLogs(adminId, action, fromTime, toTime, pageable);

            AdminAuditLogListResponse response = AdminAuditLogListResponse.builder()
                .content(logPage.getContent().stream()
                    .map(log -> AdminAuditLogResponse.builder()
                        .id(log.getId())
                        .adminUserId(log.getAdminUserId())
                        .adminUserEmail(resolveAdminEmail(log.getAdminUserId()))
                        .actionType(log.getActionType() != null ? log.getActionType().getValue() : null)
                        .targetType(log.getTargetType() != null ? log.getTargetType().name() : null)
                        .targetId(log.getTargetId())
                        .summary(log.getSummary())
                        .beforeJson(log.getBeforeJson())
                        .afterJson(log.getAfterJson())
                        .ip(log.getIp())
                        .userAgent(log.getUserAgent())
                        .createdAt(log.getCreatedAt())
                        .build())
                    .collect(Collectors.toList()))
                .page(AdminAuditLogListResponse.PageInfo.builder()
                    .number(logPage.getNumber())
                    .size(logPage.getSize())
                    .totalPages(logPage.getTotalPages())
                    .totalElements(logPage.getTotalElements())
                    .build())
                .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("감사 로그 조회 실패: {} - {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value).toLocalDateTime();
        } catch (Exception ignored) {
            return LocalDateTime.parse(value);
        }
    }
}
