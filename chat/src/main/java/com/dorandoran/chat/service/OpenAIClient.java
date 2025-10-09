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
        log.info("=== OpenAIClient.streamRawCompletion() 호출됨 ===");
        log.info("=== systemPrompt: {} ===", systemPrompt);
        log.info("=== userContent: {} ===", userContent);
        
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

        log.info("=== OpenAI API 요청 시작 ===");
        return webClient()
            .post()
            .uri("/v1/chat/completions")
            .body(BodyInserters.fromValue(req))
            .accept(MediaType.TEXT_EVENT_STREAM, MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToFlux(String.class)
            .doOnSubscribe(subscription -> log.info("=== OpenAI 스트림 구독 시작 ==="))
            .doOnNext(s -> log.info("=== OpenAI 원시 응답: {} ===", s))
            .doOnError(error -> log.error("=== OpenAI 스트림 오류 ===", error))
            .doOnComplete(() -> log.info("=== OpenAI 스트림 완료 ==="))
            .filter(s -> {
                boolean isValid = s != null && !s.isEmpty() && !s.equals("[DONE]");
                log.info("=== OpenAI 필터링: '{}' -> {} ===", s, isValid);
                return isValid;
            });
    }

    /** 텍스트 청크만 추출 */
    public Flux<String> extractText(String raw) {
        log.info("extractText 호출됨: raw='{}'", raw);
        try {
            // OpenAI Chat Completions Stream의 JSON 라인에서 텍스트 조각 경로를 탐색
            String jsonData = raw.startsWith("data: ") ? raw.substring(6) : raw;
            if ("[DONE]".equals(jsonData.trim())) {
                log.info("OpenAI 스트림 완료: [DONE]");
                return Flux.empty();
            }
            
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(jsonData);
            log.info("JSON 파싱 성공: {}", node);
            
            // OpenAI Chat Completions 스트림 형식: { "choices": [{"delta": {"content": "..."}}] }
            if (node.has("choices")) {
                log.info("OpenAI choices 발견: {}", node.get("choices"));
                return Flux.fromIterable(node.get("choices"))
                    .map(choice -> {
                        log.info("OpenAI choice 처리: {}", choice);
                        return choice.get("delta");
                    })
                    .filter(delta -> {
                        boolean hasContent = delta != null && delta.has("content");
                        log.info("OpenAI delta 필터링: {}, hasContent: {}", delta, hasContent);
                        return hasContent;
                    })
                    .map(delta -> {
                        String content = delta.get("content").asText();
                        log.info("OpenAI 텍스트 추출 성공: '{}'", content);
                        return content;
                    })
                    .filter(content -> {
                        boolean notEmpty = content != null && !content.isEmpty();
                        log.info("OpenAI 텍스트 최종 필터링: '{}', notEmpty: {}", content, notEmpty);
                        return notEmpty;
                    });
            }
            
            log.info("OpenAI choices 없음: {}", node);
            return Flux.empty();
        } catch (Exception e) {
            log.error("OpenAI 응답 파싱 오류: {}, raw: {}", e.getMessage(), raw);
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


