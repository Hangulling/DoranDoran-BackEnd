package com.dorandoran.user.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 관리 필요 내역 타입별 카운트 응답 DTO
 *
 * intimacy/conversation/voca별 PENDING 건수
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManagementQueueCountResponse {

  // intimacy 타입 건수
  private Long intimacyCount;

  // conversation 타입 건수
  private Long conversationCount;

  // voca 타입 건수
  private Long vocaCount;

  // 전체 PENDING 건수
  private Long totalPendingCount;
}