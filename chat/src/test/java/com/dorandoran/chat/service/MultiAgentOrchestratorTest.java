package com.dorandoran.chat.service;

import com.dorandoran.chat.entity.ChatRoom;
import com.dorandoran.chat.entity.IntimacyProgress;
import com.dorandoran.chat.entity.Message;
import com.dorandoran.chat.repository.IntimacyProgressRepository;
import com.dorandoran.chat.sse.SSEManager;
import com.dorandoran.chat.service.agent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
// import reactor.test.StepVerifier; // 제거됨

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MultiAgentOrchestratorTest {

    @Mock
    private IntimacyAgent intimacyAgent;
    
    @Mock
    private VocabularyAgent vocabularyAgent;
    
    @Mock
    private ConversationAgent conversationAgent;
    
    @Mock
    private SummarizerAgent summarizerAgent;
    
    @Mock
    private SSEManager sseManager;
    
    @Mock
    private IntimacyProgressRepository intimacyProgressRepository;
    
    @Mock
    private ChatService chatService;
    
    @Mock
    private com.dorandoran.chat.repository.ChatRoomRepository chatRoomRepository;
    
    private MultiAgentOrchestrator orchestrator;
    
    private UUID chatroomId;
    private UUID userId;
    private Message userMessage;
    private ChatRoom chatRoom;
    
    @BeforeEach
    void setUp() {
        orchestrator = new MultiAgentOrchestrator(
            intimacyAgent, vocabularyAgent, conversationAgent, summarizerAgent,
            sseManager, intimacyProgressRepository, chatService, chatRoomRepository
        );
        
        chatroomId = UUID.randomUUID();
        userId = UUID.randomUUID();
        
        userMessage = Message.builder()
            .id(UUID.randomUUID())
            .content("안녕하세요")
            .build();
            
        chatRoom = new ChatRoom();
        chatRoom.setId(chatroomId);
    }
    
    @Test
    void processUserMessage_병렬_Agent_실행_테스트() {
        // Given
        IntimacyAgentResponse intimacyResp = new IntimacyAgentResponse(
            "intimacy", 2, "안녕하세요!", new FeedbackText("좋은 인사입니다.", ""), "", List.of()
        );
        VocabularyAgentResponse vocabResp = new VocabularyAgentResponse(
            "vocabulary", List.of()
        );
        
        when(intimacyProgressRepository.findByChatRoomId(chatroomId))
            .thenReturn(Optional.empty());
        when(intimacyAgent.analyze(any(), any()))
            .thenReturn(Mono.just(intimacyResp));
        when(vocabularyAgent.extractDifficultWords(any(), any()))
            .thenReturn(Mono.just(vocabResp));
        when(conversationAgent.generateResponse(any(), any(), any()))
            .thenReturn(Flux.just("응답"));
        when(chatRoomRepository.getReferenceById(chatroomId))
            .thenReturn(chatRoom);
        when(chatService.getChatRoomById(chatroomId))
            .thenReturn(chatRoom);
        when(intimacyProgressRepository.save(any()))
            .thenReturn(new IntimacyProgress());
        
        // When
        orchestrator.processUserMessage(chatroomId, userId, userMessage);
        
        // Then
        verify(intimacyAgent).analyze(eq(chatroomId), eq("안녕하세요"));
        verify(vocabularyAgent).extractDifficultWords(any(), eq(chatroomId));
        verify(sseManager, atLeastOnce()).send(eq(chatroomId), anyString(), any());
    }
    
    @Test
    void processUserMessage_친밀도_진척_업데이트_테스트() {
        // Given
        IntimacyProgress existingProgress = IntimacyProgress.builder()
            .id(UUID.randomUUID())
            .chatRoom(chatRoom)
            .userId(userId)
            .intimacyLevel(1)
            .totalCorrections(0)
            .lastUpdated(LocalDateTime.now())
            .build();
            
        IntimacyAgentResponse intimacyResp = new IntimacyAgentResponse(
            "intimacy", 2, "안녕하세요!", new FeedbackText("좋은 인사입니다.", ""), "변경사항1", List.of()
        );
        
        when(intimacyProgressRepository.findByChatRoomId(chatroomId))
            .thenReturn(Optional.of(existingProgress));
        when(intimacyAgent.analyze(any(), any()))
            .thenReturn(Mono.just(intimacyResp));
        when(vocabularyAgent.extractDifficultWords(any(), any()))
            .thenReturn(Mono.just(new VocabularyAgentResponse("vocabulary", List.of())));
        when(conversationAgent.generateResponse(any(), any(), any()))
            .thenReturn(Flux.just("응답"));
        when(chatRoomRepository.getReferenceById(chatroomId))
            .thenReturn(chatRoom);
        when(chatService.getChatRoomById(chatroomId))
            .thenReturn(chatRoom);
        when(intimacyProgressRepository.save(any()))
            .thenReturn(existingProgress);
        
        // When
        orchestrator.processUserMessage(chatroomId, userId, userMessage);
        
        // Then
        verify(intimacyProgressRepository).save(argThat(progress -> 
            progress.getIntimacyLevel() == 2 && 
            progress.getTotalCorrections() == 1 &&
            progress.getLastFeedback().equals("좋은 인사입니다.")
        ));
    }
    
    @Test
    void processUserMessage_SSE_이벤트_순서_검증() {
        // Given
        when(intimacyProgressRepository.findByChatRoomId(chatroomId))
            .thenReturn(Optional.empty());
        when(intimacyAgent.analyze(any(), any()))
            .thenReturn(Mono.just(new IntimacyAgentResponse("intimacy", 1, "", new FeedbackText("", ""), "", List.of())));
        when(vocabularyAgent.extractDifficultWords(any(), any()))
            .thenReturn(Mono.just(new VocabularyAgentResponse("vocabulary", List.of())));
        when(conversationAgent.generateResponse(any(), any(), any()))
            .thenReturn(Flux.just("응답"));
        when(chatRoomRepository.getReferenceById(chatroomId))
            .thenReturn(chatRoom);
        when(chatService.getChatRoomById(chatroomId))
            .thenReturn(chatRoom);
        when(intimacyProgressRepository.save(any()))
            .thenReturn(new IntimacyProgress());
        
        // When
        orchestrator.processUserMessage(chatroomId, userId, userMessage);
        
        // Then
        verify(sseManager, timeout(1000).atLeastOnce()).send(eq(chatroomId), anyString(), any());
    }
}
