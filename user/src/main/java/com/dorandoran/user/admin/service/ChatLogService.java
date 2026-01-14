package com.dorandoran.user.admin.service;

import com.dorandoran.user.admin.dto.request.ChatLogSearchRequest;
import com.dorandoran.user.admin.dto.request.ExportRequest;
import com.dorandoran.user.admin.dto.response.AgentResultResponse;
import com.dorandoran.user.admin.dto.response.ChatLogListResponse;
import com.dorandoran.user.admin.dto.response.ChatroomOptionResponse;
import com.dorandoran.user.admin.dto.response.ExportResponse;
import com.dorandoran.user.admin.dto.response.IntimacyLevelOptionResponse;
import com.dorandoran.user.admin.dto.response.MessageTimelineResponse;
import com.dorandoran.user.admin.entity.ArchAgentResult;
import com.dorandoran.user.admin.entity.ArchMessage;
import com.dorandoran.user.admin.repository.ArchAgentResultRepository;
import com.dorandoran.user.admin.repository.ArchChatroomRepository;
import com.dorandoran.user.admin.repository.ArchMessageRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
  private final ArchAgentResultRepository archAgentResultRepository;

  private final ObjectMapper objectMapper;

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
   * 특정 채팅방의 메시지 타임라인 조회 (Agent 결과 포함)
   *
   * @param chatroomId 채팅방 ID
   * @param page 페이지 번호 (0부터 시작)
   * @param size 페이지 크기
   * @return 메시지 타임라인 페이지
   */
  public Page<MessageTimelineResponse> getMessageTimeline(UUID chatroomId, int page, int size) {
    Pageable pageable = PageRequest.of(page, size);

    // 1. 메시지 조회
    Page<ArchMessage> messages = archMessageRepository.findByChatroomIdOrderBySequence(chatroomId, pageable);

    // 2. 메시지 ID 추출
    List<UUID> messageIds = messages.getContent().stream()
        .map(ArchMessage::getId)
        .collect(Collectors.toList());

    // 3. Agent 결과 일괄 조회 (N+1 방지)
    List<ArchAgentResult> agentResults = archAgentResultRepository.findByMessageIdIn(messageIds);

    // 4. 메시지 ID별로 Agent 결과 그룹화
    Map<UUID, List<ArchAgentResult>> agentResultsByMessageId = agentResults.stream()
        .collect(Collectors.groupingBy(ArchAgentResult::getArchMessageId));

    // 5. DTO 변환 (Agent 결과 포함)
    return messages.map(message -> {
      MessageTimelineResponse.MessageTimelineResponseBuilder builder = MessageTimelineResponse.builder()
          .messageId(message.getId())
          .content(message.getContent())
          .senderType(message.getSenderType())
          .sequenceNumber(message.getSequenceNumber())
          .turnNumber(message.getTurnNumber())
          .sourceCreatedAt(message.getSourceCreatedAt())
          .tokenCount(message.getTokenCount())
          .processingTimeMs(message.getProcessingTimeMs());

      // 6. USER 메시지인 경우에만 Agent 결과 추가
      if ("USER".equals(message.getSenderType())) {
        List<ArchAgentResult> messageAgentResults = agentResultsByMessageId.get(message.getId());
        if (messageAgentResults != null && !messageAgentResults.isEmpty()) {
          builder.agentResults(buildAgentResultResponse(messageAgentResults));
        }
      }

      return builder.build();
    });
  }

  /**
   * Agent 결과 리스트를 AgentResultResponse로 변환
   *
   * @param agentResults Agent 결과 리스트
   * @return AgentResultResponse
   */
  private AgentResultResponse buildAgentResultResponse(List<ArchAgentResult> agentResults) {
    AgentResultResponse.AgentResultResponseBuilder builder = AgentResultResponse.builder();

    for (ArchAgentResult result : agentResults) {
      String agentType = result.getAgentType();
      String resultData = result.getPayloadJson();

      try {
        switch (agentType) {
          case "intimacy":
            builder.intimacy(parseIntimacyResult(resultData));
            break;
          case "conversation":
            builder.conversation(parseConversationResult(resultData));
            break;
          case "vocabulary":
            builder.vocabulary(parseVocabularyResult(resultData));
            break;
          default:
            log.warn("Unknown agent type: {}", agentType);
        }
      } catch (Exception e) {
        log.error("Failed to parse agent result. type={}, data={}", agentType, resultData, e);
      }
    }

    return builder.build();
  }

  /**
   * Intimacy Agent 결과 파싱
   */
  private AgentResultResponse.IntimacyResult parseIntimacyResult(String jsonData) throws JsonProcessingException {
    Map<String, Object> data = objectMapper.readValue(jsonData, Map.class);

    Map<String, String> feedback = (Map<String, String>) data.get("feedback");

    return AgentResultResponse.IntimacyResult.builder()
        .detectedLevel((Integer) data.get("detected_level"))
        .correctedSentence((String) data.get("corrected_sentence"))
        .corrections((String) data.get("corrections"))
        .feedback(AgentResultResponse.IntimacyResult.Feedback.builder()
            .ko(feedback != null ? feedback.get("ko") : null)
            .en(feedback != null ? feedback.get("en") : null)
            .build())
        .build();
  }

  /**
   * Conversation Agent 결과 파싱
   */
  private AgentResultResponse.ConversationResult parseConversationResult(String jsonData) throws JsonProcessingException {
    Map<String, Object> data = objectMapper.readValue(jsonData, Map.class);

    return AgentResultResponse.ConversationResult.builder()
        .content((String) data.get("content"))
        .build();
  }

  /**
   * Vocabulary Agent 결과 파싱
   */
  private AgentResultResponse.VocabularyResult parseVocabularyResult(String jsonData) throws JsonProcessingException {
    Map<String, Object> data = objectMapper.readValue(jsonData, Map.class);
    List<Map<String, Object>> words = (List<Map<String, Object>>) data.get("words");

    if (words == null) {
      return AgentResultResponse.VocabularyResult.builder()
          .words(Collections.emptyList())
          .build();
    }

    List<AgentResultResponse.VocabularyResult.Word> wordList = words.stream()
        .map(word -> AgentResultResponse.VocabularyResult.Word.builder()
            .word((String) word.get("word"))
            .difficulty((Integer) word.get("difficulty"))
            .context((String) word.get("context"))
            .build())
        .collect(Collectors.toList());

    return AgentResultResponse.VocabularyResult.builder()
        .words(wordList)
        .build();
  }

  /**
   * 채팅 로그 내보내기 요청
   *
   * @param request 내보내기 요청
   * @return 내보내기 응답 (PENDING 상태)
   */
  public ExportResponse requestExport(ExportRequest request) {
    // 1. UUID 생성
    UUID exportId = UUID.randomUUID();

    // 2. 관리 큐에 등록 (Phase 4 Day 2에서 구현)
    // TODO: managementQueueService.enqueueExport(exportId, request);

    // 3. 응답 생성
    return ExportResponse.builder()
        .exportId(exportId)
        .status(ExportResponse.ExportStatus.PENDING)
        .requestedAt(LocalDateTime.now())
        .message("내보내기 요청이 접수되었습니다.")
        .build();
  }

  /**
   * 내보내기 상태 조회
   *
   * @param exportId 내보내기 요청 ID
   * @return 내보내기 상태 정보
   */
  public ExportResponse getExportStatus(UUID exportId) {
    // Phase 4 Day 2에서 ManagementQueue를 통해 실제 상태 조회 구현
    // TODO: ManagementQueue에서 상태 조회

    // 임시 응답 (Day 2에서 실제 구현)
    return ExportResponse.builder()
        .exportId(exportId)
        .status(ExportResponse.ExportStatus.PENDING)
        .requestedAt(LocalDateTime.now())
        .message("내보내기 상태 조회 기능은 Phase 4 Day 2에서 구현됩니다.")
        .build();
  }
}