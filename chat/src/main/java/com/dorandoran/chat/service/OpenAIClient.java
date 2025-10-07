package com.dorandoran.chat.service;

import com.dorandoran.chat.config.AIConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

@Component
@RequiredArgsConstructor
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
        Map<String, Object> req = Map.of(
            "model", aiConfig.getModel(),
            "stream", true,
            "max_tokens", aiConfig.getMaxOutputTokens(),
            "temperature", 0.7,
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
            .filter(s -> s != null && !s.isEmpty() && s.startsWith("data: "));
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

    public record Usage(int inputTokens, int outputTokens) {
        public static Usage empty() { return new Usage(0, 0); }
        public boolean isEmpty() { return inputTokens == 0 && outputTokens == 0; }
    }
}


