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
    private final RedisMessagePublisher redisPublisher;
    private final RedisCacheService cacheService;

    @Transactional
    public ChatRoom getOrCreateRoom(UUID userId, UUID chatbotId, String name) {
        Optional<ChatRoom> existing = chatRoomRepository.findByUserIdAndChatbotIdAndIsDeletedFalse(userId, chatbotId);
        if (existing.isPresent()) {
            return existing.get();
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        Chatbot chatbot = chatbotRepository.findById(chatbotId)
            .orElseThrow(() -> new RuntimeException("Chatbot not found: " + chatbotId));

        ChatRoom room = ChatRoom.builder()
            .id(UUID.randomUUID())
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

    @Transactional
    public Page<ChatRoom> listRooms(UUID userId, Pageable pageable) {
        return chatRoomRepository.findByUserIdAndIsDeletedFalseOrderByLastMessageAtDesc(userId, pageable);
    }

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
     * 메시지 조회 전체 (캐싱 적용)
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
     * 채팅방 조회 (캐싱 적용)
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
}