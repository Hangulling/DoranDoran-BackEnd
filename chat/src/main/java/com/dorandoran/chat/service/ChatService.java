package com.dorandoran.chat.service;

import com.dorandoran.chat.entity.ChatRoom;
import com.dorandoran.chat.entity.Message;
import com.dorandoran.chat.entity.User;
import com.dorandoran.chat.entity.Chatbot;
import com.dorandoran.chat.entity.IntimacyProgress;
import com.dorandoran.chat.repository.ChatRoomRepository;
import com.dorandoran.chat.repository.MessageRepository;
import com.dorandoran.chat.repository.UserRepository;
import com.dorandoran.chat.repository.ChatbotRepository;
import com.dorandoran.chat.repository.IntimacyProgressRepository;
import com.dorandoran.chat.repository.UserChatbotLastInteractionRepository;
import jakarta.transaction.Transactional;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.dorandoran.chat.service.dto.LastInteractionResponse;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

/**
 * Chat Service 비즈니스 로직 (단순화 스키마 기반)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ChatbotRepository chatbotRepository;
    private final IntimacyProgressRepository intimacyProgressRepository;
    private final UserChatbotLastInteractionRepository userChatbotLastInteractionRepository;
    private final ObjectMapper objectMapper;
    // AI 트리거는 컨트롤러에서 수행하여 순환 의존 제거

    /**
     * 채팅방 조회 또는 생성 (userId + chatbotId 조합)
     */
    @Transactional
    public ChatRoom getOrCreateRoom(UUID userId, UUID chatbotId, String name) {
        return getOrCreateRoom(userId, chatbotId, name, "FRIEND", 1);
    }
    
    /**
     * 채팅방 조회 또는 생성 (컨셉과 친밀도 포함)
     */
    @Transactional
    public ChatRoom getOrCreateRoom(UUID userId, UUID chatbotId, String name, String concept, Integer intimacyLevel) {
        return getOrCreateRoom(userId, chatbotId, name, concept, intimacyLevel, null);
    }
    
    /**
     * 채팅방 조회 또는 생성 (컨셉과 친밀도 포함, 테스트 모델 포함)
     */
    @Transactional
    public ChatRoom getOrCreateRoom(UUID userId, UUID chatbotId, String name, String concept, Integer intimacyLevel, String testModel) {
        // 삭제되지 않은 활성 채팅방만 조회
        Optional<ChatRoom> existing = chatRoomRepository.findByUser_IdAndChatbot_IdAndIsDeletedFalse(userId, chatbotId);
        if (existing.isPresent()) {
            ChatRoom room = existing.get();
            
            // 기존 채팅방의 concept과 intimacyLevel 업데이트
            updateRoomSettings(room, concept, testModel);
            updateIntimacyLevel(room.getId(), userId, intimacyLevel);
            return room;
        }

        // User와 Chatbot 객체 조회
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        Chatbot chatbot = chatbotRepository.findById(chatbotId)
            .orElseThrow(() -> new RuntimeException("Chatbot not found: " + chatbotId));
        
        // UUID 충돌 방지: 기존 레코드와 겹치지 않을 때까지 생성
        UUID roomId;
        do {
            roomId = UUID.randomUUID();
        } while (chatRoomRepository.findById(roomId).isPresent());

        // settings에 concept과 testModel 저장
        ObjectNode settings = objectMapper.createObjectNode();
        settings.put("concept", concept);
        if (testModel != null && !testModel.isBlank()) {
            settings.put("testModel", testModel);
        }

        ChatRoom room = ChatRoom.builder()
            .id(roomId)
            .user(user)
            .chatbot(chatbot)
            .name(name)
            .settings(settings)
            .isArchived(false)
            .isDeleted(false)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        
        ChatRoom savedRoom = chatRoomRepository.save(room);
        
        // IntimacyProgress 초기화
        initializeIntimacyProgress(savedRoom.getId(), userId, intimacyLevel);
        
        return savedRoom;
    }

    /**
     * 딥링크 전용: 항상 새 채팅방을 만들고 topic 정보를 settings에 포함한다.
     * intimacyLevel 이 null이면 친밀도는 나중에 start-greeting API에서 설정한다.
     */
    @Transactional
    public ChatRoom createRoomWithTopic(UUID userId,
                                        UUID chatbotId,
                                        String name,
                                        String concept,
                                        Integer intimacyLevel,
                                        String topic) {
        ChatRoom savedRoom = createRoomInternal(userId, chatbotId, name, concept, topic, null);
        if (intimacyLevel != null) {
            initializeIntimacyProgress(savedRoom.getId(), userId, intimacyLevel);
        }
        return savedRoom;
    }

    /**
     * 채팅방 진입 표준 로직:
     * 기존 활성방이 있으면 아카이브 + 소프트삭제 후 신규 채팅방을 생성한다.
     */
    @Transactional
    public ChatRoom recreateRoom(UUID userId,
                                 UUID chatbotId,
                                 String name,
                                 String concept,
                                 Integer intimacyLevel,
                                 String topic,
                                 String testModel) {
        archiveAndSoftDeleteActiveRoom(userId, chatbotId);
        try {
            ChatRoom savedRoom = createRoomInternal(userId, chatbotId, name, concept, topic, testModel);
            if (intimacyLevel != null) {
                initializeIntimacyProgress(savedRoom.getId(), userId, intimacyLevel);
            }
            return savedRoom;
        } catch (DataIntegrityViolationException e) {
            if (!isDuplicateChatroomConstraint(e)) {
                throw e;
            }
            log.warn("중복 채팅방 생성 충돌 감지, 재시도 수행: userId={}, chatbotId={}", userId, chatbotId);
            archiveAndSoftDeleteActiveRoom(userId, chatbotId);
            ChatRoom retryRoom = createRoomInternal(userId, chatbotId, name, concept, topic, testModel);
            if (intimacyLevel != null) {
                initializeIntimacyProgress(retryRoom.getId(), userId, intimacyLevel);
            }
            return retryRoom;
        }
    }

    /**
     * 다음 시퀀스 번호 계산 (채팅방 내 최대값 + 1)
     */
    @Transactional
    public long nextSequenceNumber(UUID chatroomId) {
        return messageRepository.findTopByChatRoomIdOrderBySequenceNumberDesc(chatroomId)
            .map(m -> m.getSequenceNumber() + 1)
            .orElse(1L);
    }

    /**
     * 다음 턴 번호 계산 (채팅방 내 최대값 + 1)
     */
    @Transactional
    public long nextTurnNumber(UUID chatroomId) {
        return messageRepository.findTopByChatRoomIdOrderByTurnNumberDesc(chatroomId)
            .map(m -> m.getTurnNumber() + 1)
            .orElse(1L);
    }

    /**
     * 현재 진행 중인 턴 번호 조회 (가장 최근 bot 메시지의 turn_number)
     * 진행 중인 턴이 없으면 null 반환
     */
    @Transactional
    public Long getCurrentTurnNumber(UUID chatroomId) {
        Optional<Message> latestBotMessage = messageRepository
            .findTopByChatRoomIdAndSenderTypeOrderBySequenceNumberDesc(chatroomId, "bot");
        return latestBotMessage.map(Message::getTurnNumber).orElse(null);
    }

    /**
     * User 메시지 수신 시 이전 턴 완료 처리
     * 이전 턴이 미완료(0)인 경우 정상 번호로 업데이트
     */
    @Transactional
    public void updatePreviousTurnOnUserMessage(UUID chatroomId) {
        // 진행 중인 턴(turn_number = 0) 찾기
        List<Message> incompleteTurnMessages = messageRepository
            .findByChatRoomIdAndTurnNumber(chatroomId, 0L);
        
        if (!incompleteTurnMessages.isEmpty()) {
            // 이전 턴의 정상 번호 계산 (현재 최대 턴 번호)
            long previousTurnNumber = messageRepository
                .findTopByChatRoomIdOrderByTurnNumberDesc(chatroomId)
                .map(m -> {
                    // turn_number가 0이 아닌 최대값 찾기
                    if (m.getTurnNumber() > 0) {
                        return m.getTurnNumber();
                    }
                    // 모든 메시지가 0이면 1로 시작
                    return 0L;
                })
                .orElse(0L);
            
            // 이전 턴 번호가 0이면 1로 설정, 아니면 +1
            long newTurnNumber = previousTurnNumber > 0 ? previousTurnNumber : 1L;
            
            // 미완료 턴의 모든 메시지 업데이트
            for (Message msg : incompleteTurnMessages) {
                msg.setTurnNumber(newTurnNumber);
                messageRepository.save(msg);
            }
            
            log.debug("이전 턴 완료 처리: chatroomId={}, turnNumber={}, updatedMessages={}", 
                chatroomId, newTurnNumber, incompleteTurnMessages.size());
        }
    }

    /**
     * 메시지 전송: 저장 후 룸의 last_message_* 업데이트
     */
    @Transactional
    public Message sendMessage(UUID chatroomId, UUID senderId, String senderType, String content, String contentType) {
        return sendMessage(chatroomId, senderId, senderType, content, contentType, null);
    }

    /**
     * 메시지 전송 (metadata 포함): 저장 후 룸의 last_message_* 업데이트
     */
    @Transactional
    public Message sendMessage(UUID chatroomId, UUID senderId, String senderType, String content, String contentType, String metadata) {
        long seq = nextSequenceNumber(chatroomId);
        // ChatRoom 객체 조회
        ChatRoom chatRoom = chatRoomRepository.findById(chatroomId)
            .orElseThrow(() -> new RuntimeException("ChatRoom not found: " + chatroomId));
        
        // turn_number 결정 로직
        long turnNumber;
        if ("bot".equalsIgnoreCase(senderType)) {
            // Bot 메시지: 새로운 턴 시작
            turnNumber = nextTurnNumber(chatroomId);
        } else if ("system".equalsIgnoreCase(senderType)) {
            // System 메시지: 가장 최근 Bot 메시지의 턴 번호 사용
            Long currentTurn = getCurrentTurnNumber(chatroomId);
            if (currentTurn != null && currentTurn > 0) {
                // 진행 중인 턴이 있으면 그 턴 사용
                turnNumber = currentTurn;
            } else {
                // 진행 중인 턴이 없으면 새로운 턴 시작 (인사말 등 초기 시스템 메시지)
                turnNumber = nextTurnNumber(chatroomId);
            }
        } else {
            // User 메시지: 이전 턴 완료 처리 후, turn_number는 0으로 설정 (Bot 응답이 턴의 시작점)
            updatePreviousTurnOnUserMessage(chatroomId);
            turnNumber = 0L; // User 메시지는 턴의 시작점이 아니므로 0으로 설정
        }
        
        LocalDateTime now = LocalDateTime.now();
        Message message = Message.builder()
            .id(UUID.randomUUID())
            .chatRoom(chatRoom)
            .senderType(senderType)
            .senderId(senderId)
            .content(content)
            .contentType(contentType)
            .metadata(metadata)
            .sequenceNumber(seq)
            .turnNumber(turnNumber)
            .isDeleted(false)
            .isEdited(false)
            .isCancelled(false)
            .createdAt(now)
            .updatedAt(now)
            .build();
        Message saved = messageRepository.save(message);

        chatRoomRepository.findById(chatroomId).ifPresent(room -> {
            room.setLastMessageAt(LocalDateTime.now());
            room.setLastMessage(saved);
            room.setUpdatedAt(LocalDateTime.now());
            chatRoomRepository.save(room);
        });

        // 사용자 메시지인 경우 user_chatbot_last_interaction 업데이트
        if ("user".equalsIgnoreCase(senderType)) {
            try {
                UUID userId = chatRoom.getUser().getId();
                UUID chatbotId = chatRoom.getChatbot().getId();
                OffsetDateTime interactionTime = OffsetDateTime.now();
                
                userChatbotLastInteractionRepository.upsert(
                    userId,
                    chatbotId,
                    interactionTime,
                    chatroomId
                );
                log.debug("Updated user_chatbot_last_interaction: userId={}, chatbotId={}, chatroomId={}, time={}", 
                    userId, chatbotId, chatroomId, interactionTime);
            } catch (Exception e) {
                // 로그만 남기고 메시지 저장은 계속 진행
                log.error("Failed to update user_chatbot_last_interaction: chatroomId={}, senderId={}", 
                    chatroomId, senderId, e);
            }
        }

        return saved;
    }

    /**
     * 메시지 취소 처리
     */
    @Transactional
    public Message cancelMessage(UUID messageId, UUID userId) {
        Message message = messageRepository.findById(messageId)
            .orElseThrow(() -> new RuntimeException("Message not found: " + messageId));
        ChatRoom room = message.getChatRoom();
        if (!chatRoomRepository.existsByUserIdAndIdAndIsDeletedFalse(userId, room.getId())) {
            throw new RuntimeException("Access denied or room deleted: " + room.getId());
        }
        if (Boolean.TRUE.equals(message.getIsCancelled())) {
            return message;
        }
        message.setIsCancelled(true);
        message.setCancelledAt(LocalDateTime.now());
        return messageRepository.save(message);
    }

    /**
     * 메시지 취소 여부 확인
     */
    @Transactional
    public boolean isMessageCancelled(UUID messageId) {
        return messageRepository.findById(messageId)
            .map(m -> Boolean.TRUE.equals(m.getIsCancelled()))
            .orElse(false);
    }

    /**
     * 사용자별 채팅방 목록 조회 (삭제되지 않은, 최신 메시지 순) - 페이징
     */
    @Transactional
    public Page<ChatRoom> listRooms(UUID userId, Pageable pageable) {
        return listRooms(userId, pageable, null);
    }
    
    /**
     * 사용자별 채팅방 목록 조회 (삭제되지 않은, 최신 메시지 순) - 페이징 (테스트 모델 필터 포함)
     */
    @Transactional
    public Page<ChatRoom> listRooms(UUID userId, Pageable pageable, String testModel) {
        if (testModel != null && !testModel.isBlank()) {
            // 테스트 모델 필터 적용
            return chatRoomRepository.findByUser_IdAndIsDeletedFalseAndTestModelOrderByLastMessageAtDesc(userId, testModel, pageable);
        }
        return chatRoomRepository.findByUser_IdAndIsDeletedFalseOrderByLastMessageAtDesc(userId, pageable);
    }

    /**
     * 사용자별 채팅방 목록 조회 (삭제되지 않은, 최신 메시지 순) - 전체
     */
    @Transactional
    public List<ChatRoom> listRooms(UUID userId) {
        return chatRoomRepository.findByUser_IdAndIsDeletedFalseOrderByLastMessageAtDesc(userId);
    }

    /**
     * 채팅방 메시지 목록 조회 (시퀀스 오름차순) - 페이징
     */
    @Transactional
    public Page<Message> listMessages(UUID chatroomId, Pageable pageable) {
        return messageRepository.findByChatRoomIdOrderBySequenceNumberAsc(chatroomId, pageable);
    }

    /**
     * 채팅방 메시지 목록 조회 (시퀀스 오름차순) - 전체
     */
    @Transactional
    public List<Message> listMessages(UUID chatroomId) {
        return messageRepository.findByChatRoomIdOrderBySequenceNumberAsc(chatroomId);
    }

    /**
     * 채팅방 조회 (ID로)
     */
    @Transactional
    public ChatRoom getChatRoomById(UUID chatroomId) {
        return chatRoomRepository.findById(chatroomId)
            .orElseThrow(() -> new RuntimeException("ChatRoom not found: " + chatroomId));
    }

    /**
     * 채팅방 수정 (이름/설명/아카이브)
     */
    @Transactional
    public ChatRoom updateRoom(UUID chatroomId, UUID userId, String name, String description, Boolean archived) {
        ChatRoom room = getChatRoomById(chatroomId);
        if (!chatRoomRepository.existsByUserIdAndIdAndIsDeletedFalse(userId, chatroomId)) {
            throw new RuntimeException("Access denied or room deleted: " + chatroomId);
        }
        if (name != null && !name.isBlank()) {
            room.setName(name);
        }
        if (description != null) {
            room.setDescription(description);
        }
        if (archived != null) {
            room.setIsArchived(archived);
        }
        room.setUpdatedAt(java.time.LocalDateTime.now());
        return chatRoomRepository.save(room);
    }

    /**
     * 채팅방 소프트 삭제
     */
    @Transactional
    public void softDeleteRoom(UUID chatroomId, UUID userId) {
        ChatRoom room = getChatRoomById(chatroomId);
        if (!chatRoomRepository.existsByUserIdAndIdAndIsDeletedFalse(userId, chatroomId)) {
            throw new RuntimeException("Access denied or room already deleted: " + chatroomId);
        }
        room.setIsDeleted(true);
        room.setUpdatedAt(java.time.LocalDateTime.now());
        chatRoomRepository.save(room);
    }

    /**
     * 코치마크 표시 여부 조회 (room.settings.coachmarkShown)
     */
    @Transactional
    public boolean isCoachmarkShown(UUID chatroomId, UUID userId) {
        if (!chatRoomRepository.existsByUserIdAndIdAndIsDeletedFalse(userId, chatroomId)) {
            throw new RuntimeException("Access denied or room deleted: " + chatroomId);
        }
        ChatRoom room = getChatRoomById(chatroomId);
        JsonNode settings = room.getSettings();
        if (settings != null && settings.has("coachmarkShown")) {
            return settings.get("coachmarkShown").asBoolean(false);
        }
        return false;
    }

    /**
     * 코치마크 표시 완료로 설정 (room.settings.coachmarkShown=true)
     */
    @Transactional
    public ChatRoom setCoachmarkShown(UUID chatroomId, UUID userId, boolean shown) {
        if (!chatRoomRepository.existsByUserIdAndIdAndIsDeletedFalse(userId, chatroomId)) {
            throw new RuntimeException("Access denied or room deleted: " + chatroomId);
        }
        ChatRoom room = getChatRoomById(chatroomId);
        ObjectNode settings = room.getSettings() != null && room.getSettings().isObject()
            ? (ObjectNode) room.getSettings()
            : objectMapper.createObjectNode();
        settings.put("coachmarkShown", shown);
        room.setSettings(settings);
        room.setUpdatedAt(java.time.LocalDateTime.now());
        return chatRoomRepository.save(room);
    }
    
    /**
     * 친밀도 레벨 업데이트
     */
    @Transactional
    public void updateIntimacyLevel(UUID chatroomId, UUID userId, int intimacyLevel) {
        if (!chatRoomRepository.existsByUserIdAndIdAndIsDeletedFalse(userId, chatroomId)) {
            throw new RuntimeException("Access denied or room deleted: " + chatroomId);
        }
        
        // 기존 레코드 찾기
        Optional<IntimacyProgress> existingProgress = intimacyProgressRepository.findByChatRoomId(chatroomId);
        
        if (existingProgress.isPresent()) {
            // 기존 레코드 업데이트
            IntimacyProgress progress = existingProgress.get();
            progress.setIntimacyLevel(intimacyLevel);
            progress.setLastUpdated(LocalDateTime.now());
            intimacyProgressRepository.save(progress);
        } else {
            // 새 레코드 생성
            ChatRoom chatRoom = getChatRoomById(chatroomId);
            IntimacyProgress progress = IntimacyProgress.builder()
                .id(UUID.randomUUID())
                .chatRoom(chatRoom)
                .userId(userId)
                .intimacyLevel(intimacyLevel)
                .totalCorrections(0)
                .build();
            intimacyProgressRepository.save(progress);
        }
    }
    
    /**
     * IntimacyProgress 초기화
     */
    private void initializeIntimacyProgress(UUID chatroomId, UUID userId, int intimacyLevel) {
        // 이미 존재하면 업데이트만 수행하여 중복 삽입 방지 (uq_intimacy_chatroom)
        Optional<IntimacyProgress> existing = intimacyProgressRepository.findByChatRoomId(chatroomId);
        if (existing.isPresent()) {
            IntimacyProgress progress = existing.get();
            progress.setIntimacyLevel(intimacyLevel);
            if (progress.getLastFeedback() == null || progress.getLastFeedback().isBlank()) {
                progress.setLastFeedback("채팅방 생성");
            }
            if (progress.getProgressData() == null || progress.getProgressData().isBlank()) {
                progress.setProgressData("{}");
            }
            progress.setLastUpdated(LocalDateTime.now());
            intimacyProgressRepository.save(progress);
            return;
        }

        IntimacyProgress progress = IntimacyProgress.builder()
            .id(UUID.randomUUID())
            .chatRoom(getChatRoomById(chatroomId))
            .userId(userId)
            .intimacyLevel(intimacyLevel)
            .totalCorrections(0)
            .lastFeedback("채팅방 생성")
            .lastUpdated(LocalDateTime.now())
            .progressData("{}")
            .build();
        
        intimacyProgressRepository.save(progress);
    }
    
    /**
     * 채팅방의 컨셉과 친밀도 레벨 조회
     */
    public String getConcept(UUID chatroomId) {
        ChatRoom room = getChatRoomById(chatroomId);
        JsonNode settings = room.getSettings();
        if (settings != null && settings.has("concept")) {
            return settings.get("concept").asText();
        }
        return "FRIEND"; // 기본값
    }
    
    public Integer getIntimacyLevel(UUID chatroomId) {
        return intimacyProgressRepository.findByChatRoomId(chatroomId)
            .map(IntimacyProgress::getIntimacyLevel)
            .orElse(1); // 기본값
    }
    
    /**
     * 채팅방 settings에 concept 저장
     */
    private void updateRoomSettings(ChatRoom room, String concept) {
        updateRoomSettings(room, concept, null);
    }
    
    /**
     * 채팅방 settings에 concept과 testModel 저장
     */
    private void updateRoomSettings(ChatRoom room, String concept, String testModel) {
        ObjectNode settings = room.getSettings() != null && room.getSettings().isObject()
            ? (ObjectNode) room.getSettings()
            : objectMapper.createObjectNode();
        settings.put("concept", concept);
        if (testModel != null && !testModel.isBlank()) {
            settings.put("testModel", testModel);
        }
        room.setSettings(settings);
        room.setUpdatedAt(LocalDateTime.now());
        chatRoomRepository.save(room);
    }

    private ChatRoom createRoomInternal(UUID userId,
                                        UUID chatbotId,
                                        String name,
                                        String concept,
                                        String topic,
                                        String testModel) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        Chatbot chatbot = chatbotRepository.findById(chatbotId)
            .orElseThrow(() -> new RuntimeException("Chatbot not found: " + chatbotId));

        UUID roomId;
        do {
            roomId = UUID.randomUUID();
        } while (chatRoomRepository.findById(roomId).isPresent());

        ObjectNode settings = objectMapper.createObjectNode();
        settings.put("concept", concept);
        if (topic != null && !topic.isBlank()) {
            settings.put("topic", topic);
        }
        if (testModel != null && !testModel.isBlank()) {
            settings.put("testModel", testModel);
        }

        ChatRoom room = ChatRoom.builder()
            .id(roomId)
            .user(user)
            .chatbot(chatbot)
            .name(name)
            .settings(settings)
            .isArchived(false)
            .isDeleted(false)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        return chatRoomRepository.save(room);
    }

    private void archiveAndSoftDeleteActiveRoom(UUID userId, UUID chatbotId) {
        chatRoomRepository.findByUser_IdAndChatbot_IdAndIsDeletedFalse(userId, chatbotId)
            .ifPresent(room -> {
                room.setIsArchived(true);
                room.setIsDeleted(true);
                room.setUpdatedAt(LocalDateTime.now());
                chatRoomRepository.save(room);
                // partial unique index(user_id, chatbot_id where not is_deleted) 충돌 방지를 위해 즉시 반영
                chatRoomRepository.flush();
                log.info("기존 활성 채팅방 아카이브+삭제 처리: userId={}, chatbotId={}, roomId={}", userId, chatbotId, room.getId());
            });
    }

    private boolean isDuplicateChatroomConstraint(DataIntegrityViolationException e) {
        Throwable current = e;
        while (current != null) {
            String msg = current.getMessage();
            if (msg != null && msg.contains("idx_chatrooms_user_chatbot")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    /**
     * 이메일로 사용자 조회
     */
    public com.dorandoran.chat.entity.User findUserByEmail(String email) {
        // user_schema.app_user에서 조회
        return userRepository.findByEmail(email).orElse(null);
    }

    /**
     * concept별 챗봇 UUID 매핑
     */
    public UUID getChatbotIdByConcept(String concept) {
        switch (concept.toUpperCase()) {
            case "FRIEND":
                return UUID.fromString("22222222-2222-2222-2222-222222222221");
            case "HONEY":
                return UUID.fromString("22222222-2222-2222-2222-222222222222");
            case "COWORKER":
                return UUID.fromString("22222222-2222-2222-2222-222222222223");
            case "SENIOR":
                return UUID.fromString("22222222-2222-2222-2222-222222222224");
            case "BOSS":
                return UUID.fromString("22222222-2222-2222-2222-222222222225");
            default:
                return UUID.fromString("22222222-2222-2222-2222-222222222221"); // 기본값: FRIEND
        }
    }

    /**
     * 챗봇별 마지막 상호작용 상위 N 조회
     * 방 삭제 여부와 무관하게 사용자-챗봇별 마지막 대화 시간을 반환합니다.
     */
    @Transactional
    public List<LastInteractionResponse> listLastInteractionsByChatbot(UUID userId, int limit) {
        return listLastInteractionsByChatbot(userId, limit, null);
    }
    
    public List<LastInteractionResponse> listLastInteractionsByChatbot(UUID userId, int limit, String testModel) {
        log.info("=== ChatService.listLastInteractionsByChatbot 시작: userId={}, limit={}, testModel={} ===", userId, limit, testModel);
        
        List<Object[]> results = userChatbotLastInteractionRepository.findTopByUserOrder(userId, limit);
        log.debug("DB 조회 결과: {}건", results != null ? results.size() : 0);
        
        if (results == null) {
            log.warn("DB 조회 결과가 null입니다. 빈 리스트 반환");
            results = new ArrayList<>();
        }
        
        List<LastInteractionResponse> responses = new ArrayList<>();
        
        for (Object[] row : results) {
            if (row == null || row.length < 3) {
                log.warn("잘못된 DB 결과 행: row={}", row);
                continue;
            }
            
            UUID chatbotId = (UUID) row[0];
            OffsetDateTime lastInteractionAt = null;
            
            log.debug("DB 행 처리 시작: chatbotId={}, row[1] 타입={}, row[1] 값={}", 
                chatbotId, row[1] != null ? row[1].getClass().getName() : "null", row[1]);
            
            if (row[1] != null) {
                // PostgreSQL의 TIMESTAMPTZ는 다양한 타입으로 반환될 수 있음
                if (row[1] instanceof java.sql.Timestamp) {
                    lastInteractionAt = ((java.sql.Timestamp) row[1]).toInstant()
                        .atOffset(java.time.ZoneOffset.UTC);
                    log.debug("Timestamp를 OffsetDateTime으로 변환: {}", lastInteractionAt);
                } else if (row[1] instanceof OffsetDateTime) {
                    lastInteractionAt = (OffsetDateTime) row[1];
                    log.debug("OffsetDateTime 직접 사용: {}", lastInteractionAt);
                } else if (row[1] instanceof java.time.Instant) {
                    lastInteractionAt = ((java.time.Instant) row[1]).atOffset(java.time.ZoneOffset.UTC);
                    log.debug("Instant를 OffsetDateTime으로 변환: {}", lastInteractionAt);
                } else {
                    log.warn("예상치 못한 타입: chatbotId={}, 타입={}, 값={}", 
                        chatbotId, row[1].getClass().getName(), row[1]);
                }
            } else {
                log.debug("lastInteractionAt이 null: chatbotId={}", chatbotId);
            }
            
            UUID lastRoomId = (UUID) row[2];
            
            // testModel 필터 적용: lastRoomId로 채팅방 조회하여 testModel 확인
            if (testModel != null && !testModel.isBlank() && lastRoomId != null) {
                log.debug("testModel 필터 적용: testModel={}, lastRoomId={}", testModel, lastRoomId);
                Optional<ChatRoom> roomOpt = chatRoomRepository.findById(lastRoomId);
                if (roomOpt.isPresent()) {
                    ChatRoom room = roomOpt.get();
                    JsonNode settings = room.getSettings();
                    String roomTestModel = (settings != null && settings.has("testModel")) 
                        ? settings.get("testModel").asText() 
                        : null;
                    log.debug("채팅방 testModel: roomTestModel={}, 요청 testModel={}", roomTestModel, testModel);
                    // testModel이 일치하지 않으면 스킵
                    if (!testModel.equals(roomTestModel)) {
                        log.debug("testModel 불일치로 스킵: chatbotId={}", chatbotId);
                        continue;
                    }
                } else {
                    log.debug("채팅방 없음으로 스킵: lastRoomId={}", lastRoomId);
                    continue;
                }
            }
            
            // 챗봇 이름 조회
            String chatbotName = null;
            Optional<Chatbot> chatbot = chatbotRepository.findById(chatbotId);
            if (chatbot.isPresent()) {
                chatbotName = chatbot.get().getName();
                log.debug("챗봇 이름 조회: chatbotId={}, chatbotName={}", chatbotId, chatbotName);
            } else {
                log.warn("챗봇을 찾을 수 없음: chatbotId={}", chatbotId);
            }
            
            LastInteractionResponse response = new LastInteractionResponse();
            response.setChatbotId(chatbotId);
            response.setLastRoomId(lastRoomId);
            response.setLastInteractionAt(lastInteractionAt);
            response.setChatbotName(chatbotName);
            responses.add(response);
            
            log.debug("응답 객체 생성 완료: chatbotId={}, chatbotName={}, lastRoomId={}, lastInteractionAt={}", 
                chatbotId, chatbotName, lastRoomId, lastInteractionAt);
        }
        
        // testModel 필터 적용 후에도 limit만큼 반환하기 위해 부족분은 채워넣기
        int paddingCount = 0;
        while (responses.size() < limit && responses.size() < 4) {
            responses.add(new LastInteractionResponse());
            paddingCount++;
        }
        if (paddingCount > 0) {
            log.debug("서비스 레이어 패딩 추가: {}개", paddingCount);
        }
        
        log.info("=== ChatService.listLastInteractionsByChatbot 완료: 총 {}건 반환 ===", responses.size());
        return responses;
    }
}
