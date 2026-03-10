package com.dorandoran.user.admin.service;

import com.dorandoran.user.admin.client.ChatServiceClient;
import com.dorandoran.user.admin.dto.request.ChatLogSearchRequest;
import com.dorandoran.user.admin.dto.response.AgentResultResponse;
import com.dorandoran.user.admin.dto.response.ChatLogListResponse;
import com.dorandoran.user.admin.dto.response.ChatroomOptionResponse;
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
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
  private final ChatServiceClient chatServiceClient;

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
        new IntimacyLevelOptionResponse(1, "Level 1"),
//        new IntimacyLevelOptionResponse(2, "Level 2 - 표준 존댓말 / 편한 관계"),
        new IntimacyLevelOptionResponse(3, "Level 3")
    );
  }

  // 채팅 로그 리스트 검색 (검색 조건 + 페이징)
  public Page<ChatLogListResponse> searchChatLogs(ChatLogSearchRequest request) {
    if ("chat".equalsIgnoreCase(request.getDataSource())) {
      return searchChatLogsFromChatService(request);
    }
    return searchChatLogsFromArchive(request);
  }

  private Page<ChatLogListResponse> searchChatLogsFromArchive(ChatLogSearchRequest request) {
    Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
    LocalDateTime startDateTime = request.getStartDate().atStartOfDay();
    LocalDateTime endDateTime = request.getEndDate() != null
        ? request.getEndDate().atTime(23, 59, 59)
        : LocalDate.now().atTime(23, 59, 59);

    // Repository 쿼리 실행
    return archChatroomRepository.searchChatLogs(
        request.getConcept(),
        request.getIntimacyLevel(),
        startDateTime,
        endDateTime,
        pageable
    );
  }

  @SuppressWarnings("unchecked")
  private Page<ChatLogListResponse> searchChatLogsFromChatService(ChatLogSearchRequest request) {
    String from = request.getStartDate().atStartOfDay().toString();
    LocalDate endDateOrNow = request.getEndDate() != null ? request.getEndDate() : LocalDate.now();
    String to = endDateOrNow.atTime(23, 59, 59).toString();

    Map<String, Object> raw = chatServiceClient.getAdminConversations(
        null, null, from, to, request.getConcept(),
        request.getIntimacyLevel(), "chat",
        request.getPage(), request.getSize()
    );

    if (raw == null) {
      return Page.empty(PageRequest.of(request.getPage(), request.getSize()));
    }

    List<Map<String, Object>> content =
        (List<Map<String, Object>>) raw.getOrDefault("content", List.of());

    long totalElements = 0;
    Map<String, Object> pageInfo = (Map<String, Object>) raw.get("page");
    if (pageInfo != null) {
      Object te = pageInfo.get("totalElements");
      totalElements = te instanceof Number ? ((Number) te).longValue() : 0;
    }

    List<ChatLogListResponse> items = content.stream()
        .map(this::convertToChatLogListResponse)
        .collect(Collectors.toList());

    return new PageImpl<>(items, PageRequest.of(request.getPage(), request.getSize()), totalElements);
  }

  @SuppressWarnings("unchecked")
  private ChatLogListResponse convertToChatLogListResponse(Map<String, Object> item) {
    UUID chatroomId = null;
    Object cidObj = item.get("conversationId");
    if (cidObj instanceof String) {
      try { chatroomId = UUID.fromString((String) cidObj); } catch (Exception ignored) {}
    }

    String roomKey = (String) item.get("roomKey");
    Integer intimacyLevel = item.get("intimacyLevel") instanceof Number
        ? ((Number) item.get("intimacyLevel")).intValue() : null;
    LocalDateTime lastMessageAt = parseDateTime(item.get("lastMessageAt"));
    Long messageCount = item.get("lastSequenceNumber") instanceof Number
        ? ((Number) item.get("lastSequenceNumber")).longValue() : null;

    return new ChatLogListResponse(chatroomId, roomKey, null, intimacyLevel, lastMessageAt, messageCount, null);
  }

  /**
   * 특정 채팅방의 메시지 타임라인 조회 (Agent 결과 포함)
   *
   * @param chatroomId 채팅방 ID
   * @param page 페이지 번호 (0부터 시작)
   * @param size 페이지 크기
   * @param dataSource 데이터 소스 (archive 또는 chat)
   * @return 메시지 타임라인 페이지
   */
  public Page<MessageTimelineResponse> getMessageTimeline(UUID chatroomId, int page, int size, String dataSource) {
    if ("chat".equalsIgnoreCase(dataSource)) {
      return getMessageTimelineFromChatService(chatroomId, page, size);
    }
    return getMessageTimelineFromArchive(chatroomId, page, size);
  }

  private Page<MessageTimelineResponse> getMessageTimelineFromArchive(UUID chatroomId, int page, int size) {
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

  @SuppressWarnings("unchecked")
  private Page<MessageTimelineResponse> getMessageTimelineFromChatService(UUID chatroomId, int page, int size) {
    Map<String, Object> raw = chatServiceClient.getAdminConversationDetail(chatroomId, "chat");

    if (raw == null) {
      return Page.empty(PageRequest.of(page, size));
    }

    List<Map<String, Object>> timeline =
        (List<Map<String, Object>>) raw.getOrDefault("timeline", List.of());

    List<MessageTimelineResponse> allMessages = timeline.stream()
        .map(this::convertToMessageTimelineResponse)
        .collect(Collectors.toList());

    int start = page * size;
    if (start >= allMessages.size()) {
      return new PageImpl<>(List.of(), PageRequest.of(page, size), allMessages.size());
    }
    int end = Math.min(start + size, allMessages.size());
    return new PageImpl<>(allMessages.subList(start, end), PageRequest.of(page, size), allMessages.size());
  }

  @SuppressWarnings("unchecked")
  private MessageTimelineResponse convertToMessageTimelineResponse(Map<String, Object> msg) {
    UUID messageId = null;
    Object midObj = msg.get("messageId");
    if (midObj instanceof String) {
      try { messageId = UUID.fromString((String) midObj); } catch (Exception ignored) {}
    }

    Long sequenceNumber = msg.get("sequenceNumber") instanceof Number
        ? ((Number) msg.get("sequenceNumber")).longValue() : null;
    Long turnNumber = msg.get("turnNumber") instanceof Number
        ? ((Number) msg.get("turnNumber")).longValue() : null;

    return MessageTimelineResponse.builder()
        .messageId(messageId)
        .content((String) msg.get("content"))
        .senderType((String) msg.get("senderType"))
        .sequenceNumber(sequenceNumber)
        .turnNumber(turnNumber)
        .sourceCreatedAt(parseDateTime(msg.get("createdAt")))
        .tokenCount(null)
        .processingTimeMs(null)
        .agentResults(null)
        .build();
  }

  /**
   * Object를 LocalDateTime으로 변환 (String ISO 형식 또는 Jackson 배열 형식 모두 처리)
   */
  @SuppressWarnings("unchecked")
  private LocalDateTime parseDateTime(Object value) {
    if (value == null) return null;
    if (value instanceof String) {
      String str = (String) value;
      try {
        return LocalDateTime.parse(str);
      } catch (Exception ignored) {}
      try {
        return OffsetDateTime.parse(str).toLocalDateTime();
      } catch (Exception ignored) {}
      return null;
    }
    if (value instanceof List) {
      List<?> arr = (List<?>) value;
      if (arr.size() >= 6) {
        try {
          return LocalDateTime.of(
              ((Number) arr.get(0)).intValue(),
              ((Number) arr.get(1)).intValue(),
              ((Number) arr.get(2)).intValue(),
              ((Number) arr.get(3)).intValue(),
              ((Number) arr.get(4)).intValue(),
              ((Number) arr.get(5)).intValue()
          );
        } catch (Exception ignored) {}
      }
    }
    return null;
  }

  /**
   * Agent 결과 리스트를 AgentResultResponse로 변환
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
          case "conver":
            builder.conversation(parseConversationResult(resultData));
            break;
          case "voca":
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

}