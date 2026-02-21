package com.dorandoran.user.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * 관리 필요 내역 일괄 처리 완료 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchCompleteRequest {

  // 처리 완료할 관리 내역 ID 리스트
  @NotEmpty(message = "처리할 항목을 선택해주세요")
  private List<UUID> ids;

  /**
   * 처리 노트
   * - 어떻게 처리했는지 설명
   */
  @NotBlank(message = "처리 노트는 필수입니다")
  private String note;
}