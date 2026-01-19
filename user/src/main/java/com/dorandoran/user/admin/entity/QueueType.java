package com.dorandoran.user.admin.entity;

/**
 * 관리 필요 내역 큐 타입
 */
public enum QueueType {
  /**
   * 교정 작업
   * - AI 교정 오류 수정
   * - intimacy, voca, translation 항목 교정
   */
  CORRECTION,

  /**
   * 삭제 작업
   * - 부적절한 메시지 삭제
   * - 시스템 오류 메시지 삭제
   */
  DELETION
}