package com.dorandoran.chat.service.agent;

import com.dorandoran.chat.config.AIConfig;
import com.dorandoran.chat.service.OpenAIClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * 어휘 설명 Agent
 * 컨셉/레벨에 맞는 말투로 추출된 어휘를 설명
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VocabularyExplanationAgent {
    private final OpenAIClient openAIClient;
    private final AIConfig aiConfig;
    private final ObjectMapper objectMapper;

    /**
     * 추출된 어휘에 대한 설명을 컨셉/레벨에 맞는 말투로 생성
     * 
     * @param extraction 추출 결과
     * @param concept 컨셉 (FRIEND, COWORKER, BOSS, SENIOR, HONEY)
     * @param intimacyLevel 친밀도 레벨 (1-3)
     * @return 설명 결과
     */
    public Mono<VocabularyExplanationResult> generateExplanation(
        VocabularyExtractionResult extraction,
        String concept,
        int intimacyLevel
    ) {
        return generateExplanation(extraction, concept, intimacyLevel, null);
    }
    
    public Mono<VocabularyExplanationResult> generateExplanation(
        VocabularyExtractionResult extraction,
        String concept,
        int intimacyLevel,
        UUID chatroomId
    ) {
        log.info("=== VocabularyExplanationAgent.generateExplanation() 호출됨 ===");
        log.info("=== 파라미터 - originalExpression='{}', concept='{}', intimacyLevel={}, chatroomId={} ===", 
            extraction.originalExpression(), concept, intimacyLevel, chatroomId);
        
        String systemPrompt = buildExplanationPrompt(concept, intimacyLevel);
        String userPrompt = buildUserPrompt(extraction);
        
        log.debug("=== VocabularyExplanationAgent systemPrompt 길이: {} ===", systemPrompt.length());
        
        Double temperature = aiConfig.getAgents().getVocabulary().getExplanation().getTemperature();
        Integer maxTokens = aiConfig.getAgents().getVocabulary().getExplanation().getMaxTokens();
        
        log.info("=== VocabularyExplanationAgent OpenAI API 호출 시작 (temperature={}, maxTokens={}) ===", 
            temperature, maxTokens);
        
        return openAIClient.streamRawCompletion(systemPrompt, userPrompt, temperature, maxTokens, chatroomId)
            .doOnError(error -> log.error("VocabularyExplanationAgent 스트림 오류", error))
            .collectList()
            .doOnError(error -> log.error("VocabularyExplanationAgent collectList 오류", error))
            .map(chunks -> parseExplanationResponse(chunks, concept, intimacyLevel))
            .map(result -> {
                log.info("=== VocabularyExplanationAgent 파싱 완료 (검증 전) ===");
                log.info("  - roma: '{}'", result.roma());
                log.info("  - ko: '{}'", result.ko());
                log.info("  - en: '{}'", result.en());
                
                // 말투 검증 및 리라이트
                if (!isToneAppropriate(result.ko(), concept, intimacyLevel)) {
                    log.warn("VocabularyExplanationAgent: 말투 검증 실패 - 리라이트 시도");
                    VocabularyExplanationResult rewritten = rewriteExplanation(result, concept, intimacyLevel);
                    log.info("=== VocabularyExplanationAgent 리라이트 후 ===");
                    log.info("  - roma: '{}'", rewritten.roma());
                    log.info("  - ko: '{}'", rewritten.ko());
                    log.info("  - en: '{}'", rewritten.en());
                    return rewritten;
                }
                log.info("=== VocabularyExplanationAgent 말투 검증 통과 ===");
                return result;
            })
            .doOnSuccess(response -> {
                log.info("=== VocabularyExplanationAgent 최종 응답 ===");
                log.info("  - roma: '{}'", response.roma());
                log.info("  - ko: '{}' (길이: {}자)", response.ko(), response.ko().length());
                log.info("  - en: '{}' (길이: {}자)", response.en(), response.en().length());
                log.info("=== VocabularyExplanationAgent 응답 완료 ===");
            })
            .doOnError(error -> log.error("VocabularyExplanationAgent 파싱 오류", error));
    }
    
    /**
     * 어휘 설명 생성을 위한 시스템 프롬프트 생성
     * 
     * 주의: 이 메서드는 하드코딩된 프롬프트를 사용합니다.
     * DB의 vocabulary_system_prompt를 읽지 않으며, 컨셉/레벨별 말투 최적화를 위해 하드코딩을 유지합니다.
     * 
     * 프롬프트 관리 방식:
     * - 이 메서드에서 직접 프롬프트를 생성 (컨셉/레벨별 말투 규칙 포함)
     * - DB의 vocabulary_system_prompt는 resetChatbotPrompt()에서만 사용됨 (기본값 리셋용)
     * - PromptGenerationService.updateVocabularyPrompt()는 deprecated (실제로 사용되지 않음)
     * 
     * @param concept 컨셉 (FRIEND, COWORKER, BOSS, SENIOR, HONEY)
     * @param intimacyLevel 친밀도 레벨 (1-3)
     * @return 시스템 프롬프트 문자열
     */
    private String buildExplanationPrompt(String concept, int intimacyLevel) {
        // 파일에서 프롬프트 로드 시도
        String prompt = loadExplanationPromptFromFile(concept, intimacyLevel);
        if (prompt != null && !prompt.isEmpty()) {
            return prompt;
        }
        
        // 파일 로드 실패 시 fallback (기존 하드코딩 메서드 사용)
        log.warn("Explanation 프롬프트 파일 로드 실패, fallback 사용: concept={}, intimacyLevel={}", concept, intimacyLevel);
        String toneGuideline = getToneGuideline(concept, intimacyLevel);
        
        return String.format("""
            **역할 설명:**
            
            너는 추출된 한국어 단어/표현을 컨셉과 친밀도 레벨에 맞는 말투로 설명하는 전문가 역할을 수행할거야.
            
            **컨셉 및 친밀도 레벨:**
            - 컨셉: %s
            - 친밀도 레벨: %d
            
            **말투 규칙 (엄격히 준수):**
            %s
            
            **설명 작성 규칙:**
            1. context.ko: 컨셉/레벨에 맞는 말투로 100자 이내로 작성
            2. context.en: 일관된 톤으로 영어 설명 작성
            3. roma: 정확한 로마자 표기 (예: Gyeoljae, Naeul geot gata)
            
            **설명 내용:**
            - 단어/표현의 의미
            - 사용 맥락
            - 동사원형 변환 정보 (필요 시)
            - 예시 (간단히)
            
            **JSON 형식:**
            
            다음 JSON 형식으로 정확히 답변하세요:
            
            {
              "context": {
                "roma": "로마자 표기",
                "ko": "한국어 설명 (컨셉/레벨에 맞는 말투, 100자 이내)",
                "en": "English explanation (consistent tone)"
              }
            }
            
            **주의사항:**
            - JSON 형식 외의 텍스트는 출력하지 말 것
            - context.ko는 반드시 위의 말투 규칙을 준수할 것
            - 부드럽고 친근한 톤앤매너를 지켜서 작성할 것
            """, concept, intimacyLevel, toneGuideline);
    }
    
    /**
     * 파일에서 설명 프롬프트 로드
     */
    private String loadExplanationPromptFromFile(String concept, int intimacyLevel) {
        if (concept == null) {
            concept = "FRIEND";
        }
        String normalizedConcept = concept.toUpperCase();
        String filename = String.format("prompts/vocabulary/explanation/%s_%d.txt",
            normalizedConcept.toLowerCase(), intimacyLevel);
        
        try {
            ClassPathResource resource = new ClassPathResource(filename);
            if (!resource.exists()) {
                return null;
            }
            
            String content = resource.getContentAsString(StandardCharsets.UTF_8);
            return content;
            
        } catch (IOException e) {
            return null;
        }
    }
    
    private String getToneGuideline(String concept, int intimacyLevel) {
        if (concept == null) {
            concept = "FRIEND";
        }
        String normalizedConcept = concept.toUpperCase();
        
        return switch (normalizedConcept) {
            case "FRIEND" -> switch (intimacyLevel) {
                case 1 -> """
                    **FRIEND Level 1 말투 규칙:**
                    - 반말 사용 (예: "~해", "~야", "~지?")
                    - 부드럽고 예의 있는 반말 톤
                    - 예시: "'국룰'은 '국민 룰'의 줄임말이야. '당연한 것', '기본'이라는 뜻으로 쓰는 신조어야."
                    - 절대 존댓말 사용 금지
                    """;
                case 3 -> """
                    **FRIEND Level 3 말투 규칙:**
                    - 반말 사용 (예: "~해", "~야", "~지?")
                    - 친근하고 장난스러운 반말 톤
                    - 줄임말, 이모티콘 언급 가능
                    - 예시: "'국룰'은 '국민 룰'의 줄임말이야. '당연한 것', '기본'이라는 뜻으로 쓰는 신조어야."
                    - 절대 존댓말 사용 금지
                    """;
                default -> """
                    **FRIEND Level 2 말투 규칙:**
                    - 부드러운 존댓말 사용 (예: "~어요", "~해요")
                    - 예시: "'국룰'은 '국민 룰'의 줄임말이에요. '당연한 것', '기본'이라는 뜻으로 쓰는 신조어예요."
                    """;
            };
            case "COWORKER", "BOSS" -> switch (intimacyLevel) {
                case 1 -> """
                    **COWORKER/BOSS Level 1 말투 규칙:**
                    - 격식체 사용 (예: "~습니다", "~합니다")
                    - 정중하고 공식적인 톤
                    - 예시: "'결재'는 직장 상사에게 서류나 계획을 보여드리고 승인받는 것을 말합니다."
                    - 절대 반말 사용 금지
                    """;
                case 3 -> """
                    **COWORKER/BOSS Level 3 말투 규칙:**
                    - 부드러운 존댓말 사용 (예: "~어요", "~해요")
                    - 친근하지만 정중한 톤
                    - 예시: "'결재'는 직장 상사에게 서류나 계획을 보여드리고 승인받는 것을 말해요."
                    - 절대 반말 사용 금지
                    """;
                default -> """
                    **COWORKER/BOSS Level 2 말투 규칙:**
                    - 부드러운 존댓말 사용 (예: "~어요", "~해요")
                    - 예시: "'결재'는 직장 상사에게 서류나 계획을 보여드리고 승인받는 것을 말해요."
                    """;
            };
            case "SENIOR" -> """
                **SENIOR 컨셉 말투 규칙:**
                - 부드러운 존댓말 사용 (예: "~어요", "~해요")
                - 친근하지만 예의 있는 톤
                - 예시: "'족보'는 선배들이 만든 과거 시험 문제나 답안을 말해요. 대학교에서 자주 쓰는 용어예요."
                - 절대 반말 사용 금지
                """;
            case "HONEY" -> switch (intimacyLevel) {
                case 1 -> """
                    **HONEY Level 1 말투 규칙:**
                    - 부드러운 존댓말 사용 (예: "~어요", "~해요")
                    - 따뜻하고 애정 어린 톤
                    - 예시: "'국룰'은 '국민 룰'의 줄임말이에요. '당연한 것', '기본'이라는 뜻으로 쓰는 신조어예요."
                    """;
                case 3 -> """
                    **HONEY Level 3 말투 규칙:**
                    - 반말 사용 (예: "~해", "~야")
                    - 친근하고 애정 어린 반말 톤
                    - 예시: "'국룰'은 '국민 룰'의 줄임말이야. '당연한 것', '기본'이라는 뜻으로 쓰는 신조어야."
                    - 절대 존댓말 사용 금지
                    """;
                default -> """
                    **HONEY Level 2 말투 규칙:**
                    - 부드러운 존댓말 또는 반말 혼용 가능
                    - 예시: "'국룰'은 '국민 룰'의 줄임말이에요."
                    """;
            };
            default -> """
                **기본 말투 규칙:**
                - 표준 존댓말 사용 (예: "~어요", "~해요")
                - 예시: "'단어'는 의미를 가진 언어의 기본 단위예요."
                """;
        };
    }
    
    private String buildUserPrompt(VocabularyExtractionResult extraction) {
        return String.format("""
            추출된 어휘 정보:
            - 원본 표현: %s
            - 동사원형: %s
            - 카테고리: %s
            - 난이도: %d
            - 추출 이유: %s
            
            위 어휘에 대한 설명을 작성하세요.
            """, 
            extraction.originalExpression(),
            extraction.rootForm(),
            extraction.category().getDescription(),
            extraction.difficulty(),
            extraction.reason()
        );
    }
    
    private VocabularyExplanationResult parseExplanationResponse(List<String> chunks, String concept, int intimacyLevel) {
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
                    log.debug("VocabularyExplanationAgent 청크 파싱 실패 (무시): {}", e.getMessage());
                }
            }
            
            String fullResponse = contentBuilder.toString();
            log.info("VocabularyExplanationAgent 원시 응답: '{}'", fullResponse);
            
            if (fullResponse.trim().isEmpty()) {
                log.warn("VocabularyExplanationAgent 빈 응답 - 기본값 반환");
                return new VocabularyExplanationResult("", "", "");
            }
            
            JsonNode json;
            try {
                json = objectMapper.readTree(fullResponse);
                log.info("VocabularyExplanationAgent JSON 파싱 성공: {}", json.toString());
            } catch (Exception e) {
                log.warn("VocabularyExplanationAgent JSON 파싱 실패: {} - 원시 응답: '{}'", e.getMessage(), fullResponse);
                return new VocabularyExplanationResult("", "", "");
            }
            
            JsonNode contextNode = json.has("context") && json.get("context").isObject() 
                ? json.get("context") 
                : null;
            
            if (contextNode == null) {
                log.warn("VocabularyExplanationAgent: context 필드 없음 - 기본값 반환");
                return new VocabularyExplanationResult("", "", "");
            }
            
            String roma = contextNode.has("roma") ? contextNode.get("roma").asText() : "";
            String ko = contextNode.has("ko") ? contextNode.get("ko").asText() : "";
            String en = contextNode.has("en") ? contextNode.get("en").asText() : "";
            
            VocabularyExplanationResult result = new VocabularyExplanationResult(roma, ko, en);
            
            log.info("VocabularyExplanationAgent 최종 응답: ko 길이={}, en 길이={}", 
                result.ko().length(), result.en().length());
            
            return result;
        } catch (Exception e) {
            log.error("VocabularyExplanationAgent 응답 파싱 실패", e);
            return new VocabularyExplanationResult("", "", "");
        }
    }
    
    /**
     * 설명의 말투가 컨셉/레벨에 적절한지 검증
     */
    private boolean isToneAppropriate(String explanation, String concept, int intimacyLevel) {
        if (explanation == null || explanation.trim().isEmpty()) {
            return true; // 빈 설명은 검증 통과
        }
        
        if (concept == null) {
            concept = "FRIEND";
        }
        String normalizedConcept = concept.toUpperCase();
        String lowerExplanation = explanation.toLowerCase();
        
        // FRIEND Level 1/3은 반말 사용 필수
        if ("FRIEND".equals(normalizedConcept) && (intimacyLevel == 1 || intimacyLevel == 3)) {
            // 존댓말 패턴 체크
            boolean hasFormalEnding = lowerExplanation.matches(".*[해어]요.*") 
                || lowerExplanation.matches(".*이에요.*")
                || lowerExplanation.matches(".*세요.*")
                || lowerExplanation.matches(".*습니다.*")
                || lowerExplanation.contains("드셨어요")
                || lowerExplanation.contains("하시나요")
                || lowerExplanation.contains("이실까요");
            
            if (hasFormalEnding) {
                log.warn("VocabularyExplanationAgent: FRIEND Level {}(반말 필수)인데 존댓말 사용 감지", intimacyLevel);
                return false;
            }
        }
        
        // HONEY Level 3은 반말 사용 필수
        if ("HONEY".equals(normalizedConcept) && intimacyLevel == 3) {
            // 존댓말 패턴 체크
            boolean hasFormalEnding = lowerExplanation.matches(".*[해어]요.*") 
                || lowerExplanation.matches(".*이에요.*")
                || lowerExplanation.matches(".*세요.*");
            
            if (hasFormalEnding) {
                log.warn("VocabularyExplanationAgent: HONEY Level 3(반말 필수)인데 존댓말 사용 감지");
                return false;
            }
        }
        
        // COWORKER/BOSS/SENIOR는 존댓말 사용 필수
        if ("COWORKER".equals(normalizedConcept) || "BOSS".equals(normalizedConcept) || "SENIOR".equals(normalizedConcept)) {
            // 반말 패턴 체크
            boolean hasInformalEnding = lowerExplanation.matches(".*[해야지]야.*")
                || lowerExplanation.matches(".*먹었어.*")
                || lowerExplanation.matches(".*했어.*")
                || (lowerExplanation.endsWith("해") && !lowerExplanation.endsWith("해요") && !lowerExplanation.endsWith("하세요"))
                || (lowerExplanation.endsWith("야") && !lowerExplanation.contains("세요"));
            
            if (hasInformalEnding) {
                log.warn("VocabularyExplanationAgent: {}(존댓말 필수)인데 반말 사용 감지", normalizedConcept);
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 말투가 부적절한 경우 리라이트
     */
    private VocabularyExplanationResult rewriteExplanation(
        VocabularyExplanationResult original,
        String concept,
        int intimacyLevel
    ) {
        if (concept == null) {
            concept = "FRIEND";
        }
        String normalizedConcept = concept.toUpperCase();
        String ko = original.ko();
        
        // 간단한 리라이트 규칙
        if ("FRIEND".equals(normalizedConcept) && (intimacyLevel == 1 || intimacyLevel == 3)) {
            // 존댓말 → 반말 변환
            ko = ko.replace("~어요", "~어")
                .replace("~해요", "~해")
                .replace("~이에요", "~야")
                .replace("드셨어요", "먹었어")
                .replace("하시나요", "하나")
                .replace("이실까요", "일까")
                .replace("예요", "야")
                .replace("있어요", "있어")
                .replace("좋네요", "좋아")
                .replace("괜찮네요", "괜찮아");
        }
        
        if ("HONEY".equals(normalizedConcept) && intimacyLevel == 3) {
            // 존댓말 → 반말 변환
            ko = ko.replace("~어요", "~어")
                .replace("~해요", "~해")
                .replace("~이에요", "~야");
        }
        
        if ("COWORKER".equals(normalizedConcept) || "BOSS".equals(normalizedConcept) || "SENIOR".equals(normalizedConcept)) {
            // 반말 → 존댓말 변환
            if (intimacyLevel == 1) {
                // 격식체로 변환
                ko = ko.replace("~어", "~습니다")
                    .replace("~해", "~합니다")
                    .replace("먹었어", "드셨습니다")
                    .replace("했어", "하셨습니다");
            } else {
                // 부드러운 존댓말로 변환
                ko = ko.replace("~어", "~어요")
                    .replace("~해", "~해요")
                    .replace("먹었어", "드셨어요")
                    .replace("했어", "하셨어요");
            }
        }
        
        log.info("VocabularyExplanationAgent: 말투 리라이트 완료");
        return new VocabularyExplanationResult(original.roma(), ko, original.en());
    }
}

