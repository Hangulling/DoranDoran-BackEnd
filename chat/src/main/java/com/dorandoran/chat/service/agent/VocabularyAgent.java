package com.dorandoran.chat.service.agent;

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
 * 어휘 추출 Agent
 * 외국인이 이해하기 어려운 한국어 단어/표현을 최대 1개 추출
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VocabularyAgent {
    private final OpenAIClient openAIClient;
    private final ObjectMapper objectMapper;

    public Mono<VocabularyAgentResponse> extractDifficultWords(String userMessage, int userLevel) {
        log.info("=== VocabularyAgent.extractDifficultWords() 호출됨 ===");
        log.info("=== VocabularyAgent 파라미터 - userMessage='{}', userLevel={} ===", userMessage, userLevel);
        
        String systemPrompt = buildVocabularyPrompt(userLevel);
        log.info("=== VocabularyAgent systemPrompt: {} ===", systemPrompt);
        
        log.info("=== VocabularyAgent OpenAI API 호출 시작 ===");
        return openAIClient.streamRawCompletion(systemPrompt, userMessage)
            .doOnSubscribe(subscription -> log.info("=== VocabularyAgent 스트림 구독 시작 ==="))
            .doOnNext(chunk -> log.info("=== VocabularyAgent 원시 청크 받음: '{}' ===", chunk))
            .doOnError(error -> log.error("=== VocabularyAgent 스트림 오류 ===", error))
            .doOnComplete(() -> log.info("=== VocabularyAgent 스트림 완료 ==="))
            .collectList()
            .doOnSuccess(chunks -> log.info("=== VocabularyAgent collectList 성공, chunks 수: {} ===", chunks.size()))
            .doOnError(error -> log.error("=== VocabularyAgent collectList 오류 ===", error))
            .map(this::parseVocabularyResponse)
            .doOnSuccess(response -> log.info("=== VocabularyAgent 파싱 성공: {} ===", response))
            .doOnError(error -> log.error("=== VocabularyAgent 파싱 오류 ===", error));
    }
    
    private String buildVocabularyPrompt(int userLevel) {
        return String.format("""
            외국인이 이해하기 어려운 한국어 단어/표현을 최대 1개 추출하세요.
            사용자 레벨: %d (1=초급, 2=중급, 3=고급)
            
            JSON 형식:
            {
              "words": [
                {"word": "단어", "difficulty": 1-3, "context": "문맥"}
              ]
            }
            """, userLevel);
    }
    
    private VocabularyAgentResponse parseVocabularyResponse(List<String> chunks) {
        log.info("=== VocabularyAgent 파싱 시작 ===");
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
                    log.debug("VocabularyAgent 청크 파싱 실패 (무시): {}", e.getMessage());
                }
            }
            
            String fullResponse = contentBuilder.toString();
            log.info("=== VocabularyAgent 추출된 content: '{}' ===", fullResponse);
            
            if (fullResponse.trim().isEmpty()) {
                log.info("=== VocabularyAgent content가 비어있음 - 빈 응답 반환 ===");
                return new VocabularyAgentResponse("vocabulary", List.of());
            }
            
            JsonNode json = objectMapper.readTree(fullResponse);
            log.info("=== VocabularyAgent JSON 파싱 성공: {} ===", json);
            
            List<VocabularyAgentResponse.VocabularyWord> words = new ArrayList<>();
            if (json.has("words") && json.get("words").isArray()) {
                log.info("=== VocabularyAgent words 배열 발견: {} ===", json.get("words"));
                for (JsonNode wordNode : json.get("words")) {
                    String word = wordNode.has("word") ? wordNode.get("word").asText() : "";
                    int difficulty = wordNode.has("difficulty") ? wordNode.get("difficulty").asInt() : 1;
                    String context = wordNode.has("context") ? wordNode.get("context").asText() : "";
                    
                    log.info("=== VocabularyAgent 단어 처리: word='{}', difficulty={}, context='{}' ===", word, difficulty, context);
                    
                    if (!word.isEmpty()) {
                        words.add(new VocabularyAgentResponse.VocabularyWord(word, difficulty, context));
                        log.info("=== VocabularyAgent 단어 추가됨 ===");
                    } else {
                        log.info("=== VocabularyAgent 단어가 비어있어서 건너뜀 ===");
                    }
                }
            } else {
                log.info("=== VocabularyAgent words 배열이 없거나 배열이 아님 ===");
            }
            
            return new VocabularyAgentResponse("vocabulary", words);
        } catch (Exception e) {
            log.error("VocabularyAgent 응답 파싱 실패", e);
            return new VocabularyAgentResponse("vocabulary", List.of());
        }
    }
}
