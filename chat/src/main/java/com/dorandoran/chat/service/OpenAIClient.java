package com.dorandoran.chat.service;

import com.dorandoran.chat.config.AIConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OpenAIClient {

    private final AIConfig aiConfig;

    private WebClient webClient() {
        return WebClient.builder()
            .baseUrl(aiConfig.getBaseUrl())
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + aiConfig.getApiKey())
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    /**
     * OpenAI Chat Completions API (stream=true) 호출 - RAW 라인 스트림
     */
    public Flux<String> streamRawCompletion(String systemPrompt, String userContent) {
        log.info("OpenAI API 요청 시작");
        
        // 프롬프트 검증 및 로깅
        if (systemPrompt == null || systemPrompt.isBlank()) {
            log.warn("⚠️⚠️⚠️ OpenAIClient: systemPrompt가 null이거나 비어있습니다! 빈 문자열로 전송됩니다.");
        } else {
            log.debug("OpenAIClient: systemPrompt 길이={}, 내용 (처음 200자)={}", 
                systemPrompt.length(), systemPrompt.substring(0, Math.min(200, systemPrompt.length())));
        }
        
        Map<String, Object> req = Map.of(
            "model", aiConfig.getModel(),
            "stream", true,
            "max_tokens", aiConfig.getMaxOutputTokens(),
            "temperature", 0.85,
            "messages", new Object[]{
                Map.of(
                    "role", "system",
                    "content", systemPrompt == null ? "" : systemPrompt
                ),
                Map.of(
                    "role", "user",
                    "content", userContent
                )
            }
        );

        return webClient()
            .post()
            .uri("/v1/chat/completions")
            .body(BodyInserters.fromValue(req))
            .accept(MediaType.TEXT_EVENT_STREAM, MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToFlux(String.class)
            .doOnError(error -> log.error("OpenAI 스트림 오류: {}", error.getMessage()))
            .filter(s -> s != null && !s.isEmpty())
            .takeWhile(s -> !"[DONE]".equals(s.trim()));
    }

    /** 텍스트 청크만 추출 */
    public Flux<String> extractText(String raw) {
        try {
            // OpenAI Chat Completions Stream의 JSON 라인에서 텍스트 조각 경로를 탐색
            String jsonData = raw.startsWith("data: ") ? raw.substring(6) : raw;
            if ("[DONE]".equals(jsonData.trim())) {
                return Flux.empty();
            }
            
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(jsonData);
            
            // OpenAI Chat Completions 스트림 형식: { "choices": [{"delta": {"content": "..."}}] }
            if (node.has("choices")) {
                return Flux.fromIterable(node.get("choices"))
                    .map(choice -> choice.get("delta"))
                    .filter(delta -> delta != null && delta.has("content"))
                    .map(delta -> delta.get("content").asText())
                    .filter(content -> content != null && !content.isEmpty());
            }
            
            return Flux.empty();
        } catch (Exception e) {
            log.debug("OpenAI 응답 파싱 오류: {}", e.getMessage());
            return Flux.empty();
        }
    }

    /** 사용량(토큰) 추출 */
    public Usage extractUsage(String raw) {
        try {
            String jsonData = raw.startsWith("data: ") ? raw.substring(6) : raw;
            if ("[DONE]".equals(jsonData.trim())) {
                return Usage.empty();
            }
            
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(jsonData);
            
            // OpenAI Chat Completions 스트림에서 usage 정보 추출
            if (node.has("usage")) {
                JsonNode u = node.get("usage");
                int in = u.has("prompt_tokens") ? u.get("prompt_tokens").asInt(0) : 0;
                int out = u.has("completion_tokens") ? u.get("completion_tokens").asInt(0) : 0;
                return new Usage(in, out);
            }
        } catch (Exception ignored) {}
        return Usage.empty();
    }

    /**
     * 동기 방식 OpenAI 완료 호출
     * GreetingService 등 일회성 호출에 사용
     * 
     * @param systemPrompt 시스템 프롬프트
     * @param userMessage 사용자 메시지
     * @return AI 응답 텍스트
     */
    public String simpleCompletion(String systemPrompt, String userMessage) {
        log.info("=== OpenAIClient.simpleCompletion() ===");
        log.info("systemPrompt 길이={}자", systemPrompt != null ? systemPrompt.length() : 0);
        log.info("systemPrompt 내용 (처음 500자): {}", 
            systemPrompt != null ? systemPrompt.substring(0, Math.min(500, systemPrompt.length())) : "null");
        if (systemPrompt != null && systemPrompt.length() > 500) {
            log.info("systemPrompt 내용 (마지막 300자): {}", 
                systemPrompt.substring(Math.max(0, systemPrompt.length() - 300)));
        }
        log.info("userMessage={}", userMessage);
        
        try {
            List<String> chunks = streamRawCompletion(systemPrompt, userMessage)
                .flatMap(this::extractText)
                .collectList()
                .block(Duration.ofSeconds(30));  // 30초 타임아웃 설정
            
            if (chunks == null || chunks.isEmpty()) {
                log.warn("⚠️⚠️⚠️ OpenAI 응답이 비어있습니다");
                return "";
            }
            
            String result = String.join("", chunks);
            log.info("OpenAI 동기 호출 완료: 응답 길이={}자", result.length());
            log.info("OpenAI 응답 내용 (전체): {}", result);
            return result;
            
        } catch (Exception e) {
            log.error("⚠️⚠️⚠️ OpenAI 동기 호출 실패", e);
            throw new RuntimeException("AI 응답 생성 실패", e);
        }
    }

    /**
     * OpenAI Chat Completions API (stream=true) - 히스토리 포함
     * @param systemPrompt 시스템 프롬프트
     * @param messageHistory 대화 히스토리 List<Map<String, String>> 형식
     * @param userContent 현재 사용자 메시지
     */
    public Flux<String> streamRawCompletionWithHistory(
            String systemPrompt,
            List<Map<String, String>> messageHistory,
            String userContent) {
        log.info("OpenAI API 요청 시작 (히스토리 포함): {} 개 메시지", messageHistory.size());
        
        // messages 배열 구성: [system, ...history, user]
        List<Object> messages = new ArrayList<>();
        
        // 1. system 메시지
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(Map.of("role", "system", "content", systemPrompt));
        }
        
        // 2. 히스토리
        for (Map<String, String> msg : messageHistory) {
            String role = msg.get("role");
            String content = msg.get("content");
            if (role != null && content != null) {
                messages.add(Map.of("role", role, "content", content));
            }
        }
        
        // 3. 사용자 메시지
        messages.add(Map.of("role", "user", "content", userContent));
        
        Map<String, Object> req = Map.of(
            "model", aiConfig.getModel(),
            "stream", true,
            "max_tokens", aiConfig.getMaxOutputTokens(),
            "temperature", 0.85,
            "messages", messages.toArray()
        );

        return webClient()
            .post()
            .uri("/v1/chat/completions")
            .body(BodyInserters.fromValue(req))
            .accept(MediaType.TEXT_EVENT_STREAM, MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToFlux(String.class)
            .doOnError(error -> log.error("OpenAI 스트림 오류: {}", error.getMessage()))
            .filter(s -> s != null && !s.isEmpty())
            .takeWhile(s -> !"[DONE]".equals(s.trim()));
    }

    public record Usage(int inputTokens, int outputTokens) {
        public static Usage empty() { return new Usage(0, 0); }
        public boolean isEmpty() { return inputTokens == 0 && outputTokens == 0; }
    }
}


