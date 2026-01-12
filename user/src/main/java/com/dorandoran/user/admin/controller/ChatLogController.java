package com.dorandoran.user.admin.controller;

import com.dorandoran.user.admin.dto.request.ChatLogSearchRequest;
import com.dorandoran.user.admin.dto.response.ChatLogListResponse;
import com.dorandoran.user.admin.dto.response.ChatroomOptionResponse;
import com.dorandoran.user.admin.dto.response.IntimacyLevelOptionResponse;
import com.dorandoran.user.admin.service.ChatLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
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
   * GET /api/admin/chat-logs/chatrooms
   */
  @GetMapping("/chatrooms")
  public ResponseEntity<List<ChatroomOptionResponse>> getChatroomOptions() {
    log.info("채팅룸 옵션 조회 API 호출");

    List<ChatroomOptionResponse> options = chatLogService.getChatroomOptions();

    return ResponseEntity.ok(options);
  }

  /**
   * 친밀도 레벨 옵션 조회
   * GET /api/admin/chat-logs/intimacy-levels
   */
  @GetMapping("/intimacy-levels")
  public ResponseEntity<List<IntimacyLevelOptionResponse>> getIntimacyLevelOptions() {
    List<IntimacyLevelOptionResponse> options = chatLogService.getIntimacyLevelOptions();
    return ResponseEntity.ok(options);
  }

  /**
   * 채팅 로그 리스트 검색
   * GET /api/admin/chat-logs/search
   *
   * @param request 검색 조건 (startDate, endDate 필수, chatroomId, intimacyLevel 선택)
   * @return 페이징된 채팅 로그 리스트
   */
  @GetMapping("/search")
  public ResponseEntity<Page<ChatLogListResponse>> searchChatLogs(
      @Validated @ModelAttribute ChatLogSearchRequest request) {
    Page<ChatLogListResponse> result = chatLogService.searchChatLogs(request);
    return ResponseEntity.ok(result);
  }

}