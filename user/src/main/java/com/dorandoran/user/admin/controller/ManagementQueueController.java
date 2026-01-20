package com.dorandoran.user.admin.controller;

import com.dorandoran.user.admin.dto.request.BatchCompleteRequest;
import com.dorandoran.user.admin.dto.request.BatchDeleteRequest;
import com.dorandoran.user.admin.dto.request.CompleteRequest;
import com.dorandoran.user.admin.dto.request.ManagementQueueRequest;
import com.dorandoran.user.admin.dto.request.ManagementQueueUpdateRequest;
import com.dorandoran.user.admin.dto.response.ManagementQueueCountResponse;
import com.dorandoran.user.admin.dto.response.ManagementQueueResponse;
import com.dorandoran.user.admin.enums.QueueStatus;
import com.dorandoran.user.admin.enums.QueueType;
import com.dorandoran.user.admin.service.ManagementQueueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 관리 필요 내역 큐 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/management-queue")
@RequiredArgsConstructor
@Tag(name = "Admin - Management Queue", description = "관리 필요 내역 큐 API")
public class ManagementQueueController {

  private final ManagementQueueService service;

  /**
   * 관리 필요 내역 등록
   *
   * POST /api/admin/management-queue
   */
  @PostMapping
  @Operation(summary = "관리 필요 내역 등록", description = "문제 메시지를 관리 필요 내역 큐에 등록합니다")
  public ResponseEntity<ManagementQueueResponse> createManagementQueue(
      @Valid @RequestBody ManagementQueueRequest request,
      HttpServletRequest httpRequest
  ) {
    log.info("관리 필요 내역 등록 API 호출: queueType={}", request.getQueueType());

    // TODO: 인증 구현 시 실제 관리자 정보로 교체
    String adminName = "admin@company.com";  // 임시 하드코딩
    String adminIp = httpRequest.getRemoteAddr();

    ManagementQueueResponse response = service.createManagementQueue(
        request,
        adminName,
        adminIp
    );

    log.info("관리 필요 내역 등록 완료: id={}", response.getId());

    return ResponseEntity.ok(response);
  }

  /**
   * 관리 필요 내역 목록 조회 (페이징, 필터링)
   *
   * GET /api/admin/management-queue
   *
   * 쿼리 파라미터:
   * - queueType: CORRECTION, DELETION (선택)
   * - status: PENDING, COMPLETED (선택)
   * - page: 페이지 번호 (기본값 0)
   * - size: 페이지 크기 (기본값 20)
   */
  @GetMapping
  @Operation(summary = "관리 필요 내역 목록 조회", description = "관리 필요 내역을 페이징 및 필터링하여 조회합니다")
  public ResponseEntity<Page<ManagementQueueResponse>> getManagementQueueList(
      @Parameter(description = "큐 타입 (CORRECTION, DELETION)")
      @RequestParam(required = false) QueueType queueType,

      @Parameter(description = "상태 (PENDING, COMPLETED)")
      @RequestParam(required = false) QueueStatus status,

      @Parameter(description = "페이지 번호 (0부터 시작)")
      @RequestParam(defaultValue = "0") int page,

      @Parameter(description = "페이지 크기")
      @RequestParam(defaultValue = "20") int size
  ) {
    log.info("관리 필요 내역 목록 조회 API 호출: queueType={}, status={}, page={}, size={}",
        queueType, status, page, size);

    Page<ManagementQueueResponse> response = service.getManagementQueueList(
        queueType,
        status,
        page,
        size
    );

    log.info("관리 필요 내역 목록 조회 완료: totalElements={}", response.getTotalElements());

    return ResponseEntity.ok(response);
  }

  /**
   * 관리 필요 내역 단건 조회
   *
   * GET /api/admin/management-queue/{id}
   */
  @GetMapping("/{id}")
  @Operation(summary = "관리 필요 내역 단건 조회", description = "특정 관리 필요 내역의 상세 정보를 조회합니다")
  public ResponseEntity<ManagementQueueResponse> getManagementQueue(
      @Parameter(description = "관리 내역 ID")
      @PathVariable UUID id
  ) {
    log.info("관리 필요 내역 단건 조회 API 호출: id={}", id);

    ManagementQueueResponse response = service.getManagementQueue(id);

    return ResponseEntity.ok(response);
  }

