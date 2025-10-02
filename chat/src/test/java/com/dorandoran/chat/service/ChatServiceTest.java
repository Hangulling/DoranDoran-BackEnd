package com.dorandoran.chat.service;

import com.dorandoran.chat.entity.ChatRoom;
import com.dorandoran.chat.entity.Message;
import com.dorandoran.chat.repository.ChatRoomRepository;
import com.dorandoran.chat.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ChatService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private MessageRepository messageRepository;

    @InjectMocks
    private ChatService chatService;

    private UUID testUserId;
    private UUID testBotId;
    private UUID testRoomId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testBotId = UUID.randomUUID();
        testRoomId = UUID.randomUUID();
    }

    @Test
    void getAllChatRooms_성공() {
        // Given
        ChatRoom room1 = createTestChatRoom();
        ChatRoom room2 = createTestChatRoom();
        List<ChatRoom> expectedRooms = Arrays.asList(room1, room2);
        
        when(chatRoomRepository.findAll()).thenReturn(expectedRooms);

        // When
        List<ChatRoom> result = chatService.getAllChatRooms();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrder(room1, room2);
        verify(chatRoomRepository).findAll();
    }

    @Test
    void getChatRoomsByUser_성공() {
        // Given
        ChatRoom room1 = createTestChatRoom();
        ChatRoom room2 = createTestChatRoom();
        List<ChatRoom> expectedRooms = Arrays.asList(room1, room2);
        
        when(chatRoomRepository.findByUserId(testUserId)).thenReturn(expectedRooms);

        // When
        List<ChatRoom> result = chatService.getChatRoomsByUser(testUserId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrder(room1, room2);
        verify(chatRoomRepository).findByUserId(testUserId);
    }

    @Test
    void createChatRoom_성공() {
        // Given
        String roomName = "테스트 채팅방";
        ChatRoom expectedRoom = createTestChatRoom();
        
        when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(expectedRoom);

        // When
        ChatRoom result = chatService.createChatRoom(testUserId, testBotId, roomName);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(testUserId);
        assertThat(result.getBotId()).isEqualTo(testBotId);
        assertThat(result.getRoomName()).isEqualTo(roomName);
        verify(chatRoomRepository).save(any(ChatRoom.class));
    }

    @Test
    void getChatRoom_존재하는방_성공() {
        // Given
        ChatRoom expectedRoom = createTestChatRoom();
        when(chatRoomRepository.findById(testRoomId)).thenReturn(Optional.of(expectedRoom));

        // When
        Optional<ChatRoom> result = chatService.getChatRoom(testRoomId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(expectedRoom);
        verify(chatRoomRepository).findById(testRoomId);
    }

    @Test
    void getChatRoom_존재하지않는방_빈값반환() {
        // Given
        when(chatRoomRepository.findById(testRoomId)).thenReturn(Optional.empty());

        // When
        Optional<ChatRoom> result = chatService.getChatRoom(testRoomId);

        // Then
        assertThat(result).isEmpty();
        verify(chatRoomRepository).findById(testRoomId);
    }

    @Test
    void deleteChatRoom_존재하는방_성공() {
        // Given
        ChatRoom room = createTestChatRoom();
        when(chatRoomRepository.findById(testRoomId)).thenReturn(Optional.of(room));
        when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(room);

        // When
        boolean result = chatService.deleteChatRoom(testRoomId);

        // Then
        assertThat(result).isTrue();
        verify(chatRoomRepository).findById(testRoomId);
        verify(chatRoomRepository).save(any(ChatRoom.class));
    }

    @Test
    void deleteChatRoom_존재하지않는방_실패() {
        // Given
        when(chatRoomRepository.findById(testRoomId)).thenReturn(Optional.empty());

        // When
        boolean result = chatService.deleteChatRoom(testRoomId);

        // Then
        assertThat(result).isFalse();
        verify(chatRoomRepository).findById(testRoomId);
        verify(chatRoomRepository, never()).save(any(ChatRoom.class));
    }

    @Test
    void sendMessage_성공() {
        // Given
        String content = "안녕하세요";
        String senderType = "user";
        ChatRoom room = createTestChatRoom();
        Message expectedMessage = createTestMessage();
        
        when(chatRoomRepository.findById(testRoomId)).thenReturn(Optional.of(room));
        when(messageRepository.countByRoomId(testRoomId)).thenReturn(0L);
        when(messageRepository.save(any(Message.class))).thenReturn(expectedMessage);

        // When
        Message result = chatService.sendMessage(testRoomId, testUserId, testBotId, content, senderType);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo(content);
        assertThat(result.getSenderType()).isEqualTo(senderType);
        verify(chatRoomRepository).findById(testRoomId);
        verify(messageRepository).countByRoomId(testRoomId);
        verify(messageRepository).save(any(Message.class));
    }

    @Test
    void sendMessage_존재하지않는방_예외발생() {
        // Given
        String content = "안녕하세요";
        String senderType = "user";
        
        when(chatRoomRepository.findById(testRoomId)).thenReturn(Optional.empty());

        // When & Then
        try {
            chatService.sendMessage(testRoomId, testUserId, testBotId, content, senderType);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).contains("채팅방을 찾을 수 없습니다");
        }
        
        verify(chatRoomRepository).findById(testRoomId);
        verify(messageRepository, never()).save(any(Message.class));
    }

    private ChatRoom createTestChatRoom() {
        return ChatRoom.builder()
                .roomId(testRoomId)
                .userId(testUserId)
                .botId(testBotId)
                .roomName("테스트 채팅방")
                .settings("{}")
                .isDeleted(false)
                .createAt(LocalDateTime.now())
                .build();
    }

    private Message createTestMessage() {
        return Message.builder()
                .messegeId(UUID.randomUUID())
                .roomId(testRoomId)
                .userId(testUserId)
                .botId(testBotId)
                .content("테스트 메시지")
                .senderType("user")
                .chatNum(1)
                .messageSendTime(LocalDateTime.now())
                .messageType("text")
                .messageMeta("{}")
                .createdAt(LocalDateTime.now())
                .build();
    }
}
