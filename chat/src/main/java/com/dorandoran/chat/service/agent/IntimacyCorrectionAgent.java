package com.dorandoran.chat.service.agent;

import com.dorandoran.chat.config.AIConfig;
import com.dorandoran.chat.service.OpenAIClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * IntimacyCorrectionAgent
 * 교정 및 대안 표현 제안
 * Temperature: 0.5 (자연스러움)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IntimacyCorrectionAgent {
    private final OpenAIClient openAIClient;
    private final AIConfig aiConfig;
    private final ObjectMapper objectMapper;
    
    /**
     * 교정 및 대안 표현 제안 생성
     * 
     * @param analysisResult 분석 결과
     * @param concept 컨셉 (FRIEND, COWORKER, BOSS, SENIOR, HONEY)
     * @param intimacyLevel 친밀도 레벨 (1-3)
     * @return 교정 결과
     */
    public Mono<IntimacyCorrectionResult> generateCorrection(
        IntimacyAnalysisResult analysisResult,
        String concept,
        int intimacyLevel
    ) {
        return generateCorrection(analysisResult, concept, intimacyLevel, null);
    }
    
    public Mono<IntimacyCorrectionResult> generateCorrection(
        IntimacyAnalysisResult analysisResult,
        String concept,
        int intimacyLevel,
        UUID chatroomId
    ) {
        log.info("=== IntimacyCorrectionAgent.generateCorrection() 호출됨 ===");
        log.info("=== 파라미터 - concept='{}', intimacyLevel={}, chatroomId={} ===", concept, intimacyLevel, chatroomId);
        
        String systemPrompt = buildCorrectionPrompt(concept, intimacyLevel, analysisResult);
        Double temperature = aiConfig.getAgents().getIntimacy().getCorrection().getTemperature();
        Integer maxTokens = aiConfig.getAgents().getIntimacy().getCorrection().getMaxTokens();
        
        // 사용자 메시지 재구성 (분석 결과 기반)
        String userMessage = buildUserMessageFromAnalysis(analysisResult);
        
        log.info("=== IntimacyCorrectionAgent OpenAI API 호출 시작 (temperature={}, maxTokens={}) ===",
            temperature, maxTokens);
        
        return openAIClient.streamRawCompletion(systemPrompt, userMessage, temperature, maxTokens, chatroomId)
            .collectList()
            .map(this::parseCorrectionResponse)
            .map(result -> {
                // 사전 관련 코드 제거 - 빈 alternativeExpressions 반환
                return new IntimacyCorrectionResult(
                    result.correctedSentence(),
                    result.feedback(),
                    result.corrections(),
                    List.of()  // 빈 리스트
                );
            })
            .doOnNext(result -> {
                log.info("=== IntimacyCorrectionAgent 교정 결과 ===");
                log.info("  - correctedSentence: '{}'", result.correctedSentence());
                log.info("  - feedback.ko: '{}'", result.feedback().ko());
                log.info("  - alternativeExpressions: {} 개", result.alternativeExpressions().size());
            })
            .onErrorResume(error -> {
                log.error("IntimacyCorrectionAgent 처리 오류", error);
                return Mono.just(new IntimacyCorrectionResult("", new FeedbackText("", ""), List.of(), List.of()));
            });
    }
    
    /**
     * 교정 프롬프트 생성
     */
    private String buildCorrectionPrompt(String concept, int intimacyLevel, IntimacyAnalysisResult analysisResult) {
        String originalSentence = analysisResult.originalMessage();
        String problemExpression = analysisResult.problematicExpressions().isEmpty() ? "없음" : 
            analysisResult.problematicExpressions().get(0).original();
        
        // 파일에서 프롬프트 로드 시도
        String promptTemplate = loadCorrectionPromptFromFile(concept, intimacyLevel);
        if (promptTemplate != null && !promptTemplate.isEmpty()) {
            log.info("=== IntimacyCorrectionAgent: 프롬프트 파일 로드 성공 - concept={}, level={} ===", 
                concept, intimacyLevel);
            
            // 플레이스홀더 치환
            String prompt = promptTemplate
                .replace("{originalSentence}", originalSentence)
                .replace("{문제 표현이 여기에 동적으로 삽입됨}", problemExpression);
            
            // problematicExpressions가 비어있어도 컨셉 위반 가능성 있으므로 지시 추가
            if (analysisResult.problematicExpressions().isEmpty()) {
                prompt += "\n\n⚠️ 중요: 원문이 컨셉 제약을 위반하는 경우(예: FRIEND 컨셉인데 존댓말 사용) 반드시 교정하세요.";
            }
            
            return prompt;
        }
        
        // 파일 로드 실패 시 fallback (기존 하드코딩 메서드 사용)
        log.warn("Correction 프롬프트 파일 로드 실패, fallback 사용: concept={}, intimacyLevel={}", concept, intimacyLevel);
        String toneGuideline = getToneGuideline(concept, intimacyLevel);
        
        return String.format("""
            **역할 설명:**
            
            너는 추출된 문제 표현을 컨셉과 친밀도 레벨에 맞게 교정하는 전문가 역할을 수행할거야.
            
            **입력 정보:**
            - 컨셉: %s
            - 친밀도 레벨: %d
            - 문제 표현: %s
            
            **말투 규칙:**
            %s
            
            **교정 규칙:**
            1. 컨셉과 친밀도 레벨에 맞는 말투로 교정
            2. 문제 표현이 없으면 원문 유지
            
            **JSON 형식:**
            
            {
              "correctedSentence": "교정된 문장 또는 원문",
              "feedback": {
                "ko": "한국어 피드백 (150자 내)",
                "en": "English feedback (within 350 words)"
              },
              "corrections": "변경사항 설명"
            }
            
            **주의사항:**
            - 교정이 불필요하면 원문 유지
            - JSON 형식 외의 텍스트는 출력하지 말 것
            """, concept, intimacyLevel, problemExpression, toneGuideline);
    }
    
    /**
     * 파일에서 교정 프롬프트 로드
     */
    private String loadCorrectionPromptFromFile(String concept, int intimacyLevel) {
        if (concept == null) {
            concept = "FRIEND";
        }
        String normalizedConcept = concept.toUpperCase();
        String filename = String.format("prompts/intimacy/correction/%s_%d.txt",
            normalizedConcept.toLowerCase(), intimacyLevel);
        
        try {
            ClassPathResource resource = new ClassPathResource(filename);
            if (!resource.exists()) {
                log.debug("IntimacyCorrectionAgent: 프롬프트 파일 없음 - {}", filename);
                return null;
            }
            
            String content = resource.getContentAsString(StandardCharsets.UTF_8);
            log.info("IntimacyCorrectionAgent: 프롬프트 파일 로드 성공 - {}, 길이={}자", 
                filename, content.length());
            return content;
            
        } catch (IOException e) {
            log.error("IntimacyCorrectionAgent: 프롬프트 파일 로드 실패 - {}", filename, e);
            return null;
        }
    }
    
    /**
     * 말투 규칙 생성
     */
    private String getToneGuideline(String concept, int intimacyLevel) {
        if (concept == null) {
            concept = "FRIEND";
        }
        String normalizedConcept = concept.toUpperCase();
        
        return switch (normalizedConcept) {
            case "FRIEND" -> """
                **FRIEND 컨셉 말투 규칙:**
                - Level 1, 3 모두 반말 사용 필수
                - 친근하고 자연스러운 반말 표현
                - 예: "밥 먹었어?", "밥 먹었니?", "밥 먹었지?"
                """;
            case "COWORKER", "BOSS" -> """
                **COWORKER/BOSS 컨셉 말투 규칙:**
                - 모든 레벨에서 존댓말 사용 필수
                - 정중하고 업무에 맞는 표현
                - 예: "식사하셨어요?", "드셨어요?", "식사하셨나요?"
                """;
            case "SENIOR" -> """
                **SENIOR 컨셉 말투 규칙:**
                - 모든 레벨에서 존댓말 사용 필수 (Level 1도 부드러운 존댓말)
                - 반말 사용 절대 금지
                - 선후배 관계에 맞는 정중한 표현
                - 예: "식사하셨어요?", "드셨어요?", "과제 제출하셨어요?"
                """;
            case "HONEY" -> """
                **HONEY 컨셉 말투 규칙:**
                - Level 1: 존댓말 사용
                - Level 3: 반말 사용 필수
                - 애정 표현 자유롭게 사용
                """;
            default -> """
                **일반 말투 규칙:**
                - 친밀도 레벨에 맞는 적절한 표현 사용
                """;
        };
    }
    
    /**
     * 분석 결과에서 사용자 메시지 재구성
     * 원문 전체와 problematicExpressions 배열 전체를 JSON으로 전달
     * problematicExpressions가 비어있어도 원문 전체를 전달 (프롬프트 파일이 원문을 요구하므로)
     */
    private String buildUserMessageFromAnalysis(IntimacyAnalysisResult analysisResult) {
        try {
            // JSON 객체 생성
            ObjectMapper mapper = new ObjectMapper();
            com.fasterxml.jackson.databind.node.ObjectNode json = mapper.createObjectNode();
            
            // originalSentence 추가 (항상 포함)
            json.put("originalSentence", analysisResult.originalMessage());
            
            // problematicExpressions 배열 추가 (비어있으면 빈 배열)
            com.fasterxml.jackson.databind.node.ArrayNode problemsArray = mapper.createArrayNode();
            for (ProblematicExpression expr : analysisResult.problematicExpressions()) {
                com.fasterxml.jackson.databind.node.ObjectNode problemNode = mapper.createObjectNode();
                problemNode.put("original", expr.original());
                problemNode.put("type", expr.type());
                problemNode.put("reason", expr.reason());
                problemNode.put("suggestionHint", expr.suggestionHint() != null ? expr.suggestionHint() : "");
                problemsArray.add(problemNode);
            }
            json.set("problematicExpressions", problemsArray);
            
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        } catch (Exception e) {
            log.error("buildUserMessageFromAnalysis JSON 생성 실패", e);
            // Fallback: 원문과 문제 표현을 텍스트로 전달
            return String.format("원문: \"%s\"\n문제 표현: %s", 
                analysisResult.originalMessage(),
                analysisResult.problematicExpressions().isEmpty() ? "없음" : 
                    analysisResult.problematicExpressions().get(0).original());
        }
    }
    
    /**
     * 교정 응답 파싱
     */
    private IntimacyCorrectionResult parseCorrectionResponse(List<String> chunks) {
        log.info("=== IntimacyCorrectionAgent 파싱 시작: {} 개 청크 ===", chunks.size());
        try {
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
                    log.debug("IntimacyCorrectionAgent 청크 파싱 실패 (무시): {}", e.getMessage());
                }
            }
            
            String fullResponse = contentBuilder.toString();
            log.info("IntimacyCorrectionAgent 원시 응답: '{}'", fullResponse);
            
            if (fullResponse.trim().isEmpty()) {
                log.warn("IntimacyCorrectionAgent 빈 응답 - 기본값 반환");
                return new IntimacyCorrectionResult("", new FeedbackText("", ""), List.of(), List.of());
            }
            
            JsonNode json = objectMapper.readTree(fullResponse);
            log.info("IntimacyCorrectionAgent JSON 파싱 성공");
            
            String correctedSentence = json.has("correctedSentence") ? json.get("correctedSentence").asText() : "";
            
            // corrections 배열 파싱
            List<Correction> corrections = new ArrayList<>();
            if (json.has("corrections") && json.get("corrections").isArray()) {
                for (JsonNode correctionNode : json.get("corrections")) {
                    corrections.add(new Correction(
                        correctionNode.has("from") ? correctionNode.get("from").asText() : "",
                        correctionNode.has("to") ? correctionNode.get("to").asText() : "",
                        correctionNode.has("reason") ? correctionNode.get("reason").asText() : ""
                    ));
                }
            }
            
            FeedbackText feedback = new FeedbackText("", "");
            if (json.has("feedback") && json.get("feedback").isObject()) {
                JsonNode feedbackNode = json.get("feedback");
                String ko = feedbackNode.has("ko") ? feedbackNode.get("ko").asText() : "";
                String en = feedbackNode.has("en") ? feedbackNode.get("en").asText() : "";
                feedback = new FeedbackText(ko, en);
            }
            
            log.info("=== IntimacyCorrectionAgent 파싱 완료 ===");
            log.info("  - correctedSentence: '{}'", correctedSentence);
            log.info("  - feedback.ko: '{}'", feedback.ko());
            log.info("  - corrections: {} 개", corrections.size());
            
            return new IntimacyCorrectionResult(correctedSentence, feedback, corrections, List.of());
        } catch (Exception e) {
            log.error("IntimacyCorrectionAgent 응답 파싱 실패", e);
            return new IntimacyCorrectionResult("", new FeedbackText("", ""), "", List.of());
        }
    }
}

