package com.dorandoran.chat.service.agent;

import com.dorandoran.chat.entity.Message;
import com.dorandoran.chat.repository.MessageRepository;
import com.dorandoran.chat.service.OpenAIClient;
import com.dorandoran.chat.service.PromptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 대화 Agent
 * 사용자와 자연스러운 대화를 생성하는 Agent
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationAgent {
    private final OpenAIClient openAIClient;
    private final PromptService promptService;
    private final MessageRepository messageRepository;
    
    public Flux<String> generateResponse(UUID chatroomId, String userMessage) {
        log.info("=== ConversationAgent.generateResponse() 호출됨 ===");
        String systemPrompt = promptService.buildSystemPrompt(chatroomId);
        
        // 친밀도 레벨 재주입 (매 턴마다)
        String enhancedUserMessage = promptService.injectIntimacyReminder(chatroomId, userMessage);
        
        log.info("ConversationAgent 시작: chatroomId={}, userMessage='{}'", chatroomId, enhancedUserMessage);
        log.info("System Prompt: {}", systemPrompt);
        
        // 최근 메시지 히스토리 가져오기 (현재 사용자 메시지 제외)
        List<Map<String, String>> messageHistory = buildMessageHistory(chatroomId);
        log.info("ConversationAgent 히스토리: {} 개 메시지", messageHistory.size());
        
        // 디버그: 히스토리 내용 로그
        for (int i = 0; i < messageHistory.size(); i++) {
            Map<String, String> msg = messageHistory.get(i);
            log.info("히스토리[{}]: role={}, content='{}'", i, msg.get("role"), msg.get("content").substring(0, Math.min(100, msg.get("content").length())));
        }
        
        log.info("=== OpenAI API 호출 시작 (히스토리 포함) ===");
        return openAIClient.streamRawCompletionWithHistory(systemPrompt, messageHistory, enhancedUserMessage)
            .doOnError(error -> log.error("ConversationAgent 원시 응답 오류: {}", error.getMessage(), error))
            .map(raw -> {
                try {
                    String jsonData = raw.startsWith("data: ") ? raw.substring(6) : raw;
                    
                    if ("[DONE]".equals(jsonData.trim())) {
                        return "";
                    }
                    
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    com.fasterxml.jackson.databind.JsonNode node;
                    try {
                        node = mapper.readTree(jsonData);
                    } catch (Exception e) {
                        log.debug("JSON 파싱 실패 (무시): {}", e.getMessage());
                        return "";
                    }
                    
                    if (node.has("choices") && node.get("choices").isArray() && node.get("choices").size() > 0) {
                        com.fasterxml.jackson.databind.JsonNode choice = node.get("choices").get(0);
                        com.fasterxml.jackson.databind.JsonNode delta = choice.get("delta");
                        
                        if (delta != null && delta.has("content")) {
                            String content = delta.get("content").asText();
                            
                            if (content != null && !content.isEmpty()) {
                                return content;
                            }
                        }
                    }
                    return "";
                } catch (Exception e) {
                    log.error("텍스트 추출 오류: {}", e.getMessage(), e);
                    return "";
                }
            })
            .filter(content -> content != null && !content.isEmpty())
            .doOnError(error -> log.error("ConversationAgent 스트림 오류", error));
    }
    
    /**
     * 최근 메시지 히스토리를 OpenAI 형식으로 변환
     * @param chatroomId 채팅방 ID
     * @return List<Map<String, String>> [{role, content}, ...]
     */
    private List<Map<String, String>> buildMessageHistory(UUID chatroomId) {
        try {
            // 최근 메시지 조회
            List<Message> recentMessages = messageRepository
                .findByChatRoomIdOrderBySequenceNumberAsc(chatroomId);
            
            // OpenAI 형식으로 변환 (마지막 메시지 제외 - 현재 사용자 메시지)
            List<Map<String, String>> history = new ArrayList<>();
            int messagesToInclude = recentMessages.size() - 1; // 마지막 메시지(현재 사용자 메시지) 제외
            
            // 연속된 동일한 내용의 메시지를 추적하기 위한 변수
            String previousContent = null;
            String previousRole = null;
            
            for (int i = 0; i < messagesToInclude; i++) {
                Message msg = recentMessages.get(i);
                
                // system 메시지는 제외 (가이드 목적이므로)
                if ("system".equalsIgnoreCase(msg.getSenderType())) {
                    continue;
                }
                
                // senderType을 OpenAI role로 변환
                String role = "bot".equalsIgnoreCase(msg.getSenderType()) ? "assistant" : "user";
                String content = msg.getContent();
                
                // 이전 메시지와 동일한 내용인 경우 제외 (교정 메시지, 중복 메시지)
                if (previousRole != null && previousContent != null) {
                    if (content.equals(previousContent)) {
                        log.debug("중복 메시지 제외: role={}, content='{}'", role, content.substring(0, Math.min(50, content.length())));
                        continue;
                    }
                }
                
                // 메시지 추가
                history.add(Map.of("role", role, "content", content));
                
                // 이전 메시지 업데이트
                previousRole = role;
                previousContent = content;
            }
            
            // 최근 10개로 제한 (토큰 비용 최적화)
            if (history.size() > 10) {
                history = history.subList(history.size() - 10, history.size());
            }
            
            log.debug("히스토리 구성: 총 {} 메시지 중 {} 개 포함", recentMessages.size(), history.size());
            
            return history;
        } catch (Exception e) {
            log.error("히스토리 수집 실패: chatroomId={}", chatroomId, e);
            return new ArrayList<>(); // 빈 리스트 반환
        }
    }
}
