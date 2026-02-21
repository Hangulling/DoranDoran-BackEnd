package com.dorandoran.user.admin.controller;

import com.dorandoran.user.admin.dto.request.ReviewTicketCompleteRequest;
import com.dorandoran.user.admin.dto.request.ReviewTicketCreateRequest;
import com.dorandoran.user.admin.dto.request.ReviewTicketUpdateRequest;
import com.dorandoran.user.admin.dto.response.ReviewTicketCountsResponse;
import com.dorandoran.user.admin.dto.response.ReviewTicketDetailResponse;
import com.dorandoran.user.admin.dto.response.ReviewTicketItemResponse;
import com.dorandoran.user.admin.dto.response.ReviewTicketListResponse;
import com.dorandoran.user.admin.dto.response.ReviewTicketResponse;
import com.dorandoran.user.admin.entity.ReviewTicket;
import com.dorandoran.user.admin.entity.ReviewTicketItem;
import com.dorandoran.user.admin.enums.ReviewStatus;
import com.dorandoran.user.admin.service.AdminAuditLogService;
import com.dorandoran.user.admin.service.ReviewTicketService;
import com.dorandoran.user.admin.enums.ActionType;
import com.dorandoran.user.admin.enums.TargetType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 관리 필요 내역 Controller
 */
// TODO: ManagementQueueController와 100% 동일한 기능 제공 - 팀 내 통합 논의 필요
// TODO: 두 시스템(ReviewTicket vs ManagementQueue) 중 하나를 선택하거나 통합해야 함
@RestController
@RequestMapping("/api/admin/review-tickets")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Review Ticket", description = "관리 필요 내역 API")
public class ReviewTicketController {

    private final ReviewTicketService reviewTicketService;
    private final AdminAuditLogService adminAuditLogService;

