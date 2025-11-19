package com.dorandoran.chat.service.agent;

import com.dorandoran.chat.entity.IntimacyProgress;
import com.dorandoran.chat.enums.ChatRoomConcept;
import com.dorandoran.chat.repository.IntimacyProgressRepository;
import com.dorandoran.chat.repository.ChatRoomRepository;
import com.dorandoran.chat.repository.ChatbotRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 친밀도 분석 Agent
 * 외국인의 한국어 친밀도를 분석하고 교정된 문장을 제공
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IntimacyAgent {
    private final IntimacyAnalysisAgent analysisAgent;
    private final IntimacyCorrectionAgent correctionAgent;
    private final IntimacyProgressRepository intimacyProgressRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatbotRepository chatbotRepository;
    private final ObjectMapper objectMapper;

    /**
     * Facade 패턴: 내부적으로 IntimacyAnalysisAgent + IntimacyCorrectionAgent 조합 사용
     * 기존 인터페이스 유지 (하위 호환성)
     */
    public Mono<IntimacyAgentResponse> analyze(UUID chatroomId, String userMessage) {
        log.info("=== IntimacyAgent.analyze() 호출됨 (Facade) ===");
        log.info("=== IntimacyAgent 파라미터 - chatroomId={}, userMessage='{}' ===", chatroomId, userMessage);
        
        int currentLevel = intimacyProgressRepository.findByChatRoomId(chatroomId)
            .map(IntimacyProgress::getIntimacyLevel)
            .orElse(1);
        log.info("=== IntimacyAgent 현재 레벨 조회: {} ===", currentLevel);
        
        String concept = getConceptFromChatRoom(chatroomId);
        if (concept != null) {
            concept = concept.toUpperCase();
        }
        log.info("=== IntimacyAgent: concept='{}' ===", concept);
        
        // 컨셉별 레벨 검증 및 조정
        currentLevel = validateIntimacyLevel(concept, currentLevel);
        log.info("=== IntimacyAgent: 검증된 currentLevel={} ===", currentLevel);
        
        final int finalCurrentLevel = currentLevel;
        final String finalConcept = concept;
        
        // 1. IntimacyAnalysisAgent로 분석
        log.info("=== IntimacyAgent: IntimacyAnalysisAgent 호출 시작 ===");
        return analysisAgent.analyze(userMessage, finalConcept, finalCurrentLevel)
            .flatMap(analysisResult -> {
                log.info("=== IntimacyAgent: IntimacyAnalysisAgent 응답 수신 ===");
                log.info("  - detectedLevel: {}", analysisResult.detectedLevel());
                log.info("  - problematicExpressions: {}", analysisResult.problematicExpressions().size());
                
                // 2. IntimacyCorrectionAgent로 교정
                log.info("=== IntimacyAgent: IntimacyCorrectionAgent 호출 시작 ===");
                return correctionAgent.generateCorrection(analysisResult, finalConcept, finalCurrentLevel)
                    .map(correctionResult -> {
                        log.info("=== IntimacyAgent: IntimacyCorrectionAgent 응답 수신 ===");
                        log.info("  - correctedSentence: '{}'", correctionResult.correctedSentence());
                        log.info("  - feedback.ko: '{}'", correctionResult.feedback().ko());
                        log.info("  - alternativeExpressions: {} 개", correctionResult.alternativeExpressions().size());
                        
                        // 3. 사후 검증 및 최종 응답 변환
                        int normalizedDetectedLevel = normalizeDetectedLevel(analysisResult.detectedLevel());
                        
                        // 컨셉 제약 위반 검증
                        if (!correctionResult.correctedSentence().isEmpty() && 
                            !correctionResult.correctedSentence().trim().equals(userMessage.trim())) {
                            boolean isValidStyle = validateSpeechStyleAgainstConcept(
                                finalConcept,
                                finalCurrentLevel,
                                userMessage,
                                correctionResult.correctedSentence()
                            );
                            
                            if (!isValidStyle) {
                                log.warn("=== IntimacyAgent: 컨셉 제약 위반 감지 - 원문 유지 ===");
                                return new IntimacyAgentResponse(
                                    "intimacy",
                                    normalizedDetectedLevel,
                                    userMessage,
                                    new FeedbackText("", ""),
                                    "",
                                    correctionResult.alternativeExpressions()
                                );
                            }
                        }
                        
                        // 불필요한 교정 검증
                        String normalizedOriginal = normalizeSentence(userMessage);
                        String normalizedCorrected = normalizeSentence(correctionResult.correctedSentence());
                        
                        if (correctionResult.correctedSentence().isEmpty() || 
                            normalizedOriginal.equals(normalizedCorrected)) {
                            log.info("=== IntimacyAgent: 교정 불필요 감지 ===");
                            return new IntimacyAgentResponse(
                                "intimacy",
                                normalizedDetectedLevel,
                                userMessage,
                                new FeedbackText("", ""),
                                "",
                                correctionResult.alternativeExpressions()
                            );
                        }
                        
                        // 최종 응답 반환
                        log.info("=== IntimacyAgent 최종 응답 생성 완료 ===");
                        return new IntimacyAgentResponse(
                            "intimacy",
                            normalizedDetectedLevel,
                            correctionResult.correctedSentence(),
                            correctionResult.feedback(),
                            correctionResult.corrections(),
                            correctionResult.alternativeExpressions()
                        );
                    });
            })
            .doOnSuccess(response -> log.info("IntimacyAgent 최종 응답: 레벨={}, corrections='{}', feedback.ko='{}', alternativeExpressions={} 개", 
                response.detectedLevel(), response.corrections(), response.feedback().ko(), 
                response.alternativeExpressions().size()))
            .doOnError(error -> log.error("IntimacyAgent 처리 오류", error));
    }
    
    private String getConceptFromChatRoom(UUID chatroomId) {
        log.info("=== getConceptFromChatRoom 시작 - chatroomId={} ===", chatroomId);
        return chatRoomRepository.findById(chatroomId)
            .map(room -> {
                log.info("=== getConceptFromChatRoom: room 조회 완료, settings 존재 여부: {} ===", room.getSettings() != null);
                if (room.getSettings() != null && room.getSettings().has("concept")) {
                    JsonNode conceptNode = room.getSettings().get("concept");
                    log.info("=== getConceptFromChatRoom: conceptNode 존재, nodeType={}, isTextual={}, isNumber={}, isNull={}, toString='{}' ===", 
                        conceptNode.getNodeType(), conceptNode.isTextual(), conceptNode.isNumber(), conceptNode.isNull(), conceptNode.toString());
                    
                    // 안전하게 String으로 변환
                    if (conceptNode.isTextual()) {
                        String result = conceptNode.asText();
                        log.info("=== getConceptFromChatRoom: String 변환 성공 - result='{}' ===", result);
                        return result;
                    } else if (conceptNode.isNumber()) {
                        // 숫자인 경우 기본값 반환
                        log.warn("IntimacyAgent: concept이 숫자 타입입니다. 기본값 FRIEND 사용. chatroomId={}, value={}", chatroomId, conceptNode.asInt());
                        return "FRIEND";
                    } else {
                        // 다른 타입인 경우 기본값 반환
                        log.warn("IntimacyAgent: concept이 유효하지 않은 타입입니다. 기본값 FRIEND 사용. chatroomId={}, type={}, value={}", 
                            chatroomId, conceptNode.getNodeType(), conceptNode.toString());
                        return "FRIEND";
                    }
                }
                log.info("=== getConceptFromChatRoom: settings 또는 concept이 없음, 기본값 FRIEND 반환 ===");
                return "FRIEND";
            })
            .orElse("FRIEND");
    }
    
    private String buildIntimacyPrompt(UUID chatroomId, int level, String concept) {
        log.info("=== buildIntimacyPrompt 시작 - chatroomId={}, level={}, concept='{}' ===", chatroomId, level, concept);
        // 1. DB에서 Base Prompt 조회
        log.info("=== buildIntimacyPrompt: getBasePromptFromDB 호출 전 ===");
        String basePrompt = getBasePromptFromDB(chatroomId);
        log.info("=== buildIntimacyPrompt: getBasePromptFromDB 호출 후 - basePrompt 길이={} ===", basePrompt != null ? basePrompt.length() : 0);
        
        // 2. 상세 교정 지침 생성
        log.info("=== buildIntimacyPrompt: getDetailedCorrectionGuideline 호출 전 - concept='{}', level={} ===", concept, level);
        String detailedGuideline = getDetailedCorrectionGuideline(concept, level);
        log.info("=== buildIntimacyPrompt: getDetailedCorrectionGuideline 호출 후 - detailedGuideline 길이={} ===", detailedGuideline != null ? detailedGuideline.length() : 0);
        
        // 3. 컨셉 제약 정보
        log.info("=== buildIntimacyPrompt: getConceptConstraint 호출 전 - concept='{}' ===", concept);
        String conceptConstraint = getConceptConstraint(concept);
        log.info("=== buildIntimacyPrompt: getConceptConstraint 호출 후 - conceptConstraint 길이={} ===", conceptConstraint != null ? conceptConstraint.length() : 0);
        
        // 4. Dynamic Directives 생성
        String dynamicDirectives = String.format("""
            
            [분석 컨텍스트]
            현재 학습자의 목표 레벨: %d
            대화 컨셉: %s
            
            [컨셉 제약]
            %s
            
            [상세 교정 지침]
            %s
            
            [응답 형식]
            반드시 아래 JSON 형식으로만 응답하세요. 다른 텍스트는 포함하지 마세요:
            {
              "detectedLevel": 1 또는 3 (반드시 1 또는 3만 사용, 0, 2, 4 이상은 사용하지 마세요),
              "correctedSentence": "교정된 문장 또는 ''",
              "feedback": {
                "ko": "한국어 피드백 (150자 내)",
                "en": "English feedback (within 350 words)"
              },
              "corrections": "변경사항 설명 (예: '오늘 밥 먹었어?' → '오늘 밥 드셨어요?'로 변경)"
            }
            
            [교정 처리 지침]
            ⚠️ 모든 교정은 반드시 [컨셉 제약]을 준수하고, 컨셉에 맞게 일관되게 수정해야 합니다.
            - FRIEND 컨셉이면 반말로, COWORKER/SENIOR/BOSS 컨셉이면 존댓말로 일관되게 교정
            - 모든 교정시 반드시 말투를 유지해야 합니다. 존댓말은 존댓말, 반말은 반말로 교정하세요.
            - 컨셉과 친밀도 레벨에 맞는 말투를 일관되게 유지하는 것이 최우선입니다.
            
            ⚠️⚠️⚠️ 매우 중요: 레벨 차이는 무시하고 말투(존댓말/반말)만 교정하세요.
            - 입력값 intimacy_level과 감지값 detectedLevel이 다르더라도, 레벨 차이로 인한 교정은 하지 마세요.
            - 오직 컨셉 제약에 위반되는 말투(존댓말/반말)만 교정하세요.
            - 예: FRIEND 컨셉에서 detectedLevel=1, currentLevel=3이어도, 반말이면 교정하지 마세요.
            - 예: FRIEND 컨셉에서 존댓말을 사용했다면, 반말로 교정하세요 (레벨과 무관).
            
            ⚠️⚠️⚠️ detectedLevel 제한:
            - detectedLevel은 반드시 1 또는 3만 사용하세요.
            - 0, 2는 1로, 4 이상은 3으로 조정하세요.
            - detectedLevel: 0 → 1로 변경
            - detectedLevel: 2 → 1로 변경
            - detectedLevel: 4 이상 → 3으로 변경
            
            - 문장이 이미 컨셉 제약에 맞는 올바른 형태인 경우:
              - corrections: "" (반드시 빈 문자열)
              - feedback: {"ko": "", "en": ""} (반드시 빈 문자열)
              - correctedSentence: 원문 그대로 유지 (또는 빈 문자열)
              - ⚠️ 중요: 문장이 이미 컨셉에 맞는 올바른 말투라면, 절대 교정하지 마세요.
              - ⚠️ 중요: "반말을 반말로 바꾸기", "존댓말을 존댓말로 바꾸기" 같은 불필요한 교정은 절대 하지 마세요.
              - ⚠️ 중요: 레벨 차이로 인한 교정은 절대 하지 마세요.
              - 단, 원문이 컨셉 제약을 위반하는 경우에만 교정 필요 (예: FRIEND 컨셉인데 존댓말 사용)
            
            - 원문이 컨셉 제약을 위반하는 경우 (예: FRIEND 컨셉인데 존댓말 사용):
              - corrections: 구체적인 변경사항 설명 (말투 교정에 대한 설명)
              - feedback: {ko/en} 각각 명확한 피드백 (ko는 150자 내로 구체적으로 설명)
              - correctedSentence: 수정된 문장 (컨셉에 맞는 말투로)
              - 반드시 컨셉 제약을 준수한 문장으로 교정
            
            - AI가 문체를 친밀도 1~3 레벨 중 어디에도 명확히 분류하지 못할 경우:
              - detectedLevel: 1 (0은 사용하지 마세요)
              - corrections: 구체적인 변경사항 설명 (컨셉 제약 위반 시에만)
              - feedback: {ko/en} 각각 명확한 피드백 (컨셉 제약 위반 시에만)
              - correctedSentence: 수정된 문장 (컨셉 제약 위반 시에만)
              - 반드시 컨셉 제약을 준수한 문장으로 교정
            
            [중요 주의사항]
            - ⚠️ 최우선: [컨셉 제약]을 절대 위반하지 마세요. 모든 교정은 컨셉 제약을 우선적으로 준수해야 합니다.
            - ⚠️ 최우선: 컨셉에 맞게 일관되게 수정하세요. FRIEND 컨셉이면 반말로, COWORKER/SENIOR/BOSS 컨셉이면 존댓말로 일관되게 교정해야 합니다.
            - ⚠️ 최우선: 모든 교정시 반드시 말투를 유지해야 합니다. 존댓말은 존댓말, 반말은 반말로 교정하세요.
            - feedback의 ko는 intimacy_level별로 사회적 맥락에 기반하여 왜 교정되었는지 150자 내로 구체적으로 설명할 것
            - feedback은 반드시 ko와 en 두 개의 필드로 구성할 것
            - JSON 형식 외의 텍스트는 출력하지 말 것
            - 입력값 intimacy_level과 감지값 detectedLevel이 다를 경우 교정할 것
            - 줄임말, 신조어, 속어, 지역 방언 등 실생활 표현에 대해서는 교정하지 말 것 (단, 컨셉 제약에 위배되는 경우는 예외)
            - userMessage만 교정할 것, 다른 의미로 변질되거나 다른 문장은 추가하지 말 것
            
  
            """, level, concept, conceptConstraint, detailedGuideline);
        
        // 5. 합성
        return basePrompt + dynamicDirectives;
    }
    
    private String getBasePromptFromDB(UUID chatroomId) {
        log.info("=== getBasePromptFromDB 시작 - chatroomId={} ===", chatroomId);
        try {
            String result = chatRoomRepository.findById(chatroomId)
                .flatMap(room -> {
                    log.info("=== getBasePromptFromDB: room 조회 완료, chatbot 존재 여부: {} ===", room.getChatbot() != null);
                    if (room.getChatbot() == null) return Optional.empty();
                    return chatbotRepository.findById(room.getChatbot().getId());
                })
                .map(chatbot -> {
                    log.info("=== getBasePromptFromDB: chatbot 조회 완료, intimacySystemPrompt 존재 여부: {} ===", chatbot.getIntimacySystemPrompt() != null);
                    return chatbot.getIntimacySystemPrompt();
                })
                .orElseGet(() -> {
                    log.info("=== getBasePromptFromDB: 기본 프롬프트 사용 ===");
                    return getDefaultIntimacyBasePrompt();
                });
            log.info("=== getBasePromptFromDB 완료 - result 길이={} ===", result != null ? result.length() : 0);
            return result;
        } catch (Exception e) {
            log.error("getBasePromptFromDB 오류: chatroomId={}, exception={}", chatroomId, e.getMessage(), e);
            return getDefaultIntimacyBasePrompt();
        }
    }
    
    private String getDefaultIntimacyBasePrompt() {
        return """
            당신은 외국인의 한국어 친밀도를 분석하는 전문가입니다.
            
            [한국어 높임법 규칙 - 필수 준수]
            
            ❗ 높임 표현이 가능한 동사 vs 불가능한 동사:
            
            1. 인간 대상 동작 동사 → 높임 표현 가능
               - 보다 → 보시다
               - 먹다 → 드시다/잡수시다
               - 자다 → 주무시다
               - 말하다 → 말씀하시다
               ⚠️ 주의: 예시는 컨셉과 친밀도 레벨에 따라 달라집니다.
               - FRIEND 컨셉 Level 1/3: "밥 먹었어?" → 그대로 유지 (반말 유지)
               - COWORKER/SENIOR/BOSS 컨셉: "밥 먹었어?" → "밥 드셨어요?" (존댓말로 교정)
            
            2. 사물/상황 상태 자동사 → 높임 표현 금지
               - 되다 → 되시다 ❌ (잘못된 표현)
               - 생기다 → 생기시다 ❌
               - 일어나다 → 일어나시다 ❌ (상황)
               - 끝나다 → 끝나시다 ❌
               - 막히다 → 막히시다 ❌
               - 예: "세부사항이 잘못되었어요" → 그대로 유지 (교정 불필요)
               - 예: "일이 잘못되셨어요" → "일이 잘못되었어요" ✓
            
            3. 교정 원칙
               - 원문이 올바르면 교정하지 말 것
               - 높임 표현을 잘못 사용한 경우에만 교정
               - 한국어 문법상 자연스럽지 않은 표현은 교정
            
            사용자의 문장을 분석하여 반드시 JSON 형식으로만 답변하세요.
            다른 텍스트나 설명은 포함하지 마세요.
            
            응답 형식:
            {
              "detectedLevel": 1 또는 3 (반드시 1 또는 3만 사용),
              "correctedSentence": "교정된 문장",
              "feedback": {
                "ko": "한국어 피드백",
                "en": "English feedback"
              },
              "corrections": "변경사항 설명 (예: '오늘 밥 먹었어?' → '오늘 밥 드셨어요?'로 변경)"
            }
            
            교정이 필요 없는 경우:
            - corrections: ""
            - feedback: {"ko": "", "en": ""}
            - correctedSentence: 원문 그대로
            
            교정이 필요한 경우:
            - corrections: 구체적인 변경사항 설명
            - feedback: {ko/en} 각각 명확한 피드백
            - correctedSentence: 수정된 문장
            """;
    }
    
    private IntimacyAgentResponse parseIntimacyResponse(List<String> chunks) {
        log.info("=== parseIntimacyResponse 호출됨: {} 개 청크 ===", chunks.size());
        try {
            // OpenAI 스트림에서 실제 content만 추출
            StringBuilder contentBuilder = new StringBuilder();
            for (int i = 0; i < chunks.size(); i++) {
                String chunk = chunks.get(i);
                log.trace("IntimacyAgent 청크 {}: '{}'", i, chunk);
                try {
                    JsonNode chunkJson = objectMapper.readTree(chunk);
                    if (chunkJson.has("choices") && chunkJson.get("choices").isArray() && chunkJson.get("choices").size() > 0) {
                        JsonNode choice = chunkJson.get("choices").get(0);
                        if (choice.has("delta") && choice.get("delta").has("content")) {
                            String content = choice.get("delta").get("content").asText();
                            contentBuilder.append(content);
                            log.trace("IntimacyAgent content 추출: '{}'", content);
                        }
                    }
                } catch (Exception e) {
                    log.debug("IntimacyAgent 청크 파싱 실패 (무시): {} - 청크: '{}'", e.getMessage(), chunk);
                }
            }
            
            String fullResponse = contentBuilder.toString();
            log.info("IntimacyAgent 원시 응답: '{}'", fullResponse);
            
            if (fullResponse.trim().isEmpty()) {
                log.warn("IntimacyAgent 빈 응답 - 기본값 반환");
                return new IntimacyAgentResponse("intimacy", 0, "", new FeedbackText("", ""), "");
            }
            
            JsonNode json;
            try {
                json = objectMapper.readTree(fullResponse);
                log.info("IntimacyAgent JSON 파싱 성공: {}", json.toString());
            } catch (Exception e) {
                log.warn("IntimacyAgent JSON 파싱 실패: {} - 원시 응답: '{}'", e.getMessage(), fullResponse);
                return new IntimacyAgentResponse(
                    "intimacy",
                    0,
                    "",
                    new FeedbackText("분석 중 오류가 발생했습니다.", "An error occurred during analysis."),
                    ""
                );
            }
            
            int detectedLevel = json.has("detectedLevel") ? json.get("detectedLevel").asInt() : 0;
            String correctedSentence = json.has("correctedSentence") ? json.get("correctedSentence").asText() : "";
            
            // corrections 파싱 강화 - Array/String 둘 다 처리
            String corrections = "";
            if (json.has("corrections")) {
                JsonNode correctionsNode = json.get("corrections");
                if (correctionsNode.isTextual()) {
                    corrections = correctionsNode.asText();
                } else if (correctionsNode.isArray()) {
                    // Array를 String으로 변환
                    List<String> correctionsList = new ArrayList<>();
                    correctionsNode.forEach(node -> correctionsList.add(node.asText()));
                    corrections = String.join(", ", correctionsList);
                }
            }
            
            // feedback 파싱 (ko/en 구조)
            FeedbackText feedback = new FeedbackText("", "");
            if (json.has("feedback") && json.get("feedback").isObject()) {
                JsonNode feedbackNode = json.get("feedback");
                String ko = feedbackNode.has("ko") ? feedbackNode.get("ko").asText() : "";
                String en = feedbackNode.has("en") ? feedbackNode.get("en").asText() : "";
                feedback = new FeedbackText(ko, en);
            }
            
            // 빈 값 체크 및 경고
            if (detectedLevel > 0 && (corrections.isEmpty() || feedback.ko().isEmpty())) {
                log.warn("IntimacyAgent 빈 값 감지 - detectedLevel: {}, corrections: '{}', feedback.ko: '{}'", 
                    detectedLevel, corrections, feedback.ko());
            }
            
            IntimacyAgentResponse response = new IntimacyAgentResponse(
                "intimacy",
                detectedLevel,
                correctedSentence,
                feedback,
                corrections
            );
            
            log.info("IntimacyAgent 최종 응답: detectedLevel={}, corrections='{}', feedback.ko='{}'", 
                response.detectedLevel(), response.corrections(), response.feedback().ko());
            
            return response;
        } catch (Exception e) {
            log.error("IntimacyAgent 응답 파싱 실패", e);
            return new IntimacyAgentResponse(
                "intimacy",
                0,
                "",
                new FeedbackText("분석 중 오류가 발생했습니다.", "An error occurred during analysis."),
                ""
            );
        }
    }
    
    /**
     * 컨셉별 허용 친밀도 레벨 검증 및 조정
     */
    private int validateIntimacyLevel(String concept, int level) {
        log.info("=== validateIntimacyLevel 시작 - concept='{}', level={} ===", concept, level);
        try {
            log.info("=== validateIntimacyLevel: ChatRoomConcept.fromString 호출 전 - concept='{}', type={} ===", 
                concept, concept != null ? concept.getClass().getSimpleName() : "null");
            ChatRoomConcept conceptEnum = ChatRoomConcept.fromString(concept);
            log.info("=== validateIntimacyLevel: ChatRoomConcept.fromString 호출 성공 - conceptEnum={} ===", conceptEnum);
            
            if (!conceptEnum.isLevelAllowed(level)) {
                int adjustedLevel = conceptEnum.adjustLevel(level);
                log.warn("IntimacyAgent 레벨 조정: {} 컨셉에서 Level {} 는 허용되지 않습니다. Level {} 로 조정합니다.",
                    concept, level, adjustedLevel);
                return adjustedLevel;
            }
            
            log.info("=== validateIntimacyLevel: 레벨 검증 통과 - level={} ===", level);
            return level;
        } catch (Exception e) {
            log.error("IntimacyAgent 컨셉 검증 실패: {} - 기본값 Level 2 사용, concept='{}', exception={}", 
                e.getMessage(), concept, e.getClass().getSimpleName(), e);
            return 2; // 기본값
        }
    }
    
    /**
     * 컨셉별 상세 교정 지침 생성
     */
    private String getDetailedCorrectionGuideline(String concept, int level) {
        log.info("=== getDetailedCorrectionGuideline 시작 - concept='{}', level={} ===", concept, level);
        try {
            String result = switch (concept != null ? concept.toUpperCase() : "FRIEND") {
                case "BOSS" -> getBossGuideline(level);
                case "COWORKER" -> getCoworkerGuideline(level);
                case "SENIOR" -> getSeniorGuideline(level);
                case "FRIEND" -> getFriendGuideline(level);
                case "HONEY" -> getHoneyGuideline(level);
                default -> {
                    log.warn("getDetailedCorrectionGuideline: 알 수 없는 concept='{}', 기본 지침 사용", concept);
                    yield getDefaultGuideline(level);
                }
            };
            log.info("=== getDetailedCorrectionGuideline 완료 - result 길이={} ===", result != null ? result.length() : 0);
            return result;
        } catch (Exception e) {
            log.error("getDetailedCorrectionGuideline 오류: concept='{}', level={}, exception={}", concept, level, e.getMessage(), e);
            return getDefaultGuideline(level);
        }
    }
    
    private String getBossGuideline(int level) {
        return switch (level) {
            case 1 -> """
                [BOSS Level 1 - 격식체]
                허용 어미: ~습니다, ~하겠습니다, ~드리겠습니다, ~괜찮으시겠습니까?
                금지 표현: 반말, 이모티콘, 구어체 감탄사, 장난스러운 표현
                예시: "보고서를 제출하겠습니다" ✅ / "보고서 제출했어요" ❌
                
                [격식 어휘 필수]
                - 밥 먹다 → 식사하다 (필수: "식사하셨습니까?" ✅ / "밥 드셨어요?" ❌)
                - 자다 → 휴식하다/주무시다 (필수)
                - 보다 → 확인하다/검토하다 (업무 맥락)
                - 하다 → 진행하다/수행하다 (업무 맥락)
                - 말하다 → 말씀하시다/진술하시다
                잘못된 예: "밥 드셨어요?" → "식사하셨습니까?" 로 교정 필수
                """;
            case 3 -> """
                [BOSS Level 2 - 표준 존댓말]
                허용 어미: ~어요, ~해요, ~이에요, ~하시나요?
                금지 표현: 반말, 이모티콘, 구어체 감탄사, 장난스러운 표현
                예시: "업무가 어떠세요?" ✅ / "업무 어때?" ❌
                
                [격식 어휘 필수]
                - 밥 먹다 → 식사하다 (필수: "식사하셨어요?" ✅ / "밥 드셨어요?" ❌)
                - 자다 → 휴식하다/주무시다 (필수)
                - 보다 → 확인하다/검토하다 (업무 맥락)
                - 하다 → 진행하다/수행하다 (업무 맥락)
                잘못된 예: "밥 드셨어요?" → "식사하셨어요?" 로 교정 필수
                """;
            default -> getBossGuideline(2); // Level 3은 Level 2로 처리
        };
    }
    
    private String getCoworkerGuideline(int level) {
        return switch (level) {
            case 0 -> """
                [COWORKER Level 0 - 분류 불가]
                AI가 문체를 친밀도 1~3 레벨 중 어디에도 명확히 분류하지 못할 경우 사용
                직장 동료 관계에 맞는 표현으로 교정하되, 비격식 표현(이모티콘, 구어체 감탄사)은 제거
                """;
            case 1 -> """
                [COWORKER Level 1 - 격식체]
                허용 어미: ~습니다, ~입니다, ~하겠습니다
                금지 표현: 반말, 이모티콘, 구어체 감탄사
                예시: "회의 준비하겠습니다" ✅ / "회의 준비할게" ❌
                
                [격식 어휘 사용]
                - 밥 먹다 → 식사하다 (권장: "식사하셨습니까?" ✅)
                - 자다 → 휴식하다/주무시다 (권장)
                - 보다 → 확인하다/검토하다 (업무 맥락)
                - 하다 → 진행하다/수행하다 (업무 맥락)
                """;
            // case 2 -> """
            //     [COWORKER Level 2 - 표준 존댓말]
            //     허용 어미: ~어요, ~해요, ~이에요
            //     금지 표현: 반말, 이모티콘, 구어체 감탄사
            //     예시: "회의 준비해요" ✅ / "회의 준비할게" ❌
                
            //     [일상 어휘 허용하되 정중하게]
            //     - "밥 드셨어요?" (일상 어휘 허용)
            //     - "식사하셨어요?" (격식 어휘 권장)
            //     """;
            case 3 -> """
                [COWORKER Level 3 - 부드러운 존댓말]
                허용 어미: ~어요, ~해요, ~이에요, ~네요
                금지 표현: 반말, 과도한 이모티콘, 지나친 장난, 속어
                예시: "회의 준비해요" ✅ / "회의 준비할게요~" ✅ / "회의 준비해" ❌
                
                [일상 어휘 자유롭게 사용하되 존댓말 유지]
                - "밥 드셨어요?" (존댓말 유지)
                - "식사하셨어요?" (격식 어휘 권장)
                - 이모티콘(ㅋㅋ, ㅎㅎ)은 자유롭게 사용 가능하나, 반말은 절대 금지
                """;
            default -> getCoworkerGuideline(1);
        };
    }
    
    private String getSeniorGuideline(int level) {
        return switch (level) {
            case 0 -> """
                [FRIEND Level 0 - 분류 불가]
                AI가 문체를 친밀도 1~3 레벨 중 어디에도 명확히 분류하지 못할 경우 사용
                선후배 관계에 맞는 존댓말 표현으로 교정하되, 비격식 표현(반말 등)은 제거
                """;
            case 1 -> """
                [SENIOR Level 1 - 격식체]
                허용 어미: ~합니다, ~드립니다, ~하겠습니다
                금지 표현: 반말, 이모티콘, 구어체 감탄사
                예시: "과제를 제출하겠습니다" ✅ / "과제 제출할게" ❌
                
                [격식 어휘 필수]
                - 밥 먹다 → 식사하다 (필수: "식사하셨습니까?" ✅ / "밥 드셨어요?" ❌)
                - 자다 → 휴식하다/주무시다 (필수)
                - 보다 → 확인하다/검토하다 (학업 맥락)
                - 하다 → 수행하다/실시하다 (학업 맥락)
                잘못된 예: "밥 드셨어요?" → "식사하셨습니까?" 로 교정 필수
                """;
            case 3 -> """
                [SENIOR Level 3 - 부드러운 존댓말]
                허용 어미: ~어요, ~해요, ~이에요, ~네요
                금지 표현: 반말, 격식체(~습니다, ~하겠습니다), 과도한 이모티콘
                예시: "과제 제출해요" ✅ / "과제 제출할게요~" ✅ / "과제를 제출하겠습니다" ❌
                
                [일상 어휘 허용 - 격식 어휘 필수 아님]
                - 일상 어휘 사용 가능: "밥 드셨어요?" ✅, "과제 확인해요" ✅
                - 격식 어휘는 선택사항: "식사하셨어요?" (선택), "과제를 확인하셨어요?" (선택)
                - 격식체(~습니다)로 교정하지 말 것: "과제를 제출하겠습니다" ❌
                - ~어요 형태로 교정할 것: "과제 제출해요" ✅
                
                [교정 원칙]
                - Level 3에서는 부드러운 존댓말(~어요)을 사용하되, 격식체로 교정하지 말 것
                - 일상 어휘를 사용한 ~어요 형태는 그대로 유지
                - 격식 어휘를 강제로 교정하지 말 것
                """;
            // case 4 -> """
            //     [SENIOR Level 3 - 절제된 반말]
            //     허용 어미: ~해, ~지, ~야
            //     금지 표현: 과도한 이모티콘, 반말 존칭 혼용, 지나친 장난
            //     예시: "과제 제출해" ✅ / "과제 제출해요" (혼용) ❌
                
            //     [일상 어휘 허용하되 존중 유지]
            //     - "밥 먹었어?" (반말 허용)
            //     - "식사하셨어요?" (존댓말 권장, 선택)
            //     """;
            default -> getSeniorGuideline(1);
        };
    }
    
    private String getFriendGuideline(int level) {
        return switch (level) {
            case 0 -> """
                [FRIEND Level 0 - 분류 불가]
                AI가 문체를 친밀도 1~3 레벨 중 어디에도 명확히 분류하지 못할 경우 사용
                친구 관계에 맞는 반말 표현으로 교정하되, 격식 표현은 제거
                """;
            case 1 -> """
                [FRIEND Level 1 - 가벼운 반말]
                허용 어미: ~하자, ~할래?, ~지?, ~해, ~야
                금지 표현: 격식체(~습니다, ~하세요), 과도한 이모티콘, 속어
                예시: "밥 먹자" ✅ / "밥 먹을래?" ✅ / "좋아요. 언제 드실래요?" ❌ → "좋아. 언제 먹을래?" ✅
                
                [일상 어휘 사용 - 어휘 대체 불필요]
                - "밥 먹었어?" (일상 어휘 그대로 사용)
                - 친구 사이에는 격식 표현을 쓰지 않음
                """;
            case 2 -> """
                [FRIEND Level 2 - 부드러운 존댓말]
                허용 어미: ~해요, ~어요, ~이에요, ~네요, ~하시나요?, ~이실까요?
                허용 표현: 가벼운 이모티콘(ㅎㅎ), 부드러운 말 늘이기(~요~)
                금지 표현: 격식체(~습니다), 반말(~해, ~야), 과도한 이모티콘(ㅋㅋㅋ), 속어
                예시: "밥 드셨어요?" ✅ / "요즘 뭐하고 지내요?ㅎㅎ" ✅ / "밥 먹었어?" ❌ → "밥 드셨어요?" ✅
                
                [일상 어휘 존댓말로 변환]
                - "밥 먹었어?" → "밥 드셨어요?"
                - 친구 사이지만 존댓말로 예의 있게 표현
                """;
            case 3 -> """
                [FRIEND Level 3 - 완전한 반말]
                허용 어미: ~해, ~야, ~지?, ~할래?
                허용 표현: 이모티콘(ㅋㅋ, ㅎㅎ), 속어, 줄임말, 신조어
                예시: "밥 먹어" ✅ / "밥 먹자 ㅋㅋ" ✅ / "개굿ㅋㅋ 언제?" ✅
                
                [일상 어휘 자유롭게 사용]
                - "밥 먹었어?" (일상 어휘, 이모티콘 자유)
                - 줄임말, 신조어, 속어 사용 허용 (예: "개굿", "ㅋㅋ")
                - 친할수록 서술어 생략하고 짧게 표현
                """;
            default -> getFriendGuideline(1);
        };
    }
    
    private String getHoneyGuideline(int level) {
        return switch (level) {
            case 0 -> """
                [HONEY Level 0 - 분류 불가]
                AI가 문체를 친밀도 1~3 레벨 중 어디에도 명확히 분류하지 못할 경우 사용
                연인 관계에 맞는 애정 표현으로 교정
                """;
            case 1 -> """
                [HONEY Level 1 - 존댓말]
                허용 어미: ~할래요?, ~해요, ~이에요
                허용 표현: 애정 표현
                예시: "밥 먹을래요?" ✅ / "사랑해요" ✅
                
                [일상 어휘 사용 - 친밀한 표현 우선]
                - "밥 먹었어요?" (일상 어휘, 애정 표현 허용)
                """;
            case 2 -> """
                [HONEY Level 2 - 부드러운 반말]
                허용 어미: ~할래?, ~하자, ~해
                허용 표현: 애정 표현, 가벼운 이모티콘
                예시: "밥 먹을래?" ✅ / "사랑해" ✅
                
                [일상 어휘 사용 - 친밀한 표현 우선]
                - "밥 먹었어?" (반말, 애정 표현 허용)
                """;
            case 3 -> """
                [HONEY Level 3 - 부드러운 반말 + 애정표현]
                ⚠️⚠️⚠️ 매우 중요: Level 3은 반드시 부드러운 반말(~해, ~야, ~지?)을 사용합니다. 절대 존댓말(~해요, ~이에요, ~어요)을 사용하지 마세요.
                
                허용 어미: ~해, ~야, ~지?, ~할까?, ~하자
                허용 표현: 이모티콘, 애칭, 애정표현, 줄임말
                예시: "밥 먹었어?" ✅ / "사랑해 ㅎㅎ" ✅ / "보고 싶어" ✅ / "오늘 만날까?" ✅
                금지 예시: "밥 드셨어요?" ❌ / "사랑해요" ❌ / "보고 싶어요" ❌
                
                [일상 어휘 + 애정 표현 자유롭게]
                - "밥 먹었어?" (반말, 이모티콘, 애정 표현 허용)
                - 장난스럽고 애정 표현이 자유로우며, 솔직하고 진심 어린 사랑 표현
                - 속어, 줄임말, 이모티콘 자유롭게 사용
                """;
            default -> getHoneyGuideline(1);
        };
    }
    
    private String getDefaultGuideline(int level) {
        return """
            [기본 지침]
            친밀도 레벨에 맞는 적절한 표현을 사용하세요.
            """;
    }
    
    /**
     * 컨셉별 제약 정보 생성
     */
    private String getConceptConstraint(String concept) {
        log.info("=== getConceptConstraint 시작 - concept='{}' ===", concept);
        try {
            String normalizedConcept = concept != null ? concept.toUpperCase() : "FRIEND";
            log.info("=== getConceptConstraint: 정규화된 concept='{}' ===", normalizedConcept);
            String result = switch (normalizedConcept) {
                case "BOSS" -> """
                    [BOSS 제약]
                    - 절대 반말 사용 불가 (Level 3 불허)
                    - 항상 존댓말 또는 격식체 유지
                    - 이모티콘, 구어체 감탄사, 장난스러운 표현 금지
                    - 상사에 대한 존경과 정중함 유지
                    """;
                case "COWORKER" -> """
                    [COWORKER 제약]
                    - Level 3에서도 존댓말 유지 (부드럽고 친근한 존댓말)
                    - 반말 사용 절대 금지
                    - 이모티콘(ㅋㅋ, ㅎㅎ)은 허용되나, 지나친 장난, 속어는 금지
                    - 업무 환경에 맞는 적절한 표현 유지
                    """;
                case "SENIOR" -> """
                    [SENIOR 제약]
                    - 모든 레벨에서 존댓말 유지 (Level 1도 부드러운 존댓말)
                    - 반말 사용 절대 금지
                    - 과도한 이모티콘, 반말 존칭 혼용, 지나친 장난 금지
                    - 선배에 대한 존중과 예의 유지
                    """;
                case "FRIEND" -> """
                    [FRIEND 제약]
                    - 모든 레벨 허용 (Level 1-3)
                    - ⚠️⚠️⚠️ Level 1과 Level 3은 반드시 반말 사용 (절대 존댓말 금지)
                    - Level 1: 가벼운 반말 (~해, ~야, ~지?, ~할래?)
                    - Level 3: 완전한 반말 (~해, ~야, ~지?, 이모티콘, 속어 허용)
                    - 친구 관계에 맞는 자연스러운 반말 표현
                    - 절대 존댓말(~해요, ~어요, ~이에요, ~세요) 사용 금지
                    """;
                case "HONEY" -> """
                    [HONEY 제약]
                    - 모든 레벨 허용 (Level 1-3)
                    - Level 1: 부드러운 존댓말 (~해요, ~이에요, ~어요)
                    - ⚠️⚠️⚠️ Level 3은 반드시 부드러운 반말 사용 (절대 존댓말 금지)
                    - Level 3: 부드러운 반말 (~해, ~야, ~지?, ~할까?, ~하자)
                    - 연인 관계에 맞는 애정 표현 허용
                    - Level 3에서는 이모티콘, 애칭, 애정표현 자유롭게 사용
                    - 절대 존댓말(~해요, ~이에요, ~어요, ~세요) 사용 금지 (Level 3)
                    """;
                default -> {
                    log.warn("getConceptConstraint: 알 수 없는 concept='{}', 기본 제약 사용", concept);
                    yield """
                        [기본 제약]
                        - 친밀도 레벨에 맞는 적절한 표현 사용
                        """;
                }
            };
            log.info("=== getConceptConstraint 완료 - result 길이={} ===", result != null ? result.length() : 0);
            return result;
        } catch (Exception e) {
            log.error("getConceptConstraint 오류: concept='{}', exception={}", concept, e.getMessage(), e);
            return """
                [기본 제약]
                - 친밀도 레벨에 맞는 적절한 표현 사용
                """;
        }
    }
    
    /**
     * detectedLevel을 1 또는 3으로 정규화
     * 0, 2는 1로, 4 이상은 3으로 조정
     */
    private int normalizeDetectedLevel(int detectedLevel) {
        if (detectedLevel <= 1) {
            return 1;
        }
        if (detectedLevel >= 3) {
            return 3;
        }
        return 1; // 2는 1로
    }
    
    /**
     * 교정된 문장이 컨셉 제약에 맞는지 검증
     * FRIEND 컨셉이면 반말, COWORKER/SENIOR/BOSS 컨셉이면 존댓말인지 확인
     */
    private boolean validateSpeechStyleAgainstConcept(String concept, int currentLevel, String originalMessage, String correctedSentence) {
        if (correctedSentence == null || correctedSentence.trim().isEmpty()) {
            return true; // 빈 문자열은 검증 통과 (별도 처리)
        }
        
        String normalizedConcept = concept != null ? concept.toUpperCase() : "FRIEND";
        String sentence = correctedSentence.trim();
        
        // FRIEND Level 1/3은 반말 사용 필수
        if ("FRIEND".equals(normalizedConcept) && (currentLevel == 1 || currentLevel == 3)) {
            // 존댓말 어미 패턴 체크: ~해요, ~어요, ~이에요, ~세요, ~세요?, ~습니다, ~하세요 등
            boolean hasFormalEnding = sentence.matches(".*[해어]요[?!.]?$") 
                || sentence.matches(".*이에요[?!.]?$")
                || sentence.matches(".*세요[?!.]?$")
                || sentence.matches(".*습니다[?!.]?$")
                || sentence.matches(".*하세요[?!.]?$")
                || sentence.matches(".*드셨어요[?!.]?$")
                || sentence.matches(".*하시나요[?!.]?$")
                || sentence.matches(".*이실까요[?!.]?$")
                || sentence.contains("좋아요")
                || sentence.contains("좋네요")
                || sentence.contains("하세요")
                || sentence.contains("이세요");
            
            if (hasFormalEnding) {
                log.warn("=== validateSpeechStyleAgainstConcept: FRIEND Level {}(반말 필수)인데 존댓말 사용 감지 ===", currentLevel);
                log.warn("=== 원문: '{}', 교정문: '{}' ===", originalMessage, correctedSentence);
                return false;
            }
        }
        
        // HONEY Level 3은 반말 사용 필수
        if ("HONEY".equals(normalizedConcept) && currentLevel == 3) {
            // 존댓말 어미 패턴 체크: ~해요, ~어요, ~이에요, ~세요, ~세요?, ~습니다, ~하세요 등
            boolean hasFormalEnding = sentence.matches(".*[해어]요[?!.]?$") 
                || sentence.matches(".*이에요[?!.]?$")
                || sentence.matches(".*세요[?!.]?$")
                || sentence.matches(".*습니다[?!.]?$")
                || sentence.matches(".*하세요[?!.]?$")
                || sentence.matches(".*드셨어요[?!.]?$")
                || sentence.matches(".*하시나요[?!.]?$")
                || sentence.matches(".*이실까요[?!.]?$")
                || sentence.contains("좋아요")
                || sentence.contains("좋네요")
                || sentence.contains("하세요")
                || sentence.contains("이세요")
                || sentence.contains("사랑해요")
                || sentence.contains("보고 싶어요");
            
            if (hasFormalEnding) {
                log.warn("=== validateSpeechStyleAgainstConcept: HONEY Level 3(반말 필수)인데 존댓말 사용 감지 ===");
                log.warn("=== 원문: '{}', 교정문: '{}' ===", originalMessage, correctedSentence);
                return false;
            }
        }
        
        // COWORKER/SENIOR/BOSS는 존댓말 사용 필수
        if (("COWORKER".equals(normalizedConcept) || "SENIOR".equals(normalizedConcept) || "BOSS".equals(normalizedConcept))) {
            // 반말 어미 패턴 체크: ~해, ~야, ~지?, ~할래?, ~하자 등 (문장 끝)
            boolean hasInformalEnding = sentence.matches(".*[해야지래자][?!.]?$")
                || sentence.matches(".*먹었어[?!.]?$")
                || sentence.matches(".*했어[?!.]?$")
                || sentence.matches(".*있어[?!.]?$")
                || sentence.matches(".*좋아[?!.]?$")
                || sentence.matches(".*됐어[?!.]?$")
                || sentence.matches(".*알았어[?!.]?$")
                || sentence.matches(".*그래[?!.]?$")
                || sentence.matches(".*맞아[?!.]?$")
                || (sentence.endsWith("해") && !sentence.endsWith("해요") && !sentence.endsWith("하세요"))
                || (sentence.endsWith("야") && !sentence.contains("세요"));
            
            // 짧은 반말 표현 체크: "응", "어", "음", "그래", "맞아", "좋아" 등
            boolean isShortInformal = sentence.equals("응") 
                || sentence.equals("어")
                || sentence.equals("음")
                || sentence.equals("그래")
                || sentence.equals("맞아")
                || sentence.equals("좋아")
                || sentence.equals("알았어")
                || sentence.equals("됐어")
                || sentence.equals("응응")
                || sentence.equals("어어")
                || sentence.matches("^응[?!.]?$")
                || sentence.matches("^어[?!.]?$")
                || sentence.matches("^음[?!.]?$");
            
            if (hasInformalEnding || isShortInformal) {
                log.warn("=== validateSpeechStyleAgainstConcept: {} 컨셉(존댓말 필수)인데 반말 사용 감지 ===", normalizedConcept);
                log.warn("=== 원문: '{}', 교정문: '{}' ===", originalMessage, correctedSentence);
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 문장 정규화 유틸리티 메서드
     * 공백, 구두점을 정규화하여 문장 비교에 사용
     */
    private String normalizeSentence(String sentence) {
        if (sentence == null || sentence.isEmpty()) {
            return "";
        }
        return sentence.trim()
            .replaceAll("\\s+", " ") // 여러 공백을 하나로
            .replaceAll("[.,!?;:]", ""); // 구두점 제거
    }
}
