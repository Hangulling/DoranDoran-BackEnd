package com.dorandoran.chat.service.agent;

import com.dorandoran.chat.entity.IntimacyProgress;
import com.dorandoran.chat.entity.Chatbot;
import com.dorandoran.chat.repository.IntimacyProgressRepository;
import com.dorandoran.chat.repository.ChatRoomRepository;
import com.dorandoran.chat.repository.ChatbotRepository;
import com.dorandoran.chat.service.OpenAIClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 친밀도 분석 Agent
 * 외국인의 한국어 친밀도를 분석하고 교정된 문장을 제공
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IntimacyAgent {
    private final OpenAIClient openAIClient;
    private final IntimacyProgressRepository intimacyProgressRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatbotRepository chatbotRepository;
    private final ObjectMapper objectMapper;

    public Mono<IntimacyAgentResponse> analyze(UUID chatroomId, String userMessage) {
        log.info("=== IntimacyAgent.analyze() 호출됨 ===");
        log.info("=== IntimacyAgent 파라미터 - chatroomId={}, userMessage='{}' ===", chatroomId, userMessage);
        
        int currentLevel = intimacyProgressRepository.findByChatRoomId(chatroomId)
            .map(IntimacyProgress::getIntimacyLevel)
            .orElse(1);
        log.info("=== IntimacyAgent 현재 레벨 조회: {} ===", currentLevel);
        
        String concept = getConceptFromChatRoom(chatroomId);
        String systemPrompt = buildIntimacyPrompt(chatroomId, currentLevel, concept);
        log.info("=== IntimacyAgent systemPrompt: {} ===", systemPrompt);
        
        log.info("=== IntimacyAgent OpenAI API 호출 시작 ===");
        return openAIClient.streamRawCompletion(systemPrompt, userMessage)
            .doOnError(error -> log.error("IntimacyAgent 스트림 오류", error))
            .collectList()
            .doOnError(error -> log.error("IntimacyAgent collectList 오류", error))
            .map(this::parseIntimacyResponse)
            .doOnSuccess(response -> log.info("IntimacyAgent 파싱 완료: 레벨={}", response.detectedLevel()))
            .doOnError(error -> log.error("IntimacyAgent 파싱 오류", error));
    }
    
    private String getConceptFromChatRoom(UUID chatroomId) {
        return chatRoomRepository.findById(chatroomId)
            .map(room -> {
                if (room.getSettings() != null && room.getSettings().has("concept")) {
                    return room.getSettings().get("concept").asText();
                }
                return "FRIEND";
            })
            .orElse("FRIEND");
    }
    
    private String buildIntimacyPrompt(UUID chatroomId, int level, String concept) {
        // 1. DB에서 Base Prompt 조회
        String basePrompt = getBasePromptFromDB(chatroomId);
        
        // 2. Dynamic Directives 생성
        String conceptGuideline = getConceptGuideline(concept);
        String dynamicDirectives = String.format("""
            
            [분석 컨텍스트]
            현재 학습자의 목표 레벨: %d (1=격식체/존댓말, 2=부드러운 존댓말, 3=친근한 반말)
            대화 컨셉: %s
            
            [컨셉별 지침]
            %s
            
            [응답 형식]
            반드시 아래 JSON 형식으로만 응답하세요. 다른 텍스트는 포함하지 마세요:
            {
              "detectedLevel": 1-3,
              "correctedSentence": "교정된 문장",
              "feedback": "피드백 메시지",
              "corrections": ["변경사항1", "변경사항2"]
            }
            """, level, concept, conceptGuideline);
        
        // 3. 합성
        return basePrompt + dynamicDirectives;
    }
    
    private String getBasePromptFromDB(UUID chatroomId) {
        return chatRoomRepository.findById(chatroomId)
            .flatMap(room -> {
                if (room.getChatbot() == null) return Optional.empty();
                return chatbotRepository.findById(room.getChatbot().getId());
            })
            .map(Chatbot::getIntimacySystemPrompt)
            .orElse(getDefaultIntimacyBasePrompt());
    }
    
    private String getDefaultIntimacyBasePrompt() {
        return """
            당신은 외국인의 한국어 친밀도를 분석하는 전문가입니다.
            
            사용자의 문장을 분석하여 반드시 JSON 형식으로만 답변하세요.
            다른 텍스트나 설명은 포함하지 마세요.
            
            응답 형식:
            {
              "detectedLevel": 1-3,
              "correctedSentence": "교정된 문장",
              "feedback": "피드백 메시지",
              "corrections": ["변경사항1", "변경사항2"]
            }
            """;
    }
    
    private IntimacyAgentResponse parseIntimacyResponse(List<String> chunks) {
        try {
            // OpenAI 스트림에서 실제 content만 추출
            StringBuilder contentBuilder = new StringBuilder();
            for (String chunk : chunks) {
                try {
                    JsonNode chunkJson = objectMapper.readTree(chunk);
                    if (chunkJson.has("choices") && chunkJson.get("choices").isArray() && chunkJson.get("choices").size() > 0) {
                        JsonNode choice = chunkJson.get("choices").get(0);
                        if (choice.has("delta") && choice.get("delta").has("content")) {
                            String content = choice.get("delta").get("content").asText();
                            contentBuilder.append(content);
                        }
                    }
                } catch (Exception e) {
                    log.debug("IntimacyAgent 청크 파싱 실패 (무시): {}", e.getMessage());
                }
            }
            
            String fullResponse = contentBuilder.toString();
            
            if (fullResponse.trim().isEmpty()) {
                return new IntimacyAgentResponse("intimacy", 0, "", new FeedbackText("", ""), "");
            }
            
            JsonNode json;
            try {
                json = objectMapper.readTree(fullResponse);
            } catch (Exception e) {
                log.warn("IntimacyAgent JSON 파싱 실패: {}", e.getMessage());
                return new IntimacyAgentResponse(
                    "intimacy",
                    0,
                    "",
                    new FeedbackText("분석 중 오류가 발생했습니다.", "An error occurred during analysis."),
                    ""
                );
            }
            
            int detectedLevel = json.has("detectedLevel") ? json.get("detectedLevel").asInt() : 0;
            String correctedSentence = json.has("correctedSentence") ? json.get("correctedSentence").asText() : "";
            String corrections = json.has("corrections") ? json.get("corrections").asText() : "";
            
            // feedback 파싱 (ko/en 구조)
            FeedbackText feedback = new FeedbackText("", "");
            if (json.has("feedback") && json.get("feedback").isObject()) {
                JsonNode feedbackNode = json.get("feedback");
                String ko = feedbackNode.has("ko") ? feedbackNode.get("ko").asText() : "";
                String en = feedbackNode.has("en") ? feedbackNode.get("en").asText() : "";
                feedback = new FeedbackText(ko, en);
            }
            
            return new IntimacyAgentResponse(
                "intimacy",
                detectedLevel,
                correctedSentence,
                feedback,
                corrections
            );
        } catch (Exception e) {
            log.error("IntimacyAgent 응답 파싱 실패", e);
            return new IntimacyAgentResponse(
                "intimacy",
                0,
                "",
                new FeedbackText("분석 중 오류가 발생했습니다.", "An error occurred during analysis."),
                ""
            );
        }
    }
    
    private String getConceptGuideline(String concept) {
        return switch (concept) {
            case "FRIEND" -> "친구와의 대화 상황을 고려하여 자연스럽고 편한 표현을 교정하세요.";
            case "HONEY" -> "연인과의 대화 상황을 고려하여 애정 어린 표현을 교정하세요.";
            case "COWORKER" -> "직장 동료와의 대화 상황을 고려하여 예의 바르고 전문적인 표현을 교정하세요.";
            case "SENIOR" -> "선배와의 대화 상황을 고려하여 공손하고 정중한 표현을 교정하세요.";
            case "BOSS" -> "직장 상사와의 대화 상황을 고려하여 존경하고 정중한 표현을 교정하세요.";
            default -> "일반적인 상황에 맞는 적절한 표현을 교정하세요.";
        };
    }
}
