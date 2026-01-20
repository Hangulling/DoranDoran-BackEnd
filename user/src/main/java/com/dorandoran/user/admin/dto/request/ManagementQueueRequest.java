package com.dorandoran.user.admin.dto.request;

import com.dorandoran.user.admin.enums.QueueType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 관리 필요 내역 등록 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManagementQueueRequest {

  /**
   * 큐 타입
   * - CORRECTION: 교정 작업
   * - DELETION: 삭제 작업
   */
  @NotNull(message = "큐 타입은 필수입니다")
  private QueueType queueType;

  // 요청 데이터 (JSON)
  @NotNull(message = "요청 데이터는 필수입니다")
  private Map<String, Object> requestData;
}