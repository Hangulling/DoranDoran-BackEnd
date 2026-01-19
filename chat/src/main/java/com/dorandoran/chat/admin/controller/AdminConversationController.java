package com.dorandoran.chat.admin.controller;

import com.dorandoran.chat.admin.dto.AdminConversationDetailResponse;
import com.dorandoran.chat.admin.dto.AdminConversationListResponse;
import com.dorandoran.chat.admin.service.AdminConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 관리자 대화 조회 Controller
 */
@RestController
@RequestMapping("/api/admin/conversations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Conversations", description = "관리자 대화 조회 API")
public class AdminConversationController {

    private final AdminConversationService adminConversationService;

    @GetMapping
    @Operation(summary = "대화 목록 조회", description = "관리자용 대화 목록을 조회합니다.")
    public ResponseEntity<AdminConversationListResponse> getConversations(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String userEmail,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String roomKey,
            @RequestParam(required = false) Integer intimacyLevel,
            @RequestParam(required = false, defaultValue = "chat") String dataSource,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        try {
            UUID userUuid = (userId != null && !userId.isBlank()) ? UUID.fromString(userId) : null;
            LocalDateTime fromTime = parseDateTime(from);
            LocalDateTime toTime = parseDateTime(to);

            AdminConversationListResponse response = adminConversationService.getConversations(
                userUuid, userEmail, fromTime, toTime, roomKey, intimacyLevel, dataSource, page, size
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("대화 목록 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{conversationId}")
    @Operation(summary = "대화 상세 조회", description = "관리자용 대화 상세를 조회합니다.")
    public ResponseEntity<AdminConversationDetailResponse> getConversationDetail(
            @PathVariable UUID conversationId,
            @RequestParam(required = false, defaultValue = "chat") String dataSource
    ) {
        try {
            AdminConversationDetailResponse response = adminConversationService.getConversationDetail(conversationId, dataSource);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("대화 상세 조회 실패", e);
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
