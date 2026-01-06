package com.dorandoran.user.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

/**
 * 채팅룸 옵션 응답 (드롭다운용)
 */
@Getter
@AllArgsConstructor
public class ChatroomOptionResponse {

  private UUID id;
  private String name;
  private String concept;
  private String userEmail;
}