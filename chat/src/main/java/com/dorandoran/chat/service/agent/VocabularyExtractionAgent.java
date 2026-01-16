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
 * 어휘 추출 Agent
 * 컨셉별 기준으로 어려운 단어/표현을 추출
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VocabularyExtractionAgent {
    private final OpenAIClient openAIClient;
    private final AIConfig aiConfig;
    private final ObjectMapper objectMapper;

    /**
     * 챗봇 응답에서 어려운 단어/표현을 추출
     * 
     * @param botResponse 챗봇 응답 문장
     * @param concept 컨셉 (FRIEND, COWORKER, BOSS, SENIOR, HONEY)
     * @param intimacyLevel 친밀도 레벨 (1-3)
     * @return 추출 결과
     */
    public Mono<VocabularyExtractionResult> extract(String botResponse, String concept, int intimacyLevel) {
        return extract(botResponse, concept, intimacyLevel, null);
    }
    
    public Mono<VocabularyExtractionResult> extract(String botResponse, String concept, int intimacyLevel, UUID chatroomId) {
        log.info("=== VocabularyExtractionAgent.extract() 호출됨 ===");
        log.info("=== 파라미터 - botResponse='{}', concept='{}', intimacyLevel={}, chatroomId={} ===", 
            botResponse, concept, intimacyLevel, chatroomId);
        
        String systemPrompt = buildExtractionPrompt(concept, intimacyLevel);
        log.debug("=== VocabularyExtractionAgent systemPrompt 길이: {} ===", systemPrompt.length());
        
        Double temperature = aiConfig.getAgents().getVocabulary().getExtraction().getTemperature();
        Integer maxTokens = aiConfig.getAgents().getVocabulary().getExtraction().getMaxTokens();
        
        log.info("=== VocabularyExtractionAgent OpenAI API 호출 시작 (temperature={}, maxTokens={}) ===", 
            temperature, maxTokens);
        
        return openAIClient.streamRawCompletion(systemPrompt, botResponse, temperature, maxTokens, chatroomId)
            .doOnError(error -> log.error("VocabularyExtractionAgent 스트림 오류", error))
            .collectList()
            .doOnError(error -> log.error("VocabularyExtractionAgent collectList 오류", error))
            .map(this::parseExtractionResponse)
            .doOnSuccess(response -> {
                if (response != null) {
                    log.info("=== VocabularyExtractionAgent 파싱 완료 ===");
                    log.info("  - originalExpression: '{}'", response.originalExpression());
                    log.info("  - rootForm: '{}'", response.rootForm());
                    log.info("  - category: {} ({})", response.category(), response.category().getDescription());
                    log.info("  - difficulty: {}", response.difficulty());
                    log.info("  - reason: '{}'", response.reason());
                    log.info("=== VocabularyExtractionAgent 응답 완료 ===");
                } else {
                    log.info("=== VocabularyExtractionAgent 파싱 완료: 추출된 어휘 없음 ===");
                }
            })
            .doOnError(error -> log.error("VocabularyExtractionAgent 파싱 오류", error));
    }
    
    /**
     * 어휘 추출을 위한 시스템 프롬프트 생성
     * 
     * 주의: 이 메서드는 하드코딩된 프롬프트를 사용합니다.
     * DB의 vocabulary_system_prompt를 읽지 않으며, 컨셉별 최적화를 위해 하드코딩을 유지합니다.
     * 
     * 프롬프트 관리 방식:
     * - 이 메서드에서 직접 프롬프트를 생성 (컨셉별 추출 기준 포함)
     * - DB의 vocabulary_system_prompt는 resetChatbotPrompt()에서만 사용됨 (기본값 리셋용)
     * - PromptGenerationService.updateVocabularyPrompt()는 deprecated (실제로 사용되지 않음)
     * 
     * @param concept 컨셉 (FRIEND, COWORKER, BOSS, SENIOR, HONEY)
     * @param intimacyLevel 친밀도 레벨 (1-3)
     * @return 시스템 프롬프트 문자열
     */
    private String buildExtractionPrompt(String concept, int intimacyLevel) {
        // 파일에서 프롬프트 로드 시도
        String prompt = loadExtractionPromptFromFile(concept, intimacyLevel);
        if (prompt != null && !prompt.isEmpty()) {
            return prompt;
        }
        
        // 파일 로드 실패 시 fallback (기존 하드코딩 메서드 사용)
        log.warn("Extraction 프롬프트 파일 로드 실패, fallback 사용: concept={}, intimacyLevel={}", concept, intimacyLevel);
        String conceptCriteria = getConceptExtractionCriteria(concept, intimacyLevel);
        
        return String.format("""
            **역할 설명:**
            
            너는 AI 챗봇이 생성한 한국어 응답 문장을 정밀하게 분석하여, 외국인 한국어 학습자에게 난이도가 높은 어휘를 추출하는 전문가 역할을 수행할거야.
            
            **입력 정보:**
            - 분석 대상 문장 (AI 챗봇 응답): "{content}"
            - 컨셉: %s
            - 친밀도 레벨: %d
            
            **난이도 정의 (상황 독립적):**
            - 1 (초급): 일상 기본 어휘, 단순 어미, 기초 동사/명사. (예: 오늘, 하다, 좋다)
            - 2 (중급): 한자어 기반의 일반 어휘, 복합 동사, 관용 표현, 일반적인 사회/업무 용어. (예: 참고, 요청, 말씀, ~시죠)
            - 3 (고급): 복잡한 한자 숙어, 신조어/속어, 고도의 완곡/문어체 표현, 비즈니스 전문 용어. (예: 결재, 품의, 송구스럽습니다, 국룰)
            
            **컨셉별 추출 기준:**
            %s
            
            **어휘(단어/표현) 추출 규칙:**
            1. 반드시 "{content}"에 포함된 단어/표현만 추출할 것
            2. 어휘 난이도가 1, 2, 또는 3인 단어/표현을 추출할 것 (난이도 1도 포함 가능)
               - 난이도 1: 기본 어휘지만 외국인 학습자에게 유용한 단어/표현
               - 난이도 2-3: 중급 이상의 어휘
               - 가능하면 난이도 2-3을 우선 추출하되, 적절한 단어가 없으면 난이도 1도 추출 가능
            3. 항상 1개의 단어/표현만 반환할 것
            4. **표현(구) 우선 추출**: 관용 표현, 문법적 표현, 구문이 있는 경우 표현 전체를 추출할 것
               - 문법적 표현 예시: '~을 것 같아', '~아야 할 것 같아', '~지 않을까', '~아야 해', '~아도 돼' 등
               - 관용 표현 예시: '~에 대해', '~을 위해', '~을 바탕으로' 등
               - 표현이 있는 경우 표현 전체를 추출하고, 표현이 없는 경우 핵심 단어만 추출
            5. **동사원형 변환 필수**: 추출한 단어/표현이 동사, 형용사, 또는 그 변형인 경우, 반드시 동사원형(또는 기본형)으로 변환하여 `rootForm` 필드에 반환할 것
               - 예: "나을" → "낫다", "검토해" → "검토하다", "좋을 것 같아" → "좋다 + 것 같다"
               - 명사나 고유명사 등 변형이 없는 경우는 그대로 반환
               - 표현(구)의 경우, 표현 내 동사/형용사가 있으면 동사원형으로 변환하되 표현 구조는 유지
            6. 어려운 어휘가 없을 경우, null을 반환할 것
            7. 만약 추출 대상의 단어/표현이 직전 사용자 입력(userMessage)에 이미 등장한 단어/표현이라면 그 단어/표현은 건너뛰고 설명하지 않을 것
            
            **JSON 형식 (단어/표현 있을 경우):**
            
            다음 JSON 형식으로 정확히 답변하세요:
            
            {
              "originalExpression": "원본 표현 (문장에서 추출한 그대로)",
              "rootForm": "동사원형 (또는 기본형)",
              "category": "어휘 카테고리 (MZ_GENERATION, SLANG, BUSINESS_TERMS, UNIVERSITY_TERMS, FORMAL_EXPRESSIONS, HANJA_BASED, IDIOMATIC_EXPRESSION 중 하나)",
              "difficulty": 1, 2, 또는 3,
              "reason": "추출 이유 (왜 이 단어/표현을 추출했는지 간단히 설명)"
            }
            
            **주의사항:**
            - 항상 1개의 단어/표현만 반환할 것
            - 어려운 어휘가 없으면 null을 반환할 것
            - JSON 형식 외의 텍스트는 출력하지 말 것
            - category는 반드시 위에 나열된 값 중 하나를 사용할 것
            - 컨셉별 추출 기준을 엄격히 준수할 것
            """, concept, intimacyLevel, conceptCriteria);
    }
    
    /**
     * 파일에서 추출 프롬프트 로드
     */
    private String loadExtractionPromptFromFile(String concept, int intimacyLevel) {
        if (concept == null) {
            concept = "FRIEND";
        }
        String normalizedConcept = concept.toUpperCase();
        String filename = String.format("prompts/vocabulary/extraction/%s_%d.txt",
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
    
    private String getConceptExtractionCriteria(String concept, int intimacyLevel) {
        if (concept == null) {
            concept = "FRIEND";
        }
        String normalizedConcept = concept.toUpperCase();
        
        return switch (normalizedConcept) {
            case "FRIEND" -> """
                **FRIEND 컨셉 추출 기준 (우선순위 순):**
                
                1. **MZ세대 유행어 우선 추출**
                   - 예: "국룰", "레알", "개좋아", "완전", "진짜", "개웃겨", "헐랭"
                   - 최신 유행어, 인터넷 신조어
                
                2. **속어/신조어**
                   - 예: "ㅇㅈ", "ㄱㄱ", "개웃겨", "지랄", "미친"
                   - 줄임말, 비속어 (웃음으로 통하는 표현)
                
                3. **관용 표현**
                   - 예: "~을 것 같아", "~아야 해", "~아도 돼"
                   - 문법적 표현, 구문
                
                4. **한자어 기반 어휘**
                   - 예: "즉시", "참고", "요청"
                   - 일반적인 한자어
                
                **제외 대상:**
                - 비즈니스 용어 (예: "결재", "품의", "보고")
                - 격식 표현 (예: "송구스럽습니다", "말씀드리겠습니다")
                """;
            case "COWORKER", "BOSS" -> """
                **COWORKER/BOSS 컨셉 추출 기준 (우선순위 순):**
                
                1. **비즈니스 용어 우선 추출**
                   - 예: "결재", "품의", "보고", "승인", "검토", "협의"
                   - 업무 관련 전문 용어
                
                2. **격식 표현**
                   - 예: "송구스럽습니다", "말씀드리겠습니다", "드리겠습니다"
                   - 공식적이고 정중한 표현
                
                3. **한자어 기반 어휘**
                   - 예: "즉시", "참고", "요청", "확인"
                   - 업무에서 자주 사용하는 한자어
                
                4. **관용 표현**
                   - 예: "~을 바탕으로", "~에 대해", "~을 위해"
                   - 업무 문서에서 자주 사용하는 표현
                
                **제외 대상:**
                - MZ세대 유행어 (예: "국룰", "레알", "개좋아")
                - 속어/신조어 (예: "개웃겨", "지랄")
                - 대학교 용어 (예: "족보", "과제")
                """;
            case "SENIOR" -> """
                **SENIOR 컨셉 추출 기준 (우선순위 순):**
                
                1. **대학교 용어 우선 추출**
                   - 예: "족보", "과제", "출석", "수강", "전공", "교수님"
                   - 대학교 생활에서 자주 사용하는 용어
                
                2. **격식 표현**
                   - 예: "말씀드리겠습니다", "드리겠습니다"
                   - 선후배 관계에서 사용하는 정중한 표현
                
                3. **한자어 기반 어휘**
                   - 예: "즉시", "참고", "요청"
                   - 학업/학교 생활 관련 한자어
                
                4. **관용 표현**
                   - 예: "~을 바탕으로", "~에 대해"
                   - 학업 관련 표현
                
                **제외 대상:**
                - MZ세대 유행어 (예: "국룰", "레알")
                - 속어/신조어 (예: "개웃겨", "지랄")
                - 비즈니스 용어 (예: "결재", "품의")
                """;
            case "HONEY" -> """
                **HONEY 컨셉 추출 기준 (우선순위 순):**
                
                1. **MZ세대 유행어 우선 추출**
                   - 예: "국룰", "레알", "개좋아", "완전", "진짜"
                   - 최신 유행어, 인터넷 신조어
                
                2. **속어/신조어**
                   - 예: "ㅇㅈ", "ㄱㄱ", "개웃겨"
                   - 줄임말, 친근한 표현
                
                3. **관용 표현**
                   - 예: "~을 것 같아", "~아야 해"
                   - 문법적 표현
                
                4. **한자어 기반 어휘**
                   - 예: "즉시", "참고"
                   - 일반적인 한자어
                
                **제외 대상:**
                - 비즈니스 용어 (예: "결재", "품의")
                - 격식 표현 (예: "송구스럽습니다")
                """;
            default -> """
                **일반 어휘 추출 기준:**
                - 난이도 1-3인 어휘 추출 (가능하면 2-3 우선)
                - 표현 우선, 단어는 표현이 없을 때만
                """;
        };
    }
    
    private VocabularyExtractionResult parseExtractionResponse(List<String> chunks) {
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
                    log.debug("VocabularyExtractionAgent 청크 파싱 실패 (무시): {}", e.getMessage());
                }
            }
            
            String fullResponse = contentBuilder.toString();
            log.info("VocabularyExtractionAgent 원시 응답: '{}'", fullResponse);
            
            if (fullResponse.trim().isEmpty()) {
                log.info("VocabularyExtractionAgent 빈 응답 - 어휘 없음");
                return null;
            }
            
            JsonNode json;
            try {
                json = objectMapper.readTree(fullResponse);
                log.info("VocabularyExtractionAgent JSON 파싱 성공: {}", json.toString());
            } catch (Exception e) {
                log.warn("VocabularyExtractionAgent JSON 파싱 실패: {} - 원시 응답: '{}'", e.getMessage(), fullResponse);
                return null;
            }
            
            // null 체크
            if (json.isNull()) {
                log.info("VocabularyExtractionAgent: null 응답 - 어휘 없음");
                return null;
            }
            
            String originalExpression = json.has("originalExpression") ? json.get("originalExpression").asText() : "";
            String rootForm = json.has("rootForm") ? json.get("rootForm").asText() : "";
            String categoryStr = json.has("category") ? json.get("category").asText() : "";
            int difficulty = json.has("difficulty") ? json.get("difficulty").asInt() : 2;
            String reason = json.has("reason") ? json.get("reason").asText() : "";
            
            // 난이도 검증: 1-3 범위 내에 있어야 함
            if (difficulty < 1 || difficulty > 3) {
                log.warn("VocabularyExtractionAgent: 유효하지 않은 난이도 {}, 기본값 2로 설정", difficulty);
                difficulty = 2;
            }
            
            if (originalExpression.isEmpty() || rootForm.isEmpty()) {
                log.info("VocabularyExtractionAgent: 필수 필드 누락 - 어휘 없음");
                return null;
            }
            
            // VocabularyCategory 파싱
            VocabularyCategory category;
            try {
                category = VocabularyCategory.valueOf(categoryStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("VocabularyExtractionAgent: 알 수 없는 카테고리 '{}', HANJA_BASED로 기본값 사용", categoryStr);
                category = VocabularyCategory.HANJA_BASED;
            }
            
            VocabularyExtractionResult result = new VocabularyExtractionResult(
                originalExpression,
                rootForm,
                category,
                difficulty,
                reason
            );
            
            log.info("VocabularyExtractionAgent 최종 응답: originalExpression='{}', rootForm='{}', category={}, difficulty={}", 
                result.originalExpression(), result.rootForm(), result.category(), result.difficulty());
            
            return result;
        } catch (Exception e) {
            log.error("VocabularyExtractionAgent 응답 파싱 실패", e);
            return null;
        }
    }
}