    // TODO: ManagementQueueController의 getManagementQueueList()와 100% 중복
    @GetMapping
    @Operation(summary = "티켓 목록 조회", description = "관리 필요 내역 목록을 조회합니다.")
    public ResponseEntity<ReviewTicketListResponse> getTickets(
        @RequestParam(required = false, defaultValue = "OPEN") String status,
        @RequestParam(required = false) String agentType,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestHeader("X-User-Id") String userId
    ) {
        try {
            ReviewStatus statusEnum = ReviewStatus.fromString(status);
            Pageable pageable = PageRequest.of(page, size);

            Page<ReviewTicket> ticketPage = reviewTicketService.getTickets(statusEnum, agentType, pageable);

            ReviewTicketListResponse response = ReviewTicketListResponse.builder()
                .content(ticketPage.getContent().stream()
                    .map(t -> ReviewTicketResponse.builder()
                        .id(t.getId())
                        .conversationId(t.getConversationId())
                        .status(t.getStatus().getValue())
                        .agentType(t.getAgentType())
                        .note(t.getNote())
                        .createdBy(t.getCreatedBy())
                        .assignee(t.getAssignee())
                        .createdAt(t.getCreatedAt())
                        .updatedAt(t.getUpdatedAt())
                        .doneAt(t.getDoneAt())
                        .build())
                    .collect(Collectors.toList()))
                .page(ReviewTicketListResponse.PageInfo.builder()
                    .number(ticketPage.getNumber())
                    .size(ticketPage.getSize())
                    .totalPages(ticketPage.getTotalPages())
                    .totalElements(ticketPage.getTotalElements())
                    .build())
                .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("티켓 목록 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // TODO: ManagementQueueController의 createManagementQueue()와 100% 중복
    @PostMapping
    @Operation(summary = "티켓 생성", description = "관리 필요 내역을 생성합니다.")
    public ResponseEntity<ReviewTicketDetailResponse> createTicket(
        @RequestBody ReviewTicketCreateRequest request,
        @RequestHeader("X-User-Id") String userId,
        HttpServletRequest httpRequest
    ) {
        try {
            UUID adminId = UUID.fromString(userId);
            ReviewTicket ticket = reviewTicketService.createTicket(
                request.getConversationId(),
                request.getAgentType(),
                request.getNote(),
                adminId
            );

            List<ReviewTicketItem> items = request.getItems() != null
                ? request.getItems().stream()
                .map(item -> ReviewTicketItem.builder()
                    .messageId(item.getMessageId())
                    .agentType(item.getAgentType())
                    .snapshotJson(item.getSnapshotJson())
                    .build())
                .collect(Collectors.toList())
                : List.of();

            reviewTicketService.saveTicketItems(ticket, items);

            Map<String, Object> afterJson = new HashMap<>();
            if (request.getConversationId() != null) {
                afterJson.put("conversationId", request.getConversationId());
            }

            adminAuditLogService.logAction(
                ActionType.REVIEW_EXPORT,
                TargetType.REVIEW_TICKET,
                ticket.getId(),
                String.format("티켓 %d 생성", ticket.getId()),
                null,
                afterJson,
                adminId,
                httpRequest
            );

            ReviewTicketDetailResponse response = ReviewTicketDetailResponse.builder()
                .id(ticket.getId())
                .conversationId(ticket.getConversationId())
                .status(ticket.getStatus().getValue())
                .agentType(ticket.getAgentType())
                .note(ticket.getNote())
                .createdBy(ticket.getCreatedBy())
                .assignee(ticket.getAssignee())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .doneAt(ticket.getDoneAt())
                .items(items.stream()
                    .map(item -> ReviewTicketItemResponse.builder()
                        .messageId(item.getMessageId())
                        .agentType(item.getAgentType())
                        .snapshotJson(item.getSnapshotJson())
                        .build())
                    .collect(Collectors.toList()))
                .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("티켓 생성 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // TODO: ManagementQueueController의 getManagementQueue()와 100% 중복
    @GetMapping("/{ticketId}")
    @Operation(summary = "티켓 상세 조회", description = "관리 필요 내역 상세를 조회합니다.")
    public ResponseEntity<ReviewTicketDetailResponse> getTicketDetail(
        @PathVariable Long ticketId,
        @RequestHeader("X-User-Id") String userId
    ) {
        try {
            ReviewTicket ticket = reviewTicketService.getTicket(ticketId);
            List<ReviewTicketItem> items = reviewTicketService.getTicketItems(ticketId);

            ReviewTicketDetailResponse response = ReviewTicketDetailResponse.builder()
                .id(ticket.getId())
                .conversationId(ticket.getConversationId())
                .status(ticket.getStatus().getValue())
                .agentType(ticket.getAgentType())
                .note(ticket.getNote())
                .createdBy(ticket.getCreatedBy())
                .assignee(ticket.getAssignee())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .doneAt(ticket.getDoneAt())
                .items(items.stream()
                    .map(item -> ReviewTicketItemResponse.builder()
                        .messageId(item.getMessageId())
                        .agentType(item.getAgentType())
                        .snapshotJson(item.getSnapshotJson())
                        .build())
                    .collect(Collectors.toList()))
                .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("티켓 상세 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // TODO: ManagementQueueController의 getCountByItemType()와 100% 중복
    @GetMapping("/counts")
    @Operation(summary = "탭별 카운트 조회", description = "상태별 및 에이전트 타입별 카운트를 조회합니다.")
    public ResponseEntity<ReviewTicketCountsResponse> getTicketCounts(
        @RequestParam(required = false, defaultValue = "OPEN") String status,
        @RequestHeader("X-User-Id") String userId
    ) {
        try {
            ReviewStatus statusEnum = ReviewStatus.fromString(status);
            Map<String, Long> counts = reviewTicketService.getTicketCounts(statusEnum);

            ReviewTicketCountsResponse response = ReviewTicketCountsResponse.builder()
                .total(counts.getOrDefault("total", 0L))
                .byAgentType(counts.entrySet().stream()
                    .filter(e -> !e.getKey().equals("total"))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("티켓 카운트 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // TODO: ManagementQueueController의 updateManagementQueue()와 100% 중복
    @PatchMapping("/{ticketId}")
    @Operation(summary = "티켓 메모 수정", description = "관리 필요 내역의 메모를 수정합니다.")
    public ResponseEntity<ReviewTicketResponse> updateTicket(
        @PathVariable Long ticketId,
        @RequestBody ReviewTicketUpdateRequest request,
        @RequestHeader("X-User-Id") String userId,
        HttpServletRequest httpRequest
    ) {
        try {
            UUID adminId = UUID.fromString(userId);
            ReviewTicket ticket = reviewTicketService.updateTicket(ticketId, request.getNote());

            // 감사 로그 기록
            Map<String, Object> afterJson = new HashMap<>();
            afterJson.put("ticketId", ticket.getId());
            afterJson.put("note", ticket.getNote());

            adminAuditLogService.logAction(
                ActionType.REVIEW_EXPORT,  // 메모 수정은 EXPORT로 분류
                TargetType.REVIEW_TICKET,
                ticket.getId(),
                String.format("티켓 %d 메모 수정", ticketId),
                null,
                afterJson,
                adminId,
                httpRequest
            );

            ReviewTicketResponse response = ReviewTicketResponse.builder()
                .id(ticket.getId())
                .conversationId(ticket.getConversationId())
                .status(ticket.getStatus().getValue())
                .agentType(ticket.getAgentType())
                .note(ticket.getNote())
                .createdBy(ticket.getCreatedBy())
                .assignee(ticket.getAssignee())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .doneAt(ticket.getDoneAt())
                .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("티켓 메모 수정 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // TODO: ManagementQueueController의 deleteManagementQueue()와 100% 중복
    @DeleteMapping("/{ticketId}")
    @Operation(summary = "티켓 삭제", description = "관리 필요 내역을 삭제합니다.")
    public ResponseEntity<Void> deleteTicket(
        @PathVariable Long ticketId,
        @RequestHeader("X-User-Id") String userId,
        HttpServletRequest httpRequest
    ) {
        try {
            UUID adminId = UUID.fromString(userId);
            reviewTicketService.deleteTicket(ticketId);

            // 감사 로그 기록
            adminAuditLogService.logAction(
                ActionType.REVIEW_DELETE,
                TargetType.REVIEW_TICKET,
                ticketId,
                String.format("티켓 %d 삭제", ticketId),
                null,
                null,
                adminId,
                httpRequest
            );

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("티켓 삭제 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // TODO: ManagementQueueController의 batchCompleteManagementQueue()와 100% 중복
    @PostMapping("/complete")
    @Operation(summary = "다건 처리 완료", description = "여러 관리 필요 내역을 한 번에 처리 완료합니다.")
    public ResponseEntity<Void> completeTickets(
        @RequestBody ReviewTicketCompleteRequest request,
        @RequestHeader("X-User-Id") String userId,
        HttpServletRequest httpRequest
    ) {
        try {
            UUID adminId = UUID.fromString(userId);
            reviewTicketService.completeTickets(request.getTicketIds());

            // 감사 로그 기록
            Map<String, Object> afterJson = new HashMap<>();
            afterJson.put("ticketIds", request.getTicketIds());
            afterJson.put("count", request.getTicketIds().size());

            adminAuditLogService.logAction(
                ActionType.REVIEW_COMPLETE,
                TargetType.REVIEW_TICKET,
                null,
                String.format("%d개 티켓 처리 완료", request.getTicketIds().size()),
                null,
                afterJson,
                adminId,
                httpRequest
            );

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("티켓 처리 완료 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}