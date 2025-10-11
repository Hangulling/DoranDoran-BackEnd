package com.dorandoran.store.dto.request;

import com.dorandoran.store.dto.AiResponse;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 표현 보관 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookmarkRequest {

  // 메시지 ID (필수)
  @NotNull(message = "메시지 ID는 필수입니다")
  private UUID messageId;

  // 채팅방 ID (필수)
  @NotNull(message = "채팅방 ID는 필수입니다")
  private UUID chatroomId;

  // 표현 원본 (필수)
  @NotNull(message = "표현 내용은 필수입니다")
  private String content;

  // Multi-Agent AI 응답 (필수)
  @NotNull(message = "AI 응답은 필수입니다")
  private AiResponse aiResponse;

  // 챗봇 역할 (Honey, Coworker, Senior, Client)
  private String botType;
}