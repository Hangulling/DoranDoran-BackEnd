package com.dorandoran.user.admin.service;

import com.dorandoran.user.admin.dto.request.ManagementQueueRequest;
import com.dorandoran.user.admin.dto.response.ManagementQueueCountResponse;
import com.dorandoran.user.admin.dto.response.ManagementQueueResponse;
import com.dorandoran.user.admin.entity.ManagementQueue;
import com.dorandoran.user.admin.entity.QueueStatus;
import com.dorandoran.user.admin.entity.QueueType;
import com.dorandoran.user.admin.repository.ManagementQueueRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
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
   * 감사 로그 조회 (기간별, 타입별, 관리자별 필터링)
   *
   * @param queueType 큐 타입 (nullable)
   * @param adminName 관리자 이메일 (nullable)
   * @param startDate 시작일
   * @param endDate 종료일
   * @param page 페이지 번호
   * @param size 페이지 크기
   * @return 페이징된 감사 로그 목록
   */
  public Page<ManagementQueueResponse> getAuditLogs(
      QueueType queueType,
      String adminName,
      LocalDateTime startDate,
      LocalDateTime endDate,
      int page,
      int size
  ) {
    log.info("감사 로그 조회: queueType={}, adminName={}, startDate={}, endDate={}, page={}, size={}",
        queueType, adminName, startDate, endDate, page, size);

    // 페이징 설정 (최신순 정렬)
    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

    // 필터 조회
    Page<ManagementQueue> queuePage = repository.findByFilters(
        queueType,
        adminName,
        startDate,
        endDate,
        pageable
    );

    log.info("감사 로그 조회 완료: totalElements={}", queuePage.getTotalElements());

    // Entity -> DTO 변환
    return queuePage.map(this::toResponse);
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

  /**
   * 관리 필요 내역 타입별 카운트 조회
   *
   * requestData.items 배열 내 type 필드를 집계
   * - intimacy, conversation, voca 타입별 PENDING 건수
   *
   * @param status 상태 (PENDING/COMPLETED)
   * @return 타입별 카운트
   */
  public ManagementQueueCountResponse getCountByItemType(QueueStatus status) {
    log.info("관리 필요 내역 타입별 카운트 조회: status={}", status);

    // 상태별 전체 조회
    List<ManagementQueue> queues = status != null
        ? repository.findByStatus(status, PageRequest.of(0, Integer.MAX_VALUE)).getContent()
        : repository.findAll();

    long intimacyCount = 0;
    long conversationCount = 0;
    long vocaCount = 0;

    // requestData 파싱하여 items의 type 집계
    for (ManagementQueue queue : queues) {
      Map<String, Object> requestData = parseJsonb(queue.getRequestData());

      if (requestData == null || !requestData.containsKey("items")) {
        continue;
      }

      List<Map<String, Object>> items = (List<Map<String, Object>>) requestData.get("items");

      if (items == null) {
        continue;
      }

      for (Map<String, Object> item : items) {
        String type = (String) item.get("type");

        if (type == null) {
          continue;
        }

        switch (type.toLowerCase()) {
          case "intimacy":
            intimacyCount++;
            break;
          case "conversation":
          case "conver":  // 약어 처리
            conversationCount++;
            break;
          case "vocabulary":
          case "voca":  // 약어 처리
            vocaCount++;
            break;
          default:
            log.warn("알 수 없는 아이템 타입: {}", type);
        }
      }
    }

    long totalPendingCount = status == QueueStatus.PENDING
        ? repository.countByStatus(QueueStatus.PENDING)
        : queues.size();

    log.info("타입별 카운트 조회 완료: intimacy={}, conversation={}, voca={}, total={}",
        intimacyCount, conversationCount, vocaCount, totalPendingCount);

    return ManagementQueueCountResponse.builder()
        .intimacyCount(intimacyCount)
        .conversationCount(conversationCount)
        .vocaCount(vocaCount)
        .totalPendingCount(totalPendingCount)
        .build();
  }

  /**
   * 관리 필요 내역 일괄 처리 완료
   *
   * @param ids 처리 완료할 관리 내역 ID 리스트
   * @param processedBy 처리자 이메일
   * @param note 처리 노트
   * @return 처리 완료된 건수
   */
  @Transactional
  public int batchCompleteManagementQueue(
      List<UUID> ids,
      String processedBy,
      String note
  ) {
    log.info("관리 필요 내역 일괄 처리 완료: ids={}, processedBy={}, count={}",
        ids, processedBy, ids.size());

    int completedCount = 0;

    for (UUID id : ids) {
      try {
        ManagementQueue queue = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("관리 필요 내역을 찾을 수 없습니다: " + id));

        // 이미 완료된 항목은 스킵
        if (queue.getStatus() == QueueStatus.COMPLETED) {
          log.warn("이미 처리 완료된 항목입니다: id={}", id);
          continue;
        }

        // result_data 생성
        Map<String, Object> resultData = Map.of(
            "processedBy", processedBy,
            "processedAt", LocalDateTime.now().toString(),
            "action", "BATCH_COMPLETED",
            "note", note
        );

        String resultDataJson = objectMapper.writeValueAsString(resultData);

        // Entity 업데이트
        queue.complete(processedBy, note);
        queue.setResultData(resultDataJson);

        completedCount++;

      } catch (JsonProcessingException e) {
        log.error("관리 필요 내역 처리 완료 실패: id={}", id, e);
        throw new IllegalArgumentException("처리 완료 실패: " + id, e);
      } catch (IllegalArgumentException e) {
        log.error("관리 필요 내역을 찾을 수 없습니다: id={}", id);
        throw e;
      }
    }

    log.info("관리 필요 내역 일괄 처리 완료: 성공={}/{}", completedCount, ids.size());

    return completedCount;
  }

  /**
   * 관리 필요 내역 일괄 삭제
   *
   * @param ids 삭제할 관리 내역 ID 리스트
   * @return 삭제된 건수
   */
  @Transactional
  public int batchDeleteManagementQueue(List<UUID> ids) {
    log.info("관리 필요 내역 일괄 삭제: ids={}, count={}", ids, ids.size());

    int deletedCount = 0;

    for (UUID id : ids) {
      try {
        if (!repository.existsById(id)) {
          log.warn("관리 필요 내역을 찾을 수 없습니다: id={}", id);
          continue;
        }

        repository.deleteById(id);
        deletedCount++;

      } catch (Exception e) {
        log.error("관리 필요 내역 삭제 실패: id={}", id, e);
        throw new IllegalArgumentException("삭제 실패: " + id, e);
      }
    }

    log.info("관리 필요 내역 일괄 삭제 완료: 성공={}/{}", deletedCount, ids.size());

    return deletedCount;
  }
}