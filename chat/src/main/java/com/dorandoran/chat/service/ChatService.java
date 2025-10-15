package com.dorandoran.chat.service;

import com.dorandoran.chat.entity.ChatRoom;
import com.dorandoran.chat.entity.Message;
import com.dorandoran.chat.entity.User;
import com.dorandoran.chat.entity.Chatbot;
import com.dorandoran.chat.event.MessageEvent;
import com.dorandoran.chat.repository.ChatRoomRepository;
import com.dorandoran.chat.repository.MessageRepository;
import com.dorandoran.chat.repository.UserRepository;
import com.dorandoran.chat.repository.ChatbotRepository;
import com.dorandoran.chat.messaging.RedisMessagePublisher;
import jakarta.transaction.Transactional;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

/**
 * Chat Service (Redis Pub/Sub + 캐싱 통합)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ChatbotRepository chatbotRepository;
    private final ObjectMapper objectMapper;
    // AI 트리거는 컨트롤러에서 수행하여 순환 의존 제거
    private final RedisMessagePublisher redisPublisher;
    private final RedisCacheService cacheService;

    /**
     * 채팅방 조회 또는 생성 (userId + chatbotId 조합)
     */
    @Transactional
    public ChatRoom getOrCreateRoom(UUID userId, UUID chatbotId, String name) {
        Optional<ChatRoom> existing = chatRoomRepository.findByUserIdAndChatbotIdAndIsDeletedFalse(userId, chatbotId);
        if (existing.isPresent()) {
            return existing.get();
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

        ChatRoom room = ChatRoom.builder()
            .id(roomId)
            .user(user)
            .chatbot(chatbot)
            .name(name)
            .isArchived(false)
            .isDeleted(false)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        ChatRoom saved = chatRoomRepository.save(room);

        // 새로 생성된 채팅방 캐싱
        cacheService.cacheChatRoom(saved);

        return saved;
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
     * 메시지 전송 (Redis Pub/Sub + 캐싱 통합)
     */
    @Transactional
    public Message sendMessage(UUID chatroomId, UUID senderId, String senderType, String content, String contentType) {
        long seq = nextSequenceNumber(chatroomId);
        // ChatRoom 객체 조회
        ChatRoom chatRoom = chatRoomRepository.findById(chatroomId)
            .orElseThrow(() -> new RuntimeException("ChatRoom not found: " + chatroomId));

        Message message = Message.builder()
            .id(UUID.randomUUID())
            .chatRoom(chatRoom)
            .senderType(senderType)
            .senderId(senderId)
            .content(content)
            .contentType(contentType)
            .sequenceNumber(seq)
            .isDeleted(false)
            .isEdited(false)
            .build();
        Message saved = messageRepository.save(message);

        chatRoomRepository.findById(chatroomId).ifPresent(room -> {
            room.setLastMessageAt(LocalDateTime.now());
            room.setLastMessage(saved);
            room.setUpdatedAt(LocalDateTime.now());
            chatRoomRepository.save(room);

            // 채팅방 캐시 무효화 (lastMessage 변경)
            cacheService.invalidateChatRoomCache(chatroomId);
        });

        // 메시지 캐시에 추가
        cacheService.appendMessageToCache(chatroomId, saved);

        // Redis Pub/Sub으로 메시지 이벤트 발행
        MessageEvent event = new MessageEvent(
            saved.getId(),
            chatroomId,
            senderId,
            senderType,
            content,
            contentType,
            System.currentTimeMillis()
        );
        redisPublisher.publishMessage(event);

        log.debug("메시지 저장 및 발행 완료: messageId={}, chatroomId={}", saved.getId(), chatroomId);

        return saved;
    }

    /**
     * 사용자별 채팅방 목록 조회 (삭제되지 않은, 최신 메시지 순) - 페이징
     */
    @Transactional
    public Page<ChatRoom> listRooms(UUID userId, Pageable pageable) {
        return chatRoomRepository.findByUserIdAndIsDeletedFalseOrderByLastMessageAtDesc(userId, pageable);
    }

    /**
     * 사용자별 채팅방 목록 조회 (삭제되지 않은, 최신 메시지 순) - 전체
     */
    @Transactional
    public List<ChatRoom> listRooms(UUID userId) {
        return chatRoomRepository.findByUserIdAndIsDeletedFalseOrderByLastMessageAtDesc(userId);
    }

    /**
     * 메시지 조회 (캐싱 적용)
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
        // 1. 캐시 확인
        List<Message> cached = cacheService.getCachedMessages(chatroomId);
        if (!cached.isEmpty()) {
            log.debug("메시지 캐시에서 반환: chatroomId={}, count={}", chatroomId, cached.size());
            return cached;
        }

        // 2. DB 조회
        log.debug("메시지 DB 조회: chatroomId={}", chatroomId);
        List<Message> messages = messageRepository.findByChatRoomIdOrderBySequenceNumberAsc(chatroomId);

        // 3. 캐시 저장
        if (!messages.isEmpty()) {
            cacheService.cacheMessages(chatroomId, messages);
        }

        return messages;
    }

    /**
     * 채팅방 조회 (ID로)
     */
    @Transactional
    public ChatRoom getChatRoomById(UUID chatroomId) {
        // 1. 캐시 확인
        ChatRoom cached = cacheService.getCachedChatRoom(chatroomId);
        if (cached != null) {
            log.debug("채팅방 캐시에서 반환: chatroomId={}", chatroomId);
            return cached;
        }

        // 2. DB 조회
        log.debug("채팅방 DB 조회: chatroomId={}", chatroomId);
        ChatRoom chatRoom = chatRoomRepository.findById(chatroomId)
            .orElseThrow(() -> new RuntimeException("ChatRoom not found: " + chatroomId));

        // 3. 캐시 저장
        cacheService.cacheChatRoom(chatRoom);

        return chatRoom;
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
}