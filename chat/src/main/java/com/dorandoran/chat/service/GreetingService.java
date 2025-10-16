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
        sendGreeting(chatroomId, userId, 2); // 기본값 2
    }
    
    @Transactional
    public void sendGreeting(UUID chatroomId, UUID userId, int intimacyLevel) {
        try {
            ChatRoom chatRoom = chatService.getChatRoomById(chatroomId);
            String concept = chatService.getConcept(chatroomId);
            
            String greetingMessage = buildGreetingMessage(concept, intimacyLevel);
            
//            // AI 인사말 메시지 저장
//            Message greeting = chatService.sendMessage(
//                chatroomId,
//                null,
//                "bot",
//                greetingMessage,
//                "text"
//            );

            Message greeting = chatService.sendTemporaryMessage(
                chatroomId,
                null,
                "bot",
                greetingMessage,
                "text"
            );
            
            log.info("AI 인사말 발송 완료: chatroomId={}, messageId={}, concept={}, intimacyLevel={}", 
                chatroomId, greeting.getId(), concept, intimacyLevel);
                
        } catch (Exception e) {
            log.error("AI 인사말 발송 실패: chatroomId={}", chatroomId, e);
        }
    }
    
    private String buildGreetingMessage(String concept, int intimacyLevel) {
        String baseMessage = getConceptGreeting(concept);
        String intimacySuffix = getIntimacySuffix(intimacyLevel);
        return baseMessage + " " + intimacySuffix;
    }
    
    private String getConceptGreeting(String concept) {
        return switch (concept) {
            case "FRIEND" -> "안녕! 친구처럼 편하게 대화해보자!";
            case "HONEY" -> "안녕, 사랑! 우리만의 특별한 시간을 가져보자";
            case "COWORKER" -> "안녕하세요! 직장 동료로서 함께 일해보겠습니다";
            case "SENIOR" -> "안녕하세요! 선배로서 함께 공부해보겠습니다";
            default -> "안녕하세요! 함께 대화해보겠습니다";
        };
    }
    
    private String getIntimacySuffix(int intimacyLevel) {
        return switch (intimacyLevel) {
            case 1 -> "격식체(~습니다, ~입니다)로 대화해보세요.";
            case 2 -> "부드러운 존댓말(~해요, ~이에요)로 대화해보세요.";
            case 3 -> "친근한 반말(~야, ~어, ~지)로 대화해보자!";
            default -> "편하게 대화해보세요.";
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