  /**
   * 관리 필요 내역 수정 (메모만 수정 가능)
   *
   * PUT /api/admin/management-queue/{id}
   */
  @PutMapping("/{id}")
  @Operation(summary = "관리 필요 내역 메모 수정", description = "관리 필요 내역의 메모를 수정합니다")
  public ResponseEntity<ManagementQueueResponse> updateManagementQueue(
      @Parameter(description = "관리 내역 ID")
      @PathVariable UUID id,

      @Valid @RequestBody ManagementQueueUpdateRequest request
  ) {
    log.info("관리 필요 내역 메모 수정 API 호출: id={}", id);

    ManagementQueueResponse response = service.updateManagementQueue(
        id,
        request.getMemo()
    );

    log.info("관리 필요 내역 메모 수정 완료: id={}", id);

    return ResponseEntity.ok(response);
  }

  /**
   * 관리 필요 내역 삭제
   *
   * DELETE /api/admin/management-queue/{id}
   */
  @DeleteMapping("/{id}")
  @Operation(summary = "관리 필요 내역 삭제", description = "관리 필요 내역을 삭제합니다")
  public ResponseEntity<Void> deleteManagementQueue(
      @Parameter(description = "관리 내역 ID")
      @PathVariable UUID id
  ) {
    log.info("관리 필요 내역 삭제 API 호출: id={}", id);

    service.deleteManagementQueue(id);

    log.info("관리 필요 내역 삭제 완료: id={}", id);

    return ResponseEntity.noContent().build();
  }

  /**
   * 관리 필요 내역 처리 완료
   *
   * PATCH /api/admin/management-queue/{id}/complete
   */
  @PatchMapping("/{id}/complete")
  @Operation(summary = "관리 필요 내역 처리 완료", description = "관리 필요 내역을 처리 완료 상태로 변경합니다")
  public ResponseEntity<ManagementQueueResponse> completeManagementQueue(
      @Parameter(description = "관리 내역 ID")
      @PathVariable UUID id,

      @Valid @RequestBody CompleteRequest request
  ) {
    log.info("관리 필요 내역 처리 완료 API 호출: id={}", id);

    // TODO: 인증 구현 시 실제 관리자 정보로 교체
    String processedBy = "admin@company.com";  // 임시 하드코딩

    ManagementQueueResponse response = service.completeManagementQueue(
        id,
        processedBy,
        request.getNote()
    );

    log.info("관리 필요 내역 처리 완료: id={}", id);

    return ResponseEntity.ok(response);
  }

  /**
   * 감사 로그 조회 (Phase 6)
   *
   * GET /api/admin/management-queue/audit-logs
   *
   * 쿼리 파라미터:
   * - queueType: CORRECTION, DELETION (선택)
   * - adminName: 관리자 이메일 (선택)
   * - startDate: 시작일 (필수, ISO 8601 형식)
   * - endDate: 종료일 (필수, ISO 8601 형식)
   * - page: 페이지 번호 (기본값 0)
   * - size: 페이지 크기 (기본값 20)
   */
  @GetMapping("/audit-logs")
  @Operation(summary = "감사 로그 조회", description = "관리자 작업 이력을 기간별/타입별/관리자별로 조회합니다")
  public ResponseEntity<Page<ManagementQueueResponse>> getAuditLogs(
      @Parameter(description = "큐 타입 (CORRECTION, DELETION)")
      @RequestParam(required = false) QueueType queueType,

      @Parameter(description = "관리자 이메일")
      @RequestParam(required = false) String adminName,

      @Parameter(description = "시작일 (ISO 8601 형식: 2025-01-01T00:00:00)")
      @RequestParam String startDate,

      @Parameter(description = "종료일 (ISO 8601 형식: 2025-01-31T23:59:59)")
      @RequestParam String endDate,

      @Parameter(description = "페이지 번호 (0부터 시작)")
      @RequestParam(defaultValue = "0") int page,

      @Parameter(description = "페이지 크기")
      @RequestParam(defaultValue = "20") int size
  ) {
    log.info("감사 로그 조회 API 호출: queueType={}, adminName={}, startDate={}, endDate={}",
        queueType, adminName, startDate, endDate);

    // String을 LocalDateTime으로 변환
    java.time.LocalDateTime start = java.time.LocalDateTime.parse(startDate);
    java.time.LocalDateTime end = java.time.LocalDateTime.parse(endDate);

    Page<ManagementQueueResponse> response = service.getAuditLogs(
        queueType,
        adminName,
        start,
        end,
        page,
        size
    );

    log.info("감사 로그 조회 완료: totalElements={}", response.getTotalElements());

    return ResponseEntity.ok(response);
  }

