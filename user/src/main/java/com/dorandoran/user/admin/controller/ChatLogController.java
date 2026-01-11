package com.dorandoran.user.admin.controller;

import com.dorandoran.user.admin.dto.response.ChatroomOptionResponse;
import com.dorandoran.user.admin.service.ChatLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 채팅 로그 조회 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/chat-logs")
@RequiredArgsConstructor
public class ChatLogController {

  private final ChatLogService chatLogService;

  /**
   * 채팅룸 옵션 조회 (드롭다운용)
   *
   * GET /api/admin/chat-logs/chatrooms
   */
  @GetMapping("/chatrooms")
  public ResponseEntity<List<ChatroomOptionResponse>> getChatroomOptions() {
    log.info("채팅룸 옵션 조회 API 호출");

    List<ChatroomOptionResponse> options = chatLogService.getChatroomOptions();

    return ResponseEntity.ok(options);
  }
}