package com.dorandoran.chat.service;

import com.dorandoran.chat.entity.ChatRoom;
import com.dorandoran.chat.entity.Message;
import com.dorandoran.chat.entity.User;
import com.dorandoran.chat.entity.Chatbot;
import com.dorandoran.chat.entity.IntimacyProgress;
import com.dorandoran.chat.repository.ChatRoomRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import com.dorandoran.chat.repository.MessageRepository;
import com.dorandoran.chat.repository.UserRepository;
import com.dorandoran.chat.repository.ChatbotRepository;
import com.dorandoran.chat.repository.IntimacyProgressRepository;
import jakarta.transaction.Transactional;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

/**
 * Chat Service 비즈니스 로직 (단순화 스키마 기반)
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ChatbotRepository chatbotRepository;
    private final IntimacyProgressRepository intimacyProgressRepository;
    private final ObjectMapper objectMapper;
    @PersistenceContext
    private EntityManager entityManager;
    // AI 트리거는 컨트롤러에서 수행하여 순환 의존 제거

    /**
     * 채팅방 조회 또는 생성 (userId + chatbotId 조합)
     */
    @Transactional
    public ChatRoom getOrCreateRoom(UUID userId, UUID chatbotId, String name) {
        return getOrCreateRoom(userId, chatbotId, name, "FRIEND", 2);
    }
    
    /**
     * 채팅방 조회 또는 생성 (컨셉과 친밀도 포함)
     */
    @Transactional
    public ChatRoom getOrCreateRoom(UUID userId, UUID chatbotId, String name, String concept, Integer intimacyLevel) {
        // 삭제되지 않은 활성 채팅방만 조회
        Optional<ChatRoom> existing = chatRoomRepository.findByUser_IdAndChatbot_IdAndIsDeletedFalse(userId, chatbotId);
        if (existing.isPresent()) {
            ChatRoom room = existing.get();
            
            // 기존 채팅방의 concept과 intimacyLevel 업데이트
            updateRoomSettings(room, concept);
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

        // settings에 concept 저장
        ObjectNode settings = objectMapper.createObjectNode();
        settings.put("concept", concept);

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
     * 다음 시퀀스 번호 계산 (채팅방 내 최대값 + 1)
     */
    @Transactional
    public long nextSequenceNumber(UUID chatroomId) {
        return messageRepository.findTopByChatRoomIdOrderBySequenceNumberDesc(chatroomId)
            .map(m -> m.getSequenceNumber() + 1)
            .orElse(1L);
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
            .isDeleted(false)
            .isEdited(false)
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

        // 사용자-챗봇 마지막 상호작용 upsert (메시지 저장 시점)
        try {
            UUID chatbotId = chatRoom.getChatbot().getId();
            updateLastInteraction(senderId, chatbotId, chatroomId, LocalDateTime.now());
        } catch (Exception ignored) {}

        return saved;
    }

    /**
     * 사용자별 채팅방 목록 조회 (삭제되지 않은, 최신 메시지 순) - 페이징
     */
    @Transactional
    public Page<ChatRoom> listRooms(UUID userId, Pageable pageable) {
        Page<ChatRoom> result = chatRoomRepository.findByUser_IdAndIsDeletedFalseOrderByLastMessageAtDesc(userId, pageable);
        System.out.println("[DEBUG] listRooms(page) userId=" + userId + ", totalElements=" + result.getTotalElements());
        return result;
    }

    /**
     * 사용자별 채팅방 목록 조회 (삭제되지 않은, 최신 메시지 순) - 전체
     */
    @Transactional
    public List<ChatRoom> listRooms(UUID userId) {
        List<ChatRoom> result = chatRoomRepository.findByUser_IdAndIsDeletedFalseOrderByLastMessageAtDesc(userId);
        System.out.println("[DEBUG] listRooms(all) userId=" + userId + ", size=" + (result == null ? 0 : result.size()));
        return result;
    }

    /**
     * 사용자-챗봇 마지막 상호작용 시간을 업데이트 (nullable timestamp 허용)
     */
    public void updateLastInteraction(UUID userId, UUID chatbotId, UUID roomId, LocalDateTime timestamp) {
        if (userId == null) {
            System.out.println("[WARN] updateLastInteraction skipped: userId is null, chatbotId=" + chatbotId + ", roomId=" + roomId);
            return;
        }
        OffsetDateTime ts = timestamp == null ? null : timestamp.atOffset(java.time.ZoneOffset.UTC);
        entityManager.createNativeQuery("""
            INSERT INTO chat_schema.user_chatbot_last_interaction (user_id, chatbot_id, last_interaction_at, last_room_id)
            VALUES (?1, ?2, ?3, ?4)
            ON CONFLICT (user_id, chatbot_id) DO UPDATE SET
              last_interaction_at = GREATEST(EXCLUDED.last_interaction_at, chat_schema.user_chatbot_last_interaction.last_interaction_at),
              last_room_id = CASE
                WHEN EXCLUDED.last_interaction_at IS NOT NULL AND (
                  chat_schema.user_chatbot_last_interaction.last_interaction_at IS NULL OR
                  EXCLUDED.last_interaction_at >= chat_schema.user_chatbot_last_interaction.last_interaction_at
                ) THEN EXCLUDED.last_room_id
                ELSE chat_schema.user_chatbot_last_interaction.last_room_id
              END
        """)
            .setParameter(1, userId)
            .setParameter(2, chatbotId)
            .setParameter(3, ts)
            .setParameter(4, roomId)
            .executeUpdate();
    }

    /**
     * 사용자 기준 챗봇별 최신 상호작용 상위 N 조회
     */
    public List<com.dorandoran.chat.service.dto.LastInteractionResponse> listLastInteractionsByChatbot(UUID userId, int limit) {
        List<Object[]> rows = entityManager.createNativeQuery("""
            SELECT chatbot_id, last_interaction_at, last_room_id
            FROM chat_schema.user_chatbot_last_interaction
            WHERE user_id = ?1
            ORDER BY last_interaction_at DESC NULLS LAST
            LIMIT ?2
        """)
            .setParameter(1, userId)
            .setParameter(2, limit)
            .getResultList();
        List<com.dorandoran.chat.service.dto.LastInteractionResponse> out = new java.util.ArrayList<>();
        for (Object[] r : rows) {
            UUID chatbotId = (UUID) r[0];
            Object tsObj = r[1];
            UUID lastRoomId = (UUID) r[2];
            java.time.OffsetDateTime odt;
            if (tsObj == null) {
                odt = null;
            } else if (tsObj instanceof java.sql.Timestamp t) {
                odt = t.toInstant().atOffset(java.time.ZoneOffset.UTC);
            } else if (tsObj instanceof java.time.Instant i) {
                odt = java.time.OffsetDateTime.ofInstant(i, java.time.ZoneOffset.UTC);
            } else if (tsObj instanceof java.time.OffsetDateTime o) {
                odt = o;
            } else {
                System.out.println("[WARN] Unknown timestamp type from DB: " + tsObj.getClass());
                odt = null;
            }
            out.add(new com.dorandoran.chat.service.dto.LastInteractionResponse(
                    chatbotId,
                    lastRoomId,
                    odt,
                    null
            ));
        }
        return out;
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
        if (!chatRoomRepository.existsByUser_IdAndIdAndIsDeletedFalse(userId, chatroomId)) {
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
        if (!chatRoomRepository.existsByUser_IdAndIdAndIsDeletedFalse(userId, chatroomId)) {
            throw new RuntimeException("Access denied or room already deleted: " + chatroomId);
        }
        // 방 나가기 시 마지막 메시지 시간으로 집계 테이블 업데이트 (없으면 NULL)
        LocalDateTime lastTs = messageRepository.findTopByChatRoomIdOrderBySequenceNumberDesc(chatroomId)
            .map(Message::getCreatedAt)
            .orElse(null);
        try {
            updateLastInteraction(userId, room.getChatbot().getId(), chatroomId, lastTs);
        } catch (Exception ignored) {}
        room.setIsDeleted(true);
        room.setUpdatedAt(java.time.LocalDateTime.now());
        chatRoomRepository.save(room);
    }

    /**
     * 코치마크 표시 여부 조회 (room.settings.coachmarkShown)
     */
    @Transactional
    public boolean isCoachmarkShown(UUID chatroomId, UUID userId) {
        if (!chatRoomRepository.existsByUser_IdAndIdAndIsDeletedFalse(userId, chatroomId)) {
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
        if (!chatRoomRepository.existsByUser_IdAndIdAndIsDeletedFalse(userId, chatroomId)) {
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
        if (!chatRoomRepository.existsByUser_IdAndIdAndIsDeletedFalse(userId, chatroomId)) {
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
            .orElse(2); // 기본값
    }
    
    /**
     * 채팅방 settings에 concept 저장
     */
    private void updateRoomSettings(ChatRoom room, String concept) {
        ObjectNode settings = room.getSettings() != null && room.getSettings().isObject()
            ? (ObjectNode) room.getSettings()
            : objectMapper.createObjectNode();
        settings.put("concept", concept);
        room.setSettings(settings);
        room.setUpdatedAt(LocalDateTime.now());
        chatRoomRepository.save(room);
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
}
