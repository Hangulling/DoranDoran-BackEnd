package com.dorandoran.user.admin.service;

import com.dorandoran.user.admin.dto.request.ManagementQueueRequest;
import com.dorandoran.user.admin.dto.response.ManagementQueueResponse;
import com.dorandoran.user.admin.entity.ManagementQueue;
import com.dorandoran.user.admin.entity.QueueStatus;
import com.dorandoran.user.admin.entity.QueueType;
import com.dorandoran.user.admin.repository.ManagementQueueRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 관리 필요 내역 큐 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ManagementQueueService {

  private final ManagementQueueRepository repository;
  private final ObjectMapper objectMapper;

  /**
   * 관리 필요 내역 등록
   *
   * @param request 등록 요청 DTO
   * @param adminName 관리자 이메일
   * @param adminIp 관리자 IP
   * @return 등록된 관리 내역
   */
  @Transactional
  public ManagementQueueResponse createManagementQueue(
      ManagementQueueRequest request,
      String adminName,
      String adminIp
  ) {
    log.info("관리 필요 내역 등록 시작: queueType={}, adminName={}",
        request.getQueueType(), adminName);

    try {
      // request_data를 JSON 문자열로 변환
      String requestDataJson = objectMapper.writeValueAsString(request.getRequestData());

      // Entity 생성
      ManagementQueue queue = ManagementQueue.builder()
          .queueType(request.getQueueType())
          .status(QueueStatus.PENDING)  // 초기 상태는 PENDING
          .requestData(requestDataJson)
          .adminName(adminName)
          .adminIp(adminIp)
          .build();

      // 저장
      ManagementQueue saved = repository.save(queue);

      log.info("관리 필요 내역 등록 완료: id={}, queueType={}",
          saved.getId(), saved.getQueueType());

      return toResponse(saved);

    } catch (JsonProcessingException e) {
      log.error("관리 필요 내역 등록 실패: JSON 변환 오류", e);
      throw new IllegalArgumentException("요청 데이터 JSON 변환 실패", e);
    }
  }

  /**
   * 관리 필요 내역 목록 조회 (페이징, 필터링)
   *
   * @param queueType 큐 타입 (nullable)
   * @param status 상태 (nullable)
   * @param page 페이지 번호 (0부터 시작)
   * @param size 페이지 크기
   * @return 페이징된 관리 내역 목록
   */
  public Page<ManagementQueueResponse> getManagementQueueList(
      QueueType queueType,
      QueueStatus status,
      int page,
      int size
  ) {
    log.info("관리 필요 내역 목록 조회: queueType={}, status={}, page={}, size={}",
        queueType, status, page, size);

    // 페이징 설정 (최신순 정렬)
    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

    Page<ManagementQueue> queuePage;

    // 필터 조건에 따라 조회
    if (queueType != null && status != null) {
      // 타입 + 상태 모두 필터링
      queuePage = repository.findByQueueTypeAndStatus(queueType, status, pageable);
    } else if (queueType != null) {
      // 타입만 필터링
      queuePage = repository.findByQueueType(queueType, pageable);
    } else if (status != null) {
      // 상태만 필터링
      queuePage = repository.findByStatus(status, pageable);
    } else {
      // 필터 없이 전체 조회
      queuePage = repository.findAll(pageable);
    }

    log.info("관리 필요 내역 목록 조회 완료: totalElements={}, totalPages={}",
        queuePage.getTotalElements(), queuePage.getTotalPages());

    // Entity -> DTO 변환
    return queuePage.map(this::toResponse);
  }

  /**
   * 관리 필요 내역 단건 조회
   *
   * @param id 관리 내역 ID
   * @return 관리 내역 상세
   */
  public ManagementQueueResponse getManagementQueue(UUID id) {
    log.info("관리 필요 내역 단건 조회: id={}", id);

    ManagementQueue queue = repository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("관리 필요 내역을 찾을 수 없습니다: " + id));

    return toResponse(queue);
  }

  /**
   * 관리 필요 내역 메모 수정
   *
   * @param id 관리 내역 ID
   * @param newMemo 새로운 메모 내용
   * @return 수정된 관리 내역
   */
  @Transactional
  public ManagementQueueResponse updateManagementQueue(UUID id, String newMemo) {
    log.info("관리 필요 내역 메모 수정: id={}, newMemo={}", id, newMemo);

    ManagementQueue queue = repository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("관리 필요 내역을 찾을 수 없습니다: " + id));

    try {
      // 기존 request_data를 Map으로 파싱
      Map<String, Object> requestData = parseJsonb(queue.getRequestData());

      // memo 필드 업데이트
      requestData.put("memo", newMemo);

      // 다시 JSON 문자열로 변환
      String updatedRequestData = objectMapper.writeValueAsString(requestData);

      // Entity 업데이트
      queue.updateRequestData(updatedRequestData);

      log.info("관리 필요 내역 메모 수정 완료: id={}", id);

      return toResponse(queue);

    } catch (JsonProcessingException e) {
      log.error("관리 필요 내역 메모 수정 실패: JSON 변환 오류", e);
      throw new IllegalArgumentException("메모 수정 실패", e);
    }
  }

  /**
   * 관리 필요 내역 삭제
   *
   * @param id 관리 내역 ID
   */
  @Transactional
  public void deleteManagementQueue(UUID id) {
    log.info("관리 필요 내역 삭제: id={}", id);

    if (!repository.existsById(id)) {
      throw new IllegalArgumentException("관리 필요 내역을 찾을 수 없습니다: " + id);
    }

    repository.deleteById(id);

    log.info("관리 필요 내역 삭제 완료: id={}", id);
  }

  /**
   * 관리 필요 내역 처리 완료
   *
   * @param id 관리 내역 ID
   * @param processedBy 처리자 이메일
   * @param note 처리 노트
   * @return 처리 완료된 관리 내역
   */
  @Transactional
  public ManagementQueueResponse completeManagementQueue(
      UUID id,
      String processedBy,
      String note
  ) {
    log.info("관리 필요 내역 처리 완료: id={}, processedBy={}", id, processedBy);

    ManagementQueue queue = repository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("관리 필요 내역을 찾을 수 없습니다: " + id));

    // result_data 생성
    Map<String, Object> resultData = Map.of(
        "processedBy", processedBy,
        "processedAt", LocalDateTime.now().toString(),
        "action", "COMPLETED",
        "note", note
    );

    try {
      String resultDataJson = objectMapper.writeValueAsString(resultData);

      // Entity 업데이트
      queue.complete(processedBy, note);
      queue.setResultData(resultDataJson);

      log.info("관리 필요 내역 처리 완료: id={}", id);

      return toResponse(queue);

    } catch (JsonProcessingException e) {
      log.error("관리 필요 내역 처리 완료 실패: JSON 변환 오류", e);
      throw new IllegalArgumentException("처리 완료 실패", e);
    }
  }

  /**
   * Entity를 Response DTO로 변환
   *
   * @param queue ManagementQueue Entity
   * @return ManagementQueueResponse DTO
   */
  private ManagementQueueResponse toResponse(ManagementQueue queue) {
    return ManagementQueueResponse.builder()
        .id(queue.getId())
        .queueType(queue.getQueueType())
        .status(queue.getStatus())
        .requestData(parseJsonb(queue.getRequestData()))
        .resultData(parseJsonb(queue.getResultData()))
        .adminName(queue.getAdminName())
        .adminIp(queue.getAdminIp())
        .createdAt(queue.getCreatedAt())
        .updatedAt(queue.getUpdatedAt())
        .completedAt(queue.getCompletedAt())
        .errorMessage(queue.getErrorMessage())
        .build();
  }

  /**
   * JSONB String을 Map으로 파싱
   *
   * @param jsonbString JSONB 문자열
   * @return Map<String, Object> 또는 null
   */
  private Map<String, Object> parseJsonb(String jsonbString) {
    if (jsonbString == null) {
      return null;
    }

    try {
      return objectMapper.readValue(jsonbString, Map.class);
    } catch (JsonProcessingException e) {
      log.error("JSONB 파싱 실패: {}", jsonbString, e);
      return Map.of(); // 빈 Map 반환
    }
  }
}