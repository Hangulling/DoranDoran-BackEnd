package com.dorandoran.chat.service.agent;

import com.dorandoran.chat.service.OpenAIClient;
import com.dorandoran.chat.service.PromptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

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
    
    public Flux<String> generateResponse(UUID chatroomId, String userMessage) {
        log.info("=== ConversationAgent.generateResponse() 호출됨 ===");
        String systemPrompt = promptService.buildSystemPrompt(chatroomId);
        log.info("ConversationAgent 시작: chatroomId={}, userMessage='{}'", chatroomId, userMessage);
        log.info("System Prompt: {}", systemPrompt);
        
        log.info("=== OpenAI API 호출 시작 ===");
        return openAIClient.streamRawCompletion(systemPrompt, userMessage)
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
}
