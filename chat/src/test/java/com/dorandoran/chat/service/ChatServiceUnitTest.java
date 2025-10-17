package com.dorandoran.chat.service;

import com.dorandoran.chat.entity.ChatRoom;
import com.dorandoran.chat.entity.Message;
import com.dorandoran.chat.entity.User;
import com.dorandoran.chat.entity.Chatbot;
import com.dorandoran.chat.repository.ChatRoomRepository;
import com.dorandoran.chat.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceUnitTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private MessageRepository messageRepository;

    @InjectMocks
    private ChatService chatService;

    private UUID userId;
    private UUID chatbotId;
    private UUID chatroomId;

    @BeforeEach
    void setUp() {
        userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        chatbotId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        chatroomId = UUID.fromString("00000000-0000-0000-0000-000000000003");
    }

    @Test
    @DisplayName("기존 채팅방이 있으면 그대로 반환한다")
    void getOrCreateRoom_returnsExisting() {
        User user = User.builder().id(userId).build();
        Chatbot chatbot = Chatbot.builder().id(chatbotId).build();
        ChatRoom existing = ChatRoom.builder()
            .id(chatroomId)
            .user(user)
            .chatbot(chatbot)
            .name("room")
            .isDeleted(false)
            .isArchived(false)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        when(chatRoomRepository.findByUserIdAndChatbotIdAndIsDeletedFalse(userId, chatbotId))
            .thenReturn(Optional.of(existing));

        ChatRoom result = chatService.getOrCreateRoom(userId, chatbotId, "ignored");

        assertThat(result).isSameAs(existing);
        verify(chatRoomRepository, never()).save(any());
    }

    @Test
    @DisplayName("기존 채팅방이 없으면 새로 생성하여 저장한다")
    void getOrCreateRoom_createsNew() {
        when(chatRoomRepository.findByUserIdAndChatbotIdAndIsDeletedFalse(userId, chatbotId))
            .thenReturn(Optional.empty());

        ArgumentCaptor<ChatRoom> roomCaptor = ArgumentCaptor.forClass(ChatRoom.class);
        when(chatRoomRepository.save(roomCaptor.capture()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        ChatRoom result = chatService.getOrCreateRoom(userId, chatbotId, "new-room");

        ChatRoom saved = roomCaptor.getValue();
        assertThat(saved.getUser().getId()).isEqualTo(userId);
        assertThat(saved.getChatbot().getId()).isEqualTo(chatbotId);
        assertThat(saved.getName()).isEqualTo("new-room");
        assertThat(saved.getIsArchived()).isFalse();
        assertThat(saved.getIsDeleted()).isFalse();
        assertThat(result).isSameAs(saved);
    }

    @Test
    @DisplayName("다음 시퀀스: 기존 메시지가 없으면 1 반환")
    void nextSequence_noMessages_returns1() {
        when(messageRepository.findTopByChatRoomIdOrderBySequenceNumberDesc(chatroomId))
            .thenReturn(Optional.empty());

        long seq = chatService.nextSequenceNumber(chatroomId);
        assertThat(seq).isEqualTo(1L);
    }

    @Test
    @DisplayName("다음 시퀀스: 마지막 시퀀스가 10이면 11 반환")
    void nextSequence_hasMessages_returnsIncremented() {
        Message last = Message.builder().id(UUID.randomUUID()).sequenceNumber(10L).build();
        when(messageRepository.findTopByChatRoomIdOrderBySequenceNumberDesc(chatroomId))
            .thenReturn(Optional.of(last));

        long seq = chatService.nextSequenceNumber(chatroomId);
        assertThat(seq).isEqualTo(11L);
    }

    @Test
    @DisplayName("메시지 전송 시 저장되고, 채팅방 최신 메시지 메타가 갱신된다")
    void sendMessage_savesAndUpdatesRoom() {
        when(messageRepository.findTopByChatRoomIdOrderBySequenceNumberDesc(chatroomId))
            .thenReturn(Optional.empty());

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        when(messageRepository.save(messageCaptor.capture()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        ChatRoom room = ChatRoom.builder().id(chatroomId).build();
        when(chatRoomRepository.findById(chatroomId)).thenReturn(Optional.of(room));

        Message saved = chatService.sendMessage(chatroomId, userId, "user", "hello", "text");

        Message toSave = messageCaptor.getValue();
        assertThat(toSave.getChatRoom().getId()).isEqualTo(chatroomId);
        assertThat(toSave.getSenderId()).isEqualTo(userId);
        assertThat(toSave.getSenderType()).isEqualTo("user");
        assertThat(toSave.getContent()).isEqualTo("hello");
        assertThat(toSave.getContentType()).isEqualTo("text");
        assertThat(toSave.getSequenceNumber()).isEqualTo(1L);

        assertThat(saved).isSameAs(toSave);
        verify(chatRoomRepository, times(1)).save(any(ChatRoom.class));
        assertThat(room.getLastMessage().getId()).isEqualTo(saved.getId());
        assertThat(room.getLastMessageAt()).isNotNull();
    }

    @Test
    @DisplayName("방 목록 페이징 조회는 레포지토리에 위임한다")
    void listRooms_pageable_delegates() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<ChatRoom> page = new PageImpl<>(List.of());
        when(chatRoomRepository.findByUserIdAndIsDeletedFalseOrderByLastMessageAtDesc(userId, pageable))
            .thenReturn(page);

        Page<ChatRoom> result = chatService.listRooms(userId, pageable);
        assertThat(result).isSameAs(page);
    }

    @Test
    @DisplayName("메시지 목록 전체 조회는 레포지토리에 위임한다")
    void listMessages_all_delegates() {
        when(messageRepository.findByChatRoomIdOrderBySequenceNumberAsc(chatroomId))
            .thenReturn(List.of());

        List<Message> result = chatService.listMessages(chatroomId);
        assertThat(result).isEmpty();
    }
}


