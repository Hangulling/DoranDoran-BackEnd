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

/**
 * 번역 Agent
 * VocabularyAgent에서 추출한 단어를 영어로 번역하고 발음기호 제공
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TranslationAgent {
    private final OpenAIClient openAIClient;
    private final ObjectMapper objectMapper;

    public Mono<TranslationAgentResponse> translate(List<VocabularyAgentResponse.VocabularyWord> words) {
        log.info("=== TranslationAgent.translate() 호출됨 ===");
        log.info("=== TranslationAgent 파라미터 - words 수: {} ===", words.size());
        
        if (words.isEmpty()) {
            log.info("=== TranslationAgent: 단어가 비어있음 - 빈 응답 반환 ===");
            return Mono.just(new TranslationAgentResponse("translation", List.of()));
        }
        
        String systemPrompt = buildTranslationPrompt();
        String userPrompt = buildUserPrompt(words);
        log.info("=== TranslationAgent systemPrompt: {} ===", systemPrompt);
        log.info("=== TranslationAgent userPrompt: {} ===", userPrompt);
        
        log.info("=== TranslationAgent OpenAI API 호출 시작 ===");
        return openAIClient.streamRawCompletion(systemPrompt, userPrompt)
            .doOnSubscribe(subscription -> log.info("=== TranslationAgent 스트림 구독 시작 ==="))
            .doOnNext(chunk -> log.info("=== TranslationAgent 원시 청크 받음: '{}' ===", chunk))
            .doOnError(error -> log.error("=== TranslationAgent 스트림 오류 ===", error))
            .doOnComplete(() -> log.info("=== TranslationAgent 스트림 완료 ==="))
            .collectList()
            .doOnSuccess(chunks -> log.info("=== TranslationAgent collectList 성공, chunks 수: {} ===", chunks.size()))
            .doOnError(error -> log.error("=== TranslationAgent collectList 오류 ===", error))
            .map(this::parseTranslationResponse)
            .doOnSuccess(response -> log.info("=== TranslationAgent 파싱 성공: {} ===", response))
            .doOnError(error -> log.error("=== TranslationAgent 파싱 오류 ===", error));
    }
    
    private String buildTranslationPrompt() {
        return """
            한국어 단어를 영어로 번역하고 발음기호를 제공하세요.
            
            JSON 형식:
            {
              "translations": [
                {"original": "한국어", "english": "English", "pronunciation": "[발음기호]"}
              ]
            }
            """;
    }
    
    private String buildUserPrompt(List<VocabularyAgentResponse.VocabularyWord> words) {
        StringBuilder prompt = new StringBuilder("다음 한국어 단어들을 번역해주세요:\n");
        for (VocabularyAgentResponse.VocabularyWord word : words) {
            prompt.append("- ").append(word.word());
            if (!word.context().isEmpty()) {
                prompt.append(" (문맥: ").append(word.context()).append(")");
            }
            prompt.append("\n");
        }
        return prompt.toString();
    }
    
    private TranslationAgentResponse parseTranslationResponse(List<String> chunks) {
        log.info("=== TranslationAgent 파싱 시작 ===");
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
                    log.debug("TranslationAgent 청크 파싱 실패 (무시): {}", e.getMessage());
                }
            }
            
            String fullResponse = contentBuilder.toString();
            log.info("=== TranslationAgent 추출된 content: '{}' ===", fullResponse);
            
            if (fullResponse.trim().isEmpty()) {
                log.info("=== TranslationAgent content가 비어있음 - 빈 응답 반환 ===");
                return new TranslationAgentResponse("translation", List.of());
            }
            
            JsonNode json = objectMapper.readTree(fullResponse);
            log.info("=== TranslationAgent JSON 파싱 성공: {} ===", json);
            
            List<TranslationAgentResponse.TranslatedWord> translations = new ArrayList<>();
            if (json.has("translations") && json.get("translations").isArray()) {
                log.info("=== TranslationAgent translations 배열 발견: {} ===", json.get("translations"));
                for (JsonNode transNode : json.get("translations")) {
                    String original = transNode.has("original") ? transNode.get("original").asText() : "";
                    String english = transNode.has("english") ? transNode.get("english").asText() : "";
                    String pronunciation = transNode.has("pronunciation") ? transNode.get("pronunciation").asText() : "";
                    
                    log.info("=== TranslationAgent 번역 처리: original='{}', english='{}', pronunciation='{}' ===", 
                            original, english, pronunciation);
                    
                    if (!original.isEmpty() && !english.isEmpty()) {
                        translations.add(new TranslationAgentResponse.TranslatedWord(original, english, pronunciation));
                        log.info("=== TranslationAgent 번역 추가됨 ===");
                    } else {
                        log.info("=== TranslationAgent 번역이 비어있어서 건너뜀 ===");
                    }
                }
            } else {
                log.info("=== TranslationAgent translations 배열이 없거나 배열이 아님 ===");
            }
            
            log.info("=== TranslationAgent 파싱 결과: {} 번역 추출됨 ===", translations.size());
            return new TranslationAgentResponse("translation", translations);
        } catch (Exception e) {
            log.error("=== TranslationAgent 응답 파싱 중 오류 발생: {} ===", e.getMessage(), e);
            return new TranslationAgentResponse("translation", List.of());
        }
    }
}
