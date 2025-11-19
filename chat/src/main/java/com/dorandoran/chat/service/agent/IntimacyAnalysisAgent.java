package com.dorandoran.chat.service.agent;

import com.dorandoran.chat.config.AIConfig;
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
 * IntimacyAnalysisAgent
 * 사용자 메시지에서 문제 표현 및 제안 키워드 추출
 * Temperature: 0.2 (정확도 우선)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IntimacyAnalysisAgent {
    private final OpenAIClient openAIClient;
    private final AIConfig aiConfig;
    private final ObjectMapper objectMapper;
    
    /**
     * 사용자 메시지 분석
     * 
     * @param userMessage 사용자 메시지
     * @param concept 컨셉 (FRIEND, COWORKER, BOSS, SENIOR, HONEY)
     * @param intimacyLevel 친밀도 레벨 (1-3)
     * @return 분석 결과
     */
    public Mono<IntimacyAnalysisResult> analyze(String userMessage, String concept, int intimacyLevel) {
        log.info("=== IntimacyAnalysisAgent.analyze() 호출됨 ===");
        log.info("=== 파라미터 - userMessage='{}', concept='{}', intimacyLevel={} ===",
            userMessage, concept, intimacyLevel);
        
        String systemPrompt = buildAnalysisPrompt(concept, intimacyLevel);
        Double temperature = aiConfig.getAgents().getIntimacy().getAnalysis().getTemperature();
        Integer maxTokens = aiConfig.getAgents().getIntimacy().getAnalysis().getMaxTokens();
        
        log.info("=== IntimacyAnalysisAgent OpenAI API 호출 시작 (temperature={}, maxTokens={}) ===",
            temperature, maxTokens);
        
        return openAIClient.streamRawCompletion(systemPrompt, userMessage, temperature, maxTokens)
            .collectList()
            .map(this::parseAnalysisResponse)
            .doOnNext(result -> {
                log.info("=== IntimacyAnalysisAgent 분석 결과 ===");
                log.info("  - detectedLevel: {}", result.detectedLevel());
                log.info("  - problematicExpressions: {}", result.problematicExpressions().size());
            })
            .map(result -> {
                // 사전 관련 코드 제거됨
                return result;
            })
            .onErrorResume(error -> {
                log.error("IntimacyAnalysisAgent 처리 오류", error);
                return Mono.just(new IntimacyAnalysisResult(1, List.of()));
            });
    }
    
    /**
     * 분석 프롬프트 생성
     */
    private String buildAnalysisPrompt(String concept, int intimacyLevel) {
        String conceptCriteria = getConceptExtractionCriteria(concept, intimacyLevel);
        
        return String.format("""
            **역할 설명:**
            
            너는 외국인의 한국어 메시지를 분석하여 친밀도 레벨을 감지하고, 컨셉에 맞지 않는 표현을 찾는 전문가 역할을 수행할거야.
            
            **입력 정보:**
            - 분석 대상 문장 (사용자 메시지): 사용자 입력 메시지
            - 컨셉: %s
            - 친밀도 레벨: %d
            
            **컨셉별 추출 기준:**
            %s
            
            **분석 규칙:**
            1. 사용자 메시지의 친밀도 레벨을 감지하여 detectedLevel 반환 (1 또는 3)
            2. 컨셉에 맞지 않는 표현이 있으면 problematicExpressions에 추가
               - FRIEND 컨셉에서 존댓말 사용 → INFORMAL_TO_FORMAL
               - COWORKER/BOSS/SENIOR 컨셉에서 반말 사용 → FORMAL_TO_INFORMAL
            3. 문제가 없는 경우 빈 배열 반환
            
            **JSON 형식:**
            
            {
              "detectedLevel": 1 또는 3,
              "problematicExpressions": [
                {
                  "original": "원본 표현",
                  "type": "INFORMAL_TO_FORMAL 또는 FORMAL_TO_INFORMAL 또는 STYLE_MISMATCH",
                  "reason": "문제 이유 설명"
                }
              ]
            }
            
            **주의사항:**
            - detectedLevel은 반드시 1 또는 3만 사용
            - 문제 표현이 없으면 빈 배열 반환
            - JSON 형식 외의 텍스트는 출력하지 말 것
            """, concept, intimacyLevel, conceptCriteria);
    }
    
    /**
     * 컨셉별 추출 기준 생성
     */
    private String getConceptExtractionCriteria(String concept, int intimacyLevel) {
        if (concept == null) {
            concept = "FRIEND";
        }
        String normalizedConcept = concept.toUpperCase();
        
        return switch (normalizedConcept) {
            case "FRIEND" -> """
                **FRIEND 컨셉 추출 기준:**
                
                1. **반말 사용 필수**
                   - Level 1, 3 모두 반말 사용
                   - 존댓말 사용 시 문제 표현으로 감지
                
                2. **MZ세대 표현 우선**
                   - 키워드: "국룰", "레알", "개좋아", "완전", "진짜"
                   - 유행어, 인터넷 신조어
                
                3. **연령층 표현**
                   - 키워드: "밥 먹었어?", "뭐 해?", "어디 가?"
                   - 친근한 반말 표현
                
                **맥락 분석 규칙:**
                - "밥", "먹다" 등이 나오면 → "식사" 키워드 매칭
                - "회의", "미팅" 등이 나오면 → "회의" 키워드 매칭
                """;
            case "COWORKER", "BOSS" -> """
                **COWORKER/BOSS 컨셉 추출 기준:**
                
                1. **존댓말 사용 필수**
                   - 모든 레벨에서 존댓말 사용
                   - 반말 사용 시 문제 표현으로 감지
                
                2. **회사 용어 우선**
                   - 키워드: "식사", "회의", "보고", "결재", "품의"
                   - 업무 관련 전문 용어
                
                3. **격식 표현**
                   - 키워드: "송구스럽습니다", "말씀드리겠습니다"
                   - 공식적이고 정중한 표현
                
                **맥락 분석 규칙:**
                - "밥", "먹다" 등이 나오면 → "식사" 키워드 매칭
                - "회의", "미팅" 등이 나오면 → "회의" 키워드 매칭
                """;
            case "SENIOR" -> """
                **SENIOR 컨셉 추출 기준:**
                
                1. **존댓말 사용 필수**
                   - 모든 레벨에서 존댓말 사용 (Level 1도 부드러운 존댓말)
                   - 반말 사용 시 문제 표현으로 감지
                
                2. **대학교 용어 우선**
                   - 키워드: "과제", "수강", "출석", "족보", "전공"
                   - 대학교 생활 관련 용어
                
                3. **격식 표현**
                   - 키워드: "말씀드리겠습니다", "드리겠습니다"
                   - 선후배 관계에 맞는 정중한 표현
                
                **맥락 분석 규칙:**
                - "과제", "수강" 등이 나오면 → 대학교 용어 키워드 매칭
                """;
            case "HONEY" -> """
                **HONEY 컨셉 추출 기준:**
                
                1. **Level 1: 존댓말, Level 3: 반말**
                   - Level 1에서 반말 사용 시 문제 표현으로 감지
                   - Level 3에서 존댓말 사용 시 문제 표현으로 감지
                
                2. **애정표현 우선**
                   - 키워드: "사랑", "보고싶어", "그리워"
                   - 연인 관계에 맞는 표현
                
                3. **MZ세대 표현**
                   - 키워드: "국룰", "레알", "개좋아"
                   - 최신 유행어
                
                **맥락 분석 규칙:**
                - 감정 표현이 나오면 → 애정표현 키워드 매칭
                """;
            default -> """
                **일반 추출 기준:**
                - 친밀도 레벨 감지
                - 컨셉에 맞지 않는 표현 감지
                """;
        };
    }
    
    
    /**
     * 분석 응답 파싱
     */
    private IntimacyAnalysisResult parseAnalysisResponse(List<String> chunks) {
        log.info("=== IntimacyAnalysisAgent 파싱 시작: {} 개 청크 ===", chunks.size());
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
                    log.debug("IntimacyAnalysisAgent 청크 파싱 실패 (무시): {}", e.getMessage());
                }
            }
            
            String fullResponse = contentBuilder.toString();
            log.info("IntimacyAnalysisAgent 원시 응답: '{}'", fullResponse);
            
            if (fullResponse.trim().isEmpty()) {
                log.warn("IntimacyAnalysisAgent 빈 응답 - 기본값 반환");
                return new IntimacyAnalysisResult(1, List.of());
            }
            
            JsonNode json = objectMapper.readTree(fullResponse);
            log.info("IntimacyAnalysisAgent JSON 파싱 성공");
            
            int detectedLevel = json.has("detectedLevel") ? json.get("detectedLevel").asInt() : 1;
            // 1 또는 3으로 정규화
            if (detectedLevel <= 1) {
                detectedLevel = 1;
            } else if (detectedLevel >= 3) {
                detectedLevel = 3;
            } else {
                detectedLevel = 1;
            }
            
            List<ProblematicExpression> problematicExpressions = new ArrayList<>();
            if (json.has("problematicExpressions") && json.get("problematicExpressions").isArray()) {
                for (JsonNode expr : json.get("problematicExpressions")) {
                    problematicExpressions.add(new ProblematicExpression(
                        expr.has("original") ? expr.get("original").asText() : "",
                        expr.has("type") ? expr.get("type").asText() : "",
                        expr.has("reason") ? expr.get("reason").asText() : ""
                    ));
                }
            }
            
            log.info("=== IntimacyAnalysisAgent 파싱 완료 ===");
            log.info("  - detectedLevel: {}", detectedLevel);
            log.info("  - problematicExpressions: {}", problematicExpressions.size());
            
            return new IntimacyAnalysisResult(detectedLevel, problematicExpressions);
        } catch (Exception e) {
            log.error("IntimacyAnalysisAgent 응답 파싱 실패", e);
            return new IntimacyAnalysisResult(1, List.of());
        }
    }
}

