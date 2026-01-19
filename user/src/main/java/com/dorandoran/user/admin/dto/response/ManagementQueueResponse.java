package com.dorandoran.user.admin.dto.response;

import com.dorandoran.user.admin.entity.QueueStatus;
import com.dorandoran.user.admin.entity.QueueType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 관리 필요 내역 응답 DTO
 * - 단건 조회, 목록 조회에서 사용
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)  // null 필드는 응답에서 제외
public class ManagementQueueResponse {

  private UUID id;

  // 큐 타입
  private QueueType queueType;

  // 상태
  private QueueStatus status;

  // 요청 데이터 (JSON → Map)
  private Map<String, Object> requestData;

  /**
   * 처리 결과 데이터 (JSON → Map)
   * - 처리 완료 시에만 존재
   */
  private Map<String, Object> resultData;

  // 관리자 이메일
  private String adminName;

  // 관리자 IP
  private String adminIp;

  // 생성 시각
  private LocalDateTime createdAt;

  // 수정 시각
  private LocalDateTime updatedAt;

  // 완료 시각
  private LocalDateTime completedAt;

  // 에러 메시지
  private String errorMessage;
}