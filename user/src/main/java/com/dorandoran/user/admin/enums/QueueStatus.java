package com.dorandoran.user.admin.enums;

/**
 * 관리 필요 내역 큐 상태
 */
public enum QueueStatus {
  /**
   * 대기 중
   * - 아직 처리되지 않은 상태
   * - 관리자가 처리해야 함
   */
  PENDING,

  /**
   * 처리 완료
   * - 관리자가 처리를 완료한 상태
   * - result_data에 처리 결과 저장됨
   */
  COMPLETED
}