package com.dorandoran.chat.service;

import com.dorandoran.chat.entity.ChatRoom;
import com.dorandoran.chat.entity.Chatbot;
import com.dorandoran.chat.entity.User;
import com.dorandoran.chat.enums.ChatRoomConcept;
import com.dorandoran.chat.repository.IntimacyProgressRepository;
import com.dorandoran.chat.service.dto.GreetingResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GreetingServiceTest {

    @Mock
    private ChatService chatService;
    
    @Mock
    private IntimacyProgressRepository intimacyProgressRepository;
    
    @Mock
    private OpenAIClient openAIClient;

    @InjectMocks
    private GreetingService greetingService;

    private UUID chatroomId;
    private UUID userId;
    private UUID chatbotId;
    private ChatRoom mockChatRoom;
    private Chatbot mockChatbot;

    @BeforeEach
    void setUp() {
        chatroomId = UUID.randomUUID();
        userId = UUID.randomUUID();
        chatbotId = UUID.randomUUID();
        
        mockChatbot = new Chatbot();
        mockChatbot.setId(chatbotId);
        mockChatbot.setName("테스트 챗봇");
        
        mockChatRoom = new ChatRoom();
        mockChatRoom.setId(chatroomId);
        mockChatRoom.setChatbot(mockChatbot);
    }

    @Test
    void testSendGreeting_WithAISuccess() {
        // Given
        ChatRoomConcept concept = ChatRoomConcept.FRIEND;
        int intimacyLevel = 2;
        
        String mockAIResponse = """
            {
              "botMessage": "안녕하세요! 오늘 뭐 배우고 싶어요? 최근에 한국 드라마 보셨나요?",
              "guideMessage": "일상 대화나 관심 있는 주제에 대해 이야기해보세요!"
            }
            """;
        
        when(chatService.getChatRoomById(chatroomId)).thenReturn(mockChatRoom);
        when(openAIClient.simpleCompletion(anyString(), anyString())).thenReturn(mockAIResponse);
        when(chatService.sendMessage(any(), any(), anyString(), anyString(), anyString()))
            .thenReturn(new com.dorandoran.chat.entity.Message());

        // When
        GreetingResponse result = greetingService.sendGreeting(chatroomId, userId, concept, intimacyLevel);

        // Then
        assertNotNull(result);
        assertEquals("안녕하세요! 오늘 뭐 배우고 싶어요? 최근에 한국 드라마 보셨나요?", result.getBotMessage());
        assertEquals("일상 대화나 관심 있는 주제에 대해 이야기해보세요!", result.getGuideMessage());
        
        // verify that messages were saved
        verify(chatService, times(2)).sendMessage(any(), any(), anyString(), anyString(), anyString());
        verify(intimacyProgressRepository).save(any());
    }

    @Test
    void testSendGreeting_WithAIFailure_FallbackUsed() {
        // Given
        ChatRoomConcept concept = ChatRoomConcept.FRIEND;
        int intimacyLevel = 2;
        
        when(chatService.getChatRoomById(chatroomId)).thenReturn(mockChatRoom);
        when(openAIClient.simpleCompletion(anyString(), anyString()))
            .thenThrow(new RuntimeException("AI 서비스 오류"));
        when(chatService.sendMessage(any(), any(), anyString(), anyString(), anyString()))
            .thenReturn(new com.dorandoran.chat.entity.Message());

        // When
        GreetingResponse result = greetingService.sendGreeting(chatroomId, userId, concept, intimacyLevel);

        // Then
        assertNotNull(result);
        assertEquals("안녕하세요! 오늘 뭐 배우고 싶어요?", result.getBotMessage());
        assertEquals("일상 대화나 관심 있는 주제에 대해 이야기해보세요!", result.getGuideMessage());
        
        // verify that messages were still saved
        verify(chatService, times(2)).sendMessage(any(), any(), anyString(), anyString(), anyString());
    }

    @Test
    void testSendGreeting_FriendIntimacyLevel1_Fallback() {
        // Given
        ChatRoomConcept concept = ChatRoomConcept.FRIEND;
        int intimacyLevel = 1;
        
        when(chatService.getChatRoomById(chatroomId)).thenReturn(mockChatRoom);
        when(openAIClient.simpleCompletion(anyString(), anyString()))
            .thenThrow(new RuntimeException("AI 서비스 오류"));
        when(chatService.sendMessage(any(), any(), anyString(), anyString(), anyString()))
            .thenReturn(new com.dorandoran.chat.entity.Message());

        // When
        GreetingResponse result = greetingService.sendGreeting(chatroomId, userId, concept, intimacyLevel);

        // Then
        assertNotNull(result);
        assertEquals("안녕하십니까. 오늘 함께 한국어를 공부하겠습니다.", result.getBotMessage());
        assertEquals("한국어 문법이나 표현에 대해 질문해보세요!", result.getGuideMessage());
    }

    @Test
    void testSendGreeting_LoverIntimacyLevel2_Fallback() {
        // Given
        ChatRoomConcept concept = ChatRoomConcept.LOVER;
        int intimacyLevel = 2;
        
        when(chatService.getChatRoomById(chatroomId)).thenReturn(mockChatRoom);
        when(openAIClient.simpleCompletion(anyString(), anyString()))
            .thenThrow(new RuntimeException("AI 서비스 오류"));
        when(chatService.sendMessage(any(), any(), anyString(), anyString(), anyString()))
            .thenReturn(new com.dorandoran.chat.entity.Message());

        // When
        GreetingResponse result = greetingService.sendGreeting(chatroomId, userId, concept, intimacyLevel);

        // Then
        assertNotNull(result);
        assertEquals("안녕! 오늘은 무슨 얘기 나눠볼까요?", result.getBotMessage());
        assertEquals("사랑과 감정에 관한 한국어 표현을 연습해보세요!", result.getGuideMessage());
    }

    @Test
    void testSendGreeting_CoworkerIntimacyLevel3_Fallback() {
        // Given
        ChatRoomConcept concept = ChatRoomConcept.COWORKER;
        int intimacyLevel = 3;
        
        when(chatService.getChatRoomById(chatroomId)).thenReturn(mockChatRoom);
        when(openAIClient.simpleCompletion(anyString(), anyString()))
            .thenThrow(new RuntimeException("AI 서비스 오류"));
        when(chatService.sendMessage(any(), any(), anyString(), anyString(), anyString()))
            .thenReturn(new com.dorandoran.chat.entity.Message());

        // When
        GreetingResponse result = greetingService.sendGreeting(chatroomId, userId, concept, intimacyLevel);

        // Then
        assertNotNull(result);
        assertEquals("안녕! 오늘도 같이 공부해볼까?", result.getBotMessage());
        assertEquals("동료들과 자연스럽게 소통할 수 있는 표현들을 연습해보자!", result.getGuideMessage());
    }

    @Test
    void testSendGreeting_BossIntimacyLevel1_Fallback() {
        // Given
        ChatRoomConcept concept = ChatRoomConcept.BOSS;
        int intimacyLevel = 1;
        
        when(chatService.getChatRoomById(chatroomId)).thenReturn(mockChatRoom);
        when(openAIClient.simpleCompletion(anyString(), anyString()))
            .thenThrow(new RuntimeException("AI 서비스 오류"));
        when(chatService.sendMessage(any(), any(), anyString(), anyString(), anyString()))
            .thenReturn(new com.dorandoran.chat.entity.Message());

        // When
        GreetingResponse result = greetingService.sendGreeting(chatroomId, userId, concept, intimacyLevel);

        // Then
        assertNotNull(result);
        assertEquals("안녕하십니까. 오늘 학습하실 내용을 말씀해 주시기 바랍니다.", result.getBotMessage());
        assertEquals("상사와의 대화에서 필요한 격식 있는 한국어를 연습해보세요!", result.getGuideMessage());
    }
}
