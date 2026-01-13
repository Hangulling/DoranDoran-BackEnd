package com.dorandoran.user.admin.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 메시지 타임라인 응답 DTO
 * 특정 채팅방의 메시지를 순서대로 조회
 */
@Getter
@Builder
public class MessageTimelineResponse {

  // 메시지 ID
  private UUID messageId;

  // 메시지 내용
  private String content;

  // 발신자 타입 (USER, CHATBOT, SYSTEM)
  private String senderType;

  // 메시지 순서 번호 (오름차순)
  private Long sequenceNumber;

  // 대화 턴 번호
  private Long turnNumber;

  // 메시지 타입
  private String contentType;

  // 생성 시간
  private LocalDateTime sourceCreatedAt;

  // 토큰 수 (AI 응답인 경우)
  private Integer tokenCount;

  // 처리 시간 (ms, AI 응답인 경우)
  private Integer processingTimeMs;
}