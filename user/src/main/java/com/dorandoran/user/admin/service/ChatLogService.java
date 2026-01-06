package com.dorandoran.user.admin.service;

import com.dorandoran.user.admin.dto.response.ChatroomOptionResponse;
import com.dorandoran.user.admin.repository.ArchChatroomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 채팅 로그 조회 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatLogService {

  private final ArchChatroomRepository archChatroomRepository;

  /**
   * 채팅룸 옵션 조회 (드롭다운용)
   */
  public List<ChatroomOptionResponse> getChatroomOptions() {
    log.debug("채팅룸 옵션 조회 시작");

    return archChatroomRepository.findAll().stream()
        .filter(chatroom -> !chatroom.getIsDeleted())
        .map(chatroom -> new ChatroomOptionResponse(
            chatroom.getId(),
            chatroom.getName(),
            chatroom.getConcept(),
            chatroom.getUserEmailSnapshot()
        ))
        .collect(Collectors.toList());
  }
}