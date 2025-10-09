package com.dorandoran.chat.service;

import com.dorandoran.chat.entity.ChatRoom;
import com.dorandoran.chat.entity.Chatbot;
import com.dorandoran.chat.entity.IntimacyProgress;
import com.dorandoran.chat.entity.Message;
import com.dorandoran.chat.repository.IntimacyProgressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * AI 인사말 자동 발송 서비스
 * 채팅방 생성 직후 AI가 사용자에게 인사말을 보냄
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GreetingService {
    private final ChatService chatService;
    private final IntimacyProgressRepository intimacyProgressRepository;
    
    @Transactional
    public void sendGreeting(UUID chatroomId, UUID userId) {
        try {
            ChatRoom chatRoom = chatService.getChatRoomById(chatroomId);
            Chatbot chatbot = chatRoom.getChatbot();
            int intimacyLevel = chatbot.getIntimacyLevel();
            
            String greetingMessage = buildGreetingMessage(intimacyLevel);
            
            // AI 인사말 메시지 저장
            Message greeting = chatService.sendMessage(
                chatroomId, 
                null, 
                "bot", 
                greetingMessage, 
                "text"
            );
            
            // 친밀도 진척 초기화
            initializeIntimacyProgress(chatroomId, userId, intimacyLevel);
            
            log.info("AI 인사말 발송 완료: chatroomId={}, messageId={}, intimacyLevel={}", 
                chatroomId, greeting.getId(), intimacyLevel);
                
        } catch (Exception e) {
            log.error("AI 인사말 발송 실패: chatroomId={}", chatroomId, e);
        }
    }
    
    private String buildGreetingMessage(int intimacyLevel) {
        return switch (intimacyLevel) {
            case 1 -> "안녕하세요! 한국어 학습을 도와드리겠습니다. 격식체로 대화해보세요.";
            case 2 -> "안녕하세요! 부드러운 존댓말로 편하게 대화해보세요.";
            case 3 -> "안녕! 친근하게 반말로 대화해보자!";
            default -> "안녕하세요! 한국어 학습을 도와드리겠습니다.";
        };
    }
    
    private void initializeIntimacyProgress(UUID chatroomId, UUID userId, int intimacyLevel) {
        IntimacyProgress progress = IntimacyProgress.builder()
            .id(UUID.randomUUID())
            .chatRoom(chatService.getChatRoomById(chatroomId))
            .userId(userId)
            .intimacyLevel(intimacyLevel)
            .totalCorrections(0)
            .lastFeedback("AI 인사말 발송")
            .lastUpdated(LocalDateTime.now())
            .progressData("{}")
            .build();
            
        intimacyProgressRepository.save(progress);
        log.debug("친밀도 진척 초기화: chatroomId={}, level={}", chatroomId, intimacyLevel);
    }
}
