package com.dorandoran.chat.service.agent;

import com.dorandoran.chat.entity.IntimacyProgress;
import com.dorandoran.chat.entity.Chatbot;
import com.dorandoran.chat.enums.ChatRoomConcept;
import com.dorandoran.chat.repository.IntimacyProgressRepository;
import com.dorandoran.chat.repository.ChatRoomRepository;
import com.dorandoran.chat.repository.ChatbotRepository;
import com.dorandoran.chat.service.OpenAIClient;
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
    private final OpenAIClient openAIClient;
    private final IntimacyProgressRepository intimacyProgressRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatbotRepository chatbotRepository;
    private final ObjectMapper objectMapper;

    public Mono<IntimacyAgentResponse> analyze(UUID chatroomId, String userMessage) {
        log.info("=== IntimacyAgent.analyze() 호출됨 ===");
        log.info("=== IntimacyAgent 파라미터 - chatroomId={}, userMessage='{}' ===", chatroomId, userMessage);
        
        int currentLevel = intimacyProgressRepository.findByChatRoomId(chatroomId)
            .map(IntimacyProgress::getIntimacyLevel)
            .orElse(1);
        log.info("=== IntimacyAgent 현재 레벨 조회: {} ===", currentLevel);
        
        String concept = getConceptFromChatRoom(chatroomId);
        
        // 컨셉별 레벨 검증 및 조정
        currentLevel = validateIntimacyLevel(concept, currentLevel);
        
        String systemPrompt = buildIntimacyPrompt(chatroomId, currentLevel, concept);
        log.info("=== IntimacyAgent systemPrompt: {} ===", systemPrompt);
        
        log.info("=== IntimacyAgent OpenAI API 호출 시작 ===");
        return openAIClient.streamRawCompletion(systemPrompt, userMessage)
            .doOnNext(chunk -> log.trace("IntimacyAgent 스트림 청크: '{}'", chunk))
            .doOnError(error -> log.error("IntimacyAgent 스트림 오류", error))
            .collectList()
            .doOnNext(chunks -> log.info("IntimacyAgent collectList 완료: {} 개 청크", chunks.size()))
            .doOnError(error -> log.error("IntimacyAgent collectList 오류", error))
            .map(this::parseIntimacyResponse)
            .doOnSuccess(response -> log.info("IntimacyAgent 파싱 완료: 레벨={}", response.detectedLevel()))
            .doOnError(error -> log.error("IntimacyAgent 파싱 오류", error));
    }
    
    private String getConceptFromChatRoom(UUID chatroomId) {
        return chatRoomRepository.findById(chatroomId)
            .map(room -> {
                if (room.getSettings() != null && room.getSettings().has("concept")) {
                    return room.getSettings().get("concept").asText();
                }
                return "FRIEND";
            })
            .orElse("FRIEND");
    }
    
    private String buildIntimacyPrompt(UUID chatroomId, int level, String concept) {
        // 1. DB에서 Base Prompt 조회
        String basePrompt = getBasePromptFromDB(chatroomId);
        
        // 2. 상세 교정 지침 생성
        String detailedGuideline = getDetailedCorrectionGuideline(concept, level);
        
        // 3. 컨셉 제약 정보
        String conceptConstraint = getConceptConstraint(concept);
        
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
              "detectedLevel": 1-3,
              "correctedSentence": "교정된 문장",
              "feedback": {
                "ko": "한국어 피드백",
                "en": "English feedback"
              },
              "corrections": "변경사항 설명 (예: '오늘 밥 먹었어?' → '오늘 밥 드셨어요?'로 변경)"
            }
            
            [교정 처리 지침]
            교정이 필요 없는 경우:
            - corrections: ""
            - feedback: {"ko": "", "en": ""}
            - correctedSentence: 원문 그대로
            
            교정이 필요한 경우:
            - corrections: 구체적인 변경사항 설명
            - feedback: {ko/en} 각각 명확한 피드백
            - correctedSentence: 수정된 문장
            
            [중요: 컨셉별 제약을 절대 위반하지 마세요]
            """, level, concept, conceptConstraint, detailedGuideline);
        
        // 5. 합성
        return basePrompt + dynamicDirectives;
    }
    
    private String getBasePromptFromDB(UUID chatroomId) {
        return chatRoomRepository.findById(chatroomId)
            .flatMap(room -> {
                if (room.getChatbot() == null) return Optional.empty();
                return chatbotRepository.findById(room.getChatbot().getId());
            })
            .map(Chatbot::getIntimacySystemPrompt)
            .orElse(getDefaultIntimacyBasePrompt());
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
               예: "밥 먹었어?" → "밥 드셨어요?" ✓
            
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
              "detectedLevel": 1-3,
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
    
    private String getConceptGuideline(String concept) {
        return switch (concept) {
            case "FRIEND" -> "친구와의 대화 상황을 고려하여 자연스럽고 편한 표현을 교정하세요.";
            case "HONEY" -> "연인과의 대화 상황을 고려하여 애정 어린 표현을 교정하세요.";
            case "COWORKER" -> "직장 동료와의 대화 상황을 고려하여 예의 바르고 전문적인 표현을 교정하세요.";
            case "SENIOR" -> "선배와의 대화 상황을 고려하여 공손하고 정중한 표현을 교정하세요.";
            case "BOSS" -> "직장 상사와의 대화 상황을 고려하여 존경하고 정중한 표현을 교정하세요.";
            default -> "일반적인 상황에 맞는 적절한 표현을 교정하세요.";
        };
    }
    
    /**
     * 컨셉별 허용 친밀도 레벨 검증 및 조정
     */
    private int validateIntimacyLevel(String concept, int level) {
        try {
            ChatRoomConcept conceptEnum = ChatRoomConcept.fromString(concept);
            
            if (!conceptEnum.isLevelAllowed(level)) {
                int adjustedLevel = conceptEnum.adjustLevel(level);
                log.warn("IntimacyAgent 레벨 조정: {} 컨셉에서 Level {} 는 허용되지 않습니다. Level {} 로 조정합니다.",
                    concept, level, adjustedLevel);
                return adjustedLevel;
            }
            
            return level;
        } catch (Exception e) {
            log.warn("IntimacyAgent 컨셉 검증 실패: {} - 기본값 Level 2 사용", e.getMessage());
            return 2; // 기본값
        }
    }
    
    /**
     * 컨셉별 상세 교정 지침 생성
     */
    private String getDetailedCorrectionGuideline(String concept, int level) {
        return switch (concept) {
            case "BOSS" -> getBossGuideline(level);
            case "COWORKER" -> getCoworkerGuideline(level);
            case "SENIOR" -> getSeniorGuideline(level);
            case "FRIEND" -> getFriendGuideline(level);
            case "HONEY" -> getHoneyGuideline(level);
            default -> getDefaultGuideline(level);
        };
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
            case 2 -> """
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
            case 2 -> """
                [COWORKER Level 2 - 표준 존댓말]
                허용 어미: ~어요, ~해요, ~이에요
                금지 표현: 반말, 이모티콘, 구어체 감탄사
                예시: "회의 준비해요" ✅ / "회의 준비할게" ❌
                
                [일상 어휘 허용하되 정중하게]
                - "밥 드셨어요?" (일상 어휘 허용)
                - "식사하셨어요?" (격식 어휘 권장)
                """;
            case 3 -> """
                [COWORKER Level 3 - 절제된 반말]
                허용 어미: ~해, ~지, ~야
                금지 표현: 과도한 이모티콘(ㅋㅋ/ㅎㅎ), 지나친 장난, 속어
                예시: "회의 준비해" ✅ / "회의 준비해 ㅋㅋ" ❌
                
                [일상 어휘 자유롭게 사용]
                - "밥 먹었어?" (반말 허용)
                """;
            default -> getCoworkerGuideline(2);
        };
    }
    
    private String getSeniorGuideline(int level) {
        return switch (level) {
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
            case 2 -> """
                [SENIOR Level 2 - 표준 존댓말]
                허용 어미: ~어요, ~해요, ~이에요
                금지 표현: 반말, 이모티콘, 구어체 감탄사
                예시: "과제 제출해요" ✅ / "과제 제출할게" ❌
                
                [격식 어휘 필수]
                - 밥 먹다 → 식사하다 (필수: "식사하셨어요?" ✅ / "밥 드셨어요?" ❌)
                - 자다 → 휴식하다/주무시다 (필수)
                - 보다 → 확인하다/검토하다 (학업 맥락)
                - 하다 → 수행하다/실시하다 (학업 맥락)
                잘못된 예: "밥 드셨어요?" → "식사하셨어요?" 로 교정 필수
                """;
            case 3 -> """
                [SENIOR Level 3 - 절제된 반말]
                허용 어미: ~해, ~지, ~야
                금지 표현: 과도한 이모티콘, 반말 존칭 혼용, 지나친 장난
                예시: "과제 제출해" ✅ / "과제 제출해요" (혼용) ❌
                
                [일상 어휘 허용하되 존중 유지]
                - "밥 먹었어?" (반말 허용)
                - "식사하셨어요?" (존댓말 권장, 선택)
                """;
            default -> getSeniorGuideline(2);
        };
    }
    
    private String getFriendGuideline(int level) {
        return switch (level) {
            case 1 -> """
                [FRIEND Level 1 - 가벼운 반말]
                허용 어미: ~하자, ~할래?, ~지?
                금지 표현: 과도한 이모티콘, 속어
                예시: "밥 먹자" ✅ / "밥 먹을래?" ✅
                
                [일상 어휘 사용 - 어휘 대체 불필요]
                - "밥 먹었어?" (일상 어휘 그대로 사용)
                """;
            case 2 -> """
                [FRIEND Level 2 - 친근한 반말]
                허용 어미: ~하장, ~할?, ~지?
                허용 표현: 가벼운 이모티콘
                예시: "밥 먹자" ✅ / "밥 먹을래?" ✅
                
                [일상 어휘 사용 - 어휘 대체 불필요]
                - "밥 먹었어?" (일상 어휘 그대로 사용)
                """;
            case 3 -> """
                [FRIEND Level 3 - 완전한 반말]
                허용 어미: ~해, ~야, ~지?
                허용 표현: 이모티콘, 속어, 줄임말
                예시: "밥 먹어" ✅ / "밥 먹자 ㅋㅋ" ✅
                
                [일상 어휘 자유롭게 사용]
                - "밥 먹었어?" (일상 어휘, 이모티콘 자유)
                """;
            default -> getFriendGuideline(2);
        };
    }
    
    private String getHoneyGuideline(int level) {
        return switch (level) {
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
                [HONEY Level 3 - 반말 + 애정표현]
                허용 어미: ~해, ~야, ~지?
                허용 표현: 이모티콘, 애칭, 애정표현
                예시: "밥 먹어" ✅ / "사랑해 ㅎㅎ" ✅
                
                [일상 어휘 + 애정 표현 자유롭게]
                - "밥 먹었어?" (반말, 이모티콘, 애정 표현 허용)
                """;
            default -> getHoneyGuideline(2);
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
        return switch (concept) {
            case "BOSS" -> """
                [BOSS 제약]
                - 절대 반말 사용 불가 (Level 3 불허)
                - 항상 존댓말 또는 격식체 유지
                - 이모티콘, 구어체 감탄사, 장난스러운 표현 금지
                - 상사에 대한 존경과 정중함 유지
                """;
            case "COWORKER" -> """
                [COWORKER 제약]
                - Level 3에서는 절제된 반말만 허용
                - 과도한 이모티콘(ㅋㅋ/ㅎㅎ), 지나친 장난, 속어 금지
                - 업무 환경에 맞는 적절한 표현 유지
                """;
            case "SENIOR" -> """
                [SENIOR 제약]
                - Level 3에서는 절제된 반말만 허용
                - 과도한 이모티콘, 반말 존칭 혼용, 지나친 장난 금지
                - 선배에 대한 존중과 예의 유지
                """;
            case "FRIEND" -> """
                [FRIEND 제약]
                - 모든 레벨 허용 (Level 1-3)
                - 친구 관계에 맞는 자연스러운 표현
                - Level 3에서는 이모티콘, 속어, 줄임말 허용
                """;
            case "HONEY" -> """
                [HONEY 제약]
                - 모든 레벨 허용 (Level 1-3)
                - 연인 관계에 맞는 애정 표현 허용
                - Level 3에서는 이모티콘, 애칭, 애정표현 자유롭게 사용
                """;
            default -> """
                [기본 제약]
                - 친밀도 레벨에 맞는 적절한 표현 사용
                """;
        };
    }
}
