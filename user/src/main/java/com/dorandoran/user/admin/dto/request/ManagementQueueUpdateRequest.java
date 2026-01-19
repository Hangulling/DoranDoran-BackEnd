package com.dorandoran.user.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 관리 필요 내역 수정 요청 DTO
 * - 메모만 수정 가능
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManagementQueueUpdateRequest {

  // 수정할 메모 내용
  @NotBlank(message = "메모는 필수입니다")
  private String memo;
}