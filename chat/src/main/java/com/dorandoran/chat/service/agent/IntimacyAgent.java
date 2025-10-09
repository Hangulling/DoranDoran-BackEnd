package com.dorandoran.chat.service.agent;

import com.dorandoran.chat.entity.IntimacyProgress;
import com.dorandoran.chat.repository.IntimacyProgressRepository;
import com.dorandoran.chat.service.OpenAIClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
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
    private final ObjectMapper objectMapper;

    public Mono<IntimacyAgentResponse> analyze(UUID chatroomId, String userMessage) {
        log.info("=== IntimacyAgent.analyze() 호출됨 ===");
        log.info("=== IntimacyAgent 파라미터 - chatroomId={}, userMessage='{}' ===", chatroomId, userMessage);
        
        int currentLevel = intimacyProgressRepository.findByChatRoomId(chatroomId)
            .map(IntimacyProgress::getIntimacyLevel)
            .orElse(1);
        log.info("=== IntimacyAgent 현재 레벨 조회: {} ===", currentLevel);
        
        String systemPrompt = buildIntimacyPrompt(currentLevel);
        log.info("=== IntimacyAgent systemPrompt: {} ===", systemPrompt);
        
        log.info("=== IntimacyAgent OpenAI API 호출 시작 ===");
        return openAIClient.streamRawCompletion(systemPrompt, userMessage)
            .doOnSubscribe(subscription -> log.info("=== IntimacyAgent 스트림 구독 시작 ==="))
            .doOnNext(chunk -> log.info("=== IntimacyAgent 원시 청크 받음: '{}' ===", chunk))
            .doOnError(error -> log.error("=== IntimacyAgent 스트림 오류 ===", error))
            .doOnComplete(() -> log.info("=== IntimacyAgent 스트림 완료 ==="))
            .collectList()
            .doOnSuccess(chunks -> log.info("=== IntimacyAgent collectList 성공, chunks 수: {} ===", chunks.size()))
            .doOnError(error -> log.error("=== IntimacyAgent collectList 오류 ===", error))
            .map(this::parseIntimacyResponse)
            .doOnSuccess(response -> log.info("=== IntimacyAgent 파싱 성공: {} ===", response))
            .doOnError(error -> log.error("=== IntimacyAgent 파싱 오류 ===", error));
    }
    
    private String buildIntimacyPrompt(int level) {
        return String.format("""
            당신은 외국인의 한국어 친밀도를 분석하는 전문가입니다.
            현재 학습자의 목표 레벨: %d (1=격식체/존댓말, 2=부드러운 존댓말, 3=친근한 반말)
            
            다음 문장을 분석하여 JSON 형식으로 답변하세요:
            {
              "detectedLevel": 1-3,
              "correctedSentence": "교정된 문장",
              "feedback": "피드백 메시지",
              "corrections": ["변경사항1", "변경사항2"]
            }
            """, level);
    }
    
    private IntimacyAgentResponse parseIntimacyResponse(List<String> chunks) {
        log.info("=== IntimacyAgent 파싱 시작 ===");
        try {
            // OpenAI 스트림에서 실제 content만 추출
            StringBuilder contentBuilder = new StringBuilder();
            for (String chunk : chunks) {
                try {
                    JsonNode chunkJson = objectMapper.readTree(chunk);
                    if (chunkJson.has("choices") && chunkJson.get("choices").isArray()) {
                        for (JsonNode choice : chunkJson.get("choices")) {
                            if (choice.has("delta") && choice.get("delta").has("content")) {
                                String content = choice.get("delta").get("content").asText();
                                contentBuilder.append(content);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.debug("IntimacyAgent 청크 파싱 실패 (무시): {}", e.getMessage());
                }
            }
            
            String fullResponse = contentBuilder.toString();
            log.info("=== IntimacyAgent 추출된 content: '{}' ===", fullResponse);
            
            if (fullResponse.trim().isEmpty()) {
                log.info("=== IntimacyAgent content가 비어있음 - 기본값 반환 ===");
                return new IntimacyAgentResponse("intimacy", 1, "", "", List.of());
            }
            
            JsonNode json = objectMapper.readTree(fullResponse);
            log.info("=== IntimacyAgent JSON 파싱 성공: {} ===", json);
            
            int detectedLevel = json.has("detectedLevel") ? json.get("detectedLevel").asInt() : 1;
            String correctedSentence = json.has("correctedSentence") ? json.get("correctedSentence").asText() : "";
            String feedback = json.has("feedback") ? json.get("feedback").asText() : "";
            
            log.info("=== IntimacyAgent 필드 추출: detectedLevel={}, correctedSentence='{}', feedback='{}' ===", 
                    detectedLevel, correctedSentence, feedback);
            
            List<String> corrections = new ArrayList<>();
            if (json.has("corrections") && json.get("corrections").isArray()) {
                log.info("=== IntimacyAgent corrections 배열 발견: {} ===", json.get("corrections"));
                for (JsonNode correction : json.get("corrections")) {
                    String correctionText = correction.asText();
                    corrections.add(correctionText);
                    log.info("=== IntimacyAgent correction 추가: '{}' ===", correctionText);
                }
            } else {
                log.info("=== IntimacyAgent corrections 배열이 없거나 배열이 아님 ===");
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
                1,
                "",
                "분석 중 오류가 발생했습니다.",
                List.of()
            );
        }
    }
}
