package com.dorandoran.user.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 채팅 로그 리스트 응답 DTO
 */
@Getter
@AllArgsConstructor
public class ChatLogListResponse {

  // 채팅방 ID
  private UUID chatroomId;

  // 채팅방 이름
  private String chatroomName;

  // 챗봇 컨셉
  private String concept;

  // 친밀도 레벨 (스냅샷)
  private Integer intimacyLevel;

  // 마지막 메시지 시간
  private LocalDateTime lastMessageAt;

  // 메시지 개수
  private Long messageCount;

  // 사용자 이메일 (스냅샷)
  private String userEmail;
}