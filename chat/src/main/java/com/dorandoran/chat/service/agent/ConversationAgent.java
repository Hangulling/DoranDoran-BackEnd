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
            .doOnSubscribe(subscription -> log.info("=== ConversationAgent 스트림 구독 시작 ==="))
            .doOnNext(raw -> log.info("=== ConversationAgent 원시 응답 받음: {} ===", raw))
            .doOnError(error -> log.error("=== ConversationAgent 원시 응답 오류: {} ===", error.getMessage(), error))
            .map(raw -> {
                log.info("=== ConversationAgent map() 함수 호출됨: {} ===", raw);
                // 각 원시 응답에서 텍스트 추출
                try {
                    String jsonData = raw.startsWith("data: ") ? raw.substring(6) : raw;
                    log.info("=== JSON 데이터: {} ===", jsonData);
                    
                    if ("[DONE]".equals(jsonData.trim())) {
                        log.info("=== OpenAI 스트림 완료 [DONE] ===");
                        return "";
                    }
                    
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(jsonData);
                    log.info("=== JSON 파싱 성공: {} ===", node);
                    
                    if (node.has("choices")) {
                        log.info("=== choices 발견: {} ===", node.get("choices"));
                        for (com.fasterxml.jackson.databind.JsonNode choice : node.get("choices")) {
                            log.info("=== choice 처리: {} ===", choice);
                            com.fasterxml.jackson.databind.JsonNode delta = choice.get("delta");
                            log.info("=== delta: {} ===", delta);
                            
                            if (delta != null && delta.has("content")) {
                                String content = delta.get("content").asText();
                                log.info("=== content 추출: '{}' ===", content);
                                
                                if (content != null && !content.isEmpty()) {
                                    log.info("=== 텍스트 청크 추출 성공: '{}' ===", content);
                                    return content;
                                } else {
                                    log.info("=== content가 비어있음 ===");
                                }
                            } else {
                                log.info("=== delta가 null이거나 content가 없음 ===");
                            }
                        }
                    } else {
                        log.info("=== choices가 없음 ===");
                    }
                    log.info("=== 빈 문자열 반환 ===");
                    return "";
                } catch (Exception e) {
                    log.error("=== 텍스트 추출 오류: {} ===", e.getMessage(), e);
                    return "";
                }
            })
            .doOnNext(content -> log.info("=== map() 결과: '{}' ===", content))
            .filter(content -> {
                boolean isValid = content != null && !content.isEmpty();
                log.info("=== 필터링: '{}' -> {} ===", content, isValid);
                return isValid;
            })
            .doOnNext(chunk -> log.info("=== 최종 텍스트 청크: '{}' ===", chunk))
            .doOnComplete(() -> log.info("=== ConversationAgent 스트림 완료 ==="))
            .doOnError(error -> log.error("=== ConversationAgent 스트림 오류 ===", error))
            .doOnCancel(() -> log.info("=== ConversationAgent 스트림 취소 ==="));
    }
}
