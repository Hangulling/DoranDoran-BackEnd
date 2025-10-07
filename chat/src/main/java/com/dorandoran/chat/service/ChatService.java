package com.dorandoran.chat.service;

import com.dorandoran.chat.entity.ChatRoom;
import com.dorandoran.chat.entity.Message;
import com.dorandoran.chat.entity.User;
import com.dorandoran.chat.entity.Chatbot;
import com.dorandoran.chat.repository.ChatRoomRepository;
import com.dorandoran.chat.repository.MessageRepository;
import com.dorandoran.chat.repository.UserRepository;
import com.dorandoran.chat.repository.ChatbotRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
    // AI 트리거는 컨트롤러에서 수행하여 순환 의존 제거

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
        return chatRoomRepository.save(room);
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
        });

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
}
