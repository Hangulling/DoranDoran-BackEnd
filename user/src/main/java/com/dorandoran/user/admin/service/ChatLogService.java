package com.dorandoran.user.admin.service;

import com.dorandoran.user.admin.dto.request.ChatLogSearchRequest;
import com.dorandoran.user.admin.dto.response.ChatLogListResponse;
import com.dorandoran.user.admin.dto.response.ChatroomOptionResponse;
import com.dorandoran.user.admin.dto.response.IntimacyLevelOptionResponse;
import com.dorandoran.user.admin.dto.response.MessageTimelineResponse;
import com.dorandoran.user.admin.entity.ArchMessage;
import com.dorandoran.user.admin.repository.ArchChatroomRepository;
import com.dorandoran.user.admin.repository.ArchMessageRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
  private final ArchMessageRepository archMessageRepository;

  // 채팅룸 드롭다운 옵션 조회
  public List<ChatroomOptionResponse> getChatroomOptions() {
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

  // 친밀도 레벨 옵션 조회
  public List<IntimacyLevelOptionResponse> getIntimacyLevelOptions() {
    return List.of(
        new IntimacyLevelOptionResponse(1, "Level 1 - 격식체 / 첫 만남"),
        new IntimacyLevelOptionResponse(2, "Level 2 - 표준 존댓말 / 편한 관계"),
        new IntimacyLevelOptionResponse(3, "Level 3 - 친근한 반말 / 아주 친한 사이")
    );
  }

  // 채팅 로그 리스트 검색 (검색 조건 + 페이징)
  public Page<ChatLogListResponse> searchChatLogs(ChatLogSearchRequest request) {
    // Pageable 객체 생성
    Pageable pageable = PageRequest.of(request.getPage(), request.getSize());

    // LocalDate를 LocalDateTime으로 변환
    LocalDateTime startDateTime = request.getStartDate().atStartOfDay();

    // endDate가 null이면 현재 날짜의 23:59:59 사용
    LocalDateTime endDateTime = request.getEndDate() != null
        ? request.getEndDate().atTime(23, 59, 59)
        : LocalDate.now().atTime(23, 59, 59);

    // Repository 쿼리 실행
    return archChatroomRepository.searchChatLogs(
        request.getChatroomId(),
        request.getIntimacyLevel(),
        startDateTime,
        endDateTime,
        pageable
    );
  }

  /**
   * 특정 채팅방의 메시지 타임라인 조회
   *
   * @param chatroomId 채팅방 ID
   * @param page 페이지 번호 (0부터 시작)
   * @param size 페이지 크기
   * @return 메시지 타임라인 페이지
   */
  public Page<MessageTimelineResponse> getMessageTimeline(UUID chatroomId, int page, int size) {
    // 페이지 요청 객체 생성
    Pageable pageable = PageRequest.of(page, size);

    // 메시지 조회 (sequenceNumber 오름차순)
    Page<ArchMessage> messagePage = archMessageRepository
        .findByChatroomIdOrderBySequence(chatroomId, pageable);

    // Entity -> DTO 변환
    return messagePage.map(message -> MessageTimelineResponse.builder()
        .messageId(message.getId())
        .content(message.getContent())
        .senderType(message.getSenderType())
        .sequenceNumber(message.getSequenceNumber())
        .turnNumber(message.getTurnNumber())
        .sourceCreatedAt(message.getSourceCreatedAt())
        .tokenCount(message.getTokenCount())
        .processingTimeMs(message.getProcessingTimeMs())
        .build());
  }
}