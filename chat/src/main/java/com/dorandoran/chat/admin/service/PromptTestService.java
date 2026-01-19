package com.dorandoran.chat.admin.service;

import com.dorandoran.chat.admin.dto.PromptTestRequest;
import com.dorandoran.chat.admin.dto.PromptTestResponse;
import com.dorandoran.chat.service.agent.*;
import com.dorandoran.chat.service.GreetingService;
import com.dorandoran.chat.service.OpenAIClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

/**
 * 프롬프트 테스트 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PromptTestService {

    private final IntimacyAnalysisAgent intimacyAnalysisAgent;
    private final IntimacyCorrectionAgent intimacyCorrectionAgent;
    private final VocabularyExtractionAgent vocabularyExtractionAgent;
    private final VocabularyExplanationAgent vocabularyExplanationAgent;
    private final ConversationAgent conversationAgent;
    private final GreetingService greetingService;
    private final ObjectMapper objectMapper;

    /**
     * 프롬프트 테스트 실행
     */
    public Mono<PromptTestResponse> testPrompt(PromptTestRequest request) {
        log.info("=== [PromptTestService] 프롬프트 테스트 시작 ===");
        log.info("  - agentType: {}", request.getAgentType());
        log.info("  - concept: {}", request.getConcept());
        log.info("  - intimacyLevel: {}", request.getIntimacyLevel());
        log.info("  - inputText: {}", request.getInputText());
        log.info("  - ⚠️ 주의: Agent는 resources/prompts/ 디렉토리의 파일을 읽습니다.");
        
        Instant startTime = Instant.now();
        
        return switch (request.getAgentType().toUpperCase()) {
            case "INTIMACY_ANALYSIS" -> {
                log.info("  - INTIMACY_ANALYSIS Agent 실행");
                yield testIntimacyAnalysis(request)
                    .doOnNext(result -> log.info("  - INTIMACY_ANALYSIS 결과: detectedLevel={}, problematicExpressions={}개", 
                        result.detectedLevel(), result.problematicExpressions().size()))
                    .map(result -> buildResponse(result.toString(), startTime, null));
            }
            case "INTIMACY_CORRECTION" -> {
                log.info("  - INTIMACY_CORRECTION Agent 실행 (Analysis 먼저 실행)");
                yield testIntimacyCorrection(request)
                    .doOnNext(result -> log.info("  - INTIMACY_CORRECTION 결과 수신"))
                    .map(result -> buildResponse(result.toString(), startTime, null));
            }
            case "VOCABULARY_EXTRACTION" -> {
                log.info("  - VOCABULARY_EXTRACTION Agent 실행");
                yield testVocabularyExtraction(request)
                    .doOnNext(result -> log.info("  - VOCABULARY_EXTRACTION 결과: {}", result != null ? "추출 성공" : "추출 없음"))
                    .map(result -> buildResponse(result != null ? result.toString() : "추출된 어휘 없음", startTime, null));
            }
            case "VOCABULARY_EXPLANATION" -> {
                log.info("  - VOCABULARY_EXPLANATION Agent 실행 (Extraction 먼저 실행)");
                yield testVocabularyExplanation(request)
                    .doOnNext(result -> log.info("  - VOCABULARY_EXPLANATION 결과 수신"))
                    .map(result -> buildResponse(result.toString(), startTime, null));
            }
            case "CONVERSATION" -> {
                log.warn("  - CONVERSATION Agent는 채팅방 컨텍스트가 필요합니다.");
                yield testConversation(request)
                    .map(result -> buildResponse(result, startTime, null));
            }
            case "GREETING" -> {
                log.warn("  - GREETING Agent는 채팅방 컨텍스트가 필요합니다.");
                yield testGreeting(request)
                    .map(result -> buildResponse(result, startTime, null));
            }
            default -> {
                log.error("  - 지원하지 않는 Agent 타입: {}", request.getAgentType());
                yield Mono.error(new IllegalArgumentException("Unsupported agent type: " + request.getAgentType()));
            }
        };
    }

    private Mono<com.dorandoran.chat.service.agent.IntimacyAnalysisResult> testIntimacyAnalysis(PromptTestRequest request) {
        log.info("    [PromptTestService] IntimacyAnalysisAgent.analyze() 호출");
        log.info("      - 예상 프롬프트 파일: prompts/intimacy/analysis/{}_{}.txt", 
            request.getConcept().toLowerCase(), request.getIntimacyLevel());
        long agentStartTime = System.currentTimeMillis();
        
        return intimacyAnalysisAgent.analyze(
            request.getInputText(),
            request.getConcept(),
            request.getIntimacyLevel(),
            null
        )
        .doOnNext(result -> {
            long agentEndTime = System.currentTimeMillis();
            log.info("    [PromptTestService] IntimacyAnalysisAgent.analyze() 완료");
            log.info("      - Agent 실행 시간: {}ms", agentEndTime - agentStartTime);
            log.info("      - detectedLevel: {}", result.detectedLevel());
            log.info("      - problematicExpressions 개수: {}", result.problematicExpressions().size());
        })
        .doOnError(error -> {
            log.error("    [PromptTestService] IntimacyAnalysisAgent.analyze() 실패", error);
        });
    }

    private Mono<com.dorandoran.chat.service.agent.IntimacyCorrectionResult> testIntimacyCorrection(PromptTestRequest request) {
        // Correction은 Analysis 결과가 필요하므로, 먼저 Analysis를 실행
        return intimacyAnalysisAgent.analyze(
            request.getInputText(),
            request.getConcept(),
            request.getIntimacyLevel(),
            null
        ).flatMap(analysisResult -> 
            intimacyCorrectionAgent.generateCorrection(
                analysisResult,
                request.getConcept(),
                request.getIntimacyLevel(),
                null
            )
        );
    }

    private Mono<com.dorandoran.chat.service.agent.VocabularyExtractionResult> testVocabularyExtraction(PromptTestRequest request) {
        return vocabularyExtractionAgent.extract(
            request.getInputText(),
            request.getConcept(),
            request.getIntimacyLevel(),
            null
        );
    }

    private Mono<com.dorandoran.chat.service.agent.VocabularyExplanationResult> testVocabularyExplanation(PromptTestRequest request) {
        // Explanation은 Extraction 결과가 필요하므로, 먼저 Extraction을 실행
        return vocabularyExtractionAgent.extract(
            request.getInputText(),
            request.getConcept(),
            request.getIntimacyLevel(),
            null
        ).flatMap(extractionResult -> {
            if (extractionResult == null) {
                return Mono.error(new IllegalArgumentException("Extraction result is null"));
            }
            return vocabularyExplanationAgent.generateExplanation(
                extractionResult,
                request.getConcept(),
                request.getIntimacyLevel(),
                null
            );
        });
    }

    private Mono<String> testConversation(PromptTestRequest request) {
        // Conversation은 chatroomId와 메시지 히스토리가 필요하므로 실제 테스트는 복잡함
        // 여기서는 간단히 입력 텍스트를 그대로 반환
        // TODO: 실제 ConversationAgent 테스트를 위해서는 임시 채팅방 생성 및 메시지 히스토리 구성 필요
        return Mono.just("Conversation Agent 테스트는 채팅방 컨텍스트가 필요합니다. 실제 채팅에서 테스트해주세요.");
    }

    private Mono<String> testGreeting(PromptTestRequest request) {
        // Greeting은 chatroomId가 필요하므로 실제 테스트는 복잡함
        // 여기서는 간단히 입력 텍스트를 그대로 반환
        // TODO: 실제 GreetingService 테스트를 위해서는 임시 채팅방 생성 필요
        return Mono.just("Greeting Agent 테스트는 채팅방 컨텍스트가 필요합니다. 실제 채팅에서 테스트해주세요.");
    }

    private PromptTestResponse buildResponse(String outputText, Instant startTime, Integer tokens) {
        long latencyMs = Duration.between(startTime, Instant.now()).toMillis();
        return PromptTestResponse.builder()
            .outputText(outputText)
            .latencyMs(latencyMs)
            .tokens(tokens)
            .build();
    }
}
