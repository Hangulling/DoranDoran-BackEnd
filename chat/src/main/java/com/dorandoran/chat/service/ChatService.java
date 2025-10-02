package com.dorandoran.chat.service;

import com.dorandoran.chat.entity.ChatRoom;
import com.dorandoran.chat.entity.Message;
import com.dorandoran.chat.repository.ChatRoomRepository;
import com.dorandoran.chat.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Chat 서비스 비즈니스 로직
 * 채팅방 관리, 메시지 전송 등의 핵심 기능 제공
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository;

    /**
     * 모든 채팅방 조회
     */
    public List<ChatRoom> getAllChatRooms() {
        log.info("모든 채팅방 조회 요청");
        return chatRoomRepository.findAll();
    }

    /**
     * 사용자의 채팅방 조회
     */
    public List<ChatRoom> getChatRoomsByUser(UUID userId) {
        log.info("사용자 {}의 채팅방 조회 요청", userId);
        return chatRoomRepository.findByUserId(userId);
    }

    /**
     * 채팅방 생성
     */
    @Transactional
    public ChatRoom createChatRoom(UUID userId, UUID botId, String roomName) {
        log.info("채팅방 생성 요청 - 사용자: {}, 봇: {}, 방명: {}", userId, botId, roomName);
        
        ChatRoom room = ChatRoom.builder()
                .roomId(UUID.randomUUID())
                .userId(userId)
                .botId(botId)
                .roomName(roomName)
                .settings("{}")
                .isDeleted(false)
                .build();
        
        ChatRoom savedRoom = chatRoomRepository.save(room);
        log.info("채팅방 생성 완료 - ID: {}", savedRoom.getRoomId());
        return savedRoom;
    }

    /**
     * 채팅방 조회
     */
    public Optional<ChatRoom> getChatRoom(UUID roomId) {
        log.info("채팅방 조회 요청 - ID: {}", roomId);
        return chatRoomRepository.findById(roomId);
    }

    /**
     * 채팅방 삭제 (소프트 삭제)
     */
    @Transactional
    public boolean deleteChatRoom(UUID roomId) {
        log.info("채팅방 삭제 요청 - ID: {}", roomId);
        
        Optional<ChatRoom> roomOpt = chatRoomRepository.findById(roomId);
        if (roomOpt.isEmpty()) {
            log.warn("삭제할 채팅방을 찾을 수 없음 - ID: {}", roomId);
            return false;
        }
        
        ChatRoom room = roomOpt.get();
        room.setIsDeleted(true);
        chatRoomRepository.save(room);
        
        log.info("채팅방 삭제 완료 - ID: {}", roomId);
        return true;
    }

    /**
     * 채팅방의 메시지 조회
     */
    public List<Message> getMessages(UUID roomId) {
        log.info("채팅방 {}의 메시지 조회 요청", roomId);
        return messageRepository.findByRoomIdOrderByCreatedAtAsc(roomId);
    }

    /**
     * 메시지 전송
     */
    @Transactional
    public Message sendMessage(UUID roomId, UUID userId, UUID botId, 
                             String content, String senderType) {
        log.info("메시지 전송 요청 - 방: {}, 사용자: {}, 내용: {}", roomId, userId, content);
        
        // 채팅방 존재 확인
        Optional<ChatRoom> roomOpt = chatRoomRepository.findById(roomId);
        if (roomOpt.isEmpty()) {
            throw new IllegalArgumentException("채팅방을 찾을 수 없습니다: " + roomId);
        }
        
        // 메시지 번호 계산 (해당 방의 메시지 개수 + 1)
        long messageCount = messageRepository.countByRoomId(roomId);
        
        Message message = Message.builder()
                .messegeId(UUID.randomUUID())
                .roomId(roomId)
                .userId(userId)
                .botId(botId)
                .content(content)
                .senderType(senderType)
                .chatNum((int) (messageCount + 1))
                .messageSendTime(LocalDateTime.now())
                .messageType("text")
                .messageMeta("{}")
                .build();
        
        Message savedMessage = messageRepository.save(message);
        log.info("메시지 전송 완료 - ID: {}", savedMessage.getMessegeId());
        return savedMessage;
    }

    /**
     * 메시지 검색
     */
    public List<Message> searchMessages(UUID roomId, String keyword) {
        log.info("메시지 검색 요청 - 방: {}, 키워드: {}", roomId, keyword);
        return messageRepository.findByRoomIdAndContentContainingIgnoreCase(roomId, keyword);
    }

    /**
     * 채팅방 통계 조회
     */
    public ChatRoomStats getChatRoomStats(UUID roomId) {
        log.info("채팅방 통계 조회 요청 - 방: {}", roomId);
        
        long messageCount = messageRepository.countByRoomId(roomId);
        Optional<Message> lastMessage = messageRepository.findTopByRoomIdOrderByCreatedAtDesc(roomId);
        
        return ChatRoomStats.builder()
                .roomId(roomId)
                .messageCount(messageCount)
                .lastMessageTime(lastMessage.map(Message::getCreatedAt).orElse(null))
                .build();
    }

    /**
     * 채팅방 통계 DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class ChatRoomStats {
        private UUID roomId;
        private long messageCount;
        private LocalDateTime lastMessageTime;
    }
}
