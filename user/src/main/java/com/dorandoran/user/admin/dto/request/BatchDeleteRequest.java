package com.dorandoran.user.admin.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * 관리 필요 내역 일괄 삭제 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchDeleteRequest {

  // 삭제할 관리 내역 ID 리스트
  @NotEmpty(message = "삭제할 항목을 선택해주세요")
  private List<UUID> ids;
}