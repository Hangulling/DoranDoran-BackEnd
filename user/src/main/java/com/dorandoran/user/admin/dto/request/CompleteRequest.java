package com.dorandoran.user.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 관리 필요 내역 처리 완료 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompleteRequest {

  /**
   * 처리 노트
   * - 어떻게 처리했는지 설명
   */
  @NotBlank(message = "처리 노트는 필수입니다")
  private String note;
}