  /**
   * 관리 필요 내역 타입별 카운트 조회
   *
   * GET /api/admin/management-queue/count
   *
   * 쿼리 파라미터:
   * - status: PENDING, COMPLETED (선택, 기본값: PENDING)
   */
  @GetMapping("/count")
  @Operation(summary = "관리 필요 내역 타입별 카운트", description = "intimacy/conversation/voca 타입별 건수를 조회합니다")
  public ResponseEntity<ManagementQueueCountResponse> getCountByItemType(
      @Parameter(description = "상태 (PENDING, COMPLETED)")
      @RequestParam(required = false, defaultValue = "PENDING") QueueStatus status
  ) {
    log.info("관리 필요 내역 타입별 카운트 조회 API 호출: status={}", status);

    ManagementQueueCountResponse response = service.getCountByItemType(status);

    log.info("관리 필요 내역 타입별 카운트 조회 완료: intimacy={}, conversation={}, voca={}",
        response.getIntimacyCount(), response.getConversationCount(), response.getVocaCount());

    return ResponseEntity.ok(response);
  }

  /**
   * 관리 필요 내역 일괄 처리 완료
   *
   * PATCH /api/admin/management-queue/complete-batch
   */
  @PatchMapping("/complete-batch")
  @Operation(summary = "관리 필요 내역 일괄 처리 완료", description = "선택한 여러 관리 필요 내역을 한 번에 처리 완료합니다")
  public ResponseEntity<Map<String, Object>> batchCompleteManagementQueue(
      @Valid @RequestBody BatchCompleteRequest request,
      HttpServletRequest httpRequest
  ) {
    log.info("관리 필요 내역 일괄 처리 완료 API 호출: ids={}", request.getIds());

    // TODO: 인증 구현 시 실제 관리자 정보로 교체
    String processedBy = "admin@company.com";  // 임시 하드코딩

    int completedCount = service.batchCompleteManagementQueue(
        request.getIds(),
        processedBy,
        request.getNote()
    );

    log.info("관리 필요 내역 일괄 처리 완료: 성공={}/{}", completedCount, request.getIds().size());

    return ResponseEntity.ok(Map.of(
        "completedCount", completedCount,
        "totalRequested", request.getIds().size(),
        "message", completedCount + "건 처리 완료되었습니다"
    ));
  }

  /**
   * 관리 필요 내역 일괄 삭제
   *
   * DELETE /api/admin/management-queue/batch
   */
  @DeleteMapping("/batch")
  @Operation(summary = "관리 필요 내역 일괄 삭제", description = "선택한 여러 관리 필요 내역을 한 번에 삭제합니다")
  public ResponseEntity<Map<String, Object>> batchDeleteManagementQueue(
      @Valid @RequestBody BatchDeleteRequest request
  ) {
    log.info("관리 필요 내역 일괄 삭제 API 호출: ids={}", request.getIds());

    int deletedCount = service.batchDeleteManagementQueue(request.getIds());

    log.info("관리 필요 내역 일괄 삭제 완료: 성공={}/{}", deletedCount, request.getIds().size());

    return ResponseEntity.ok(Map.of(
        "deletedCount", deletedCount,
        "totalRequested", request.getIds().size(),
        "message", deletedCount + "건 삭제되었습니다"
    ));
  }

}