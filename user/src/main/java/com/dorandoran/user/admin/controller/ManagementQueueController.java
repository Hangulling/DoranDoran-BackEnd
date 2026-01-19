package com.dorandoran.user.admin.controller;

import com.dorandoran.user.admin.dto.request.CompleteRequest;
import com.dorandoran.user.admin.dto.request.ManagementQueueRequest;
import com.dorandoran.user.admin.dto.request.ManagementQueueUpdateRequest;
import com.dorandoran.user.admin.dto.response.ManagementQueueResponse;
import com.dorandoran.user.admin.entity.QueueStatus;
import com.dorandoran.user.admin.entity.QueueType;
import com.dorandoran.user.admin.service.ManagementQueueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
}